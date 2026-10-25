package com.look4counter.queryeditor.sql

import com.intellij.codeInsight.daemon.impl.ParameterHintsPresentationManager
import com.intellij.openapi.editor.Inlay
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.util.Disposer

internal object SqlParameterHints {
    fun install(editor: EditorEx) {
        val lifetime = Disposer.newDisposable("Glue SQL parameter hints")
        EditorUtil.disposeWithEditor(editor, lifetime)
        val hints = mutableListOf<Inlay<*>>()
        fun clear() {
            hints.filter { it.isValid }.forEach { Disposer.dispose(it) }
            hints.clear()
        }
        fun update() {
            if (editor.isDisposed) return
            val text = editor.document.charsSequence
            val lexer = SqlLexer().apply { start(text) }
            val presentation = ParameterHintsPresentationManager.getInstance()
            editor.inlayModel.execute(true) {
                clear()
                var index = 0
                while (lexer.tokenType != null) {
                    if (lexer.tokenType == SqlTokens.PARAMETER && lexer.tokenEnd == lexer.tokenStart + 1 &&
                        text[lexer.tokenStart] == '?') {
                        presentation.addHint(editor, lexer.tokenStart, false, "${index++}:", null, false)
                            ?.let { hints.add(it) }
                    }
                    lexer.advance()
                }
            }
        }
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = update()
        }, lifetime)
        Disposer.register(lifetime) { clear() }
        update()
    }
}
