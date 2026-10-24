package com.look4counter

import com.look4counter.modeleditor.ServiceDiagramLayout
import com.look4counter.modeleditor.ServiceDiagramPlacement
import com.look4counter.modeleditor.DiagramPoint
import com.look4counter.modeleditor.DiagramNode
import com.look4counter.modeleditor.DiagramNodeKind
import com.look4counter.modeleditor.DiagramWire
import com.look4counter.modeleditor.DiagramBendpoint
import com.look4counter.modeleditor.DiagramGeometry
import com.look4counter.modeleditor.DiagramEdits
import junit.framework.TestCase

internal fun sharedWireSegments(paths: List<List<DiagramPoint>>): Set<Pair<Int, Int>> {
    val overlaps = mutableSetOf<Pair<Int, Int>>()
    for (i in paths.indices) for (j in i + 1 until paths.size) {
        for ((a, b) in paths[i].zipWithNext()) for ((c, d) in paths[j].zipWithNext()) {
            val vertical = a.x == b.x && c.x == d.x && a.x == c.x &&
                minOf(maxOf(a.y, b.y), maxOf(c.y, d.y)) > maxOf(minOf(a.y, b.y), minOf(c.y, d.y))
            val horizontal = a.y == b.y && c.y == d.y && a.y == c.y &&
                minOf(maxOf(a.x, b.x), maxOf(c.x, d.x)) > maxOf(minOf(a.x, b.x), minOf(c.x, d.x))
            if (vertical || horizontal) overlaps += i to j
        }
    }
    return overlaps
}

class ServiceDiagramLayoutTest : TestCase() {
    private fun checkLayout(layout: ServiceDiagramPlacement) {
        val boxes = layout.activities.map { (name, at) -> Triple(at, layout.widths.getValue(name), 30.0) } +
            listOf(Triple(layout.initial, 20.0, 20.0), Triple(layout.final, 20.0, 20.0))
        boxes.forEachIndexed { i, (a, aw, ah) ->
            assertTrue(a.x.isFinite() && a.y.isFinite())
            boxes.drop(i + 1).forEach { (b, bw, bh) ->
                assertTrue("겹치는 도형: $a, $b", a.x + aw <= b.x || b.x + bw <= a.x || a.y + ah <= b.y || b.y + bh <= a.y)
            }
        }
        layout.routes.forEach { route ->
            assertTrue(route.points.all { it.x.isFinite() && it.y.isFinite() && it.x % 1 == 0.0 && it.y % 1 == 0.0 })
            route.points.zipWithNext().forEach { (a, b) -> assertTrue("직각이 아닌 구간: $a → $b", a.x == b.x || a.y == b.y) }
        }
        assertEquals("겹치는 wire 선분", emptySet<Pair<Int, Int>>(), sharedWireSegments(layout.routes.map { it.points }))
    }

    fun testBranchesCyclesAndDisconnectedNodesHaveDeterministicFiniteLayout() {
        val names = listOf("Router", "Left", "Right", "Join", "Detached")
        val edges = mapOf("Router" to listOf("Left", "Right"), "Left" to listOf("Join"), "Right" to listOf("Join"), "Join" to listOf("Router"))
        val layout = ServiceDiagramLayout.arrange(names, "Router", edges)
        assertEquals(layout, ServiceDiagramLayout.arrange(names, "Router", edges))
        assertEquals(names.toSet(), layout.activities.keys)
        assertTrue(layout.initial.y < layout.activities.getValue("Router").y)
        assertEquals(6, layout.routes.size)
        checkLayout(layout)
    }

    fun testEmptyAndSelfLoopLayoutsTerminate() {
        val empty = ServiceDiagramLayout.arrange(emptyList(), "", emptyMap())
        assertTrue(empty.activities.isEmpty())
        assertTrue(empty.routes.isEmpty())
        checkLayout(empty)
        val layout = ServiceDiagramLayout.arrange(listOf("Loop"), "Loop", mapOf("Loop" to listOf("Loop")))
        assertEquals(1, layout.activities.size)
        assertEquals(layout, ServiceDiagramLayout.arrange(listOf("Loop"), "Loop", mapOf("Loop" to listOf("Loop"))))
        assertEquals(2, layout.routes.size)
        assertTrue(layout.routes.last().bends.size >= 2)
        checkLayout(layout)
    }

    fun testParallelAndEndRoutesKeepInputOrderAndSeparatePorts() {
        val names = listOf("A", "B")
        val edges = mapOf("A" to listOf("B", "B", "end"), "B" to listOf("A", "end"))
        val layout = ServiceDiagramLayout.arrange(names, "A", edges)
        assertEquals(6, layout.routes.size)
        assertEquals(3, layout.routes.slice(1..3).map { it.start }.distinct().size)
        assertFalse(layout.routes[0].end == layout.routes[4].end)
        assertFalse(layout.routes[1].end == layout.routes[2].end)
        assertFalse(layout.routes[3].end == layout.routes[5].end)
        for (index in listOf(3, 5)) {
            val at = layout.routes[index].end
            assertEquals(10.0, kotlin.math.hypot(at.x - layout.final.x - 10, at.y - layout.final.y - 10), 0.75)
        }
        assertEquals(layout, ServiceDiagramLayout.arrange(names, "A", edges))
        checkLayout(layout)
    }

    fun testManyBranchesAndMergesGrowPortSpaceWithoutSharedSegments() {
        val branches = (1..8).map { "Branch$it" }
        val names = listOf("Router") + branches + "Join"
        val edges = linkedMapOf("Router" to branches)
        branches.forEach { edges[it] = listOf("Join") }
        edges["Join"] = listOf("end")
        val layout = ServiceDiagramLayout.arrange(names, "Router", edges)
        assertTrue(layout.widths.getValue("Router") >= 162.0)
        assertTrue(layout.widths.getValue("Join") >= 162.0)
        assertEquals(18, layout.routes.size)
        assertEquals(8, layout.routes.slice(1..8).map { it.start }.distinct().size)
        checkLayout(layout)
    }

    fun testExplicitContactsPreserveOffsetRoutesAndTransitionOnNodeEdits() {
        val a = DiagramNode(DiagramNodeKind.Activity, "a", "A", DiagramPoint(0.0, 0.0), 100.0, 30.0)
        val b = a.copy(id = "b", name = "B", location = DiagramPoint(0.0, 160.0))
        val points = listOf(DiagramPoint(25.0, 30.0), DiagramPoint(25.0, 80.0), DiagramPoint(75.0, 80.0), DiagramPoint(75.0, 160.0))
        val bends = points.drop(1).dropLast(1).map { at -> DiagramBendpoint(
            DiagramPoint(at.x - a.center.x, at.y - a.center.y), DiagramPoint(at.x - b.center.x, at.y - b.center.y), null) }
        val wire = DiagramWire(0, "wire", "success", a, b, points.first(), points.last(), bends, true)
        assertEquals(points, DiagramGeometry.path(wire))
        val delta = DiagramPoint(33.0, -21.0)
        val moved = DiagramEdits.relocateNodes(wire, mapOf(a to a.copy(location = a.location + delta), b to b.copy(location = b.location + delta)))
        assertTrue(moved.explicitContacts)
        assertEquals(points.map { it + delta }, DiagramGeometry.path(moved))
        assertFalse(DiagramEdits.relocateNodes(wire, mapOf(a to a.copy(location = a.location + delta))).explicitContacts)
        assertFalse(DiagramEdits.relocateNodes(wire, mapOf(a to a.copy(width = 160.0))).explicitContacts)
    }

    fun testDagUsesDownwardLayers() {
        val layout = ServiceDiagramLayout.arrange(listOf("Router", "Left", "Right", "Join"), "Router",
            mapOf("Router" to listOf("Left", "Right"), "Left" to listOf("Join"), "Right" to listOf("Join"), "Join" to listOf("end")))
        assertTrue(layout.initial.y < layout.activities.getValue("Router").y)
        for (branch in listOf("Left", "Right")) {
            assertTrue(layout.activities.getValue("Router").y < layout.activities.getValue(branch).y)
            assertTrue(layout.activities.getValue(branch).y < layout.activities.getValue("Join").y)
        }
        assertTrue(layout.activities.getValue("Join").y < layout.final.y)
        checkLayout(layout)
    }
}
