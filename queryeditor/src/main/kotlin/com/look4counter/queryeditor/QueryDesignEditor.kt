package com.look4counter.queryeditor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.command.CommandProcessor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.vfs.newvfs.events.VFilePropertyChangeEvent
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.xml.XmlTag
import com.intellij.ui.OnePixelSplitter
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import com.look4counter.queryeditor.sql.SqlDocuments
import com.look4counter.queryeditor.database.QueryDataSourceToolbar
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.Component
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.beans.PropertyChangeListener
import java.beans.PropertyChangeSupport
import javax.swing.DefaultListModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JList
import javax.swing.BorderFactory
import javax.swing.JComponent
import javax.swing.ListSelectionModel
import javax.swing.event.DocumentEvent as SwingDocumentEvent
import javax.swing.event.DocumentListener as SwingDocumentListener

internal class QueryDesignEditor(private val project: Project, private val file: VirtualFile) : UserDataHolderBase(), FileEditor {
    private val model = QueryMapDocumentModel(project, file)
    private val changes = PropertyChangeSupport(this)
    private val queryModel = DefaultListModel<QueryEntry>()
    private val queryList = JBList(queryModel).apply {
        name = "query.list"
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        accessibleContext.accessibleName = "쿼리 id 목록"
        cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, selected: Boolean, focused: Boolean): Component {
                super.getListCellRendererComponent(list, value, index, selected, focused)
                border = BorderFactory.createCompoundBorder(border, JBUI.Borders.empty(0, 8))
                return this
            }
        }
    }
    private val mapDescription = JBTextField().apply {
        name = "queryMap.desc"
        accessibleContext.accessibleName = "queryMap desc"
    }
    private val status = JBLabel().apply { name = "query.status" }
    private val fields = listOf("id", "desc").associateWith { attribute ->
        JBTextField().apply { name = "query.$attribute" }
    }
    private val resultType = QueryResultTypeEditor(project, fields.getValue("id"))
    private val named = JBCheckBox().apply {
        name = "query.isNamed"
        accessibleContext.accessibleName = "isNamed"
        toolTipText = "isNamed"
    }
    private val emptyDocument = EditorFactory.getInstance().createDocument("")
    private val sqlEditor = QuerySqlEditor(project, emptyDocument)
    private val splitter = OnePixelSplitter(false, "glue.query.design.splitter", 0.70f)
    private val panel = JBPanel<JBPanel<*>>(BorderLayout())
    private val buffers = mutableListOf<SqlBuffer>()
    private val pending = mutableListOf<QueryEdit>()
    private var mapTarget: SmartPsiElementPointer<XmlTag>? = null
    private var originalMapDescription = ""
    private var selected: QueryEntry? = null
    private var updating = false
    private var applying = false
    private var scheduled = false
    private var disposed = false
    private var notice: String? = null

    private data class SqlBuffer(val target: SmartPsiElementPointer<XmlTag>, val document: Document, var original: String)

    init {
        sqlEditor.setDisposedWith(this)
        resultType.setDisposedWith(this)
        sqlEditor.minimumSize = JBUI.size(240, 100)
        val sidebar = createSidebar()
        splitter.name = "query.splitter"
        splitter.setHonorComponentsMinimumSize(true)
        splitter.firstComponent = sqlEditor
        splitter.secondComponent = sidebar
        Disposer.register(this) { splitter.dispose() }
        panel.preferredSize = JBUI.size(1066, 600)
        panel.add(QueryDataSourceToolbar(project, this), BorderLayout.NORTH)
        panel.add(splitter, BorderLayout.CENTER)
        panel.add(status, BorderLayout.SOUTH)

        queryList.addListSelectionListener { event ->
            if (!updating && !event.valueIsAdjusting) {
                val target = queryList.selectedValue?.target
                flushPending()
                refresh(target)
            }
        }
        fields.forEach { (attribute, input) ->
            input.document.addDocumentListener(object : SwingDocumentListener {
                override fun insertUpdate(event: SwingDocumentEvent) = changed()
                override fun removeUpdate(event: SwingDocumentEvent) = changed()
                override fun changedUpdate(event: SwingDocumentEvent) = changed()
                private fun changed() {
                    val query = selected ?: return
                    if (!updating && input.isEnabled) enqueue(QueryEdit(query.target, attribute, query.attribute(attribute), input.text))
                }
            })
        }
        resultType.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                val query = selected ?: return
                if (!updating && resultType.isEnabled) {
                    enqueue(QueryEdit(query.target, "resultType", query.resultType, event.document.text))
                }
            }
        }, this)
        mapDescription.document.addDocumentListener(object : SwingDocumentListener {
            override fun insertUpdate(event: SwingDocumentEvent) = changed()
            override fun removeUpdate(event: SwingDocumentEvent) = changed()
            override fun changedUpdate(event: SwingDocumentEvent) = changed()
            private fun changed() {
                val target = mapTarget ?: return
                if (!updating && mapDescription.isEnabled) {
                    enqueue(QueryEdit(target, "desc", originalMapDescription, mapDescription.text))
                }
            }
        })
        named.addActionListener {
            val query = selected
            if (!updating && named.isEnabled && query != null) {
                enqueue(QueryEdit(query.target, "isNamed", query.isNamed, named.isSelected.toString()))
            }
        }
        model.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (!applying) scheduleRefresh()
                changes.firePropertyChange(FileEditor.getPropModified(), null, isModified)
            }
        }, this)
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                if (events.any { it is VFilePropertyChangeEvent && it.file == file && it.propertyName == VirtualFile.PROP_WRITABLE }) {
                    scheduleRefresh()
                }
            }
        })
        scheduleRefresh()
    }

    private fun createSidebar(): JComponent {
        val form = JBPanel<JBPanel<*>>(GridBagLayout())
        val constraints = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = JBUI.insets(3)
            weightx = 1.0
        }
        val inputs: Map<String, JComponent> = fields + ("resultType" to resultType)
        val labels = mapOf("id" to "ID", "desc" to "Description", "resultType" to "ResultType")
        inputs.entries.forEachIndexed { row, (attribute, input) ->
            constraints.insets = JBUI.insets(3)
            constraints.gridy = row
            constraints.gridx = 0
            constraints.gridwidth = 1
            constraints.weightx = 0.0
            form.add(JBLabel(labels.getValue(attribute)).apply { labelFor = input }, constraints)
            constraints.gridx = 1
            constraints.weightx = 1.0
            constraints.gridwidth = if (attribute == "resultType") 1 else 2
            form.add(input, constraints)
        }
        constraints.insets = JBUI.insets(3)
        constraints.gridy = inputs.keys.indexOf("resultType")
        constraints.gridx = 2
        constraints.gridwidth = 1
        constraints.weightx = 0.0
        form.add(named, constraints)
        val header = JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(mapDescription, BorderLayout.NORTH)
            add(form, BorderLayout.CENTER)
        }
        return JBPanel<JBPanel<*>>(BorderLayout(0, JBUI.scale(8))).apply {
            name = "query.sidebar"
            border = JBUI.Borders.empty(8)
            preferredSize = JBUI.size(320, 400)
            minimumSize = JBUI.size(220, 100)
            add(header, BorderLayout.NORTH)
            add(JBScrollPane(queryList), BorderLayout.CENTER)
        }
    }

    private fun enqueue(edit: QueryEdit) {
        if (disposed || !model.writable) return
        val index = pending.indexOfFirst { it.target == edit.target && it.attribute == edit.attribute }
        if (index < 0) pending.add(edit) else pending[index] = edit.copy(original = pending[index].original)
        scheduleRefresh()
    }

    private fun scheduleRefresh() {
        if (disposed || scheduled) return
        scheduled = true
        ApplicationManager.getApplication().invokeLater({
            scheduled = false
            if (!disposed && !project.isDisposed) {
                flushPending()
                refresh()
            }
        }, ModalityState.nonModal())
    }

    private fun flushPending() {
        if (disposed || pending.isEmpty()) return
        val edits = pending.toList()
        pending.clear()
        applying = true
        try {
            if (!model.apply(edits)) notice = "수정 대상을 확인할 수 없습니다. Source의 최신 내용을 확인하세요."
        } finally {
            applying = false
        }
    }

    private fun refresh(preferred: SmartPsiElementPointer<XmlTag>? = selected?.target) {
        if (disposed || project.isDisposed) return
        PsiDocumentManager.getInstance(project).commitDocument(model.document)
        val snapshot = model.snapshot()
        val oldIndex = queryList.selectedIndex.coerceAtLeast(0)
        val target = preferred?.element
        updating = true
        try {
            mapTarget = snapshot.target
            originalMapDescription = snapshot.description
            if (mapDescription.text != snapshot.description) mapDescription.text = snapshot.description
            mapDescription.isEnabled = snapshot.target != null && snapshot.problem == null && model.writable
            mapDescription.toolTipText = snapshot.description
            queryModel.clear()
            snapshot.queries.forEach(queryModel::addElement)
            if (snapshot.queries.isNotEmpty()) {
                val matching = if (target == null) -1 else snapshot.queries.indexOfFirst { it.target.element == target }
                queryList.selectedIndex = if (matching >= 0) matching else oldIndex.coerceAtMost(snapshot.queries.lastIndex)
            }
            selected = queryList.selectedValue
            val query = selected
            val editable = query != null && snapshot.problem == null && model.writable
            fields.forEach { (attribute, input) ->
                val value = query?.attribute(attribute).orEmpty()
                if (input.text != value) input.text = value
                input.isEnabled = editable
            }
            val resultTypeValue = query?.resultType.orEmpty()
            if (resultType.text != resultTypeValue) resultType.text = resultTypeValue
            resultType.isEnabled = editable
            named.isSelected = query?.isNamed.toBoolean()
            named.isEnabled = editable
            if (query == null) {
                sqlEditor.document = emptyDocument
            } else {
                val buffer = buffers.firstOrNull { it.target.element != null && it.target.element == query.target.element }
                    ?: createBuffer(query).also(buffers::add)
                buffer.original = query.sql
                if (buffer.document.text != query.sql) {
                    CommandProcessor.getInstance().runUndoTransparentAction {
                        ApplicationManager.getApplication().runWriteAction { buffer.document.setText(query.sql) }
                    }
                }
                if (sqlEditor.document !== buffer.document) sqlEditor.document = buffer.document
            }
            sqlEditor.setViewer(!editable || !query.sqlEditable)
            status.text = snapshot.problem ?: notice ?: when {
                snapshot.queries.isEmpty() -> "queryMap에 query가 없습니다."
                !model.writable -> "읽기 전용 파일입니다."
                query?.sqlEditable == false -> "중첩 태그나 주석이 있는 SQL은 Source 탭에서 편집하세요."
                else -> ""
            }
            status.isVisible = status.text.isNotEmpty()
            notice = null
            buffers.removeAll { it.target.element == null }
        } finally {
            updating = false
        }
    }

    private fun createBuffer(query: QueryEntry): SqlBuffer {
        val buffer = SqlBuffer(query.target, SqlDocuments.create(project, query.sql), query.sql)
        buffer.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (updating || disposed) return
                val current = selected ?: return
                if (current.target.element == buffer.target.element && current.sqlEditable && model.writable) {
                    enqueue(QueryEdit(buffer.target, null, buffer.original, event.document.text))
                }
            }
        }, this)
        return buffer
    }

    override fun getComponent(): JComponent = panel
    override fun getPreferredFocusedComponent(): JComponent = sqlEditor
    override fun getName(): String = "Design"
    override fun getFile(): VirtualFile = file
    override fun setState(state: FileEditorState) = Unit
    override fun selectNotify() {
        scheduleRefresh()
    }
    override fun deselectNotify() = scheduleRefresh()
    override fun isModified(): Boolean = FileDocumentManager.getInstance().isDocumentUnsaved(model.document)
    override fun isValid(): Boolean = file.isValid
    override fun addPropertyChangeListener(listener: PropertyChangeListener) = changes.addPropertyChangeListener(listener)
    override fun removePropertyChangeListener(listener: PropertyChangeListener) = changes.removePropertyChangeListener(listener)
    override fun dispose() {
        disposed = true
        pending.clear()
        buffers.clear()
    }
}
