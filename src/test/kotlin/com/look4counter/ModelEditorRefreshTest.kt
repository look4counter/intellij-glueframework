package com.look4counter

import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.impl.LaterInvocator
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import com.intellij.util.ui.UIUtil
import com.look4counter.modeleditor.ModelDesignEditorProvider
import com.look4counter.modeleditor.ModelDiagramCanvas
import com.look4counter.modeleditor.DiagramNodeKind
import com.look4counter.modeleditor.ChildCreationToolbar
import com.look4counter.modeleditor.ActivityPreset
import java.awt.event.MouseEvent
import javax.swing.JScrollPane

class ModelEditorRefreshTest : BasePlatformTestCase() {
    private fun inWriteSafeModal(action: () -> Unit) {
        val modal = Any()
        val guard = TransactionGuard.getInstance() as TransactionGuardImpl
        guard.performUserActivity { LaterInvocator.enterModal(modal) }
        try {
            assertTrue(guard.isWriteSafeModality(ModalityState.current()))
            action()
        } finally { LaterInvocator.leaveModal(modal) }
    }

    fun testNewEditorRefreshesBeforeSafeModalCloses() {
        val file = myFixture.addFileToProject("modal-new.glue_uml_ad", xml("latest")).virtualFile
        var editor: FileEditor? = null
        try {
            inWriteSafeModal {
                unsafe {
                    editor = ModelDesignEditorProvider().createEditor(project, file)
                    assertTrue(canvas(editor!!).diagram.nodes.isEmpty())
                }
                UIUtil.dispatchAllInvocationEvents()
                assertEquals("latest", canvas(editor!!).diagram.nodes.single().name)
            }
        } finally { editor?.let(Disposer::dispose) }
    }

    fun testOpenServicePromotesPendingRefreshWhileModalRemainsOpen() {
        val file = myFixture.addFileToProject("flows/modal-target.glue_uml_ad", xml("old")).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val manager = object : com.intellij.mock.Mock.MyFileEditorManager() {
            override fun openFile(target: com.intellij.openapi.vfs.VirtualFile, focusEditor: Boolean): Array<FileEditor> {
                assertEquals(file, target)
                return arrayOf(editor)
            }
            override fun setSelectedEditor(target: com.intellij.openapi.vfs.VirtualFile, editorTypeId: String) {
                assertEquals("glue-model-design", editorTypeId)
                editor.selectNotify()
            }
        }
        project.replaceService(com.intellij.openapi.fileEditor.FileEditorManager::class.java, manager, testRootDisposable)
        try {
            WriteCommandAction.runWriteCommandAction(project) { document.setText(xml("latest")) }
            assertEquals("old", canvas(editor).diagram.nodes.single().name)
            inWriteSafeModal {
                unsafe { assertTrue(com.look4counter.modeleditor.ServiceFiles.open(project, "modal-target-service")) }
                UIUtil.dispatchAllInvocationEvents()
                assertTrue(PsiDocumentManager.getInstance(project).isCommitted(document))
                assertEquals("latest", canvas(editor).diagram.nodes.single().name)
                WriteCommandAction.runWriteCommandAction(project) { document.setText(xml("newest")) }
                UIUtil.dispatchAllInvocationEvents()
                assertEquals("newest", canvas(editor).diagram.nodes.single().name)
                assertEquals(xml("newest"), document.text)
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("newest", canvas(editor).diagram.nodes.single().name)
        } finally { Disposer.dispose(editor) }
    }

    fun testWriteUnsafeModalDefersCommitUntilModalCloses() {
        val file = myFixture.addFileToProject("modal-unsafe.glue_uml_ad", xml("latest")).virtualFile
        val modal = Any()
        var editor: FileEditor? = null
        unsafe { LaterInvocator.enterModal(modal) }
        try {
            assertFalse(TransactionGuard.getInstance().isWriteSafeModality(ModalityState.current()))
            unsafe { editor = ModelDesignEditorProvider().createEditor(project, file) }
            UIUtil.dispatchAllInvocationEvents()
            assertTrue(canvas(editor!!).diagram.nodes.isEmpty())
        } finally { LaterInvocator.leaveModal(modal) }
        try {
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("latest", canvas(editor!!).diagram.nodes.single().name)
        } finally { editor?.let(Disposer::dispose) }
    }

    private fun click(canvas: ModelDiagramCanvas, x: Int = 300, y: Int = 200) {
        canvas.dispatchEvent(MouseEvent(canvas, MouseEvent.MOUSE_CLICKED, 0, 0, x, y, 1, false, MouseEvent.BUTTON1))
    }
    fun testCreationImmediatelyAfterSelectNotifyAndBeforeQueuedRefresh() {
        val file = myFixture.addFileToProject("pending-create.glue_uml_ad", xml("old")).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = canvas(editor).apply { setSize(600, 400); setCreationKind(DiagramNodeKind.Activity) }
            editor.selectNotify()
            click(canvas)
            assertEquals(2, canvas.diagram.nodes.size)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(2, canvas.diagram.nodes.size)
            assertEquals("2", canvas.diagram.nodes.last().id)
        } finally { Disposer.dispose(editor) }
    }
    fun testConsecutiveCreationDoesNotWaitForRefreshQueue() {
        val file = myFixture.addFileToProject("continuous-create.glue_uml_ad", xml("old")).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = canvas(editor).apply { setSize(600, 400); setCreationKind(DiagramNodeKind.Activity) }
            click(canvas)
            click(canvas, 430, 240)
            assertEquals(3, canvas.diagram.nodes.size)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(3, canvas.diagram.nodes.size)
            assertEquals(listOf("1", "2", "3"), canvas.diagram.nodes.map { it.id })
            assertEquals(listOf(com.look4counter.modeleditor.DiagramPoint(250.0, 185.0),
                com.look4counter.modeleditor.DiagramPoint(380.0, 225.0)), canvas.diagram.nodes.drop(1).map { it.location })
        } finally { Disposer.dispose(editor) }
    }
    fun testCreationUsesLatestSourceAndNewMaximumId() {
        val file = myFixture.addFileToProject("changed-create.glue_uml_ad", xml("old")).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = canvas(editor).apply { setSize(600, 400); setCreationKind(DiagramNodeKind.Activity) }
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            WriteCommandAction.runWriteCommandAction(project) { document.setText(xml("latest").replace("id=\"1\"", "id=\"50\"")) }
            click(canvas)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(listOf("50", "51"), canvas.diagram.nodes.map { it.id })
            assertEquals("latest", canvas.diagram.nodes.first().name)
        } finally { Disposer.dispose(editor) }
    }
    fun testInvalidLatestSourcePreventsCreation() {
        val file = myFixture.addFileToProject("invalid-create.glue_uml_ad", xml("old")).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = canvas(editor).apply { setSize(600, 400); setCreationKind(DiagramNodeKind.Activity) }
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            val invalid = "<GlueDiagram><child"
            WriteCommandAction.runWriteCommandAction(project) { document.setText(invalid) }
            click(canvas)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(invalid, document.text)
            assertNull(canvas.creationKind)
        } finally { Disposer.dispose(editor) }
    }
    fun testUnsafeCreationIsDeferredAndCancelledModeIsRespected() {
        val file = myFixture.addFileToProject("unsafe-create.glue_uml_ad", xml("old")).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = canvas(editor).apply { setSize(600, 400); setCreationKind(DiagramNodeKind.Activity) }
            unsafe { click(canvas) }
            assertEquals(1, canvas.diagram.nodes.size)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(1, canvas.diagram.nodes.size)
            unsafe { click(canvas, 430, 240) }
            canvas.setCreationKind(null)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(1, canvas.diagram.nodes.size)
        } finally { Disposer.dispose(editor) }
    }
    fun testMenuSelectionQueuesPresetSnapshotAndModeRestartCancelsOldRequest() {
        val file = myFixture.addFileToProject("queue-preset.glue_uml_ad", xml("old")).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = canvas(editor).apply { setSize(600, 400) }
            val toolbar = editor.component.components.filterIsInstance<ChildCreationToolbar>().single()
            val preset = ActivityPreset("snapshot")
            toolbar.selectPreset(preset)
            click(canvas)
            assertEquals(2, canvas.diagram.nodes.size)
            preset.name = "modified"
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("snapshot", canvas.diagram.nodes.last().name)
            click(canvas)
            canvas.setCreationKind(null)
            canvas.setCreationKind(DiagramNodeKind.Activity)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(3, canvas.diagram.nodes.size)
            click(canvas)
            Disposer.dispose(editor)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(4, canvas.diagram.nodes.size)
        } finally { if (!Disposer.isDisposed(editor)) Disposer.dispose(editor) }
    }
    private fun xml(name: String) = """<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><child xsi:type="Activity" id="1" name="$name" location="20,40"/></GlueDiagram>"""
    private fun canvas(editor: FileEditor) = (editor.component.components.filterIsInstance<JScrollPane>().single().viewport.view as ModelDiagramCanvas)
    private fun unsafe(action: () -> Unit) {
        val guard = TransactionGuard.getInstance() as TransactionGuardImpl
        guard.performActivity(false) {
            assertFalse(guard.isWritingAllowed)
            action()
        }
    }

    fun testSelectNotifyDefersCommitFromWriteUnsafeContextAndShowsLatestDocument() {
        val file = myFixture.addFileToProject("refresh.glue_uml_ad", xml("old")).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        try {
            val canvas = canvas(editor)
            assertEquals("old", canvas.diagram.nodes.single().name)
            val document = FileDocumentManager.getInstance().getDocument(file)!!
            WriteCommandAction.runWriteCommandAction(project) { document.setText(xml("first")) }
            unsafe {
                assertFalse(PsiDocumentManager.getInstance(project).isCommitted(document))
                editor.selectNotify()
                editor.selectNotify()
                assertFalse(PsiDocumentManager.getInstance(project).isCommitted(document))
                assertEquals("old", canvas.diagram.nodes.single().name)
            }
            WriteCommandAction.runWriteCommandAction(project) { document.setText(xml("latest")) }
            UIUtil.dispatchAllInvocationEvents()
            assertTrue(PsiDocumentManager.getInstance(project).isCommitted(document))
            assertEquals("latest", canvas.diagram.nodes.single().name)
            assertEquals(xml("latest"), document.text)
        } finally { Disposer.dispose(editor) }
    }

    fun testCreationFromWriteUnsafeContextDefersInitialRefresh() {
        val file = myFixture.addFileToProject("create-unsafe.glue_uml_ad", xml("old")).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText(xml("latest")) }
        var editor: FileEditor? = null
        try {
            unsafe {
                editor = ModelDesignEditorProvider().createEditor(project, file)
                assertTrue(canvas(editor!!).diagram.nodes.isEmpty())
                assertFalse(PsiDocumentManager.getInstance(project).isCommitted(document))
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("latest", canvas(editor!!).diagram.nodes.single().name)
        } finally { editor?.let(Disposer::dispose) }
    }

    fun testDisposedEditorDoesNotApplyQueuedRefresh() {
        val file = myFixture.addFileToProject("disposed-refresh.glue_uml_ad", xml("old")).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val canvas = canvas(editor)
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText(xml("new")) }
        unsafe { editor.selectNotify() }
        Disposer.dispose(editor)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("old", canvas.diagram.nodes.single().name)
    }
}
