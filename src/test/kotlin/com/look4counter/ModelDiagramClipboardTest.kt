package com.look4counter

import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonShortcuts
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import com.look4counter.modeleditor.*
import java.awt.Cursor
import java.awt.Point
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseEvent
import javax.swing.JScrollPane

class ModelDiagramClipboardTest : BasePlatformTestCase() {
    fun testPasteAfterZoomKeepsCopiedScreenAnchorAndModelSize() {
        val session = open()
        session.canvas.setSize(600, 400)
        click(session.canvas, 70, 55)
        copy(session.canvas)
        val before = session.canvas.selectedNodes.single()
        session.canvas.setZoom(1.5)
        paste(session.canvas)
        val pasted = session.canvas.selectedNodes.single()
        val screen = DiagramPoint(pasted.location.x * session.canvas.zoom + session.canvas.screenOffset.x,
            pasted.location.y * session.canvas.zoom + session.canvas.screenOffset.y)
        assertEquals(35.0, screen.x, session.canvas.zoom / 2)
        assertEquals(55.0, screen.y, session.canvas.zoom / 2)
        assertEquals(pasted.location.x, kotlin.math.round(pasted.location.x))
        assertEquals(pasted.location.y, kotlin.math.round(pasted.location.y))
        assertEquals(before.width, pasted.width)
        assertEquals(before.height, pasted.height)
    }
    private data class Session(val canvas: ModelDiagramCanvas, val source: TextEditor,
        val editor: com.intellij.openapi.fileEditor.FileEditor) {
        val document get() = source.editor.document
    }
    private val sessions = mutableListOf<Session>()

    override fun tearDown() {
        try {
            sessions.asReversed().forEach {
                Disposer.dispose(it.source)
                Disposer.dispose(it.editor)
            }
        } finally {
            super.tearDown()
        }
    }

    private fun fixture(bent: Boolean = true) = """<Glue:GlueDiagram xmlns:Glue="http://www.glueframework.com/activitydiagram/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" id="0">
        <child xsi:type="Glue:Activity" id="2" name="A" location="20,40" size="100,-1" output="//@wire.0" custom="child">
          <property key="description" value="복사 &amp; 보존"/>
        </child>
        <child xsi:type="Glue:Activity" id="9" name="B" location="220,40" size="100,-1" input="//@wire.0" output="//@wire.1"/>
        <child xsi:type="Glue:InitialState" id="30" location="220,160" input="//@wire.1"/>
        <wire id="150" guardcondition="guard" firstRelativePoint="100,55" lastRelativePoint="220,55" custom="wire">
          ${if (bent) """<bendpoint firstDimension="100,55" secondDimension="-100,55" weight="0.2" custom="bend"/>""" else ""}
        </wire>
        <wire id="7" firstRelativePoint="220,55" lastRelativePoint="230,170"/>
        </Glue:GlueDiagram>""".trimIndent()

    private fun open(text: String = fixture()): Session {
        val file = myFixture.addFileToProject("model-${sessions.size}.glue_uml_ad", text).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val source = ModelSourceEditorProvider().createEditor(project, file) as TextEditor
        com.intellij.testFramework.EditorTestUtil.waitForLoading(source.editor)
        val panel = editor.component
        val scroll = panel.components.filterIsInstance<JScrollPane>().single()
        return Session(scroll.viewport.view as ModelDiagramCanvas, source, editor).also { sessions.add(it) }
    }

    private fun press(canvas: ModelDiagramCanvas, x: Int, y: Int, shift: Boolean = false, button: Int = MouseEvent.BUTTON1) {
        val mask = if (button == MouseEvent.BUTTON1) MouseEvent.BUTTON1_DOWN_MASK else MouseEvent.BUTTON3_DOWN_MASK
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0,
            mask or if (shift) MouseEvent.SHIFT_DOWN_MASK else 0, x, y, 1, false, button))
    }

    private fun move(canvas: ModelDiagramCanvas, x: Int, y: Int, button: Int = MouseEvent.BUTTON1) {
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1,
            if (button == MouseEvent.BUTTON1) MouseEvent.BUTTON1_DOWN_MASK else MouseEvent.BUTTON3_DOWN_MASK,
            x, y, 0, false, MouseEvent.NOBUTTON))
    }

    private fun release(canvas: ModelDiagramCanvas, x: Int, y: Int, button: Int = MouseEvent.BUTTON1) {
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, x, y, 1, false, button))
        UIUtil.dispatchAllInvocationEvents()
    }

    private fun click(canvas: ModelDiagramCanvas, x: Int, y: Int, shift: Boolean = false) {
        press(canvas, x, y, shift)
        release(canvas, x, y)
    }

    private fun selectPair(canvas: ModelDiagramCanvas) {
        click(canvas, 70, 55)
        click(canvas, 270, 55, true)
        assertEquals(listOf("2", "9"), canvas.selectedNodes.map { it.id })
    }

    private fun escape(canvas: ModelDiagramCanvas) {
        val action = ActionUtil.getActions(canvas).single { it.templatePresentation.text == "드래그 취소" }
        assertEquals(CommonShortcuts.ESCAPE.shortcuts.toList(), action.shortcutSet.shortcuts.toList())
        action.actionPerformed(AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, DataContext.EMPTY_CONTEXT))
    }

    private fun copy(canvas: ModelDiagramCanvas) {
        val context = DataManager.getInstance().customizeDataContext(DataContext.EMPTY_CONTEXT, canvas)
        assertSame(canvas.copyProvider, PlatformDataKeys.COPY_PROVIDER.getData(context))
        assertTrue(canvas.copyProvider!!.isCopyEnabled(context))
        val action = ActionManager.getInstance().getAction(IdeActions.ACTION_COPY)
        action.actionPerformed(AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context))
    }

    private fun paste(canvas: ModelDiagramCanvas) {
        val context = DataManager.getInstance().customizeDataContext(DataContext.EMPTY_CONTEXT, canvas)
        assertSame(canvas.pasteProvider, PlatformDataKeys.PASTE_PROVIDER.getData(context))
        assertTrue(canvas.pasteProvider!!.isPasteEnabled(context))
        val action = ActionManager.getInstance().getAction(IdeActions.ACTION_PASTE)
        action.actionPerformed(AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context))
        UIUtil.dispatchAllInvocationEvents()
    }

    private fun delete(canvas: ModelDiagramCanvas, shortcut: Boolean = true) {
        val context = DataManager.getInstance().customizeDataContext(DataContext.EMPTY_CONTEXT, canvas)
        assertSame(canvas.deleteProvider, PlatformDataKeys.DELETE_ELEMENT_PROVIDER.getData(context))
        assertTrue(canvas.deleteProvider!!.canDeleteElement(context))
        val action = if (shortcut) ActionUtil.getActions(canvas).single { it.templatePresentation.text == "선택 요소 삭제" }
            else ActionManager.getInstance().getAction(IdeActions.ACTION_DELETE)
        if (shortcut) assertEquals(CommonShortcuts.DELETE.shortcuts.toList(), action.shortcutSet.shortcuts.toList())
        action.actionPerformed(AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context))
        UIUtil.dispatchAllInvocationEvents()
    }

    private fun assertDeleteUndoAndSave(session: Session, original: String) {
        val deleted = session.document.text
        assertTrue(deleted != original)
        UndoManager.getInstance(project).undo(session.source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, session.document.text)
        UndoManager.getInstance(project).redo(session.source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(deleted, session.document.text)
        FileDocumentManager.getInstance().saveDocument(session.document)
        assertEquals(deleted, String(session.editor.file!!.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
    }

    private fun root(session: Session): com.intellij.psi.xml.XmlTag {
        PsiDocumentManager.getInstance(project).commitDocument(session.document)
        return (PsiManager.getInstance(project).findFile(session.editor.file!!) as XmlFile).rootTag!!
    }

    fun testCopySelectedChildrenIncludesOnlyInternalWiresAndPreservesXmlAndUndo() {
        val session = open()
        val canvas = session.canvas
        val original = session.document.text
        val oldDiagram = canvas.diagram
        selectPair(canvas)
        copy(canvas)
        assertEquals(original, session.document.text)
        paste(canvas)
        assertEquals(5, canvas.diagram.nodes.size)
        assertEquals(3, canvas.diagram.wires.size)
        assertEquals(listOf("151", "152"), canvas.selectedNodes.map { it.id })
        val pasted = canvas.diagram.wires.last()
        assertEquals("153", pasted.id)
        assertEquals("151", pasted.start!!.id)
        assertEquals("152", pasted.end!!.id)
        assertEquals(listOf(2), pasted.start!!.outputs)
        assertEquals(listOf(2), pasted.end!!.inputs)
        assertEmpty(pasted.end!!.outputs)
        assertEquals(oldDiagram.nodes, canvas.diagram.nodes.take(3))
        assertEquals(oldDiagram.wires, canvas.diagram.wires.take(2))
        assertEquals(oldDiagram.wires[0].bendpoints, pasted.bendpoints)
        DiagramGeometry.path(oldDiagram.wires[0]).zip(DiagramGeometry.path(pasted)).forEach { (before, after) ->
            assertEquals(before + DiagramPoint(15.0, 15.0), after)
        }
        val tags = root(session).subTags
        assertTrue(tags.first { it.getAttributeValue("id") == "151" }.text.contains("value=\"복사 &amp; 보존\""))
        val wireTag = tags.first { it.getAttributeValue("id") == "153" }
        assertEquals("wire", wireTag.getAttributeValue("custom"))
        assertEquals("bend", wireTag.subTags.single().getAttributeValue("custom"))
        val edited = session.document.text
        UndoManager.getInstance(project).undo(session.source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, session.document.text)
        UndoManager.getInstance(project).redo(session.source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(edited, session.document.text)
        FileDocumentManager.getInstance().saveDocument(session.document)
        assertEquals(edited, String(session.editor.file!!.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
    }

    fun testSingleChildCopyDropsExternalReferencesAndClipboardIsSnapshot() {
        val session = open()
        click(session.canvas, 270, 55)
        copy(session.canvas)
        WriteCommandAction.runWriteCommandAction(project) {
            session.document.setText(session.document.text.replace("name=\"B\"", "name=\"Changed\""))
        }
        UIUtil.dispatchAllInvocationEvents()
        paste(session.canvas)
        val added = session.canvas.selectedNodes.single()
        assertEquals("B", added.name)
        assertEquals("151", added.id)
        assertEmpty(added.inputs)
        assertEmpty(added.outputs)
        assertEquals(2, session.canvas.diagram.wires.size)
    }

    fun testPasteAccountsForNavigationAndCascadesByFifteenPixels() {
        val session = open()
        selectPair(session.canvas)
        copy(session.canvas)
        press(session.canvas, 500, 300, button = MouseEvent.BUTTON3)
        move(session.canvas, 580, 340, MouseEvent.BUTTON3)
        release(session.canvas, 580, 340, MouseEvent.BUTTON3)
        paste(session.canvas)
        assertEquals(DiagramPoint(-45.0, 15.0), session.canvas.selectedNodes.first().location)
        assertEquals(DiagramPoint(80.0, 40.0), session.canvas.pan)
        paste(session.canvas)
        assertEquals(listOf("154", "155"), session.canvas.selectedNodes.map { it.id })
        assertEquals(DiagramPoint(-30.0, 30.0), session.canvas.selectedNodes.first().location)
        copy(session.canvas)
        paste(session.canvas)
        assertEquals(DiagramPoint(-15.0, 45.0), session.canvas.selectedNodes.first().location)
    }

    fun testCopyCanPasteIntoDifferentDocumentWithLargeIdsAndNamespaces() {
        val source = open()
        selectPair(source.canvas)
        copy(source.canvas)
        val target = open("""<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><child xsi:type="InitialState" id="999999999999999999999999999999" location="0,0"/></GlueDiagram>""")
        paste(target.canvas)
        assertEquals(listOf("1000000000000000000000000000000", "1000000000000000000000000000001"), target.canvas.selectedNodes.map { it.id })
        assertEquals(1, target.canvas.diagram.wires.size)
        assertEquals(listOf(0), target.canvas.selectedNodes.first().outputs)
        assertNull(target.canvas.diagram.message)
        assertEquals("http://www.glueframework.com/activitydiagram/", root(target).subTags.first { it.getAttributeValue("name") == "A" }.getAttributeValue("xmlns:Glue"))
    }

    fun testMixedActivityAndStateCopyRemapsTheSelectedWireByDeclarationIndex() {
        val session = open()
        click(session.canvas, 270, 55)
        click(session.canvas, 230, 170, true)
        copy(session.canvas)
        paste(session.canvas)
        assertEquals(listOf(DiagramNodeKind.Activity, DiagramNodeKind.InitialState), session.canvas.selectedNodes.map { it.kind })
        assertEquals(listOf("151", "152"), session.canvas.selectedNodes.map { it.id })
        val wire = session.canvas.diagram.wires.last()
        assertEquals("153", wire.id)
        assertEquals(2, wire.index)
        assertEquals(listOf(2), wire.start!!.outputs)
        assertEmpty(wire.start!!.inputs)
        assertEquals(listOf(2), wire.end!!.inputs)
        assertEquals(DiagramPoint(235.0, 175.0), wire.end!!.location)
    }

    fun testPasteIntoEmptyPannedDocumentKeepsCopiedScreenPositionAndNegativeCoordinates() {
        val source = open("""<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><child xsi:type="FinalState" id="5" location="-40,-30"/></GlueDiagram>""")
        click(source.canvas, 30, 30)
        assertEquals(1, source.canvas.selectedNodes.size)
        copy(source.canvas)
        val target = open("<GlueDiagram/>")
        press(target.canvas, 400, 200, button = MouseEvent.BUTTON3)
        move(target.canvas, 500, 320, MouseEvent.BUTTON3)
        release(target.canvas, 500, 320, MouseEvent.BUTTON3)
        paste(target.canvas)
        val location = target.canvas.selectedNodes.single().location
        assertEquals(DiagramPoint(-65.0, -85.0), location)
        assertEquals(DiagramPoint(35.0, 35.0), location + target.canvas.screenOffset)
        assertEquals(DiagramPoint(100.0, 120.0), target.canvas.pan)
        assertEquals("1", target.canvas.selectedNodes.single().id)
    }

    fun testPasteCompensatesForViewportScrolling() {
        val session = open("""<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><child xsi:type="InitialState" id="5" location="100,100"/></GlueDiagram>""")
        session.editor.component.setSize(400, 300)
        session.editor.component.doLayout()
        val scroll = session.editor.component.components.filterIsInstance<JScrollPane>().single()
        session.canvas.preferredSize = java.awt.Dimension(1200, 900)
        scroll.doLayout()
        session.canvas.setSize(1200, 900)
        scroll.viewport.viewPosition = Point(60, 40)
        assertEquals(60, session.canvas.visibleRect.x)
        assertEquals(40, session.canvas.visibleRect.y)
        click(session.canvas, 110, 110)
        copy(session.canvas)
        scroll.viewport.viewPosition = Point(120, 90)
        paste(session.canvas)
        val location = session.canvas.selectedNodes.single().location
        assertEquals(DiagramPoint(175.0, 165.0), location)
        assertEquals(DiagramPoint(55.0, 75.0), location + session.canvas.screenOffset)
        assertEquals(Point(120, 90), scroll.viewport.viewPosition)
    }

    fun testInvalidTargetDocumentDisablesPasteWithoutChangingClipboard() {
        val source = open()
        selectPair(source.canvas)
        copy(source.canvas)
        val clipboard = DiagramClipboard.read()
        val target = open("<GlueDiagram><child")
        assertFalse(target.canvas.pasteProvider!!.isPasteEnabled(DataContext.EMPTY_CONTEXT))
        target.canvas.pasteProvider!!.performPaste(DataContext.EMPTY_CONTEXT)
        assertEquals("<GlueDiagram><child", target.document.text)
        assertSame(clipboard, DiagramClipboard.read())
    }

    fun testClipboardActionsIgnoreWireOnlySelectionAndRejectReadOnlyOrForeignPaste() {
        val session = open()
        com.intellij.openapi.ide.CopyPasteManager.getInstance().setContents(StringSelection("plain text"))
        assertFalse(session.canvas.pasteProvider!!.isPasteEnabled(DataContext.EMPTY_CONTEXT))
        click(session.canvas, 170, 110)
        assertNotNull(session.canvas.selectedWire)
        assertFalse(session.canvas.copyProvider!!.isCopyEnabled(DataContext.EMPTY_CONTEXT))
        session.canvas.copyProvider!!.performCopy(DataContext.EMPTY_CONTEXT)
        assertNull(DiagramClipboard.read())
        selectPair(session.canvas)
        WriteCommandAction.runWriteCommandAction(project) { session.editor.file!!.isWritable = false }
        try {
            copy(session.canvas)
            val original = session.document.text
            assertFalse(session.canvas.pasteProvider!!.isPasteEnabled(DataContext.EMPTY_CONTEXT))
            session.canvas.pasteProvider!!.performPaste(DataContext.EMPTY_CONTEXT)
            assertEquals(original, session.document.text)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { session.editor.file!!.isWritable = true }
        }
        press(session.canvas, 70, 55)
        move(session.canvas, 80, 65)
        assertFalse(session.canvas.copyProvider!!.isCopyEnabled(DataContext.EMPTY_CONTEXT))
        assertFalse(session.canvas.pasteProvider!!.isPasteEnabled(DataContext.EMPTY_CONTEXT))
        escape(session.canvas)
    }

    fun testEscapeCancelsBatchMoveAndResizeWithoutXmlOrUndo() {
        val session = open()
        selectPair(session.canvas)
        val original = session.canvas.diagram
        val text = session.document.text
        val undoBefore = UndoManager.getInstance(project).isUndoAvailable(session.source)
        for ((x, y) in listOf(70 to 55, 120 to 55, 20 to 55)) {
            press(session.canvas, x, y)
            move(session.canvas, x + 30, y + 20)
            assertTrue(session.canvas.diagram != original)
            escape(session.canvas)
            assertEquals(original, session.canvas.diagram)
            assertFalse(session.canvas.isDragging)
            assertEquals(Cursor.DEFAULT_CURSOR, session.canvas.cursor.type)
            release(session.canvas, x + 30, y + 20)
            assertEquals(text, session.document.text)
            assertEquals(undoBefore, UndoManager.getInstance(project).isUndoAvailable(session.source))
        }
    }

    fun testEscapeCancelsWireEndpointBendpointAndMidpointPreview() {
        val session = open()
        click(session.canvas, 170, 110)
        val original = session.canvas.diagram
        val undoBefore = UndoManager.getInstance(project).isUndoAvailable(session.source)
        val path = DiagramGeometry.path(original.wires[0])
        val middle = DiagramPoint((path[0].x + path[1].x) / 2, (path[0].y + path[1].y) / 2)
        for (handle in listOf(path.first(), path.last(), path[1], middle)) {
            press(session.canvas, kotlin.math.round(handle.x).toInt(), kotlin.math.round(handle.y).toInt())
            move(session.canvas, 360, 230)
            assertTrue(session.canvas.isDragging)
            escape(session.canvas)
            assertEquals(original, session.canvas.diagram)
            assertEquals(path, session.canvas.wirePath(session.canvas.selectedWire!!))
            release(session.canvas, 360, 230)
            assertEquals(fixture(), session.document.text)
            assertEquals(undoBefore, UndoManager.getInstance(project).isUndoAvailable(session.source))
        }
    }

    fun testEscapeRestoresNavigationAndSelectionAndDoesNotUndoNewSource() {
        val session = open()
        click(session.canvas, 70, 55)
        val selected = session.canvas.selectedNodes
        press(session.canvas, 400, 240, button = MouseEvent.BUTTON3)
        move(session.canvas, 450, 290, MouseEvent.BUTTON3)
        escape(session.canvas)
        release(session.canvas, 450, 290, MouseEvent.BUTTON3)
        assertEquals(DiagramPoint(0.0, 0.0), session.canvas.pan)
        press(session.canvas, 400, 240)
        move(session.canvas, 0, 0)
        assertTrue(session.canvas.selectedNodes.size > 1)
        escape(session.canvas)
        release(session.canvas, 0, 0)
        assertEquals(selected, session.canvas.selectedNodes)
        assertNull(session.canvas.selectionBox)
        press(session.canvas, 70, 55)
        move(session.canvas, 90, 75)
        WriteCommandAction.runWriteCommandAction(project) {
            session.document.setText(session.document.text.replace("location=\"20,40\"", "location=\"30,50\""))
        }
        UIUtil.dispatchAllInvocationEvents()
        escape(session.canvas)
        release(session.canvas, 90, 75)
        assertEquals(DiagramPoint(30.0, 50.0), session.canvas.diagram.nodes.first().location)
    }

    fun testDeleteFirstWireCleansBothEndpointsAndRemapsRemainingWire() {
        val session = open()
        val original = session.document.text
        val retained = session.canvas.diagram.wires[1]
        click(session.canvas, 170, 110)
        delete(session.canvas)
        assertEquals(3, session.canvas.diagram.nodes.size)
        assertEquals(listOf("7"), session.canvas.diagram.wires.map { it.id })
        val nodes = session.canvas.diagram.nodes
        assertEmpty(nodes[0].outputs)
        assertEmpty(nodes[1].inputs)
        assertEquals(listOf(0), nodes[1].outputs)
        assertEquals(listOf(0), nodes[2].inputs)
        assertEquals(DiagramGeometry.path(retained), DiagramGeometry.path(session.canvas.diagram.wires.single()))
        assertNull(session.canvas.selectedWire)
        assertEmpty(session.canvas.selectedNodes)
        assertDeleteUndoAndSave(session, original)
    }

    fun testDeleteLastWireThroughIdeActionKeepsFirstWireXmlUnchanged() {
        val session = open()
        val original = session.document.text
        val wireBefore = root(session).subTags.first { it.localName == "wire" }.text
        click(session.canvas, 230, 120)
        assertEquals("7", session.canvas.selectedWire!!.id)
        delete(session.canvas, shortcut = false)
        assertEquals(listOf("150"), session.canvas.diagram.wires.map { it.id })
        assertEquals(wireBefore, root(session).subTags.single { it.localName == "wire" }.text)
        assertEquals(listOf(0), session.canvas.diagram.nodes[1].inputs)
        assertEmpty(session.canvas.diagram.nodes[1].outputs)
        assertEmpty(session.canvas.diagram.nodes[2].inputs)
        assertDeleteUndoAndSave(session, original)
    }

    fun testDeleteChildCascadesToWiresAndReferencesButKeepsOtherChildren() {
        val session = open()
        val original = session.document.text
        click(session.canvas, 270, 55)
        press(session.canvas, 500, 300, button = MouseEvent.BUTTON3)
        move(session.canvas, 540, 350, MouseEvent.BUTTON3)
        release(session.canvas, 540, 350, MouseEvent.BUTTON3)
        delete(session.canvas)
        assertEquals(listOf("2", "30"), session.canvas.diagram.nodes.map { it.id })
        assertEmpty(session.canvas.diagram.wires)
        session.canvas.diagram.nodes.forEach { assertEmpty(it.inputs); assertEmpty(it.outputs) }
        assertEmpty(session.canvas.selectedNodes)
        assertTrue(root(session).subTags.first { it.getAttributeValue("id") == "2" }.text.contains("value=\"복사 &amp; 보존\""))
        assertEquals(DiagramPoint(40.0, 50.0), session.canvas.pan)
        assertDeleteUndoAndSave(session, original)
        assertEquals(DiagramPoint(40.0, 50.0), session.canvas.pan)
    }

    fun testDeleteMultipleChildrenDeduplicatesSharedWiresAndKeepsClipboardSnapshot() {
        val session = open()
        val original = session.document.text
        selectPair(session.canvas)
        copy(session.canvas)
        val snapshot = DiagramClipboard.read()
        delete(session.canvas)
        assertEquals(listOf("30"), session.canvas.diagram.nodes.map { it.id })
        assertEmpty(session.canvas.diagram.wires)
        assertEmpty(session.canvas.diagram.nodes.single().inputs)
        assertSame(snapshot, DiagramClipboard.read())
        assertDeleteUndoAndSave(session, original)
        paste(session.canvas)
        assertEquals(listOf("31", "32"), session.canvas.selectedNodes.map { it.id })
        assertEquals("33", session.canvas.diagram.wires.single().id)
        assertEquals(listOf(0), session.canvas.selectedNodes.first().outputs)
        assertEquals(listOf(0), session.canvas.selectedNodes.last().inputs)
    }

    fun testDeleteRemapsNonAdjacentWiresAndUnsupportedChildReferences() {
        val text = fixture()
            .replace("output=\"//@wire.0\"", "output=\"//@wire.0 //@wire.2\"")
            .replace("output=\"//@wire.1\"", "output=\"//@wire.1 //@wire.3\"")
            .replace("id=\"30\" location=\"220,160\" input=\"//@wire.1\"", "id=\"30\" location=\"220,160\" input=\"//@wire.1\" output=\"//@wire.4\"")
            .replace("</Glue:GlueDiagram>", """<child xsi:type="Glue:FinalState" id="40" location="400,160" input="//@wire.2 //@wire.3 //@wire.4"/>
                <child xsi:type="Glue:Unknown" id="888" input="//@wire.0 //@wire.2 //@wire.4" output="//@wire.3" custom="unknown"/>
                <wire id="151" firstRelativePoint="100,55" lastRelativePoint="410,170" custom="retain"/>
                <wire id="152"/>
                <wire id="153"/>
                </Glue:GlueDiagram>""")
        val session = open(text)
        val retainedPaths = listOf(2, 4).map { DiagramGeometry.path(session.canvas.diagram.wires[it]) }
        click(session.canvas, 270, 55)
        delete(session.canvas)
        assertEquals(listOf("151", "153"), session.canvas.diagram.wires.map { it.id })
        assertEquals(retainedPaths, session.canvas.diagram.wires.map(DiagramGeometry::path))
        assertEquals(listOf(0), session.canvas.diagram.nodes.single { it.id == "2" }.outputs)
        assertEquals(listOf(1), session.canvas.diagram.nodes.single { it.id == "30" }.outputs)
        assertEquals(listOf(0, 1), session.canvas.diagram.nodes.single { it.id == "40" }.inputs)
        val unknown = root(session).subTags.first { it.getAttributeValue("id") == "888" }
        assertEquals("//@wire.0 //@wire.1", unknown.getAttributeValue("input"))
        assertNull(unknown.getAttributeValue("output"))
        assertEquals("unknown", unknown.getAttributeValue("custom"))
        assertDeleteUndoAndSave(session, text)
    }

    fun testDeleteDisabledWithoutSelectionAndForReadOnlyDraggingOrChangedSource() {
        val session = open()
        val provider = session.canvas.deleteProvider!!
        assertFalse(provider.canDeleteElement(DataContext.EMPTY_CONTEXT))
        provider.deleteElement(DataContext.EMPTY_CONTEXT)
        assertEquals(fixture(), session.document.text)
        click(session.canvas, 70, 55)
        WriteCommandAction.runWriteCommandAction(project) { session.editor.file!!.isWritable = false }
        try {
            assertFalse(provider.canDeleteElement(DataContext.EMPTY_CONTEXT))
            provider.deleteElement(DataContext.EMPTY_CONTEXT)
            assertEquals(fixture(), session.document.text)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { session.editor.file!!.isWritable = true }
        }
        press(session.canvas, 70, 55)
        move(session.canvas, 90, 75)
        assertFalse(provider.canDeleteElement(DataContext.EMPTY_CONTEXT))
        provider.deleteElement(DataContext.EMPTY_CONTEXT)
        assertEquals(fixture(), session.document.text)
        escape(session.canvas)
        release(session.canvas, 90, 75)
        val latest = fixture().replace("id=\"2\"", "id=\"200\"")
        WriteCommandAction.runWriteCommandAction(project) { session.document.setText(latest) }
        assertFalse(provider.canDeleteElement(DataContext.EMPTY_CONTEXT))
        provider.deleteElement(DataContext.EMPTY_CONTEXT)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(latest, session.document.text)
        WriteCommandAction.runWriteCommandAction(project) { session.document.setText("<GlueDiagram><child") }
        UIUtil.dispatchAllInvocationEvents()
        assertFalse(provider.canDeleteElement(DataContext.EMPTY_CONTEXT))
        provider.deleteElement(DataContext.EMPTY_CONTEXT)
        assertEquals("<GlueDiagram><child", session.document.text)
    }
}
