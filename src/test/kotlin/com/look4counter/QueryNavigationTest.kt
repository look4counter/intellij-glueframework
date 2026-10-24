package com.look4counter

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.NavigatableFileEditor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.QueryEditorNavigation
import com.look4counter.queryeditor.QueryNavigation
import com.look4counter.queryeditor.queryindex.QueryIdIndexService
import com.intellij.util.ui.UIUtil
import javax.swing.JTextField

class QueryNavigationTest : BasePlatformTestCase() {
    private fun components(component: java.awt.Component): Sequence<java.awt.Component> = sequence {
        yield(component)
        if (component is java.awt.Container) component.components.forEach { yieldAll(components(it)) }
    }
    fun testOpenIdAndFindInFilesOffsetSelectSameQueryWithoutChangingXml() {
        val text = """<queryMap><query id="first"><![CDATA[select 1]]></query><query id="second"><![CDATA[select 2]]></query></queryMap>"""
        val file = myFixture.addFileToProject("navigation.glue_sql", text).virtualFile
        val index = QueryIdIndexService.getInstance(project)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("Query 수집", { !index.isUpdating }, 10)
        assertEquals(1, QueryNavigation.find(project, "second").size)
        val opened = QueryNavigation.open(project, "second")
        assertTrue("열린 에디터: ${FileEditorManager.getInstance(project).getAllEditors(file).map { it.javaClass.name }}", opened)
        UIUtil.dispatchAllInvocationEvents()
        val editor = com.look4counter.queryeditor.QueryDesignEditorProvider().createEditor(project, file)
        UIUtil.dispatchAllInvocationEvents()
        fun selectedId() = components(editor.component).filterIsInstance<JTextField>().single { it.name == "query.id" }.text
        assertEquals("second", selectedId())
        val descriptor = OpenFileDescriptor(project, file, text.indexOf("select 1"))
        assertTrue((editor as NavigatableFileEditor).canNavigateTo(descriptor))
        editor.navigateTo(descriptor)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("first", selectedId())
        assertFalse(editor.canNavigateTo(OpenFileDescriptor(project, file, 1)))
        assertFalse(QueryNavigation.open(project, "missing"))
        assertEquals(text, com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)!!.text)
        com.intellij.openapi.util.Disposer.dispose(editor)
        FileEditorManager.getInstance(project).closeFile(file)
    }
}
