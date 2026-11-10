package com.look4counter.queryeditor.sql

import com.intellij.openapi.progress.ProgressManager
import java.util.IdentityHashMap
import java.util.Collections
import java.util.Locale

data class SqlColumnProblem(val start: Int, val end: Int, val message: String)

object SqlColumnValidator {
    fun analyze(text: String): List<SqlColumnProblem> = try {
        SqlFormattingTokens(text) // Incomplete quotes, comments and parentheses cannot prove absence.
        Validation(text).analyze()
    } catch (_: SqlAnalysisLimit) { emptyList() }
      catch (_: SqlFormatFailure) { emptyList() }

    private class Validation(text: String) {
        private val parser = SqlQueryParser(text, includeDml = true)
        private val resolver = SqlQueryScope(null)
        private val cache = IdentityHashMap<Any, Shape>()
        private val visiting = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())
        private val ignored = mutableSetOf<Int>()
        private val problems = linkedMapOf<Int, SqlColumnProblem>()
        private var steps = 0
        private val unknown = Shape(emptyList(), true)
        private val grammar = setOf("NULL", "MATCHED", "THEN", "MERGE", "DELETE", "INSERT", "UPDATE", "SET", "VALUES",
            "ROWNUM", "ROWID", "ORA_ROWSCN", "LEVEL", "CONNECT_BY_ISLEAF", "CONNECT_BY_ISCYCLE", "SYSDATE", "SYSTIMESTAMP", "UID",
            "AT", "TIME", "ZONE", "FILTER", "PARTITION", "OVER", "ASC", "DESC", "NULLS", "FIRST", "LAST", "RETURNING")

        private data class Shape(val slots: List<SqlIdentifier?>, val unknownWidth: Boolean = false) {
            val complete: Boolean get() = !unknownWidth && slots.all { it != null }
            fun contains(name: SqlIdentifier) = slots.any { declared -> declared != null &&
                if (declared.quoted && name.quoted) declared.value == name.value else declared.value.equals(name.value, ignoreCase = true) }
        }

        fun analyze(): List<SqlColumnProblem> {
            val tokens = parser.tokens
            val unfinished = setOf("SELECT", "FROM", "JOIN", "AS", "WITH", "SET", "WHERE", "ON", "AND", "OR", "GROUP", "ORDER", "HAVING", "INTO", "USING")
            val last = tokens.lastOrNull() ?: return emptyList()
            if (last.symbol(".") || last.symbol(",") || unfinished.any(last::keyword) ||
                tokens.count { it.keyword("CASE") } != tokens.count { it.keyword("END") } ||
                tokens.zipWithNext().any { (a, b) -> a.symbol(",") && (b.symbol(")") || b.symbol(";") || b.keyword("FROM")) }) return emptyList()
            (listOf(0) + tokens.filter { it.symbol(";") }.map { it.end }).forEach(parser::parse)
            parser.scopes.forEach { scope -> scope.projections.forEach { expression(it) } }
            tokens.forEachIndexed { index, token ->
                if (index % 128 == 0) ProgressManager.checkCanceled()
                if (token.identifier == null || token.start in ignored || tokens.getOrNull(index + 1)?.symbol(".") == true ||
                    tokens.getOrNull(index + 1)?.symbol("(") == true) return@forEachIndexed
                val scope = parser.scopes.filter { it.start <= token.start && token.end <= it.end }
                    .minByOrNull { it.end - it.start } ?: return@forEachIndexed
                if (transformed(scope)) return@forEachIndexed
                val clause = scope.clause(token.start)
                if (clause !in setOf("SELECT", "WHERE", "ON", "GROUP", "HAVING", "ORDER", "SET", "VALUES")) return@forEachIndexed
                var first = index
                while (first >= 2 && tokens[first - 1].symbol(".") && tokens[first - 2].identifier != null) first -= 2
                val path = sqlPath(tokens.subList(first, index + 1)) ?: return@forEachIndexed
                val previous = tokens.getOrNull(first - 1)
                if (previous?.keyword("AS") == true || previous?.keyword("COLLATE") == true || previous?.keyword("OVER") == true ||
                    previous?.keyword("WINDOW") == true || previous?.symbol("::") == true || previous?.symbol("@") == true) return@forEachIndexed
                val direct = scope.projections.any { sqlPath(expression(it)) == path && it.any { part -> part.start == token.start } }
                if (!token.identifier.quoted && path.size == 1 &&
                    (token.text.uppercase(Locale.ROOT) in grammar || !direct && token.text.uppercase(Locale.ROOT) in SqlKeywords.words)) return@forEachIndexed
                if ((token.text.uppercase(Locale.ROOT) in setOf("N", "E", "X", "B", "Q") || token.text.startsWith("_")) &&
                    tokens.getOrNull(index + 1)?.let { it.start == token.end && it.text.startsWith("'") } == true) return@forEachIndexed
                if (path.size == 1 && clause in setOf("ORDER", "GROUP", "HAVING", "WHERE", "ON") &&
                    scope.projections.any { item -> item.lastOrNull()?.let { it.start in ignored && it.identifier?.matches(path.last()) == true } == true }) return@forEachIndexed
                val sources = if (path.size > 1) resolver.sources(scope, path.dropLast(1)) else resolver.visible(scope)
                if (sources.isNotEmpty() && sources.all { source(it).complete } && sources.none { source(it).contains(path.last()) }) error(token)
            }
            parser.scopes.forEach { scope ->
                val target = scope.sources.firstOrNull() ?: return@forEach
                val shape = source(target)
                if (shape.complete) scope.writeColumns.filter { !shape.contains(it.identifier!!) }.forEach(::error)
            }
            return problems.values.sortedBy { it.start }
        }

        private fun error(token: SqlToken) {
            problems[token.start] = SqlColumnProblem(token.start, token.end, "확정된 출력에 '${token.text}' 컬럼이 없습니다.")
        }

        private fun transformed(scope: SqlScope) = scope.top.any { token ->
            listOf("PIVOT", "UNPIVOT", "MODEL", "MATCH_RECOGNIZE").any(token::keyword)
        }

        private fun shape(key: Any, action: () -> Shape): Shape {
            ProgressManager.checkCanceled()
            cache[key]?.let { return it }
            if (key in visiting) return unknown
            if (visiting.size >= 48 || ++steps > 4096) throw SqlAnalysisLimit()
            visiting.add(key)
            return try { action().also { cache[key] = it } } finally { visiting.remove(key) }
        }

        private fun rename(actual: Shape, declared: List<SqlIdentifier>?): Shape {
            if (declared == null) return actual
            return Shape(declared + actual.slots.drop(declared.size), actual.unknownWidth || declared.size > actual.slots.size)
        }

        private fun source(source: SqlSource): Shape = shape(source) {
            val actual = when {
                source.cte != null -> shape(source.cte) {
                    rename(source.cte.query?.scopes?.firstOrNull()?.let(::output) ?: unknown, source.cte.columnNames)
                }
                source.query != null -> source.query.scopes.firstOrNull()?.let(::output) ?: unknown
                else -> unknown
            }
            rename(actual, source.columnNames)
        }

        private fun output(scope: SqlScope): Shape = shape(scope) {
            val slots = mutableListOf<SqlIdentifier?>()
            var unknownWidth = false
            scope.projections.forEach { item ->
                val expr = expression(item)
                val alias = item.lastOrNull()?.takeIf { it.start in ignored }?.identifier
                val path = sqlPath(expr)
                when {
                    alias != null -> slots.add(alias)
                    path != null -> slots.add(path.last())
                    expr.lastOrNull()?.symbol("*") == true && (expr.size == 1 || expr.getOrNull(expr.lastIndex - 1)?.symbol(".") == true) -> {
                        if (transformed(scope)) unknownWidth = true
                        val sources = if (expr.size == 1) scope.sources else sqlPath(expr.dropLast(2))?.let { resolver.sources(scope, it) }.orEmpty()
                        if (sources.isEmpty()) unknownWidth = true
                        sources.forEach { relation -> source(relation).let { slots.addAll(it.slots); unknownWidth = unknownWidth || it.unknownWidth } }
                    }
                    else -> slots.add(null)
                }
            }
            Shape(slots, unknownWidth || scope.projections.isEmpty())
        }

        private fun expression(item: List<SqlToken>): List<SqlToken> {
            var depth = 0
            val top = item.filter { token ->
                if (token.symbol(")")) depth--
                val include = depth == 0
                if (token.symbol("(")) depth++
                include
            }
            val asIndex = top.indexOfLast { it.keyword("AS") }
            val explicit = asIndex >= 0 && asIndex + 2 == top.size
            val previous = top.getOrNull(top.lastIndex - 1)
            val implicit = sqlPath(item) == null && top.lastOrNull()?.identifier != null && previous != null &&
                (previous.identifier != null || previous.symbol(")") || previous.text.firstOrNull()?.let { it.isDigit() || it == '\'' } == true)
            if (!explicit && !implicit) return item
            val alias = top.last()
            ignored.add(alias.start)
            return item.takeWhile { it.start < if (explicit) top[asIndex].start else alias.start }
        }
    }
}
