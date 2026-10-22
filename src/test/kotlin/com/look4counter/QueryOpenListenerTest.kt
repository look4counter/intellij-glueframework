package com.look4counter

import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.application.TransactionGuardImpl
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.EditorTextField
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Container

class QueryOpenListenerTest : BasePlatformTestCase() {
    private val editors get() = FileEditorManager.getInstance(project)

    override fun tearDown() {
        try {
            editors.openFiles.forEach(editors::closeFile)
        } finally {
            super.tearDown()
        }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    fun testOpeningTrimsSharedDocumentWithoutSavingAndSupportsUndoAndReopen() {
        val file = myFixture.addFileToProject("opened.glue_sql", PADDED).virtualFile
        val documents = FileDocumentManager.getInstance()
        val document = documents.getDocument(file)!!
        editors.openFile(file, false)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(TRIMMED, document.text)
        assertEquals(PADDED, file.contentsToByteArray().toString(Charsets.UTF_8))
        assertTrue(documents.isDocumentUnsaved(document))
        val opened = editors.getAllEditors(file)
        val source = opened.filterIsInstance<TextEditor>().single()
        assertSame(document, source.editor.document)
        val design = FileEditorProviderManager.getInstance().getProviderList(project, file)
            .single { it.editorTypeId == "glue-query-design" }.createEditor(project, file)
        try {
            UIUtil.dispatchAllInvocationEvents()
            val sql = descendants(design.component).filterIsInstance<EditorTextField>().single { it.name == "query.sql" }
            assertEquals("delete from emp\nwhere empno=?", sql.text)
        } finally {
            Disposer.dispose(design)
        }
        UndoManager.getInstance(project).undo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(PADDED, document.text)
        UndoManager.getInstance(project).redo(source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(TRIMMED, document.text)
        documents.saveDocument(document)
        editors.closeFile(file)
        val stamp = document.modificationStamp
        editors.openFile(file, false)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(TRIMMED, document.text)
        assertEquals(stamp, document.modificationStamp)
        assertFalse(documents.isDocumentUnsaved(document))
    }

    fun testOpenEventDefersWritesFromUnsafeContext() {
        val file = myFixture.addFileToProject("unsafe-open.glue_sql", TRIMMED).virtualFile
        editors.openFile(file, false)
        UIUtil.dispatchAllInvocationEvents()
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText(PADDED) }
        val guard = TransactionGuard.getInstance() as TransactionGuardImpl
        guard.performActivity(false) {
            assertFalse(guard.isWritingAllowed)
            project.messageBus.syncPublisher(FileEditorManagerListener.FILE_EDITOR_MANAGER).fileOpened(editors, file)
            assertEquals(PADDED, document.text)
        }
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(TRIMMED, document.text)
    }

    fun testQueuedOpenEventForClosedFileDoesNotChangeDocument() {
        val file = myFixture.addFileToProject("closed.glue_sql", PADDED).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        assertFalse(editors.isFileOpen(file))
        project.messageBus.syncPublisher(FileEditorManagerListener.FILE_EDITOR_MANAGER).fileOpened(editors, file)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(PADDED, document.text)
    }

    fun testOpeningPreservesExcludedAndReadOnlyFiles() {
        val cases = mapOf(
            "other.xml" to PADDED,
            "malformed.glue_sql" to "<queryMap><query> sql </queryMap>",
            "mixed.glue_sql" to "<queryMap><query> a<!-- keep --><if/> b </query></queryMap>",
            "readonly.glue_sql" to PADDED,
        )
        cases.forEach { (name, text) ->
            val file = myFixture.addFileToProject(name, text).virtualFile
            if (name.startsWith("readonly")) WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
            try {
                editors.openFile(file, false)
                UIUtil.dispatchAllInvocationEvents()
                assertEquals(text, FileDocumentManager.getInstance().getDocument(file)!!.text)
            } finally {
                editors.closeFile(file)
                if (!file.isWritable) WriteCommandAction.runWriteCommandAction(project) { file.isWritable = true }
            }
        }
    }

    companion object {
        private const val PADDED = "<queryMap><query id=\"same\">\n  <![CDATA[delete from emp\nwhere empno=?]]>\n</query>" +
            "<query id=\"same\"><![CDATA[\n  delete from emp\nwhere empno=?\n]]></query></queryMap>"
        private const val TRIMMED = "<queryMap><query id=\"same\"><![CDATA[delete from emp\nwhere empno=?]]></query>" +
            "<query id=\"same\"><![CDATA[delete from emp\nwhere empno=?]]></query></queryMap>"
    }
}
