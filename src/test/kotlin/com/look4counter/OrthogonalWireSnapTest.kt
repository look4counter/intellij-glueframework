package com.look4counter

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import com.intellij.util.xmlb.XmlSerializer
import com.look4counter.modeleditor.*
import java.awt.Component
import java.awt.Container
import java.awt.event.MouseEvent
import kotlin.math.roundToInt

class OrthogonalWireSnapTest : BasePlatformTestCase() {
    private lateinit var previous: ModelSnapState
    private val editors = mutableListOf<FileEditor>()
    private var sequence = 0
    private val xml = """<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <child xsi:type="InitialState" id="a" location="0,0" size="20,20" output="//@wire.0"/>
        <child xsi:type="FinalState" id="b" location="200,200" size="20,20" input="//@wire.0"/>
        <wire id="wire" custom="keep"><bendpoint firstDimension="90,70" secondDimension="-110,-130" weight="0.2" custom="bend"/></wire>
        </GlueDiagram>"""

    override fun setUp() {
        super.setUp()
        previous = ModelSnapSettings.getInstance().state
        ModelSnapSettings.getInstance().loadState(ModelSnapState())
    }
    override fun tearDown() {
        try {
            editors.asReversed().forEach(Disposer::dispose)
            ModelSnapSettings.getInstance().loadState(previous)
        } finally { super.tearDown() }
    }
    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }
    private fun open(): FileEditor {
        val file = myFixture.addFileToProject("orthogonal-${sequence++}.glue_uml_ad", xml).virtualFile
        return ModelDesignEditorProvider().createEditor(project, file).also { editors += it }
    }
    private fun canvas(editor: FileEditor) = descendants(editor.component).filterIsInstance<ModelDiagramCanvas>().single()
        .apply { setSize(600, 400) }
    private fun toolbar(editor: FileEditor) = descendants(editor.component).filterIsInstance<ChildCreationToolbar>().single()
    private fun toggle(editor: FileEditor, value: Boolean) {
        val action = toolbar(editor).orthogonalAction
        action.setSelected(AnActionEvent.createFromAnAction(action, null, ActionPlaces.TOOLBAR, DataContext.EMPTY_CONTEXT), value)
    }
    private fun mouse(canvas: ModelDiagramCanvas, type: Int, x: Double, y: Double, alt: Boolean = false) {
        val point = canvas.screenOffset + DiagramPoint(x * canvas.zoom, y * canvas.zoom)
        val modifiers = (if (alt) MouseEvent.ALT_DOWN_MASK else 0) or
            (if (type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED) MouseEvent.BUTTON1_DOWN_MASK else 0)
        canvas.dispatchEvent(MouseEvent(canvas, type, System.currentTimeMillis(), modifiers, point.x.roundToInt(), point.y.roundToInt(),
            1, false, if (type == MouseEvent.MOUSE_DRAGGED) MouseEvent.NOBUTTON else MouseEvent.BUTTON1))
    }
    private fun bend(canvas: ModelDiagramCanvas) = DiagramGeometry.path(canvas.diagram.wires.single())[1]

    fun testMagnetEngagementRetentionReleaseAndIndependentAxes() {
        val snap = OrthogonalWireSnap()
        val anchors = listOf(DiagramPoint(100.0, 200.0))
        assertEquals(DiagramPoint(100.0, 200.0), snap.position(DiagramPoint(106.0, 194.0), anchors, 1.0))
        assertEquals(DiagramPoint(100.0, 200.0), snap.position(DiagramPoint(110.0, 190.0), anchors, 1.0))
        assertEquals(DiagramPoint(111.0, 200.0), snap.position(DiagramPoint(111.0, 191.0), anchors, 1.0))
        assertEquals(DiagramPoint(107.0, 200.0), snap.position(DiagramPoint(107.0, 197.0), anchors, 1.0))
        snap.reset()
        assertEquals(DiagramPoint(109.0, 200.0), snap.position(DiagramPoint(109.0, 201.0), anchors, 1.0))
    }

    fun testZoomNegativeCoordinatesAndFallbackSnapAreIndependent() {
        for (zoom in listOf(0.2, 0.5, 1.0, 1.5)) {
            val snap = OrthogonalWireSnap()
            val anchor = DiagramPoint(-100.0, -200.0)
            assertEquals(anchor, snap.position(anchor + DiagramPoint(6 / zoom, -6 / zoom), listOf(anchor), zoom))
            assertEquals(anchor, snap.position(anchor + DiagramPoint(10 / zoom, -10 / zoom), listOf(anchor), zoom))
            val far = anchor + DiagramPoint(11 / zoom, -11 / zoom)
            assertEquals(far, snap.position(far, listOf(anchor), zoom))
            snap.reset()
            assertEquals(DiagramPoint(-100.0, 999.0), snap.position(DiagramPoint(-99.0, 500.0), listOf(anchor), zoom,
                DiagramPoint(777.0, 999.0)))
        }
    }

    fun testActivityNeighborUsesCenterAxisAfterContactRecalculation() {
        val start = DiagramNode(DiagramNodeKind.Activity, "a", "A", DiagramPoint(20.0, 40.0), 100.0, 30.0)
        val end = start.copy(id = "b", location = DiagramPoint(320.0, 240.0))
        val wire = DiagramWire(0, "wire", "success", start, end, null, null,
            listOf(DiagramBendpoint(DiagramPoint(100.0, 100.0), DiagramPoint(-200.0, -100.0), 0.2)))
        val neighbors = OrthogonalWireSnap.neighbors(wire, 0, false)
        assertEquals(listOf(start.center, end.center), neighbors)
        val snapped = OrthogonalWireSnap().position(DiagramPoint(167.0, 59.0), neighbors, 1.0)
        val moved = DiagramEdits.dragBendpoint(wire, 0, snapped)
        val path = DiagramGeometry.path(moved)
        assertEquals(55.0, path[1].y)
        assertEquals(path[0].y, path[1].y)
    }

    fun testToolbarOrderSharedStatePersistenceAndSnapIndependence() {
        val first = open()
        val second = open()
        val actions = toolbar(first).toolbar.actionGroup.getChildren(null).toList()
        val snapIndex = actions.indexOf(toolbar(first).snapAction)
        assertSame(toolbar(first).orthogonalAction, actions[snapIndex + 1])
        assertFalse(canvas(first).orthogonalEnabled)
        val before = FileDocumentManager.getInstance().getDocument(first.file!!)!!.text
        toggle(first, true)
        assertTrue(canvas(first).orthogonalEnabled)
        assertTrue(canvas(second).orthogonalEnabled)
        assertFalse(ModelSnapSettings.getInstance().enabled)
        assertTrue(canvas(open()).orthogonalEnabled)
        assertEquals(before, FileDocumentManager.getInstance().getDocument(first.file!!)!!.text)
        val settings = ModelSnapSettings.getInstance()
        val state = XmlSerializer.serialize(settings.state)
        settings.loadState(ModelSnapState())
        assertFalse(canvas(first).orthogonalEnabled)
        settings.loadState(XmlSerializer.deserialize(state, ModelSnapState::class.java))
        assertTrue(canvas(first).orthogonalEnabled)
        assertFalse(settings.enabled)
        settings.enabled = true
        toggle(second, false)
        assertTrue(settings.enabled)
        assertFalse(canvas(first).orthogonalEnabled)
    }

    fun testBendpointPreviewSticksThenReleasesAndCancelRestoresOriginal() {
        val editor = open()
        val canvas = canvas(editor)
        val original = canvas.diagram
        toggle(editor, true)
        canvas.selectWireByIndex(0)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 100.0, 80.0)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 207.0, 83.0)
        assertEquals(DiagramPoint(210.0, 83.0), bend(canvas))
        val path = DiagramGeometry.path(canvas.diagram.wires.single())
        assertEquals(path[1].x, path[2].x)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 219.0, 83.0)
        assertEquals(210.0, bend(canvas).x)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 221.0, 83.0)
        assertEquals(221.0, bend(canvas).x)
        canvas.cancelDrag()
        assertEquals(original, canvas.diagram)
    }

    fun testAltAndToggleOffImmediatelyReleaseCurrentBendpoint() {
        val editor = open()
        val canvas = canvas(editor)
        toggle(editor, true)
        canvas.selectWireByIndex(0)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 100.0, 80.0)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 207.0, 83.0)
        assertEquals(210.0, bend(canvas).x)
        toggle(editor, false)
        assertEquals(207.0, bend(canvas).x)
        toggle(editor, true)
        assertEquals(210.0, bend(canvas).x)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 207.0, 83.0, alt = true)
        assertEquals(207.0, bend(canvas).x)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 207.0, 83.0, alt = true)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(207.0, bend(canvas).x)
    }

    fun testSegmentMidpointInsertsSnappedBendAndNextGestureDoesNotRetainMagnet() {
        val editor = open()
        val canvas = canvas(editor)
        toggle(editor, true)
        canvas.selectWireByIndex(0)
        val path = DiagramGeometry.path(canvas.diagram.wires.single())
        val midpoint = DiagramPoint((path[0].x + path[1].x) / 2, (path[0].y + path[1].y) / 2)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, midpoint.x, midpoint.y)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 103.0, 40.0)
        val preview = DiagramGeometry.path(canvas.diagram.wires.single())
        assertEquals(4, preview.size)
        assertEquals(DiagramPoint(100.0, 40.0), preview[1])
        assertEquals(preview[1].x, preview[2].x)
        canvas.cancelDrag()
        canvas.selectWireByIndex(0)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 100.0, 80.0)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 219.0, 83.0)
        assertEquals(219.0, bend(canvas).x)
        canvas.cancelDrag()
    }

    fun testSnappedReleasePersistsMetadataAndSupportsUndoRedoSave() {
        val editor = open()
        val source = ModelSourceEditorProvider().createEditor(project, editor.file!!) as TextEditor
        editors += source
        EditorTestUtil.waitForLoading(source.editor)
        val canvas = canvas(editor)
        toggle(editor, true)
        val original = source.editor.document.text
        canvas.selectWireByIndex(0)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 100.0, 80.0)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 207.0, 83.0)
        val preview = bend(canvas)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 207.0, 83.0)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(preview, bend(canvas))
        assertEquals(0.2, canvas.diagram.wires.single().bendpoints.single().weight)
        val changed = source.editor.document.text
        assertTrue(changed.contains("custom=\"bend\""))
        UndoManager.getInstance(project).undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, source.editor.document.text)
        UndoManager.getInstance(project).redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(changed, source.editor.document.text)
        assertEquals(preview, bend(canvas))
        FileDocumentManager.getInstance().saveDocument(source.editor.document)
        assertEquals(changed, String(editor.file!!.contentsToByteArray(), Charsets.UTF_8))
    }
}
