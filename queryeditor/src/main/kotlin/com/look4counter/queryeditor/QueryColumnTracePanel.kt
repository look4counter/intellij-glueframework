package com.look4counter.queryeditor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.ScrollType
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.ColoredTreeCellRenderer
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.treeStructure.Tree
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.intellij.util.ui.JBUI
import com.look4counter.queryeditor.database.DatabaseMetadataService
import com.look4counter.queryeditor.sql.SqlTrace
import com.look4counter.queryeditor.sql.SqlTraceNode
import com.look4counter.queryeditor.sql.SqlTraceResult
import java.awt.BorderLayout
import java.awt.event.ActionEvent
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.AbstractAction
import javax.swing.JComponent
import javax.swing.JTree
import javax.swing.KeyStroke
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel

internal class QueryColumnTracePanel(private val project: Project, private val field: QuerySqlEditor) :
    JBPanel<QueryColumnTracePanel>(BorderLayout(0, JBUI.scale(4))), Disposable {
    private class Item(val vertex: SqlTraceNode? = null, val marker: String? = null) :
        DefaultMutableTreeNode(vertex ?: marker ?: "TRACE") {
        override fun toString(): String = listOfNotNull(vertex?.name, vertex?.entry?.toString(), marker).joinToString("  ")
    }

    private data class ViewState(val document: Document, val stamp: Long, val nodes: List<SqlTraceNode>,
                                 val expanded: Set<List<String>>, val selected: List<String>?)

    private val root = Item()
    private val treeModel = DefaultTreeModel(root)
    private val tree = Tree(treeModel).apply {
        name = "query.trace.tree"
        accessibleContext.accessibleName = "컬럼 및 테이블 TRACE 경로"
        isRootVisible = false
        showsRootHandles = true
        selectionModel.selectionMode = TreeSelectionModel.SINGLE_TREE_SELECTION
        setLineStyleAngled()
        cellRenderer = object : ColoredTreeCellRenderer() {
            override fun customizeCellRenderer(tree: JTree, value: Any?, selected: Boolean, expanded: Boolean,
                                               leaf: Boolean, row: Int, hasFocus: Boolean) {
                border = JBUI.Borders.empty(0, 8)
                val item = value as? Item ?: return
                item.vertex?.let {
                    append(it.name, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                    append("  ${it.entry.line}: ${it.entry.text}", SimpleTextAttributes.GRAYED_ATTRIBUTES)
                }
                item.marker?.let { append("  $it", SimpleTextAttributes.GRAYED_ATTRIBUTES) }
            }
        }
        emptyText.text = "추적할 컬럼이나 테이블에 커서를 놓으세요."
        emptyText.setShowAboveCenter(false)
    }
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, this)
    private var generation = 0L
    private var editor: EditorEx? = null
    private var traceDocument: Document? = null
    private var displayedNodes: List<SqlTraceNode> = emptyList()
    private var stamp = -1L
    private var viewState: ViewState? = null
    private var navigating = false
    @Volatile private var disposed = false

    init {
        name = "query.trace"
        minimumSize = JBUI.size(160, 100)
        preferredSize = JBUI.size(280, 180)
        add(JBScrollPane(tree), BorderLayout.CENTER)
        tree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(event: MouseEvent) {
                if (event.button != MouseEvent.BUTTON1) return
                val path = tree.getPathForLocation(event.x, event.y) ?: return
                // The standard disclosure control is outside the node renderer bounds.
                if (tree.getPathBounds(path)?.contains(event.point) == true) navigate(path, false)
            }
        })
        tree.getInputMap(JComponent.WHEN_FOCUSED).put(KeyStroke.getKeyStroke("ENTER"), "trace.navigate")
        tree.actionMap.put("trace.navigate", object : AbstractAction() {
            override fun actionPerformed(event: ActionEvent?) = navigate(tree.selectionPath, true)
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

    private fun key(path: TreePath): List<String> = path.path.drop(1).mapNotNull {
        (it as? Item)?.vertex?.let { vertex -> "${vertex.entry.offset}:${vertex.name}" }
    }

    private fun invalidateTrace() {
        if (!disposed && traceDocument === field.document && stamp == field.document.modificationStamp && root.childCount > 0) {
            val expanded = mutableSetOf<List<String>>()
            val paths = tree.getExpandedDescendants(TreePath(root.path))
            while (paths?.hasMoreElements() == true) expanded.add(key(paths.nextElement()))
            viewState = ViewState(field.document, stamp, displayedNodes, expanded, tree.selectionPath?.let(::key))
        } else if (disposed || viewState?.document !== field.document || viewState?.stamp != field.document.modificationStamp) {
            viewState = null
        }
        ++generation
        alarm.cancelAllRequests()
        root.removeAllChildren()
        treeModel.reload()
        traceDocument = null
        displayedNodes = emptyList()
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
                    showTrace(result, document, documentStamp)
                }, ModalityState.nonModal())
            }
        }, 150)
    }

    private fun showTrace(result: SqlTraceResult, document: Document, documentStamp: Long) {
        val vertices = result.graph.nodes.associateBy { it.id }
        val items = mutableListOf<Item>()
        var count = 0
        var truncated = false
        fun marker(parent: Item, text: String) {
            if (count < 2048) { parent.add(Item(marker = text)); count++ }
            if (count >= 2048) truncated = true
        }
        fun add(id: Int, parent: Item, ancestors: Set<Int>, depth: Int) {
            if (truncated) return
            if (count >= 2047) { marker(parent, "표시 한도에 도달했습니다."); truncated = true; return }
            val vertex = vertices[id] ?: return
            val cycle = id in ancestors
            val item = Item(vertex, if (cycle) "(순환 참조)" else null)
            parent.add(item)
            items.add(item)
            count++
            if (cycle) return
            if (depth >= 96 && vertex.children.isNotEmpty()) { marker(item, "경로 깊이 한도에 도달했습니다."); return }
            vertex.children.forEach { add(it, item, ancestors + id, depth + 1) }
        }
        result.graph.roots.forEach { add(it, root, emptySet(), 0) }
        treeModel.reload()
        tree.expandPath(TreePath(root.path))
        val saved = viewState?.takeIf { it.document === document && it.stamp == documentStamp && it.nodes == result.graph.nodes }
        if (saved == null) {
            var row = 0
            while (row < tree.rowCount) tree.expandRow(row++)
        } else {
            items.filter { key(TreePath(it.path)) in saved.expanded }.forEach { tree.expandPath(TreePath(it.path)) }
        }
        val candidates = items.filter { item -> item.marker == null &&
            (if (result.graph.selectedNodeId != null) item.vertex?.id == result.graph.selectedNodeId
             else result.selectedOffset != null && item.vertex?.entry?.offset == result.selectedOffset) }
        val selected = candidates.firstOrNull { key(TreePath(it.path)) == saved?.selected } ?: candidates.firstOrNull()
        tree.selectionPath = selected?.let { TreePath(it.path) }
        tree.selectionPath?.let { tree.scrollPathToVisible(it) }
        traceDocument = document
        displayedNodes = result.graph.nodes
        stamp = documentStamp
    }

    private fun navigate(path: TreePath?, focusEditor: Boolean) {
        val current = editor?.takeUnless { it.isDisposed } ?: return
        if (disposed || project.isDisposed || field.document !== current.document || current.document.modificationStamp != stamp) return
        val entry = (path?.lastPathComponent as? Item)?.vertex?.entry ?: return
        navigating = true
        try {
            tree.selectionPath = path
            tree.scrollPathToVisible(path)
            current.selectionModel.removeSelection()
            current.caretModel.moveToOffset(entry.offset.coerceIn(0, current.document.textLength))
            current.scrollingModel.scrollToCaret(ScrollType.MAKE_VISIBLE)
            if (focusEditor) current.contentComponent.requestFocusInWindow()
        } finally { navigating = false }
    }

    override fun dispose() {
        disposed = true
        invalidateTrace()
        editor = null
    }
}
