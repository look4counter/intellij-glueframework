package com.look4counter.queryeditor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import com.look4counter.queryeditor.database.DatabaseMetadataService
import com.look4counter.queryeditor.sql.SqlTrace
import com.look4counter.queryeditor.sql.SqlTraceEntry
import java.awt.BorderLayout
import java.awt.Component
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.DefaultListModel
import javax.swing.DefaultListCellRenderer
import javax.swing.BorderFactory
import javax.swing.JList
import javax.swing.JComponent
import javax.swing.KeyStroke
import javax.swing.ListSelectionModel

internal class QueryColumnTracePanel(private val project: Project, private val field: QuerySqlEditor) :
    JBPanel<QueryColumnTracePanel>(BorderLayout(0, JBUI.scale(4))), Disposable {
    private val rows = DefaultListModel<SqlTraceEntry>()
    private val list = JBList(rows).apply {
        name = "query.trace.list"
        accessibleContext.accessibleName = "컬럼 및 테이블 TRACE 경로"
        selectionMode = ListSelectionModel.SINGLE_SELECTION
        cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, selected: Boolean, focused: Boolean): Component {
                super.getListCellRendererComponent(list, value, index, selected, focused)
                border = BorderFactory.createCompoundBorder(border, JBUI.Borders.empty(0, 8))
                return this
            }
        }
        visibleRowCount = 6
        emptyText.text = "추적할 컬럼이나 테이블에 커서를 놓으세요."
        emptyText.setShowAboveCenter(false)
    }
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var generation = 0L
    private var editor: EditorEx? = null
    private var stamp = -1L
    private var navigating = false
    @Volatile private var disposed = false

    init {
        name = "query.trace"
        minimumSize = JBUI.size(160, 100)
        preferredSize = JBUI.size(280, 180)
        add(JBScrollPane(list), BorderLayout.CENTER)
        list.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(event: MouseEvent) {
                if (event.button != MouseEvent.BUTTON1) return
                val index = list.locationToIndex(event.point)
                if (index >= 0 && list.getCellBounds(index, index)?.contains(event.point) == true) navigate(index)
            }
        })
        list.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "trace.navigate")
        list.actionMap.put("trace.navigate", object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent?) = navigate(list.selectedIndex)
        })
        field.addSettingsProvider { attach(it) }
    }

    private fun attach(value: EditorEx) {
        editor = value
        val lifetime = Disposer.newDisposable("Glue SQL trace listeners")
        EditorUtil.disposeWithEditor(value, lifetime)
        value.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) {
                if (!navigating && editor === value) refresh()
            }
        }, lifetime)
        value.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) {
                if (editor === value) refresh()
            }
        }, lifetime)
        Disposer.register(lifetime) {
            if (editor === value) { editor = null; invalidateTrace() }
        }
        refresh()
    }

    private fun invalidateTrace() {
        ++generation
        alarm.cancelAllRequests()
        rows.clear()
        stamp = -1L
    }

    fun refresh() {
        if (disposed || project.isDisposed) return
        invalidateTrace()
        val revision = generation
        alarm.addRequest({
            val current = editor?.takeUnless { it.isDisposed } ?: return@addRequest
            if (disposed || project.isDisposed || revision != generation || current.document !== field.document) return@addRequest
            val document = current.document
            val documentStamp = document.modificationStamp
            val offset = current.caretModel.offset
            val text = document.text
            val metadata = DatabaseMetadataService.getInstance(project).snapshot()
            AppExecutorUtil.getAppExecutorService().execute {
                if (disposed || project.isDisposed) return@execute
                val result = SqlTrace.analyze(text, offset, metadata)
                ApplicationManager.getApplication().invokeLater({
                    if (disposed || project.isDisposed || revision != generation || editor !== current || current.isDisposed ||
                        field.document !== document || document.modificationStamp != documentStamp || current.caretModel.offset != offset) return@invokeLater
                    stamp = documentStamp
                    result.entries.forEach(rows::addElement)
                }, ModalityState.nonModal())
            }
        }, 150)
    }

    private fun navigate(index: Int) {
        val current = editor?.takeUnless { it.isDisposed } ?: return
        if (disposed || project.isDisposed || field.document !== current.document || current.document.modificationStamp != stamp) return
        val entry = if (index in 0 until rows.size()) rows.getElementAt(index) else return
        navigating = true
        try {
            current.selectionModel.removeSelection()
            current.caretModel.moveToOffset(entry.offset.coerceIn(0, current.document.textLength))
            current.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            current.contentComponent.requestFocusInWindow()
        } finally { navigating = false }
    }

    override fun dispose() {
        disposed = true
        invalidateTrace()
        editor = null
    }
}
