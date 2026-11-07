package com.look4counter.queryeditor.sql

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.ex.util.EditorUtil
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil

internal object SqlColumnErrorHighlighting {
    fun install(editor: EditorEx) {
        val lifetime = Disposer.newDisposable("Glue SQL column diagnostics")
        EditorUtil.disposeWithEditor(editor, lifetime)
        val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, lifetime)
        val highlights = mutableListOf<RangeHighlighter>()
        var generation = 0L
        fun clear() {
            highlights.forEach { if (it.isValid) editor.markupModel.removeHighlighter(it) }
            highlights.clear()
        }
        fun update() {
            if (editor.isDisposed || Disposer.isDisposed(lifetime)) return
            clear()
            alarm.cancelAllRequests()
            val revision = ++generation
            alarm.addRequest({
                if (editor.isDisposed || Disposer.isDisposed(lifetime) || revision != generation) return@addRequest
                val document = editor.document
                val stamp = document.modificationStamp
                val text = document.text
                AppExecutorUtil.getAppExecutorService().execute {
                    if (editor.isDisposed || Disposer.isDisposed(lifetime)) return@execute
                    val problems = SqlColumnValidator.analyze(text)
                    ApplicationManager.getApplication().invokeLater({
                        if (editor.isDisposed || Disposer.isDisposed(lifetime) || revision != generation ||
                            editor.document !== document || document.modificationStamp != stamp) return@invokeLater
                        problems.forEach { problem ->
                            val highlight = editor.markupModel.addRangeHighlighter(CodeInsightColors.ERRORS_ATTRIBUTES,
                                problem.start, problem.end, HighlighterLayer.ERROR, HighlighterTargetArea.EXACT_RANGE)
                            highlight.errorStripeTooltip = problem.message
                            highlight.setErrorStripeMarkColor(editor.colorsScheme.getAttributes(CodeInsightColors.ERRORS_ATTRIBUTES)?.errorStripeColor)
                            highlights.add(highlight)
                        }
                    }, ModalityState.nonModal())
                }
            }, 200)
        }
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) = update()
        }, lifetime)
        Disposer.register(lifetime) { ++generation; clear() }
        update()
    }
}
