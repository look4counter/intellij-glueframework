package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.vfs.VirtualFile
import com.look4counter.modeleditor.ServiceFiles

class ModelEditorNavigationTest : BasePlatformTestCase() {
    fun testOpenServiceSelectsDesignForNewAndAlreadyOpenSource() {
        val file = myFixture.addFileToProject("flows/sample2.glue_uml_ad", "<GlueDiagram/>").virtualFile
        val providers = FileEditorProviderManager.getInstance().getProviderList(project, file)
        val editors = providers.associate { it.editorTypeId to it.createEditor(project, file) }
        var selected: FileEditor? = null
        var opened: VirtualFile? = null
        val manager = object : com.intellij.mock.Mock.MyFileEditorManager() {
            override fun openFile(target: VirtualFile, focusEditor: Boolean): Array<FileEditor> {
                opened = target
                if (selected == null) selected = editors["glue-model-source"]
                return editors.values.toTypedArray()
            }
            override fun setSelectedEditor(target: VirtualFile, editorTypeId: String) {
                assertEquals(opened, target)
                selected = editors.getValue(editorTypeId)
            }
        }
        project.replaceService(FileEditorManager::class.java, manager, testRootDisposable)
        try {
        assertTrue(ServiceFiles.open(project, "sample2-service"))
        assertEquals(file, opened)
        assertEquals("Design", selected!!.name)
        manager.setSelectedEditor(file, "glue-model-source")
        assertEquals("Source", selected!!.name)
        assertTrue(ServiceFiles.open(project, "sample2-service"))
        assertEquals("Design", selected!!.name)
        } finally { editors.values.forEach(com.intellij.openapi.util.Disposer::dispose) }
    }
}
