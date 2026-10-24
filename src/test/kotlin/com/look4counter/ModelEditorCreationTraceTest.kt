package com.look4counter

import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.TestLoggerFactory
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import com.look4counter.modeleditor.*
import java.awt.event.MouseEvent
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.swing.JScrollPane

class ModelEditorCreationTraceTest : BasePlatformTestCase() {
    private fun event(canvas: ModelDiagramCanvas, id: Int = MouseEvent.MOUSE_CLICKED, count: Int = 1) =
        MouseEvent(canvas, id, 1234, 0, 300, 200, count, false, MouseEvent.BUTTON1)

    fun testCanvasReportsClickCountRejectionAndCorrelatedSubmission() {
        val canvas = ModelDiagramCanvas().apply { setSize(600, 400); setCreationKind(DiagramNodeKind.Activity) }
        val lines = mutableListOf<String>()
        canvas.onCreationTrace = { lines += it }
        var submitted = 0
        canvas.onChildCreateRequested = { _, _ -> submitted++ }
        val rejected = event(canvas, count = 2)
        canvas.dispatchEvent(rejected)
        val id = ChildCreationTrace.inputId(rejected)
        assertTrue(lines.any { it.startsWith("CANVAS_CLICK") && it.contains("input=$id") && it.contains("count=2") })
        assertTrue(lines.any { it == "IGNORE input=$id reason=click_count count=2" })
        assertEquals(0, submitted)
        val accepted = event(canvas)
        canvas.dispatchEvent(accepted)
        assertTrue(lines.any { it.startsWith("SUBMIT input=${ChildCreationTrace.inputId(accepted)} ") })
        assertEquals(1, submitted)
        assertTrue(canvas.diagram.nodes.isEmpty())
    }

    fun testSmallPressReleaseCreatesWhenClickEventIsMissingAndLargeMoveDoesNot() {
        val canvas = ModelDiagramCanvas().apply { setSize(600, 400); setCreationKind(DiagramNodeKind.Activity) }
        var submitted = 0
        canvas.onChildCreateRequested = { _, _ -> submitted++ }
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 1000, 0, 300, 200, 1, false, MouseEvent.BUTTON1))
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 1010, 0, 301, 201, 1, false, MouseEvent.BUTTON1))
        assertEquals(1, submitted)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_CLICKED, 1010, 0, 301, 201, 1, false, MouseEvent.BUTTON1))
        assertEquals(1, submitted)
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_PRESSED, 2000, 0, 300, 200, 1, false, MouseEvent.BUTTON1))
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_RELEASED, 2010, 0, 320, 220, 1, false, MouseEvent.BUTTON1))
        assertEquals(1, submitted)
    }

    fun testRawObservationDoesNotConsumeAndOnlyRecordsCreationPointerEvents() {
        val canvas = ModelDiagramCanvas().apply { setSize(600, 400); setCreationKind(DiagramNodeKind.Activity) }
        val lines = mutableListOf<String>()
        val trace = ChildCreationTrace({ "test-state" }, { lines += it })
        val input = event(canvas, MouseEvent.MOUSE_PRESSED)
        trace.observeRaw(canvas, input)
        assertFalse(input.isConsumed)
        assertEquals(1, lines.size)
        assertTrue(lines.single().contains("stage=RAW_PRESS input=${ChildCreationTrace.inputId(input)}"))
        assertTrue(lines.single().endsWith("test-state"))
        trace.observeRaw(canvas, event(canvas, MouseEvent.MOUSE_MOVED))
        trace.observeRaw(canvas, MouseEvent(canvas, MouseEvent.MOUSE_CLICKED, 1235, 0, 900, 900, 1, false, MouseEvent.BUTTON1))
        canvas.setCreationKind(null)
        trace.observeRaw(canvas, event(canvas))
        assertEquals(1, lines.size)
    }

    fun testQueueLogsSameRequestThroughCompletionAndModeCancellation() {
        val file = myFixture.addFileToProject("trace-run.glue_uml_ad", "<GlueDiagram/>").virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = editor.component.components.filterIsInstance<JScrollPane>().single().viewport.view as ModelDiagramCanvas
            canvas.setSize(600, 400)
            canvas.setCreationKind(DiagramNodeKind.Activity)
            val first = event(canvas)
            canvas.dispatchEvent(first)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(1, canvas.diagram.nodes.size)
            canvas.dispatchEvent(event(canvas))
            canvas.setCreationKind(null)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(2, canvas.diagram.nodes.size)
            val bytes = ByteArrayOutputStream()
            PrintStream(bytes, true, Charsets.UTF_8).use { TestLoggerFactory.dumpLogTo("creation trace", it) }
            val text = bytes.toString(Charsets.UTF_8)
            val lines = text.lineSequence().filter { it.contains("[ModelChildCreation]") && it.contains("file=trace-run.glue_uml_ad") }.toList()
            assertTrue(text, lines.isNotEmpty())
            assertFalse(lines.joinToString("\n"), lines.any { it.contains("DRAIN_") || it.contains("QUEUE_") })
        } finally { Disposer.dispose(editor) }
    }
    fun testRawObserverIsRemovedWithOwner() {
        val canvas = ModelDiagramCanvas().apply { setSize(600, 400); setCreationKind(DiagramNodeKind.Activity) }
        val lines = mutableListOf<String>()
        val trace = ChildCreationTrace({ "test-state" }, { lines += it })
        val owner = Disposer.newDisposable()
        try {
            trace.install(canvas, owner)
            com.intellij.ide.IdeEventQueue.getInstance().dispatchEvent(event(canvas, MouseEvent.MOUSE_PRESSED))
            assertEquals(1, lines.count { it.contains("stage=RAW_PRESS") })
            Disposer.dispose(owner)
            com.intellij.ide.IdeEventQueue.getInstance().dispatchEvent(event(canvas, MouseEvent.MOUSE_PRESSED))
            assertEquals(1, lines.count { it.contains("stage=RAW_PRESS") })
        } finally { if (!Disposer.isDisposed(owner)) Disposer.dispose(owner) }
    }
}
