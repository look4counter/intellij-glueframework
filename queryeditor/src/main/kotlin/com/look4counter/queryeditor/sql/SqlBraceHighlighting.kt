package com.look4counter.queryeditor.sql

import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.event.CaretEvent
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Disposer

internal object SqlBraceHighlighting {
    fun install(editor: EditorEx) {
        val lifetime = Disposer.newDisposable("Glue SQL brace highlighting")
        EditorUtil.disposeWithEditor(editor, lifetime)
        val highlights = mutableListOf<RangeHighlighter>()
        fun clear() {
            highlights.forEach { editor.markupModel.removeHighlighter(it) }
            highlights.clear()
        }
        fun update() {
            if (editor.isDisposed) return
            clear()
            val text = editor.document.charsSequence
            val caret = editor.caretModel.offset
            val lexer = SqlLexer().apply { start(text) }
            val stack = ArrayDeque<Pair<Char, Int>>()
            val pairs = mutableMapOf<Int, Int>()
            val braces = mutableSetOf<Int>()
            while (lexer.tokenType != null) {
                if (lexer.tokenType == SqlTokens.SYMBOL && lexer.tokenEnd == lexer.tokenStart + 1) {
                    val offset = lexer.tokenStart
                    val char = text[offset]
                    when (char) {
                        '(', '[', '{' -> { braces.add(offset); stack.addLast(char to offset) }
                        ')', ']', '}' -> {
                            braces.add(offset)
                            val expected = when (char) { ')' -> '('; ']' -> '['; else -> '{' }
                            if (stack.lastOrNull()?.first == expected) {
                                val start = stack.removeLast().second
                                pairs[start] = offset
                                pairs[offset] = start
                            } else stack.clear()
                        }
                    }
                }
                lexer.advance()
            }
            val at = when { caret in braces -> caret; caret - 1 in braces -> caret - 1; else -> return }
            val match = pairs[at] ?: return
            for (offset in listOf(at, match)) {
                val highlight = editor.markupModel.addRangeHighlighter(CodeInsightColors.MATCHED_BRACE_ATTRIBUTES,
                    offset, offset + 1, HighlighterLayer.ADDITIONAL_SYNTAX, HighlighterTargetArea.EXACT_RANGE)
                highlights.add(highlight)
            }
        }
        editor.caretModel.addCaretListener(object : CaretListener {
            override fun caretPositionChanged(event: CaretEvent) = update()
        }, lifetime)
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = update()
        }, lifetime)
        Disposer.register(lifetime) { clear() }
        update()
    }
}
