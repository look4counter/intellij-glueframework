package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import com.look4counter.modeleditor.*
import java.awt.Component
import java.awt.Container
import java.awt.image.BufferedImage
import java.awt.Color
import java.awt.event.MouseEvent
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.math.hypot
import kotlin.math.roundToInt

class ModelDiagramTest : BasePlatformTestCase() {
    fun testIntegerCoordinateSerializationPreservesSignedValuesAndSizeSentinel() {
        assertEquals("0", DiagramXmlText.coordinate(0.0))
        assertEquals("0", DiagramXmlText.coordinate(-0.1))
        assertEquals("24", DiagramXmlText.coordinate(23.6))
        assertEquals("-24", DiagramXmlText.coordinate(-23.6))
        assertEquals("-1", DiagramXmlText.coordinate(-1.0))
        assertEquals("-24,0", DiagramXmlText.point(DiagramPoint(-23.6, 0.2)))
        val start = state(0.0, 0.0)
        val end = activity(80.0, 100.0)
        assertFalse(wire(start, end).canEditCondition)
        assertTrue(wire(end, start).canEditCondition)
        assertTrue(wire(end, start).copy(condition = "").canEditCondition)
    }

    fun testCoordinateNormalizationLeavesWeightAndOtherAttributesUnchanged() {
        val xml = myFixture.configureByText("coordinates.xml", """
            <wire firstRelativePoint="-23.6,0.2" lastRelativePoint="100.7,-50.8" guardcondition="failure">
              <bendpoint firstDimension="-1.2,20.9" secondDimension="30.1,-40.9" weight="0.5"/>
            </wire>
        """.trimIndent()) as XmlFile
        val tag = xml.rootTag!!
        WriteCommandAction.runWriteCommandAction(project) { DiagramXmlText.normalizeCoordinates(tag) }
        assertEquals("-24,0", tag.getAttributeValue("firstRelativePoint"))
        assertEquals("101,-51", tag.getAttributeValue("lastRelativePoint"))
        assertEquals("failure", tag.getAttributeValue("guardcondition"))
        val bend = tag.subTags.single()
        assertEquals("-1,21", bend.getAttributeValue("firstDimension"))
        assertEquals("30,-41", bend.getAttributeValue("secondDimension"))
        assertEquals("0.5", bend.getAttributeValue("weight"))
        val child = myFixture.configureByText("child-coordinates.xml", """<child location="-1.9,2.8" size="100.6,-1"/>""") as XmlFile
        WriteCommandAction.runWriteCommandAction(project) { DiagramXmlText.normalizeCoordinates(child.rootTag!!) }
        assertEquals("-2,3", child.rootTag!!.getAttributeValue("location"))
        assertEquals("101,-1", child.rootTag!!.getAttributeValue("size"))
    }

    fun testMissingWireConditionDefaultsAndInitialStateHidesLabel() {
        val xml = myFixture.configureByText("conditions.glue_uml_ad", """
            <model:GlueDiagram xmlns:model="urn:model" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
              <child xsi:type="model:InitialState" id="1" output="//@wire.0"/>
              <child xsi:type="model:Activity" id="2" input="//@wire.0" output="//@wire.1"/>
              <wire id="3"/><wire id="4" guardcondition=""/><wire id="5" guardcondition="failure"/>
            </model:GlueDiagram>
        """.trimIndent()) as XmlFile
        val wires = ModelDiagramParser.parse(xml).wires
        assertEquals(listOf("success", "success", "failure"), wires.map { it.condition })
        assertFalse(wires[0].showsCondition)
        assertTrue(wires[1].showsCondition)
        assertFalse(wires[0].copy(condition = "custom").showsCondition)
    }

    fun testGroupMoveTranslatesEntireWireWithoutChangingRelativeBends() {
        val a = activity(20.0, 40.0)
        val b = activity(20.0, 180.0)
        val delta = DiagramPoint(37.0, -23.0)
        val moved = mapOf(a to a.copy(location = a.location + delta), b to b.copy(location = b.location + delta))
        for (bends in listOf(emptyList(), listOf(DiagramBendpoint(DiagramPoint(80.0, 40.0), DiagramPoint(80.0, -100.0), 0.5)))) {
            val original = wire(a, b, bends).copy(firstRelativePoint = DiagramPoint(50.0, 70.0), lastRelativePoint = DiagramPoint(50.0, 180.0))
            val edited = DiagramEdits.relocateNodes(original, moved)
            assertEquals(original.firstRelativePoint!! + delta, edited.firstRelativePoint)
            assertEquals(original.lastRelativePoint!! + delta, edited.lastRelativePoint)
            assertEquals(bends, edited.bendpoints)
            assertEquals(DiagramGeometry.path(original).map { it + delta }, DiagramGeometry.path(edited))
            assertEquals(original.firstRelativePoint, DiagramEdits.relocateNodes(original, mapOf(a to moved.getValue(a))).firstRelativePoint)
            val resized = mapOf(a to a.copy(width = a.width + 20), b to b.copy(width = b.width + 20))
            assertEquals(original.firstRelativePoint, DiagramEdits.relocateNodes(original, resized).firstRelativePoint)
        }
    }

    fun testResizeKeepsDirectWireContactWhenNoPreferredPointWasStored() {
        val a = activity(20.0, 40.0)
        val b = activity(20.0, 160.0)
        val original = wire(a, b).copy(firstRelativePoint = null)
        val path = DiagramGeometry.path(original)
        for (node in listOf(a, b)) for (left in listOf(false, true)) for (delta in listOf(-20.0, 40.0)) {
            val resized = node.copy(width = node.width + delta,
                location = if (left) node.location + DiagramPoint(-delta, 0.0) else node.location)
            val edited = DiagramEdits.relocateNodes(original, mapOf(node to resized))
            assertEquals(path, DiagramGeometry.path(edited))
            assertEquals(path.first(), edited.firstRelativePoint)
            assertEmpty(edited.bendpoints)
        }
    }

    fun testResizeRebasesBothRelativeVectorsWithoutMovingAbsoluteBends() {
        val a = activity(20.0, 40.0)
        for (b in listOf(activity(220.0, 160.0), state(220.0, 160.0))) {
            for (count in 0..4) {
                val bends = (0 until count).map { i -> DiagramBendpoint(
                    DiagramPoint(110.0 + i * 10, 65.0 + i * 20),
                    DiagramPoint(-90.0 + i * 15, -45.0 + i * 10), 0.1 + i * 0.2) }
                val original = wire(a, b, bends)
                val absolute = DiagramGeometry.path(original).drop(1).dropLast(1)
                for (left in listOf(false, true)) for (delta in listOf(-20.0, 40.0)) {
                    val resizedA = a.copy(width = a.width + delta,
                        location = if (left) a.location + DiagramPoint(-delta, 0.0) else a.location)
                    val resizedB = if (b.isActivity) b.copy(width = b.width + delta,
                        location = if (left) b.location + DiagramPoint(-delta, 0.0) else b.location) else b
                    val variants = listOf(mapOf(a to resizedA), mapOf(b to resizedB), mapOf(a to resizedA, b to resizedB))
                    for (edits in variants) {
                        val edited = DiagramEdits.relocateNodes(original, edits)
                        val actual = DiagramGeometry.path(edited).drop(1).dropLast(1)
                        assertEquals(absolute.size, actual.size)
                        absolute.zip(actual).forEach { (before, after) -> point(before, after) }
                        assertEquals(bends.map { it.weight }, edited.bendpoints.map { it.weight })
                        assertEquals(original.firstRelativePoint, edited.firstRelativePoint)
                        assertEquals(original.lastRelativePoint, edited.lastRelativePoint)
                        for (i in bends.indices) {
                            point(a.center + bends[i].first, edited.start!!.center + edited.bendpoints[i].first)
                            point(b.center + bends[i].second, edited.end!!.center + edited.bendpoints[i].second)
                        }
                    }
                }
            }
        }
    }

    fun testEditorResizePersistsRebasedBendsAndUndoRedoRestoresGeometry() = withModelEditor(wireFixture(true)) { canvas, source, document, file ->
        val original = document.text
        val originalPath = canvas.wirePath(canvas.diagram.wires[0]).drop(1).dropLast(1)
        leftDown(canvas, 50, 55)
        val start = canvas.diagram.nodes.single { it.id == "a" }
        val edge = (start.location.x + start.width).roundToInt()
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 1, MouseEvent.BUTTON1_DOWN_MASK,
            edge, 55, 1, false, MouseEvent.BUTTON1))
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 2, MouseEvent.BUTTON1_DOWN_MASK,
            edge + 40, 55, 0, false, MouseEvent.NOBUTTON))
        originalPath.zip(canvas.wirePath(canvas.diagram.wires[0]).drop(1).dropLast(1)).forEach { (a, b) -> point(a, b) }
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 3, 0,
            edge + 40, 55, 1, false, MouseEvent.BUTTON1))
        UIUtil.dispatchAllInvocationEvents()
        val resized = document.text
        assertTrue(resized.contains("firstDimension=\"80,55\""))
        assertTrue(resized.contains("secondDimension=\"-100,55\""))
        assertTrue(resized.contains("weight=\"0.2\" custom=\"point\""))
        assertEquals(originalPath, canvas.wirePath(canvas.diagram.wires[0]).drop(1).dropLast(1))
        assertEquals(originalPath, DiagramGeometry.path(parse(resized).wires[0]).drop(1).dropLast(1))
        val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, document.text)
        undo.redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(resized, document.text)
        FileDocumentManager.getInstance().saveDocument(document)
        assertEquals(resized, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
    }

    fun testActivityVerticalContactsClampXForStatePreviewAndBendsBothDirections() {
        val activity = activity(20.0, 40.0)
        for (x in listOf(20.0, 21.0, 27.9, 28.0, 70.0, 112.0, 112.1, 119.0, 120.0)) {
            for (y in listOf(20.0, 100.0)) {
                val at = DiagramPoint(x, y)
                val target = state(x - 10, y - 10)
                val expected = DiagramPoint(x.coerceIn(28.0, 112.0), if (y < 40) 40.0 else 70.0)
                val direct = wire(activity, target).copy(firstRelativePoint = null)
                point(expected, DiagramGeometry.path(direct).first())
                point(expected, DiagramGeometry.path(wire(target, activity).copy(firstRelativePoint = null)).last())
                point(expected, DiagramGeometry.previewStateEndpoint(direct, false, at).first())
                point(expected, DiagramGeometry.previewStateEndpoint(wire(target, activity), true, at).last())
                val far = state(400.0, 400.0)
                fun bent(start: DiagramNode, end: DiagramNode) = wire(start, end, listOf(DiagramBendpoint(
                    DiagramPoint(at.x - start.center.x, at.y - start.center.y),
                    DiagramPoint(at.x - end.center.x, at.y - end.center.y), 0.7)))
                point(expected, DiagramGeometry.path(bent(activity, far)).first())
                point(expected, DiagramGeometry.path(bent(far, activity)).last())
                point(at, DiagramGeometry.path(bent(activity, far))[1])
            }
        }
    }

    fun testExistingActivityNarrowerThanSixteenUsesCenterWithoutInvalidClamp() {
        for (width in listOf(1.0, 6.0, 16.0)) {
            val activity = activity(20.0, 40.0, width)
            for (x in listOf(20.0, 20 + width / 2, 20 + width)) {
                val path = DiagramGeometry.path(wire(activity, state(x - 10, -10.0)).copy(firstRelativePoint = null))
                point(DiagramPoint(20 + width / 2, 40.0), path.first())
                assertTrue(path.all { it.x.isFinite() && it.y.isFinite() })
            }
        }
    }

    fun testActivityStateSideContactTracksYAndClampsInsetSymmetrically() {
        val activity = activity(20.0, 40.0)
        for (x in listOf(-100.0, 230.0)) for (y in listOf(40.0, 45.0, 48.0, 50.0, 55.0, 60.0, 62.0, 65.0, 70.0)) {
            val state = state(x - 10, y - 10)
            val expected = DiagramPoint(if (x < 20) 20.0 else 120.0, y.coerceIn(48.0, 62.0))
            val path = DiagramGeometry.path(wire(activity, state).copy(firstRelativePoint = null))
            point(expected, path.first())
            assertStateContact(state.center, path[path.lastIndex - 1], path.last())
            assertEquals(path.reversed(), DiagramGeometry.path(wire(state, activity).copy(firstRelativePoint = null)))
            val preview = DiagramGeometry.previewStateEndpoint(wire(activity, state), false, state.center)
            point(expected, preview.first())
            assertEquals(preview.reversed(), DiagramGeometry.previewStateEndpoint(wire(state, activity), true, state.center))
        }
    }

    fun testBothStateKindsUseRadialPerimeterForStoredContactsBendsAndExplicitPaths() {
        for (kind in listOf(DiagramNodeKind.InitialState, DiagramNodeKind.FinalState)) {
            val node = state(0.0, 0.0).copy(kind = kind)
            for (other in listOf(state(200.0, 0.0), activity(200.0, 100.0))) {
                for (explicit in listOf(false, true)) for (bent in listOf(false, true)) {
                    val original = wire(node, other).copy(
                        firstRelativePoint = DiagramPoint(15.0, 10.0),
                        lastRelativePoint = DiagramPoint(205.0, 105.0),
                        explicitContacts = explicit,
                        bendpoints = if (bent) listOf(DiagramBendpoint(
                            DiagramPoint(100.0, 80.0), DiagramPoint(-100.0, 80.0), 0.3)) else emptyList())
                    val path = DiagramGeometry.path(original)
                    assertStateContact(node.center, path[1], path.first())
                    val changedContact = original.copy(firstRelativePoint = DiagramPoint(5.0, 5.0))
                    assertEquals(path, DiagramGeometry.path(changedContact))
                    val reverse = original.copy(start = other, end = node)
                    val reversed = DiagramGeometry.path(reverse)
                    assertStateContact(node.center, reversed[reversed.lastIndex - 1], reversed.last())
                    val moved = node.copy(location = DiagramPoint(40.0, 30.0))
                    val movedPath = DiagramGeometry.path(original.copy(start = moved))
                    assertStateContact(moved.center, movedPath[1], movedPath.first())
                    if (!other.isActivity) assertStateContact(other.center, path[path.lastIndex - 1], path.last())
                }
            }
        }
        val original = wire(state(0.0, 0.0), state(200.0, 0.0))
        point(DiagramPoint(30.0, 10.0),
            DiagramGeometry.previewStateEndpoint(original, true, DiagramPoint(20.0, 10.0)).first())
    }

    fun testStateEndpointRadialPerimeterPersistsThroughMoveSaveAndUndo() {
        val text = wireFixture().replace("xsi:type=\"Activity\" id=\"b\" name=\"B\" location=\"220,40\"",
            "xsi:type=\"InitialState\" id=\"b\" name=\"B\" location=\"220,45\"")
        withModelEditor(text) { canvas, source, document, file ->
            leftDown(canvas, 170, 55)
            drag(canvas, 220, 55, 15, -5, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            val state = canvas.diagram.nodes.single { it.id == "b" }
            val endpoint = canvas.wirePath(canvas.selectedWire!!).last()
            point(DiagramPoint(220.0, 55.0), endpoint)
            assertEquals(DiagramPoint(235.0, 50.0), canvas.selectedWire!!.lastRelativePoint)
            val anchored = document.text
            val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
            undo.undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(text, document.text)
            undo.redo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(anchored, document.text)
            point(endpoint, canvas.wirePath(canvas.selectedWire!!).last())
            drag(canvas, endpoint.x.roundToInt(), endpoint.y.roundToInt(),
                230 - endpoint.x.roundToInt(), 65 - endpoint.y.roundToInt(), MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(DiagramPoint(230.0, 65.0), canvas.selectedWire!!.lastRelativePoint)
            point(DiagramPoint(220.0, 55.0), canvas.wirePath(canvas.selectedWire!!).last())
            undo.undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(anchored, document.text)
            drag(canvas, state.center.x.toInt(), state.center.y.toInt(), 40, 30, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            val moved = canvas.diagram.wires[0]
            assertEquals(DiagramPoint(275.0, 80.0), moved.lastRelativePoint)
            val movedPath = canvas.wirePath(moved)
            assertStateContact(moved.end!!.center, movedPath[movedPath.lastIndex - 1], movedPath.last())
            FileDocumentManager.getInstance().saveDocument(document)
            assertEquals(document.text, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
            undo.undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(anchored, document.text)
        }
    }

    fun testDirectActivityMidpointSlidesBothAnchorsWithinInsetOverlap() {
        val a = activity(20.0, 40.0)
        for ((b, vertical) in listOf(activity(40.0, 160.0) to true, activity(220.0, 44.0) to false)) {
            for ((position, expected) in listOf(
                (if (vertical) DiagramPoint(48.0, 115.0) else DiagramPoint(170.0, 52.0)) to if (vertical) 48.0 else 52.0,
                (if (vertical) DiagramPoint(112.0, 115.0) else DiagramPoint(170.0, 62.0)) to if (vertical) 112.0 else 62.0,
                (if (vertical) DiagramPoint(80.0, 115.0) else DiagramPoint(170.0, 57.0)) to if (vertical) 80.0 else 57.0)) {
                for ((start, end) in listOf(a to b, b to a)) {
                    val original = wire(start, end)
                    val edited = DiagramEdits.dragMidpoint(original, 0, position)
                    assertEmpty(edited.bendpoints)
                    val path = DiagramGeometry.path(edited)
                    path.forEach { assertEquals(expected, if (vertical) it.x else it.y, 0.000001) }
                    assertEquals(original.start, edited.start)
                    assertEquals(original.end, edited.end)
                }
            }
        }
        val single = wire(a, activity(104.0, 160.0))
        for (x in listOf(112.0)) {
            val edited = DiagramEdits.dragMidpoint(single, 0, DiagramPoint(x, 110.0))
            assertEmpty(edited.bendpoints)
            DiagramGeometry.path(edited).forEach { assertEquals(112.0, it.x, 0.000001) }
        }
        val diagonal = wire(a, activity(220.0, 160.0))
        assertEquals(1, DiagramEdits.dragMidpoint(diagonal, 0, DiagramPoint(170.0, 300.0)).bendpoints.size)
    }

    fun testDirectActivityMidpointSplitsImmediatelyOutsideEitherBoundary() {
        val a = activity(20.0, 40.0)
        for ((b, positions) in listOf(
            activity(40.0, 160.0) to listOf(DiagramPoint(47.9, 115.0), DiagramPoint(112.1, 115.0)),
            activity(220.0, 44.0) to listOf(DiagramPoint(170.0, 51.9), DiagramPoint(170.0, 62.1)),
            activity(104.0, 160.0) to listOf(DiagramPoint(111.9, 115.0), DiagramPoint(112.1, 115.0)))) {
            for ((start, end) in listOf(a to b, b to a)) {
                val original = wire(start, end)
                for (position in positions) {
                    val edited = DiagramEdits.dragMidpoint(original, 0, position)
                    assertEquals(1, edited.bendpoints.size)
                    point(position, DiagramGeometry.path(edited)[1])
                    assertEquals(0.5, edited.bendpoints.single().weight)
                    assertEquals(original.firstRelativePoint, edited.firstRelativePoint)
                }
            }
        }
    }

    fun testDirectActivityMidpointRejectsStartOrEndCrossingInsideSlideRange() {
        val a = activity(20.0, 40.0)
        for ((b, positions) in listOf(
            activity(40.0, 160.0) to listOf(DiagramPoint(80.0, 55.0), DiagramPoint(80.0, 175.0)),
            activity(220.0, 44.0) to listOf(DiagramPoint(70.0, 57.0), DiagramPoint(270.0, 57.0)))) {
            for ((start, end) in listOf(a to b, b to a)) {
                val original = wire(start, end)
                for (position in positions) {
                    val edited = DiagramEdits.dragMidpoint(original, 0, position)
                    assertEquals(1, edited.bendpoints.size)
                    point(position, DiagramGeometry.path(edited)[1])
                    assertEquals(original.firstRelativePoint, edited.firstRelativePoint)
                }
            }
        }
    }

    fun testHorizontalMidpointSlidePersistsWithUndoAndNavigation() = assertMidpointSlidePersists(false)

    fun testVerticalMidpointSlidePersistsWithUndoAndNavigation() = assertMidpointSlidePersists(true)

    fun testHorizontalMidpointSplitPersistsWithUndoAndNavigation() = assertMidpointSlidePersists(false, true)

    fun testVerticalMidpointSplitPersistsWithUndoAndNavigation() = assertMidpointSlidePersists(true, true)

    fun testHorizontalMidpointCrossingPersistsWithUndoAndNavigation() = assertMidpointSlidePersists(false, true, true)

    fun testVerticalMidpointCrossingPersistsWithUndoAndNavigation() = assertMidpointSlidePersists(true, true, true)

    private fun assertMidpointSlidePersists(vertical: Boolean, split: Boolean = false, crossing: Boolean = false) {
            val text = if (vertical) wireFixture().replace("id=\"b\" name=\"B\" location=\"220,40\"",
                "id=\"b\" name=\"B\" location=\"20,160\"") else wireFixture()
            withModelEditor(text) { canvas, source, document, file ->
                val x = if (vertical) 100 else 170
                val y = if (vertical) 115 else 55
                leftDown(canvas, x, y)
                assertEquals(0, canvas.selectedWire!!.index)
                drag(canvas, 350, 250, 20, 30)
                leftDown(canvas, x + 20, y + 30)
                for (position in if (vertical) listOf(DiagramPoint(100.0, 55.0), DiagramPoint(100.0, 175.0))
                    else listOf(DiagramPoint(70.0, 55.0), DiagramPoint(270.0, 55.0))) {
                    canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK,
                        position.x.toInt() + 20, position.y.toInt() + 30, 0, false, MouseEvent.NOBUTTON))
                    assertEquals(1, canvas.selectedWire!!.bendpoints.size)
                    point(position, canvas.wirePath(canvas.selectedWire!!)[1])
                    assertEquals(text, document.text)
                }
                for (outside in listOf(false, true, false, true, split)) {
                    val position = if (vertical) DiagramPoint(if (outside) 113.0 else 112.0, 115.0)
                        else DiagramPoint(170.0, if (outside) 63.0 else 62.0)
                    canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK,
                        position.x.toInt() + 20, position.y.toInt() + 30, 0, false, MouseEvent.NOBUTTON))
                    assertEquals(if (outside) 1 else 0, canvas.selectedWire!!.bendpoints.size)
                    if (outside) point(position, canvas.wirePath(canvas.selectedWire!!)[1])
                    assertEquals(text, document.text)
                }
                val release = if (crossing) {
                    if (vertical) DiagramPoint(100.0, 55.0) else DiagramPoint(270.0, 55.0)
                } else if (vertical) DiagramPoint(if (split) 113.0 else 112.0, 115.0)
                    else DiagramPoint(170.0, if (split) 63.0 else 62.0)
                canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0,
                    release.x.toInt() + 20, release.y.toInt() + 30, 1, false, MouseEvent.BUTTON1))
                UIUtil.dispatchAllInvocationEvents()
                val expected = if (split) canvas.wirePath(canvas.selectedWire!!) else if (vertical) listOf(DiagramPoint(112.0, 70.0), DiagramPoint(112.0, 160.0))
                    else listOf(DiagramPoint(120.0, 62.0), DiagramPoint(220.0, 62.0))
                assertEquals(expected, canvas.wirePath(canvas.selectedWire!!))
                assertEquals(if (split) 1 else 0, canvas.selectedWire!!.bendpoints.size)
                if (split) point(release, expected[1])
                assertTrue(document.text.contains("custom=\"keep\""))
                assertEquals(split, document.text.contains("<bendpoint"))
                val edited = document.text
                val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
                undo.undo(source)
                UIUtil.dispatchAllInvocationEvents()
                assertEquals(text, document.text)
                undo.redo(source)
                UIUtil.dispatchAllInvocationEvents()
                assertEquals(edited, document.text)
                assertEquals(expected, canvas.wirePath(canvas.selectedWire!!))
                FileDocumentManager.getInstance().saveDocument(document)
                assertEquals(edited, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
                point(DiagramPoint(20.0, 30.0), canvas.pan)
            }
    }

    fun testUnconnectedChildrenDoNotBlockBendpointOrMidpointMerge() {
        val start = state(0.0, 40.0)
        val end = state(200.0, 40.0)
        val bend = DiagramBendpoint(DiagramPoint(100.0, 60.0), DiagramPoint(-100.0, 60.0), 0.7)
        for (child in listOf(activity(80.0, 40.0), state(100.0, 40.0))) {
            for (existing in listOf(false, true)) {
                val original = wire(start, end, if (existing) listOf(bend) else emptyList()).copy(condition = "")
                val canvas = ModelDiagramCanvas()
                canvas.display(ModelDiagram(listOf(start, end, child), listOf(original)))
                leftDown(canvas, if (existing) 60 else 40, if (existing) 80 else 50)
                assertNotNull(canvas.selectedWire)
                leftDown(canvas, 110, if (existing) 110 else 50)
                canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK,
                    110, 50, 0, false, MouseEvent.NOBUTTON))
                assertEmpty(canvas.selectedWire!!.bendpoints)
                canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0,
                    110, 50, 1, false, MouseEvent.BUTTON1))
                assertEmpty(canvas.selectedWire!!.bendpoints)
            }
        }
    }

    fun testMergeStillRejectsStartAndEndChildCrossings() {
        val start = state(0.0, 0.0)
        val end = state(200.0, 0.0)
        val straight = wire(start, end)
        val bent = straight.copy(bendpoints = listOf(DiagramBendpoint(
            DiagramPoint(100.0, 60.0), DiagramPoint(-100.0, 60.0), 0.7)))
        for (position in listOf(DiagramPoint(-50.0, 10.0), DiagramPoint(270.0, 10.0))) {
            for (result in listOf(DiagramEdits.dragBendpoint(bent, 0, position),
                DiagramEdits.dragMidpoint(straight, 0, position))) {
                assertEquals(1, result.bendpoints.size)
                point(position, DiagramGeometry.path(result)[1])
            }
        }
        assertEmpty(DiagramEdits.dragBendpoint(bent, 0, DiagramPoint(110.0, 15.0)).bendpoints)
        assertEmpty(DiagramEdits.dragMidpoint(straight, 0, DiagramPoint(110.0, 15.0)).bendpoints)
        val source = activity(0.0, 0.0)
        val target = state(200.0, 5.0)
        fun at(x: Double, y: Double) = DiagramBendpoint(
            DiagramPoint(x - source.center.x, y - source.center.y),
            DiagramPoint(x - target.center.x, y - target.center.y), 0.5)
        val returning = wire(source, target, listOf(at(-30.0, 15.0), at(120.0, 70.0)))
        assertEquals(2, DiagramEdits.dragBendpoint(returning, 1, DiagramPoint(120.0, 15.0)).bendpoints.size)
    }
    private fun activity(x: Double, y: Double, width: Double = 100.0) =
        DiagramNode(DiagramNodeKind.Activity, "a", "Activity", DiagramPoint(x, y), width, 30.0)
    private fun assertStateContact(center: DiagramPoint, neighbor: DiagramPoint, contact: DiagramPoint) {
        val dx = neighbor.x - center.x
        val dy = neighbor.y - center.y
        val length = hypot(dx, dy)
        val expected = if (length == 0.0) center else
            center + DiagramPoint(dx * 10.0 / length, dy * 10.0 / length)
        point(expected, contact)
    }

    private fun state(x: Double, y: Double) =
        DiagramNode(DiagramNodeKind.InitialState, "s", "", DiagramPoint(x, y), 20.0, 20.0)
    private fun wire(a: DiagramNode?, b: DiagramNode?, bends: List<DiagramBendpoint> = emptyList()) =
        DiagramWire(0, "99", "success", a, b, DiagramPoint(40.0, 40.0), null, bends)
    private fun point(expected: DiagramPoint, actual: DiagramPoint) {
        assertEquals(expected.x, actual.x, 0.000001)
        assertEquals(expected.y, actual.y, 0.000001)
    }
    private fun parse(text: String): ModelDiagram = ModelDiagramParser.parse(
        myFixture.configureByText("diagram.glue_uml_ad", text) as XmlFile)
    private fun sample() = Files.readString(Path.of("modeleditor/sample.glue_uml_ad")).replace("\r\n", "\n")

    fun testSampleParsingUsesWireOrderAndFixedSizes() {
        val diagram = parse(sample())
        assertNull(diagram.message)
        assertEquals(8, diagram.nodes.size)
        assertEquals(12, diagram.wires.size)
        assertTrue(diagram.nodes.filter { it.isActivity }.all { it.height == 30.0 })
        assertTrue(diagram.nodes.filterNot { it.isActivity }.all { it.width == 20.0 && it.height == 20.0 })
        assertEquals(505.0, diagram.nodes.single { it.name == "Router" }.width)
        val delete = diagram.wires[2]
        assertEquals("3", delete.id)
        assertEquals("Router", delete.start!!.name)
        assertEquals("삭제", delete.end!!.name)
        assertTrue(diagram.wires.all { DiagramGeometry.path(it).size >= 2 })
        assertEquals(0.5, diagram.wires[5].bendpoints.single().weight)
    }

    fun testMissingEndpointsAndUnknownTypesAreSkipped() {
        val diagram = parse("""<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
            <child xsi:type="Activity" id="0" size="80,900" output="//@wire.0"/>
            <child xsi:type="Unknown" input="//@wire.0"/>
            <wire id="0"/><wire id="1"/>
            </GlueDiagram>""")
        assertEquals(1, diagram.nodes.size)
        assertEquals(30.0, diagram.nodes.single().height)
        assertTrue(diagram.wires.all { DiagramGeometry.path(it).isEmpty() })
        assertNotNull(parse("<GlueDiagram>").message)
        assertNotNull(parse("").message)
    }

    fun testStateRadialPerimetersAndVerticalStateActivityConnectionsAreSymmetric() {
        val a = state(10.0, 10.0)
        val b = state(100.0, 100.0)
        val diagonal = DiagramGeometry.path(wire(a, b))
        assertStateContact(a.center, diagonal[1], diagonal.first())
        assertStateContact(b.center, diagonal[0], diagonal.last())
        val activity = activity(0.0, 100.0)
        val points = DiagramGeometry.path(wire(a, activity))
        point(DiagramPoint(20.0, 30.0), points[0])
        point(DiagramPoint(20.0, 100.0), points[1])
        assertEquals(points.reversed(), DiagramGeometry.path(wire(activity, a)))
        val below = state(40.0, 200.0)
        point(DiagramPoint(50.0, 130.0), DiagramGeometry.path(wire(activity, below))[0])
    }

    fun testActivityOverlapPrefersVerticalAndClampsBothAxes() {
        val a = activity(0.0, 0.0)
        val b = activity(50.0, 100.0)
        val (start, end) = DiagramGeometry.endpoints(a, b, DiagramPoint(-99.0, 0.0))
        point(DiagramPoint(58.0, 30.0), start)
        point(DiagramPoint(58.0, 100.0), end)
        val reverse = DiagramGeometry.endpoints(b, a, DiagramPoint(999.0, 0.0))
        point(DiagramPoint(92.0, 100.0), reverse.first)
        point(DiagramPoint(92.0, 30.0), reverse.second)
        val horizontal = DiagramGeometry.endpoints(a, activity(200.0, 10.0), DiagramPoint(0.0, 999.0))
        point(DiagramPoint(100.0, 22.0), horizontal.first)
        point(DiagramPoint(200.0, 22.0), horizontal.second)
        val both = DiagramGeometry.endpoints(a, activity(50.0, 10.0), DiagramPoint(70.0, 20.0))
        assertEquals(70.0, both.first.x)
        assertEquals(70.0, both.second.x)
    }

    fun testActivityDirectConnectionUsesInsetRangesAndClosestEdgesBothDirections() {
        val a = activity(0.0, 0.0)
        val b = activity(50.0, 100.0)
        for ((hint, x) in listOf(-100.0 to 58.0, 70.0 to 70.0, 1000.0 to 92.0)) {
            val forward = DiagramGeometry.endpoints(a, b, DiagramPoint(hint, 0.0))
            point(DiagramPoint(x, 30.0), forward.first)
            point(DiagramPoint(x, 100.0), forward.second)
            assertEquals(forward.second to forward.first, DiagramGeometry.endpoints(b, a, DiagramPoint(hint, 0.0)))
        }
        val right = activity(200.0, 10.0)
        for ((hint, y) in listOf(-100.0 to 18.0, 20.0 to 20.0, 1000.0 to 22.0)) {
            val forward = DiagramGeometry.endpoints(a, right, DiagramPoint(0.0, hint))
            point(DiagramPoint(100.0, y), forward.first)
            point(DiagramPoint(200.0, y), forward.second)
            assertEquals(forward.second to forward.first, DiagramGeometry.endpoints(right, a, DiagramPoint(0.0, hint)))
        }
        val overlapping = DiagramGeometry.endpoints(a, activity(50.0, 10.0), DiagramPoint(70.0, 0.0))
        assertEquals(10.0, kotlin.math.abs(overlapping.first.y - overlapping.second.y))
        val touchingInset = DiagramGeometry.endpoints(a, activity(84.0, 100.0), DiagramPoint(0.0, 0.0))
        assertEquals(92.0, touchingInset.first.x)
        assertEquals(92.0, touchingInset.second.x)
        val narrow = DiagramGeometry.endpoints(a, activity(95.0, 100.0), DiagramPoint(0.0, 0.0))
        assertTrue(narrow.first.x.isFinite() && narrow.first.y.isFinite())
        assertTrue(narrow.first.x != narrow.second.x && narrow.first.y != narrow.second.y)
        val thinYOverlap = DiagramGeometry.endpoints(a, activity(200.0, 25.0), DiagramPoint(0.0, 0.0))
        assertTrue(thinYOverlap.first.x != thinYOverlap.second.x && thinYOverlap.first.y != thinYOverlap.second.y)
    }

    fun testDisjointCornersUseRadiusNineInAllQuadrants() {
        val a = activity(0.0, 0.0)
        for (x in listOf(-200.0, 200.0)) for (y in listOf(-100.0, 100.0)) {
            val b = activity(x, y)
            val (start, end) = DiagramGeometry.endpoints(a, b, a.center)
            val cornerA = DiagramPoint(if (x > 0) 92.0 else 8.0, if (y > 0) 22.0 else 8.0)
            val cornerB = DiagramPoint(x + if (x > 0) 8.0 else 92.0, y + if (y > 0) 8.0 else 22.0)
            assertEquals(9.0, hypot(start.x - cornerA.x, start.y - cornerA.y), 0.000001)
            assertEquals(9.0, hypot(end.x - cornerB.x, end.y - cornerB.y), 0.000001)
            val s = state(x, y)
            val mixed = DiagramGeometry.path(wire(a, s))
            assertEquals(9.0, hypot(mixed[0].x - cornerA.x, mixed[0].y - cornerA.y), 0.000001)
            assertEquals(10.0, hypot(mixed[1].x - s.center.x, mixed[1].y - s.center.y), 0.000001)
            assertEquals(mixed.reversed(), DiagramGeometry.path(wire(s, a)))
        }
    }

    fun testMultipleBendsInterpolateByIndexAndIgnoreWeight() {
        val a = activity(0.0, 0.0)
        val b = activity(200.0, 100.0)
        val bend = DiagramBendpoint(DiagramPoint(0.0, 0.0), DiagramPoint(0.0, 0.0), 0.99)
        val points = DiagramGeometry.path(wire(a, b, listOf(bend, bend)))
        assertEquals(4, points.size)
        point(DiagramPoint(50 + 200.0 / 3, 15 + 100.0 / 3), points[1])
        point(DiagramPoint(50 + 400.0 / 3, 15 + 200.0 / 3), points[2])
        assertEquals(points, DiagramGeometry.path(wire(a, b, listOf(bend.copy(weight = 0.0), bend.copy(weight = 1.0)))))
        val centered = DiagramGeometry.path(wire(a, activity(0.0, 100.0), listOf(bend)))
        point(DiagramPoint(50.0, 30.0), centered.first())
        point(DiagramPoint(50.0, 100.0), centered.last())
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    fun testConditionCenterUsesMiddleSegmentForZeroToFourBends() {
        assertNull(DiagramGeometry.labelCenter(emptyList()))
        for (n in 0..4) {
            val points = (0..n + 1).map { DiagramPoint(it * 30.0, it * it * 10.0) }
            val segment = n / 2
            point(DiagramPoint((points[segment].x + points[segment + 1].x) / 2,
                (points[segment].y + points[segment + 1].y) / 2), DiagramGeometry.labelCenter(points)!!)
        }
    }

    private fun render(canvas: ModelDiagramCanvas): BufferedImage {
        canvas.setSize(canvas.preferredSize)
        val image = BufferedImage(canvas.width, canvas.height, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { graphics -> try { canvas.paint(graphics) } finally { graphics.dispose() } }
        return image
    }

    fun testDefaultBackgroundWhiteActivitiesGrayWiresAndLargerFinalCircle() {
        val canvas = ModelDiagramCanvas()
        assertEquals(UIUtil.getPanelBackground(), canvas.background)
        val diagram = parse(sample())
        canvas.display(diagram)
        for ((name, background) in listOf("light" to Color(242, 242, 242), "dark" to Color(43, 43, 43))) {
            canvas.background = background
            val image = render(canvas)
            assertEquals(background.rgb, image.getRGB(5, 5))
            assertEquals(Color.WHITE.rgb, image.getRGB(45, 75))
            assertEquals(Color.BLACK.rgb, image.getRGB(203, 244))
            assertEquals(Color.BLACK.rgb, image.getRGB(203, 256))
            val wirePixels = (100..115).map { image.getRGB(61, it) }
            assertTrue(wirePixels.any { it != background.rgb })
            assertTrue(wirePixels.all { rgb -> val c = Color(rgb, true); c.red == c.green && c.green == c.blue })
            val path = DiagramGeometry.path(diagram.wires[1])
            val center = DiagramGeometry.labelCenter(path)!!
            val metrics = canvas.getFontMetrics(javax.swing.UIManager.getFont("Label.font"))
            val labelBottom = (center.y + (metrics.height - 3) / 2.0 - 1).toInt()
            assertEquals(background.rgb, image.getRGB(center.x.toInt(), labelBottom))
            assertTrue((0 until image.width).any { x -> (0 until image.height).any { y -> image.getRGB(x, y) == Color(145, 145, 145).rgb } })
            val output = Path.of("build/reports/model-diagram-$name.png")
            Files.createDirectories(output.parent)
            ImageIO.write(image, "png", output.toFile())
        }
    }

    private fun drag(canvas: ModelDiagramCanvas, x: Int, y: Int, dx: Int, dy: Int, button: Int = MouseEvent.BUTTON3) {
        val mask = (if (button == MouseEvent.BUTTON3) MouseEvent.BUTTON3_DOWN_MASK else MouseEvent.BUTTON1_DOWN_MASK) or MouseEvent.ALT_DOWN_MASK
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0, mask, x, y, 1, false, button))
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, mask, x + dx, y + dy, 0, false, MouseEvent.NOBUTTON))
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, MouseEvent.ALT_DOWN_MASK, x + dx, y + dy, 1, false, button))
    }

    fun testRightDragAnywherePansWithoutMovingNodesOrXml() {
        val xml = sample()
        val diagram = parse(xml)
        val canvas = ModelDiagramCanvas()
        canvas.display(diagram)
        canvas.setSize(canvas.preferredSize)
        drag(canvas, 45, 75, 10, 10)
        point(DiagramPoint(10.0, 10.0), canvas.pan)
        drag(canvas, 71, 115, -10, -10)
        point(DiagramPoint(0.0, 0.0), canvas.pan)
        drag(canvas, 600, 280, 40, 50)
        point(DiagramPoint(40.0, 50.0), canvas.pan)
        assertEquals(java.awt.Cursor.DEFAULT_CURSOR, canvas.cursor.type)
        drag(canvas, 45 + 40, 75 + 50, -20, -20)
        point(DiagramPoint(20.0, 30.0), canvas.pan)
        drag(canvas, 600, 280, -70, -90)
        point(DiagramPoint(-50.0, -60.0), canvas.pan)
        canvas.display(diagram)
        point(DiagramPoint(-50.0, -60.0), canvas.pan)
        assertEquals(diagram, canvas.diagram)
        assertEquals(xml, myFixture.file.text)
    }

    private fun leftDown(canvas: ModelDiagramCanvas, x: Int, y: Int, shift: Boolean = false) {
        val modifiers = MouseEvent.BUTTON1_DOWN_MASK or if (shift) MouseEvent.SHIFT_DOWN_MASK else 0
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 0, modifiers, x, y, 1, false, MouseEvent.BUTTON1))
    }

    fun testClickShiftSelectionAndEightHandlesIdentifyLastSelectedChild() {
        val canvas = ModelDiagramCanvas()
        canvas.display(parse(sample()))
        leftDown(canvas, 45, 75)
        assertEquals(listOf("Router"), canvas.selectedNodes.map { it.name })
        leftDown(canvas, 100, 135, true)
        assertEquals(listOf("Router", "삭제"), canvas.selectedNodes.map { it.name })
        assertEquals("삭제", canvas.lastSelectedNode!!.name)
        val image = render(canvas)
        assertEquals(Color.WHITE.rgb, image.getRGB(36, 60))
        val handles = listOf(84 to 120, 127 to 120, 169 to 120, 169 to 135,
            169 to 150, 127 to 150, 84 to 150, 84 to 135)
        for ((x, y) in handles) {
            assertEquals(Color.BLACK.rgb, image.getRGB(x, y))
            assertEquals(Color.BLACK.rgb, image.getRGB(x - 1, y - 1))
            assertEquals(Color.BLACK.rgb, image.getRGB(x - 2, y - 2))
            assertEquals(Color.WHITE.rgb, image.getRGB(x - 3, y - 3))
            assertEquals(Color.WHITE.rgb, image.getRGB(x + 2, y + 2))
        }
        assertEquals(Color.BLACK.rgb, image.getRGB(33, 57))
        assertEquals(Color.WHITE.rgb, image.getRGB(34, 58))
        val output = Path.of("build/reports/model-diagram-selection.png")
        Files.createDirectories(output.parent)
        ImageIO.write(image, "png", output.toFile())
        leftDown(canvas, 45, 75, true)
        assertEquals(listOf("삭제", "Router"), canvas.selectedNodes.map { it.name })
        leftDown(canvas, 202, 34)
        assertEquals(listOf(DiagramNodeKind.InitialState), canvas.selectedNodes.map { it.kind })
        val before = canvas.selectedNodes
        drag(canvas, 202, 34, 20, 20)
        assertEquals(before, canvas.selectedNodes)
        leftDown(canvas, 45 + 20, 75 + 20)
        assertEquals(listOf("Router"), canvas.selectedNodes.map { it.name })
        canvas.display(canvas.diagram)
        assertEquals(listOf("Router"), canvas.selectedNodes.map { it.name })
        leftDown(canvas, 600, 280)
        assertEmpty(canvas.selectedNodes)
    }

    fun testLeftBoxSelectsOnlyFullyEnclosedChildrenInEitherDirection() {
        val canvas = ModelDiagramCanvas()
        val xml = sample()
        canvas.display(parse(xml))
        canvas.setSize(canvas.preferredSize)
        leftDown(canvas, 70, 110)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK,
            400, 220, 0, false, MouseEvent.NOBUTTON))
        assertNotNull(canvas.selectionBox)
        assertEquals(setOf("등록", "삭제", "수정"), canvas.selectedNodes.map { it.name }.toSet())
        assertFalse(canvas.selectedNodes.any { it.name == "Search[조회]" })
        point(DiagramPoint(0.0, 0.0), canvas.pan)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 400, 220, 1, false, MouseEvent.BUTTON1))
        assertNull(canvas.selectionBox)
        drag(canvas, 400, 220, -330, -110, MouseEvent.BUTTON1)
        assertEquals(setOf("등록", "삭제", "수정"), canvas.selectedNodes.map { it.name }.toSet())
        drag(canvas, 180, 10, 40, 40, MouseEvent.BUTTON1)
        assertEquals(listOf(DiagramNodeKind.InitialState), canvas.selectedNodes.map { it.kind })
        drag(canvas, 600, 280, 20, 20)
        drag(canvas, 70 + 20, 110 + 20, 330, 110, MouseEvent.BUTTON1)
        assertEquals(setOf("등록", "삭제", "수정"), canvas.selectedNodes.map { it.name }.toSet())
        assertEquals(xml, myFixture.file.text)
    }

    fun testLabelBackgroundIsThreePixelsShorterAndChildTextIsGray() {
        val canvas = ModelDiagramCanvas()
        canvas.font = java.awt.Font("Dialog", java.awt.Font.PLAIN, 12)
        canvas.background = Color(242, 242, 242)
        val diagram = parse(sample())
        canvas.display(diagram)
        val image = render(canvas)
        val center = DiagramGeometry.labelCenter(DiagramGeometry.path(diagram.wires[1]))!!
        val metrics = canvas.getFontMetrics(canvas.font)
        val outsideY = (center.y - metrics.height / 2).toInt()
        assertTrue(image.getRGB(center.x.toInt(), outsideY) != canvas.background.rgb)
        val insideY = (center.y + (metrics.height - 3) / 2.0 - 1).toInt()
        assertEquals(canvas.background.rgb, image.getRGB(center.x.toInt(), insideY))
        val textPixels = (260..315).flatMap { x -> (65..85).map { y -> image.getRGB(x, y) } }
        assertFalse(textPixels.contains(Color.BLACK.rgb))
        assertTrue(textPixels.contains(Color(145, 145, 145).rgb))
    }

    fun testWireThresholdLabelHitAndEndpointAndBendpointHandles() {
        val a = state(40.0, 40.0).copy(kind = DiagramNodeKind.FinalState)
        val b = state(140.0, 40.0)
        val straight = wire(a, b).copy(condition = "", index = 7)
        val canvas = ModelDiagramCanvas()
        canvas.display(ModelDiagram(listOf(a, b), listOf(straight)))
        leftDown(canvas, 90, 54)
        assertEquals(7, canvas.selectedWire!!.index)
        assertEmpty(canvas.selectedNodes)
        leftDown(canvas, 90, 55)
        assertNull(canvas.selectedWire)
        val bent = straight.copy(condition = "guard", bendpoints = listOf(
            DiagramBendpoint(DiagramPoint(0.0, 50.0), DiagramPoint(0.0, 50.0), 0.8)))
        canvas.display(ModelDiagram(listOf(a, b), listOf(bent)))
        val label = DiagramGeometry.labelCenter(DiagramGeometry.path(bent))!!
        leftDown(canvas, label.x.roundToInt() + 5, label.y.roundToInt() - 4)
        assertEquals(7, canvas.selectedWire!!.index)
        val image = render(canvas)
        val selectedPath = canvas.wirePath(canvas.selectedWire!!)
        for ((x, y) in listOf(selectedPath.first(), selectedPath.last()).map { it.x.roundToInt() to it.y.roundToInt() }) {
            assertEquals(Color.BLACK.rgb, image.getRGB(x - 2, y - 2))
            assertEquals(Color.WHITE.rgb, image.getRGB(x - 3, y - 3))
        }
        assertEquals(Color.BLACK.rgb, image.getRGB(100, 100))
        assertEquals(Color.BLACK.rgb, image.getRGB(98, 98))
        assertEquals(Color.WHITE.rgb, image.getRGB(97, 97))
        assertEquals(Color.WHITE.rgb, image.getRGB(102, 102))
        for ((x, y) in selectedPath.zipWithNext().map { (a, b) -> ((a.x + b.x) / 2).roundToInt() to ((a.y + b.y) / 2).roundToInt() }) {
            assertEquals(Color.BLACK.rgb, image.getRGB(x, y))
            assertEquals(Color.WHITE.rgb, image.getRGB(x - 2, y - 2))
            assertEquals(Color.WHITE.rgb, image.getRGB(x + 1, y + 1))
        }
        val output = Path.of("build/reports/model-wire-selection.png")
        Files.createDirectories(output.parent)
        ImageIO.write(image, "png", output.toFile())
        drag(canvas, 300, 200, 20, 30)
        assertNotNull(canvas.selectedWire)
        leftDown(canvas, label.x.roundToInt() + 25, label.y.roundToInt() + 26)
        assertEquals(7, canvas.selectedWire!!.index)
        leftDown(canvas, 70, 73)
        assertNull(canvas.selectedWire)
        assertEquals(1, canvas.selectedNodes.size)
    }

    fun testChildDragPreviewsAndCommitsOnlyOnRelease() {
        val canvas = ModelDiagramCanvas()
        val diagram = parse(sample())
        canvas.display(diagram)
        var commits = 0
        canvas.onNodesMoved = { moves ->
            commits++
            assertEquals(DiagramPoint(66.0, 100.0), moves.entries.single().value)
            canvas.display(canvas.diagram)
        }
        val oldPath = DiagramGeometry.path(diagram.wires[0])
        leftDown(canvas, 45, 75)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK or MouseEvent.ALT_DOWN_MASK,
            75, 115, 0, false, MouseEvent.NOBUTTON))
        assertEquals(0, commits)
        assertEquals(DiagramPoint(66.0, 100.0), canvas.diagram.nodes.single { it.name == "Router" }.location)
        assertFalse(oldPath == DiagramGeometry.path(canvas.diagram.wires[0]))
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, MouseEvent.ALT_DOWN_MASK, 75, 115, 1, false, MouseEvent.BUTTON1))
        assertEquals(1, commits)
        point(DiagramPoint(0.0, 0.0), canvas.pan)
        canvas.canMoveNodes = { false }
        drag(canvas, 80, 115, 20, 20, MouseEvent.BUTTON1)
        assertEquals(1, commits)
        assertEquals(DiagramPoint(66.0, 100.0), canvas.diagram.nodes.single { it.name == "Router" }.location)
    }

    fun testLastSelectedChildAndWireArePaintedAndHitInFront() {
        val a = activity(20.0, 40.0).copy(id = "a", name = "A")
        val b = activity(60.0, 50.0).copy(id = "b", name = "B")
        val canvas = ModelDiagramCanvas()
        canvas.display(ModelDiagram(listOf(a, b), emptyList()))
        leftDown(canvas, 25, 55)
        assertEquals("a", canvas.lastSelectedNode!!.id)
        assertEquals(Color.WHITE.rgb, render(canvas).getRGB(80, 50))
        leftDown(canvas, 80, 55)
        assertEquals("a", canvas.lastSelectedNode!!.id)
        leftDown(canvas, 300, 200)
        leftDown(canvas, 80, 55)
        assertEquals("b", canvas.lastSelectedNode!!.id)
        assertEquals(Color.WHITE.rgb, render(canvas).getRGB(80, 70))

        val start = state(40.0, 40.0).copy(id = "start")
        val end = state(140.0, 40.0).copy(id = "end")
        val blocker = activity(80.0, 35.0, 40.0).copy(id = "block", name = "")
        canvas.display(ModelDiagram(listOf(start, end, blocker), listOf(wire(start, end).copy(condition = ""))))
        leftDown(canvas, 65, 50)
        assertNotNull(canvas.selectedWire)
        assertEquals(Color.BLACK.rgb, render(canvas).getRGB(100, 50))
        leftDown(canvas, 100, 50)
        assertNotNull(canvas.selectedWire)
        leftDown(canvas, 300, 200)
        leftDown(canvas, 100, 50)
        assertEquals("block", canvas.lastSelectedNode!!.id)
    }

    fun testNavigationCoordinatesStayAtTopLeftAndLabelGetsBottomPixel() {
        val canvas = ModelDiagramCanvas()
        canvas.font = java.awt.Font("Dialog", java.awt.Font.PLAIN, 12)
        canvas.display(parse(sample()))
        val before = render(canvas)
        val center = DiagramGeometry.labelCenter(DiagramGeometry.path(canvas.diagram.wires[1]))!!
        val oldHeight = canvas.getFontMetrics(canvas.font).height - 3
        assertEquals(canvas.background.rgb, before.getRGB(center.x.toInt(), (center.y + oldHeight / 2.0).toInt()))
        drag(canvas, 600, 280, 40, 50)
        val after = render(canvas)
        val prefixBefore = (8..17).flatMap { x -> (8..24).map { y -> before.getRGB(x, y) } }
        val prefixAfter = (8..17).flatMap { x -> (8..24).map { y -> after.getRGB(x, y) } }
        assertEquals(prefixBefore, prefixAfter)
        assertTrue(prefixBefore.any { it != canvas.background.rgb })
        assertFalse((18..100).flatMap { x -> (8..24).map { y -> before.getRGB(x, y) } } ==
            (18..100).flatMap { x -> (8..24).map { y -> after.getRGB(x, y) } })
    }

    fun testActivitySideHandlesResizeKeepOppositeEdgeAndClampMinimum() {
        val canvas = ModelDiagramCanvas()
        canvas.display(parse(sample()))
        leftDown(canvas, 45, 75)
        drag(canvas, 541, 75, 40, 0, MouseEvent.BUTTON1)
        var node = canvas.diagram.nodes.single { it.name == "Router" }
        assertEquals(545.0, node.width)
        assertEquals(DiagramPoint(36.0, 60.0), node.location)
        drag(canvas, 36, 75, 20, 10, MouseEvent.BUTTON1)
        node = canvas.diagram.nodes.single { it.name == "Router" }
        assertEquals(525.0, node.width)
        assertEquals(DiagramPoint(56.0, 60.0), node.location)
        assertEquals(581.0, node.location.x + node.width)
        assertEquals(30.0, node.height)
        drag(canvas, 56, 75, 1000, 0, MouseEvent.BUTTON1)
        node = canvas.diagram.nodes.single { it.name == "Router" }
        assertEquals(16.0, node.width)
        assertEquals(581.0, node.location.x + node.width)
        canvas.canMoveNodes = { false }
        drag(canvas, 581, 75, 50, 0, MouseEvent.BUTTON1)
        assertEquals(16.0, canvas.diagram.nodes.single { it.name == "Router" }.width)
        point(DiagramPoint(0.0, 0.0), canvas.pan)
    }

    fun testEditorResizePreservesXmlHeightAndSupportsUndoRedoAndSave() {
        val original = sample()
        val file = myFixture.addFileToProject("resize.glue_uml_ad", original).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val source = ModelSourceEditorProvider().createEditor(project, file) as com.intellij.openapi.fileEditor.TextEditor
        try {
            com.intellij.testFramework.EditorTestUtil.waitForLoading(source.editor)
            val canvas = descendants(editor.component).filterIsInstance<ModelDiagramCanvas>().single()
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            leftDown(canvas, 45, 75)
            drag(canvas, 541, 75, 40, 0, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            val wider = document.text
            assertTrue(wider.contains("size=\"545,-1\""))
            assertTrue(wider.contains("firstDimension=\"72,172\""))
            val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
            undo.undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(original, document.text)
            undo.redo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(wider, document.text)
            drag(canvas, 36, 75, 20, 0, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            val leftResized = document.text
            assertTrue(leftResized.contains("location=\"56,60\""))
            assertTrue(leftResized.contains("size=\"525,-1\""))
            assertTrue(leftResized.contains("firstDimension=\"62,172\""))
            undo.undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(wider, document.text)
            FileDocumentManager.getInstance().saveDocument(document)
            assertEquals(wider, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
            WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
            drag(canvas, 581, 75, 40, 0, MouseEvent.BUTTON1)
            assertEquals(wider, document.text)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { file.isWritable = true }
            Disposer.dispose(editor)
            Disposer.dispose(source)
        }
    }

    fun testEditorChildMoveUpdatesOnlyLocationSupportsUndoSaveAndReadOnly() {
        val original = sample()
        val file = myFixture.addFileToProject("move.glue_uml_ad", original).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val source = com.look4counter.modeleditor.ModelSourceEditorProvider().createEditor(project, file) as com.intellij.openapi.fileEditor.TextEditor
        try {
            com.intellij.testFramework.EditorTestUtil.waitForLoading(source.editor)
            val canvas = descendants(editor.component).filterIsInstance<ModelDiagramCanvas>().single()
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            drag(canvas, 45, 75, 30, 40, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            val expected = original.replace("location=\"36,60\"", "location=\"66,100\"")
            assertEquals(expected, document.text)
            assertTrue(editor.isModified)
            val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
            assertTrue(undo.isUndoAvailable(source))
            undo.undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(original, document.text)
            assertEquals(DiagramPoint(36.0, 60.0), canvas.diagram.nodes.single { it.name == "Router" }.location)
            undo.redo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(expected, document.text)
            FileDocumentManager.getInstance().saveDocument(document)
            assertEquals(expected, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
            assertFalse(editor.isModified)
            WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
            drag(canvas, 80, 115, 30, 40, MouseEvent.BUTTON1)
            assertEquals(expected, document.text)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { file.isWritable = true }
            Disposer.dispose(editor)
            Disposer.dispose(source)
        }
    }

    fun testSourceChangeDuringChildDragCancelsPendingMove() {
        val file = myFixture.addFileToProject("changed.glue_uml_ad", sample()).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = descendants(editor.component).filterIsInstance<ModelDiagramCanvas>().single()
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            leftDown(canvas, 45, 75)
            canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK, 75, 115, 0, false, MouseEvent.NOBUTTON))
            val latest = document.text.replace("name=\"Router\"", "name=\"Latest\"")
            WriteCommandAction.runWriteCommandAction(project) { document.setText(latest) }
            canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 75, 115, 1, false, MouseEvent.BUTTON1))
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(latest, document.text)
            assertEquals(DiagramPoint(36.0, 60.0), canvas.diagram.nodes.single { it.name == "Latest" }.location)
        } finally { Disposer.dispose(editor) }
    }

    fun testMovingChildNegativeKeepsOtherNodesAndNavigationFixed() {
        val canvas = ModelDiagramCanvas()
        val original = parse(sample())
        canvas.display(original)
        val before = render(canvas)
        drag(canvas, 45, 75, -200, -100, MouseEvent.BUTTON1)
        val after = render(canvas)
        point(DiagramPoint(0.0, 0.0), canvas.pan)
        assertEquals(DiagramPoint(-164.0, -40.0), canvas.diagram.nodes.single { it.name == "Router" }.location)
        assertEquals(original.nodes.filter { it.name != "Router" }, canvas.diagram.nodes.filter { it.name != "Router" })
        assertEquals(before.getRGB(202, 34), after.getRGB(202, 34))
        assertEquals(Color.BLACK.rgb, after.getRGB(202, 34))
        canvas.display(canvas.diagram)
        assertEquals(Color.BLACK.rgb, render(canvas).getRGB(202, 34))
    }

    private fun hover(canvas: ModelDiagramCanvas, x: Int, y: Int) {
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_MOVED, 0, 0, x, y, 0, false, MouseEvent.NOBUTTON))
    }

    fun testActivityAndWireHandleHoverCursorsAfterNavigation() {
        val canvas = ModelDiagramCanvas()
        canvas.display(parse(sample()))
        leftDown(canvas, 45, 75)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 1, 0, 45, 75, 1, false, MouseEvent.BUTTON1))
        hover(canvas, 36, 75)
        assertEquals(java.awt.Cursor.W_RESIZE_CURSOR, canvas.cursor.type)
        hover(canvas, 541, 75)
        assertEquals(java.awt.Cursor.E_RESIZE_CURSOR, canvas.cursor.type)
        hover(canvas, 600, 280)
        assertEquals(java.awt.Cursor.DEFAULT_CURSOR, canvas.cursor.type)
        drag(canvas, 600, 280, 40, 50)
        hover(canvas, 76, 125)
        assertEquals(java.awt.Cursor.W_RESIZE_CURSOR, canvas.cursor.type)
        leftDown(canvas, 600, 280)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 1, 0, 600, 280, 1, false, MouseEvent.BUTTON1))
        leftDown(canvas, 420, 160)
        assertEquals(5, canvas.selectedWire!!.index)
        val points = canvas.wirePath(canvas.selectedWire!!)
        val handles = points + points.zipWithNext().map { (a, b) -> DiagramPoint((a.x + b.x) / 2, (a.y + b.y) / 2) }
        for (point in handles) {
            hover(canvas, point.x.roundToInt() + 40, point.y.roundToInt() + 50)
            assertEquals(java.awt.Cursor.MOVE_CURSOR, canvas.cursor.type)
        }
        hover(canvas, 600, 280)
        assertEquals(java.awt.Cursor.DEFAULT_CURSOR, canvas.cursor.type)
    }

    fun testInvalidWireEndpointDropsRestoreOriginalConnectionAndShape() {
        val xml = sample()
        val canvas = ModelDiagramCanvas()
        val diagram = parse(xml)
        canvas.display(diagram)
        leftDown(canvas, 380, 110)
        val wire = canvas.selectedWire!!
        assertEquals(5, wire.index)
        val original = canvas.wirePath(wire)
        leftDown(canvas, original.first().x.roundToInt(), original.first().y.roundToInt())
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK, 280, 110, 0, false, MouseEvent.NOBUTTON))
        val previewFirst = canvas.wirePath(wire).first()
        assertEquals(10.0, hypot(previewFirst.x - 280, previewFirst.y - 110), 0.000001)
        val startCenter = wire.start!!.center
        point(DiagramPoint(original[1].x + (280 - startCenter.x) / 2,
            original[1].y + (110 - startCenter.y) / 2), canvas.wirePath(wire)[1])
        val previewLast = canvas.wirePath(wire).last()
        assertEquals(10.0, hypot(previewLast.x - wire.end!!.center.x, previewLast.y - wire.end!!.center.y), 0.000001)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 280, 110, 1, false, MouseEvent.BUTTON1))
        assertEquals(original, canvas.wirePath(wire))
        drag(canvas, 600, 280, 40, 50)
        val end = canvas.wirePath(wire).last()
        drag(canvas, end.x.roundToInt() + 40, end.y.roundToInt() + 50,
            500 - end.x.roundToInt() - 40, 200 - end.y.roundToInt() - 50, MouseEvent.BUTTON1)
        assertEquals(original, canvas.wirePath(wire))
        assertEquals(original[1], canvas.wirePath(wire)[1])
        assertEquals(diagram.nodes, canvas.diagram.nodes)
        assertEquals(diagram.wires, canvas.diagram.wires)
        assertEquals(xml, myFixture.file.text)
        point(DiagramPoint(40.0, 50.0), canvas.pan)
        val image = render(canvas)
        val output = Path.of("build/reports/model-wire-endpoint-drag.png")
        Files.createDirectories(output.parent)
        ImageIO.write(image, "png", output.toFile())
        canvas.display(diagram)
        assertEquals(original, canvas.wirePath(wire))
    }

    private fun wireFixture(bend: Boolean = false) = """<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <child xsi:type="Activity" id="a" name="A" location="20,40" size="100,-1" output="//@wire.0 //@wire.1"/>
        <child xsi:type="Activity" id="b" name="B" location="220,40" size="100,-1" input="//@wire.0"/>
        <child xsi:type="Activity" id="c" name="C" location="220,160" size="80,-1" input="//@wire.1"/>
        <wire id="99" custom="keep" firstRelativePoint="100,55" lastRelativePoint="220,55">
            ${if (bend) "<bendpoint firstDimension=\"100,55\" secondDimension=\"-100,55\" weight=\"0.2\" custom=\"point\"/>" else ""}
        </wire>
        <wire id="7" firstRelativePoint="100,55" lastRelativePoint="220,175"/>
        </GlueDiagram>""".trimIndent()

    private fun stateMidpointFixture() = wireFixture()
        .replace("xsi:type=\"Activity\" id=\"a\" name=\"A\" location=\"20,40\"", "xsi:type=\"InitialState\" id=\"a\" name=\"A\" location=\"60,45\"")
        .replace("xsi:type=\"Activity\" id=\"b\" name=\"B\" location=\"220,40\"", "xsi:type=\"FinalState\" id=\"b\" name=\"B\" location=\"260,45\"")
    private fun withModelEditor(text: String, action: (ModelDiagramCanvas, com.intellij.openapi.fileEditor.TextEditor,
        com.intellij.openapi.editor.Document, com.intellij.openapi.vfs.VirtualFile) -> Unit) {
        val file = myFixture.addFileToProject("edit.glue_uml_ad", text).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val source = ModelSourceEditorProvider().createEditor(project, file) as com.intellij.openapi.fileEditor.TextEditor
        try {
            com.intellij.testFramework.EditorTestUtil.waitForLoading(source.editor)
            action(descendants(editor.component).filterIsInstance<ModelDiagramCanvas>().single(), source,
                FileDocumentManager.getInstance().getDocument(file)!!, file)
        } finally {
            if (!file.isWritable) WriteCommandAction.runWriteCommandAction(project) { file.isWritable = true }
            Disposer.dispose(editor)
            Disposer.dispose(source)
        }
    }

    fun testSelectedChildrenMoveTogetherAndOnlyActivitiesResizeTogether() {
        val canvas = ModelDiagramCanvas()
        val original = parse(sample())
        canvas.display(original)
        leftDown(canvas, 100, 135)
        leftDown(canvas, 200, 135, true)
        leftDown(canvas, 202, 34, true)
        drag(canvas, 100, 135, 20, 30, MouseEvent.BUTTON1)
        assertEquals(3, canvas.selectedNodes.size)
        for (id in listOf("9", "8", "0")) {
            val old = original.nodes.single { it.id == id }
            val moved = canvas.diagram.nodes.single { it.id == id }
            assertEquals(old.location + DiagramPoint(20.0, 30.0), moved.location)
        }
        assertEquals(original.nodes.single { it.name == "Router" }, canvas.diagram.nodes.single { it.name == "Router" })
        val stateBefore = canvas.diagram.nodes.single { it.id == "0" }
        drag(canvas, 189, 165, 25, 0, MouseEvent.BUTTON1)
        assertEquals(110.0, canvas.diagram.nodes.single { it.id == "9" }.width)
        assertEquals(110.0, canvas.diagram.nodes.single { it.id == "8" }.width)
        assertEquals(stateBefore, canvas.diagram.nodes.single { it.id == "0" })
        drag(canvas, 104, 165, 1000, 0, MouseEvent.BUTTON1)
        assertEquals(16.0, canvas.diagram.nodes.single { it.id == "9" }.width)
        assertEquals(16.0, canvas.diagram.nodes.single { it.id == "8" }.width)
        point(DiagramPoint(0.0, 0.0), canvas.pan)
    }

    fun testBatchMoveAndResizeAreSingleUndoCommands() = withModelEditor(wireFixture()) { canvas, source, document, file ->
        leftDown(canvas, 50, 55)
        leftDown(canvas, 250, 55, true)
        drag(canvas, 50, 55, 10, 20, MouseEvent.BUTTON1)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(2, canvas.selectedNodes.size)
        assertEquals(DiagramPoint(30.0, 60.0), canvas.diagram.nodes.single { it.id == "a" }.location)
        assertEquals(DiagramPoint(230.0, 60.0), canvas.diagram.nodes.single { it.id == "b" }.location)
        val afterMove = document.text
        val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(wireFixture(), document.text)
        undo.redo(source)
        UIUtil.dispatchAllInvocationEvents()
        drag(canvas, 130, 75, 30, 0, MouseEvent.BUTTON1)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(130.0, canvas.diagram.nodes.single { it.id == "a" }.width)
        assertEquals(130.0, canvas.diagram.nodes.single { it.id == "b" }.width)
        assertEquals(80.0, canvas.diagram.nodes.single { it.id == "c" }.width)
        val resized = document.text
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(afterMove, document.text)
        undo.redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(resized, document.text)
        FileDocumentManager.getInstance().saveDocument(document)
        assertEquals(resized, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
    }

    fun testBendpointMoveAndMidpointInsertionPreserveOtherPointsWeightAndUndo() = withModelEditor(wireFixture(true)) { canvas, source, document, file ->
        val initial = canvas.diagram.wires[0]
        val initialPath = canvas.wirePath(initial)
        val segment = DiagramPoint((initialPath[1].x + initialPath[2].x) / 2, (initialPath[1].y + initialPath[2].y) / 2)
        leftDown(canvas, segment.x.roundToInt(), segment.y.roundToInt())
        assertEquals(0, canvas.selectedWire!!.index)
        drag(canvas, 170, 110, 0, 20, MouseEvent.BUTTON1)
        UIUtil.dispatchAllInvocationEvents()
        point(DiagramPoint(170.0, 130.0), canvas.wirePath(canvas.selectedWire!!)[1])
        assertEquals(0.2, canvas.selectedWire!!.bendpoints.single().weight)
        assertTrue(document.text.contains("custom=\"point\""))
        val afterMove = document.text
        val path = canvas.wirePath(canvas.selectedWire!!)
        val mid = DiagramPoint((path[0].x + path[1].x) / 2, (path[0].y + path[1].y) / 2)
        hover(canvas, mid.x.roundToInt() + 2, mid.y.roundToInt() + 2)
        assertEquals(java.awt.Cursor.MOVE_CURSOR, canvas.cursor.type)
        hover(canvas, mid.x.roundToInt() + 3, mid.y.roundToInt())
        assertEquals(java.awt.Cursor.DEFAULT_CURSOR, canvas.cursor.type)
        drag(canvas, mid.x.roundToInt() + 2, mid.y.roundToInt() + 2,
            150 - mid.x.roundToInt() - 2, 90 - mid.y.roundToInt() - 2, MouseEvent.BUTTON1)
        UIUtil.dispatchAllInvocationEvents()
        val edited = canvas.selectedWire!!
        assertEquals(2, edited.bendpoints.size)
        point(DiagramPoint(150.0, 90.0), canvas.wirePath(edited)[1])
        point(DiagramPoint(170.0, 130.0), canvas.wirePath(edited)[2])
        assertEquals(listOf(0.5, 0.2), edited.bendpoints.map { it.weight })
        assertTrue(document.text.contains("custom=\"point\""))
        assertTrue(document.text.contains("custom=\"keep\""))
        val bendImage = Path.of("build/reports/model-wire-bendpoints.png")
        Files.createDirectories(bendImage.parent)
        ImageIO.write(render(canvas), "png", bendImage.toFile())
        val afterInsert = document.text
        val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(afterMove, document.text)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(wireFixture(true), document.text)
        undo.redo(source)
        undo.redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(afterInsert, document.text)
        FileDocumentManager.getInstance().saveDocument(document)
        assertEquals(afterInsert, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
        WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
        val bend = canvas.wirePath(canvas.selectedWire!!)[1]
        drag(canvas, bend.x.roundToInt(), bend.y.roundToInt(), 10, 10, MouseEvent.BUTTON1)
        assertEquals(afterInsert, document.text)
    }

    fun testEndpointReconnectPreviewsTargetUpdatesIndexRefsAndUndoes() = withModelEditor(wireFixture()) { canvas, source, document, file ->
        leftDown(canvas, 170, 55)
        val original = canvas.selectedWire!!
        val originalPath = canvas.wirePath(original)
        leftDown(canvas, 120, 55)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK, 250, 175, 0, false, MouseEvent.NOBUTTON))
        assertEquals("c", canvas.selectedWire!!.start!!.id)
        assertEquals("b", canvas.selectedWire!!.end!!.id)
        assertEquals(wireFixture(), document.text)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK, 270, 55, 0, false, MouseEvent.NOBUTTON))
        assertEquals("a", canvas.selectedWire!!.start!!.id)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 270, 55, 1, false, MouseEvent.BUTTON1))
        assertEquals(wireFixture(), document.text)
        assertEquals(originalPath, canvas.wirePath(canvas.selectedWire!!))
        drag(canvas, 120, 55, 130, 120, MouseEvent.BUTTON1)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("c", canvas.selectedWire!!.start!!.id)
        assertEquals("b", canvas.selectedWire!!.end!!.id)
        assertEquals(listOf(1), canvas.diagram.nodes.single { it.id == "a" }.outputs)
        assertEquals(listOf(0), canvas.diagram.nodes.single { it.id == "c" }.outputs)
        assertEquals(listOf(1), canvas.diagram.nodes.single { it.id == "c" }.inputs)
        assertEquals(listOf(0), canvas.diagram.nodes.single { it.id == "b" }.inputs)
        assertEquals(listOf("99", "7"), canvas.diagram.wires.map { it.id })
        val afterStart = document.text
        val end = canvas.wirePath(canvas.selectedWire!!).last()
        drag(canvas, end.x.roundToInt(), end.y.roundToInt(), 70 - end.x.roundToInt(), 55 - end.y.roundToInt(), MouseEvent.BUTTON1)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("c", canvas.selectedWire!!.start!!.id)
        assertEquals("a", canvas.selectedWire!!.end!!.id)
        assertEquals(listOf(0), canvas.diagram.nodes.single { it.id == "a" }.inputs)
        assertEmpty(canvas.diagram.nodes.single { it.id == "b" }.inputs)
        val afterEnd = document.text
        val endBeforeInvalid = canvas.wirePath(canvas.selectedWire!!).last()
        val pathBeforeInvalid = canvas.wirePath(canvas.selectedWire!!)
        drag(canvas, endBeforeInvalid.x.roundToInt(), endBeforeInvalid.y.roundToInt(),
            250 - endBeforeInvalid.x.roundToInt(), 175 - endBeforeInvalid.y.roundToInt(), MouseEvent.BUTTON1)
        assertEquals(afterEnd, document.text)
        assertEquals(pathBeforeInvalid, canvas.wirePath(canvas.selectedWire!!))
        val reconnectImage = Path.of("build/reports/model-wire-reconnected.png")
        Files.createDirectories(reconnectImage.parent)
        ImageIO.write(render(canvas), "png", reconnectImage.toFile())
        val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(afterStart, document.text)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(wireFixture(), document.text)
        undo.redo(source)
        undo.redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(afterEnd, document.text)
        FileDocumentManager.getInstance().saveDocument(document)
        assertEquals(afterEnd, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
        val point = canvas.wirePath(canvas.selectedWire!!).first()
        WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
        drag(canvas, point.x.roundToInt(), point.y.roundToInt(), 200, 200, MouseEvent.BUTTON1)
        assertEquals(afterEnd, document.text)
    }

    fun testSourceAndReadOnlyChangesDuringWireDragCancelWithoutOverwriting() = withModelEditor(wireFixture(true)) { canvas, _, document, file ->
        leftDown(canvas, 200, 85)
        leftDown(canvas, 170, 110)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK, 170, 130, 0, false, MouseEvent.NOBUTTON))
        val latest = document.text.replace("custom=\"keep\"", "custom=\"latest\"")
        WriteCommandAction.runWriteCommandAction(project) { document.setText(latest) }
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 170, 130, 1, false, MouseEvent.BUTTON1))
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(latest, document.text)
        point(DiagramPoint(170.0, 110.0), canvas.wirePath(canvas.selectedWire!!)[1])
        val start = canvas.wirePath(canvas.selectedWire!!).first()
        leftDown(canvas, start.x.roundToInt(), start.y.roundToInt())
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK, 250, 175, 0, false, MouseEvent.NOBUTTON))
        assertEquals("c", canvas.selectedWire!!.start!!.id)
        WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 250, 175, 1, false, MouseEvent.BUTTON1))
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(latest, document.text)
        assertEquals("a", canvas.selectedWire!!.start!!.id)
    }

    fun testFinalStateRingKeepsBackgroundAndCanvasDoesNotShrink() {
        val canvas = ModelDiagramCanvas()
        canvas.background = Color(43, 43, 43)
        val node = state(100.0, 100.0).copy(kind = DiagramNodeKind.FinalState)
        canvas.display(ModelDiagram(listOf(node), emptyList()))
        val image = render(canvas)
        assertEquals(canvas.background.rgb, image.getRGB(102, 110))
        assertEquals(Color.BLACK.rgb, image.getRGB(110, 110))
        val distant = activity(900.0, 600.0).copy(id = "distant")
        canvas.display(ModelDiagram(listOf(node, distant), emptyList()))
        val largeSize = canvas.preferredSize
        canvas.display(ModelDiagram(listOf(node, distant.copy(location = DiagramPoint(200.0, 200.0))), emptyList()))
        assertEquals(largeSize, canvas.preferredSize)
    }

    fun testEditorNegativeMoveKeepsViewportAndUnrelatedStateThroughUndo() {
        val file = myFixture.addFileToProject("negative.glue_uml_ad", sample()).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val source = ModelSourceEditorProvider().createEditor(project, file) as com.intellij.openapi.fileEditor.TextEditor
        try {
            com.intellij.testFramework.EditorTestUtil.waitForLoading(source.editor)
            val canvas = descendants(editor.component).filterIsInstance<ModelDiagramCanvas>().single()
            val scroll = descendants(editor.component).filterIsInstance<javax.swing.JScrollPane>().single()
            scroll.setSize(300, 200)
            scroll.doLayout()
            canvas.setSize(canvas.preferredSize)
            scroll.viewport.viewPosition = java.awt.Point(60, 30)
            val viewportPosition = scroll.viewport.viewPosition
            drag(canvas, 45, 75, -200, -100, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(viewportPosition, scroll.viewport.viewPosition)
            point(DiagramPoint(0.0, 0.0), canvas.pan)
            assertEquals(Color.BLACK.rgb, render(canvas).getRGB(202, 34))
            com.intellij.openapi.command.undo.UndoManager.getInstance(project).undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(viewportPosition, scroll.viewport.viewPosition)
            assertEquals(Color.BLACK.rgb, render(canvas).getRGB(202, 34))
        } finally {
            Disposer.dispose(editor)
            Disposer.dispose(source)
        }
    }

    fun testVirtualStateEndpointUsesRadialPerimeterInEitherDirection() {
        val a = state(20.0, 20.0)
        val b = state(100.0, 100.0)
        val source = wire(a, b)
        val mouse = DiagramPoint(200.0, 20.0)
        for (start in listOf(true, false)) {
            val path = DiagramGeometry.previewStateEndpoint(source, start, mouse)
            val centers = if (start) listOf(mouse, b.center) else listOf(a.center, mouse)
            path.zip(centers).forEachIndexed { index, (contact, center) ->
                assertStateContact(center, centers[1 - index], contact)
            }
        }
    }

    fun testVirtualStateEndpointCorrectsActivityEdgesAndStatePerimeterSymmetrically() {
        val activity = activity(0.0, 100.0)
        val state = state(300.0, 300.0)
        val mouse = DiagramPoint(50.0, 20.0)
        val expected = listOf(DiagramPoint(50.0, 100.0), DiagramPoint(50.0, 30.0))
        assertEquals(expected, DiagramGeometry.previewStateEndpoint(wire(activity, state), false, mouse))
        assertEquals(expected.reversed(), DiagramGeometry.previewStateEndpoint(wire(state, activity), true, mouse))
        val below = DiagramPoint(50.0, 200.0)
        assertEquals(listOf(DiagramPoint(50.0, 130.0), DiagramPoint(50.0, 190.0)),
            DiagramGeometry.previewStateEndpoint(wire(activity, state), false, below))
        for (x in listOf(-200.0, 200.0)) for (y in listOf(0.0, 250.0)) {
            val center = DiagramPoint(x, y)
            val path = DiagramGeometry.previewStateEndpoint(wire(activity, state), false, center)
            val corner = DiagramPoint(if (x < 0) 8.0 else 92.0, if (y < 100) 108.0 else 122.0)
            assertEquals(9.0, hypot(path.first().x - corner.x, path.first().y - corner.y), 0.000001)
            assertEquals(10.0, hypot(path.last().x - center.x, path.last().y - center.y), 0.000001)
            val reverse = DiagramGeometry.previewStateEndpoint(wire(state, activity), true, center)
            point(path.first(), reverse.last())
            point(path.last(), reverse.first())
        }
    }

    fun testVirtualStateEndpointMovesBendsProportionallyAndPreservesMetadata() {
        val a = activity(0.0, 100.0)
        val b = activity(200.0, 200.0)
        val source = wire(a, b, listOf(
            DiagramBendpoint(DiagramPoint(90.0, 70.0), DiagramPoint(-40.0, 50.0), 0.1),
            DiagramBendpoint(DiagramPoint(120.0, 90.0), DiagramPoint(-20.0, 70.0), 0.9)))
        val bends = DiagramGeometry.path(source).drop(1).dropLast(1)
        val mouse = DiagramPoint(350.0, 50.0)
        for (start in listOf(true, false)) {
            val preview = DiagramGeometry.previewStateEndpoint(source, start, mouse)
            assertEquals(4, preview.size)
            bends.zip(preview.drop(1).dropLast(1)).forEachIndexed { index, (old, actual) ->
                val t = (index + 1).toDouble() / (bends.size + 1)
                val center = if (start) a.center else b.center
                val fraction = if (start) 1 - t else t
                point(DiagramPoint(old.x + (mouse.x - center.x) * fraction,
                    old.y + (mouse.y - center.y) * fraction), actual)
            }
            val endpoint = if (start) preview.first() else preview.last()
            assertEquals(10.0, hypot(endpoint.x - mouse.x, endpoint.y - mouse.y), 0.000001)
        }
        assertEquals(listOf(0.1, 0.9), source.bendpoints.map { it.weight })
    }

    fun testOneToFourBendsFollowBothCentersByTheirOwnInterpolationRatio() {
        val a = activity(0.0, 100.0)
        val b = activity(200.0, 200.0)
        val startDelta = DiagramPoint(20.0, 40.0)
        val endDelta = DiagramPoint(-30.0, 60.0)
        for (count in 1..4) {
            val bends = (0 until count).map { i ->
                DiagramBendpoint(DiagramPoint(40.0 + i * 10, 70.0 + i * 20),
                    DiagramPoint(-50.0 + i * 15, 30.0 + i * 10), i.toDouble() / count)
            }
            val source = wire(a, b, bends)
            val moved = source.copy(start = a.copy(location = a.location + startDelta),
                end = b.copy(location = b.location + endDelta))
            val points = DiagramGeometry.path(moved).drop(1).dropLast(1)
            val original = DiagramGeometry.path(source).drop(1).dropLast(1)
            points.forEachIndexed { i, actual ->
                val t = (i + 1).toDouble() / (count + 1)
                val first = a.center + startDelta + bends[i].first
                val second = b.center + endDelta + bends[i].second
                point(DiagramPoint(first.x + (second.x - first.x) * t, first.y + (second.y - first.y) * t), actual)
                point(DiagramPoint(original[i].x + startDelta.x * (1 - t) + endDelta.x * t,
                    original[i].y + startDelta.y * (1 - t) + endDelta.y * t), actual)
            }
            assertSame(bends, moved.bendpoints)
        }
    }

    fun testEndpointDragTreatsMouseAsStateAfterNavigationAndRollsBack() {
        val canvas = ModelDiagramCanvas()
        canvas.display(parse(wireFixture()))
        leftDown(canvas, 170, 55)
        val originalWire = canvas.selectedWire!!
        val originalPath = canvas.wirePath(originalWire)
        drag(canvas, 350, 250, 40, 50)
        leftDown(canvas, 260, 105)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK, 110, 180, 0, false, MouseEvent.NOBUTTON))
        val preview = canvas.wirePath(canvas.selectedWire!!)
        point(DiagramPoint(70.0, 70.0), preview.first())
        point(DiagramPoint(70.0, 120.0), preview.last())
        assertEquals("a", canvas.selectedWire!!.start!!.id)
        assertEquals("b", canvas.selectedWire!!.end!!.id)
        assertEquals(wireFixture(), myFixture.file.text)
        point(DiagramPoint(40.0, 50.0), canvas.pan)
        val output = Path.of("build/reports/model-wire-virtual-state.png")
        Files.createDirectories(output.parent)
        ImageIO.write(render(canvas), "png", output.toFile())
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 110, 180, 1, false, MouseEvent.BUTTON1))
        assertEquals(originalPath, canvas.wirePath(canvas.selectedWire!!))
    }

    fun testMidpointSplitThresholdUsesDistanceToOriginalLineInBothDirections() {
        val horizontal = wire(state(10.0, 10.0), state(110.0, 10.0))
        for (offset in listOf(-5.0, -4.0, 0.0, 4.0, 5.0))
            assertSame(horizontal, DiagramEdits.dragMidpoint(horizontal, 0, DiagramPoint(70.0, 20 + offset)))
        for (offset in listOf(-6.0, 6.0)) {
            val edited = DiagramEdits.dragMidpoint(horizontal, 0, DiagramPoint(70.0, 20 + offset))
            assertEquals(1, edited.bendpoints.size)
            point(DiagramPoint(70.0, 20 + offset), DiagramGeometry.path(edited)[1])
        }
        // Past the endpoint, A-B crosses the target State even though B lies on the original line.
        assertEquals(1, DiagramEdits.dragMidpoint(horizontal, 0, DiagramPoint(500.0, 20.0)).bendpoints.size)
        val vertical = wire(state(10.0, 10.0), state(10.0, 110.0))
        assertSame(vertical, DiagramEdits.dragMidpoint(vertical, 0, DiagramPoint(25.0, 70.0)))
        assertSame(vertical, DiagramEdits.dragMidpoint(vertical, 0, DiagramPoint(15.0, 70.0)))
        assertEquals(1, DiagramEdits.dragMidpoint(vertical, 0, DiagramPoint(26.0, 70.0)).bendpoints.size)
        val diagonal = wire(state(0.0, 0.0), state(30.0, 40.0))
        for (sign in listOf(-1, 1)) {
            assertSame(diagonal, DiagramEdits.dragMidpoint(diagonal, 0, DiagramPoint(25.0 - 4 * sign, 30.0 + 3 * sign)))
            assertEquals(1, DiagramEdits.dragMidpoint(diagonal, 0,
                DiagramPoint(25.0 - 4.8 * sign, 30.0 + 3.6 * sign)).bendpoints.size)
        }
        val zero = wire(state(10.0, 10.0), state(10.0, 10.0))
        assertEquals(listOf(zero.start!!.center, zero.end!!.center), DiagramGeometry.path(zero))
        assertEquals(1, DiagramEdits.dragMidpoint(zero, 0, DiagramPoint(36.0, 20.0)).bendpoints.size)
    }

    fun testMidpointSplitMergeRepeatsDuringDragAndInsideReleaseDoesNotEdit() = withModelEditor(stateMidpointFixture()) { canvas, _, document, _ ->
        leftDown(canvas, 170, 55)
        drag(canvas, 350, 250, 20, 30)
        val original = canvas.selectedWire!!
        val stamp = document.modificationStamp
        leftDown(canvas, 190, 85)
        fun move(dy: Int) {
            canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK,
                190, 85 + dy, 0, false, MouseEvent.NOBUTTON))
        }
        move(5)
        assertEquals(original, canvas.selectedWire)
        move(6)
        assertEquals(1, canvas.selectedWire!!.bendpoints.size)
        move(5)
        assertEquals(original, canvas.selectedWire)
        move(-6)
        assertEquals(1, canvas.selectedWire!!.bendpoints.size)
        move(-5)
        assertEquals(original, canvas.selectedWire)
        assertEquals(stamp, document.modificationStamp)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 190, 80, 1, false, MouseEvent.BUTTON1))
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(stateMidpointFixture(), document.text)
        assertEquals(stamp, document.modificationStamp)
        point(DiagramPoint(20.0, 30.0), canvas.pan)
    }

    fun testMidpointOutsideReleaseInsertsSingleBendAndSupportsUndo() = withModelEditor(stateMidpointFixture()) { canvas, source, document, _ ->
        leftDown(canvas, 170, 55)
        leftDown(canvas, 170, 55)
        for (dy in listOf(6, 4, -6, -4, 10)) {
            canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK,
                170, 55 + dy, 0, false, MouseEvent.NOBUTTON))
        }
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 170, 65, 1, false, MouseEvent.BUTTON1))
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(1, canvas.selectedWire!!.bendpoints.size)
        point(DiagramPoint(170.0, 65.0), canvas.wirePath(canvas.selectedWire!!)[1])
        val edited = document.text
        assertTrue(edited != stateMidpointFixture())
        val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(stateMidpointFixture(), document.text)
        undo.redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(edited, document.text)
    }

    fun testExistingBendpointMergesAtFivePixelsAndKeepsOtherPoints() {
        val a = state(10.0, 10.0)
        val b = state(110.0, 10.0)
        fun bend(point: DiagramPoint, weight: Double) = DiagramBendpoint(
            DiagramPoint(point.x - a.center.x, point.y - a.center.y),
            DiagramPoint(point.x - b.center.x, point.y - b.center.y), weight)
        val single = wire(a, b, listOf(bend(DiagramPoint(70.0, 60.0), 0.7)))
        for (offset in listOf(-5.0, 0.0, 5.0))
            assertEmpty(DiagramEdits.dragBendpoint(single, 0, DiagramPoint(70.0, 20 + offset)).bendpoints)
        for (offset in listOf(-6.0, 6.0)) {
            val moved = DiagramEdits.dragBendpoint(single, 0, DiagramPoint(70.0, 20 + offset))
            assertEquals(1, moved.bendpoints.size)
            point(DiagramPoint(70.0, 20 + offset), DiagramGeometry.path(moved)[1])
            assertEquals(0.7, moved.bendpoints.single().weight)
        }
        val positions = listOf(DiagramPoint(40.0, 50.0), DiagramPoint(70.0, -20.0), DiagramPoint(100.0, 60.0))
        val original = wire(a, b, positions.mapIndexed { i, at -> bend(at, (i + 1) / 10.0) })
        for (index in positions.indices) {
            val mergedPath = listOf(a.center) + positions.filterIndexed { i, _ -> i != index } + b.center
            val p = mergedPath[index]
            val q = mergedPath[index + 1]
            val merged = DiagramEdits.dragBendpoint(original, index, DiagramPoint((p.x + q.x) / 2, (p.y + q.y) / 2))
            assertEquals(2, merged.bendpoints.size)
            positions.filterIndexed { i, _ -> i != index }.zip(DiagramGeometry.path(merged).drop(1).dropLast(1))
                .forEach { (expected, actual) -> point(expected, actual) }
            assertEquals(original.bendpoints.filterIndexed { i, _ -> i != index }.map { it.weight }, merged.bendpoints.map { it.weight })
        }
        val diagonal = wire(state(0.0, 0.0), state(30.0, 40.0), single.bendpoints)
        for (sign in listOf(-1, 1)) {
            assertEmpty(DiagramEdits.dragBendpoint(diagonal, 0, DiagramPoint(25.0 - 4 * sign, 30.0 + 3 * sign)).bendpoints)
            assertEquals(1, DiagramEdits.dragBendpoint(diagonal, 0, DiagramPoint(25.0 - 4.8 * sign, 30.0 + 3.6 * sign)).bendpoints.size)
        }
    }

    fun testExistingBendpointMergeRestoreDuringDragAndSinglePointDeletionUndo() = withModelEditor(wireFixture(true)) { canvas, source, document, file ->
        leftDown(canvas, 200, 85)
        drag(canvas, 350, 250, 20, 30)
        val stamp = document.modificationStamp
        leftDown(canvas, 190, 140)
        fun move(y: Int) {
            canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK,
                190, y + 30, 0, false, MouseEvent.NOBUTTON))
        }
        move(60)
        assertEmpty(canvas.selectedWire!!.bendpoints)
        move(61)
        assertEquals(1, canvas.selectedWire!!.bendpoints.size)
        move(50)
        assertEmpty(canvas.selectedWire!!.bendpoints)
        move(49)
        assertEquals(1, canvas.selectedWire!!.bendpoints.size)
        move(50)
        assertEmpty(canvas.selectedWire!!.bendpoints)
        assertEquals(stamp, document.modificationStamp)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 190, 80, 1, false, MouseEvent.BUTTON1))
        UIUtil.dispatchAllInvocationEvents()
        assertEmpty(canvas.selectedWire!!.bendpoints)
        assertFalse(document.text.contains("<bendpoint"))
        assertTrue(document.text.contains("custom=\"keep\""))
        val deleted = document.text
        val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(wireFixture(true), document.text)
        assertEquals(0.2, canvas.selectedWire!!.bendpoints.single().weight)
        undo.redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(deleted, document.text)
        FileDocumentManager.getInstance().saveDocument(document)
        assertEquals(deleted, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
        point(DiagramPoint(20.0, 30.0), canvas.pan)
    }

    fun testMiddleBendpointDeletionKeepsRemainingXmlTagsWeightsAndPositions() {
        val one = "<bendpoint firstDimension=\"100,55\" secondDimension=\"-100,55\" weight=\"0.2\" custom=\"point\"/>"
        val three = """<bendpoint firstDimension="80,45" secondDimension="-120,45" weight="0.1" custom="left"/>
            <bendpoint firstDimension="100,85" secondDimension="-100,85" weight="0.2" custom="middle"/>
            <bendpoint firstDimension="120,45" secondDimension="-80,45" weight="0.3" custom="right"/>"""
        val original = wireFixture(true).replace(one, three)
        withModelEditor(original) { canvas, source, document, file ->
            leftDown(canvas, 170, 140)
            assertEquals(0, canvas.selectedWire!!.index)
            drag(canvas, 170, 140, 0, -35, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(2, canvas.selectedWire!!.bendpoints.size)
            val points = canvas.wirePath(canvas.selectedWire!!)
            point(DiagramPoint(150.0, 100.0), points[1])
            point(DiagramPoint(190.0, 100.0), points[2])
            assertEquals(listOf(0.1, 0.3), canvas.selectedWire!!.bendpoints.map { it.weight })
            assertTrue(document.text.contains("custom=\"left\""))
            assertTrue(document.text.contains("custom=\"right\""))
            assertFalse(document.text.contains("custom=\"middle\""))
            val edited = document.text
            val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
            undo.undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(original, document.text)
            undo.redo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(edited, document.text)
            WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
            drag(canvas, 150, 100, 10, -30, MouseEvent.BUTTON1)
            assertEquals(edited, document.text)
        }
    }

    fun testHorizontalActivityBothEndpointDragsUpdateSharedCoordinateAndPersist() = withModelEditor(wireFixture()) { canvas, source, document, file ->
        leftDown(canvas, 170, 55)
        drag(canvas, 120, 55, -70, -5, MouseEvent.BUTTON1)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(listOf(DiagramPoint(120.0, 50.0), DiagramPoint(220.0, 50.0)), canvas.wirePath(canvas.selectedWire!!))
        val afterStart = document.text
        leftDown(canvas, 220, 50)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_DRAGGED, 1, MouseEvent.BUTTON1_DOWN_MASK, 250, 60, 0, false, MouseEvent.NOBUTTON))
        assertEquals(listOf(DiagramPoint(120.0, 60.0), DiagramPoint(220.0, 60.0)), canvas.wirePath(canvas.selectedWire!!))
        assertEquals(afterStart, document.text)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2, 0, 250, 60, 1, false, MouseEvent.BUTTON1))
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(DiagramPoint(250.0, 60.0), canvas.selectedWire!!.firstRelativePoint)
        assertEquals(DiagramPoint(250.0, 60.0), canvas.selectedWire!!.lastRelativePoint)
        assertEquals(listOf(DiagramPoint(120.0, 60.0), DiagramPoint(220.0, 60.0)), canvas.wirePath(canvas.selectedWire!!))
        assertEquals(listOf(0, 1), canvas.diagram.nodes.single { it.id == "a" }.outputs)
        assertEquals(listOf(0), canvas.diagram.nodes.single { it.id == "b" }.inputs)
        val afterEnd = document.text
        val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(afterStart, document.text)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(wireFixture(), document.text)
        undo.redo(source)
        undo.redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(afterEnd, document.text)
        FileDocumentManager.getInstance().saveDocument(document)
        assertEquals(afterEnd, String(file.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
        drag(canvas, 350, 250, 40, 50)
        drag(canvas, 260, 110, 30, -19, MouseEvent.BUTTON1)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(listOf(DiagramPoint(120.0, 48.0), DiagramPoint(220.0, 48.0)), canvas.wirePath(canvas.selectedWire!!))
        point(DiagramPoint(40.0, 50.0), canvas.pan)
    }

    fun testVerticalActivityEndDragFollowsMouseAndClampsBothSharedAnchors() {
        val text = wireFixture().replace("id=\"b\" name=\"B\" location=\"220,40\"", "id=\"b\" name=\"B\" location=\"20,160\"")
        withModelEditor(text) { canvas, source, document, _ ->
            leftDown(canvas, 100, 120)
            assertEquals(0, canvas.selectedWire!!.index)
            drag(canvas, 100, 70, -50, -15, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(listOf(DiagramPoint(50.0, 70.0), DiagramPoint(50.0, 160.0)), canvas.wirePath(canvas.selectedWire!!))
            drag(canvas, 50, 160, 65, 15, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(listOf(DiagramPoint(112.0, 70.0), DiagramPoint(112.0, 160.0)), canvas.wirePath(canvas.selectedWire!!))
            val atRight = document.text
            drag(canvas, 112, 160, -87, 15, MouseEvent.BUTTON1)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(listOf(DiagramPoint(28.0, 70.0), DiagramPoint(28.0, 160.0)), canvas.wirePath(canvas.selectedWire!!))
            assertEquals(DiagramPoint(25.0, 175.0), canvas.selectedWire!!.firstRelativePoint)
            val undo = com.intellij.openapi.command.undo.UndoManager.getInstance(project)
            undo.undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(atRight, document.text)
            assertEquals(listOf(DiagramPoint(112.0, 70.0), DiagramPoint(112.0, 160.0)), canvas.wirePath(canvas.selectedWire!!))
        }
    }

    fun testDesignTracksUnsavedSourceWithoutRewritingAndCapturesSample() {
        val original = sample()
        val file = myFixture.addFileToProject("sample.glue_uml_ad", original).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = descendants(editor.component).filterIsInstance<ModelDiagramCanvas>().single()
            assertEquals(8, canvas.diagram.nodes.size)
            canvas.setSize(canvas.preferredSize)
            val image = BufferedImage(canvas.width, canvas.height, BufferedImage.TYPE_INT_ARGB)
            image.createGraphics().let { graphics -> try { canvas.paint(graphics) } finally { graphics.dispose() } }
            val output = Path.of("build/reports/model-diagram.png")
            Files.createDirectories(output.parent)
            ImageIO.write(image, "png", output.toFile())
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            assertEquals(original, document.text)
            val changed = original.replace("name=\"Router\"", "name=\"Changed\"").replace("weight=\"0.5\"", "weight=\"0.99\"")
            WriteCommandAction.runWriteCommandAction(project) { document.setText(changed) }
            UIUtil.dispatchAllInvocationEvents()
            editor.selectNotify()
            assertTrue(canvas.diagram.nodes.any { it.name == "Changed" })
            assertEquals(0.99, canvas.diagram.wires[5].bendpoints.single().weight)
            assertEquals(changed, document.text)
            assertTrue(editor.isModified)
            WriteCommandAction.runWriteCommandAction(project) { document.setText("<GlueDiagram>") }
            UIUtil.dispatchAllInvocationEvents()
            assertNotNull(canvas.diagram.message)
        } finally { Disposer.dispose(editor) }
    }
}
