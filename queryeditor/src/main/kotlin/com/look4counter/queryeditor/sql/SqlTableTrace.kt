package com.look4counter.queryeditor.sql

import com.intellij.openapi.progress.ProgressManager
import java.util.IdentityHashMap
import java.util.Locale

internal object SqlTableTrace {
    // Null means this caret belongs to the column analyzer, not a table selection.
    fun analyze(text: String, offset: Int): SqlTraceResult? {
        if (text.length > SqlQueryParser.MAX_LENGTH) return SqlTraceResult(emptyList(), "SQL 분석 범위를 초과했습니다.")
        val caret = offset.coerceIn(0, text.length)
        // Parser tokens distinguish quoted identifiers from strings and omit comments.
        // The completion guard excludes both kinds of quotes, so it cannot be used here.
        return try { Trace(text).analyze(caret) }
        catch (_: SqlAnalysisLimit) { SqlTraceResult(emptyList(), "SQL 분석 범위를 초과했습니다.") }
    }

    private class Trace(val text: String) {
        val parser = SqlQueryParser(text, includeDml = true)
        val sources = IdentityHashMap<SqlSource, Node>()
        val definitions = IdentityHashMap<SqlCte, Node>()
        var steps = 0
        var depth = 0

        class Node(val offset: Int, val name: String) {
            val inputs = linkedSetOf<Node>()
            val consumers = linkedSetOf<Node>()
            fun add(input: Node) { inputs.add(input); input.consumers.add(this) }
        }

        fun check() {
            ProgressManager.checkCanceled()
            if (++steps > 16_384 || sources.size + definitions.size > 4096 || depth > 96) throw SqlAnalysisLimit()
        }

        fun build(node: Node, query: SqlQuery?): Node {
            check()
            depth++
            try {
                query?.scopes?.forEach { scope -> scope.sources.forEach { node.add(source(it)) } }
            } finally { depth-- }
            return node
        }

        fun definition(cte: SqlCte): Node {
            definitions[cte]?.let { return it }
            val node = Node(cte.nameToken.start, "CTE ${cte.name.spelling} (정의)")
            definitions[cte] = node
            return build(node, cte.query)
        }

        fun source(source: SqlSource): Node {
            sources[source]?.let { return it }
            check()
            val alias = source.alias?.let { " AS ${it.spelling}" }.orEmpty()
            val name = when {
                source.cte != null -> "CTE ${source.cte.name.spelling}$alias (참조)"
                source.query != null -> "파생 테이블 ${source.reference}"
                else -> "테이블 ${source.path.joinToString(".") { it.spelling }}$alias"
            }
            val node = Node(source.nameTokens.lastOrNull()?.start ?: source.aliasToken?.start ?: source.start, name)
            sources[source] = node
            if (source.cte != null) {
                depth++
                try { node.add(definition(source.cte)) } finally { depth-- }
            } else build(node, source.query)
            return node
        }

        fun selectedSource(source: SqlSource) = source.cte?.let(::definition) ?: source(source)

        fun selectableAlias(token: SqlToken): Boolean = token.identifier?.quoted == true ||
            token.text.uppercase(Locale.ROOT) !in setOf("PIVOT", "UNPIVOT", "MODEL", "MATCH_RECOGNIZE")

        fun analyze(offset: Int): SqlTraceResult? {
            parser.parse(offset)
            val tokens = parser.tokens
            val index = tokens.indexOfFirst { it.identifier != null && it.start < offset && offset <= it.end }
                .takeIf { it >= 0 } ?: tokens.indexOfFirst { it.start <= offset && offset < it.end }
            val token = tokens.getOrNull(index)?.takeIf { it.identifier != null } ?: return null
            parser.scopes.forEach { scope ->
                scope.ctes.forEach(::definition)
                scope.sources.forEach(::source)
            }
            val cte = definitions.keys.firstOrNull { it.nameToken.start == token.start }
            val source = sources.keys.firstOrNull { source ->
                source.aliasToken?.start == token.start && selectableAlias(token) || source.nameTokens.any { it.start == token.start }
            }
            val selectedReference = source ?: qualifier(index)
            val selected = when {
                cte != null -> definition(cte)
                else -> selectedReference?.let(::selectedSource)
            } ?: return null
            val selectedNode = if (cte != null) definition(cte) else selectedReference?.let(::source)
            return result(selected, selectedNode).copy(selectedOffset = selectedNode?.offset)
        }

        fun qualifier(index: Int): SqlSource? {
            val tokens = parser.tokens
            if (tokens.getOrNull(index + 1)?.symbol(".") != true) return null
            var first = index
            while (first >= 2 && tokens[first - 1].symbol(".") && tokens[first - 2].identifier != null) first -= 2
            // Do not interpret a bind or a function name as a table qualifier.
            if (tokens.getOrNull(first - 1)?.symbol(":") == true) return null
            var last = index
            while (last + 2 < tokens.size && tokens[last + 1].symbol(".") &&
                (tokens[last + 2].identifier != null || tokens[last + 2].symbol("*"))) last += 2
            if (tokens.getOrNull(last + 1)?.symbol("(") == true) return null
            val pathEnd = if (tokens.getOrNull(last + 1)?.symbol(".") == true) last + 1 else last - 1
            if (pathEnd <= first) return null
            val path = sqlPath(tokens.subList(first, pathEnd)) ?: return null
            val token = tokens[index]
            val scope = parser.scopes.filter { it.start <= token.start && token.end <= it.end }
                .minByOrNull { it.end - it.start } ?: return null
            return SqlQueryScope(null).sources(scope, path).singleOrNull()?.takeIf {
                it.aliasToken?.let(::selectableAlias) != false
            }
        }

        fun result(selected: Node, selectedNode: Node?): SqlTraceResult {
            val ancestors = linkedSetOf<Node>()
            val upstream = ArrayDeque<Node>().apply { add(selected) }
            while (upstream.isNotEmpty()) {
                check()
                val node = upstream.removeFirst()
                if (ancestors.add(node)) upstream.addAll(node.inputs)
            }
            // Follow every use of the proven origins; do not add other inputs of later joins.
            val origins = ancestors.filter { it.inputs.isEmpty() }.ifEmpty { listOf(selected) }
            val related = linkedSetOf<Node>()
            val downstream = ArrayDeque<Node>().apply { addAll(origins) }
            while (downstream.isNotEmpty()) {
                check()
                val node = downstream.removeFirst()
                if (related.add(node)) downstream.addAll(node.consumers)
            }
            val visited = mutableSetOf<Node>()
            val active = mutableSetOf<Node>()
            val offsets = mutableListOf<Int>()
            var cycle = false
            fun visit(node: Node) {
                check()
                if (node in active) { cycle = true; return }
                if (!visited.add(node)) return
                if (active.size >= 96) throw SqlAnalysisLimit()
                active.add(node)
                node.inputs.filter { it in related }.forEach(::visit)
                active.remove(node)
                offsets.add(node.offset)
            }
            related.sortedBy { it.offset }.forEach(::visit)
            val starts = mutableListOf(0)
            text.forEachIndexed { i, c -> if (c == '\n') starts.add(i + 1) }
            fun entry(position: Int): SqlTraceEntry {
                val line = starts.binarySearch(position).let { if (it >= 0) it else -it - 2 }
                val end = text.indexOf('\n', starts[line]).takeIf { it >= 0 } ?: text.length
                return SqlTraceEntry(line + 1, text.substring(starts[line], end).trim(), position)
            }
            val entries = offsets.filter { it in text.indices }.map(::entry).distinctBy { it.line }
            val graph = SqlTraceGraphBuilder.build(related, { it.inputs },
                { it.offset.takeIf { position -> position in text.indices }?.let(::entry) }, { it.name }, selectedNode)
            return SqlTraceResult(entries, if (cycle) "순환 참조가 있어 일부 경로만 표시합니다." else null, graph = graph)
        }
    }
}
