package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.psi.xml.XmlFile
import com.look4counter.modeleditor.*
import java.awt.event.MouseEvent
import java.awt.event.MouseWheelEvent

class ModelEditorFeaturesTest : BasePlatformTestCase() {
    fun testWireCreationToEmptySpaceHasNoMagnetBendsAndTargetPreviewMatchesCreation() {
        val a = activity()
        val state = DiagramNode(DiagramNodeKind.FinalState, "2", "", DiagramPoint(320.0, 200.0), 20.0, 20.0)
        for (magnet in listOf(false, true)) {
            val canvas = ModelDiagramCanvas().apply {
                display(ModelDiagram(listOf(a, state), emptyList())); setSize(600, 400)
                wireMagnetEnabled = magnet
                setWireCreationMode(true)
            }
            fun click(x: Int, y: Int) {
                mouse(canvas, MouseEvent.MOUSE_PRESSED, x, y)
                mouse(canvas, MouseEvent.MOUSE_RELEASED, x, y)
                mouse(canvas, MouseEvent.MOUSE_CLICKED, x, y)
            }
            click(120, 55)
            mouse(canvas, MouseEvent.MOUSE_MOVED, 280, 160)
            assertTrue(canvas.wireCreationPreview!!.bendpoints.isEmpty())
            assertEquals(2, DiagramGeometry.path(canvas.wireCreationPreview!!).size)
            mouse(canvas, MouseEvent.MOUSE_MOVED, 330, 210)
            val preview = DiagramGeometry.path(canvas.wireCreationPreview!!)
            val actualCanvas = ModelDiagramCanvas().apply {
                display(ModelDiagram(listOf(a, state), listOf(canvas.wireCreationPreview!!))); setSize(600, 400)
            }
            fun render(component: ModelDiagramCanvas): java.awt.image.BufferedImage {
                val image = java.awt.image.BufferedImage(600, 400, java.awt.image.BufferedImage.TYPE_INT_ARGB)
                image.createGraphics().let { try { component.paint(it) } finally { it.dispose() } }
                return image
            }
            val ghostImage = render(canvas)
            val actualImage = render(actualCanvas)
            for (x in 210..300) for (y in 80..175) assertEquals(actualImage.getRGB(x, y), ghostImage.getRGB(x, y))
            var actual: DiagramWire? = null
            canvas.onWireCreateRequested = { actual = it }
            click(330, 210)
            assertEquals(preview, DiagramGeometry.path(actual!!))
        }
    }
    fun testWireCreationPreviewUsesStatePerimeterAndDoesNotConnectToSameChild() {
        val a = activity()
        val state = DiagramNode(DiagramNodeKind.InitialState, "2", "", DiagramPoint(320.0, 200.0), 20.0, 20.0)
        val canvas = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(a, state), emptyList())); setSize(600, 400) }
        var created: DiagramWire? = null
        canvas.onWireCreateRequested = { created = it }
        fun click(x: Int, y: Int) {
            mouse(canvas, MouseEvent.MOUSE_PRESSED, x, y)
            mouse(canvas, MouseEvent.MOUSE_RELEASED, x, y)
            mouse(canvas, MouseEvent.MOUSE_CLICKED, x, y)
        }
        canvas.setWireCreationMode(true)
        click(330, 210)
        click(330, 210)
        assertNull(created)
        mouse(canvas, MouseEvent.MOUSE_MOVED, 120, 55)
        val path = DiagramGeometry.path(canvas.wireCreationPreview!!)
        assertEquals(10.0, kotlin.math.hypot(path.first().x - state.center.x, path.first().y - state.center.y), 0.000001)
        click(120, 55)
        assertEquals(state, created!!.start)
        assertEquals(a, created!!.end)
        assertTrue(canvas.wireCreationMode)
        canvas.cancelDrag()
        assertFalse(canvas.wireCreationMode)
        assertTrue(canvas.diagram.wires.isEmpty())
    }
    fun testMiniMapDragUsesPressProjectionAndReleaseThenEscapeRestoresPan() {
        val canvas = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(activity()), emptyList())); setSize(600, 400) }
        val original = canvas.diagram
        val first = canvas.miniMapProjection!!
        val startX = first.contentArea.centerX.toInt(); val startY = first.contentArea.centerY.toInt()
        mouse(canvas, MouseEvent.MOUSE_PRESSED, startX, startY)
        assertTrue(canvas.isDragging)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, startX + 12, startY + 7)
        fun expected(x: Int, y: Int) = DiagramPoint(
            (x.toDouble().coerceIn(first.contentArea.minX, first.contentArea.maxX) - first.offsetX) / first.scale,
            (y.toDouble().coerceIn(first.contentArea.minY, first.contentArea.maxY) - first.offsetY) / first.scale)
        val dragged = canvas.miniMapProjection!!.modelViewport
        assertEquals(expected(startX + 12, startY + 7).x, dragged.centerX, 0.000001)
        assertEquals(expected(startX + 12, startY + 7).y, dragged.centerY, 0.000001)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, startX + 18, startY + 9)
        val released = canvas.miniMapProjection!!.modelViewport
        assertEquals(expected(startX + 18, startY + 9).x, released.centerX, 0.000001)
        assertEquals(expected(startX + 18, startY + 9).y, released.centerY, 0.000001)
        assertFalse(canvas.isDragging)
        val beforeCancel = canvas.pan
        val second = canvas.miniMapProjection!!
        mouse(canvas, MouseEvent.MOUSE_PRESSED, second.contentArea.centerX.toInt(), second.contentArea.centerY.toInt())
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 5000, -5000)
        assertTrue(canvas.pan != beforeCancel)
        canvas.cancelDrag()
        assertEquals(beforeCancel, canvas.pan)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 5000, -5000)
        assertEquals(beforeCancel, canvas.pan)
        assertEquals(original, canvas.diagram)
        assertEmpty(canvas.selectedNodes)
    }

    fun testExistingBendpointMagnetUsesStartAndEndActivityRanges() {
        val a = activity()
        val b = a.copy(id = "2", location = DiagramPoint(20.0, 240.0))
        val bend = DiagramPoint(150.0, 155.0)
        val wire = DiagramWire(0, "3", "", a, b, null, null, listOf(DiagramBendpoint(
            DiagramPoint(bend.x - a.center.x, bend.y - a.center.y), DiagramPoint(bend.x - b.center.x, bend.y - b.center.y), 0.5)))
        val canvas = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(a, b), listOf(wire))); wireMagnetEnabled = true }
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 150, 110)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, 150, 110)
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 150, 155)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 142, 165)
        val point = canvas.wirePath(canvas.diagram.wires.single())[1]
        assertEquals(150.0, point.x)
        assertEquals(165.0, point.y)
        canvas.cancelDrag()
        assertEquals(wire, canvas.diagram.wires.single())
    }
    fun testMiddleWireHandleUsesMagnetAndBendpointStaysFreeOutsideNodeRanges() {
        for (magnet in listOf(false, true)) {
            val a = activity().copy(outputs = listOf(0))
            val b = a.copy(id = "2", location = DiagramPoint(20.0, 240.0), outputs = emptyList(), inputs = listOf(0))
            val wire = DiagramWire(0, "3", "", a, b, null, null, emptyList())
            val canvas = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(a, b), listOf(wire))); wireMagnetEnabled = magnet }
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 120, 155)
            mouse(canvas, MouseEvent.MOUSE_RELEASED, 120, 155)
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 120, 155)
            mouse(canvas, MouseEvent.MOUSE_DRAGGED, 142, 155)
            assertEquals(if (magnet) 150.0 else 142.0, canvas.wirePath(canvas.diagram.wires.single()).first().x)
            canvas.cancelDrag()
            assertEquals(wire, canvas.diagram.wires.single())
        }
        val wire = DiagramWire(0, "3", "", activity(), activity().copy(id = "2", location = DiagramPoint(20.0, 240.0)), null, null, emptyList())
        assertEquals(DiagramPoint(500.0, 400.0), WireMagnet.dragPosition(wire, DiagramPoint(500.0, 400.0)))
        assertEquals(DiagramPoint(150.0, 155.0), WireMagnet.dragPosition(wire, DiagramPoint(142.0, 155.0)))
    }

    fun testRootHudHidesDescriptionAndUsesEquals() {
        val canvas = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(activity()), emptyList(), properties = listOf(
            ActivityProperty("title", "A"), ActivityProperty("description", "hidden")))); setSize(600, 400) }
        assertEquals(listOf("title=A"), canvas.diagramPropertyLines)
        assertEquals(2, canvas.diagram.properties.size)
    }
    private fun mouse(canvas: ModelDiagramCanvas, type: Int, x: Int, y: Int) = canvas.dispatchEvent(
        MouseEvent(canvas, type, 0, 0, x, y, 1, false, MouseEvent.BUTTON1))
    private fun render(canvas: ModelDiagramCanvas): java.awt.image.BufferedImage {
        val image = java.awt.image.BufferedImage(canvas.width, canvas.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { try { canvas.paint(it) } finally { it.dispose() } }
        return image
    }
    fun testWireEndpointUsesMagnetOnlyWhenEnabled() {
        for (magnet in listOf(false, true)) {
            val a = activity().copy(outputs = listOf(0))
            val b = a.copy(id = "2", location = DiagramPoint(20.0, 240.0), outputs = emptyList(), inputs = listOf(0))
            val wire = DiagramWire(0, "3", "", a, b, null, null, emptyList())
            val canvas = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(a, b), listOf(wire))); wireMagnetEnabled = magnet }
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 120, 155)
            mouse(canvas, MouseEvent.MOUSE_RELEASED, 120, 155)
            assertNotNull(canvas.selectedWire)
            mouse(canvas, MouseEvent.MOUSE_PRESSED, 120, 70)
            mouse(canvas, MouseEvent.MOUSE_DRAGGED, 142, 45)
            assertEquals(if (magnet) 150.0 else 142.0, canvas.wirePath(canvas.diagram.wires.single()).first().x)
            mouse(canvas, MouseEvent.MOUSE_RELEASED, 142, 45)
            assertEquals(if (magnet) 150.0 else 142.0, canvas.wirePath(canvas.diagram.wires.single()).first().x)
        }
    }
    fun testRangeBoxIsTranslucentSkyBlueAndRootPropertyHudRenders() {
        val canvas = ModelDiagramCanvas().apply {
            display(ModelDiagram(listOf(activity()), emptyList(), properties = listOf(ActivityProperty("service", "Sample"))))
            setSize(600, 400)
        }
        mouse(canvas, MouseEvent.MOUSE_PRESSED, 40, 150)
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 260, 300)
        val image = render(canvas)
        val pixel = java.awt.Color(image.getRGB(150, 220))
        assertTrue(pixel.blue > pixel.red)
        assertTrue(pixel.green > pixel.red)
        assertTrue(pixel.rgb != java.awt.Color(90, 180, 255).rgb)
        val output = java.nio.file.Path.of("build/reports/model-navigation-features.png")
        java.nio.file.Files.createDirectories(output.parent)
        javax.imageio.ImageIO.write(image, "png", output.toFile())
        canvas.cancelDrag()
        assertNull(canvas.selectionBox)
    }
    fun testEditorViewportHasNoScrollbarsAndToolbarMagnetChangesCanvasState() {
        val file = myFixture.addFileToProject("features.glue_uml_ad", """<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <child xsi:type="Activity" id="1" name="Far" location="5000,5000" size="100,-1"/></GlueDiagram>""").virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            editor.component.setSize(640, 480); editor.component.doLayout()
            val scroll = editor.component.components.filterIsInstance<javax.swing.JScrollPane>().single()
            scroll.doLayout(); scroll.viewport.doLayout()
            val canvas = scroll.viewport.view as ModelDiagramCanvas
            assertFalse(scroll.verticalScrollBar.isVisible)
            assertFalse(scroll.horizontalScrollBar.isVisible)
            assertEquals(scroll.viewport.extentSize, canvas.size)
            val toolbar = editor.component.components.filterIsInstance<ChildCreationToolbar>().single()
            val event = com.intellij.openapi.actionSystem.AnActionEvent.createFromAnAction(toolbar.magnetAction, null,
                com.intellij.openapi.actionSystem.ActionPlaces.UNKNOWN, com.intellij.openapi.actionSystem.DataContext.EMPTY_CONTEXT)
            assertFalse(canvas.wireMagnetEnabled)
            toolbar.magnetAction.setSelected(event, true)
            assertTrue(canvas.wireMagnetEnabled)
        } finally { com.intellij.openapi.util.Disposer.dispose(editor) }
    }
    private fun activity() = DiagramNode(DiagramNodeKind.Activity, "1", "A", DiagramPoint(20.0, 40.0), 200.0, 30.0)
    fun testRootPropertiesStaySeparateFromChildProperties() {
        val xml = myFixture.configureByText("properties.glue_uml_ad", """<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <property key="service" value="A &amp; B"/><property key="description" value="first&#10;second"/>
            <child xsi:type="Activity" id="1"><property key="child-only" value="hidden"/></child></GlueDiagram>""") as XmlFile
        val diagram = ModelDiagramParser.parse(xml)
        assertEquals(listOf("service", "description"), diagram.properties.map { it.key })
        assertEquals("A & B", diagram.properties[0].value)
        assertEquals("first\nsecond", diagram.properties[1].value)
    }
    fun testMagnetUsesCenterThirtyUnitGridAndInset() {
        val a = activity()
        assertEquals(DiagramPoint(150.0, 55.0), WireMagnet.position(a, DiagramPoint(142.0, 42.0)))
        assertEquals(DiagramPoint(120.0, 55.0), WireMagnet.position(a, DiagramPoint(118.0, 68.0)))
        assertEquals(DiagramPoint(210.0, 55.0), WireMagnet.position(a, DiagramPoint(219.0, 50.0)))
        val narrow = a.copy(width = 16.0)
        assertEquals(narrow.center, WireMagnet.position(narrow, DiagramPoint(20.0, 40.0)))
        val state = a.copy(kind = DiagramNodeKind.InitialState, width = 20.0, height = 20.0)
        val mouse = DiagramPoint(25.0, 43.0)
        assertEquals(mouse, WireMagnet.position(state, mouse))
    }
    fun testWheelAndControlWheelNavigateWithoutMovingDiagram() {
        val canvas = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(activity()), emptyList())); setSize(600, 400) }
        val original = canvas.diagram
        fun wheel(ctrl: Boolean) = canvas.dispatchEvent(MouseWheelEvent(canvas, MouseEvent.MOUSE_WHEEL, 0,
            if (ctrl) MouseEvent.CTRL_DOWN_MASK else 0, 200, 200, 0, false, MouseWheelEvent.WHEEL_UNIT_SCROLL, 3, 2))
        wheel(false)
        assertEquals(DiagramPoint(0.0, -60.0), canvas.pan)
        wheel(true)
        assertEquals(DiagramPoint(-60.0, -60.0), canvas.pan)
        assertEquals(original, canvas.diagram)
    }
    fun testActivityStateMagnetKeepsSnappedContactInSerializableBendpointGeometry() {
        val a = activity()
        val state = a.copy(kind = DiagramNodeKind.FinalState, id = "2", location = DiagramPoint(130.0, 200.0), width = 20.0, height = 20.0)
        val wire = DiagramWire(0, "3", "", a, state, DiagramPoint(150.0, 55.0), null, emptyList())
        val snapped = WireMagnet.attach(wire, true, DiagramPoint(150.0, 55.0))
        assertEquals(150.0, DiagramGeometry.path(snapped).first().x)
        assertEquals(70.0, DiagramGeometry.path(snapped).first().y)
        assertEquals(state, snapped.end)
    }
    fun testMiniMapClickCentersClickedModelPoint() {
        val canvas = ModelDiagramCanvas().apply { display(ModelDiagram(listOf(activity()), emptyList())); setSize(600, 400) }
        val projection = canvas.miniMapProjection!!
        val mapped = projection.map(activity().center)
        val point = java.awt.Point(mapped.x.toInt(), mapped.y.toInt())
        val target = DiagramPoint((point.x - projection.offsetX) / projection.scale, (point.y - projection.offsetY) / projection.scale)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0, 0, point.x, point.y, 1, false, MouseEvent.BUTTON1))
        val viewport = canvas.miniMapProjection!!.modelViewport
        assertEquals(target.x, viewport.centerX, 0.000001)
        assertEquals(target.y, viewport.centerY, 0.000001)
        assertEmpty(canvas.selectedNodes)
    }
}
