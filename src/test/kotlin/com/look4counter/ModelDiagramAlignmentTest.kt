package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.modeleditor.*
import java.awt.event.MouseEvent

class ModelDiagramAlignmentTest : BasePlatformTestCase() {
    private fun send(canvas: ModelDiagramCanvas, type: Int, x: Int, y: Int, modifiers: Int = 0) = canvas.dispatchEvent(
        MouseEvent(canvas, type, 0, modifiers, x, y, 1, false, MouseEvent.BUTTON1))

    fun testSnapToggleControlsChildMoveAndResizeGuides() {
        val a = node("1", 20.0, 40.0)
        val b = node("2", 220.0, 140.0)
        for (snap in listOf(false, true)) {
            val canvas = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(a, b), emptyList())); snapEnabled = snap }
            send(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
            send(canvas, MouseEvent.MOUSE_DRAGGED, 167, 153)
            assertEquals(if (snap) DiagramPoint(120.0, 140.0) else DiagramPoint(117.0, 138.0), canvas.diagram.nodes[0].location)
            assertEquals(snap, canvas.alignmentGuides.isNotEmpty())
            if (snap) {
                canvas.snapEnabled = false
                assertTrue(canvas.alignmentGuides.isEmpty())
                assertEquals(DiagramPoint(117.0, 138.0), canvas.diagram.nodes[0].location)
            }
            canvas.cancelDrag()
            canvas.snapEnabled = snap
            canvas.selectNodesByIds(setOf("1"))
            send(canvas, MouseEvent.MOUSE_PRESSED, 120, 55)
            send(canvas, MouseEvent.MOUSE_DRAGGED, 217, 55)
            assertEquals(if (snap) 200.0 else 197.0, canvas.diagram.nodes[0].width)
            assertEquals(snap, canvas.alignmentGuides.isNotEmpty())
            canvas.cancelDrag()
            canvas.selectNodesByIds(setOf("1"))
            send(canvas, MouseEvent.MOUSE_PRESSED, 120, 55)
            send(canvas, MouseEvent.MOUSE_DRAGGED, -300, 55)
            assertEquals(16.0, canvas.diagram.nodes[0].width)
            canvas.cancelDrag()
        }
    }

    fun testClickReadOnlyAndSourceReplacementClearPreview() {
        val a = node("1", 20.0, 40.0); val b = node("2", 123.0, 40.0)
        val model = ModelDiagram(listOf(a, b), emptyList())
        val canvas = ModelDiagramCanvas().apply { snapEnabled = true; display(model) }
        var commits = 0
        canvas.onNodesMoved = { commits++ }
        send(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
        send(canvas, MouseEvent.MOUSE_RELEASED, 70, 55)
        assertEquals(0, commits)
        assertEquals(a, canvas.diagram.nodes[0])
        send(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
        send(canvas, MouseEvent.MOUSE_DRAGGED, 72, 55)
        assertFalse(canvas.alignmentGuides.isEmpty())
        canvas.canMoveNodes = { false }
        send(canvas, MouseEvent.MOUSE_RELEASED, 72, 55)
        assertEquals(0, commits)
        assertEquals(a, canvas.diagram.nodes[0])
        assertTrue(canvas.alignmentGuides.isEmpty())
        canvas.canMoveNodes = { true }
        send(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
        send(canvas, MouseEvent.MOUSE_DRAGGED, 72, 55)
        canvas.display(model.copy(nodes = listOf(a.copy(name = "Source"), b)))
        send(canvas, MouseEvent.MOUSE_RELEASED, 72, 55)
        assertEquals(0, commits)
        assertEquals("Source", canvas.diagram.nodes[0].name)
        assertTrue(canvas.alignmentGuides.isEmpty())
    }

    fun testResizeCommonDeltaFixedOppositeEdgeAndMinimum() {
        val a = node("1", 20.0, 40.0, 60.0); val b = node("2", 20.0, 100.0, 80.0)
        val target = node("3", 400.0, 200.0)
        val canvas = ModelDiagramCanvas().apply { snapEnabled = true; display(ModelDiagram(listOf(a, b, target), emptyList())) }
        canvas.selectNodesByIds(setOf("1", "2"))
        send(canvas, MouseEvent.MOUSE_PRESSED, 20, 115)
        send(canvas, MouseEvent.MOUSE_DRAGGED, 3, 115)
        assertEquals(80.0, canvas.diagram.nodes[0].width)
        assertEquals(100.0, canvas.diagram.nodes[1].width)
        assertEquals(0.0, canvas.diagram.nodes[0].location.x)
        assertEquals(a.location.x + a.width, canvas.diagram.nodes[0].location.x + canvas.diagram.nodes[0].width)
        canvas.cancelDrag()
        canvas.selectNodesByIds(setOf("1", "2"))
        send(canvas, MouseEvent.MOUSE_PRESSED, 20, 115)
        send(canvas, MouseEvent.MOUSE_DRAGGED, 200, 115)
        assertEquals(16.0, canvas.diagram.nodes[0].width)
        assertEquals(36.0, canvas.diagram.nodes[1].width)
        canvas.cancelDrag()
        assertEquals(listOf(a, b, target), canvas.diagram.nodes)
    }

    fun testEqualSpacingMovePersistsAsOneUndoAndRedo() {
        val file = myFixture.addFileToProject("snap.glue_uml_ad", """<Glue:GlueDiagram xmlns:Glue="http://www.glueframework.com/activitydiagram/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <child xsi:type="Glue:Activity" id="1" name="A" location="20,40" size="100,-1"/>
            <child xsi:type="Glue:Activity" id="2" name="B" location="20,140" size="100,-1"/>
            <child xsi:type="Glue:Activity" id="3" name="C" location="420,140" size="100,-1"/>
            </Glue:GlueDiagram>""").virtualFile
        val design = ModelDesignEditorProvider().createEditor(project, file)
        val source = ModelSourceEditorProvider().createEditor(project, file) as com.intellij.openapi.fileEditor.TextEditor
        fun find(component: java.awt.Component): ModelDiagramCanvas? = if (component is ModelDiagramCanvas) component
            else (component as? java.awt.Container)?.components?.firstNotNullOfOrNull(::find)
        try {
            com.intellij.testFramework.EditorTestUtil.waitForLoading(source.editor)
            val canvas = find(design.component)!!.apply { snapEnabled = true }
            val original = source.editor.document.text
            send(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
            send(canvas, MouseEvent.MOUSE_DRAGGED, 267, 153)
            assertEquals(listOf(100.0, 100.0), canvas.alignmentGuides.mapNotNull { it.distance })
            send(canvas, MouseEvent.MOUSE_RELEASED, 268, 154)
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents()
            assertEquals(original.replace("location=\"20,40\"", "location=\"220,140\""), source.editor.document.text)
            val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
            undo.undo(source)
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents()
            assertEquals(original, source.editor.document.text)
            undo.redo(source)
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents()
            assertEquals(DiagramPoint(220.0, 140.0), canvas.diagram.nodes[0].location)
        } finally {
            com.intellij.openapi.util.Disposer.dispose(source)
            com.intellij.openapi.util.Disposer.dispose(design)
        }
    }

    private fun node(id: String, x: Double, y: Double, width: Double = 100.0) =
        DiagramNode(DiagramNodeKind.Activity, id, id, DiagramPoint(x, y), width, 30.0)

    private fun transpose(node: DiagramNode) = node.copy(location = DiagramPoint(node.location.y, node.location.x),
        width = node.height, height = node.width)

    fun testSpacingMultiSelectionAltNavigationFinalReleaseAndImage() {
        val nodes = listOf(node("s1", 20.0, 40.0, 40.0), node("s2", 80.0, 40.0, 40.0),
            node("a", 20.0, 140.0), node("b", 420.0, 140.0))
        val canvas = ModelDiagramCanvas().apply { snapEnabled = true; display(ModelDiagram(nodes, emptyList())); setSize(720, 400) }
        canvas.selectNodesByIds(setOf("s1", "s2"))
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0, 0, 100, 300, 1, false, MouseEvent.BUTTON3))
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 0, MouseEvent.BUTTON3_DOWN_MASK, 130, 320, 1, false, MouseEvent.NOBUTTON))
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 0, 0, 130, 320, 1, false, MouseEvent.BUTTON3))
        val x = 40 + canvas.screenOffset.x.toInt(); val y = 55 + canvas.screenOffset.y.toInt()
        send(canvas, MouseEvent.MOUSE_PRESSED, x, y)
        send(canvas, MouseEvent.MOUSE_DRAGGED, x + 197, y + 98)
        assertEquals(listOf(100.0, 100.0), canvas.alignmentGuides.mapNotNull { it.distance })
        val image = java.awt.image.BufferedImage(720, 400, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { try { canvas.paint(it) } finally { it.dispose() } }
        val output = java.nio.file.Path.of("build/reports/model-equal-spacing.png")
        java.nio.file.Files.createDirectories(output.parent)
        javax.imageio.ImageIO.write(image, "png", output.toFile())
        send(canvas, MouseEvent.MOUSE_DRAGGED, x + 197, y + 98, MouseEvent.ALT_DOWN_MASK)
        assertEquals(DiagramPoint(217.0, 138.0), canvas.diagram.nodes[0].location)
        assertTrue(canvas.alignmentGuides.isEmpty())
        send(canvas, MouseEvent.MOUSE_RELEASED, x + 198, y + 99)
        assertEquals(DiagramPoint(220.0, 140.0), canvas.diagram.nodes[0].location)
        assertEquals(DiagramPoint(280.0, 140.0), canvas.diagram.nodes[1].location)
        assertEquals(nodes.drop(2), canvas.diagram.nodes.drop(2))
        assertTrue(canvas.alignmentGuides.isEmpty())
    }

    fun testSpacingPreviewClearsOnEscapeSourceAndReadOnly() {
        val nodes = listOf(node("s", 20.0, 40.0), node("a", 20.0, 140.0), node("b", 420.0, 140.0))
        for (action in listOf("escape", "source", "readOnly")) {
            val canvas = ModelDiagramCanvas().apply { snapEnabled = true; display(ModelDiagram(nodes, emptyList())) }
            var commits = 0
            canvas.onNodesMoved = { commits++ }
            send(canvas, MouseEvent.MOUSE_PRESSED, 70, 55)
            send(canvas, MouseEvent.MOUSE_DRAGGED, 267, 153)
            assertEquals(2, canvas.alignmentGuides.count { it.distance != null })
            when (action) {
                "escape" -> canvas.cancelDrag()
                "source" -> canvas.display(ModelDiagram(nodes, emptyList()))
                else -> canvas.canMoveNodes = { false }
            }
            send(canvas, MouseEvent.MOUSE_RELEASED, 267, 153)
            assertEquals(nodes, canvas.diagram.nodes)
            assertTrue(canvas.alignmentGuides.isEmpty())
            assertEquals(0, commits)
        }
    }

    fun testEqualSpacingBetweenAndOnBothOutsidesAlongBothAxes() {
        data class Example(val secondX: Double, val rawX: Double, val expectedX: Double, val gap: Double)
        val examples = listOf(Example(400.0, 197.0, 200.0, 100.0),
            Example(160.0, 317.0, 320.0, 60.0), Example(160.0, -157.0, -160.0, 60.0))
        for (vertical in listOf(false, true)) for (example in examples) {
            fun orient(node: DiagramNode) = if (vertical) transpose(node) else node
            val selected = orient(node("s", 0.0, 0.0))
            val targets = listOf(orient(node("a", 0.0, 0.0)), orient(node("b", example.secondX, 0.0)))
            val result = DiagramAlignment.move(listOf(selected), targets,
                if (vertical) DiagramPoint(0.0, example.rawX) else DiagramPoint(example.rawX, 0.0))
            assertEquals(example.expectedX, if (vertical) result.delta.y else result.delta.x, 0.000001)
            val distances = result.guides.filter { it.distance != null }
            assertEquals(2, distances.size)
            distances.forEach { guide ->
                assertEquals(example.gap, guide.distance!!, 0.000001)
                assertEquals(example.gap, if (vertical) guide.to.y - guide.from.y else guide.to.x - guide.from.x, 0.000001)
            }
        }
    }

    fun testSpacingUsesOuterEdgesWithDifferentSizesAndHalfPixelGaps() {
        val selected = node("s", 0.0, 0.0, 61.0)
        val a = node("a", 0.0, 0.0, 80.0)
        val b = node("b", 301.0, 0.0, 140.0)
        val between = DiagramAlignment.move(listOf(selected), listOf(a, b), DiagramPoint(158.0, 0.0))
        assertEquals(160.0, between.delta.x)
        assertEquals(listOf(80.0, 80.0), between.guides.mapNotNull { it.distance })
        val fractional = DiagramAlignment.move(listOf(selected.copy(width = 60.0)), listOf(a, b), DiagramPoint(158.0, 0.0))
        assertEquals(160.5, fractional.delta.x)
        assertEquals(listOf(80.5, 80.5), fractional.guides.mapNotNull { it.distance })
        val outside = DiagramAlignment.move(listOf(selected), listOf(a, b), DiagramPoint(660.0, 0.0))
        assertEquals(662.0, outside.delta.x)
        assertEquals(listOf(221.0, 221.0), outside.guides.mapNotNull { it.distance })
        val stateA = node("state-a", 0.0, 0.0, 20.0).copy(kind = DiagramNodeKind.InitialState, height = 20.0)
        val stateB = stateA.copy(id = "state-b", kind = DiagramNodeKind.FinalState, location = DiagramPoint(100.0, 0.0))
        val states = DiagramAlignment.move(listOf(selected.copy(width = 40.0)), listOf(stateA, stateB), DiagramPoint(37.0, 0.0))
        assertEquals(40.0, states.delta.x)
        assertEquals(listOf(20.0, 20.0), states.guides.mapNotNull { it.distance })
    }

    fun testSpacingThresholdRecoveryAndOrdinaryAlignmentCompetition() {
        val selected = node("s", 0.0, 0.0)
        val targets = listOf(node("a", 0.0, 0.0), node("b", 160.0, 0.0))
        for (raw in listOf(315.0, 325.0)) {
            val result = DiagramAlignment.move(listOf(selected), targets, DiagramPoint(raw, 0.0))
            assertEquals(320.0, result.delta.x)
            assertEquals(2, result.guides.count { it.distance != null })
        }
        val outside = DiagramAlignment.move(listOf(selected), targets, DiagramPoint(314.0, 0.0))
        assertEquals(314.0, outside.delta.x)
        assertTrue(outside.guides.none { it.distance != null })
        val closerEdge = DiagramAlignment.move(listOf(selected), targets + node("edge", 315.0, 200.0), DiagramPoint(316.0, 0.0))
        assertEquals(315.0, closerEdge.delta.x)
        assertTrue(closerEdge.guides.none { it.distance != null })
        val tie = DiagramAlignment.move(listOf(selected), targets + node("edge", 314.0, 200.0), DiagramPoint(317.0, 0.0))
        assertEquals(314.0, tie.delta.x)
        val samePosition = DiagramAlignment.move(listOf(selected), targets + node("edge", 320.0, 200.0), DiagramPoint(317.0, 0.0))
        assertEquals(320.0, samePosition.delta.x)
        assertEquals(2, samePosition.guides.count { it.distance != null })
    }

    fun testSpacingIgnoresOverlapInsufficientSpaceDifferentRowsAndNonAdjacentPairs() {
        val selected = node("s", 0.0, 0.0)
        val examples = listOf(
            listOf(node("a", 0.0, 0.0), node("b", 80.0, 0.0)) to 260.0,
            listOf(node("a", 0.0, 0.0), node("b", 160.0, 0.0)) to 130.0,
            listOf(node("a", 0.0, 0.0), node("b", 160.0, 200.0)) to 317.0,
            listOf(node("a", 0.0, 0.0), node("b", 160.0, 0.0), node("c", 300.0, 0.0)) to 317.0,
            listOf(node("a", 0.0, 0.0), node("b", 160.0, 0.0), node("c", 300.0, 0.0)) to 700.0)
        examples.forEach { (targets, raw) ->
            val result = DiagramAlignment.move(listOf(selected), targets, DiagramPoint(raw, 0.0))
            assertTrue(result.guides.none { it.distance != null })
        }
        val touching = DiagramAlignment.move(listOf(selected), listOf(node("a", 0.0, 0.0), node("b", 100.0, 0.0)), DiagramPoint(202.0, 0.0))
        assertEquals(200.0, touching.delta.x)
        assertEquals(listOf(0.0, 0.0), touching.guides.mapNotNull { it.distance })
    }

    fun testMoveThresholdAndRawPointerRecovery() {
        val a = node("1", 0.0, 0.0); val b = node("2", 200.0, 100.0)
        assertEquals(100.0, DiagramAlignment.move(listOf(a), listOf(b), DiagramPoint(95.0, 0.0)).delta.x)
        assertEquals(94.0, DiagramAlignment.move(listOf(a), listOf(b), DiagramPoint(94.0, 0.0)).delta.x)
        assertEquals(100.0, DiagramAlignment.move(listOf(a), listOf(b), DiagramPoint(103.0, 0.0)).delta.x)
    }

    fun testGroupBoundsCenterAndNegativeCoordinates() {
        val a = node("1", -200.0, -100.0); val b = node("2", -50.0, -100.0)
        val target = node("3", 200.0, 0.0)
        val result = DiagramAlignment.move(listOf(a, b), listOf(target), DiagramPoint(148.0, 98.0))
        assertEquals(DiagramPoint(150.0, 100.0), result.delta)
        assertEquals(2, result.guides.size)
        assertEquals(DiagramPoint(148.0, 98.0), DiagramAlignment.move(listOf(a, b), emptyList(), DiagramPoint(148.0, 98.0)).delta)
    }

    fun testResizeEqualWidthBothSidesAndMinimum() {
        val a = node("1", 0.0, 0.0, 60.0); val b = node("2", 400.0, 100.0)
        for (left in listOf(false, true)) {
            assertEquals(40.0, DiagramAlignment.resize(listOf(a), listOf(b), 37.0, left).delta.x)
            assertEquals(-44.0, DiagramAlignment.resize(listOf(a), listOf(b), -100.0, left).delta.x)
        }
    }

    fun testCanvasLiveSnapAltReleaseAndCancel() {
        val canvas = ModelDiagramCanvas().apply { snapEnabled = true }
        val a = node("1", 20.0, 40.0); val b = node("2", 220.0, 140.0)
        canvas.display(ModelDiagram(listOf(a, b), emptyList()))
        fun send(type: Int, x: Int, y: Int, modifiers: Int = 0) = canvas.dispatchEvent(
            MouseEvent(canvas, type, 0, modifiers, x, y, 1, false, MouseEvent.BUTTON1))
        val offset = canvas.screenOffset
        val x = (70 + offset.x).toInt(); val y = (55 + offset.y).toInt()
        send(MouseEvent.MOUSE_PRESSED, x, y)
        send(MouseEvent.MOUSE_DRAGGED, x + 97, y + 98)
        assertEquals(DiagramPoint(120.0, 140.0), canvas.diagram.nodes[0].location)
        assertFalse(canvas.alignmentGuides.isEmpty())
        canvas.setSize(600, 400)
        val image = java.awt.image.BufferedImage(600, 400, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { try { canvas.paint(it) } finally { it.dispose() } }
        val output = java.nio.file.Path.of("build/reports/model-alignment-guides.png")
        java.nio.file.Files.createDirectories(output.parent)
        javax.imageio.ImageIO.write(image, "png", output.toFile())
        send(MouseEvent.MOUSE_DRAGGED, x + 97, y + 98, MouseEvent.ALT_DOWN_MASK)
        assertEquals(DiagramPoint(117.0, 138.0), canvas.diagram.nodes[0].location)
        assertTrue(canvas.alignmentGuides.isEmpty())
        canvas.cancelDrag()
        assertEquals(a, canvas.diagram.nodes[0])
        send(MouseEvent.MOUSE_PRESSED, x, y)
        send(MouseEvent.MOUSE_DRAGGED, x + 90, y + 90)
        send(MouseEvent.MOUSE_RELEASED, x + 98, y + 99)
        assertEquals(DiagramPoint(120.0, 140.0), canvas.diagram.nodes[0].location)
        assertTrue(canvas.alignmentGuides.isEmpty())
    }
}
