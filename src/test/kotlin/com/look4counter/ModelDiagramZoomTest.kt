package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.modeleditor.*
import java.awt.event.MouseEvent

class ModelDiagramZoomTest : BasePlatformTestCase() {
    private val node = DiagramNode(DiagramNodeKind.Activity, "1", "A", DiagramPoint(100.0, 100.0), 100.0, 30.0)
    private fun canvas() = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(node), emptyList())); setSize(600, 400) }
    private fun mouse(canvas: ModelDiagramCanvas, type: Int, x: Int, y: Int, alt: Boolean = false) {
        canvas.dispatchEvent(MouseEvent(canvas, type, 0, if (alt) MouseEvent.ALT_DOWN_MASK else 0,
            x, y, 1, false, MouseEvent.BUTTON1))
    }
    fun testZoomControlsPreserveViewportCenterAndResetWithoutChangingModel() {
        val canvas = canvas()
        val controls = ZoomControls(canvas)
        val original = canvas.diagram
        val initial = canvas.miniMapProjection!!.modelViewport
        assertEquals((20..150 step 10).map { "$it%" }, (0 until controls.select.itemCount).map(controls.select::getItemAt))
        assertEquals("100%", controls.select.selectedItem)
        controls.select.selectedItem = "120%"
        assertEquals(1.2, canvas.zoom)
        val zoomed = canvas.miniMapProjection!!.modelViewport
        assertEquals(initial.centerX, zoomed.centerX, 0.000001)
        assertEquals(initial.centerY, zoomed.centerY, 0.000001)
        assertEquals(500.0, zoomed.width)
        controls.select.selectedItem = "150%"
        assertEquals(1.5, canvas.zoom)
        controls.select.selectedItem = "20%"
        assertEquals(0.2, canvas.zoom)
        canvas.setZoom(9.0)
        assertEquals("150%", controls.select.selectedItem)
        canvas.setZoom(0.01)
        assertEquals("20%", controls.select.selectedItem)
        controls.select.selectedItem = "100%"
        assertEquals(1.0, canvas.zoom)
        assertEquals("100%", controls.select.selectedItem)
        assertEquals(original, canvas.diagram)
    }
    fun testZoomedMoveAndCreationUseModelCoordinates() {
        val canvas = canvas()
        canvas.setZoom(1.5)
        canvas.navigateTo(node.center)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 300, 200)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 360, 230, true)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 360, 230, true)
        assertEquals(DiagramPoint(140.0, 120.0), canvas.diagram.nodes.single().location)
        canvas.setCreationKind(DiagramNodeKind.Activity)
        mouse(canvas, MouseEvent.MOUSE_MOVED, 300, 200)
        assertEquals(node.center, canvas.ghostNode!!.center)
        var created: DiagramPoint? = null
        canvas.onChildCreateRequested = { _, point -> created = point }
        mouse(canvas, MouseEvent.MOUSE_CLICKED, 300, 200)
        assertEquals(DiagramPoint(node.center.x - 50, node.center.y - 15), created)
    }
    fun testZoomedMiniMapNavigationAndWireCreation() {
        val canvas = canvas()
        val target = node.copy(id = "2", location = DiagramPoint(300.0, 100.0))
        canvas.display(ModelDiagram(listOf(node, target), emptyList()))
        canvas.setZoom(0.5)
        canvas.navigateTo(node.center)
        canvas.setWireCreationMode(true)
        mouse(canvas, MouseEvent.MOUSE_CLICKED, 300, 200)
        mouse(canvas, MouseEvent.MOUSE_MOVED, 400, 200)
        assertEquals(target, canvas.wireCreationPreview!!.end)
        val projection = canvas.miniMapProjection!!
        val point = projection.map(target.center)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, point.x.toInt(), point.y.toInt())
        mouse(canvas, MouseEvent.MOUSE_RELEASED, point.x.toInt(), point.y.toInt())
        val viewport = canvas.miniMapProjection!!.modelViewport
        assertEquals(1200.0, viewport.width)
        assertEquals(target.center.x, viewport.centerX, 10.0)
    }
    fun testZoomedRangeSelectionAndResizeKeepModelGeometry() {
        val canvas = canvas()
        canvas.setZoom(1.5)
        canvas.navigateTo(node.center)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 180, 150)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 420, 250)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 420, 250)
        assertEquals(listOf(node), canvas.selectedNodes)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 375, 200)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 435, 200, true)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 435, 200, true)
        assertEquals(140.0, canvas.diagram.nodes.single().width)
        assertEquals(node.location, canvas.diagram.nodes.single().location)
    }
}
