package com.look4counter.modeleditor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorPolicy
import com.intellij.openapi.fileEditor.FileEditorProvider
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.XmlElementFactory
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFilePreCloseCheck
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBPanel
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.JComponent
import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.CopyProvider
import com.intellij.ide.DeleteProvider
import com.intellij.ide.PasteProvider
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.DataContext
import java.math.BigInteger

class ModelDesignEditorProvider : FileEditorProvider, DumbAware {
    override fun accept(project: Project, file: VirtualFile): Boolean = file.fileType == GlueUmlAdFileType.INSTANCE || ServiceDiagramAdapter.accepts(project, file)
    override fun createEditor(project: Project, file: VirtualFile): FileEditor = ModelDesignEditor(project, file)
    override fun getEditorTypeId(): String = "glue-model-design"
    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.PLACE_BEFORE_DEFAULT_EDITOR
}

private class ModelDesignEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val changes = PropertyChangeSupport(this)
    private val snapSettings = ModelSnapSettings.getInstance()
    private val canvas = ModelDiagramCanvas().apply { snapEnabled = snapSettings.enabled }
    private val document = FileDocumentManager.getInstance().getDocument(file)
    private val serviceAdapter = if (ServiceDiagramAdapter.accepts(project, file)) ServiceDiagramAdapter(project, file) else null
    private var disposed = false
    private var scheduled = false
    private var scheduledRefreshModality: ModalityState? = null
    private var refreshGeneration = 0L
    private var pendingPreserveOrigin = false
    private data class ChildCreationRequest(val kind: DiagramNodeKind, val location: DiagramPoint,
                                            val preset: ActivityPreset?, val revision: Long, val requestId: Long, val inputId: String?)
    private var nextCreationRequest = 0L
    private val creationTrace = ChildCreationTrace(::creationTraceState)
    private var moveStamp = -1L
    private val kindSetting = "glue.model.lastChildKind"
    private val creationToolbar: ChildCreationToolbar = ChildCreationToolbar(canvas, DiagramNodeKind.entries.firstOrNull {
            it.name == PropertiesComponent.getInstance(project).getValue(kindSetting)
        }?.takeIf { serviceAdapter == null || it == DiagramNodeKind.Activity } ?: DiagramNodeKind.Activity, ::canEditStructure, { canvas.creationKind != null }, { selected, kind ->
            canvas.setCreationKind(if (selected) kind else null, creationToolbar.selectedPreset)
            canvas.requestFocusInWindow()
        }, { kind ->
            PropertiesComponent.getInstance(project).setValue(kindSetting, kind.name)
            if (canvas.creationKind != null) canvas.setCreationKind(kind)
        }, { snapSettings.enabled }, { snapSettings.enabled = it }, ::editDiagram,
        { canEditDocument() && !scheduled && !canvas.isDragging && canvas.creationKind == null && !canvas.wireCreationMode },
        presets = { ModelEditorSettings.getInstance(project).state.presets },
        onPresetChanged = { preset -> if (canvas.creationKind == DiagramNodeKind.Activity) canvas.setCreationKind(DiagramNodeKind.Activity, preset) },
        wireCreationState = { canvas.wireCreationMode },
        onWireCreationChanged = { canvas.setWireCreationMode(it); canvas.requestFocusInWindow() },
        allowedKinds = if (serviceAdapter == null) DiagramNodeKind.entries.toSet() else setOf(DiagramNodeKind.Activity),
        showCommit = serviceAdapter != null,
        canCommit = ::canCommitServiceChanges,
        onCommit = ::commitServiceChanges,
        canRollback = ::canCommitServiceChanges,
        onRollback = ::rollbackPendingChanges)
    private val panel = JBPanel<JBPanel<*>>(BorderLayout()).apply {
        isFocusable = true
        add(creationToolbar, BorderLayout.NORTH)
        add(JBScrollPane(canvas, javax.swing.ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER,
            javax.swing.ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER).apply { isWheelScrollingEnabled = false }, BorderLayout.CENTER)
    }
    init {
        val application = ApplicationManager.getApplication()
        application.messageBus.connect(this).subscribe(ModelSnapSettingsListener.TOPIC, object : ModelSnapSettingsListener {
            override fun snapChanged(enabled: Boolean) {
                val update = Runnable {
                    if (!disposed && !project.isDisposed) {
                        canvas.snapEnabled = snapSettings.enabled
                        creationToolbar.refresh()
                    }
                }
                if (application.isDispatchThread) update.run() else application.invokeLater(update)
            }
        })
        canvas.onCreationTrace = creationTrace::line
        creationToolbar.onCreationTrace = creationTrace::line
        creationTrace.install(canvas, this)
        creationToolbar.add(ZoomControls(canvas), BorderLayout.EAST)
        canvas.navigationOnly = true
        canvas.canMoveNodes = { canEditStructure() }
        canvas.onMoveStarted = { moveStamp = document?.modificationStamp ?: -1L }
        canvas.onNodesMoved = { moves -> editNodes(moves.mapValues { (node, location) -> node.copy(location = location) }) }
        canvas.onNodesResized = { edits -> editNodes(edits) }
        canvas.onWireEdited = { original, edited, removed -> editWire(original, edited, removed) }
        canvas.onWireEditRequested = ::editCondition
        canvas.onActivityEditRequested = ::editActivity
        canvas.onChildCreateRequested = ::createChildImmediately
        canvas.onWireCreateRequested = ::createWire
        canvas.onCreationModeChanged = { creationToolbar.refresh() }
        canvas.onWireCreationModeChanged = { creationToolbar.refresh() }
        canvas.activityColor = { node -> ModelEditorSettings.getInstance(project).colorForClass(node.className) }
        canvas.activityForeground = { node -> ModelEditorSettings.getInstance(project).foregroundForClass(node.className) }
        project.messageBus.connect(this).subscribe(ModelEditorSettingsListener.TOPIC, object : ModelEditorSettingsListener {
            override fun settingsChanged() {
                ApplicationManager.getApplication().invokeLater {
                    if (!disposed && !project.isDisposed) { creationToolbar.refreshPresets(); canvas.repaint() }
                }
            }
        })
        canvas.copyProvider = object : CopyProvider {
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
            override fun isCopyEnabled(context: DataContext) = !disposed && !scheduled && !canvas.isDragging && canvas.creationKind == null && !canvas.wireCreationMode &&
                canvas.diagram.message == null && canvas.selectedNodes.isNotEmpty()
            override fun isCopyVisible(context: DataContext) = true
            override fun performCopy(context: DataContext) {
                if (isCopyEnabled(context)) copySelection()
            }
        }
        canvas.pasteProvider = object : PasteProvider {
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
            override fun isPastePossible(context: DataContext) = DiagramClipboard.read()?.children?.isNotEmpty() == true
            override fun isPasteEnabled(context: DataContext) = !disposed && !scheduled && !canvas.isDragging && canvas.creationKind == null && !canvas.wireCreationMode &&
                canEditStructure() && isPastePossible(context)
            override fun performPaste(context: DataContext) {
                if (isPasteEnabled(context)) DiagramClipboard.read()?.let(::pasteSelection)
            }
        }
        canvas.deleteProvider = object : DeleteProvider {
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
            override fun canDeleteElement(context: DataContext) = canEditStructure() && !scheduled && !canvas.isDragging && canvas.creationKind == null && !canvas.wireCreationMode &&
                file.isValid && canvas.diagram.message == null && document?.isWritable == true && file.isWritable &&
                (canvas.selectedNodes.isNotEmpty() || canvas.selectedWire != null)
            override fun deleteElement(context: DataContext) {
                if (canDeleteElement(context)) deleteSelection()
            }
        }
        document?.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                requestRefresh()
            }
        }, this)
        refresh()
    }

    private fun modelXml(): XmlFile? {
        val source = PsiManager.getInstance(project).findFile(file) as? XmlFile ?: return null
        return serviceAdapter?.read(source) ?: source
    }

    private fun writeModel(name: String, groupId: String?, action: Runnable, xml: XmlFile) {
        try {
            val adapter = serviceAdapter
            if (adapter == null) {
                WriteCommandAction.runWriteCommandAction(project, name, groupId, action, xml)
            } else {
                ApplicationManager.getApplication().runWriteAction(Runnable {
                    action.run()
                    adapter.markPendingChanges()
                })
            }
        } catch (error: IllegalArgumentException) {
            if (serviceAdapter == null) throw error
            com.intellij.openapi.ui.Messages.showErrorDialog(project, error.message.orEmpty(), "Service 편집")
        }
        creationToolbar.refresh()
    }

    private fun canCommitServiceChanges(): Boolean = serviceAdapter?.hasPendingChanges() == true && !disposed && file.isValid &&
        file.isWritable && document?.isWritable == true

    private fun commitServiceChanges() {
        val adapter = serviceAdapter ?: return
        if (!canCommitServiceChanges()) return
        val view = adapter.pendingView() ?: return
        try {
            WriteCommandAction.runWriteCommandAction(project, "Service XML Commit", null, Runnable {
                adapter.apply(view)
            }, adapter.source())
            refresh(preserveOrigin = true)
        } catch (error: IllegalArgumentException) {
            com.intellij.openapi.ui.Messages.showErrorDialog(project, error.message.orEmpty(), "Service XML Commit 실패")
        }
        creationToolbar.refresh()
    }

    fun hasPendingServiceChanges() = serviceAdapter?.hasPendingChanges() == true

    fun rollbackPendingChanges() {
        val adapter = serviceAdapter ?: return
        if (!adapter.hasPendingChanges()) return
        adapter.rollback()
        refresh(preserveOrigin = true)
        creationToolbar.refresh()
    }

    private fun canEditDocument() = !disposed && file.isValid && file.isWritable && document?.isWritable == true && canvas.diagram.message == null

    private fun canEditStructure() = canEditDocument()

    private fun uniqueServiceActivityName(wanted: String, used: MutableSet<String>): String {
        var candidate = wanted
        var suffix = 2
        while (candidate in used || candidate == "end") candidate = "${wanted}_${suffix++}"
        used += candidate
        return candidate
    }

    private fun editCondition(wire: DiagramWire) {
        if (!canEditStructure()) return
        if (!wire.canEditCondition) return
        val source = document ?: return
        if (!canEditDocument() || scheduled || canvas.isDragging || canvas.creationKind != null || canvas.wireCreationMode) return
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml() ?: return
        val tag = xml.rootTag?.subTags?.filter { it.localName == "wire" }?.getOrNull(wire.index) ?: return
        if (ModelDiagramParser.parse(xml) != canvas.diagram) { refresh(); return }
        val stamp = source.modificationStamp
        val edited = project.getService(ModelDiagramDialogs::class.java).editCondition(wire.condition) ?: return
        if (!canEditDocument() || stamp != source.modificationStamp || !tag.isValid) { refresh(); return }
        if (edited == wire.condition) return
        writeModel("Wire 조건 변경", null, Runnable {
            DiagramXmlText.write(tag, "guardcondition", edited)
        }, xml)
        refresh()
    }

    private fun editDiagram() {
        val source = document ?: return
        if (!canEditDocument() || scheduled || canvas.isDragging || canvas.creationKind != null || canvas.wireCreationMode) return
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml() ?: return
        val tag = xml.rootTag ?: return
        if (ModelDiagramParser.parse(xml) != canvas.diagram) { refresh(); return }
        val properties = tag.subTags.filter { it.localName == "property" }
        val descriptions = properties.filter { DiagramXmlText.read(it, "key") == "description" }
        val initial = ActivityPropertiesData("", properties.mapIndexedNotNull { index, property ->
            val key = DiagramXmlText.read(property, "key")
            if (key == "description") null else ActivityProperty(key, DiagramXmlText.read(property, "value"), index)
        }, descriptions.firstOrNull()?.let { DiagramXmlText.read(it, "value") }.orEmpty())
        val stamp = source.modificationStamp
        val edited = project.getService(ModelDiagramDialogs::class.java).editDiagram(initial) ?: return
        if (!canEditDocument() || stamp != source.modificationStamp || !tag.isValid) { refresh(); return }
        if (edited == initial || edited.validationError() != null) return
        val retained = edited.properties.mapNotNull { it.sourceIndex }
        if (retained.distinct().size != retained.size || retained.any { index -> initial.properties.none { it.sourceIndex == index } }) return
        writeModel("Diagram 속성 변경", null, Runnable {
            initial.properties.mapNotNull { it.sourceIndex }.filter { it !in retained }.forEach { properties[it].delete() }
            edited.properties.forEach { property ->
                val target = property.sourceIndex?.let { properties[it] }
                    ?: tag.addSubTag(XmlElementFactory.getInstance(project).createTagFromText("<property/>"), false)
                if (DiagramXmlText.read(target, "key") != property.key) DiagramXmlText.write(target, "key", property.key)
                if (DiagramXmlText.read(target, "value") != property.value) DiagramXmlText.write(target, "value", property.value)
            }
            if (edited.description != initial.description) {
                if (edited.description.isEmpty()) descriptions.forEach { it.delete() }
                else {
                    val target = descriptions.firstOrNull()
                        ?: tag.addSubTag(XmlElementFactory.getInstance(project).createTagFromText("<property key=\"description\"/>"), false)
                    DiagramXmlText.write(target, "value", edited.description)
                }
            }
        }, xml)
        refresh()
    }

    private fun editActivity(node: DiagramNode) {
        val source = document ?: return
        if (!node.isActivity || !canEditStructure() || scheduled || canvas.isDragging || canvas.creationKind != null || canvas.wireCreationMode) return
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml() ?: return
        if (ModelDiagramParser.parse(xml) != canvas.diagram) { refresh(); return }
        val tag = xml.rootTag?.subTags?.firstOrNull { it.localName == "child" && it.getAttributeValue("id") == node.id &&
            it.getAttributeValue("type", "http://www.w3.org/2001/XMLSchema-instance")?.substringAfterLast(':') == "Activity" } ?: return
        val properties = tag.subTags.filter { it.localName == "property" }
        val descriptionTags = properties.filter { DiagramXmlText.read(it, "key") == "description" }
        val initial = ActivityPropertiesData(node.name, properties.mapIndexedNotNull { index, property ->
            val key = DiagramXmlText.read(property, "key")
            if (key == "description") null else ActivityProperty(key, DiagramXmlText.read(property, "value"), index)
        }, descriptionTags.firstOrNull()?.let { DiagramXmlText.read(it, "value") }.orEmpty())
        val stamp = source.modificationStamp
        val edited = project.getService(ModelDiagramDialogs::class.java).editActivity(initial) ?: return
        if (!canEditDocument() || stamp != source.modificationStamp || !tag.isValid) { refresh(); return }
        if (edited == initial || edited.validationError() != null) return
        val retained = edited.properties.mapNotNull { it.sourceIndex }
        if (retained.distinct().size != retained.size || retained.any { index -> initial.properties.none { it.sourceIndex == index } }) return
        writeModel("Activity 속성 변경", null, Runnable {
            if (edited.name != initial.name) DiagramXmlText.write(tag, "name", edited.name)
            initial.properties.mapNotNull { it.sourceIndex }.filter { it !in retained }.forEach { properties[it].delete() }
            edited.properties.forEach { property ->
                val target = property.sourceIndex?.let { properties[it] }
                    ?: tag.addSubTag(XmlElementFactory.getInstance(project).createTagFromText("<property/>"), false)
                if (DiagramXmlText.read(target, "key") != property.key) DiagramXmlText.write(target, "key", property.key)
                if (DiagramXmlText.read(target, "value") != property.value) DiagramXmlText.write(target, "value", property.value)
            }
            if (edited.description != initial.description) {
                if (edited.description.isEmpty()) descriptionTags.forEach { it.delete() }
                else {
                    val target = descriptionTags.firstOrNull()
                        ?: tag.addSubTag(XmlElementFactory.getInstance(project).createTagFromText("<property key=\"description\"/>"), false)
                    DiagramXmlText.write(target, "value", edited.description)
                }
            }
        }, xml)
        refresh()
    }

    private fun createChildImmediately(kind: DiagramNodeKind, location: DiagramPoint) {
        val requestId = ++nextCreationRequest
        val inputId = canvas.lastCreationInputId
        if (!canEditStructure() || (serviceAdapter != null && kind != DiagramNodeKind.Activity) || canvas.creationKind != kind) {
            creationTrace.record("CREATE_REJECT", "request=$requestId input=$inputId requestedKind=$kind reason=not_editable_or_mode")
            return
        }
        val request = ChildCreationRequest(kind, location, canvas.creationPreset?.detached(), canvas.creationRevision, requestId, inputId)
        creationTrace.record("CREATE_ACCEPT", "request=$requestId input=$inputId requestedKind=$kind modelX=${location.x} modelY=${location.y}")
        try { createChild(request) } catch (error: Throwable) {
            creationTrace.record("CREATE_ERROR", "request=${request.requestId} input=${request.inputId} error=${error.javaClass.name}")
            throw error
        }
    }

    private fun createChild(request: ChildCreationRequest) {
        if (!canEditStructure()) return
        if (serviceAdapter != null && request.kind != DiagramNodeKind.Activity) return
        val kind = request.kind
        val location = request.location
        val context = "request=${request.requestId} input=${request.inputId}"
        val source = document ?: run { creationTrace.record("CREATE_CANCEL", "$context reason=document_missing"); return }
        if (!canEditDocument() || canvas.creationKind != kind) {
            creationTrace.record("CREATE_CANCEL", "$context reason=not_editable_or_mode"); return
        }
        if (!TransactionGuard.getInstance().isWritingAllowed) {
            creationTrace.record("CREATE_CANCEL", "$context reason=write_unsafe")
            requestRefresh(preserveOrigin = true)
            return
        }
        creationTrace.record("CREATE_BEGIN", "$context requestedKind=$kind requestedRevision=${request.revision}")
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml()
            ?: run { creationTrace.record("CREATE_CANCEL", "$context reason=xml_missing"); return }
        val root = xml.rootTag ?: run { creationTrace.record("CREATE_CANCEL", "$context reason=root_missing"); return }
        if (ModelDiagramParser.parse(xml).message != null) {
            creationTrace.record("CREATE_CANCEL", "$context reason=invalid_xml"); refresh(); return
        }
        val id = ((root.subTags.mapNotNull { it.getAttributeValue("id")?.toBigIntegerOrNull() }.maxOrNull()
            ?: BigInteger.ZERO) + BigInteger.ONE).toString()
        val preset = request.preset?.takeIf { kind == DiagramNodeKind.Activity }
        fun number(value: Double) = DiagramXmlText.coordinate(value)
        writeModel("Child 추가", null, Runnable {
            val child = XmlElementFactory.getInstance(project).createTagFromText("<child/>")
            val xsiUri = "http://www.w3.org/2001/XMLSchema-instance"
            val xsiPrefix = root.attributes.firstOrNull { it.name.startsWith("xmlns:") && it.value == xsiUri }
                ?.name?.substringAfter(':') ?: "xsi"
            if (root.getAttributeValue("xmlns:$xsiPrefix") != xsiUri) child.setAttribute("xmlns:$xsiPrefix", xsiUri)
            val typePrefix = root.attributes.firstOrNull { it.name.startsWith("xmlns:") &&
                it.value == "http://www.glueframework.com/activitydiagram/" }?.name?.substringAfter(':')
            child.setAttribute("$xsiPrefix:type", if (typePrefix == null) kind.name else "$typePrefix:${kind.name}")
            child.setAttribute("id", id)
            child.setAttribute("location", "${number(location.x)},${number(location.y)}")
            child.setAttribute("size", if (kind == DiagramNodeKind.Activity) "100,-1" else "-1,-1")
            if (kind == DiagramNodeKind.Activity) {
                val wanted = preset?.name ?: "Activity"
                val name = if (serviceAdapter == null) wanted else uniqueServiceActivityName(wanted,
                    root.subTags.filter { it.localName == "child" }.map { DiagramXmlText.read(it, "name") }.toMutableSet())
                DiagramXmlText.write(child, "name", name)
                preset?.properties?.filter { it.key != "description" }?.forEach { property ->
                    val tag = child.addSubTag(XmlElementFactory.getInstance(project).createTagFromText("<property/>"), false)
                    DiagramXmlText.write(tag, "key", property.key); DiagramXmlText.write(tag, "value", property.value)
                }
                if (!preset?.description.isNullOrEmpty()) {
                    val tag = child.addSubTag(XmlElementFactory.getInstance(project).createTagFromText("<property key=\"description\"/>"), false)
                    DiagramXmlText.write(tag, "value", preset.description)
                }
            }
            val firstWire = root.subTags.firstOrNull { it.localName == "wire" }
            if (firstWire == null) root.addSubTag(child, false) else root.addBefore(child, firstWire)
        }, xml)
        refresh(preserveOrigin = true)
        canvas.selectNodesByIds(setOf(id))
        creationTrace.record("CREATE_DONE", "$context nodeId=$id kind=$kind modelX=${location.x} modelY=${location.y}")
    }

    private fun createWire(draft: DiagramWire) {
        if (!canEditStructure()) return
        val source = document ?: return
        if (!canEditDocument() || scheduled || !canvas.wireCreationMode || source.modificationStamp != moveStamp) { refresh(); return }
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml() ?: return
        val root = xml.rootTag ?: return
        if (ModelDiagramParser.parse(xml) != canvas.diagram) { refresh(); return }
        val start = draft.start ?: return
        val end = draft.end ?: return
        if (start.id == end.id && start.kind == end.kind) return
        val children = root.subTags.filter { it.localName == "child" }
        fun tagFor(node: DiagramNode) = children.firstOrNull { it.getAttributeValue("id") == node.id &&
            it.getAttributeValue("type", "http://www.w3.org/2001/XMLSchema-instance")?.substringAfterLast(':') == node.kind.name }
        val startTag = tagFor(start) ?: return
        val endTag = tagFor(end) ?: return
        val index = root.subTags.count { it.localName == "wire" }
        val id = ((root.subTags.mapNotNull { it.getAttributeValue("id")?.toBigIntegerOrNull() }.maxOrNull() ?: BigInteger.ZERO) + BigInteger.ONE).toString()
        fun number(value: Double) = DiagramXmlText.coordinate(value)
        fun point(value: DiagramPoint) = "${number(value.x)},${number(value.y)}"
        writeModel("Wire 추가", null, Runnable {
            val wire = XmlElementFactory.getInstance(project).createTagFromText("<wire/>")
            wire.setAttribute("id", id)
            DiagramXmlText.write(wire, "guardcondition", draft.condition.ifBlank { "success" })
            draft.firstRelativePoint?.let { wire.setAttribute("firstRelativePoint", point(it)) }
            draft.lastRelativePoint?.let { wire.setAttribute("lastRelativePoint", point(it)) }
            draft.bendpoints.forEach { bend ->
                val tag = wire.addSubTag(XmlElementFactory.getInstance(project).createTagFromText("<bendpoint/>"), false)
                tag.setAttribute("firstDimension", point(bend.first)); tag.setAttribute("secondDimension", point(bend.second))
                bend.weight?.let { tag.setAttribute("weight", it.toString()) }
            }
            root.addSubTag(wire, false)
            for ((tag, attribute) in listOf(startTag to "output", endTag to "input")) {
                val existing = tag.getAttributeValue(attribute).orEmpty().split(Regex("\\s+")).filter { it.isNotEmpty() }
                tag.setAttribute(attribute, (existing + "//@wire.$index").joinToString(" "))
            }
        }, xml)
        refresh()
        canvas.selectWireByIndex(index)
    }

    private fun deleteSelection() {
        if (!canEditStructure()) return
        val source = document ?: return
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml() ?: return
        val root = xml.rootTag ?: return
        val model = ModelDiagramParser.parse(xml)
        if (model != canvas.diagram) { refresh(); return }
        val children = root.subTags.filter { it.localName == "child" }
        val wires = root.subTags.filter { it.localName == "wire" }
        val selected = canvas.selectedNodes
        val removedChildren = selected.map { node ->
            children.firstOrNull { it.getAttributeValue("id").orEmpty() == node.id &&
                it.getAttributeValue("type", "http://www.w3.org/2001/XMLSchema-instance")?.substringAfterLast(':') == node.kind.name }
                ?: return
        }.toSet()
        val removedWireIndices = selected.flatMap { it.inputs + it.outputs }.toMutableSet()
        canvas.selectedWire?.let { removedWireIndices.add(it.index) }
        removedWireIndices.retainAll(wires.indices.toSet())
        if (removedChildren.isEmpty() && removedWireIndices.isEmpty()) return
        val retainedIndices = wires.indices.filter { it !in removedWireIndices }
            .mapIndexed { newIndex, oldIndex -> oldIndex to newIndex }.toMap()
        val reference = Regex("^//@wire\\.(\\d+)$")
        writeModel("선택 Child·Wire 삭제", null, Runnable {
            children.filter { it !in removedChildren }.forEach { tag ->
                for (attribute in listOf("input", "output")) {
                    val raw = tag.getAttributeValue(attribute) ?: continue
                    val before = raw.split(Regex("\\s+")).filter { it.isNotEmpty() }
                    val after = before.mapNotNull { token ->
                        val oldIndex = reference.matchEntire(token)?.groupValues?.get(1)?.toIntOrNull()
                        when {
                            oldIndex in removedWireIndices -> null
                            oldIndex != null && retainedIndices[oldIndex] != null -> "//@wire.${retainedIndices.getValue(oldIndex)}"
                            else -> token
                        }
                    }
                    if (after != before) {
                        if (after.isEmpty()) tag.getAttribute(attribute)?.delete()
                        else tag.setAttribute(attribute, after.joinToString(" "))
                    }
                }
            }
            wires.filterIndexed { index, _ -> index in removedWireIndices }.forEach { it.delete() }
            removedChildren.forEach { it.delete() }
        }, xml)
        canvas.clearSelection()
        refresh(preserveOrigin = true)
    }

    private fun copySelection() {
        val source = document ?: return
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml() ?: return
        val root = xml.rootTag ?: return
        val model = ModelDiagramParser.parse(xml)
        if (model != canvas.diagram) { refresh(); return }
        val selected = canvas.selectedNodes.toSet()
        val childTags = root.subTags.filter { it.localName == "child" }
        val children = model.nodes.filter { it in selected }.map { node ->
            val tag = childTags.firstOrNull { it.getAttributeValue("id").orEmpty() == node.id &&
                it.getAttributeValue("type", "http://www.w3.org/2001/XMLSchema-instance")?.substringAfter(':') == node.kind.name }
                ?: return
            CopiedDiagramChild(node, tag.text)
        }
        if (children.isEmpty()) return
        val wireTags = root.subTags.filter { it.localName == "wire" }
        val wires = model.wires.filter { it.start in selected && it.end in selected }.map {
            CopiedDiagramWire(it, wireTags[it.index].text)
        }
        val namespaces = root.attributes.filter { it.name == "xmlns" || it.name.startsWith("xmlns:") }
            .associate { it.name to it.value.orEmpty() }
        DiagramClipboard.write(DiagramClipboardData(children, wires, namespaces, canvas.screenOffset, canvas.zoom))
    }

    private fun pasteSelection(data: DiagramClipboardData) {
        if (!canEditStructure()) return
        val source = document ?: return
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml() ?: return
        val root = xml.rootTag ?: return
        if (ModelDiagramParser.parse(xml).message != null) return
        val offset = 15.0 * (data.pasteCount + 1)
        val anchor = DiagramPoint(data.children.minOf { it.node.location.x }, data.children.minOf { it.node.location.y })
        val delta = DiagramPoint((anchor.x * data.zoom + data.screenOffset.x - canvas.screenOffset.x + offset) / canvas.zoom - anchor.x,
            (anchor.y * data.zoom + data.screenOffset.y - canvas.screenOffset.y + offset) / canvas.zoom - anchor.y)
        val wireBase = root.subTags.count { it.localName == "wire" }
        val wireIndices = data.wires.mapIndexed { index, copied -> copied.wire.index to wireBase + index }.toMap()
        var nextId = (root.subTags.mapNotNull { it.getAttributeValue("id")?.toBigIntegerOrNull() }
            .maxOrNull() ?: BigInteger.ZERO) + BigInteger.ONE
        val pastedIds = linkedSetOf<String>()
        val usedNames = root.subTags.filter { it.localName == "child" }.map { DiagramXmlText.read(it, "name") }.toMutableSet()
        fun number(value: Double) = DiagramXmlText.coordinate(value)
        fun point(value: DiagramPoint) = "${number(value.x)},${number(value.y)}"
        writeModel("Child·Wire 붙여넣기", null, Runnable {
            fun cloneTag(text: String): XmlTag = XmlElementFactory.getInstance(project).createTagFromText(text).also { tag ->
                data.namespaces.forEach { (name, value) -> tag.setAttribute(name, value) }
                tag.setAttribute("id", nextId.toString())
                nextId += BigInteger.ONE
            }
            fun refs(tag: XmlTag, attribute: String, originals: List<Int>) {
                val mapped = originals.mapNotNull { wireIndices[it] }.joinToString(" ") { "//@wire.$it" }
                if (mapped.isEmpty()) tag.getAttribute(attribute)?.delete() else tag.setAttribute(attribute, mapped)
            }
            val children = data.children.map { copied ->
                cloneTag(copied.xml).also { tag ->
                    pastedIds.add(tag.getAttributeValue("id")!!)
                    if (serviceAdapter != null && copied.node.isActivity)
                        DiagramXmlText.write(tag, "name", uniqueServiceActivityName(copied.node.name, usedNames))
                    tag.setAttribute("location", point(copied.node.location + delta))
                    refs(tag, "input", copied.node.inputs)
                    refs(tag, "output", copied.node.outputs)
                    DiagramXmlText.normalizeCoordinates(tag)
                }
            }
            val wires = data.wires.map { copied ->
                cloneTag(copied.xml).also { tag ->
                    copied.wire.firstRelativePoint?.let { tag.setAttribute("firstRelativePoint", point(it + delta)) }
                    copied.wire.lastRelativePoint?.let { tag.setAttribute("lastRelativePoint", point(it + delta)) }
                    tag.putUserData(explicitWireContactsKey, serviceAdapter != null && copied.wire.explicitContacts)
                    DiagramXmlText.normalizeCoordinates(tag)
                }
            }
            val firstWire = root.subTags.firstOrNull { it.localName == "wire" }
            children.forEach { if (firstWire == null) root.addSubTag(it, false) else root.addBefore(it, firstWire) }
            wires.forEach { root.addSubTag(it, false) }
        }, xml)
        data.pasteCount++
        refresh(preserveOrigin = true)
        canvas.selectNodesByIds(pastedIds)
    }

    private fun editWire(original: DiagramWire, edited: DiagramWire, removedBendpoint: Int?) {
        if (!canEditStructure()) return
        val source = document ?: return
        if (!source.isWritable || !file.isWritable || source.modificationStamp != moveStamp) { refresh(); return }
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml() ?: return
        val root = xml.rootTag ?: return
        val tag = root.subTags.filter { it.localName == "wire" }.getOrNull(original.index) ?: return
        val children = root.subTags.filter { it.localName == "child" }
        fun child(node: DiagramNode?): XmlTag? = children.firstOrNull {
            node != null && it.getAttributeValue("id").orEmpty() == node.id &&
                it.getAttributeValue("type", "http://www.w3.org/2001/XMLSchema-instance")?.substringAfterLast(':') == node.kind.name
        }
        val start = child(edited.start)
        val end = child(edited.end)
        if (start == null || end == null || start == end) { refresh(); return }
        val oldBends = tag.subTags.filter { it.localName == "bendpoint" }
        if (oldBends.size != original.bendpoints.size || edited.bendpoints.size !in maxOf(0, oldBends.size - 1)..oldBends.size + 1 ||
            (edited.bendpoints.size == oldBends.size - 1 && (removedBendpoint == null || removedBendpoint !in oldBends.indices))) {
            refresh(); return
        }
        fun number(value: Double) = DiagramXmlText.coordinate(value)
        fun point(value: DiagramPoint) = "${number(value.x)},${number(value.y)}"
        fun updateRefs(attribute: String, target: XmlTag) {
            val reference = "//@wire.${original.index}"
            children.forEach { node ->
                val before = node.getAttributeValue(attribute).orEmpty().split(Regex("\\s+")).filter { it.isNotEmpty() }
                val after = before.filter { it != reference } + if (node == target) listOf(reference) else emptyList()
                if (before != after) {
                    if (after.isEmpty()) node.getAttribute(attribute)?.delete()
                    else node.setAttribute(attribute, after.joinToString(" "))
                }
            }
        }
        writeModel("Wire 경로·연결 변경", null, Runnable {
            val sameStart = edited.start?.id == original.start?.id && edited.start?.kind == original.start?.kind
            val sameEnd = edited.end?.id == original.end?.id && edited.end?.kind == original.end?.kind
            tag.putUserData(explicitWireContactsKey, edited.explicitContacts && sameStart && sameEnd)
            if (edited.start != original.start) updateRefs("output", start)
            if (edited.end != original.end) updateRefs("input", end)
            if (edited.firstRelativePoint != original.firstRelativePoint) edited.firstRelativePoint?.let { tag.setAttribute("firstRelativePoint", point(it)) }
            if (edited.lastRelativePoint != original.lastRelativePoint) edited.lastRelativePoint?.let { tag.setAttribute("lastRelativePoint", point(it)) }
            if (edited.bendpoints != original.bendpoints) {
                if (edited.bendpoints.size < oldBends.size) oldBends[removedBendpoint!!].delete()
                if (edited.bendpoints.size > oldBends.size) {
                    val oldPositions = DiagramGeometry.path(original).drop(1).dropLast(1)
                    val newPositions = DiagramGeometry.path(edited).drop(1).dropLast(1)
                    val insertedAt = oldPositions.indices.firstOrNull { index ->
                        kotlin.math.hypot(oldPositions[index].x - newPositions[index].x, oldPositions[index].y - newPositions[index].y) > 0.000001
                    } ?: oldBends.size
                    val newTag = XmlElementFactory.getInstance(project).createTagFromText("<bendpoint/>")
                    if (insertedAt < oldBends.size) tag.addBefore(newTag, oldBends[insertedAt]) else tag.addSubTag(newTag, false)
                }
                tag.subTags.filter { it.localName == "bendpoint" }.zip(edited.bendpoints).forEach { (bendTag, bend) ->
                    bendTag.setAttribute("firstDimension", point(bend.first))
                    bendTag.setAttribute("secondDimension", point(bend.second))
                    if (bendTag.getAttributeValue("weight")?.toDoubleOrNull() != bend.weight) {
                        if (bend.weight == null) bendTag.getAttribute("weight")?.delete()
                        else bendTag.setAttribute("weight", bend.weight.toString())
                    }
                }
            }
            DiagramXmlText.normalizeCoordinates(tag)
        }, xml)
        refresh()
    }

    private fun editNodes(editsByNode: Map<DiagramNode, DiagramNode>) {
        if (!canEditStructure()) return
        val source = document ?: return
        if (!source.isWritable || !file.isWritable || source.modificationStamp != moveStamp) {
            refresh()
            return
        }
        PsiDocumentManager.getInstance(project).commitDocument(source)
        val xml = modelXml() ?: return
        val tags = xml.rootTag?.subTags?.filter { it.localName == "child" }.orEmpty()
        val edits = editsByNode.map { (node, edited) ->
            val tag = tags.firstOrNull { it.getAttributeValue("id").orEmpty() == node.id &&
                it.getAttributeValue("type", "http://www.w3.org/2001/XMLSchema-instance")?.substringAfterLast(':') == node.kind.name }
            Triple(tag, node, edited)
        }
        if (edits.any { it.first == null }) { refresh(); return }
        fun number(value: Double) = DiagramXmlText.coordinate(value)
        val wires = ModelDiagramParser.parse(xml).wires
        val wireTags = xml.rootTag?.subTags?.filter { it.localName == "wire" }.orEmpty()
        writeModel("Child 위치·너비 변경", null, Runnable {
            edits.forEach { (tag, original, edited) ->
                if (edited.location != original.location)
                    tag!!.setAttribute("location", "${number(edited.location.x)},${number(edited.location.y)}")
                if (edited.width != original.width) {
                    val height = tag!!.getAttributeValue("size")?.substringAfter(',', "-1") ?: "-1"
                    tag.setAttribute("size", "${number(edited.width)},$height")
                }
                DiagramXmlText.normalizeCoordinates(tag!!)
            }
            wires.forEach { wire ->
                val moved = DiagramEdits.relocateNodes(wire, editsByNode)
                wireTags[wire.index].putUserData(explicitWireContactsKey, moved.explicitContacts)
                if (moved.firstRelativePoint != wire.firstRelativePoint) moved.firstRelativePoint?.let {
                    wireTags[wire.index].setAttribute("firstRelativePoint", "${number(it.x)},${number(it.y)}")
                }
                if (moved.lastRelativePoint != wire.lastRelativePoint) moved.lastRelativePoint?.let {
                    wireTags[wire.index].setAttribute("lastRelativePoint", "${number(it.x)},${number(it.y)}")
                }
                if (moved.bendpoints != wire.bendpoints) {
                    wireTags[wire.index].subTags.filter { it.localName == "bendpoint" }
                        .zip(moved.bendpoints).forEach { (tag, bend) ->
                            tag.setAttribute("firstDimension", "${number(bend.first.x)},${number(bend.first.y)}")
                            tag.setAttribute("secondDimension", "${number(bend.second.x)},${number(bend.second.y)}")
                        }
                }
                if (wire.start in editsByNode || wire.end in editsByNode)
                    DiagramXmlText.normalizeCoordinates(wireTags[wire.index])
            }
        }, xml)
        refresh()
    }

    private fun requestRefresh(preserveOrigin: Boolean = false) {
        if (disposed || !file.isValid || project.isDisposed) return
        pendingPreserveOrigin = pendingPreserveOrigin || preserveOrigin
        val requestedModality = ModalityState.defaultModalityState()
        val modality = if (TransactionGuard.getInstance().isWriteSafeModality(requestedModality)) requestedModality
            else ModalityState.nonModal()
        if (scheduled && scheduledRefreshModality == modality) {
            if (canvas.creationKind != null) creationTrace.record("REFRESH_MERGED")
            return
        }
        scheduled = true
        scheduledRefreshModality = modality
        val generation = ++refreshGeneration
        val traceRefresh = canvas.creationKind != null
        if (traceRefresh) creationTrace.record("REFRESH_SCHEDULED")
        ApplicationManager.getApplication().invokeLater({
            if (generation != refreshGeneration) return@invokeLater
            val preserve = pendingPreserveOrigin
            pendingPreserveOrigin = false
            scheduled = false
            scheduledRefreshModality = null
            if (traceRefresh) creationTrace.record("REFRESH_RUN")
            if (!disposed && file.isValid && !project.isDisposed) refresh(preserve)
        }, modality)
    }

    private fun refresh(preserveOrigin: Boolean = false) {
        if (disposed || !file.isValid || project.isDisposed) return
        if (!ApplicationManager.getApplication().isDispatchThread || !TransactionGuard.getInstance().isWritingAllowed) {
            requestRefresh(preserveOrigin)
            return
        }
        document?.let { PsiDocumentManager.getInstance(project).commitDocument(it) }
        val model = try {
            modelXml()?.let(ModelDiagramParser::parse) ?: ModelDiagram(emptyList(), emptyList(), "XML을 Source 탭에서 확인하세요.")
        } catch (error: IllegalArgumentException) {
            ModelDiagram(emptyList(), emptyList(), error.message ?: "Service XML을 Source 탭에서 확인하세요.")
        }
        canvas.changeHighlights = if (model.message == null) serviceAdapter?.changeHighlights() ?: DiagramChangeHighlights() else DiagramChangeHighlights()
        canvas.display(model, preserveOrigin)
        val editable = canEditDocument()
        if (!editable) { canvas.setCreationKind(null); canvas.setWireCreationMode(false) }
        creationToolbar.refresh()
    }

    override fun selectNotify() = requestRefresh()

    private fun creationTraceState(): String {
        val app = ApplicationManager.getApplication()
        return "file=${file.name} mode=${canvas.creationKind} revision=${canvas.creationRevision} preset=${canvas.creationPreset != null} " +
            "refreshPending=$scheduled " +
            "stamp=${document?.modificationStamp} docWritable=${document?.isWritable} fileValid=${file.isValid} fileWritable=${file.isValid && file.isWritable} " +
            "canvasInvalid=${canvas.diagram.message != null} disposed=$disposed projectDisposed=${project.isDisposed} " +
            "edt=${app.isDispatchThread} writeSafe=${TransactionGuard.getInstance().isWritingAllowed} " +
            "modality=${if (app.isDispatchThread) ModalityState.current() else "background"}"
    }

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent = canvas
    override fun getName(): String = "Design"
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) = Unit
    override fun isModified(): Boolean = serviceAdapter?.hasPendingChanges() == true ||
        (document?.let { FileDocumentManager.getInstance().isDocumentUnsaved(it) } == true)
    override fun isValid(): Boolean = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = changes.addPropertyChangeListener(listener)
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = changes.removePropertyChangeListener(listener)
    override fun dispose() {
        disposed = true
        creationTrace.record("SESSION_END")
    }
}

class ModelServicePendingCloseCheck : VirtualFilePreCloseCheck {
    override fun canCloseFile(file: VirtualFile): Boolean {
        val candidates = ProjectManager.getInstance().openProjects.flatMap { project ->
            FileEditorManager.getInstance(project).getEditors(file).filterIsInstance<ModelDesignEditor>()
                .map { project to it }
        }.filter { (_, editor) -> editor.hasPendingServiceChanges() }
        if (candidates.isEmpty()) return true
        val (owner, _) = candidates.first()
        val choice = Messages.showDialog(owner,
            "커밋하지 않은 변경사항이 있습니다. 변경사항을 버리고 편집기를 닫을까요?",
            "서비스 변경사항", arrayOf("변경사항 버리고 닫기", "계속 편집"), 1, Messages.getQuestionIcon())
        if (choice != 0) return false
        candidates.map { it.second }.distinct().forEach(ModelDesignEditor::rollbackPendingChanges)
        return true
    }
}
