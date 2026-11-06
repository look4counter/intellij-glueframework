package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.modeleditor.*
import java.awt.event.MouseEvent

class ModelDiagramWireCreationTest : BasePlatformTestCase() {
    private val start = DiagramNode(DiagramNodeKind.Activity, "a", "A", DiagramPoint(20.0, 40.0), 200.0, 30.0)
    private val end = DiagramNode(DiagramNodeKind.FinalState, "b", "", DiagramPoint(320.0, 200.0), 20.0, 20.0)
    private fun canvas() = ModelDiagramCanvas().apply {
        display(ModelDiagram(listOf(start, end), emptyList()))
        setSize(600, 400)
        snapEnabled = false
        setWireCreationMode(true)
    }

    private fun mouse(canvas: ModelDiagramCanvas, type: Int, x: Int, y: Int, button: Int = MouseEvent.BUTTON1) {
        val mask = if (type == MouseEvent.MOUSE_PRESSED || type == MouseEvent.MOUSE_DRAGGED)
            if (button == MouseEvent.BUTTON1) MouseEvent.BUTTON1_DOWN_MASK else MouseEvent.BUTTON3_DOWN_MASK else 0
        canvas.dispatchEvent(MouseEvent(canvas, type, 10, mask, x, y, 1, false,
            if (type == MouseEvent.MOUSE_DRAGGED) MouseEvent.NOBUTTON else button))
    }

    private fun chooseStart(canvas: ModelDiagramCanvas) {
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 100, 55)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 120, 55)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 120, 55)
        mouse(canvas, MouseEvent.MOUSE_MOVED, 330, 210)
        assertEquals(start, canvas.wireCreationPreview?.start)
    }

    fun testMovingPressAndReleaseSelectBothEndsWithoutClickEvents() {
        val canvas = canvas()
        val created = mutableListOf<DiagramWire>()
        canvas.onWireCreateRequested = { created.add(it) }
        chooseStart(canvas)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 328, 208)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 333, 212)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 333, 212)
        assertEquals(1, created.size)
        assertEquals(start, created.single().start)
        assertEquals(end, created.single().end)
        assertEquals(DiagramPoint(333.0, 212.0), created.single().lastRelativePoint)
        assertNull(canvas.wireCreationPreview)
        assertTrue(canvas.wireCreationMode)
    }

    fun testFollowingClicksDoNotSelectAgainEvenWhenCallbackReplacesModel() {
        val canvas = canvas()
        var count = 0
        canvas.onWireCreateRequested = { count++; canvas.display(canvas.diagram) }
        chooseStart(canvas)
        mouse(canvas, MouseEvent.MOUSE_CLICKED, 120, 55)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 330, 210)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 330, 210)
        mouse(canvas, MouseEvent.MOUSE_CLICKED, 330, 210)
        mouse(canvas, MouseEvent.MOUSE_MOVED, 120, 55)
        assertEquals(1, count)
        assertNull(canvas.wireCreationPreview)
        chooseStart(canvas)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 330, 210)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 330, 210)
        assertEquals(2, count)
    }

    fun testInvalidReleaseTargetsDoNotCreateWiresAndKeepStart() {
        for ((x, y) in listOf(260 to 160, 120 to 55, 470 to 70, -10 to 55, 620 to 200)) {
            val canvas = canvas()
            var count = 0
            canvas.onWireCreateRequested = { count++ }
            chooseStart(canvas)
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 330, 210)
            mouse(canvas, MouseEvent.MOUSE_DRAGGED, x, y)
            mouse(canvas, MouseEvent.MOUSE_RELEASED, x, y)
            mouse(canvas, MouseEvent.MOUSE_CLICKED, x, y)
            assertEquals("Invalid release at $x,$y", 0, count)
            mouse(canvas, MouseEvent.MOUSE_MOVED, 330, 210)
            assertEquals(start, canvas.wireCreationPreview?.start)
        }
    }

    fun testCancelledSwitchedAndReplacedGesturesCannotSelectStaleEndpoints() {
        for (change in listOf<(ModelDiagramCanvas) -> Unit>(
            { it.cancelDrag(); it.setWireCreationMode(true) },
            { it.setWireCreationMode(false); it.setWireCreationMode(true) },
            { it.display(it.diagram) })) {
            val canvas = canvas()
            var count = 0
            canvas.onWireCreateRequested = { count++ }
            chooseStart(canvas)
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 330, 210)
            change(canvas)
            mouse(canvas, MouseEvent.MOUSE_RELEASED, 330, 210)
            mouse(canvas, MouseEvent.MOUSE_CLICKED, 330, 210)
            mouse(canvas, MouseEvent.MOUSE_MOVED, 120, 55)
            assertEquals(0, count)
            assertNull(canvas.wireCreationPreview)
        }
    }

    fun testReadOnlyAndInvalidModelsRejectReleaseSelection() {
        for (invalidModel in listOf(false, true)) {
            val canvas = canvas()
            var count = 0
            canvas.onWireCreateRequested = { count++ }
            chooseStart(canvas)
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 330, 210)
            if (invalidModel) canvas.display(canvas.diagram.copy(message = "invalid")) else canvas.canMoveNodes = { false }
            mouse(canvas, MouseEvent.MOUSE_RELEASED, 330, 210)
            mouse(canvas, MouseEvent.MOUSE_CLICKED, 330, 210)
            assertEquals(0, count)
        }
    }

    fun testRightButtonPanningDoesNotSelectWireEndpoints() {
        val canvas = canvas()
        var count = 0
        canvas.onWireCreateRequested = { count++ }
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 100, 55, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 130, 65, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 130, 65, MouseEvent.BUTTON3)
        assertEquals(DiagramPoint(30.0, 10.0), canvas.pan)
        assertNull(canvas.wireCreationPreview)
        assertEquals(0, count)
    }

    fun testZoomedReleaseUsesFinalModelCoordinates() {
        val canvas = canvas()
        canvas.setZoom(1.5)
        canvas.navigateTo(DiagramPoint(200.0, 150.0))
        var created: DiagramWire? = null
        canvas.onWireCreateRequested = { created = it }
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 140, 55)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 150, 55)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 150, 55)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 492, 288)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 498, 294)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 498, 294)
        assertEquals(end, created?.end)
        assertEquals(DiagramPoint(332.0, 212.66666666666666), created?.lastRelativePoint)
    }
}
