package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.modeleditor.*
import java.awt.Color
import java.awt.Font
import java.awt.Rectangle
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.nio.file.Files
import java.nio.file.Path
import kotlin.system.measureNanoTime
import javax.swing.JComponent
import javax.swing.RepaintManager

class ModelDiagramPerformanceTest : BasePlatformTestCase() {
    fun testEditingDragRepaintsOnlyChangedAreaWithoutTrails() {
        for (scale in listOf(0.5, 1.0, 1.5)) {
            val node = DiagramNode(DiagramNodeKind.Activity, "a", "Activity", DiagramPoint(60.0, 160.0), 100.0, 30.0)
            val end = node.copy(id = "b", location = DiagramPoint(240.0, 160.0))
            val wire = DiagramWire(0, "w", "a long wire condition", node, end, null, null,
                listOf(DiagramBendpoint(DiagramPoint(90.0, 25.0), DiagramPoint(-90.0, 25.0), 0.5)))
            // Move, resize, bendpoint and endpoint gestures, with a connected labelled wire.
            for ((x, y) in listOf(90 to 175, 160 to 175, 200 to 200, 160 to 182)) {
                val canvas = canvas(ModelDiagram(listOf(node, end), listOf(wire)))
                canvas.setZoom(scale)
                canvas.navigateTo(DiagramPoint(200.0, 160.0))
                canvas.selectNodesByIds(setOf("a"))
                if (x == 200 || y == 182) canvas.selectWireByIndex(0)
                val position = if (y == 182) canvas.wirePath(wire).first() else DiagramPoint(x.toDouble(), y.toDouble())
                val press = java.awt.Point((canvas.pan.x + position.x * canvas.zoom).toInt(),
                    (canvas.pan.y + position.y * canvas.zoom).toInt())
                mouse(canvas, MouseEvent.MOUSE_PRESSED, press.x, press.y)
                var image = render(canvas)
                val previous = RepaintManager.currentManager(canvas)
                var dirty: Rectangle? = null
                RepaintManager.setCurrentManager(object : RepaintManager() {
                    override fun addDirtyRegion(component: JComponent, x: Int, y: Int, w: Int, h: Int) {
                        if (component === canvas && w > 0 && h > 0)
                            dirty = dirty?.union(Rectangle(x, y, w, h)) ?: Rectangle(x, y, w, h)
                    }
                })
                try {
                    repeat(3) { step ->
                        dirty = null
                        mouse(canvas, MouseEvent.MOUSE_DRAGGED, press.x + 20 + step * 5, press.y + 20 + step * 3)
                        assertTrue("Gesture must edit geometry: scale=$scale gesture=$x,$y",
                            canvas.diagram != ModelDiagram(listOf(node, end), listOf(wire)) ||
                                canvas.wirePath(canvas.diagram.wires.single()) != DiagramGeometry.path(wire))
                        val region = dirty ?: error("Editing did not repaint")
                        assertTrue("Editing must use a partial repaint: $region", region.width * region.height < canvas.width * canvas.height)
                        println("drag scale=$scale gesture=$x,$y step=$step dirtyPixels=${region.width * region.height} fullPixels=${canvas.width * canvas.height}")
                        val graphics = image.createGraphics()
                        try { graphics.clip = region; canvas.paint(graphics) } finally { graphics.dispose() }
                        val expected = render(canvas)
                        for (px in 0 until canvas.width) for (py in 0 until canvas.height)
                            assertEquals("Drag trail at scale=$scale gesture=$x,$y step=$step pixel=$px,$py",
                                expected.getRGB(px, py), image.getRGB(px, py))
                        image = expected
                    }
                } finally { RepaintManager.setCurrentManager(previous); canvas.cancelDrag() }
            }
        }
    }

    fun testRemovingAlignmentGuidesRequestsFullRepaint() {
        val node = DiagramNode(DiagramNodeKind.Activity, "a", "A", DiagramPoint(20.0, 40.0), 100.0, 30.0)
        val canvas = canvas(ModelDiagram(listOf(node, node.copy(id = "b", location = DiagramPoint(220.0, 140.0))), emptyList()))
        canvas.snapEnabled = true
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 0, MouseEvent.BUTTON1_DOWN_MASK,
            167, 153, 0, false, MouseEvent.NOBUTTON))
        assertTrue(canvas.alignmentGuides.isNotEmpty())
        render(canvas)
        val previous = RepaintManager.currentManager(canvas)
        var dirty: Rectangle? = null
        RepaintManager.setCurrentManager(object : RepaintManager() {
            override fun addDirtyRegion(component: JComponent, x: Int, y: Int, w: Int, h: Int) {
                if (component === canvas && w > 0 && h > 0)
                    dirty = dirty?.union(Rectangle(x, y, w, h)) ?: Rectangle(x, y, w, h)
            }
        })
        try {
            mouse(canvas, MouseEvent.MOUSE_DRAGGED, 170, 157)
            assertTrue(canvas.alignmentGuides.isEmpty())
            assertEquals(Rectangle(0, 0, canvas.width, canvas.height), dirty)
        } finally { RepaintManager.setCurrentManager(previous); canvas.cancelDrag() }
    }

    fun testGhostMovementRepaintsOnlyChangedAreaWithoutTrails() {
        val canvas = canvas(model(2000))
        canvas.setCreationKind(DiagramNodeKind.Activity)
        val previous = RepaintManager.currentManager(canvas)
        var dirty: Rectangle? = null
        val manager = object : RepaintManager() {
            override fun addDirtyRegion(component: JComponent, x: Int, y: Int, w: Int, h: Int) {
                if (component === canvas && w > 0 && h > 0) {
                    val next = Rectangle(x, y, w, h)
                    dirty = dirty?.union(next) ?: next
                }
            }
        }
        RepaintManager.setCurrentManager(manager)
        try {
            for (scale in listOf(0.5, 1.0, 2.0)) {
                canvas.setZoom(scale)
                mouse(canvas, MouseEvent.MOUSE_MOVED, 200, 150)
                val before = render(canvas)
                dirty = null
                mouse(canvas, MouseEvent.MOUSE_MOVED, 215, 160)
                val region = dirty ?: error("Ghost movement did not request repaint")
                assertTrue("Ghost must use a partial repaint: $region", region.width * region.height < canvas.width * canvas.height / 2)
                val graphics = before.createGraphics()
                try { graphics.clip = region; canvas.paint(graphics) } finally { graphics.dispose() }
                val expected = render(canvas)
                for (x in 0 until canvas.width) for (y in 0 until canvas.height)
                    assertEquals("Ghost trail at $scale: $x,$y", expected.getRGB(x, y), before.getRGB(x, y))
                assertEquals(2000, canvas.diagram.nodes.size)
            }
        } finally { RepaintManager.setCurrentManager(previous) }
    }

    private fun model(count: Int): ModelDiagram {
        val nodes = (0 until count).map { index ->
            DiagramNode(DiagramNodeKind.Activity, "n$index", "Activity $index",
                DiagramPoint(20.0 + index % 40 * 160, 40.0 + index / 40 * 70), 100.0, 30.0,
                inputs = if (index > 0) listOf(index - 1) else emptyList(),
                outputs = if (index < count - 1) listOf(index) else emptyList())
        }
        return ModelDiagram(nodes, nodes.zipWithNext().mapIndexed { index, (a, b) ->
            DiagramWire(index, "w$index", "success", a, b, null, null, emptyList())
        })
    }

    private fun canvas(model: ModelDiagram) = ModelDiagramCanvas().apply {
        font = Font("Dialog", Font.PLAIN, 12)
        background = Color(242, 242, 242)
        foreground = Color.BLACK
        display(model)
        setSize(600, 400)
    }

    private fun render(canvas: ModelDiagramCanvas, clip: Rectangle? = null): BufferedImage {
        val image = BufferedImage(canvas.width, canvas.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try { if (clip != null) graphics.clip = clip; canvas.paint(graphics) } finally { graphics.dispose() }
        return image
    }

    private fun mouse(canvas: ModelDiagramCanvas, type: Int, x: Int, y: Int) {
        canvas.dispatchEvent(MouseEvent(canvas, type, 0, MouseEvent.BUTTON1_DOWN_MASK or MouseEvent.ALT_DOWN_MASK,
            x, y, if (type == MouseEvent.MOUSE_DRAGGED) 0 else 1, false,
            if (type == MouseEvent.MOUSE_DRAGGED) MouseEvent.NOBUTTON else MouseEvent.BUTTON1))
    }

    fun testLargeModelMeasurement() {
        val lines = mutableListOf("nodes,paint_ms,projection_100_ms,drag_15_ms,paint_node_calls")
        for (count in listOf(500, 2000)) {
            val canvas = canvas(model(count))
            var calls = 0
            canvas.activityColor = { calls++; Color.WHITE }
            repeat(3) { render(canvas) }
            calls = 0
            val paint = (0 until 7).map { measureNanoTime { render(canvas) } }.sorted()[3] / 1e6
            val draws = calls / 7
            val projection = measureNanoTime { repeat(100) { assertNotNull(canvas.miniMapProjection) } } / 1e6
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
            val drag = measureNanoTime {
                repeat(15) { mouse(canvas, MouseEvent.MOUSE_DRAGGED, 90 + it, 75 + it) }
            } / 1e6
            canvas.cancelDrag()
            assertEquals(model(count), canvas.diagram)
            lines.add("$count,$paint,$projection,$drag,$draws")
        }
        val tag = System.getenv("GLUE_CANVAS_BENCHMARK")?.takeIf { it.matches(Regex("[a-z]+")) } ?: "current"
        val output = Path.of("build/reports/model-canvas-$tag.csv")
        Files.createDirectories(output.parent)
        Files.write(output, lines)
        println(lines.joinToString("\n"))
    }

    fun testMiniMapProjectionAndImageAreReusedAndStyleChangesInvalidate() {
        val canvas = canvas(model(2000))
        val projection = canvas.miniMapProjection
        repeat(100) { assertSame(projection, canvas.miniMapProjection) }
        var calls = 0
        canvas.activityColor = { calls++; Color.WHITE }
        render(canvas)
        calls = 0
        repeat(3) { render(canvas) }
        assertTrue("Repeated paints must not draw the entire minimap: $calls", calls < 600)
        val before = render(canvas)
        canvas.activityColor = { Color.RED }
        val changed = render(canvas)
        val mini = canvas.miniMapProjection!!.contentArea.bounds
        assertTrue((mini.x until mini.x + mini.width).any { x ->
            (mini.y until mini.y + mini.height).any { y -> before.getRGB(x, y) != changed.getRGB(x, y) }
        })
        canvas.setZoom(0.5)
        assertEquals(0.5, canvas.zoom)
        assertNotSame(projection, canvas.miniMapProjection)
        canvas.font = Font("Dialog", Font.BOLD, 16)
        render(canvas)
        canvas.changeHighlights = DiagramChangeHighlights(nodeChanges = mapOf("n0" to DiagramChangeKind.Modified))
        render(canvas)
    }

    fun testWarmPaintSkipsOffscreenNodesAndPartialClipMatchesFullPaint() {
        val canvas = canvas(model(2000))
        var calls = 0
        canvas.activityColor = { calls++; Color.WHITE }
        render(canvas)
        calls = 0
        val full = render(canvas)
        assertTrue("Only visible nodes should be painted: $calls", calls < 200)
        val clip = Rectangle(0, 100, 280, 160)
        val partial = render(canvas, clip)
        for (x in clip.x until clip.x + clip.width) for (y in clip.y until clip.y + clip.height)
            assertEquals("pixel $x,$y", full.getRGB(x, y), partial.getRGB(x, y))
    }

    fun testChildDragKeepsUnrelatedWiresAndPathsAndCancelRestoresOriginal() {
        val original = model(2000)
        val canvas = canvas(original)
        val unrelated = original.wires[100]
        val path = canvas.wirePath(unrelated)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 90, 75)
        assertSame(unrelated, canvas.diagram.wires[100])
        assertSame(path, canvas.wirePath(canvas.diagram.wires[100]))
        assertNotSame(original.wires[0], canvas.diagram.wires[0])
        assertEquals(DiagramPoint(40.0, 60.0), canvas.diagram.nodes[0].location)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 100, 85)
        assertSame(unrelated, canvas.diagram.wires[100])
        assertEquals(DiagramPoint(50.0, 70.0), canvas.diagram.nodes[0].location)
        canvas.cancelDrag()
        assertEquals(original, canvas.diagram)
        assertFalse(canvas.isDragging)
    }

    fun testCrossingWireAndOffscreenPathLabelRemainVisible() {
        val left = DiagramNode(DiagramNodeKind.Activity, "a", "A", DiagramPoint(-300.0, 150.0), 100.0, 30.0)
        val right = left.copy(id = "b", location = DiagramPoint(900.0, 150.0))
        val wire = DiagramWire(0, "w", "a very long condition label extending into the viewport", left, right,
            DiagramPoint(-200.0, 165.0), DiagramPoint(900.0, 165.0), emptyList(), true)
        val canvas = canvas(ModelDiagram(listOf(left, right), listOf(wire)))
        canvas.navigateTo(DiagramPoint(300.0, 165.0))
        val image = render(canvas)
        assertTrue((0 until 400).any { x -> image.getRGB(x, 200) != canvas.background.rgb })
        val clip = Rectangle(100, 180, 180, 50)
        val partial = render(canvas, clip)
        for (x in clip.x until clip.x + clip.width) for (y in clip.y until clip.y + clip.height)
            assertEquals(image.getRGB(x, y), partial.getRGB(x, y))
    }

    private fun assertMiniMapMatchesFreshScene(current: ModelDiagramCanvas) {
        val actual = render(current)
        val fresh = canvas(current.diagram).apply {
            activityColor = current.activityColor
            activityForeground = current.activityForeground
            font = current.font
            background = current.background
            foreground = current.foreground
            changeHighlights = current.changeHighlights
        }
        val expected = render(fresh)
        val frame = current.miniMapProjection!!.frame
        assertEquals(fresh.miniMapProjection, current.miniMapProjection)
        for (x in frame.x until frame.x + frame.width) for (y in frame.y until frame.y + frame.height)
            assertEquals("minimap pixel $x,$y", expected.getRGB(x, y), actual.getRGB(x, y))
    }

    fun testPartialMiniMapMoveResizeAndWireRefreshMatchesFullRendering() {
        val original = model(2000)
        for (kind in listOf("move", "resize", "wire")) {
            val canvas = canvas(original)
            var calls = 0
            canvas.activityColor = { calls++; Color.WHITE }
            render(canvas)
            calls = 0
            when (kind) {
                "move" -> {
                    mouse(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
                    mouse(canvas, MouseEvent.MOUSE_DRAGGED, 90, 75)
                }
                "resize" -> {
                    canvas.selectNodesByIds(setOf("n0"))
                    mouse(canvas, MouseEvent.MOUSE_PRESSED, 120, 55)
                    mouse(canvas, MouseEvent.MOUSE_DRAGGED, 145, 55)
                }
                else -> {
                    canvas.selectWireByIndex(0)
                    mouse(canvas, MouseEvent.MOUSE_PRESSED, 150, 55)
                    mouse(canvas, MouseEvent.MOUSE_DRAGGED, 150, 90)
                }
            }
            assertSame(original.wires[100], canvas.diagram.wires[100])
            render(canvas)
            assertTrue("Partial minimap refresh must avoid redrawing every node ($kind): $calls", calls < 300)
            assertMiniMapMatchesFreshScene(canvas)
            canvas.cancelDrag()
            assertEquals(original, canvas.diagram)
            assertMiniMapMatchesFreshScene(canvas)
        }
    }

    fun testMiniMapProjectionTracksViewportAndClosureStyleInvalidation() {
        val canvas = canvas(model(2000))
        var color = Color.WHITE
        canvas.activityColor = { color }
        val before = render(canvas)
        color = Color.BLUE
        canvas.invalidateAppearance()
        val after = render(canvas)
        val frame = canvas.miniMapProjection!!.frame
        assertTrue((frame.x until frame.x + frame.width).any { x ->
            (frame.y until frame.y + frame.height).any { y -> before.getRGB(x, y) != after.getRGB(x, y) }
        })
        val first = canvas.miniMapProjection
        canvas.navigateTo(DiagramPoint(1000.0, 1000.0))
        val moved = canvas.miniMapProjection!!
        assertNotSame(first, moved)
        assertEquals(1000.0, moved.modelViewport.centerX)
        assertEquals(1000.0, moved.modelViewport.centerY)
        canvas.setSize(800, 500)
        assertNotSame(moved, canvas.miniMapProjection)
        canvas.display(ModelDiagram(emptyList(), emptyList()))
        assertNull(canvas.miniMapProjection)
        render(canvas)
    }
}
