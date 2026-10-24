package com.look4counter.modeleditor

import com.intellij.ide.IdeEventQueue
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import java.awt.Point
import java.awt.event.MouseEvent
import java.util.concurrent.atomic.AtomicLong
import javax.swing.SwingUtilities

class ChildCreationTrace(private val state: () -> String,
                         private val write: (String) -> Unit = { logger.info(it) }) {
    private val session = sessions.incrementAndGet()
    fun record(stage: String, detail: String = "") {
        write("[ModelChildCreation] session=$session stage=$stage $detail ${state()}")
    }
    fun line(message: String) = record(message.substringBefore(' '), message.substringAfter(' ', ""))
    fun install(canvas: ModelDiagramCanvas, owner: Disposable) {
        IdeEventQueue.getInstance().addPreprocessor({ event ->
            if (event is MouseEvent) observeRaw(canvas, event)
            false
        }, owner)
        record("SESSION_START")
    }
    fun observeRaw(canvas: ModelDiagramCanvas, event: MouseEvent) {
        if (canvas.creationKind == null) return
        if (event.id != MouseEvent.MOUSE_PRESSED && event.id != MouseEvent.MOUSE_RELEASED && event.id != MouseEvent.MOUSE_CLICKED) return
        val point = when {
            event.component === canvas -> event.point
            canvas.isShowing && event.component?.isShowing == true -> Point(event.xOnScreen, event.yOnScreen).also {
                SwingUtilities.convertPointFromScreen(it, canvas)
            }
            else -> return
        }
        if (!canvas.visibleRect.contains(point)) return
        record("RAW_${eventName(event)}", "${mouseDetails(event)} canvasX=${point.x} canvasY=${point.y}")
    }
    companion object {
        private val logger = Logger.getInstance(ChildCreationTrace::class.java)
        private val sessions = AtomicLong()
        fun inputId(event: MouseEvent) = "${event.`when`}-${System.identityHashCode(event).toString(16)}"
        fun eventName(event: MouseEvent) = when (event.id) {
            MouseEvent.MOUSE_PRESSED -> "PRESS"
            MouseEvent.MOUSE_RELEASED -> "RELEASE"
            MouseEvent.MOUSE_CLICKED -> "CLICK"
            else -> event.id.toString()
        }
        fun mouseDetails(event: MouseEvent) = "input=${inputId(event)} when=${event.`when`} count=${event.clickCount} button=${event.button} " +
            "x=${event.x} y=${event.y} consumed=${event.isConsumed} source=${event.component?.javaClass?.name}"
    }
}
