package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.codeStyle.CodeStyleManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.EditorTestUtil
import com.intellij.util.ui.UIUtil
import org.jdom.Element

class EditorMultiViewTest : BasePlatformTestCase() {
    fun testEachFileHasExactlyDesignAndSourceEditors() {
        for (extension in listOf("glue_sql", "glue_uml_ad")) {
            val file = myFixture.addFileToProject("tabs.$extension", "<root/>").virtualFile
            val providers = FileEditorProviderManager.getInstance().getProviderList(project, file)
            val editors = providers.map { it.createEditor(project, file) }
            try {
                assertEquals(listOf("Design", "Source"), editors.map { it.name })
                assertTrue(editors[1] is TextEditor)
                val source = editors[1] as TextEditor
                EditorTestUtil.waitForLoading(source.editor)
                assertSame(FileDocumentManager.getInstance().getDocument(file), source.editor.document)
                assertFalse(editors[0].isModified)
            } finally {
                editors.forEach(Disposer::dispose)
            }
        }
    }

    fun testProvidersDoNotClaimOtherFiles() {
        val files = listOf("plain.xml", "plain.txt", "plain.sql", "plain.uml")
        for (name in files) {
            val file = myFixture.addFileToProject(name, "<root/>").virtualFile
            val providers = FileEditorProviderManager.getInstance().getProviderList(project, file)
            assertFalse(providers.any { it.editorTypeId.startsWith("glue-") })
        }
    }

    fun testSwitchingViewsPreservesUnsavedSourceAndSave() {
        for (extension in listOf("glue_sql", "glue_uml_ad")) {
            val file = myFixture.addFileToProject("switch.$extension", "<root/>").virtualFile
            val providers = FileEditorProviderManager.getInstance().getProviderList(project, file)
            val editors = providers.map { it.createEditor(project, file) }
            try {
                val source = editors.single { it.name == "Source" } as TextEditor
                val design = editors.single { it.name == "Design" }
                EditorTestUtil.waitForLoading(source.editor)
                source.selectNotify()
                val text = "<root><changed/></root>"
                WriteCommandAction.runWriteCommandAction(project) { source.editor.document.setText(text) }
                source.deselectNotify()
                design.selectNotify()
                design.deselectNotify()
                source.selectNotify()
                assertEquals(text, source.editor.document.text)
                UIUtil.dispatchAllInvocationEvents()
                assertTrue(FileDocumentManager.getInstance().isDocumentUnsaved(source.editor.document))
                assertTrue(source.isModified)
                val undo = UndoManager.getInstance(project)
                assertTrue(undo.isUndoAvailable(source))
                undo.undo(source)
                assertEquals("<root/>", source.editor.document.text)
                undo.redo(source)
                assertEquals(text, source.editor.document.text)
                FileDocumentManager.getInstance().saveDocument(source.editor.document)
                assertEquals(text, String(file.contentsToByteArray(), Charsets.UTF_8))
                assertEquals(extension, file.extension)
            } finally {
                editors.forEach(Disposer::dispose)
            }
        }
    }

    fun testSourceStateAndDisposal() {
        for (extension in listOf("glue_sql", "glue_uml_ad")) {
            val file = myFixture.addFileToProject("state.$extension", "<root>content</root>").virtualFile
            val provider = FileEditorProviderManager.getInstance().getProviderList(project, file)
                .single { it.editorTypeId.endsWith("-source") }
            val source = provider.createEditor(project, file) as TextEditor
            val editor = source.editor
            try {
                EditorTestUtil.waitForLoading(editor)
                editor.caretModel.moveToOffset(8)
                val state = source.getState(FileEditorStateLevel.FULL)
                val xml = Element("state")
                provider.writeState(state, project, xml)
                editor.caretModel.moveToOffset(0)
                source.setState(provider.readState(xml, project, file))
                assertEquals(8, editor.caretModel.offset)
            } finally {
                Disposer.dispose(source)
            }
            assertTrue(editor.isDisposed)
        }
    }

    fun testEmptyMalformedAndReadOnlyFilesStillHaveSource() {
        for (extension in listOf("glue_sql", "glue_uml_ad")) {
            for ((label, text) in listOf("empty" to "", "malformed" to "<root>")) {
                val file = myFixture.addFileToProject("$label.$extension", text).virtualFile
                val providers = FileEditorProviderManager.getInstance().getProviderList(project, file)
                assertEquals(2, providers.size)
                val source = providers.single { it.editorTypeId.endsWith("-source") }
                    .createEditor(project, file) as TextEditor
                try {
                    assertEquals(text, source.editor.document.text)
                    WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
                    assertFalse(source.editor.document.isWritable)
                } finally {
                    WriteCommandAction.runWriteCommandAction(project) { file.isWritable = true }
                    Disposer.dispose(source)
                }
            }
        }
    }

    fun testXmlClosingTagCompletion() {
        for (extension in listOf("glue_sql", "glue_uml_ad")) {
            myFixture.configureByText("completion.$extension", "<root></ro<caret>")
            myFixture.completeBasic()
            assertTrue(myFixture.file.text.contains("</root"))
        }
    }

    fun testXmlSyntaxHighlightingFormattingAndErrors() {
        for (extension in listOf("glue_sql", "glue_uml_ad")) {
            val file = myFixture.configureByText("format.$extension", "<root><child/></root>")
            val iterator = myFixture.editor.highlighter.createIterator(0)
            assertTrue(iterator.tokenType.toString().startsWith("XML_"))
            WriteCommandAction.runWriteCommandAction(project) {
                CodeStyleManager.getInstance(project).reformat(file)
            }
            assertTrue(file.text.contains("\n"))
            myFixture.configureByText("broken.$extension", "<root></wrong>")
            assertTrue(myFixture.doHighlighting().any { it.severity.name == "ERROR" })
        }
    }
}
