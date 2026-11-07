package com.look4counter.queryeditor.sql

import com.intellij.openapi.progress.ProgressManager
import java.util.Collections
import java.util.IdentityHashMap

data class SqlTraceNode(val id: Int, val entry: SqlTraceEntry, val name: String, val children: List<Int>)

data class SqlTraceGraph(
    val nodes: List<SqlTraceNode> = emptyList(),
    val roots: List<Int> = emptyList(),
    val selectedNodeId: Int? = null,
)

object SqlTraceGraphBuilder {
    fun <T : Any> build(nodes: Collection<T>, inputs: (T) -> Collection<T>,
                        entry: (T) -> SqlTraceEntry?, name: (T) -> String, selected: T? = null): SqlTraceGraph {
        if (nodes.size > 4096) throw SqlAnalysisLimit()
        var steps = 0
        fun check() {
            ProgressManager.checkCanceled()
            if (++steps > 65_536) throw SqlAnalysisLimit()
        }
        fun identitySet(): MutableSet<T> = Collections.newSetFromMap(IdentityHashMap<T, Boolean>())
        val allowed = identitySet().apply { addAll(nodes) }
        val visible = nodes.mapNotNull { node -> entry(node)?.let { Triple(node, it, name(node)) } }
            .sortedWith(compareBy({ it.second.offset }, { it.third }))
        val ids = IdentityHashMap<T, Int>()
        visible.forEachIndexed { id, value -> ids[value.first] = id }
        val children = List(visible.size) { linkedSetOf<Int>() }
        visible.forEachIndexed { consumer, value ->
            val visited = identitySet()
            val queue = ArrayDeque<T>().apply { addAll(inputs(value.first)) }
            while (queue.isNotEmpty()) {
                check()
                val input = queue.removeFirst()
                if (input !in allowed || !visited.add(input)) continue
                val parent = ids[input]
                if (parent != null) children[parent].add(consumer)
                else queue.addAll(inputs(input))
            }
        }
        val hasParent = BooleanArray(visible.size)
        children.forEach { row -> row.forEach { hasParent[it] = true } }
        val roots = visible.indices.filter { !hasParent[it] }.toMutableList()
        val reached = mutableSetOf<Int>()
        fun reach(start: Int) {
            val queue = ArrayDeque<Int>().apply { add(start) }
            while (queue.isNotEmpty()) {
                check()
                val id = queue.removeFirst()
                if (reached.add(id)) queue.addAll(children[id])
            }
        }
        roots.forEach(::reach)
        // A component made entirely of a cycle still needs a visible starting point.
        visible.indices.forEach { if (it !in reached) { roots.add(it); reach(it) } }
        return SqlTraceGraph(visible.mapIndexed { id, value ->
            SqlTraceNode(id, value.second, value.third, children[id].sorted())
        }, roots, selected?.let { ids[it] })
    }
}
