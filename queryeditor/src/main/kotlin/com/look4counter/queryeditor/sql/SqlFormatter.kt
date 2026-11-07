package com.look4counter.queryeditor.sql

import com.intellij.openapi.progress.ProgressManager

data class SqlFormatResult(val sql: String?, val problem: String? = null)
data class SqlFormattingOptions(val alignWhereEquals: Boolean = false, val alignSelectAs: Boolean = false)

object SqlFormatter {
    fun formatSelection(text: String, start: Int, end: Int, options: SqlFormattingOptions = SqlFormattingOptions()): SqlFormatResult {
        val lexer = SqlLexer()
        lexer.start(text)
        while (lexer.tokenType != null) {
            if (lexer.tokenType == SqlTokens.COMMENT || lexer.tokenType == SqlTokens.QUOTED || lexer.tokenType == SqlTokens.PARAMETER) {
                if (start > lexer.tokenStart && start < lexer.tokenEnd || end > lexer.tokenStart && end < lexer.tokenEnd) {
                    return SqlFormatResult(null, "주석·문자열·파라미터 중간에서 선택한 범위는 포맷하지 않습니다.")
                }
            }
            lexer.advance()
        }
        val fragment = text.substring(start, end)
        if (fragment.isBlank()) return SqlFormatResult(fragment)
        return format(fragment, options)
    }
    fun format(text: String, options: SqlFormattingOptions = SqlFormattingOptions()): SqlFormatResult = try {
        val input = SqlFormattingTokens(text)
        val formatter = Formatter(input, text, options)
        val original = formatter.format()
        val padding = formatter.padding()
        SqlFormatResult(if (padding.isEmpty()) original else Formatter(input, text, options, padding).format())
    } catch (error: SqlFormatFailure) { SqlFormatResult(null, error.message) }

    private class Writer(private val source: String) {
        private val text = StringBuilder()
        private var indent: String? = ""
        private var separator = false
        private var consumed = 0
        var row = 0
            private set
        val column: Int get() = indent?.length ?: (text.length - maxOf(text.lastIndexOf("\n"), text.lastIndexOf("\r")) - 1 + if (separator) 1 else 0)
        fun before(token: SqlFormattingToken) {
            if (token.end <= consumed) return
            val gap = source.substring(consumed, token.start)
            val lastBreak = maxOf(gap.lastIndexOf('\n'), gap.lastIndexOf('\r'))
            if (lastBreak >= 0) {
                val breaks = gap.substring(0, lastBreak + 1)
                text.append(breaks)
                row += lineCount(breaks)
                separator = false
                indent = if (token.commentLine != null) "" else gap.substring(lastBreak + 1)
            }
            consumed = token.end
        }
        fun commentLine(token: SqlFormattingToken) {
            val lineEnd = source.indexOf('\n', token.end).takeIf { it >= 0 } ?: source.length
            consumed = if (lineEnd > 0 && source[lineEnd - 1] == '\r') lineEnd - 1 else lineEnd
        }
        fun line(at: Int) {
            if (indent != null) indent = " ".repeat(at.coerceAtLeast(0))
        }
        fun space() {
            if (indent == null && text.isNotEmpty() && !text.last().isWhitespace()) separator = true
        }
        fun raw(value: String) {
            indent?.let { text.append(it); indent = null }
            if (separator) text.append(' ')
            separator = false
            text.append(value)
            row += lineCount(value)
        }
        fun result(): String {
            if (consumed == 0) return source
            val trailing = source.substring(consumed)
            return text.toString() + if (trailing.any { it == '\n' || it == '\r' }) trailing else ""
        }
        private fun lineCount(value: String): Int = value.indices.count { value[it] == '\n' || value[it] == '\r' && value.getOrNull(it + 1) != '\n' }
    }

    private class Formatter(private val input: SqlFormattingTokens, source: String, private val options: SqlFormattingOptions,
                            private val padding: Map<Int, Int> = emptyMap()) {
        private val tokens = input.tokens
        private val writer = Writer(source)
        private var previous: SqlFormattingToken? = null
        private var joinNext = false
        private data class Anchor(val token: Int, val column: Int)
        private class AlignmentGroup {
            val anchors = mutableListOf<Anchor>()
            val rows = mutableSetOf<Int>()
        }
        private val groups = mutableListOf<AlignmentGroup>()
        private fun group() = AlignmentGroup().also(groups::add)

        fun padding(): Map<Int, Int> = buildMap {
            groups.forEach { group ->
                val maximum = group.anchors.maxOfOrNull { it.column } ?: return@forEach
                group.anchors.forEach { anchor -> if (maximum > anchor.column) put(anchor.token, maximum - anchor.column) }
            }
        }
        private val spacedParentheses = setOf("SELECT", "FROM", "WHERE", "ON", "AND", "OR", "NOT", "IN", "EXISTS", "AS", "BY", "OVER", "FILTER", "VALUES", "WHEN", "THEN", "ELSE")

        fun format(): String {
            range(0, tokens.size, 0, true)
            return writer.result()
        }

        private fun range(start: Int, end: Int, base: Int, query: Boolean, vertical: Int? = null, pivot: Boolean = false,
                          inheritedWhere: AlignmentGroup? = null) {
            var index = start
            var clause: String? = null
            var expressionColumn = base + 7
            var firstExpression = true
            var between = false
            val cases = ArrayDeque<Int>()
            var statement: String? = null
            var anchorWidth = 6
            var statementBase = base
            var insertColumns = false
            var whereAlignment = inheritedWhere
            var selectAlignment: AlignmentGroup? = null
            while (index < end) {
                if (index % 256 == 0) ProgressManager.checkCanceled()
                val token = tokens[index]
                writer.before(token)
                if (token.comment) {
                    if (token.commentLine != null) {
                        writer.line(0)
                        writer.raw(token.commentLine)
                        writer.commentLine(token)
                        writer.line(base)
                        previous = token
                        index++
                        continue
                    }
                    writer.space()
                    writer.raw(token.raw)
                    if (token.raw.startsWith("--")) writer.line(base)
                    previous = token
                    index++
                    continue
                }
                if (query && clause == null && token.keyword("WITH")) {
                    index = with(index, end, base)
                    continue
                }
                val heading = if (query && cases.isEmpty() && previous?.symbol(".") != true) heading(index, end) else null
                if (heading != null) {
                    val (words, count) = heading
                    val firstWord = words.substringBefore(' ')
                    if (statement == null && firstWord in setOf("SELECT", "UPDATE", "INSERT", "MERGE")) {
                        statement = firstWord
                        anchorWidth = maxOf(6, firstWord.length)
                    }
                    if (firstWord == "INSERT") insertColumns = true
                    spaceBefore(token)
                    when {
                        statement == "MERGE" && firstWord == "ON" && clause == "USING" -> writer.space()
                        statement == "INSERT" && firstWord == "VALUES" && previous?.symbol(")") == true -> writer.space()
                        statement == "MERGE" && firstWord in setOf("MERGE", "USING", "WHEN", "THEN", "UPDATE", "INSERT", "VALUES") -> writer.line(statementBase)
                        else -> writer.line(statementBase + anchorWidth - firstWord.length)
                    }
                    if (firstWord == "SELECT") statementBase = writer.column
                    repeat(count) { part ->
                        if (part > 0) { writer.before(tokens[index + part]); writer.space() }
                        writer.raw(tokens[index + part].raw.uppercase())
                    }
                    clause = words.substringBefore(' ')
                    whereAlignment = if (clause == "WHERE" && options.alignWhereEquals) group() else null
                    selectAlignment = if (clause == "SELECT" && options.alignSelectAs) group() else null
                    expressionColumn = writer.column + 1
                    firstExpression = true
                    between = false
                    previous = tokens[index + count - 1]
                    joinNext = false
                    index += count
                    continue
                }
                if (token.symbol(",")) {
                    if (cases.isEmpty() && (vertical != null || query && clause in setOf("SELECT", "FROM", "GROUP", "ORDER", "SET", "VALUES"))) {
                        writer.line(vertical?.minus(2) ?: (expressionColumn - 2))
                    }
                    writer.raw(",")
                    previous = token
                    joinNext = false
                    index++
                    continue
                }
                if (token.keyword("CASE") && previous?.symbol(".") != true) {
                    spaceBefore(token)
                    if (cases.size >= 48) throw SqlFormatFailure("CASE 표현식이 너무 깊어 포맷하지 않았습니다.")
                    cases.addLast(writer.column)
                    writer.raw("CASE")
                    previous = token
                    firstExpression = false
                    index++
                    continue
                }
                if (cases.isNotEmpty() && listOf("WHEN", "THEN", "ELSE", "END").any(token::keyword)) {
                    writer.space()
                    writer.line(cases.last())
                    writer.raw(token.printed)
                    if (token.keyword("END")) cases.removeLast()
                    previous = token
                    joinNext = false
                    index++
                    continue
                }
                if (query && cases.isEmpty() && clause in setOf("WHERE", "ON", "HAVING")) {
                    if (token.keyword("BETWEEN")) between = true
                    if (token.keyword("AND") || token.keyword("OR")) {
                        if (token.keyword("AND") && between) between = false
                        else {
                            writer.space()
                            writer.line(statementBase + anchorWidth - token.raw.length)
                            writer.raw(token.printed)
                            previous = token
                            joinNext = false
                            index++
                            continue
                        }
                    }
                }
                if (token.symbol("(")) {
                    val close = input.pairs[index]
                    val first = (index + 1 until close).firstOrNull { !tokens[it].comment }
                    val subquery = first != null && (tokens[first].keyword("SELECT") || tokens[first].keyword("WITH"))
                    val pivotBlock = query && !subquery && clause in setOf("PIVOT", "UNPIVOT")
                    val pivotIn = pivot && previous?.keyword("IN") == true
                    val list = query && !subquery && (clause == "VALUES" || insertColumns && clause in setOf("INSERT", "INTO") || statement == "MERGE" && clause == "ON")
                    if (subquery && clause == "FROM" && previous?.keyword("FROM") != true && previous?.symbol(",") != true) {
                        writer.space()
                        writer.line(expressionColumn)
                    } else if (statement == "MERGE" && clause == "VALUES") {
                        writer.space()
                        writer.line(base)
                    }
                    else if (pivotBlock || list || previous?.word != true || previous!!.raw.uppercase() in spacedParentheses) writer.space()
                    val openingColumn = writer.column
                    writer.raw("(")
                    previous = token
                    val listColumn = if (list) base + 6 else null
                    val pivotColumn = statementBase + (6 - (clause?.length ?: 0)).coerceAtLeast(0)
                    if (subquery) writer.line(openingColumn + 4)
                    if (list) writer.line(base + 6)
                    if (pivotBlock) writer.line(pivotColumn + 4)
                    if (pivotIn) writer.line(base + 6)
                    range(index + 1, close,
                        when { subquery -> openingColumn + 4; pivotBlock -> pivotColumn + 4; else -> statementBase },
                        subquery,
                        when { pivotBlock -> pivotColumn + 4; pivotIn -> base + 6; else -> listColumn }, pivotBlock,
                        if (subquery || cases.isNotEmpty()) null else whereAlignment)
                    writer.before(tokens[close])
                    if (subquery) writer.line(openingColumn)
                    if (list) writer.line(base)
                    if (pivotBlock) writer.line(pivotColumn)
                    if (pivotIn) writer.line(base)
                    writer.raw(")")
                    previous = tokens[close]
                    joinNext = false
                    firstExpression = false
                    if (list && clause != "VALUES") insertColumns = false
                    index = close + 1
                    continue
                }
                if (token.symbol(";")) {
                    writer.raw(";")
                    writer.line(base)
                    clause = null
                    statement = null
                    whereAlignment = null
                    selectAlignment = null
                    anchorWidth = 6
                    statementBase = base
                    previous = token
                    joinNext = false
                    index++
                    continue
                }
                if (token.symbol(".") || token.symbol("::")) {
                    writer.raw(token.raw)
                    previous = token
                    index++
                    continue
                }
                val unary = token.raw in setOf("-", "+", "~") &&
                    (previous == null || previous!!.symbol("(") || previous!!.symbol(",") ||
                        previous!!.type == SqlTokens.SYMBOL || previous!!.keyword("SELECT") || previous!!.keyword("THEN") || previous!!.keyword("ELSE"))
                spaceBefore(token)
                val alignment = if (cases.isEmpty() && token.symbol("=")) whereAlignment
                    else if (cases.isEmpty() && token.keyword("AS") && previous?.symbol(".") != true) selectAlignment else null
                if (alignment != null && alignment.rows.add(writer.row)) alignment.anchors.add(Anchor(index, writer.column))
                padding[index]?.let { writer.raw(" ".repeat(it)) }
                val sourceName = clause in setOf("FROM", "JOIN", "LEFT", "RIGHT", "FULL", "INNER", "CROSS", "NATURAL") && !token.keyword("AS")
                writer.raw(if (token.word && (sourceName || previous?.keyword("AS") == true || previous?.symbol(".") == true)) token.raw else token.printed)
                previous = token
                joinNext = unary
                if (query && clause == "SELECT" && firstExpression && (token.keyword("DISTINCT") || token.keyword("ALL"))) {
                    expressionColumn = writer.column + 1
                } else firstExpression = false
                index++
            }
            if (cases.isNotEmpty()) throw SqlFormatFailure("닫히지 않은 CASE 표현식을 확인하세요.")
        }

        private fun spaceBefore(token: SqlFormattingToken) {
            val prev = previous
            val literalPrefix = token.type == SqlTokens.QUOTED && token.raw.startsWith("'") && prev?.word == true &&
                prev.end == token.start && (prev.raw.uppercase() in setOf("N", "E", "X", "B") || prev.raw.startsWith("_"))
            val linkedName = token.raw.startsWith("@") && token.type == SqlTokens.PARAMETER && prev?.word == true &&
                prev.end == token.start && prev.raw.uppercase() !in spacedParentheses
            if (!joinNext && !literalPrefix && !linkedName && prev?.symbol(".") != true && prev?.symbol("::") != true && prev?.symbol("(") != true) writer.space()
            joinNext = false
        }

        private fun heading(index: Int, end: Int): Pair<String, Int>? {
            val token = tokens[index]
            fun next(word: String, at: Int = index + 1) = at < end && tokens[at].keyword(word)
            if ((token.keyword("INSERT") || token.keyword("MERGE")) && next("INTO")) return "${token.raw.uppercase()} INTO" to 2
            for (word in listOf("SELECT", "FROM", "WHERE", "HAVING", "ON", "JOIN", "UPDATE", "INSERT", "MERGE", "INTO", "USING", "SET", "VALUES", "PIVOT", "UNPIVOT")) {
                if (token.keyword(word)) return word to 1
            }
            if (token.keyword("WHEN")) {
                if (next("MATCHED")) return if (next("THEN", index + 2)) "WHEN MATCHED THEN" to 3 else "WHEN MATCHED" to 2
                if (next("NOT") && next("MATCHED", index + 2)) return if (next("THEN", index + 3)) "WHEN NOT MATCHED THEN" to 4 else "WHEN NOT MATCHED" to 3
            }
            if (token.keyword("THEN")) return "THEN" to 1
            for (word in listOf("GROUP", "ORDER", "CONNECT")) {
                if (token.keyword(word) && next("BY")) return "$word BY" to 2
            }
            for (word in listOf("LEFT", "RIGHT", "FULL", "INNER", "CROSS", "NATURAL")) {
                if (!token.keyword(word)) continue
                if (next("JOIN")) return "$word JOIN" to 2
                if (next("OUTER") && next("JOIN", index + 2)) return "$word OUTER JOIN" to 3
            }
            for (word in listOf("UNION", "INTERSECT", "EXCEPT")) {
                if (token.keyword(word)) return when {
                    next("ALL") -> "$word ALL" to 2
                    next("DISTINCT") -> "$word DISTINCT" to 2
                    else -> word to 1
                }
            }
            return null
        }

        private fun with(start: Int, end: Int, base: Int): Int {
            var header = start
            writer.line(base)
            while (header < end) {
                var index = header
                var previousWord: SqlFormattingToken? = null
                var body = -1
                while (index < end) {
                    val token = tokens[index]
                    if (token.symbol("(")) {
                        if (previousWord?.keyword("AS") == true || previousWord?.keyword("MATERIALIZED") == true) {
                            body = index
                            break
                        }
                        index = input.pairs[index] + 1
                    } else {
                        if (!token.comment) previousWord = token
                        index++
                    }
                }
                if (body < 0) throw SqlFormatFailure("WITH 본문의 AS와 괄호를 확인하세요.")
                range(header, body, base, false)
                writer.before(tokens[body])
                writer.space()
                writer.raw("(")
                previous = tokens[body]
                writer.line(base + 4)
                val close = input.pairs[body]
                range(body + 1, close, base + 4, true)
                writer.before(tokens[close])
                writer.line(base)
                writer.raw(")")
                previous = tokens[close]
                index = close + 1
                while (index < end && tokens[index].comment) {
                    val comment = tokens[index]
                    writer.before(comment)
                    if (comment.commentLine != null) writer.line(0) else writer.space()
                    writer.raw(comment.commentLine ?: comment.raw)
                    if (comment.commentLine != null) writer.commentLine(comment)
                    if (comment.commentLine != null || comment.raw.startsWith("--")) writer.line(base)
                    previous = tokens[index++]
                }
                if (index >= end || !tokens[index].symbol(",")) return index
                writer.before(tokens[index])
                writer.line(base)
                writer.raw(",")
                previous = tokens[index]
                header = index + 1
            }
            return end
        }
    }
}
