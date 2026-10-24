package com.look4counter

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import com.look4counter.modeleditor.*
import java.awt.Color
import java.awt.Point
import java.awt.Rectangle
import java.awt.event.MouseEvent
import java.awt.geom.Rectangle2D
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JScrollPane

class ModelDiagramMiniMapTest : BasePlatformTestCase() {
    private fun activity(x: Double, y: Double) = DiagramNode(DiagramNodeKind.Activity, "a", "Activity", DiagramPoint(x, y), 100.0, 30.0)
    private fun state(x: Double, y: Double) = DiagramNode(DiagramNodeKind.InitialState, "b", "", DiagramPoint(x, y), 20.0, 20.0)
    private fun diagram(a: DiagramNode = activity(20.0, 40.0), b: DiagramNode = state(460.0, 60.0)): ModelDiagram {
        val wire = DiagramWire(0, "1", "guard", a, b, null, null, emptyList())
        return ModelDiagram(listOf(a, b), listOf(wire))
    }
    private fun canvas(model: ModelDiagram = diagram()) = ModelDiagramCanvas().apply { display(model); setSize(600, 400) }

    private fun mouse(canvas: ModelDiagramCanvas, id: Int, x: Int, y: Int, button: Int = MouseEvent.BUTTON1, count: Int = 1) {
        val mask = if (id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_DRAGGED)
            if (button == MouseEvent.BUTTON3) MouseEvent.BUTTON3_DOWN_MASK else MouseEvent.BUTTON1_DOWN_MASK else 0
        canvas.dispatchEvent(MouseEvent(canvas, id, 1, mask, x, y, count, false,
            if (id == MouseEvent.MOUSE_DRAGGED || id == MouseEvent.MOUSE_MOVED) MouseEvent.NOBUTTON else button))
    }
    private fun click(canvas: ModelDiagramCanvas, x: Int, y: Int, count: Int = 1) {
        mouse(canvas, MouseEvent.MOUSE_PRESSED, x, y, count = count)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, x, y, count = count)
        mouse(canvas, MouseEvent.MOUSE_CLICKED, x, y, count = count)
    }
    private fun render(canvas: ModelDiagramCanvas): BufferedImage {
        val image = BufferedImage(canvas.width, canvas.height, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { try { canvas.paint(it) } finally { it.dispose() } }
        return image
    }

    private fun assertWholeOverviewFits(projection: MiniMapProjection) {
        fun contains(bounds: Rectangle2D): Boolean {
            val area = projection.contentArea
            val tolerance = 0.000001
            return bounds.minX >= area.minX - tolerance && bounds.minY >= area.minY - tolerance &&
                bounds.maxX <= area.maxX + tolerance && bounds.maxY <= area.maxY + tolerance
        }
        assertTrue("현재 화면 사각형 전체가 미니맵 안에 있어야 한다", contains(projection.viewport))
        assertTrue("모든 도형도 미니맵 안에 있어야 한다", contains(projection.map(projection.modelBounds)))
        assertEquals(projection.modelViewport.width * projection.scale, projection.viewport.width, 0.000001)
        assertEquals(projection.modelViewport.height * projection.scale, projection.viewport.height, 0.000001)
        assertEquals(projection.modelViewport.width / projection.modelViewport.height,
            projection.viewport.width / projection.viewport.height, 0.000001)
    }

    fun testSmallDiagramAndLargeVisibleCanvasKeepWholeViewportAndAspectRatio() {
        for (visible in listOf(Rectangle(0, 0, 1200, 400), Rectangle(100, 80, 300, 900))) {
            val projection = DiagramMiniMap.layout(listOf(state(-10.0, -10.0)), emptyList(), emptyList(),
                visible, DiagramPoint(30.0, 30.0))!!
            assertWholeOverviewFits(projection)
            assertEquals(visible.width.toDouble(), projection.modelViewport.width)
            assertEquals(visible.height.toDouble(), projection.modelViewport.height)
            assertEquals(visible.x - 30.0, projection.modelViewport.x)
            assertEquals(visible.y - 30.0, projection.modelViewport.y)
        }
    }

    fun testLargeNavigationInEveryDirectionShrinksOverviewAndKeepsFrameFixed() {
        val visible = Rectangle(0, 0, 600, 400)
        val nodes = listOf(activity(20.0, 40.0), state(460.0, 60.0))
        val near = DiagramMiniMap.layout(nodes, emptyList(), emptyList(), visible, DiagramPoint(0.0, 0.0))!!
        assertWholeOverviewFits(near)
        for (translation in listOf(DiagramPoint(6000.0, 0.0), DiagramPoint(-6000.0, 0.0),
            DiagramPoint(0.0, 6000.0), DiagramPoint(0.0, -6000.0), DiagramPoint(-6000.0, 6000.0))) {
            val far = DiagramMiniMap.layout(nodes, emptyList(), emptyList(), visible, translation)!!
            assertWholeOverviewFits(far)
            assertEquals(near.frame, far.frame)
            assertTrue("먼 화면까지 포함하도록 축소해야 한다", far.scale < near.scale)
            assertEquals(-translation.x, far.modelViewport.x, 0.000001)
            assertEquals(-translation.y, far.modelViewport.y, 0.000001)
        }
        assertEquals(near, DiagramMiniMap.layout(nodes, emptyList(), emptyList(), visible, DiagramPoint(0.0, 0.0)))
    }

    fun testViewportUsesScrollExtentAfterResizeAndNavigation() {
        val canvas = canvas(diagram(activity(-100.0, -60.0), state(1000.0, 800.0)))
        val scroll = JScrollPane(canvas)
        scroll.setSize(420, 300)
        scroll.doLayout()
        canvas.setSize(canvas.preferredSize)
        scroll.viewport.viewPosition = Point(100, 80)
        val first = canvas.miniMapProjection!!
        assertWholeOverviewFits(first)
        assertEquals(scroll.viewport.extentSize.width.toDouble(), first.modelViewport.width)
        assertEquals(scroll.viewport.extentSize.height.toDouble(), first.modelViewport.height)
        scroll.setSize(640, 480)
        scroll.doLayout()
        scroll.viewport.viewPosition = Point(200, 150)
        val resized = canvas.miniMapProjection!!
        assertWholeOverviewFits(resized)
        assertTrue(resized.modelViewport.width > first.modelViewport.width)
        assertTrue(resized.modelViewport.height > first.modelViewport.height)
        assertEquals(scroll.viewport.extentSize.width.toDouble(), resized.modelViewport.width)
        assertEquals(scroll.viewport.extentSize.height.toDouble(), resized.modelViewport.height)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 250, 350, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 6250, -5650, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 6250, -5650, MouseEvent.BUTTON3)
        val navigated = canvas.miniMapProjection!!
        assertWholeOverviewFits(navigated)
        assertEquals(resized.frame, navigated.frame)
        assertTrue(navigated.scale < resized.scale)
        assertEquals(resized.modelViewport.width, navigated.modelViewport.width)
        assertEquals(resized.modelViewport.height, navigated.modelViewport.height)
    }

    fun testNavigationRendersAllFourBlueViewportEdgesWithoutChangingDiagram() {
        val canvas = canvas()
        val original = canvas.diagram
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 200, 200, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 1400, 1000, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 1400, 1000, MouseEvent.BUTTON3)
        val projection = canvas.miniMapProjection!!
        assertWholeOverviewFits(projection)
        val viewport = projection.viewport
        val image = render(canvas)
        val samples = listOf(DiagramPoint(viewport.centerX, viewport.minY), DiagramPoint(viewport.centerX, viewport.maxY),
            DiagramPoint(viewport.minX, viewport.centerY), DiagramPoint(viewport.maxX, viewport.centerY))
        for (sample in samples) {
            val x = sample.x.toInt(); val y = sample.y.toInt()
            assertTrue("현재 화면 사각형의 네 변을 모두 그려야 한다", (-1..1).any { dx -> (-1..1).any { dy ->
                val pixel = Color(image.getRGB(x + dx, y + dy), true)
                pixel.blue > pixel.red + 30 && pixel.blue > pixel.green + 30
            } })
        }
        Files.createDirectories(Path.of("build/reports"))
        ImageIO.write(image, "png", Path.of("build/reports/model-minimap-navigation.png").toFile())
        assertEquals(original, canvas.diagram)
        assertEmpty(canvas.selectedNodes)
    }

    fun testProjectionFitsAllNodesWiresAndLabelsIncludingNegativeCoordinates() {
        val a = activity(-100.0, -60.0)
        val b = state(500.0, 300.0)
        val path = listOf(a.center, DiagramPoint(-250.0, 400.0), b.center)
        val label = Rectangle2D.Double(700.0, 100.0, 120.0, 20.0)
        val visible = Rectangle(120, 80, 600, 400)
        val projection = DiagramMiniMap.layout(listOf(a, b), listOf(path), listOf(label), visible, DiagramPoint(20.0, -40.0))!!
        assertEquals(visible.x + visible.width - 12, projection.frame.x + projection.frame.width)
        assertEquals(visible.y + 12, projection.frame.y)
        assertTrue(projection.frame.width <= 200 && projection.frame.height <= 140)
        assertTrue(projection.scale > 0 && projection.scale <= 0.5)
        assertEquals(minOf(0.5, projection.contentArea.width / projection.overviewBounds.width,
            projection.contentArea.height / projection.overviewBounds.height), projection.scale, 0.000001)
        val points = path + listOf(a.location, a.location + DiagramPoint(a.width, a.height), b.location,
            b.location + DiagramPoint(20.0, 20.0), DiagramPoint(label.maxX, label.maxY))
        points.forEach {
            val mapped = projection.map(it)
            assertTrue(projection.contentArea.contains(mapped.x, mapped.y))
        }
        assertEquals(Rectangle2D.Double(100.0, 120.0, 600.0, 400.0), projection.modelViewport)
        val expected = projection.map(DiagramPoint(100.0, 120.0))
        assertEquals(expected.x, projection.viewport.x)
        assertEquals(expected.y, projection.viewport.y)
    }

    fun testMiniMapHidesForEmptyInvalidAndTinyCanvasAndHandlesSingleNode() {
        val canvas = canvas(ModelDiagram(emptyList(), emptyList()))
        assertNull(canvas.miniMapProjection)
        canvas.display(ModelDiagram(emptyList(), emptyList(), "invalid"))
        assertNull(canvas.miniMapProjection)
        canvas.display(ModelDiagram(listOf(state(-10.0, -10.0)), emptyList()))
        assertNotNull(canvas.miniMapProjection)
        assertTrue(canvas.miniMapProjection!!.scale.isFinite())
        canvas.setSize(150, 100)
        assertNull(canvas.miniMapProjection)
    }

    fun testMiniMapStaysAtVisibleTopRightThroughPanAndScroll() {
        val canvas = canvas(diagram(activity(-100.0, -60.0), state(1000.0, 800.0)))
        val scroll = JScrollPane(canvas)
        scroll.setSize(420, 300)
        scroll.doLayout()
        canvas.setSize(canvas.preferredSize)
        scroll.viewport.viewPosition = Point(100, 80)
        val first = canvas.miniMapProjection!!
        assertEquals(12, first.frame.y - canvas.visibleRect.y)
        scroll.viewport.viewPosition = Point(200, 150)
        val second = canvas.miniMapProjection!!
        assertEquals(first.frame.x - 100, second.frame.x - 200)
        assertEquals(first.frame.y - 80, second.frame.y - 150)
        assertEquals(100.0, second.modelViewport.x - first.modelViewport.x)
        assertEquals(70.0, second.modelViewport.y - first.modelViewport.y)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 250, 350, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 280, 390, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 280, 390, MouseEvent.BUTTON3)
        val third = canvas.miniMapProjection!!
        assertEquals(second.frame, third.frame)
        assertEquals(-30.0, third.modelViewport.x - second.modelViewport.x)
        assertEquals(-40.0, third.modelViewport.y - second.modelViewport.y)
    }

    fun testMiniMapFollowsMoveResizeAndSourceModelChanges() {
        val model = diagram(activity(20.0, 40.0), state(400.0, 240.0))
        val canvas = canvas(model)
        val before = canvas.miniMapProjection!!
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 90, 75)
        val moved = canvas.miniMapProjection!!
        assertEquals(before.frame, moved.frame)
        assertTrue(before.modelBounds.x != moved.modelBounds.x)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 90, 75)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 140, 75)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 540, 75)
        val resized = canvas.miniMapProjection!!
        assertTrue(resized.modelBounds.width > moved.modelBounds.width)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 540, 75)
        canvas.display(ModelDiagram(emptyList(), emptyList()))
        assertNull(canvas.miniMapProjection)
        canvas.display(model)
        assertEquals(before.modelBounds, canvas.miniMapProjection!!.modelBounds)
    }

    fun testMiniMapLeftClicksDoNotSelectEditOrCreateAndRightDragStillPans() {
        val canvas = canvas()
        var edits = 0
        var created = 0
        canvas.onActivityEditRequested = { edits++ }
        canvas.onWireEditRequested = { edits++ }
        canvas.onChildCreateRequested = { _, _ -> created++ }
        val original = canvas.diagram
        click(canvas, 470, 70)
        click(canvas, 470, 70, 2)
        assertEmpty(canvas.selectedNodes)
        assertNull(canvas.selectedWire)
        assertEquals(0, edits)
        canvas.setCreationKind(DiagramNodeKind.Activity)
        mouse(canvas, MouseEvent.MOUSE_MOVED, 470, 70)
        assertNull(canvas.ghostNode)
        click(canvas, 470, 70)
        assertEquals(0, created)
        assertEquals(original, canvas.diagram)
        val beforeRightDrag = canvas.pan
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 470, 70, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 500, 110, MouseEvent.BUTTON3)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 500, 110, MouseEvent.BUTTON3)
        assertEquals(beforeRightDrag + DiagramPoint(30.0, 40.0), canvas.pan)
        assertNull(canvas.ghostNode)
    }

    fun testMiniMapDoesNotInterruptAnExistingChildDragAcrossItsArea() {
        val canvas = canvas()
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 470, 70)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 470, 70)
        assertEquals(DiagramPoint(420.0, 55.0), canvas.diagram.nodes.first().location)
        assertEquals(100.0, canvas.diagram.nodes.first().width)
    }

    fun testMiniMapIsTranslucentAndRendersWithoutXmlEdits() {
        val file = myFixture.addFileToProject("minimap.glue_uml_ad", """<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <child xsi:type="Activity" id="1" name="Activity" location="20,40" size="100,-1" output="//@wire.0"/>
            <child xsi:type="InitialState" id="2" location="460,60" input="//@wire.0"/>
            <wire id="3" guardcondition="guard"/>
            </GlueDiagram>""".trimIndent()).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val source = ModelSourceEditorProvider().createEditor(project, file) as TextEditor
        try {
            editor.component.setSize(640, 480)
            editor.component.doLayout()
            val scroll = editor.component.components.filterIsInstance<JScrollPane>().single()
            scroll.doLayout()
            val canvas = scroll.viewport.view as ModelDiagramCanvas
            canvas.setSize(600, 400)
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            val stamp = document.modificationStamp
            val original = document.text
            assertEquals(DiagramPoint(470.0, 70.0), canvas.diagram.nodes.last().center)
            assertTrue(canvas.miniMapProjection!!.frame.contains(470, 70))
            val image = render(canvas)
            Files.createDirectories(Path.of("build/reports"))
            ImageIO.write(image, "png", Path.of("build/reports/model-minimap.png").toFile())
            assertTrue(image.getRGB(470, 70) != Color.BLACK.rgb)
            assertTrue(image.getRGB(470, 70) != canvas.background.rgb)
            assertEquals(stamp, document.modificationStamp)
            assertEquals(original, document.text)
            assertEmpty(canvas.selectedNodes)
        } finally {
            Disposer.dispose(source)
            Disposer.dispose(editor)
            UIUtil.dispatchAllInvocationEvents()
        }
    }
}
