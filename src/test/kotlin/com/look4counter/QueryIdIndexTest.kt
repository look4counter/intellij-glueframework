package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.queryindex.QueryIdIndexService

class QueryIdIndexTest : BasePlatformTestCase() {
    private fun QueryIdIndexService.collected(prefix: String = ""): List<com.look4counter.queryeditor.queryindex.IndexedQueryId> {
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("Query ID 갱신", { !isUpdating }, 10)
        return snapshot(prefix)
    }
    private fun xml(id: String, description: String = "설명") =
        """<queryMap xmlns="urn:query"><query id="$id" desc="$description"><![CDATA[select 1]]></query></queryMap>"""

    fun testInitialCollectionIncludesUnopenedFilesAndDescriptions() {
        myFixture.addFileToProject("queries/a.glue_sql", xml("sample.a"))
        myFixture.addFileToProject("queries/b.GLUE_SQL", xml("sample.b", "두 번째"))
        myFixture.addFileToProject("queries/ignored.xml", xml("ignored"))
        val index = QueryIdIndexService.getInstance(project)
        assertEquals(listOf("sample.a", "sample.b"), index.collected("SAMPLE.").map { it.id })
        assertEquals("두 번째", index.collected("sample.b").single().description)
        assertEquals("b.GLUE_SQL", index.collected("sample.b").single().fileName)
    }

    fun testUnsavedChangesInvalidXmlAndUndoContentRecovery() {
        val file = myFixture.addFileToProject("queries/a.glue_sql", xml("before")).virtualFile
        val index = QueryIdIndexService.getInstance(project)
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText(xml("after", "수정")) }
        assertEquals(listOf("after"), index.collected().map { it.id })
        assertEquals("수정", index.collected().single().description)
        assertTrue(FileDocumentManager.getInstance().isDocumentUnsaved(document))
        WriteCommandAction.runWriteCommandAction(project) { document.setText("<queryMap><query id=\"broken\"") }
        assertTrue(index.collected().isEmpty())
        WriteCommandAction.runWriteCommandAction(project) { document.setText(xml("before")) }
        assertEquals(listOf("before"), index.collected().map { it.id })
    }

    fun testCreateExternalEditRenameMoveAndDeleteRefreshMemory() {
        val index = QueryIdIndexService.getInstance(project)
        assertTrue(index.collected().isEmpty())
        val file = myFixture.addFileToProject("queries/new.glue_sql", xml("created")).virtualFile
        assertEquals(listOf("created"), index.collected().map { it.id })
        WriteCommandAction.runWriteCommandAction(project) { VfsUtil.saveText(file, xml("external")) }
        assertEquals(listOf("external"), index.collected().map { it.id })
        WriteCommandAction.runWriteCommandAction(project) { file.rename(this, "renamed.glue_sql") }
        assertEquals("renamed.glue_sql", index.collected().single().fileName)
        val directory = myFixture.tempDirFixture.findOrCreateDir("moved")
        WriteCommandAction.runWriteCommandAction(project) { file.move(this, directory) }
        assertTrue(index.collected().single().fileUrl.endsWith("/moved/renamed.glue_sql"))
        WriteCommandAction.runWriteCommandAction(project) { file.rename(this, "renamed.txt") }
        assertTrue(index.collected().isEmpty())
        WriteCommandAction.runWriteCommandAction(project) { file.rename(this, "again.glue_sql") }
        assertEquals(listOf("external"), index.collected().map { it.id })
        WriteCommandAction.runWriteCommandAction(project) { file.delete(this) }
        assertTrue(index.collected().isEmpty())
    }

    fun testDirectChildrenNamespacesBlankIdsAndDuplicateFiles() {
        myFixture.addFileToProject("a.glue_sql", """<q:queryMap xmlns:q="urn:q" xmlns:other="urn:other">
            <q:query id="same" desc="A &amp; B"/><q:query id=""/><other:query id="wrong"/>
            <q:group><q:query id="nested"/></q:group></q:queryMap>""")
        myFixture.addFileToProject("b.glue_sql", xml("same"))
        myFixture.addFileToProject("wrong.glue_sql", "<other><query id=\"wrong-root\"/></other>")
        val entries = QueryIdIndexService.getInstance(project).collected()
        assertEquals(listOf("same", "same"), entries.map { it.id })
        assertEquals(listOf("a.glue_sql", "b.glue_sql"), entries.map { it.fileName })
        assertEquals("A & B", entries[0].description)
    }

    fun testExcludedRootsAndDirectoryRenameRebuildContentScope() {
        myFixture.addFileToProject("included/a.glue_sql", xml("included"))
        val excluded = myFixture.addFileToProject("excluded/b.glue_sql", xml("excluded")).virtualFile.parent
        val index = QueryIdIndexService.getInstance(project)
        assertEquals(listOf("excluded", "included"), index.collected().map { it.id })
        com.intellij.testFramework.PsiTestUtil.addExcludedRoot(module, excluded)
        assertEquals(listOf("included"), index.collected().map { it.id })
        com.intellij.testFramework.PsiTestUtil.removeExcludedRoot(module, excluded)
        assertEquals(listOf("excluded", "included"), index.collected().map { it.id })
        WriteCommandAction.runWriteCommandAction(project) { excluded.rename(this, "renamed-directory") }
        assertTrue(index.collected("excluded").single().fileUrl.contains("/renamed-directory/"))
        WriteCommandAction.runWriteCommandAction(project) { excluded.delete(this) }
        assertEquals(listOf("included"), index.collected().map { it.id })
    }

    fun testQueryDesignIdChangeAndUndoUpdateMemory() {
        val file = myFixture.addFileToProject("design.glue_sql", xml("before")).virtualFile
        val index = QueryIdIndexService.getInstance(project)
        assertEquals(listOf("before"), index.collected().map { it.id })
        val design = com.look4counter.queryeditor.QueryDesignEditorProvider().createEditor(project, file)
        val source = com.look4counter.queryeditor.QuerySourceEditorProvider().createEditor(project, file) as com.intellij.openapi.fileEditor.TextEditor
        fun fields(component: java.awt.Component): Sequence<java.awt.Component> = sequence {
            yield(component)
            if (component is java.awt.Container) component.components.forEach { yieldAll(fields(it)) }
        }
        try {
            com.intellij.testFramework.EditorTestUtil.waitForLoading(source.editor)
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents()
            fields(design.component).filterIsInstance<javax.swing.JTextField>().single { it.name == "query.id" }.text = "after"
            com.intellij.util.ui.UIUtil.dispatchAllInvocationEvents()
            assertEquals(listOf("after"), index.collected().map { it.id })
            com.intellij.openapi.command.undo.UndoManager.getInstance(project).undo(source)
            assertEquals(listOf("before"), index.collected().map { it.id })
        } finally {
            com.intellij.openapi.util.Disposer.dispose(source)
            com.intellij.openapi.util.Disposer.dispose(design)
        }
    }
}
