package com.look4counter.queryeditor.sql

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.TokenType
import java.util.Locale

internal data class SqlIdentifier(val value: String, val quoted: Boolean = false, val spelling: String = value) {
    fun matches(other: SqlIdentifier): Boolean =
        if (quoted || other.quoted) value == other.value else value.equals(other.value, ignoreCase = true)
}

internal data class SqlToken(val text: String, val start: Int, val end: Int, val identifier: SqlIdentifier? = null) {
    fun keyword(word: String) = identifier?.quoted != true && text.equals(word, ignoreCase = true)
    fun symbol(value: String) = identifier == null && text == value
}

internal class SqlCte(val name: SqlIdentifier, val columnNames: List<SqlIdentifier>?, val nameToken: SqlToken) {
    var query: SqlQuery? = null
}

internal data class SqlSource(
    val path: List<SqlIdentifier>,
    val alias: SqlIdentifier?,
    val cte: SqlCte? = null,
    val query: SqlQuery? = null,
    val columnNames: List<SqlIdentifier>? = null,
    val callable: Boolean = false,
    val nameTokens: List<SqlToken> = emptyList(),
    val aliasToken: SqlToken? = null,
    val start: Int = -1,
) {
    val reference: String get() = alias?.spelling ?: path.joinToString(".") { it.spelling }
    fun matches(parts: List<SqlIdentifier>): Boolean = if (alias != null) {
        parts.size == 1 && alias.matches(parts.single())
    } else path.isNotEmpty() && (samePath(path, parts) || parts.size == 1 && path.last().matches(parts.single()))

    private fun samePath(a: List<SqlIdentifier>, b: List<SqlIdentifier>) =
        a.size == b.size && a.zip(b).all { (left, right) -> left.matches(right) }
}

internal class SqlScope(
    val start: Int,
    val end: Int,
    val top: List<SqlToken>,
    val projections: List<List<SqlToken>>,
    val ctes: List<SqlCte>,
    val outer: SqlScope?,
    val statement: String = "SELECT",
) {
    val sources = mutableListOf<SqlSource>()
    val writeColumns = mutableListOf<SqlToken>()

    fun clause(offset: Int): String? {
        var clause: String? = null
        var cases = 0
        for (token in top) {
            if (token.end > offset) break
            if (token.keyword("CASE")) { cases++; continue }
            if (token.keyword("END") && cases > 0) { cases--; continue }
            if (cases > 0) continue
            when {
                statement != "SELECT" && token.keyword("SET") -> clause = "SET"
                statement != "SELECT" && token.keyword("VALUES") -> clause = "VALUES"
                statement != "SELECT" && token.keyword("INSERT") -> clause = "INSERT"
                statement != "SELECT" && token.keyword("USING") -> clause = "FROM"
                statement != "SELECT" && token.keyword("WHEN") -> clause = "ON"
                token.keyword("SELECT") -> clause = "SELECT"
                token.keyword("FROM") || token.keyword("JOIN") -> clause = "FROM"
                token.keyword("ON") || token.keyword("USING") -> clause = "ON"
                token.keyword("WHERE") -> clause = "WHERE"
                token.keyword("GROUP") -> clause = "GROUP"
                token.keyword("HAVING") -> clause = "HAVING"
                token.keyword("ORDER") -> clause = "ORDER"
                token.keyword("LIMIT") || token.keyword("OFFSET") || token.keyword("FETCH") -> clause = null
                token.symbol(",") && clause == "ON" -> clause = "FROM"
            }
        }
        return clause
    }
}

internal data class SqlQuery(val scopes: List<SqlScope>)
internal class SqlAnalysisLimit : RuntimeException(null, null, false, false)

/** A bounded, tolerant structural parser. It deliberately does not validate SQL or execute it. */
internal class SqlQueryParser(private val text: String, private val includeDml: Boolean = false) {
    companion object {
        const val MAX_LENGTH = 262_144
        private const val MAX_TOKENS = 20_000
        private const val MAX_DEPTH = 48
        private val CLAUSES = setOf("WHERE", "GROUP", "HAVING", "ORDER", "LIMIT", "OFFSET", "FETCH", "WINDOW", "QUALIFY", "FOR")
        private val ALIAS_BOUNDARIES = CLAUSES + setOf("SELECT", "FROM", "JOIN", "INNER", "LEFT", "RIGHT", "FULL", "OUTER",
            "CROSS", "NATURAL", "ON", "USING", "UNION", "EXCEPT", "INTERSECT", "AS", "WITH", "CONNECT", "START", "SET", "RETURNING",
            "VALUES", "WHEN", "MATCHED", "THEN", "INSERT", "UPDATE", "DELETE")
    }

    val tokens: List<SqlToken> = tokenize()
    private val pairs = IntArray(tokens.size) { tokens.size }
    val scopes = mutableListOf<SqlScope>()
    private val parsedGroups = mutableSetOf<Int>()

    init {
        val stack = ArrayDeque<Int>()
        tokens.forEachIndexed { index, token ->
            if (token.symbol("(")) {
                stack.addLast(index)
                if (stack.size > MAX_DEPTH) throw SqlAnalysisLimit()
            } else if (token.symbol(")") && stack.isNotEmpty()) pairs[stack.removeLast()] = index
        }
    }

    fun parse(offset: Int) {
        val start = tokens.indexOfLast { it.symbol(";") && it.end <= offset } + 1
        val end = tokens.indexOfFirst { it.symbol(";") && it.start >= offset }.takeIf { it >= 0 } ?: tokens.size
        query(start, end, emptyList(), null, 0)
    }

    private fun tokenize(): List<SqlToken> {
        if (text.length > MAX_LENGTH) throw SqlAnalysisLimit()
        val lexer = SqlLexer()
        lexer.start(text)
        val result = mutableListOf<SqlToken>()
        while (lexer.tokenType != null) {
            if (result.size % 256 == 0) ProgressManager.checkCanceled()
            val start = lexer.tokenStart
            var end = lexer.tokenEnd
            val type = lexer.tokenType
            if (type == TokenType.WHITE_SPACE || type == SqlTokens.COMMENT) {
                lexer.advance()
                continue
            }
            val first = text[start]
            val quoted = first == '"' || first == '`' || first == '['
            if (quoted && (type == SqlTokens.QUOTED || type == SqlTokens.SYMBOL)) {
                val closing = if (first == '[') ']' else first
                if (first != '"') {
                    end = start + 1
                    while (end < text.length) {
                        if (text[end++] == closing) {
                            if (end < text.length && text[end] == closing) end++ else break
                        }
                    }
                }
                val raw = text.substring(start, end)
                val value = raw.substring(1).removeSuffix(closing.toString()).replace("$closing$closing", closing.toString())
                result.add(SqlToken(raw, start, end, SqlIdentifier(value, true, raw)))
                lexer.start(text, end, text.length, 0)
            } else {
                val raw = text.substring(start, end)
                val name = if (type == SqlTokens.IDENTIFIER || type == SqlTokens.KEYWORD) SqlIdentifier(raw) else null
                result.add(SqlToken(raw, start, end, name))
                lexer.advance()
            }
            if (result.size > MAX_TOKENS) throw SqlAnalysisLimit()
        }
        return result
    }

    private fun topIndices(start: Int, end: Int): List<Int> {
        val result = mutableListOf<Int>()
        var index = start
        while (index < end) {
            result.add(index)
            index = if (tokens[index].symbol("(")) afterGroup(index, end) else index + 1
        }
        return result
    }

    private fun afterGroup(index: Int, end: Int) = (pairs[index] + 1).coerceAtMost(end)
    private fun endOffset(end: Int) = tokens.getOrNull(end)?.start ?: text.length
    private fun keyword(index: Int, value: String) = tokens.getOrNull(index)?.keyword(value) == true
    private fun isSet(index: Int) = listOf("UNION", "EXCEPT", "INTERSECT").any { keyword(index, it) }

    private fun names(start: Int, end: Int): List<SqlIdentifier> =
        topIndices(start, end).mapNotNull { tokens[it].identifier }

    private fun query(start: Int, end: Int, inherited: List<SqlCte>, outer: SqlScope?, depth: Int): SqlQuery? {
        if (depth > MAX_DEPTH) throw SqlAnalysisLimit()
        ProgressManager.checkCanceled()
        var index = start
        var ctes = inherited
        if (keyword(index, "WITH")) {
            index++
            val recursive = keyword(index, "RECURSIVE")
            if (recursive) index++
            while (index < end) {
                val nameToken = tokens[index]
                val name = tokens[index].identifier ?: break
                index++
                var columns: List<SqlIdentifier>? = null
                if (tokens.getOrNull(index)?.symbol("(") == true) {
                    columns = names(index + 1, pairs[index].coerceAtMost(end))
                    index = afterGroup(index, end)
                }
                if (!keyword(index, "AS")) break
                index++
                if (keyword(index, "NOT")) index++
                if (keyword(index, "MATERIALIZED")) index++
                if (tokens.getOrNull(index)?.symbol("(") != true) break
                val definition = SqlCte(name, columns, nameToken)
                val bodyEnvironment = if (recursive) shadow(ctes, definition) else ctes
                parsedGroups.add(index)
                definition.query = query(index + 1, pairs[index].coerceAtMost(end), bodyEnvironment, null, depth + 1)
                ctes = shadow(ctes, definition)
                index = afterGroup(index, end)
                if (tokens.getOrNull(index)?.symbol(",") != true) break
                index++
            }
        }
        val branches = mutableListOf<SqlScope>()
        var branchStart = index
        val separators = topIndices(index, end).filter(::isSet)
        for (branchEnd in separators + end) {
            val indices = topIndices(branchStart, branchEnd)
            val select = indices.firstOrNull { keyword(it, "SELECT") }
            if (select != null) {
                if (includeDml && indices.any { it < select && keyword(it, "INSERT") }) {
                    dml(branchStart, select, indices.filter { it < select }, ctes, outer, depth)
                }
                val top = indices.filter { it >= select }
                val from = top.firstOrNull { keyword(it, "FROM") }
                val projectionEnd = from ?: top.firstOrNull { boundary(it) } ?: branchEnd
                val scope = SqlScope(tokens[select].start, endOffset(branchEnd), top.map { tokens[it] },
                    projectionItems(select + 1, projectionEnd), ctes, outer)
                scopes.add(scope)
                branches.add(scope)
                if (from != null) parseSources(scope, from + 1, top.firstOrNull { it > from && boundary(it) } ?: branchEnd, depth)
                nested(scope, select + 1, branchEnd, depth)
            } else if (includeDml) {
                dml(branchStart, branchEnd, indices, ctes, outer, depth)?.let(branches::add)
            }
            branchStart = branchEnd + 1
        }
        return branches.takeIf { it.isNotEmpty() }?.let(::SqlQuery)
    }

    private fun shadow(ctes: List<SqlCte>, new: SqlCte) = ctes.filterNot { it.name.matches(new.name) } + new

    private fun dml(start: Int, end: Int, indices: List<Int>, ctes: List<SqlCte>, outer: SqlScope?, depth: Int): SqlScope? {
        var cases = 0
        val top = indices.filter { index ->
            when {
                keyword(index, "CASE") -> { cases++; false }
                keyword(index, "END") && cases > 0 -> { cases--; false }
                else -> cases == 0
            }
        }
        val head = top.firstOrNull { i -> listOf("UPDATE", "DELETE", "INSERT", "MERGE").any { keyword(i, it) } } ?: return null
        val statement = tokens[head].text.uppercase(Locale.ROOT)
        val scope = SqlScope(tokens[head].start, endOffset(end), indices.map { tokens[it] }, emptyList(), ctes, outer, statement)
        scopes.add(scope)
        fun first(word: String) = top.firstOrNull { keyword(it, word) }
        fun addSources(from: Int, until: Int) { if (from < until) parseSources(scope, from, until, depth) }
        when (statement) {
            "UPDATE" -> addSources(head + 1, first("SET") ?: end)
            "MERGE", "INSERT" -> {
                val into = first("INTO") ?: head
                val stop = top.firstOrNull { it > into && (tokens[it].symbol("(") || keyword(it, "USING") || keyword(it, "VALUES")) } ?: end
                addSources(into + 1, stop)
            }
        }
        first("FROM")?.let { from ->
            addSources(from + 1, top.firstOrNull { it > from && (boundary(it) || keyword(it, "USING")) } ?: end)
        }
        first("USING")?.let { using ->
            addSources(using + 1, top.firstOrNull { it > using && (keyword(it, "ON") || boundary(it) || keyword(it, "WHEN")) } ?: end)
        }
        top.filter { keyword(it, "INSERT") }.forEach { insert ->
            val open = top.firstOrNull { it > insert && tokens[it].symbol("(") }
            val branchEnd = top.firstOrNull { it > insert && (keyword(it, "WHEN") || keyword(it, "RETURNING")) } ?: end
            val values = top.firstOrNull { it > insert && it < branchEnd && keyword(it, "VALUES") }
            if (open != null && (values == null || open < values)) {
                val columns = projectionItems(open + 1, pairs[open].coerceAtMost(branchEnd))
                if (columns.all { sqlPath(it) != null }) {
                    scope.writeColumns.addAll(columns.map { it.last() })
                }
            }
        }
        nested(scope, start, end, depth)
        return scope
    }
    private fun boundary(index: Int) = tokens[index].identifier?.quoted != true && tokens[index].text.uppercase(Locale.ROOT) in CLAUSES

    private fun projectionItems(start: Int, end: Int): List<List<SqlToken>> {
        val items = mutableListOf<List<SqlToken>>()
        var itemStart = start
        if (keyword(itemStart, "DISTINCT") || keyword(itemStart, "ALL")) itemStart++
        for (comma in topIndices(itemStart, end).filter { tokens[it].symbol(",") } + end) {
            if (itemStart < comma) items.add(tokens.subList(itemStart, comma))
            itemStart = comma + 1
        }
        return items
    }

    private fun parseSources(scope: SqlScope, start: Int, end: Int, depth: Int) {
        var index = start
        var expected = true
        while (index < end) {
            val token = tokens[index]
            if (token.symbol(",") || token.keyword("JOIN")) {
                expected = true
                index++
                continue
            }
            if (!expected) {
                index = if (token.symbol("(")) afterGroup(index, end) else index + 1
                continue
            }
            val lateral = token.keyword("LATERAL")
            if (lateral) index++
            if (index >= end) break
            val sourceStart = tokens[index].start
            val nameTokens = mutableListOf<SqlToken>()
            val path = mutableListOf<SqlIdentifier>()
            var derived: SqlQuery? = null
            var callable = false
            if (tokens[index].symbol("(")) {
                parsedGroups.add(index)
                derived = query(index + 1, pairs[index].coerceAtMost(end), scope.ctes, if (lateral) scope else null, depth + 1)
                index = afterGroup(index, end)
            } else {
                val name = tokens[index].identifier
                if (name == null || tokens[index].text.uppercase(Locale.ROOT) in ALIAS_BOUNDARIES && !name.quoted) {
                    index++
                    continue
                }
                path.add(name)
                nameTokens.add(tokens[index])
                index++
                while (index + 1 < end && tokens[index].symbol(".") && tokens[index + 1].identifier != null) {
                    path.add(tokens[index + 1].identifier!!)
                    nameTokens.add(tokens[index + 1])
                    index += 2
                }
                if (index < end && tokens[index].symbol("(")) {
                    callable = true
                    index = afterGroup(index, end)
                }
            }
            if (keyword(index, "AS")) index++
            val aliasToken = tokens.getOrNull(index)?.takeIf { index < end && it.identifier != null &&
                (it.identifier.quoted || it.text.uppercase(Locale.ROOT) !in ALIAS_BOUNDARIES) }
            val alias = aliasToken?.identifier
            if (alias != null) index++
            var columns: List<SqlIdentifier>? = null
            if (alias != null && index < end && tokens[index].symbol("(") &&
                !keyword(index + 1, "SELECT") && !keyword(index + 1, "WITH")) {
                columns = names(index + 1, pairs[index].coerceAtMost(end))
                index = afterGroup(index, end)
            }
            val cte = if (path.size == 1 && !callable) scope.ctes.lastOrNull { it.name.matches(path.single()) } else null
            scope.sources.add(SqlSource(path, alias, cte, derived, columns, callable, nameTokens, aliasToken, sourceStart))
            // Keep adjacent derived tables distinct while the FROM clause is still being edited.
            expected = index < end && tokens[index].symbol("(") &&
                (keyword(index + 1, "SELECT") || keyword(index + 1, "WITH"))
        }
    }

    private fun nested(scope: SqlScope, start: Int, end: Int, depth: Int) {
        if (depth > MAX_DEPTH) throw SqlAnalysisLimit()
        var index = start
        while (index < end) {
            if (!tokens[index].symbol("(")) { index++; continue }
            val close = pairs[index].coerceAtMost(end)
            if (parsedGroups.add(index)) {
                if (keyword(index + 1, "SELECT") || keyword(index + 1, "WITH")) {
                    query(index + 1, close, scope.ctes, scope, depth + 1)
                } else nested(scope, index + 1, close, depth + 1)
            }
            index = afterGroup(index, end)
        }
    }
}
