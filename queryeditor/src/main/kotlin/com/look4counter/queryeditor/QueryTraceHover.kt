package com.look4counter.queryeditor

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.event.EditorMouseEvent
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.editor.markup.HighlighterLayer
import com.intellij.openapi.editor.markup.HighlighterTargetArea
import com.intellij.openapi.editor.markup.RangeHighlighter
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.util.Alarm
import com.intellij.util.concurrency.AppExecutorUtil
import com.look4counter.queryeditor.database.DatabaseMetadataService
import com.look4counter.queryeditor.sql.SqlTrace
import java.awt.Cursor
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import java.awt.event.FocusAdapter
import java.awt.event.FocusEvent

internal class QueryTraceHover(private val project: Project, private val editor: EditorEx, private val lifetime: Disposable) {
    private val alarm = Alarm(Alarm.ThreadToUse.SWING_THREAD, lifetime)
    private var revision = 0L
    private var offset: Int? = null
    private var control = false
    private var highlight: RangeHighlighter? = null
    private var requested: Int? = null

    init {
        editor.addEditorMouseMotionListener(object : EditorMouseMotionListener {
            override fun mouseMoved(event: EditorMouseEvent) {
                offset = event.offset.takeIf { event.area == EditorMouseEventArea.EDITING_AREA && event.isOverText }
                control = event.mouseEvent.isControlDown && !event.mouseEvent.isAltDown && !event.mouseEvent.isShiftDown
                update()
            }
            override fun mouseDragged(event: EditorMouseEvent) { offset = null; clear() }
        }, lifetime)
        editor.addEditorMouseListener(object : EditorMouseListener {
            override fun mouseExited(event: EditorMouseEvent) { offset = null; clear() }
        }, lifetime)
        val keys = object : KeyAdapter() {
            override fun keyPressed(event: KeyEvent) {
                if (event.keyCode == KeyEvent.VK_CONTROL) { control = true; update() }
                else if (event.isAltDown || event.isShiftDown) { control = false; clear() }
            }
            override fun keyReleased(event: KeyEvent) {
                if (!event.isControlDown || event.keyCode == KeyEvent.VK_CONTROL) { control = false; clear() }
            }
        }
        val focus = object : FocusAdapter() {
            override fun focusLost(event: FocusEvent) { control = false; clear() }
        }
        editor.contentComponent.addKeyListener(keys)
        editor.contentComponent.addFocusListener(focus)
        editor.document.addDocumentListener(object : DocumentListener {
            override fun documentChanged(event: DocumentEvent) { offset = null; clear() }
        }, lifetime)
        Disposer.register(lifetime) {
            clear()
            editor.contentComponent.removeKeyListener(keys)
            editor.contentComponent.removeFocusListener(focus)
        }
    }

    fun clear() {
        ++revision
        alarm.cancelAllRequests()
        requested = null
        highlight?.let { if (it.isValid) it.dispose() }
        highlight = null
        if (!editor.isDisposed) editor.setCustomCursor(this, null)
    }

    private fun update() {
        val position = offset
        if (!control || position == null || editor.isDisposed || project.isDisposed || Disposer.isDisposed(lifetime)) {
            clear()
            return
        }
        if (highlight?.let { it.isValid && position >= it.startOffset && position < it.endOffset } == true || requested == position) return
        clear()
        requested = position
        val request = revision
        val document = editor.document
        val stamp = document.modificationStamp
        alarm.addRequest({
            if (editor.isDisposed || Disposer.isDisposed(lifetime) || revision != request) return@addRequest
            val text = document.text
            val metadata = DatabaseMetadataService.getInstance(project).snapshot()
            AppExecutorUtil.getAppExecutorService().execute {
                if (project.isDisposed || Disposer.isDisposed(lifetime)) return@execute
                val link = SqlTrace.sourceLink(text, position, metadata)
                ApplicationManager.getApplication().invokeLater({
                    if (editor.isDisposed || project.isDisposed || Disposer.isDisposed(lifetime) || revision != request ||
                        !control || offset != position || editor.document !== document || document.modificationStamp != stamp) return@invokeLater
                    if (link != null) {
                        highlight = editor.markupModel.addRangeHighlighter(CodeInsightColors.HYPERLINK_ATTRIBUTES,
                            link.range.startOffset, link.range.endOffset, HighlighterLayer.HYPERLINK, HighlighterTargetArea.EXACT_RANGE)
                        editor.setCustomCursor(this, Cursor.getPredefinedCursor(Cursor.HAND_CURSOR))
                    }
                }, ModalityState.nonModal())
            }
        }, 75)
    }
}
