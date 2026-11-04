package com.look4counter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.ApplicationImpl
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import com.look4counter.modeleditor.*
import java.awt.event.MouseEvent
import javax.swing.JScrollPane

class ModelActivityCreationTest : BasePlatformTestCase() {
    private val xml = """<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <child xsi:type="Activity" id="1" name="A" location="20,40" size="100,-1"/>
        <child xsi:type="Activity" id="2" name="B" location="220,140" size="100,-1"/>
        </GlueDiagram>"""
    private var sequence = 0
    private fun send(canvas: ModelDiagramCanvas, type: Int, x: Int, y: Int, alt: Boolean = false) {
        val button = if (type == MouseEvent.MOUSE_MOVED || type == MouseEvent.MOUSE_DRAGGED) MouseEvent.NOBUTTON else MouseEvent.BUTTON1
        val modifiers = (if (alt) MouseEvent.ALT_DOWN_MASK else 0) or
            (if (type == MouseEvent.MOUSE_DRAGGED || type == MouseEvent.MOUSE_PRESSED) MouseEvent.BUTTON1_DOWN_MASK else 0)
        canvas.dispatchEvent(MouseEvent(canvas, type, System.currentTimeMillis(), modifiers, x, y, 1, false, button))
    }
    private fun unlocked(action: () -> Unit) {
        val app = ApplicationManager.getApplication() as ApplicationImpl
        app.threadingSupport.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack {
            assertFalse(app.isWriteIntentLockAcquired)
            action()
        }
    }
    private fun withEditor(text: String = xml, action: (ModelDiagramCanvas, TextEditor) -> Unit) {
        val file = myFixture.addFileToProject("creation-${sequence++}.glue_uml_ad", text).virtualFile
        val design = ModelDesignEditorProvider().createEditor(project, file)
        val source = ModelSourceEditorProvider().createEditor(project, file) as TextEditor
        try {
            EditorTestUtil.waitForLoading(source.editor)
            UIUtil.dispatchAllInvocationEvents()
            val canvas = design.component.components.filterIsInstance<JScrollPane>().single().viewport.view as ModelDiagramCanvas
            canvas.setSize(600, 400)
            canvas.snapEnabled = true
            canvas.setCreationKind(DiagramNodeKind.Activity)
            action(canvas, source)
        } finally { Disposer.dispose(design); Disposer.dispose(source) }
    }

    fun testUnlockedClickCreatesSnappedActivityAndSupportsSingleUndoRedo() = withEditor { canvas, source ->
        val original = source.editor.document.text
        send(canvas, MouseEvent.MOUSE_MOVED, 167, 153)
        val ghost = canvas.ghostNode!!
        assertEquals(DiagramPoint(120.0, 140.0), ghost.location)
        unlocked { send(canvas, MouseEvent.MOUSE_CLICKED, 167, 153) }
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(3, canvas.diagram.nodes.size)
        assertEquals(ghost.location, canvas.selectedNodes.single().location)
        val after = source.editor.document.text
        val undo = UndoManager.getInstance(project)
        undo.undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, source.editor.document.text)
        undo.redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(after, source.editor.document.text)
    }

    fun testUnlockedReleaseCreatesPresetExactlyOnce() = withEditor { canvas, _ ->
        canvas.setCreationKind(DiagramNodeKind.Activity, ActivityPreset("Created preset"))
        send(canvas, MouseEvent.MOUSE_PRESSED, 150, 160)
        send(canvas, MouseEvent.MOUSE_DRAGGED, 167, 153)
        val location = canvas.ghostNode!!.location
        unlocked {
            send(canvas, MouseEvent.MOUSE_RELEASED, 167, 153)
            send(canvas, MouseEvent.MOUSE_CLICKED, 167, 153)
        }
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(3, canvas.diagram.nodes.size)
        assertEquals("Created preset", canvas.selectedNodes.single().name)
        assertEquals(location, canvas.selectedNodes.single().location)
    }

    fun testDeferredRequestsAreCancelledByModeAndPresetChanges() {
        for (sameKind in listOf(false, true)) withEditor { canvas, source ->
            val original = source.editor.document.text
            unlocked {
                send(canvas, MouseEvent.MOUSE_CLICKED, 167, 153)
                canvas.setCreationKind(if (sameKind) DiagramNodeKind.Activity else null, ActivityPreset("New preset"))
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(original, source.editor.document.text)
            assertEquals(2, canvas.diagram.nodes.size)
        }
    }

    fun testDeferredRequestDoesNotRunAfterDisposal() {
        val file = myFixture.addFileToProject("dispose-creation.glue_uml_ad", xml).virtualFile
        val design = ModelDesignEditorProvider().createEditor(project, file)
        val canvas = design.component.components.filterIsInstance<JScrollPane>().single().viewport.view as ModelDiagramCanvas
        canvas.setSize(600, 400)
        canvas.setCreationKind(DiagramNodeKind.Activity)
        val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)!!
        try {
            unlocked { send(canvas, MouseEvent.MOUSE_CLICKED, 167, 153); Disposer.dispose(design) }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(xml, document.text)
        } finally { Disposer.dispose(design) }
    }

    fun testCreationSnapToggleAltAndGuideCleanup() = withEditor { canvas, source ->
        val original = source.editor.document.text
        send(canvas, MouseEvent.MOUSE_MOVED, 167, 153)
        assertEquals(DiagramPoint(120.0, 140.0), canvas.ghostNode!!.location)
        assertTrue(canvas.alignmentGuides.isNotEmpty())
        canvas.snapEnabled = false
        assertEquals(DiagramPoint(117.0, 138.0), canvas.ghostNode!!.location)
        assertTrue(canvas.alignmentGuides.isEmpty())
        canvas.snapEnabled = true
        assertEquals(DiagramPoint(120.0, 140.0), canvas.ghostNode!!.location)
        send(canvas, MouseEvent.MOUSE_MOVED, 167, 153, alt = true)
        assertEquals(DiagramPoint(117.0, 138.0), canvas.ghostNode!!.location)
        assertTrue(canvas.alignmentGuides.isEmpty())
        send(canvas, MouseEvent.MOUSE_MOVED, 167, 153)
        send(canvas, MouseEvent.MOUSE_EXITED, 167, 153)
        assertNull(canvas.ghostNode)
        assertTrue(canvas.alignmentGuides.isEmpty())
        send(canvas, MouseEvent.MOUSE_MOVED, 167, 153)
        canvas.cancelDrag()
        assertTrue(canvas.alignmentGuides.isEmpty())
        assertEquals(original, source.editor.document.text)
    }

    fun testEqualSpacingAndZoomedNegativeCoordinates() {
        val a = DiagramNode(DiagramNodeKind.Activity, "1", "A", DiagramPoint(20.0, 140.0), 100.0, 30.0)
        val b = a.copy(id = "2", location = DiagramPoint(420.0, 140.0))
        val canvas = ModelDiagramCanvas().apply {
            setSize(600, 400); display(ModelDiagram(listOf(a, b), emptyList()))
            snapEnabled = true; setCreationKind(DiagramNodeKind.Activity)
        }
        send(canvas, MouseEvent.MOUSE_MOVED, 267, 153)
        assertEquals(DiagramPoint(220.0, 140.0), canvas.ghostNode!!.location)
        assertTrue(canvas.alignmentGuides.any { it.distance == 100.0 })
        for (scale in listOf(0.5, 1.0, 1.5)) {
            canvas.display(ModelDiagram(listOf(a.copy(location = DiagramPoint(-180.0, -160.0))), emptyList()))
            canvas.setZoom(scale)
            canvas.navigateTo(DiagramPoint(-133.0, -147.0))
            send(canvas, MouseEvent.MOUSE_MOVED, 300, 200)
            val location = canvas.ghostNode!!.location
            assertEquals(DiagramPoint(-180.0, -160.0), location)
            var submitted: DiagramPoint? = null
            canvas.onChildCreateRequested = { _, point -> submitted = point }
            send(canvas, MouseEvent.MOUSE_CLICKED, 300, 200)
            assertEquals(location, submitted)
        }
    }
}
