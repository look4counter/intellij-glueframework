package com.look4counter.queryeditor.sql

import com.intellij.lexer.Lexer
import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase
import com.intellij.psi.tree.IElementType
import com.intellij.ui.JBColor
import java.awt.Font

internal class SqlSyntaxHighlighter : SyntaxHighlighterBase() {
    override fun getHighlightingLexer(): Lexer = SqlLexer()

    override fun getTokenHighlights(tokenType: IElementType): Array<TextAttributesKey> = when (tokenType) {
        SqlTokens.KEYWORD -> arrayOf(KEYWORD)
        SqlTokens.NUMBER -> arrayOf(NUMBER)
        SqlTokens.QUOTED -> arrayOf(QUOTED)
        SqlTokens.PARAMETER -> arrayOf(PARAMETER)
        SqlTokens.COMMENT -> arrayOf(DefaultLanguageHighlighterColors.LINE_COMMENT)
        else -> emptyArray()
    }

    companion object {
        private fun color(name: String, light: Int, dark: Int): TextAttributesKey = TextAttributesKey.createTextAttributesKey(
            name, TextAttributes(JBColor(light, dark), null, null, null, Font.PLAIN),
        )
        private val KEYWORD = color("GLUE_QUERY_SQL_KEYWORD", 0xC46600, 0xFFA657)
        private val NUMBER = color("GLUE_QUERY_SQL_NUMBER", 0x175BC1, 0x6CA8F5)
        private val QUOTED = color("GLUE_QUERY_SQL_QUOTED", 0x26823B, 0x78C78B)
        private val PARAMETER = color("GLUE_QUERY_SQL_PARAMETER", 0x008B9A, 0x56D4DD)
    }
}
