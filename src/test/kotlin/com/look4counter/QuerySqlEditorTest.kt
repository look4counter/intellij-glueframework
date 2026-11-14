package com.look4counter

import com.intellij.codeInsight.daemon.impl.ParameterHintsPresentationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JList

class QuerySqlEditorTest : BasePlatformTestCase() {
    fun testNullLiteralHasNoErrorUnderlineWhileMissingColumnIsStillReported() {
        val sql = "WITH t AS (SELECT id FROM physical) SELECT NULL, NULL AS empty_value, missing FROM t WHERE id IS NULL"
        val field = openSql(sql)
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val before = source.text
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("NULL literal diagnostics", {
            columnErrors(field).size == 1
        }, 10)
        val error = columnErrors(field).single()
        assertEquals(sql.indexOf("missing"), error.startOffset)
        assertEquals("missing", field.text.substring(error.startOffset, error.endOffset))
        assertEquals(sql, field.text)
        assertEquals(before, source.text)
    }

    private fun assertNoTraceNavigationActions(field: EditorTextField) {
        val actions = com.intellij.openapi.actionSystem.ex.ActionUtil.getActions(field.editor!!.contentComponent)
        assertFalse(actions.any { it.templatePresentation.text in setOf("추적 뒤로", "추적 앞으로") })
        val navigationShortcuts = listOf(com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_BACK,
            com.intellij.openapi.actionSystem.IdeActions.ACTION_GOTO_FORWARD).flatMap {
            com.intellij.openapi.keymap.KeymapUtil.getActiveKeymapShortcuts(it).shortcuts.toList()
        }.toSet()
        assertFalse(actions.any { action -> action.shortcutSet.shortcuts.any { it in navigationShortcuts } })
    }

    fun testTraceDoesNotRegisterIdeNavigationShortcutsOrButtons() {
        val field = openSql("SELECT id FROM employee")
        assertFalse(descendants(design!!.component).filterIsInstance<javax.swing.JButton>()
            .any { it.name in setOf("query.trace.back", "query.trace.forward") })
        val panel = descendants(design!!.component).filterIsInstance<javax.swing.JPanel>().single { it.name == "query.trace" }
        assertNull((panel.layout as java.awt.BorderLayout).getLayoutComponent(java.awt.BorderLayout.NORTH))
        assertNoTraceNavigationActions(field)
    }

    fun testTableTraceHighlightsNamesAndBoundAliasesWithoutCrossingRenamedCte() {
        val sql = "WITH t AS (SELECT e.id FROM employee e) SELECT a.id FROM t a WHERE a.id=1"
        val field = openSql(sql, secondQuery = true)
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val before = source.text
        val expected = setOf(sql.indexOf("t AS"), sql.indexOf("FROM t") + 5, sql.indexOf("t a WHERE") + 2,
            sql.indexOf("a.id"), sql.lastIndexOf("a.id"))
        field.editor!!.caretModel.moveToOffset(sql.indexOf("a.id"))
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("table trace background", {
            traceHighlights(field).map { it.startOffset }.toSet() == expected
        }, 10)
        assertEquals(setOf(sql.indexOf("t AS"), sql.indexOf("FROM t") + 5), traceEntries().map { it.offset }.toSet())
        field.editor!!.caretModel.moveToOffset(sql.indexOf("employee"))
        val physical = setOf(sql.indexOf("employee"), sql.indexOf("employee e") + 9, sql.indexOf("e.id"))
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("physical table background", {
            traceHighlights(field).map { it.startOffset }.toSet() == physical
        }, 10)
        assertEquals(listOf(sql.indexOf("employee")), traceEntries().map { it.offset })
        descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }.selectedIndex = 0
        UIUtil.dispatchAllInvocationEvents()
        assertTrue(traceHighlights(field).isEmpty())
        assertEquals(before, source.text)
    }

    private fun clickSql(field: EditorTextField, offset: Int, control: Boolean = true) {
        val editor = field.editor!!
        val point = editor.offsetToXY(offset)
        editor.contentComponent.setSize(maxOf(editor.contentComponent.width, point.x + 100), maxOf(editor.contentComponent.height, point.y + 100))
        val event = java.awt.event.MouseEvent(editor.contentComponent, java.awt.event.MouseEvent.MOUSE_PRESSED,
            System.currentTimeMillis(), if (control) java.awt.event.InputEvent.CTRL_DOWN_MASK else 0,
            point.x + 1, point.y + editor.lineHeight / 2, 1, false, java.awt.event.MouseEvent.BUTTON1)
        val dispatcher = com.intellij.openapi.keymap.impl.IdeMouseEventDispatcher()
        com.intellij.openapi.keymap.KeymapManager.getInstance()
        com.intellij.openapi.actionSystem.ActionManager.getInstance()
        for (id in listOf(java.awt.event.MouseEvent.MOUSE_PRESSED, java.awt.event.MouseEvent.MOUSE_RELEASED, java.awt.event.MouseEvent.MOUSE_CLICKED)) {
            val mouse = java.awt.event.MouseEvent(event.component, id, event.`when`, event.modifiersEx, event.x, event.y, 1, false, event.button)
            if (!dispatcher.dispatchMouseEvent(mouse) && !mouse.isConsumed) {
                val listener = (editor as com.intellij.openapi.editor.impl.EditorImpl).mouseListener
                when (id) {
                    java.awt.event.MouseEvent.MOUSE_PRESSED -> listener.mousePressed(mouse)
                    java.awt.event.MouseEvent.MOUSE_RELEASED -> listener.mouseReleased(mouse)
                    else -> listener.mouseClicked(mouse)
                }
            }
        }
    }

    private fun waitForCaret(field: EditorTextField, offset: Int) {
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("trace navigation", {
            field.editor!!.caretModel.offset == offset
        }, 10)
    }

    private fun moveSqlMouse(field: EditorTextField, offset: Int, control: Boolean) {
        val editor = field.editor!!
        val point = editor.offsetToXY(offset)
        val event = java.awt.event.MouseEvent(editor.contentComponent, java.awt.event.MouseEvent.MOUSE_MOVED,
            System.currentTimeMillis(), if (control) java.awt.event.InputEvent.CTRL_DOWN_MASK else 0,
            point.x + 1, point.y + editor.lineHeight / 2, 0, false)
        editor.contentComponent.mouseMotionListeners.forEach { it.mouseMoved(event) }
    }

    private fun sourceUnderlines(field: EditorTextField) = field.editor!!.markupModel.allHighlighters.filter {
        it.textAttributesKey == com.intellij.openapi.editor.colors.CodeInsightColors.HYPERLINK_ATTRIBUTES
    }

    private fun traceHighlights(field: EditorTextField) = field.editor!!.markupModel.allHighlighters.filter {
        it.textAttributesKey?.externalName == "GLUE_SQL_TRACE_OCCURRENCE"
    }

    fun testControlHoverUnderlinesOnlyAvailableSourceAndClearsOnReleaseExitAndEdit() {
        val sql = "WITH a AS (SELECT id FROM physical) SELECT id FROM a"
        val field = openSql(sql)
        val origin = sql.lastIndexOf("id")
        moveSqlMouse(field, origin, true)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("source underline", { sourceUnderlines(field).size == 1 }, 10)
        assertEquals(origin, sourceUnderlines(field).single().startOffset)
        assertEquals(origin + 2, sourceUnderlines(field).single().endOffset)
        val release = java.awt.event.KeyEvent(field.editor!!.contentComponent, java.awt.event.KeyEvent.KEY_RELEASED,
            System.currentTimeMillis(), 0, java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.CHAR_UNDEFINED)
        field.editor!!.contentComponent.keyListeners.forEach { it.keyReleased(release) }
        assertTrue(sourceUnderlines(field).isEmpty())
        moveSqlMouse(field, origin, false)
        val press = java.awt.event.KeyEvent(field.editor!!.contentComponent, java.awt.event.KeyEvent.KEY_PRESSED,
            System.currentTimeMillis(), java.awt.event.InputEvent.CTRL_DOWN_MASK, java.awt.event.KeyEvent.VK_CONTROL, java.awt.event.KeyEvent.CHAR_UNDEFINED)
        field.editor!!.contentComponent.keyListeners.forEach { it.keyPressed(press) }
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("stationary source underline", { sourceUnderlines(field).size == 1 }, 10)
        val exit = java.awt.event.MouseEvent(field.editor!!.contentComponent, java.awt.event.MouseEvent.MOUSE_EXITED,
            System.currentTimeMillis(), 0, 0, 0, 0, false)
        field.editor!!.contentComponent.mouseListeners.forEach { it.mouseExited(exit) }
        assertTrue(sourceUnderlines(field).isEmpty())
        moveSqlMouse(field, origin, true)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("source underline before edit", { sourceUnderlines(field).size == 1 }, 10)
        WriteCommandAction.runWriteCommandAction(project) { field.document.insertString(0, " ") }
        assertTrue(sourceUnderlines(field).isEmpty())
        moveSqlMouse(field, field.text.indexOf("physical"), true)
        clickSql(field, field.text.indexOf("physical"))
        UIUtil.dispatchAllInvocationEvents()
        assertNoTraceNavigationActions(field)
    }

    fun testTracedColumnBackgroundIsReplacedByTableBackgroundAndClearsOnQueryChange() {
        val sql = "WITH a AS (SELECT id FROM first_table), b AS (SELECT id FROM second_table) SELECT a.id FROM a JOIN b ON a.id=b.id WHERE a.id=1 AND b.id=2"
        val field = openSql(sql, secondQuery = true)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("a.id") + 2)
        val expected = setOf(sql.indexOf("id FROM first_table"), sql.indexOf("a.id") + 2, sql.indexOf("ON a.id") + 5, sql.indexOf("WHERE a.id") + 8)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("trace gray background", {
            traceHighlights(field).map { it.startOffset }.toSet() == expected
        }, 10)
        assertTrue(traceHighlights(field).all { it.textAttributes?.backgroundColor != null && field.text.substring(it.startOffset, it.endOffset) == "id" })
        field.editor!!.caretModel.moveToOffset(sql.indexOf("first_table"))
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("table background replaces columns", {
            traceHighlights(field).map { it.startOffset } == listOf(sql.indexOf("first_table"))
        }, 10)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("a.id") + 2)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("trace background restored", { traceHighlights(field).size == 4 }, 10)
        descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }.selectedIndex = 0
        UIUtil.dispatchAllInvocationEvents()
        assertTrue(traceHighlights(field).isEmpty())
    }

    fun testControlClickUsesClickedColumnWithoutRegisteringNavigationActions() {
        val sql = "WITH a AS (SELECT id FROM physical), b AS (SELECT id FROM a) SELECT id FROM b"
        val field = openSql(sql)
        var sourceActions = 0
        var nativeDeclarations = 0
        val native = com.intellij.openapi.actionSystem.ActionManager.getInstance().getAction("GotoDeclaration")
        com.intellij.openapi.application.ApplicationManager.getApplication().messageBus.connect(testRootDisposable)
            .subscribe(com.intellij.openapi.actionSystem.ex.AnActionListener.TOPIC, object : com.intellij.openapi.actionSystem.ex.AnActionListener {
                override fun beforeActionPerformed(action: com.intellij.openapi.actionSystem.AnAction, event: com.intellij.openapi.actionSystem.AnActionEvent) {
                    if (action.templatePresentation.text == "SQL 출처 이동") sourceActions++
                    if (action === native) nativeDeclarations++
                }
            })
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val before = source.text
        val origin = sql.lastIndexOf("id")
        val middle = sql.indexOf("id FROM a")
        val first = sql.indexOf("id FROM physical")
        assertNoTraceNavigationActions(field)
        field.editor!!.caretModel.moveToOffset(0)
        clickSql(field, origin)
        waitForCaret(field, middle)
        clickSql(field, middle)
        waitForCaret(field, first)
        assertNoTraceNavigationActions(field)
        assertEquals(sql, field.text)
        assertEquals(before, source.text)
        assertEquals(2, sourceActions)
        assertEquals(0, nativeDeclarations)
        WriteCommandAction.runWriteCommandAction(project) { field.document.insertString(0, " ") }
        assertNoTraceNavigationActions(field)
    }

    fun testControlClickTableQualifierAndQuerySwitchDoNotRegisterNavigationActions() {
        val sql = "WITH t AS (SELECT id FROM physical) SELECT a.id FROM t a"
        val field = openSql(sql, secondQuery = true)
        val origin = sql.indexOf("a.id")
        val declaration = sql.lastIndexOf("t a")
        clickSql(field, origin)
        waitForCaret(field, declaration)
        clickSql(field, declaration)
        waitForCaret(field, sql.indexOf("t AS"))
        assertNoTraceNavigationActions(field)
        descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }.selectedIndex = 0
        UIUtil.dispatchAllInvocationEvents()
        assertNoTraceNavigationActions(field)
        descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }.selectedIndex = 1
        UIUtil.dispatchAllInvocationEvents()
        assertNoTraceNavigationActions(field)
    }

    fun testOrdinarySqlClickDoesNotNavigateAndTreeNavigationStillWorks() {
        val sql = "WITH t AS (\n SELECT id FROM physical\n)\nSELECT id FROM t"
        val field = openSql(sql)
        val origin = sql.lastIndexOf("id")
        clickSql(field, origin, false)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(origin, field.editor!!.caretModel.offset)
        assertNoTraceNavigationActions(field)
        waitForTrace(listOf(2, 4))
        val tree = traceTree()
        tree.selectionPath = pathAt(sql.indexOf("id"))
        tree.actionMap.get("trace.navigate").actionPerformed(null)
        assertEquals(sql.indexOf("id"), field.editor!!.caretModel.offset)
        assertNoTraceNavigationActions(field)
    }

    fun testControlClickWithMultipleInputsLetsUserChooseSource() {
        val sql = "WITH a AS (SELECT x FROM first_table), b AS (SELECT x FROM second_table) SELECT a.x+b.x AS x FROM a JOIN b ON 1=1"
        val field = openSql(sql)
        var captured: com.intellij.openapi.ui.popup.JBPopup? = null
        com.intellij.ui.UiInterceptors.registerPossible(testRootDisposable,
            object : com.intellij.ui.UiInterceptors.UiInterceptor<com.intellij.openapi.ui.popup.JBPopup>(com.intellij.openapi.ui.popup.JBPopup::class.java) {
                override fun doIntercept(popup: com.intellij.openapi.ui.popup.JBPopup) { captured = popup }
            })
        clickSql(field, sql.indexOf("x FROM a JOIN"))
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("source chooser", {
            captured != null
        }, 10)
        val popup = captured!!
        try {
            val list = descendants(popup.content).filterIsInstance<JList<*>>().single()
            assertEquals(2, list.model.size)
            list.selectedIndex = 1
            popup.closeOk(null)
            waitForCaret(field, sql.indexOf("x FROM second_table"))
            assertNoTraceNavigationActions(field)
            assertEquals(sql, field.text)
        } finally { if (!popup.isDisposed) popup.cancel() }
    }

    private fun columnErrors(field: EditorTextField) = field.editor!!.markupModel.allHighlighters.filter {
        it.textAttributesKey == com.intellij.openapi.editor.colors.CodeInsightColors.ERRORS_ATTRIBUTES
    }

    fun testKnownColumnErrorsUpdateWithoutChangingSqlAndClearOnQuerySwitch() {
        val sql = "WITH t AS (SELECT id FROM physical) SELECT t.missing FROM t WHERE t.bad=1"
        val field = openSql(sql, secondQuery = true)
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val sourceBefore = source.text
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("column errors", { columnErrors(field).size == 2 }, 10)
        assertEquals(listOf("missing", "bad"), columnErrors(field).sortedBy { it.startOffset }.map { field.text.substring(it.startOffset, it.endOffset) })
        assertTrue(columnErrors(field).all { it.errorStripeTooltip.toString().contains("컬럼이 없습니다") })
        assertTrue(columnErrors(field).all {
            it.getTextAttributes(field.editor!!.colorsScheme)?.effectType == com.intellij.openapi.editor.markup.EffectType.WAVE_UNDERSCORE
        })
        assertEquals(sql, field.text)
        assertEquals(sourceBefore, source.text)
        val fixed = sql.replace("missing", "id").replace("bad", "id")
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText(fixed) }
        assertTrue(columnErrors(field).isEmpty())
        UIUtil.dispatchAllInvocationEvents()
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText(sql) }
        UIUtil.dispatchAllInvocationEvents()
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("restored errors", { columnErrors(field).size == 2 }, 10)
        val queries = descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }
        queries.selectedIndex = 0
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("select 100", field.text)
        assertTrue(columnErrors(field).isEmpty())
    }

    fun testTraceUsesOneCenteredPromptWithoutFooter() {
        val sql = "SELECT id\nFROM (\n SELECT id FROM physical\n) q\nWHERE missing=1"
        val field = openSql(sql)
        val list = traceTree()
        val prompt = "추적할 컬럼이나 테이블에 커서를 놓으세요."
        assertEquals(prompt, list.emptyText.text)
        assertFalse(list.emptyText.isShowAboveCenter)
        assertTrue(descendants(design!!.component).none { it.name == "query.trace.status" })
        field.editor!!.caretModel.moveToOffset(sql.indexOf("id") + 1)
        waitForTrace(listOf(3, 1))
        assertEquals(prompt, list.emptyText.text)
        val missing = sql.indexOf("missing") + 1
        assertNotNull(com.look4counter.queryeditor.sql.SqlColumnTrace.analyze(sql, missing, null).message)
        field.editor!!.caretModel.moveToOffset(missing)
        assertEquals(prompt, list.emptyText.text)
        assertNull(list.toolTipText)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("id") + 1)
        waitForTrace(listOf(3, 1))
        assertEquals(prompt, list.emptyText.text)
        assertFalse(list.emptyText.isShowAboveCenter)
    }

    fun testTraceShowsOnlyOutputStagesAlsoFromConditionCaret() {
        val sql = "WITH t AS (\n SELECT id FROM physical\n)\nSELECT id\nFROM t\nWHERE id=1\nGROUP BY id\nHAVING id>0\nORDER BY id"
        val field = openSql(sql)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("SELECT id\n") + 8)
        waitForTrace(listOf(2, 4))
        assertEquals(1, traceTree().leadSelectionRow)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("WHERE id") + 7)
        waitForTrace(listOf(2, 4))
        assertEquals(-1, traceTree().leadSelectionRow)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("SELECT id FROM") + 8)
        waitForTrace(listOf(2, 4))
        assertEquals(0, traceTree().leadSelectionRow)
        assertEquals(sql, field.text)
    }

    fun testInlineSubqueryFormattingAlignsFromWithoutMovingLines() {
        val sql = "SELECT HELLO AS B\n     , (SELECT *\n             FROM DUAL) AS C\n  FROM FAVORITE"
        val field = openSql(sql)
        invokeFormatAction(formatAction(field))
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("SELECT HELLO AS B\n     , (SELECT *\n          FROM DUAL) AS C\n  FROM FAVORITE", field.text)
    }

    fun testSavedAlignmentOptionsFormatWholeQueryWithUndoRedo() {
        val settings = com.look4counter.queryeditor.settings.QueryFormattingSettings.getInstance(project)
        val originalSettings = settings.state
        val sql = "select AB as A\n,LONG_NAME as B\nfrom T\nwhere AB=AB\nand HELO=HI"
        val plain = "select AB as A\n     , LONG_NAME as B\n  from T\n where AB = AB\n   and HELO = HI"
        val aligned = plain.replace("AB as A", "AB        as A").replace("where AB =", "where AB   =")
        try {
            val field = openSql(sql)
            settings.loadState(com.look4counter.queryeditor.settings.QueryFormattingState(true, true))
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(sql, field.text)
            invokeFormatAction(formatAction(field))
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(aligned, field.text)
            val source = FileEditorProviderManager.getInstance().getProviderList(project, design!!.file!!)
                .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design!!.file!!) as TextEditor
            try {
                UndoManager.getInstance(project).undo(source)
                UIUtil.dispatchAllInvocationEvents()
                assertEquals(sql, field.text)
                UndoManager.getInstance(project).redo(source)
                UIUtil.dispatchAllInvocationEvents()
                assertEquals(aligned, field.text)
            } finally { Disposer.dispose(source) }
            settings.loadState(com.look4counter.queryeditor.settings.QueryFormattingState())
            invokeFormatAction(formatAction(field))
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(plain, field.text)
        } finally { settings.loadState(originalSettings) }
    }

    fun testSavedAlignmentOptionsAffectOnlySelectedSql() {
        val settings = com.look4counter.queryeditor.settings.QueryFormattingSettings.getInstance(project)
        val originalSettings = settings.state
        val part = "select A as X\n,LONG_NAME as Y\nfrom T\nwhere A=1\nand LONG_NAME=2"
        val sql = "-- unchanged AS =  \n$part\n;select RAW as untouched from U where RAW=3"
        try {
            settings.loadState(com.look4counter.queryeditor.settings.QueryFormattingState(true, true))
            val field = openSql(sql)
            val start = sql.indexOf(part)
            field.editor!!.selectionModel.setSelection(start, start + part.length)
            invokeFormatAction(formatAction(field))
            UIUtil.dispatchAllInvocationEvents()
            val expected = "select A         as X\n     , LONG_NAME as Y\n  from T\n where A         = 1\n   and LONG_NAME = 2"
            assertEquals(sql.replace(part, expected), field.text)
        } finally { settings.loadState(originalSettings) }
    }

    private fun traceTree(): com.intellij.ui.treeStructure.Tree = descendants(design!!.component).filterIsInstance<com.intellij.ui.treeStructure.Tree>()
        .single { it.name == "query.trace.tree" }

    private fun traceNodes(): List<javax.swing.tree.DefaultMutableTreeNode> = java.util.Collections.list(
        (traceTree().model.root as javax.swing.tree.DefaultMutableTreeNode).preorderEnumeration())
        .filterIsInstance<javax.swing.tree.DefaultMutableTreeNode>().drop(1)

    private fun vertex(node: Any?): com.look4counter.queryeditor.sql.SqlTraceNode? =
        (node as? javax.swing.tree.DefaultMutableTreeNode)?.userObject as? com.look4counter.queryeditor.sql.SqlTraceNode

    private fun traceEntries() = traceNodes().mapNotNull { vertex(it)?.entry }

    private fun pathAt(offset: Int) = javax.swing.tree.TreePath(traceNodes().first { vertex(it)?.entry?.offset == offset }.path)

    private fun waitForTrace(lines: List<Int>) {
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("column trace", {
            val list = traceTree()
            traceEntries().map { it.line } == lines
        }, 10)
    }

    fun testColumnTraceClickAndEnterNavigateWithoutChangingSql() {
        val sql = "WITH TB_MAST AS (\n    SELECT SS\n      FROM DUAL\n)\n, TB_SUB AS (\n    SELECT *\n      FROM TB_MAST\n)\nSELECT SS\n  FROM TB_SUB A"
        val field = openSql(sql)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("SS") + 1)
        waitForTrace(listOf(2, 6, 9))
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val sourceBefore = source.text
        val list = traceTree()
        assertTrue(descendants(design!!.component).filterIsInstance<javax.swing.JLabel>().none { it.text == "TRACE" })
        assertEquals(listOf("2: SELECT SS", "6: SELECT *", "9: SELECT SS"),
            traceEntries().map { it.toString() })
        list.setSize(300, 160)
        val bounds = list.getRowBounds(0)
        val click = java.awt.event.MouseEvent(list, java.awt.event.MouseEvent.MOUSE_PRESSED,
            System.currentTimeMillis(), 0, bounds.x + 4, bounds.y + 4, 1, false, java.awt.event.MouseEvent.BUTTON1)
        list.mouseListeners.forEach { it.mousePressed(click) }
        assertEquals(sql.indexOf("SS"), field.editor!!.caretModel.offset)
        assertEquals(0, list.leadSelectionRow)
        assertEquals(3, list.rowCount)
        list.setSelectionRow(1)
        list.actionMap.get("trace.navigate").actionPerformed(null)
        assertEquals(sql.indexOf("*"), field.editor!!.caretModel.offset)
        assertEquals(1, list.leadSelectionRow)
        assertEquals(sql, field.text)
        assertEquals(sourceBefore, source.text)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("DUAL"))
        waitForTrace(listOf(3))
    }

    fun testTableTraceNavigatesSwitchesToColumnsAndClearsAfterQuerySwitch() {
        val sql = "WITH t AS (\n SELECT id FROM physical\n)\nSELECT a.id\nFROM t a"
        val field = openSql(sql, secondQuery = true)
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val sourceBefore = source.text
        field.editor!!.caretModel.moveToOffset(sql.indexOf("a.id") + 1)
        waitForTrace(listOf(1, 5))
        val list = traceTree()
        assertEquals(1, list.leadSelectionRow)
        list.setSize(300, 160)
        val bounds = list.getRowBounds(0)
        val click = java.awt.event.MouseEvent(list, java.awt.event.MouseEvent.MOUSE_PRESSED,
            System.currentTimeMillis(), 0, bounds.x + 4, bounds.y + 4, 1, false, java.awt.event.MouseEvent.BUTTON1)
        list.mouseListeners.forEach { it.mousePressed(click) }
        assertEquals(sql.indexOf("t AS"), field.editor!!.caretModel.offset)
        assertEquals(0, list.leadSelectionRow)
        assertEquals(2, list.rowCount)
        list.setSelectionRow(1)
        list.actionMap.get("trace.navigate").actionPerformed(null)
        assertEquals(sql.lastIndexOf("t a"), field.editor!!.caretModel.offset)
        assertEquals(1, list.leadSelectionRow)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("a.id") + 3)
        waitForTrace(listOf(2, 4))
        assertEquals(1, list.leadSelectionRow)
        field.editor!!.caretModel.moveToOffset(sql.lastIndexOf('a') + 1)
        waitForTrace(listOf(1, 5))
        assertEquals(1, list.leadSelectionRow)
        assertEquals(sql, field.text)
        assertEquals(sourceBefore, source.text)
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText("SELECT * FROM replacement") }
        assertEquals(0, list.rowCount)
        field.editor!!.caretModel.moveToOffset(field.text.length)
        waitForTrace(listOf(1))
        val queries = descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }
        queries.selectedIndex = 0
        UIUtil.dispatchAllInvocationEvents()
        waitForTrace(emptyList())
        assertEquals("select 100", field.text)
    }

    fun testColumnTraceInvalidatesAfterEditingAndSwitchingQueries() {
        val sql = "WITH t AS (\n SELECT SS FROM DUAL\n)\nSELECT SS FROM t"
        val field = openSql(sql, secondQuery = true)
        field.editor!!.caretModel.moveToOffset(sql.lastIndexOf("SS") + 1)
        waitForTrace(listOf(2, 4))
        val list = traceTree()
        list.setSelectionRow(0)
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText("SELECT AA FROM DUAL") }
        assertEquals(0, list.rowCount)
        list.actionMap.get("trace.navigate").actionPerformed(null)
        UIUtil.dispatchAllInvocationEvents()
        field.editor!!.caretModel.moveToOffset("SELECT A".length)
        waitForTrace(listOf(1))
        val queries = descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }
        queries.selectedIndex = 0
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("select 100", field.text)
        waitForTrace(emptyList())
    }

    fun testTracePressWithSmallMouseMotionNavigatesWithoutClickAndIgnoresOtherTargets() {
        val sql = "WITH t AS (\n SELECT id FROM physical\n)\nSELECT id FROM t"
        val field = openSql(sql)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("id") + 1)
        waitForTrace(listOf(2, 4))
        val list = traceTree()
        list.setSize(300, 160)
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val before = source.text
        val cell = list.getRowBounds(1)
        val entry = traceEntries()[1]
        fun event(id: Int, x: Int, y: Int, button: Int) = java.awt.event.MouseEvent(list, id,
            System.currentTimeMillis(), if (id == java.awt.event.MouseEvent.MOUSE_DRAGGED) java.awt.event.InputEvent.BUTTON1_DOWN_MASK else 0,
            x, y, 1, false, button)
        val press = event(java.awt.event.MouseEvent.MOUSE_PRESSED, cell.x + 4, cell.y + 4, java.awt.event.MouseEvent.BUTTON1)
        list.mouseListeners.forEach { it.mousePressed(press) }
        val drag = event(java.awt.event.MouseEvent.MOUSE_DRAGGED, cell.x + 5, cell.y + 5, java.awt.event.MouseEvent.NOBUTTON)
        list.mouseMotionListeners.forEach { it.mouseDragged(drag) }
        val release = event(java.awt.event.MouseEvent.MOUSE_RELEASED, cell.x + 5, cell.y + 5, java.awt.event.MouseEvent.BUTTON1)
        list.mouseListeners.forEach { it.mouseReleased(release) }
        // mouseClicked를 보내지 않아도 누른 항목으로 이동한다.
        assertEquals(entry.offset, field.editor!!.caretModel.offset)
        assertEquals(1, list.leadSelectionRow)
        val first = list.getRowBounds(0)
        val right = event(java.awt.event.MouseEvent.MOUSE_PRESSED, first.x + 4, first.y + 4, java.awt.event.MouseEvent.BUTTON3)
        list.mouseListeners.forEach { it.mousePressed(right) }
        assertEquals(entry.offset, field.editor!!.caretModel.offset)
        val blank = event(java.awt.event.MouseEvent.MOUSE_PRESSED, 4, 150, java.awt.event.MouseEvent.BUTTON1)
        list.mouseListeners.forEach { it.mousePressed(blank) }
        assertEquals(entry.offset, field.editor!!.caretModel.offset)
        assertEquals(sql, field.text)
        assertEquals(before, source.text)
    }

    fun testStandardTraceTreePreservesCollapsedBranchesAndUsesNativeControls() {
        val sql = "WITH a AS (\n SELECT * FROM origin\n), b AS (\n SELECT * FROM a\n), c AS (\n SELECT * FROM a\n)\nSELECT *\nFROM b JOIN c ON 1=1"
        val field = openSql(sql)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("a AS"))
        val lines = listOf(1, 4, 6)
        waitForTrace(lines)
        val tree = traceTree()
        assertFalse(tree.isRootVisible)
        assertTrue(tree.showsRootHandles)
        tree.setSize(500, 400)
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val before = source.text
        val branchOffset = sql.indexOf("a AS")
        tree.collapsePath(pathAt(branchOffset))
        field.editor!!.caretModel.moveToOffset(sql.indexOf("a AS") + 1)
        waitForTrace(lines)
        val branch = pathAt(branchOffset)
        assertFalse(tree.isExpanded(branch))
        val bounds = tree.getPathBounds(branch)!!
        val caret = field.editor!!.caretModel.offset
        val arrow = java.awt.event.MouseEvent(tree, java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
            java.awt.event.InputEvent.BUTTON1_DOWN_MASK, bounds.x - com.intellij.util.ui.JBUI.scale(8),
            bounds.y + bounds.height / 2, 1, false, java.awt.event.MouseEvent.BUTTON1)
        tree.mouseListeners.forEach { it.mousePressed(arrow) }
        assertTrue(tree.isExpanded(branch))
        assertEquals(caret, field.editor!!.caretModel.offset)
        tree.selectionPath = branch
        tree.actionMap.get("selectParent").actionPerformed(java.awt.event.ActionEvent(tree, 0, "left"))
        assertFalse(tree.isExpanded(branch))
        tree.actionMap.get("selectChild").actionPerformed(java.awt.event.ActionEvent(tree, 0, "right"))
        assertTrue(tree.isExpanded(branch))
        assertEquals(caret, field.editor!!.caretModel.offset)
        tree.collapsePath(branch)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("FROM a") + 6)
        waitForTrace(lines)
        assertTrue(tree.isExpanded(pathAt(branchOffset)))
        assertEquals(sql.indexOf("FROM a") + 5, vertex(tree.selectionPath!!.lastPathComponent)!!.entry.offset)
        assertEquals(sql, field.text)
        assertEquals(before, source.text)
    }

    fun testTraceTreeShowsSharedConsumerOnEachPathAndKeepsSelectedOccurrence() {
        val sql = "WITH a AS (\n SELECT id FROM first_table\n), b AS (\n SELECT id FROM second_table\n)\nSELECT a.id+b.id AS id FROM a JOIN b ON 1=1"
        val field = openSql(sql)
        val joined = sql.indexOf("AS id") + 3
        field.editor!!.caretModel.moveToOffset(joined + 1)
        waitForTrace(listOf(2, 6, 4, 6))
        val totals = traceNodes().filter { vertex(it)?.entry?.offset == joined }
        assertEquals(2, totals.size)
        assertEquals(setOf(sql.indexOf("id FROM first_table"), sql.indexOf("id FROM second_table")), totals.map { vertex(it.parent)!!.entry.offset }.toSet())
        val tree = traceTree()
        tree.setSize(500, 240)
        val secondPath = javax.swing.tree.TreePath(totals.last().path)
        val bounds = tree.getPathBounds(secondPath)!!
        val press = java.awt.event.MouseEvent(tree, java.awt.event.MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
            0, bounds.x + 4, bounds.y + 4, 1, false, java.awt.event.MouseEvent.BUTTON1)
        tree.mouseListeners.forEach { it.mousePressed(press) }
        assertEquals(secondPath, tree.selectionPath)
        assertEquals(joined, field.editor!!.caretModel.offset)
        field.editor!!.caretModel.moveToOffset(joined + 2)
        waitForTrace(listOf(2, 6, 4, 6))
        assertEquals(sql.indexOf("id FROM second_table"), vertex((tree.selectionPath!!.lastPathComponent as javax.swing.tree.DefaultMutableTreeNode).parent)!!.entry.offset)
        assertEquals(sql, field.text)
    }

    fun testTraceTreeStartsExpandedWhenChangingToADifferentGraphInSameDocument() {
        val sql = "WITH a AS (SELECT id FROM pa), b AS (SELECT id FROM pb) SELECT a.id FROM a JOIN b ON a.id=b.id"
        val field = openSql(sql)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("pa") + 1)
        waitForTrace(listOf(1))
        val tree = traceTree()
        tree.collapsePath(pathAt(sql.indexOf("pa")))
        assertEquals(1, tree.rowCount)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("pb") + 1)
        waitForTrace(listOf(1))
        assertEquals(1, tree.rowCount)
        assertEquals("테이블 pb", vertex(tree.getPathForRow(0).lastPathComponent)!!.name)
        assertEquals(sql, field.text)
    }

    fun testTraceTreeStopsRecursiveBranchesAndBoundsManySameNameReferences() {
        val sql = "WITH RECURSIVE t(id) AS (SELECT id FROM t) SELECT id FROM t"
        val field = openSql(sql)
        field.editor!!.caretModel.moveToOffset(sql.indexOf("t(id)") + 1)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("recursive trace tree", {
            traceNodes().any { it.toString().contains("순환 참조") }
        }, 10)
        assertTrue(traceNodes().size < 12)
        val branches = (1..1050).joinToString(" UNION ALL ") { "SELECT id FROM t" }
        val wide = "WITH RECURSIVE t(id) AS ($branches) SELECT id FROM t"
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText(wide) }
        UIUtil.dispatchAllInvocationEvents()
        field.editor!!.caretModel.moveToOffset(wide.indexOf("t(id)") + 1)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("bounded trace tree", {
            traceNodes().any { it.toString().contains("표시 한도") }
        }, 10)
        assertTrue(traceNodes().size <= 2048)
        assertEquals(wide, field.text)
    }

    fun testSecondProjectionCompletesAdjacentDerivedAlias() {
        val sql = """
            SELECT A.DEPT
                 , B.
              FROM (
                       SELECT DEPT
                            , JOB
                            , SALARY
                         FROM employee
                   ) A
                   (
                       SELECT DEPT2
                            , JOB
                            , SALARY
                         FROM employee
                   ) B
        """.trimIndent()
        val field = openSql(sql)
        configureCompletion(field, sql.indexOf("B.") + 2)
        val choices = myFixture.completeBasic()!!
        assertEquals(setOf("DEPT2", "JOB", "SALARY"), choices.map { it.lookupString }.toSet())
        myFixture.lookup.currentItem = choices.single { it.lookupString == "DEPT2" }
        myFixture.finishLookup('\n')
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(sql.replace("B.", "B.DEPT2"), field.text)
    }

    fun testAdjacentDerivedAliasOffersOnlyItsOwnOutputWithoutMetadata() {
        val tail = "\n  FROM (\n           SELECT DEPT, JOB, SALARY FROM employee\n       ) A\n       (\n           SELECT DEPT2, JOB, SALARY FROM employee\n       ) B"
        val field = openSql("SELECT A.$tail")
        configureCompletion(field, "SELECT A.".length)
        val choices = myFixture.completeBasic()!!
        assertEquals(setOf("DEPT", "JOB", "SALARY"), choices.map { it.lookupString }.toSet())
        myFixture.lookup.currentItem = choices.single { it.lookupString == "DEPT" }
        myFixture.finishLookup('\n')
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("SELECT A.DEPT$tail", field.text)
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText("SELECT B.$tail") }
        UIUtil.dispatchAllInvocationEvents()
        configureCompletion(field, "SELECT B.".length)
        assertEquals(setOf("DEPT2", "JOB", "SALARY"), myFixture.completeBasic()!!.map { it.lookupString }.toSet())
    }
    fun testFormatOnlySelectionPreservesSurroundingSqlAndUndoRedo() {
        val part = "select A,B from EMP"
        val original = "-- untouched  \n" + part + "\n; select X,Y from OTHER"
        val field = openSql(original)
        val start = original.indexOf(part)
        field.editor!!.selectionModel.setSelection(start, start + part.length)
        invokeFormatAction(formatAction(field))
        UIUtil.dispatchAllInvocationEvents()
        val expected = original.replace(part, "select A, B from EMP")
        assertEquals(expected, field.text)
        val source = FileEditorProviderManager.getInstance().getProviderList(project, design!!.file!!)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design!!.file!!) as TextEditor
        try {
            UndoManager.getInstance(project).undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(original, field.text)
            UndoManager.getInstance(project).redo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(expected, field.text)
        } finally { Disposer.dispose(source) }
    }
    private fun formatButton(): javax.swing.JButton {
        assertTrue(descendants(design!!.component).none { it.name == "query.sql.format" })
        val field = descendants(design!!.component).filterIsInstance<EditorTextField>().single { it.name == "query.sql" }
        val action = formatAction(field)
        val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(
            action, null, "test", com.intellij.openapi.actionSystem.DataContext.EMPTY_CONTEXT)
        action.update(event)
        return javax.swing.JButton().apply {
            isEnabled = event.presentation.isEnabled
            addActionListener { invokeFormatAction(action) }
        }
    }
    private fun formatAction(field: EditorTextField): com.intellij.openapi.actionSystem.AnAction {
        val actions = field.getClientProperty(com.intellij.openapi.actionSystem.AnAction.ACTIONS_KEY) as? List<*>
        assertNotNull(actions)
        return actions!!.filterIsInstance<com.intellij.openapi.actionSystem.AnAction>().single { it.templatePresentation.text == "SQL 포맷" }
    }

    private fun invokeFormatAction(action: com.intellij.openapi.actionSystem.AnAction) {
        action.actionPerformed(com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(
            action, null, "test", com.intellij.openapi.actionSystem.DataContext.EMPTY_CONTEXT))
    }

    private fun withoutWriteIntent(action: () -> Unit) {
        val application = com.intellij.openapi.application.ApplicationManager.getApplication()
            as com.intellij.openapi.application.impl.ApplicationImpl
        application.threadingSupport.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack(action)
    }

    fun testFormatShortcutUsesIdeKeymapAndWorksFromUnlockedContext() {
        val field = openSql("select A,B from EMP")
        val action = formatAction(field)
        val standard = com.intellij.openapi.actionSystem.ActionManager.getInstance()
            .getAction(com.intellij.openapi.actionSystem.IdeActions.ACTION_EDITOR_REFORMAT)
        assertEquals(standard.shortcutSet.shortcuts.toList(), action.shortcutSet.shortcuts.toList())
        withoutWriteIntent { invokeFormatAction(action) }
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("select A, B from EMP", field.text)
    }

    fun testFormatRejectsInvalidSqlWithoutChangingSource() {
        val field = openSql("select 'open")
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val original = source.text
        val stamp = source.modificationStamp
        formatButton().doClick()
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("select 'open", field.text)
        assertEquals(original, source.text)
        assertEquals(stamp, source.modificationStamp)
        val status = descendants(design!!.component).filterIsInstance<javax.swing.JLabel>().single { it.name == "query.status" }
        assertTrue(status.text.contains("닫히지 않은"))
    }

    fun testFormatProtectsReadonlyFileAndDocument() {
        val field = openSql("select A,B from EMP")
        val file = design!!.file!!
        val source = FileDocumentManager.getInstance().getDocument(file)!!
        val original = source.text
        val action = formatAction(field)
        WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
        try {
            UIUtil.dispatchAllInvocationEvents()
            assertFalse(formatButton().isEnabled)
            invokeFormatAction(action)
            assertEquals(original, source.text)
        } finally { WriteCommandAction.runWriteCommandAction(project) { file.isWritable = true } }
        UIUtil.dispatchAllInvocationEvents()
        source.setReadOnly(true)
        try {
            design!!.selectNotify()
            UIUtil.dispatchAllInvocationEvents()
            assertFalse(formatButton().isEnabled)
            invokeFormatAction(action)
            assertEquals(original, source.text)
        } finally { source.setReadOnly(false) }
    }

    fun testFormatProtectsMixedInvalidAndMissingQueriesAndDisposedEditor() {
        val field = openSql("select A,B from EMP")
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val action = formatAction(field)
        for (xml in listOf("<queryMap><query id=\"target\">select A<!-- keep --><if/> from EMP</query></queryMap>",
            "<queryMap>", "<queryMap/>")) {
            WriteCommandAction.runWriteCommandAction(project) { source.setText(xml) }
            UIUtil.dispatchAllInvocationEvents()
            assertFalse(formatButton().isEnabled)
            invokeFormatAction(action)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(xml, source.text)
        }
        val original = source.text
        Disposer.dispose(design!!)
        design = null
        invokeFormatAction(action)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, source.text)
    }

    fun testFormatDeferredBeforeSelectionChangeDoesNotModifyEitherQuery() {
        openSql("select A,B from EMP", secondQuery = true)
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val original = source.text
        val button = formatButton()
        val list = descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }
        withoutWriteIntent {
            button.doClick()
            list.selectedIndex = 0
        }
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, source.text)
    }

    fun testFormatDoesNotOverwriteNewSourceWhenPendingEditConflicts() {
        val field = openSql("select A,B from EMP")
        val source = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val latest = source.text.replace("select A,B from EMP", "select X,Y from EMP")
        WriteCommandAction.runWriteCommandAction(project) {
            field.document.setText("select C,D from EMP")
            source.setText(latest)
        }
        formatButton().doClick()
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(latest, source.text)
        assertEquals("select X,Y from EMP", field.text)
    }

    fun testFormatSelectedSqlUpdatesOnlyItsSourceAndSupportsSingleUndoRedo() {
        val original = "select COL1,COL2 from DUAL where COL1=COL and COL2=COL"
        val expected = "select COL1, COL2 from DUAL where COL1 = COL and COL2 = COL"
        val field = openSql(original, secondQuery = true)
        val button = formatButton()
        assertTrue(button.isEnabled)
        button.doClick()
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(expected, field.text)
        val rootDocument = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val manager = PsiDocumentManager.getInstance(project)
        manager.commitDocument(rootDocument)
        val queries = (manager.getPsiFile(rootDocument) as XmlFile).rootTag!!.subTags
        assertEquals("select 100", queries[0].value.textElements.joinToString("") { it.value })
        assertEquals(expected, queries[1].value.textElements.joinToString("") { it.value })
        val stamp = rootDocument.modificationStamp
        button.doClick()
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(stamp, rootDocument.modificationStamp)
        FileDocumentManager.getInstance().saveDocument(rootDocument)
        assertEquals(rootDocument.text, String(design!!.file!!.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
        val source = FileEditorProviderManager.getInstance().getProviderList(project, design!!.file!!)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design!!.file!!) as TextEditor
        try {
            UndoManager.getInstance(project).undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(original, field.text)
            UndoManager.getInstance(project).redo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(expected, field.text)
        } finally { Disposer.dispose(source) }
    }

    private fun contextMetadata(): com.look4counter.queryeditor.settings.DataSourceSettingsService {
        val settings = com.look4counter.queryeditor.settings.DataSourceSettingsService.getInstance(project)
        settings.replaceDataSources(listOf(com.look4counter.queryeditor.settings.DataSourceDefinition(
            "context", "Context", "jdbc:postgresql://localhost/db")))
        val service = com.look4counter.queryeditor.database.DatabaseMetadataService.getInstance(project)
        service.complete(service.beginRefresh().single(), com.look4counter.queryeditor.database.MetadataSnapshot(listOf(
            com.look4counter.queryeditor.database.TableMetadata(null, "public", "emp_table", listOf("id", "emp_id", "emp_name")),
            com.look4counter.queryeditor.database.TableMetadata(null, "public", "emp_view", listOf("view_id")),
            com.look4counter.queryeditor.database.TableMetadata(null, "public", "dept_table", listOf("id", "dept_name", "emp_unrelated")),
        )))
        return settings
    }

    fun testFromCompletionExcludesColumns() {
        val settings = contextMetadata()
        try {
            val field = openSql("select * from emp_")
            configureCompletion(field, field.text.length)
            assertEquals(setOf("emp_table", "emp_view"), myFixture.completeBasic()!!.map { it.lookupString }.toSet())
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testColumnCompletionGroupsNamesBeforeKeywordsAndSortsWithinGroups() {
        val settings = contextMetadata()
        try {
            val sql = "SELECT  FROM emp_table"
            val field = openSql(sql)
            configureCompletion(field, sql.indexOf("  ") + 1)
            myFixture.completeBasic()
            val names = myFixture.lookup.items.map { it.lookupString }
            assertEquals(listOf("emp_id", "emp_name", "id"), names.take(3))
            val keywords = names.drop(3)
            assertTrue(keywords.contains("SELECT"))
            assertEquals(keywords.sortedWith(String.CASE_INSENSITIVE_ORDER), keywords)
            assertEquals(sql, field.text)
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testTableCompletionMergesCtesAndTablesIntoOneAlphabeticalGroup() {
        val settings = contextMetadata()
        try {
            val sql = "WITH aaa AS (SELECT id FROM emp_table), zzz AS (SELECT id FROM emp_table) SELECT * FROM "
            val field = openSql(sql)
            configureCompletion(field, sql.length)
            myFixture.completeBasic()
            val names = myFixture.lookup.items.map { it.lookupString }
            assertEquals(listOf("aaa", "dept_table", "emp_table", "emp_view", "zzz"), names.take(5))
            val keywords = names.drop(5)
            assertTrue(keywords.contains("SELECT"))
            assertEquals(keywords.sortedWith(String.CASE_INSENSITIVE_ORDER), keywords)
            assertEquals(sql, field.text)
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testFallbackCompletionGroupsColumnsBeforeTablesAndKeywords() {
        val settings = contextMetadata()
        try {
            val field = openSql(" ")
            configureCompletion(field, 1)
            myFixture.completeBasic()
            val names = myFixture.lookup.items.map { it.lookupString }
            val columns = listOf("dept_name", "emp_id", "emp_name", "emp_unrelated", "id", "view_id")
            assertEquals(columns, names.take(columns.size))
            assertEquals(listOf("dept_table", "emp_table", "emp_view"), names.drop(columns.size).take(3))
            val keywords = names.drop(columns.size + 3)
            assertTrue(keywords.contains("SELECT"))
            assertEquals(keywords.sortedWith(String.CASE_INSENSITIVE_ORDER), keywords)
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testKeywordNamedColumnKeepsColumnPriorityWithoutDuplicateItems() {
        val settings = contextMetadata()
        try {
            val metadata = com.look4counter.queryeditor.database.DatabaseMetadataService.getInstance(project)
            assertTrue(metadata.complete(metadata.beginRefresh().single(), com.look4counter.queryeditor.database.MetadataSnapshot(listOf(
                com.look4counter.queryeditor.database.TableMetadata(null, "public", "emp_table", listOf("z_column", "SELECT"))))))
            val sql = "SELECT  FROM emp_table"
            val field = openSql(sql)
            configureCompletion(field, sql.indexOf("  ") + 1)
            myFixture.completeBasic()
            val names = myFixture.lookup.items.map { it.lookupString }
            assertEquals(listOf("SELECT", "z_column"), names.take(2))
            assertEquals(1, names.count { it == "SELECT" })
            val presentation = com.intellij.codeInsight.lookup.LookupElementPresentation()
            myFixture.lookup.items.first().renderElement(presentation)
            assertTrue(presentation.typeText!!.contains("컬럼"))
            assertTrue(presentation.typeText!!.contains("SQL 키워드"))
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testOutputColumnsJoinColumnGroupBeforeKeywords() {
        val settings = contextMetadata()
        try {
            val sql = "SELECT emp_name AS zzz_output FROM emp_table ORDER BY "
            val field = openSql(sql)
            configureCompletion(field, sql.length)
            myFixture.completeBasic()
            val names = myFixture.lookup.items.map { it.lookupString }
            assertEquals(listOf("emp_id", "emp_name", "id", "zzz_output"), names.take(4))
            assertTrue(names.drop(4).contains("SELECT"))
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testAliasCompletionUsesFollowingFromAndPreservesQualifierOnInsertion() {
        val settings = contextMetadata()
        try {
            val sql = "select e.emp_ from emp_table AS e join dept_table d on e.id = d.id"
            val field = openSql(sql, secondQuery = true)
            configureCompletion(field, sql.indexOf("emp_") + 4)
            val variants = myFixture.completeBasic()!!
            assertEquals(setOf("emp_id", "emp_name"), variants.map { it.lookupString }.toSet())
            assertEquals(listOf("emp_id", "emp_name"), myFixture.lookup.items.map { it.lookupString })
            myFixture.lookup.currentItem = variants.single { it.lookupString == "emp_name" }
            myFixture.finishLookup('\n')
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(sql.replace("e.emp_ ", "e.emp_name "), field.text)
            val rootDocument = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
            PsiDocumentManager.getInstance(project).commitDocument(rootDocument)
            val queries = (PsiDocumentManager.getInstance(project).getPsiFile(rootDocument) as XmlFile).rootTag!!.subTags
            assertEquals("select 100", queries[0].value.textElements.joinToString("") { it.value })
            assertEquals(field.text, queries[1].value.textElements.joinToString("") { it.value })
            val source = FileEditorProviderManager.getInstance().getProviderList(project, design!!.file!!)
                .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design!!.file!!) as TextEditor
            try {
                UndoManager.getInstance(project).undo(source)
                UIUtil.dispatchAllInvocationEvents()
                assertEquals(sql, field.text)
                UndoManager.getInstance(project).redo(source)
                UIUtil.dispatchAllInvocationEvents()
                assertEquals(sql.replace("e.emp_ ", "e.emp_name "), field.text)
            } finally { Disposer.dispose(source) }
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testCteCompletionAfterDotAndDuplicateColumnInsertion() {
        val settings = contextMetadata()
        try {
            val sql = "WITH t(a,b) AS (SELECT id,emp_name FROM emp_table) SELECT x. FROM t x"
            val field = openSql(sql)
            configureCompletion(field, sql.indexOf("x.") + 2)
            val psi = PsiDocumentManager.getInstance(project).getPsiFile(field.document)!!
            assertTrue(com.intellij.util.textCompletion.TextCompletionContributor().invokeAutoPopup(psi.findElementAt(0)!!, '.'))
            val variants = myFixture.completeBasic()!!
            assertEquals(setOf("a", "b"), variants.map { it.lookupString }.toSet())
            myFixture.lookup.currentItem = variants.single { it.lookupString == "b" }
            myFixture.finishLookup('\n')
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(sql.replace("x. ", "x.b "), field.text)

            val duplicate = "SELECT id FROM emp_table e JOIN dept_table d ON e.id=d.id"
            WriteCommandAction.runWriteCommandAction(project) { field.document.setText(duplicate) }
            UIUtil.dispatchAllInvocationEvents()
            configureCompletion(field, duplicate.indexOf("id") + 2)
            val choices = myFixture.completeBasic()!!
            assertTrue(choices.any { it.lookupString == "e.id" })
            assertTrue(choices.any { it.lookupString == "d.id" })
            myFixture.lookup.currentItem = choices.single { it.lookupString == "d.id" }
            myFixture.finishLookup('\n')
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(duplicate.replaceFirst("SELECT id", "SELECT d.id"), field.text)
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testSqlBracePairsFollowCaretAndIgnoreQuotedAndCommentedBraces() {
        val sql = "select (a + [b * {c} ] ), '(' -- )\n /* [ ] */ x"
        val field = openSql(sql)
        val editor = field.editor!!
        fun ranges(): List<Int> = editor.markupModel.allHighlighters
            .filter { it.textAttributesKey == com.intellij.openapi.editor.colors.CodeInsightColors.MATCHED_BRACE_ATTRIBUTES }
            .map { it.startOffset }.sorted()
        for ((open, close) in listOf('(' to ')', '[' to ']', '{' to '}')) {
            val first = sql.indexOf(open)
            val last = sql.indexOf(close)
            editor.caretModel.moveToOffset(first)
            assertEquals(listOf(first, last), ranges())
            editor.caretModel.moveToOffset(last + 1)
            assertEquals(listOf(first, last), ranges())
        }
        editor.caretModel.moveToOffset(sql.indexOf("'('") + 1)
        assertEmpty(ranges())
        editor.caretModel.moveToOffset(sql.indexOf("-- )") + 3)
        assertEmpty(ranges())
        editor.caretModel.moveToOffset(sql.indexOf("/* [") + 3)
        assertEmpty(ranges())
        editor.caretModel.moveToOffset(0)
        assertEmpty(ranges())
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText("select (a)") }
        editor.caretModel.moveToOffset(7)
        assertEquals(listOf(7, 9), ranges())
        WriteCommandAction.runWriteCommandAction(project) { field.document.deleteString(9, 10) }
        assertEmpty(ranges())
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText("select ([)]") }
        editor.caretModel.moveToOffset(7)
        assertEmpty(ranges())
    }

    fun testSqlBraceHighlightsClearOnQuerySwitchAndDisposal() {
        val field = openSql("select (1)", secondQuery = true)
        var editor = field.editor!!
        fun currentEditor(): com.intellij.openapi.editor.Editor {
            field.ensureWillComputePreferredSize()
            field.preferredSize
            return field.editor!!
        }
        fun highlights() = editor.markupModel.allHighlighters.filter {
            it.textAttributesKey == com.intellij.openapi.editor.colors.CodeInsightColors.MATCHED_BRACE_ATTRIBUTES
        }
        editor.caretModel.moveToOffset(7)
        assertEquals(2, highlights().size)
        val initial = highlights()
        val list = descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }
        list.selectedIndex = 0
        UIUtil.dispatchAllInvocationEvents()
        assertTrue(editor.isDisposed)
        assertTrue(initial.none { it.isValid })
        editor = currentEditor()
        assertEmpty(highlights())
        list.selectedIndex = 1
        UIUtil.dispatchAllInvocationEvents()
        assertTrue(editor.isDisposed)
        editor = currentEditor()
        editor.caretModel.moveToOffset(7)
        val previous = highlights()
        assertEquals(2, previous.size)
        Disposer.dispose(design!!)
        design = null
        UIUtil.dispatchAllInvocationEvents()
        assertTrue(editor.isDisposed)
        assertTrue(previous.none { it.isValid })
    }

    private var design: FileEditor? = null

    override fun tearDown() {
        try {
            design?.let(Disposer::dispose)
        } finally {
            super.tearDown()
        }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private fun openSql(text: String, secondQuery: Boolean = false): EditorTextField {
        fun query(id: String, sql: String) = "<query id=\"$id\"><![CDATA[${sql.replace("]]>", "]]]]><![CDATA[>")}]]></query>"
        val xml = "<queryMap>" + (if (secondQuery) query("first", "select 100") else "") + query("target", text) + "</queryMap>"
        val file = myFixture.addFileToProject("syntax.glue_sql", xml).virtualFile
        design = FileEditorProviderManager.getInstance().getProviderList(project, file)
            .single { it.editorTypeId == "glue-query-design" }.createEditor(project, file)
        UIUtil.dispatchAllInvocationEvents()
        if (secondQuery) descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }.selectedIndex = 1
        val field = descendants(design!!.component).filterIsInstance<EditorTextField>().single { it.name == "query.sql" }
        field.ensureWillComputePreferredSize()
        field.preferredSize
        return field
    }

    private data class Token(val text: String, val type: String, val color: Color?)

    private fun tokens(field: EditorTextField): List<Token> {
        val iterator = (field.editor as EditorEx).highlighter.createIterator(0)
        return buildList {
            while (!iterator.atEnd()) {
                add(Token(field.text.substring(iterator.start, iterator.end), iterator.tokenType?.toString().orEmpty(), iterator.textAttributes?.foregroundColor))
                iterator.advance()
            }
        }
    }

    private fun parameterHints(field: EditorTextField): List<Pair<Int, String>> {
        val presentation = ParameterHintsPresentationManager.getInstance()
        return presentation.getParameterHintsInRange(field.editor!!, 0, field.document.textLength)
            .map { it.offset to presentation.getHintText(it) }
    }

    fun testPositionalParametersShareCyanAndHaveOrderedInlineHints() {
        val sql = "select ?, :name, '?', \"?\", ? -- ?\n /* outer ? /* nested ? */ ? */ ?; ?"
        val field = openSql(sql)
        val expected = listOf(sql.indexOf('?'), sql.indexOf("? --"), sql.indexOf("?;"), sql.lastIndexOf('?'))
        assertEquals(expected.zip(listOf("0:", "1:", "2:", "3:")), parameterHints(field))
        val parameters = tokens(field).filter { it.type == "GLUE_SQL_PARAMETER" }
        assertEquals(listOf("?", ":name", "?", "?", "?"), parameters.map { it.text })
        assertTrue(parameters.all { it.color!!.rgb == Color(0x008B9A).rgb })
        assertEquals(sql, field.document.text)
    }

    fun testParameterHintRenumberingQuerySwitchUndoAndStorage() {
        val field = openSql("select ?, ?", secondQuery = true)
        val original = field.text
        val source = FileEditorProviderManager.getInstance().getProviderList(project, design!!.file!!)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design!!.file!!) as TextEditor
        try {
            WriteCommandAction.runWriteCommandAction(project) { field.document.insertString(7, "?, ") }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(listOf("0:", "1:", "2:"), parameterHints(field).map { it.second })
            UndoManager.getInstance(project).undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(original, field.text)
            assertEquals(listOf("0:", "1:"), parameterHints(field).map { it.second })
            UndoManager.getInstance(project).redo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(listOf("0:", "1:", "2:"), parameterHints(field).map { it.second })
            val list = descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }
            list.selectedIndex = 0
            assertTrue(parameterHints(field).isEmpty())
            list.selectedIndex = 1
            assertEquals(listOf("0:", "1:", "2:"), parameterHints(field).map { it.second })
            WriteCommandAction.runWriteCommandAction(project) { field.document.setText("select '?', :name -- ?") }
            UIUtil.dispatchAllInvocationEvents()
            assertTrue(parameterHints(field).isEmpty())
            val rootDocument = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
            FileDocumentManager.getInstance().saveDocument(rootDocument)
            assertTrue(rootDocument.text.contains("select '?', :name -- ?"))
            assertFalse(rootDocument.text.contains("0:"))
            assertEquals(rootDocument.text, String(design!!.file!!.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
        } finally { Disposer.dispose(source) }
    }

    fun testSqlTokenBoundariesAndRequestedColors() {
        val field = openSql("sElEcT table1, 12, 3.5, .25, 1e-2, 0xFF, 'it''s SELECT 99', \"EMP\"\"NAME\"\n" +
            "from emp where empno=:select\n-- SELECT 99 'ignored'\n/* UPDATE 42 */\n'open SELECT 77")
        val values = tokens(field)
        assertEquals("GLUE_SQL_KEYWORD", values.first().type)
        assertEquals(setOf("12", "3.5", ".25", "1e-2", "0xFF"), values.filter { it.type == "GLUE_SQL_NUMBER" }.map { it.text }.toSet())
        assertEquals(listOf("'it''s SELECT 99'", "\"EMP\"\"NAME\"", "'open SELECT 77"),
            values.filter { it.type == "GLUE_SQL_QUOTED" }.map { it.text })
        assertEquals(2, values.count { it.type == "GLUE_SQL_COMMENT" })
        assertEquals("GLUE_SQL_IDENTIFIER", values.single { it.text == "table1" }.type)
        assertEquals("GLUE_SQL_PARAMETER", values.single { it.text == ":select" }.type)
        assertEquals(Color(0x008B9A).rgb, values.single { it.text == ":select" }.color!!.rgb)
        assertEquals(Color(0xC46600).rgb, values.first().color!!.rgb)
        assertEquals(Color(0x175BC1).rgb, values.first { it.type == "GLUE_SQL_NUMBER" }.color!!.rgb)
        assertEquals(Color(0x26823B).rgb, values.first { it.type == "GLUE_SQL_QUOTED" }.color!!.rgb)
        assertTrue(values.filter { it.type == "GLUE_SQL_COMMENT" }.all { it.color!!.rgb == Color(0x3B7F4C).rgb })
        assertEquals(field.text, values.joinToString("") { it.text })
    }

    private fun configureCompletion(field: EditorTextField, offset: Int) {
        val manager = PsiDocumentManager.getInstance(project)
        manager.commitDocument(field.document)
        val psi = manager.getPsiFile(field.document)
        assertNotNull("SQL buffer must expose a PSI document for completion", psi)
        assertEquals(true, psi!!.getUserData(com.intellij.util.textCompletion.TextCompletionUtil.AUTO_POPUP_KEY))
        assertTrue(com.intellij.util.textCompletion.TextCompletionContributor().invokeAutoPopup(psi.findElementAt(0)!!, 'e'))
        myFixture.configureFromExistingVirtualFile(psi.virtualFile)
        assertSame(field.document, myFixture.editor.document)
        myFixture.editor.caretModel.moveToOffset(offset)
    }

    fun testKeywordCompletionUpdatesOnlySelectedQueryAndSupportsUndo() {
        val field = openSql("se", secondQuery = true)
        configureCompletion(field, 2)
        val variants = myFixture.completeBasic()!!
        assertTrue(variants.any { it.lookupString == "SELECT" })
        assertTrue(variants.all { it.lookupString.startsWith("SE", ignoreCase = true) })
        myFixture.lookup.currentItem = variants.single { it.lookupString == "SELECT" }
        myFixture.finishLookup('\n')
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("SELECT", field.text.uppercase())
        val rootDocument = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val manager = PsiDocumentManager.getInstance(project)
        manager.commitDocument(rootDocument)
        val queries = (manager.getPsiFile(rootDocument) as XmlFile).rootTag!!.subTags
        assertEquals("select 100", queries[0].value.textElements.joinToString("") { it.value })
        assertEquals(field.text, queries[1].value.textElements.joinToString("") { it.value })
        assertEquals("GLUE_SQL_KEYWORD", tokens(field).single().type)
        val source = FileEditorProviderManager.getInstance().getProviderList(project, design!!.file!!)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design!!.file!!) as TextEditor
        try {
            UndoManager.getInstance(project).undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("se", field.text)
            UndoManager.getInstance(project).redo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("SELECT", field.text.uppercase())
            val list = descendants(design!!.component).filterIsInstance<JList<*>>().single { it.name == "query.list" }
            list.selectedIndex = 0
            assertEquals("GLUE_SQL_KEYWORD", tokens(field).first().type)
            list.selectedIndex = 1
            assertEquals("GLUE_SQL_KEYWORD", tokens(field).first().type)
            configureCompletion(field, 2)
            assertTrue(myFixture.completeBasic()!!.any { it.lookupString == "SELECT" })
        } finally {
            Disposer.dispose(source)
        }
    }

    fun testCompletionIsSuppressedInsideQuotesCommentsAndNumbers() {
        val field = openSql("select 1")
        val cases = listOf("SELECT example_name, 'se<caret>'", "SELECT \"se<caret>\"", "SELECT 1 -- se<caret>",
            "SELECT 1 /* se<caret> */", "SELECT 'open se<caret>", "SELECT 12<caret>", "SELECT :se<caret>")
        for (sample in cases) {
            val text = sample.replace("<caret>", "")
            WriteCommandAction.runWriteCommandAction(project) { field.document.setText(text) }
            UIUtil.dispatchAllInvocationEvents()
            configureCompletion(field, sample.indexOf("<caret>"))
            val variants = myFixture.completeBasic()
            assertTrue("Unexpected candidates inside $sample", variants.isNullOrEmpty())
            assertEquals(text, field.text)
        }
    }

    fun testIncrementalHighlightingReclassifiesClosedQuotesAndNestedComments() {
        val field = openSql("SELECT 'open\nFROM emp")
        assertEquals(listOf("SELECT"), tokens(field).filter { it.type == "GLUE_SQL_KEYWORD" }.map { it.text })
        val updated = "SELECT 'it\\'s' 42 /* outer /* SELECT */ 99 */ END-EXEC"
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText(updated) }
        UIUtil.dispatchAllInvocationEvents()
        val values = tokens(field)
        assertEquals(listOf("SELECT", "END-EXEC"), values.filter { it.type == "GLUE_SQL_KEYWORD" }.map { it.text })
        assertEquals(listOf("42"), values.filter { it.type == "GLUE_SQL_NUMBER" }.map { it.text })
        assertEquals(1, values.count { it.type == "GLUE_SQL_COMMENT" })
        assertEquals(updated, values.joinToString("") { it.text })
    }

    fun testParametersAreDistinctFromCastsAssignmentsQuotesAndComments() {
        val field = openSql("select :id, :_name2, :한글, value::type, :target := 12, ':hidden', \"x:quoted\" -- :comment")
        val values = tokens(field)
        assertEquals(listOf(":id", ":_name2", ":한글", ":target"), values.filter { it.type == "GLUE_SQL_PARAMETER" }.map { it.text })
        assertEquals(field.text, values.joinToString("") { it.text })
    }

    fun testReadOnlySqlEditorDoesNotOfferCompletion() {
        val field = openSql("se")
        val file = design!!.file!!
        WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
        try {
            design!!.selectNotify()
            UIUtil.dispatchAllInvocationEvents()
            field.preferredSize
            val editor = field.editor as EditorEx
            assertTrue(editor.isViewer)
            editor.caretModel.moveToOffset(2)
            com.intellij.codeInsight.completion.CodeCompletionHandlerBase(com.intellij.codeInsight.completion.CompletionType.BASIC)
                .invokeCompletion(project, editor)
            assertNull(com.intellij.codeInsight.lookup.LookupManager.getActiveLookup(editor))
            assertEquals("se", field.text)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { file.isWritable = true }
        }
    }

    fun testOpenAndSaveNormalizationPreserveHighlightingAndCompletion() {
        val field = openSql(" \nselect 12\n ")
        val file = design!!.file!!
        com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(file, false)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("select 12", field.text)
        assertEquals("GLUE_SQL_KEYWORD", tokens(field).first().type)
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText(" \nselect 34\n ") }
        UIUtil.dispatchAllInvocationEvents()
        FileDocumentManager.getInstance().saveDocument(FileDocumentManager.getInstance().getDocument(file)!!)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("select 34", field.text)
        assertEquals(listOf("34"), tokens(field).filter { it.type == "GLUE_SQL_NUMBER" }.map { it.text })
        configureCompletion(field, 2)
        assertTrue(myFixture.completeBasic()!!.any { it.lookupString == "SELECT" })
    }

    private fun captureSql(dark: Boolean) {
        val colors = EditorColorsManager.getInstance()
        val previousScheme = colors.globalScheme
        val previousLaf = javax.swing.UIManager.getLookAndFeel()
        val wasDark = !JBColor.isBright()
        try {
            if (dark) {
                javax.swing.UIManager.setLookAndFeel(com.intellij.ide.ui.laf.darcula.DarculaLaf())
                JBColor.setDark(true)
                colors.setGlobalScheme(colors.getScheme("Darcula")!!)
            }
            val sample = "select empno, 123, 3.14, 'hello SQL', \"EMP_NAME\"\nfrom emp\nwhere empno = :empno and ename = :name and salary > ? and deptno = ?;\n-- SELECT 99 ':comment'\n/* UPDATE 5 */"
            val field = openSql(sample)
            field.setSize(720, 220)
            repeat(3) { descendants(field).filterIsInstance<Container>().forEach { it.doLayout() } }
            val image = java.awt.image.BufferedImage(field.width, field.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try { field.paint(graphics) } finally { graphics.dispose() }
            val output = Path.of(".gradle-validation", "sql-syntax-${if (dark) "dark" else "light"}.png")
            Files.createDirectories(output.parent)
            javax.imageio.ImageIO.write(image, "png", output.toFile())
            val values = tokens(field)
            assertEquals(listOf("0:", "1:"), parameterHints(field).map { it.second })
            assertTrue(values.filter { it.text == "?" }.all { it.color!!.rgb == Color(if (dark) 0x56D4DD else 0x008B9A).rgb })
            assertEquals(Color(if (dark) 0xFFA657 else 0xC46600).rgb, values.first().color!!.rgb)
            assertEquals(Color(if (dark) 0x6CA8F5 else 0x175BC1).rgb, values.first { it.type == "GLUE_SQL_NUMBER" }.color!!.rgb)
            assertEquals(Color(if (dark) 0x78C78B else 0x26823B).rgb, values.first { it.type == "GLUE_SQL_QUOTED" }.color!!.rgb)
            assertEquals(Color(if (dark) 0x56D4DD else 0x008B9A).rgb, values.first { it.type == "GLUE_SQL_PARAMETER" }.color!!.rgb)
            assertTrue(values.filter { it.type == "GLUE_SQL_COMMENT" }.all { it.color!!.rgb == Color(if (dark) 0x6A9955 else 0x3B7F4C).rgb })
        } finally {
            design?.let(Disposer::dispose)
            design = null
            colors.setGlobalScheme(previousScheme)
            javax.swing.UIManager.setLookAndFeel(previousLaf)
            JBColor.setDark(wasDark)
        }
    }

    fun testLightSyntaxCapture() = captureSql(false)
    fun testDarkSyntaxCapture() = captureSql(true)

    fun testAllDatabaseMetadataCompletionIsDistinctAndInsertsIntoQuery() {
        val settings = com.look4counter.queryeditor.settings.DataSourceSettingsService.getInstance(project)
        settings.replaceDataSources(listOf(com.look4counter.queryeditor.settings.DataSourceDefinition("one", "Local", "jdbc:postgresql://localhost/db")))
        val metadata = com.look4counter.queryeditor.database.DatabaseMetadataService.getInstance(project)
        try {
            val snapshot = com.look4counter.queryeditor.database.MetadataSnapshot(listOf(
                com.look4counter.queryeditor.database.TableMetadata(null, "public", "emp_table", listOf("emp_id", "emp_name", "emp_id")),
                com.look4counter.queryeditor.database.TableMetadata(null, "other", "emp_table", listOf("emp_id", "SELECT"))))
            metadata.complete(metadata.beginRefresh().single(), snapshot)
            val field = openSql("select emp_")
            configureCompletion(field, field.text.length)
            val variants = myFixture.completeBasic()!!
            assertEquals(setOf("emp_id", "emp_name"), variants.map { it.lookupString }.toSet())
            assertEquals(2, variants.size)
            myFixture.lookup.currentItem = variants.single { it.lookupString == "emp_id" }
            myFixture.finishLookup('\n')
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("select emp_id", field.text)
            val root = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
            assertTrue(root.text.contains("select emp_id"))
            settings.replaceDataSources(emptyList())
            WriteCommandAction.runWriteCommandAction(project) { field.document.setText("emp_") }
            configureCompletion(field, 4)
            assertTrue(myFixture.completeBasic().isNullOrEmpty())
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testSettingsRefreshReadsLocalDriverAndEditorHasNoMetadataToolbar() {
        val directory = Files.createTempDirectory("glue-toolbar-driver")
        val jar = directory.resolve("driver.jar")
        val settings = com.look4counter.queryeditor.settings.DataSourceSettingsService.getInstance(project)
        val configurable = com.look4counter.queryeditor.settings.GlueQueryDatabaseConfigurable(project)
        try {
            val resource = "com/look4counter/FixtureJdbcDriver.class"
            java.util.jar.JarOutputStream(Files.newOutputStream(jar)).use { output ->
                output.putNextEntry(java.util.zip.ZipEntry(resource))
                requireNotNull(javaClass.classLoader.getResourceAsStream(resource)).use { it.copyTo(output) }
                output.closeEntry()
            }
            settings.replaceDataSources(listOf(com.look4counter.queryeditor.settings.DataSourceDefinition(
                "toolbar", "Local", "jdbc:glue-test:public", "com.look4counter.FixtureJdbcDriver", "", false,
                "POSTGRESQL", mutableListOf(jar.toString()))))
            openSql("select emp")
            val editorControls = descendants(design!!.component).toList()
            assertTrue(editorControls.none { it.name in setOf("query.datasource", "query.metadata.refresh", "query.metadata.status") })
            val settingsComponent = configurable.createComponent()
            configurable.reset()
            val controls = descendants(settingsComponent).toList()
            val button = controls.filterIsInstance<javax.swing.JButton>().single { it.name == "query.metadata.refresh" }
            val status = controls.filterIsInstance<javax.swing.JLabel>().single { it.name == "query.metadata.status" }
            button.doClick()
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("metadata refresh", { button.isEnabled }, 10)
            assertEquals("테이블 1 · 컬럼 1", status.text)
            val metadata = com.look4counter.queryeditor.database.DatabaseMetadataService.getInstance(project)
            assertEquals(listOf("EMP"), metadata.snapshot()!!.tableNames)
            System.setProperty("glue.test.fail", "true")
            button.doClick()
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("failed metadata refresh", { button.isEnabled }, 10)
            assertTrue(status.text.startsWith("조회 실패"))
            assertFalse(status.text.contains("secret-value"))
            assertEquals(listOf("EMP"), metadata.snapshot()!!.tableNames)
        } finally {
            configurable.disposeUIResources()
            settings.replaceDataSources(emptyList())
            listOf("glue.test.closed", "glue.test.deregistered", "glue.test.fail").forEach(System::clearProperty)
            Files.delete(jar)
            Files.delete(directory)
        }
    }
}
