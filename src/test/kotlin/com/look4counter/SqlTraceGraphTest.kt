package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.sql.SqlTraceEntry
import com.look4counter.queryeditor.sql.SqlTraceGraph
import com.look4counter.queryeditor.sql.SqlTraceGraphBuilder

class SqlTraceGraphTest : BasePlatformTestCase() {
    private class Vertex(val name: String, val offset: Int? = null) {
        val inputs = mutableListOf<Vertex>()
    }

    private fun graph(nodes: List<Vertex>, selected: Vertex? = null): SqlTraceGraph =
        SqlTraceGraphBuilder.build(nodes, { it.inputs }, { vertex -> vertex.offset?.let { SqlTraceEntry(1, "SQL", it) } },
            { it.name }, selected)

    fun testBranchesAndRejoiningKeepOnlyTheirActualEdges() {
        val a = Vertex("a", 0)
        val b = Vertex("b", 0)
        val merge = Vertex("merge", 10).apply { inputs.addAll(listOf(a, b)) }
        val end = Vertex("end", 20).apply { inputs.add(merge) }
        val result = graph(listOf(a, b, merge, end), merge)
        fun children(name: String) = result.nodes.single { it.name == name }.children.map { result.nodes[it].name }
        assertEquals(listOf("a", "b"), result.roots.map { result.nodes[it].name })
        assertEquals(listOf("merge"), children("a"))
        assertEquals(listOf("merge"), children("b"))
        assertEquals(listOf("end"), children("merge"))
        assertEquals("merge", result.nodes[result.selectedNodeId!!].name)
        assertEquals(4, result.nodes.size)
    }

    fun testTransparentNodesPreserveConnectivityEvenWithInternalCycle() {
        val origin = Vertex("origin", 0)
        val hidden = Vertex("hidden").apply { inputs.add(origin) }
        val otherHidden = Vertex("other hidden").apply { inputs.add(hidden) }
        hidden.inputs.add(otherHidden)
        val end = Vertex("end", 20).apply { inputs.add(otherHidden) }
        val result = graph(listOf(origin, hidden, otherHidden, end))
        assertEquals(2, result.nodes.size)
        assertEquals(listOf(result.nodes.single { it.name == "end" }.id), result.nodes.single { it.name == "origin" }.children)
    }

    fun testVisibleCycleHasARootAndRetainsItsExactEdges() {
        val a = Vertex("a", 0)
        val b = Vertex("b", 10).apply { inputs.add(a) }
        a.inputs.add(b)
        val result = graph(listOf(a, b))
        assertEquals(listOf(0), result.roots)
        assertEquals(listOf(1), result.nodes[0].children)
        assertEquals(listOf(0), result.nodes[1].children)
    }

    fun testOversizedGraphStopsAtAnalysisLimit() {
        try {
            graph((0..4096).map { Vertex("$it", it) })
            fail("분석 한도를 적용해야 합니다.")
        } catch (error: RuntimeException) {
            assertEquals("SqlAnalysisLimit", error.javaClass.simpleName)
        }
    }
}
