package com.look4counter.queryeditor.sql

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType

internal class SqlFormatFailure(message: String) : RuntimeException(message, null, false, false)

internal data class SqlFormattingToken(val raw: String, val start: Int, val end: Int, val type: IElementType, val commentLine: String? = null) {
    val word: Boolean get() = type == SqlTokens.IDENTIFIER || type == SqlTokens.KEYWORD
    val printed: String get() = raw
    val comment: Boolean get() = type == SqlTokens.COMMENT
    fun keyword(value: String) = word && raw.equals(value, ignoreCase = true)
    fun symbol(value: String) = type == SqlTokens.SYMBOL && raw == value
}

internal class SqlFormattingTokens(text: String) {
    val tokens: List<SqlFormattingToken>
    val pairs: IntArray

    init {
        if (text.length > 262_144) throw SqlFormatFailure("SQL이 너무 길어 포맷하지 않았습니다.")
        val result = mutableListOf<SqlFormattingToken>()
        val lexer = SqlLexer()
        lexer.start(text)
        while (lexer.tokenType != null) {
            if (result.size % 256 == 0) ProgressManager.checkCanceled()
            val start = lexer.tokenStart
            var end = lexer.tokenEnd
            var type = lexer.tokenType!!
            if (type == TokenType.WHITE_SPACE) { lexer.advance(); continue }
            val first = text[start]
            val parameter = parameterEnd(text, start)
            if (parameter != null) {
                end = parameter
                type = SqlTokens.PARAMETER
            } else if (type == SqlTokens.QUOTED || type == SqlTokens.SYMBOL && first in "`[") {
                val closing = if (first == '[') ']' else first
                end = quoteEnd(text, start, closing)
                type = SqlTokens.QUOTED
            } else if (type == SqlTokens.COMMENT && text.startsWith("/*", start)) {
                var depth = 0
                var index = start
                while (index < end) {
                    when {
                        text.startsWith("/*", index) -> { depth++; index += 2 }
                        text.startsWith("*/", index) -> { depth--; index += 2 }
                        else -> index++
                    }
                }
                if (depth != 0) throw SqlFormatFailure("닫히지 않은 블록 주석을 확인하세요.")
            } else if ((type == SqlTokens.SYMBOL || first == '?') && first in "+-*/%=<>!~^|&@#?:") {
                var operatorEnd = start
                while (operatorEnd < text.length && text[operatorEnd] in "+-*/%=<>!~^|&@#?:") {
                    if (operatorEnd > start && (text.startsWith("--", operatorEnd) || text.startsWith("/*", operatorEnd) ||
                            parameterEnd(text, operatorEnd) != null)) break
                    operatorEnd++
                }
                end = maxOf(end, operatorEnd)
                type = SqlTokens.SYMBOL
            }
            val lineStart = text.lastIndexOf('\n', start - 1) + 1
            val lineEnd = text.indexOf('\n', end).takeIf { it >= 0 } ?: text.length
            val commentLine = if (type == SqlTokens.COMMENT && text.substring(lineStart, start).isBlank() &&
                text.substring(end, lineEnd).isBlank()) text.substring(lineStart, lineEnd).removeSuffix("\r") else null
            result.add(SqlFormattingToken(text.substring(start, end), start, end, type, commentLine))
            if (result.size > 20_000) throw SqlFormatFailure("SQL 토큰이 너무 많아 포맷하지 않았습니다.")
            lexer.start(text, end, text.length, 0)
        }
        tokens = result
        pairs = IntArray(tokens.size) { -1 }
        val stack = ArrayDeque<Int>()
        tokens.forEachIndexed { index, token ->
            if (token.symbol("(")) {
                stack.addLast(index)
                if (stack.size > 48) throw SqlFormatFailure("SQL 괄호가 너무 깊어 포맷하지 않았습니다.")
            } else if (token.symbol(")")) {
                if (stack.isEmpty()) throw SqlFormatFailure("여는 괄호가 없는 닫는 괄호를 확인하세요.")
                pairs[stack.removeLast()] = index
            }
        }
        if (stack.isNotEmpty()) throw SqlFormatFailure("닫히지 않은 괄호를 확인하세요.")
    }

    private fun parameterEnd(text: String, start: Int): Int? {
        if (text[start] == '?') return if (text.getOrNull(start + 1) in listOf('|', '&')) null else start + 1
        if (text[start] != ':' && text[start] != '@') return null
        var index = start + 1
        if (text[start] == '@') while (index < text.length && text[index] == '@') index++
        if (index >= text.length || !SqlLexer.isWord(text[index])) return null
        while (index < text.length && SqlLexer.isWord(text[index])) index++
        return index
    }

    private fun quoteEnd(text: String, start: Int, closing: Char): Int {
        var index = start + 1
        while (index < text.length) {
            val char = text[index++]
            if (char == '\\' && closing != ']' && index < text.length) index++
            else if (char == closing) {
                if (index < text.length && text[index] == closing) index++ else return index
            }
        }
        throw SqlFormatFailure("닫히지 않은 문자열 또는 인용 식별자를 확인하세요.")
    }
}
