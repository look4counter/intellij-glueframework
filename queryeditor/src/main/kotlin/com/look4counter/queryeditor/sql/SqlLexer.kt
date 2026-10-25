package com.look4counter.queryeditor.sql

import com.intellij.lang.Language
import com.intellij.lexer.LexerBase
import com.intellij.psi.TokenType
import com.intellij.psi.tree.IElementType
import java.util.Locale

internal object SqlTokens {
    val KEYWORD = IElementType("GLUE_SQL_KEYWORD", Language.ANY)
    val IDENTIFIER = IElementType("GLUE_SQL_IDENTIFIER", Language.ANY)
    val PARAMETER = IElementType("GLUE_SQL_PARAMETER", Language.ANY)
    val NUMBER = IElementType("GLUE_SQL_NUMBER", Language.ANY)
    val QUOTED = IElementType("GLUE_SQL_QUOTED", Language.ANY)
    val COMMENT = IElementType("GLUE_SQL_COMMENT", Language.ANY)
    val SYMBOL = IElementType("GLUE_SQL_SYMBOL", Language.ANY)
}

internal class SqlLexer : LexerBase() {
    private var buffer: CharSequence = ""
    private var limit = 0
    private var start = 0
    private var end = 0
    private var type: IElementType? = null

    override fun start(buffer: CharSequence, startOffset: Int, endOffset: Int, initialState: Int) {
        this.buffer = buffer
        limit = endOffset
        start = startOffset
        locate()
    }

    override fun getState(): Int = 0
    override fun getTokenType(): IElementType? = type
    override fun getTokenStart(): Int = start
    override fun getTokenEnd(): Int = end
    override fun getBufferSequence(): CharSequence = buffer
    override fun getBufferEnd(): Int = limit

    override fun advance() {
        start = end
        locate()
    }

    private fun locate() {
        end = start
        if (start >= limit) {
            type = null
            return
        }
        val char = buffer[end++]
        type = when {
            char.isWhitespace() -> {
                while (end < limit && buffer[end].isWhitespace()) end++
                TokenType.WHITE_SPACE
            }
            char == '-' && next('-') -> {
                while (end < limit && buffer[end] != '\n') end++
                SqlTokens.COMMENT
            }
            char == '/' && next('*') -> {
                end++
                var depth = 1
                while (end < limit && depth > 0) {
                    when {
                        buffer[end] == '/' && end + 1 < limit && buffer[end + 1] == '*' -> { depth++; end += 2 }
                        buffer[end] == '*' && end + 1 < limit && buffer[end + 1] == '/' -> { depth--; end += 2 }
                        else -> end++
                    }
                }
                SqlTokens.COMMENT
            }
            char == '\'' || char == '"' -> {
                while (end < limit) {
                    val current = buffer[end++]
                    if (current == '\\' && end < limit) end++
                    else if (current == char) {
                        if (end < limit && buffer[end] == char) end++ else break
                    }
                }
                SqlTokens.QUOTED
            }
            char.isDigit() || (char == '.' && end < limit && buffer[end].isDigit()) -> {
                val match = NUMBER.find(buffer.subSequence(0, limit), start)
                end = match?.takeIf { it.range.first == start }?.range?.last?.plus(1) ?: end
                SqlTokens.NUMBER
            }
            char == ':' && next(':') -> {
                end++
                SqlTokens.SYMBOL
            }
            char == ':' && end < limit && (buffer[end].isLetter() || buffer[end] == '_' || buffer[end] == '$') -> {
                while (end < limit && isWord(buffer[end])) end++
                SqlTokens.PARAMETER
            }
            char == '?' -> SqlTokens.PARAMETER
            isWord(char) -> {
                while (end < limit && isWord(buffer[end])) end++
                val compound = SqlKeywords.hyphenated.firstOrNull {
                    start + it.length <= limit && buffer.startsWith(it, start, ignoreCase = true) &&
                        (start + it.length == limit || !isWord(buffer[start + it.length]))
                }
                if (compound != null) end = start + compound.length
                if (buffer.subSequence(start, end).toString().uppercase(Locale.ROOT) in SqlKeywords.words) {
                    SqlTokens.KEYWORD
                } else SqlTokens.IDENTIFIER
            }
            else -> SqlTokens.SYMBOL
        }
    }

    private fun next(char: Char): Boolean = end < limit && buffer[end] == char

    companion object {
        private val NUMBER = Regex("(?:0[xX][0-9a-fA-F]+|(?:[0-9]+(?:\\.[0-9]*)?|\\.[0-9]+)(?:[eE][+-]?[0-9]+)?)")
        fun isWord(char: Char): Boolean = char.isLetterOrDigit() || char == '_' || char == '$'

        fun completionAllowed(text: String, offset: Int): Boolean {
            val lexer = SqlLexer()
            lexer.start(text)
            while (lexer.tokenType != null) {
                if (lexer.tokenStart < offset && offset <= lexer.tokenEnd) {
                    return lexer.tokenType !in setOf(SqlTokens.QUOTED, SqlTokens.COMMENT, SqlTokens.NUMBER, SqlTokens.PARAMETER)
                }
                lexer.advance()
            }
            return true
        }
    }
}
