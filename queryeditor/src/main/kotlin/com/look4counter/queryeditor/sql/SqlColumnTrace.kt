package com.look4counter.queryeditor.sql

import com.intellij.openapi.progress.ProgressManager
import com.look4counter.queryeditor.database.MetadataSnapshot
import java.util.IdentityHashMap
import java.util.Locale

data class SqlTraceEntry(val line: Int, val text: String, val offset: Int) {
    override fun toString() = "$line: $text"
}

data class SqlTraceResult(val entries: List<SqlTraceEntry>, val message: String? = null)

object SqlColumnTrace {
    fun analyze(text: String, offset: Int, metadata: MetadataSnapshot?): SqlTraceResult {
        if (text.length > SqlQueryParser.MAX_LENGTH) return SqlTraceResult(emptyList(), "SQL 분석 범위를 초과했습니다.")
        if (!SqlLexer.completionAllowed(text, offset.coerceIn(0, text.length))) return SqlTraceResult(emptyList())
        return try { Trace(text, metadata).analyze(offset) }
        catch (_: SqlAnalysisLimit) { SqlTraceResult(emptyList(), "SQL 분석 범위를 초과했습니다.") }
    }

    private class Trace(val text: String, metadata: MetadataSnapshot?) {
        val parser = SqlQueryParser(text)
        val resolver = SqlQueryScope(metadata)
        val outputCache = IdentityHashMap<SqlScope, List<Output>>()
        val outputScopes = IdentityHashMap<SqlScope, Boolean>()
        val slots = mutableMapOf<Pair<SqlScope, Int>, Node>()
        val sources = IdentityHashMap<SqlSource, MutableMap<SqlIdentifier, Node>>()
        val references = mutableMapOf<Int, Node>()
        val building = mutableSetOf<Node>()
        var steps = 0

        class Node(val offset: Int? = null) {
            val inputs = linkedSetOf<Node>()
            val consumers = linkedSetOf<Node>()
            var message: String? = null
            fun add(input: Node) { inputs.add(input); input.consumers.add(this) }
        }

        data class Output(val name: SqlIdentifier, val token: SqlToken, val expression: List<SqlToken>,
                          val source: SqlSource? = null)

        fun analyze(offset: Int): SqlTraceResult {
            parser.parse(offset)
            val tokens = parser.tokens
            val index = tokens.indexOfFirst { (it.identifier != null || it.symbol("*")) && it.start < offset && offset <= it.end }.takeIf { it >= 0 }
                ?: tokens.indexOfFirst { it.start <= offset && offset < it.end }
            val token = tokens.getOrNull(index) ?: return SqlTraceResult(emptyList())
            if (token.identifier == null && !token.symbol("*")) return SqlTraceResult(emptyList())
            val scope = parser.scopes.filter { it.start <= token.start && token.end <= it.end }
                .minByOrNull { it.end - it.start } ?: return SqlTraceResult(emptyList())
            if (scope.clause(token.start) !in setOf("SELECT", "WHERE", "ON", "HAVING", "GROUP", "ORDER")) return SqlTraceResult(emptyList())
            if (!isOutputScope(scope)) return SqlTraceResult(emptyList())
            // Nodes are identified by scope and output slot, never by the displayed star position.
            parser.scopes.filter(::isOutputScope).forEach { current -> outputs(current).indices.forEach { outputNode(current, it) } }
            tokens.forEachIndexed { i, candidate ->
                if (candidate.identifier == null || tokens.getOrNull(i + 1)?.symbol(".") == true ||
                    tokens.getOrNull(i + 1)?.symbol("(") == true ||
                    (!candidate.identifier.quoted && candidate.text.uppercase(Locale.ROOT) in SqlKeywords.words)) return@forEachIndexed
                val current = parser.scopes.filter { it.start <= candidate.start && candidate.end <= it.end }
                    .minByOrNull { it.end - it.start } ?: return@forEachIndexed
                if (!isOutputScope(current) || current.clause(candidate.start) != "SELECT") return@forEachIndexed
                if (outputs(current).any { it.token.start == candidate.start }) return@forEachIndexed
                var first = i
                while (first >= 2 && tokens[first - 1].symbol(".") && tokens[first - 2].identifier != null) first -= 2
                sqlPath(tokens.subList(first, i + 1))?.let { reference(current, it, candidate) }
            }
            val selectedOutputs = outputs(scope).indices.filter { outputs(scope)[it].token.start == token.start }
            // A condition can select the column to inspect without becoming a displayed stage.
            if (selectedOutputs.isEmpty() && references[token.start] == null && token.identifier != null &&
                tokens.getOrNull(index + 1)?.symbol(".") != true && tokens.getOrNull(index + 1)?.symbol("(") != true &&
                (token.identifier.quoted || token.text.uppercase(Locale.ROOT) !in SqlKeywords.words)) {
                var first = index
                while (first >= 2 && tokens[first - 1].symbol(".") && tokens[first - 2].identifier != null) first -= 2
                sqlPath(tokens.subList(first, index + 1))?.let { reference(scope, it, token) }
            }
            val seeds = selectedOutputs.map { outputNode(scope, it) }.ifEmpty { listOfNotNull(references[token.start]) }
            if (seeds.isEmpty()) return SqlTraceResult(emptyList())
            val connected = linkedSetOf<Node>()
            val queue = ArrayDeque<Node>().apply { addAll(seeds) }
            while (queue.isNotEmpty()) {
                check()
                val node = queue.removeFirst()
                if (connected.add(node)) { queue.addAll(node.inputs); queue.addAll(node.consumers) }
            }
            val offsets = linkedSetOf<Int>()
            val visited = mutableSetOf<Node>()
            val active = mutableSetOf<Node>()
            var message: String? = null
            fun visit(node: Node, depth: Int) {
                if (node in active) { message = message ?: "순환 참조가 있어 일부 경로만 표시합니다."; return }
                if (!visited.add(node)) return
                if (depth > 48) throw SqlAnalysisLimit()
                active.add(node)
                node.inputs.forEach { visit(it, depth + 1) }
                active.remove(node)
                node.offset?.let(offsets::add)
                message = message ?: node.message
            }
            connected.sortedBy { it.offset ?: Int.MAX_VALUE }.forEach { visit(it, 0) }
            val starts = mutableListOf(0)
            text.forEachIndexed { i, c -> if (c == '\n') starts.add(i + 1) }
            return SqlTraceResult(offsets.map { position ->
                val line = starts.binarySearch(position).let { if (it >= 0) it else -it - 2 }
                val end = text.indexOf('\n', starts[line]).takeIf { it >= 0 } ?: text.length
                SqlTraceEntry(line + 1, text.substring(starts[line], end).trim(), position)
            }, message)
        }

        fun check() {
            ProgressManager.checkCanceled()
            if (++steps > 4096 || building.size >= 48) throw SqlAnalysisLimit()
        }

        fun isOutputScope(scope: SqlScope): Boolean = outputScopes.getOrPut(scope) {
            check()
            val parent = parser.scopes.filter { it.start < scope.start && scope.end <= it.end }
                .minByOrNull { it.end - it.start }
            parent == null || parent.clause(scope.start) in setOf("SELECT", "FROM") && isOutputScope(parent)
        }

        fun build(node: Node, action: () -> Unit): Node {
            check()
            building.add(node)
            try { action() } finally { building.remove(node) }
            return node
        }

        fun outputNode(scope: SqlScope, index: Int): Node {
            val key = scope to index
            slots[key]?.let { return it }
            val output = outputs(scope)[index]
            val node = Node(output.token.start)
            slots[key] = node
            return build(node) {
                if (output.source != null) node.add(source(output.source, output.name))
                else expression(scope, output.expression, node)
            }
        }

        fun outputs(scope: SqlScope): List<Output> = outputCache.getOrPut(scope) {
            check()
            scope.projections.flatMap { item ->
                val star = item.lastOrNull()?.symbol("*") == true &&
                    (item.size == 1 || item.getOrNull(item.lastIndex - 1)?.symbol(".") == true)
                if (star) {
                    val sources = if (item.size == 1) scope.sources else
                        sqlPath(item.dropLast(2))?.let { resolver.sources(scope, it) }.orEmpty()
                    sources.flatMap { source -> resolver.columns(source).map { name ->
                        Output(SqlIdentifier(name), item.last(), item, source)
                    } }
                } else {
                    var depth = 0
                    val top = item.filter { t ->
                        if (t.symbol(")")) depth--
                        val include = depth == 0
                        if (t.symbol("(")) depth++
                        include
                    }
                    val asIndex = top.indexOfLast { it.keyword("AS") }
                    val explicit = asIndex >= 0 && asIndex + 2 == top.size
                    val path = sqlPath(item)
                    val previous = top.getOrNull(top.lastIndex - 1)
                    val implicit = path == null && top.lastOrNull()?.identifier != null && previous != null &&
                        (previous.identifier != null || previous.symbol(")") || previous.text.firstOrNull()?.let { it.isDigit() || it == '\'' } == true)
                    val alias = if (explicit || implicit) top.last() else null
                    val name = alias?.identifier ?: path?.last()
                    if (name == null) emptyList() else {
                        val expression = if (alias == null) item else item.takeWhile { it.start < (if (explicit) top[asIndex].start else alias.start) }
                        listOf(Output(name, alias ?: item.last(), expression))
                    }
                }
            }
        }

        fun reference(scope: SqlScope, path: List<SqlIdentifier>, token: SqlToken): Node {
            references[token.start]?.let { return it }
            check()
            val projected = outputs(scope).any { output -> output.expression.any { it.start == token.start } }
            val node = Node(if (projected || scope.clause(token.start) != "SELECT") null else token.start)
            references[token.start] = node
            val name = path.last()
            if (path.size == 1 && scope.clause(token.start) in setOf("ORDER", "GROUP")) {
                val aliases = outputs(scope).indices.filter { outputs(scope)[it].name.matches(name) }
                if (aliases.size == 1) { node.add(outputNode(scope, aliases.single())); return node }
                if (aliases.size > 1) { node.message = "출력 컬럼을 유일하게 확인할 수 없습니다."; return node }
            }
            val visible = resolver.visible(scope)
            val sources = if (path.size > 1) resolver.sources(scope, path.dropLast(1)) else {
                val known = visible.filter { source -> resolver.columns(source).any { name.matches(SqlIdentifier(it, name.quoted)) } }
                val unknown = visible.filter { it.query == null && it.cte == null && resolver.columns(it).isEmpty() }
                (known + unknown).distinct()
            }
            if (sources.size != 1) {
                node.message = if (sources.isEmpty()) "컬럼의 출처를 확인할 수 없습니다." else "컬럼 출처가 여러 개여서 경로를 확정할 수 없습니다."
                return node
            }
            node.add(source(sources.single(), name))
            return node
        }

        fun source(source: SqlSource, name: SqlIdentifier): Node {
            val canonical = if (name.quoted) name.copy(spelling = name.value) else SqlIdentifier(name.value.uppercase(Locale.ROOT))
            val columns = sources.getOrPut(source) { mutableMapOf() }
            columns[canonical]?.let { return it }
            val node = Node()
            columns[canonical] = node
            return build(node) {
                val query = source.cte?.query ?: source.query ?: return@build
                val first = query.scopes.firstOrNull() ?: return@build
                val declared = source.columnNames ?: source.cte?.columnNames
                val available = outputs(first)
                val names = declared ?: available.map { it.name }
                val indices = names.indices.filter { names[it].matches(name) }
                if (indices.size != 1) { node.message = "출력 컬럼을 유일하게 확인할 수 없습니다."; return@build }
                val index = indices.single()
                for (branch in query.scopes) {
                    if (index !in outputs(branch).indices) { node.message = "출력 컬럼 위치를 확인할 수 없습니다."; continue }
                    node.add(outputNode(branch, index))
                }
            }
        }

        fun expression(scope: SqlScope, tokens: List<SqlToken>, node: Node) {
            check()
            sqlPath(tokens)?.let { node.add(reference(scope, it, tokens.last())); return }
            if (tokens.any { it.keyword("SELECT") || it.keyword("WITH") }) {
                node.message = "스칼라 서브쿼리 표현식은 추적할 수 없습니다."
                return
            }
            var index = 0
            while (index < tokens.size) {
                val token = tokens[index]
                if (token.identifier == null || (!token.identifier.quoted && token.text.uppercase(Locale.ROOT) in SqlKeywords.words)) { index++; continue }
                val path = mutableListOf(token.identifier)
                var last = index
                while (last + 2 < tokens.size && tokens[last + 1].symbol(".") && tokens[last + 2].identifier != null) {
                    path.add(tokens[last + 2].identifier!!)
                    last += 2
                }
                if (tokens.getOrNull(last + 1)?.symbol("(") != true) node.add(reference(scope, path, tokens[last]))
                index = last + 1
            }
        }
    }
}
