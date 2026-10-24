package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import com.intellij.util.ui.UIUtil
import com.look4counter.modeleditor.*
import javax.swing.JScrollPane

class ModelServiceDirectEditorTest : BasePlatformTestCase() {
    private lateinit var previous: ModelEditorState
    override fun setUp() {
        super.setUp()
        val settings = ModelEditorSettings.getInstance(project)
        previous = settings.state
        settings.loadState(ModelEditorState(noUseGlueUmlAdFile = true))
    }
    override fun tearDown() {
        try { ModelEditorSettings.getInstance(project).loadState(previous) } finally { super.tearDown() }
    }
    private fun xml() = """<service name="original-service" initial="Router" xmlns="http://www.poscoict.com/glueframework/service" extra="keep">
        <!-- keep this comment -->
        <transaction-manager id="test-tx" commit="true" custom="tx"/>
        <activity name="Router" class="sample.Router" extra="activity">
            <transition name="go" value="Next" custom="transition"/>
            <property name="sql-key" value="sample.select" custom="property"/>
        </activity>
        <activity name="Next" class="sample.Next"><transition value="end"/></activity>
        <activity name="Detached" class="sample.Detached"/>
        <extra-element keep="yes"/>
    </service>""".trimIndent()
    private fun canvas(editor: FileEditor) = editor.component.components.filterIsInstance<JScrollPane>().single().viewport.view as ModelDiagramCanvas
    private fun source(file: com.intellij.openapi.vfs.VirtualFile) = PsiManager.getInstance(project).findFile(file) as XmlFile

    fun testServiceProjectionAndGeometryChangesDoNotModifySourceOrCreateModelFiles() {
        val file = myFixture.addFileToProject("services/direct-service.xml", xml()).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        val original = document.text
        val model = ModelDiagramParser.parse(view)
        assertTrue(model.wires.all { it.explicitContacts })
        assertFalse(view.text.contains("explicitContacts"))
        assertEquals(listOf("Router", "Next", "Detached"), model.nodes.filter { it.isActivity }.map { it.name })
        assertEquals(3, model.wires.size)
        assertEquals("Router", model.wires.first().end!!.name)
        assertEquals(DiagramNodeKind.FinalState, model.wires.last().end!!.kind)
        WriteCommandAction.runWriteCommandAction(project) {
            val child = view.rootTag!!.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Router" }
            child.setAttribute("location", "333,444")
            child.setAttribute("size", "230,-1")
            adapter.apply(view)
        }
        assertEquals(original, document.text)
        assertFalse(FileDocumentManager.getInstance().isDocumentUnsaved(document))
        assertEquals(model, ModelDiagramParser.parse(ServiceDiagramAdapter(project, file).read(source(file))))
        assertNull(file.parent.findChild("direct.glue_uml_ad"))
    }

    fun testRenameConditionsPropertiesAndTransactionsUpdateOnlyServiceSemantics() {
        val file = myFixture.addFileToProject("edit-service.xml", xml()).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        WriteCommandAction.runWriteCommandAction(project) {
            val root = view.rootTag!!
            val router = root.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Router" }
            DiagramXmlText.write(router, "name", "Renamed")
            val property = router.subTags.single { DiagramXmlText.read(it, "key") == "sql-key" }
            DiagramXmlText.write(property, "value", "한글 & <value>\nline")
            root.subTags.filter { it.localName == "wire" }[1].setAttribute("guardcondition", "changed")
            root.subTags.single { it.localName == "property" }.setAttribute("value", "false")
            adapter.apply(view)
        }
        PsiDocumentManager.getInstance(project).commitDocument(document)
        val root = source(file).rootTag!!
        assertEquals("original-service", root.getAttributeValue("name"))
        assertEquals("Renamed", root.getAttributeValue("initial"))
        assertEquals("keep", root.getAttributeValue("extra"))
        assertTrue(document.text.contains("keep this comment"))
        assertTrue(document.text.contains("extra-element"))
        val activity = root.subTags.first { it.localName == "activity" }
        assertEquals("activity", activity.getAttributeValue("extra"))
        val transition = activity.subTags.single { it.localName == "transition" }
        assertEquals("changed", transition.getAttributeValue("name"))
        assertEquals("transition", transition.getAttributeValue("custom"))
        val property = activity.subTags.single { it.localName == "property" }
        assertEquals("한글 & <value>\nline", DiagramXmlText.read(property, "value"))
        assertEquals("property", property.getAttributeValue("custom"))
        val tx = root.subTags.single { it.localName == "transaction-manager" }
        assertEquals("false", tx.getAttributeValue("commit"))
        assertEquals("tx", tx.getAttributeValue("custom"))
        assertFalse(document.text.contains("location="))
        assertFalse(document.text.contains("bendpoint"))
    }

    fun testSameDesignEditorReflectsSourceChangesAndRecoversAfterInvalidSource() {
        val file = myFixture.addFileToProject("refresh-service.xml", xml()).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            assertEquals(3, canvas(editor).diagram.nodes.count { it.isActivity })
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            WriteCommandAction.runWriteCommandAction(project) { document.setText(xml().replace("sample.Router", "sample.New")) }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("sample.New", canvas(editor).diagram.nodes.first { it.name == "Router" }.className)
            WriteCommandAction.runWriteCommandAction(project) { document.setText("<service>") }
            UIUtil.dispatchAllInvocationEvents()
            assertNotNull(canvas(editor).diagram.message)
            WriteCommandAction.runWriteCommandAction(project) { document.setText(xml()) }
            UIUtil.dispatchAllInvocationEvents()
            assertNull(canvas(editor).diagram.message)
            assertEquals(xml(), document.text)
        } finally { Disposer.dispose(editor) }
    }

    fun testExistingDesignActionsEditActualServiceAndUndoRedoRefreshesDesign() {
        val file = myFixture.addFileToProject("actions-service.xml", xml()).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val textEditor = com.intellij.openapi.fileEditor.impl.text.TextEditorProvider.getInstance().createEditor(project, file)
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val before = document.text
        project.replaceService(ModelDiagramDialogs::class.java, object : ModelDiagramDialogs {
            override fun editActivity(data: ActivityPropertiesData) = data.copy(name = "Edited")
            override fun editCondition(condition: String) = "updated"
        }, testRootDisposable)
        try {
            val canvas = canvas(editor)
            val movedNode = canvas.diagram.nodes.first { it.name == "Router" }
            canvas.onMoveStarted!!.invoke()
            canvas.onNodesMoved!!.invoke(mapOf(movedNode to DiagramPoint(400.0, 500.0)))
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(before, document.text)
            canvas.onActivityEditRequested!!.invoke(canvas.diagram.nodes.first { it.name == "Router" })
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("Edited", source(file).rootTag!!.getAttributeValue("initial"))
            assertTrue(canvas.diagram.nodes.any { it.name == "Edited" })
            val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
            assertTrue(undo.isUndoAvailable(textEditor))
            undo.undo(textEditor)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(before, document.text)
            assertTrue(canvas.diagram.nodes.any { it.name == "Router" })
            undo.redo(textEditor)
            UIUtil.dispatchAllInvocationEvents()
            assertTrue(canvas.diagram.nodes.any { it.name == "Edited" })
            val wire = canvas.diagram.wires.first { it.start?.name == "Edited" }
            canvas.onWireEditRequested!!.invoke(wire)
            UIUtil.dispatchAllInvocationEvents()
            val actual = source(file).rootTag!!.subTags.first { it.localName == "activity" }
            assertEquals("updated", actual.subTags.first { it.localName == "transition" }.getAttributeValue("name"))
            val originalWire = canvas.diagram.wires.first { it.start?.name == "Edited" }
            canvas.onMoveStarted!!.invoke()
            canvas.onWireEdited!!.invoke(originalWire, originalWire.copy(end = canvas.diagram.nodes.single { it.name == "Detached" }), null)
            UIUtil.dispatchAllInvocationEvents()
            assertFalse(canvas.diagram.wires.first { it.start?.name == "Edited" }.explicitContacts)
            assertEquals("Detached", actual.subTags.first { it.localName == "transition" }.getAttributeValue("value"))
            assertFalse(document.text.contains("location="))
        } finally { Disposer.dispose(editor); Disposer.dispose(textEditor) }
    }

    fun testDeletingActivityAndAddingAndRemovingPropertiesKeepsProjectionMappingsValid() {
        val file = myFixture.addFileToProject("delete-service.xml", xml()).virtualFile
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        WriteCommandAction.runWriteCommandAction(project) {
            val root = view.rootTag!!
            root.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Detached" }.delete()
            val router = root.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Router" }
            router.subTags.first { it.getAttributeValue("key") == "sql-key" }.delete()
            router.addSubTag(com.intellij.psi.XmlElementFactory.getInstance(project).createTagFromText("<property key=\"new-key\" value=\"first\"/>"), false)
            adapter.apply(view)
        }
        WriteCommandAction.runWriteCommandAction(project) {
            val router = view.rootTag!!.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Router" }
            router.subTags.first { it.getAttributeValue("key") == "new-key" }.setAttribute("value", "second")
            adapter.apply(view)
        }
        val root = source(file).rootTag!!
        assertEquals(2, root.subTags.count { it.localName == "activity" })
        val actual = root.subTags.first { it.localName == "activity" }.subTags.single { it.localName == "property" }
        assertEquals("new-key", actual.getAttributeValue("name"))
        assertEquals("second", actual.getAttributeValue("value"))
    }

    fun testOpenServiceAndProvidersFollowOptionAndDoNotExportModels() {
        val model = myFixture.addFileToProject("flows/target.glue_uml_ad", "<GlueDiagram/>").virtualFile
        val service = myFixture.addFileToProject("services/target-service.xml", xml()).virtualFile
        val other = myFixture.addFileToProject("services/other.xml", "<root/>").virtualFile
        val settings = ModelEditorSettings.getInstance(project)
        settings.loadState(ModelEditorState(serviceDirectory = "services", noUseGlueUmlAdFile = true))
        assertEquals(service, ServiceFiles.resolve(project, "target-service"))
        assertEquals(listOf("target-service"), ServiceFiles.names(project))
        assertTrue(ModelDesignEditorProvider().accept(project, service))
        assertTrue(ModelSourceEditorProvider().accept(project, service))
        assertFalse(ModelDesignEditorProvider().accept(project, other))
        assertNull(ModelServiceSaveListener().save(project, model, "<GlueDiagram/>"))
        var opened: com.intellij.openapi.vfs.VirtualFile? = null
        var selected = ""
        val manager = object : com.intellij.mock.Mock.MyFileEditorManager() {
            override fun openFile(file: com.intellij.openapi.vfs.VirtualFile, focusEditor: Boolean): Array<FileEditor> { opened = file; return emptyArray() }
            override fun setSelectedEditor(file: com.intellij.openapi.vfs.VirtualFile, editorTypeId: String) { selected = editorTypeId }
        }
        project.replaceService(FileEditorManager::class.java, manager, testRootDisposable)
        assertTrue(ServiceFiles.open(project, "target-service.xml"))
        assertEquals(service, opened)
        assertEquals("glue-model-design", selected)
        settings.loadState(ModelEditorState())
        assertEquals(model, ServiceFiles.resolve(project, "target-service"))
        assertFalse(ModelDesignEditorProvider().accept(project, service))
    }

    fun testForeignNamespaceMetadataAndParallelAndLoopRoutesArePreserved() {
        val text = xml().replace("<extra-element keep=\"yes\"/>", """<x:activity xmlns:x="urn:extension" keep="yes"/>""")
            .replace("<property name=\"sql-key\"", """<x:property xmlns:x="urn:extension" name="opaque"/><property name="sql-key"""")
            .replace("<transition name=\"go\" value=\"Next\" custom=\"transition\"/>", """<transition name="go" value="Next"/><transition name="other" value="Next"/><transition name="loop" value="Router"/>""")
        val file = myFixture.addFileToProject("routes-service.xml", text).virtualFile
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        val model = ModelDiagramParser.parse(view)
        assertEquals(3, model.nodes.count { it.isActivity })
        assertEquals(3, model.wires.count { it.start?.name == "Router" })
        assertTrue(model.wires.single { it.condition == "loop" }.bendpoints.isNotEmpty())
        model.wires.forEach { wire ->
            val path = DiagramGeometry.path(wire)
            assertTrue(path.size >= 2)
            path.zipWithNext().forEach { (a, b) -> assertTrue("직각이 아닌 표시 경로: $a → $b", a.x == b.x || a.y == b.y) }
        }
        assertEquals(emptySet<Pair<Int, Int>>(), sharedWireSegments(model.wires.map(DiagramGeometry::path)))
        assertEquals(model, ModelDiagramParser.parse(ServiceDiagramAdapter(project, file).read(source(file))))
        WriteCommandAction.runWriteCommandAction(project) {
            val property = view.rootTag!!.subTags.first { it.getAttributeValue("name") == "Router" }.subTags.first { it.getAttributeValue("key") == "sql-key" }
            property.setAttribute("value", "changed")
            adapter.apply(view)
        }
        val actual = source(file).rootTag!!
        assertEquals(1, actual.subTags.count { it.localName == "activity" && it.namespace == "urn:extension" })
        val router = actual.subTags.first { it.localName == "activity" && it.namespace == ServiceDiagramAdapter.SERVICE_NAMESPACE }
        assertEquals(1, router.subTags.count { it.localName == "property" && it.namespace == "urn:extension" })
    }

    fun testInvalidReferencesAndReadOnlyDocumentsCannotOverwriteSource() {
        val invalid = xml().replace("value=\"Next\"", "value=\"Missing\"")
        val file = myFixture.addFileToProject("invalid-service.xml", invalid).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            assertNotNull(canvas(editor).diagram.message)
            assertEquals(invalid, document.text)
            WriteCommandAction.runWriteCommandAction(project) { document.setText(xml()) }
            UIUtil.dispatchAllInvocationEvents()
            document.setReadOnly(true)
            assertFalse(canvas(editor).canMoveNodes())
            val node = canvas(editor).diagram.nodes.first { it.isActivity }
            canvas(editor).onActivityEditRequested!!.invoke(node)
            assertEquals(xml(), document.text)
        } finally { document.setReadOnly(false); Disposer.dispose(editor) }
    }

    fun testCreationCopyPasteAndDeleteUseServiceXmlWithoutDuplicateActivityNames() {
        val file = myFixture.addFileToProject("creation-service.xml", xml()).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = canvas(editor)
            canvas.setCreationKind(DiagramNodeKind.Activity, ActivityPreset("Router", mutableListOf(PresetProperty("class", "sample.New"))))
            canvas.onChildCreateRequested!!.invoke(DiagramNodeKind.Activity, DiagramPoint(600.0, 600.0))
            UIUtil.dispatchAllInvocationEvents()
            assertTrue(canvas.diagram.nodes.any { it.name == "Router_2" })
            canvas.setCreationKind(null)
            canvas.selectNodesByIds(setOf(canvas.diagram.nodes.single { it.name == "Router_2" }.id))
            val context = com.intellij.openapi.actionSystem.DataContext.EMPTY_CONTEXT
            canvas.copyProvider!!.performCopy(context)
            assertTrue(canvas.pasteProvider!!.isPasteEnabled(context))
            canvas.pasteProvider!!.performPaste(context)
            UIUtil.dispatchAllInvocationEvents()
            assertTrue(canvas.diagram.nodes.any { it.name == "Router_2_2" })
            assertTrue(canvas.deleteProvider!!.canDeleteElement(context))
            canvas.deleteProvider!!.deleteElement(context)
            UIUtil.dispatchAllInvocationEvents()
            val names = source(file).rootTag!!.subTags.filter { it.localName == "activity" }.map { it.getAttributeValue("name") }
            assertTrue("Router_2" in names)
            assertFalse("Router_2_2" in names)
            assertEquals(names.size, names.distinct().size)
            assertNull(file.parent.findChild("creation.glue_uml_ad"))
        } finally { Disposer.dispose(editor) }
    }

    fun testSeparatedPortsSurviveGroupMoveAndSingleMoveReturnsToEditableGeometry() {
        val file = myFixture.addFileToProject("ports-service.xml", xml()).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val before = document.text
        try {
            val canvas = canvas(editor)
            val wire = canvas.diagram.wires.first { it.start?.name == "Router" }
            val oldPath = DiagramGeometry.path(wire)
            val delta = DiagramPoint(40.0, 60.0)
            canvas.onMoveStarted!!.invoke()
            canvas.onNodesMoved!!.invoke(listOf(wire.start!!, wire.end!!).associateWith { it.location + delta })
            UIUtil.dispatchAllInvocationEvents()
            var moved = canvas.diagram.wires.first { it.start?.name == "Router" }
            assertTrue(moved.explicitContacts)
            assertEquals(oldPath.map { it + delta }, DiagramGeometry.path(moved))
            canvas.onMoveStarted!!.invoke()
            canvas.onNodesMoved!!.invoke(mapOf(moved.start!! to moved.start!!.location + delta))
            UIUtil.dispatchAllInvocationEvents()
            moved = canvas.diagram.wires.first { it.start?.name == "Router" }
            assertFalse(moved.explicitContacts)
            assertEquals(before, document.text)
            assertFalse(document.text.contains("explicitContacts"))
        } finally { Disposer.dispose(editor) }
    }
}
