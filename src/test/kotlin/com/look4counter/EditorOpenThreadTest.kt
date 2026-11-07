package com.look4counter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.impl.ApplicationImpl
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.NavigatableFileEditor
import com.intellij.openapi.fileEditor.OpenFileDescriptor
import com.intellij.openapi.util.Disposer
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import com.intellij.util.ui.UIUtil
import com.look4counter.queryeditor.QueryDesignEditorProvider
import com.look4counter.queryeditor.QueryEditorNavigation
import com.look4counter.modeleditor.ModelDesignEditorProvider
import com.look4counter.modeleditor.ModelDiagramCanvas
import java.awt.Component
import java.awt.Container
import javax.swing.JTextField

class EditorOpenThreadTest : BasePlatformTestCase() {
    private fun withoutWriteIntent(action: () -> Unit) {
        val app = ApplicationManager.getApplication() as ApplicationImpl
        app.threadingSupport.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack {
            assertFalse(app.isWriteIntentLockAcquired)
            action()
        }
    }

    private fun components(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(components(it)) }
    }

    private fun selected(editor: FileEditor) = components(editor.component).filterIsInstance<JTextField>()
        .single { it.name == "query.id" }.text

    fun testQueryCreationAndSearchNavigationWithoutWriteIntent() {
        val file = myFixture.addFileToProject("search.glue_sql", XML).virtualFile
        var editor: FileEditor? = null
        try {
            withoutWriteIntent {
                editor = QueryDesignEditorProvider().createEditor(project, file)
                (editor as NavigatableFileEditor).navigateTo(OpenFileDescriptor(project, file, XML.indexOf("select 2")))
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("second", selected(editor!!))
            assertEquals(XML, com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)!!.text)
        } finally { editor?.let(Disposer::dispose) }
    }

    fun testQueryIdNavigationWithoutWriteIntent() {
        val file = myFixture.addFileToProject("id-search.glue_sql", XML).virtualFile
        val editor = QueryDesignEditorProvider().createEditor(project, file)
        try {
            UIUtil.dispatchAllInvocationEvents()
            withoutWriteIntent { assertTrue((editor as QueryEditorNavigation).selectQuery("second")) }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("second", selected(editor))
        } finally { Disposer.dispose(editor) }
    }

    fun testModelCreationAndSelectionWithoutWriteIntent() {
        val file = myFixture.addFileToProject("search.glue_uml_ad",
            """<GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"><child xsi:type="Activity" id="1" name="found" location="20,40"/></GlueDiagram>""").virtualFile
        var editor: FileEditor? = null
        try {
            withoutWriteIntent {
                editor = ModelDesignEditorProvider().createEditor(project, file)
                editor!!.selectNotify()
            }
            UIUtil.dispatchAllInvocationEvents()
            val canvas = components(editor!!.component).filterIsInstance<ModelDiagramCanvas>().single()
            assertEquals("found", canvas.diagram.nodes.single().name)
        } finally { editor?.let(Disposer::dispose) }
    }

    fun testLastSearchRequestWinsAcrossIdAndOffsetNavigation() {
        val file = myFixture.addFileToProject("repeat.glue_sql", XML).virtualFile
        val editor = QueryDesignEditorProvider().createEditor(project, file)
        try {
            UIUtil.dispatchAllInvocationEvents()
            withoutWriteIntent {
                val navigation = editor as QueryEditorNavigation
                navigation.selectQuery("second")
                navigation.selectQueryAtOffset(XML.indexOf("select 1"))
                navigation.selectQueryAtOffset(XML.indexOf("select 2"))
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("second", selected(editor))
        } finally { Disposer.dispose(editor) }
    }

    fun testSearchUsesUncommittedSourceAndDisposedRequestDoesNothing() {
        val file = myFixture.addFileToProject("latest.glue_sql", XML).virtualFile
        val editor = QueryDesignEditorProvider().createEditor(project, file)
        try {
            UIUtil.dispatchAllInvocationEvents()
            val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)!!
            val latest = XML.replace("second", "latest")
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) { document.setText(latest) }
            withoutWriteIntent { (editor as QueryEditorNavigation).selectQueryAtOffset(latest.indexOf("select 2")) }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("latest", selected(editor))
            assertEquals(latest, document.text)
            withoutWriteIntent {
                (editor as QueryEditorNavigation).selectQuery("first")
                Disposer.dispose(editor)
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("latest", selected(editor))
        } finally { if (!Disposer.isDisposed(editor)) Disposer.dispose(editor) }
    }

    fun testUnsafeModalDefersSearchUntilClosed() {
        val file = myFixture.addFileToProject("modal.glue_sql", XML).virtualFile
        val editor = QueryDesignEditorProvider().createEditor(project, file)
        val modal = Any()
        try {
            UIUtil.dispatchAllInvocationEvents()
            val guard = com.intellij.openapi.application.TransactionGuard.getInstance() as
                com.intellij.openapi.application.TransactionGuardImpl
            guard.performActivity(false) { com.intellij.openapi.application.impl.LaterInvocator.enterModal(modal) }
            try {
                assertFalse(guard.isWriteSafeModality(com.intellij.openapi.application.ModalityState.current()))
                withoutWriteIntent { (editor as QueryEditorNavigation).selectQueryAtOffset(XML.indexOf("select 2")) }
                UIUtil.dispatchAllInvocationEvents()
                assertEquals("first", selected(editor))
            } finally { com.intellij.openapi.application.impl.LaterInvocator.leaveModal(modal) }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("second", selected(editor))
        } finally { Disposer.dispose(editor) }
    }

    fun testLegacyFlagCannotEnableServiceEditorsWithoutWriteIntent() {
        val settings = com.look4counter.modeleditor.ModelEditorSettings.getInstance(project)
        val previous = settings.state
        settings.loadState(previous.copy(noUseGlueUmlAdFile = true))
        val text = """<service name="found-service" initial="Found" xmlns="http://www.poscoict.com/glueframework/service"><activity name="Found" class="sample.Found"><transition value="end"/></activity></service>"""
        val file = myFixture.addFileToProject("found-service.xml", text).virtualFile
        try {
            withoutWriteIntent {
                assertFalse(settings.state.noUseGlueUmlAdFile)
                assertFalse(ModelDesignEditorProvider().accept(project, file))
                assertFalse(com.look4counter.modeleditor.ModelSourceEditorProvider().accept(project, file))
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(text, com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)!!.text)
        } finally {
            settings.loadState(previous)
        }
    }

    fun testBothSourceProvidersCanCreateWithoutWriteIntent() {
        val query = myFixture.addFileToProject("source.glue_sql", XML).virtualFile
        val model = myFixture.addFileToProject("source.glue_uml_ad", "<GlueDiagram/>").virtualFile
        val editors = mutableListOf<FileEditor>()
        try {
            withoutWriteIntent {
                editors += com.look4counter.queryeditor.QuerySourceEditorProvider().createEditor(project, query)
                editors += com.look4counter.modeleditor.ModelSourceEditorProvider().createEditor(project, model)
            }
            assertEquals(2, editors.size)
            assertTrue(editors.all { it.isValid })
        } finally { editors.forEach(Disposer::dispose) }
    }

    fun testModelReferenceOpenDefersBeforeFileManager() {
        val targetFile = myFixture.addFileToProject("referenced.glue_uml_ad", "<GlueDiagram/>").virtualFile
        var opens = 0
        val manager = object : com.intellij.mock.Mock.MyFileEditorManager() {
            override fun openFile(file: com.intellij.openapi.vfs.VirtualFile, focusEditor: Boolean): Array<FileEditor> {
                assertTrue(ApplicationManager.getApplication().isWriteIntentLockAcquired)
                assertEquals(targetFile, file)
                opens++
                return emptyArray()
            }
            override fun setSelectedEditor(file: com.intellij.openapi.vfs.VirtualFile, editorTypeId: String) = Unit
        }
        project.replaceService(com.intellij.openapi.fileEditor.FileEditorManager::class.java, manager, testRootDisposable)
        withoutWriteIntent {
            assertTrue(com.look4counter.modeleditor.ServiceFiles.open(project, "referenced-service"))
            assertEquals(0, opens)
        }
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(1, opens)
    }

    fun testReadOnlyQueryNavigationAndInvalidatedFileCancellation() {
        val file = myFixture.addFileToProject("readonly.glue_sql", XML).virtualFile
        val editor = QueryDesignEditorProvider().createEditor(project, file)
        try {
            UIUtil.dispatchAllInvocationEvents()
            val document = com.intellij.openapi.fileEditor.FileDocumentManager.getInstance().getDocument(file)!!
            document.setReadOnly(true)
            withoutWriteIntent { (editor as QueryEditorNavigation).selectQuery("second") }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("second", selected(editor))
            assertEquals(XML, document.text)
            document.setReadOnly(false)
            withoutWriteIntent { (editor as QueryEditorNavigation).selectQuery("first") }
            com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) { file.delete(this) }
            UIUtil.dispatchAllInvocationEvents()
            assertFalse(editor.isValid)
            assertEquals("second", selected(editor))
        } finally { Disposer.dispose(editor) }
    }

    fun testQueryOpenEntryPointsDeferBeforeFileManagerAndCommit() {
        val file = myFixture.addFileToProject("open.glue_sql", XML).virtualFile
        val index = com.look4counter.queryeditor.queryindex.QueryIdIndexService.getInstance(project)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("쿼리 수집", { !index.isUpdating }, 10)
        val editor = QueryDesignEditorProvider().createEditor(project, file)
        var opens = 0
        val manager = object : com.intellij.mock.Mock.MyFileEditorManager() {
            override fun openFile(target: com.intellij.openapi.vfs.VirtualFile, focusEditor: Boolean): Array<FileEditor> {
                assertTrue(ApplicationManager.getApplication().isWriteIntentLockAcquired)
                assertEquals(file, target)
                opens++
                return arrayOf(editor)
            }
            override fun setSelectedEditor(target: com.intellij.openapi.vfs.VirtualFile, editorTypeId: String) = Unit
        }
        project.replaceService(com.intellij.openapi.fileEditor.FileEditorManager::class.java,
            manager, testRootDisposable)
        try {
            withoutWriteIntent {
                assertTrue(com.look4counter.queryeditor.QueryNavigation.open(project, "second", file))
                assertEquals(0, opens)
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("second", selected(editor))
            withoutWriteIntent {
                assertTrue(com.look4counter.queryeditor.QueryNavigation.open(project,
                    com.look4counter.queryeditor.QueryLocation("first", file, XML.indexOf("select 1"))))
            }
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("first", selected(editor))
            assertEquals(2, opens)
        } finally { Disposer.dispose(editor) }
    }

    companion object {
        private const val XML = """<queryMap><query id="first"><![CDATA[select 1]]></query><query id="second"><![CDATA[select 2]]></query></queryMap>"""
    }
}
