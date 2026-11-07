package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.replaceService
import com.look4counter.modeleditor.*

class ModelServiceDirectEditorTest : BasePlatformTestCase() {

    fun testSourceConflictRejectsCommitAndRollbackReloadsLatestSource() {
        val file = myFixture.addFileToProject("conflict-service.xml", xml()).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        WriteCommandAction.runWriteCommandAction(project) {
            view.rootTag!!.subTags.first { it.getAttributeValue("name") == "Router" }.setAttribute("name", "Pending")
        }
        adapter.markPendingChanges()
        val latest = xml().replace("sample.Router", "sample.Latest")
        WriteCommandAction.runWriteCommandAction(project) { document.setText(latest) }
        PsiDocumentManager.getInstance(project).commitDocument(document)
        assertSame(view, adapter.read(source(file)))
        WriteCommandAction.runWriteCommandAction(project) {
            try {
                adapter.apply(view)
                fail("Source 변경과 충돌하는 Commit을 거부해야 합니다.")
            } catch (error: IllegalArgumentException) {
                assertTrue(error.message.orEmpty().contains("Source XML"))
            }
        }
        assertEquals(latest, document.text)
        assertTrue(adapter.hasPendingChanges())
        assertTrue(ModelDiagramParser.parse(view).nodes.any { it.name == "Pending" })
        adapter.rollback()
        val restored = ModelDiagramParser.parse(adapter.read(source(file)))
        assertFalse(adapter.hasPendingChanges())
        assertEquals("sample.Latest", restored.nodes.single { it.name == "Router" }.className)
        assertFalse(restored.nodes.any { it.name == "Pending" })
        assertEquals(latest, document.text)
    }

    fun testPendingChangeQueriesReuseComparisonUntilViewChanges() {
        val file = myFixture.addFileToProject("cached-service.xml", xml()).virtualFile
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        val child = view.rootTag!!.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Router" }
        WriteCommandAction.runWriteCommandAction(project) { DiagramXmlText.write(child, "name", "Changed") }
        adapter.markPendingChanges()
        val changed = adapter.changeHighlights()
        assertFalse(changed.isEmpty)
        repeat(100) {
            assertTrue(adapter.hasPendingChanges())
            assertSame(changed, adapter.changeHighlights())
        }
        WriteCommandAction.runWriteCommandAction(project) { DiagramXmlText.write(child, "name", "Router") }
        val restored = adapter.changeHighlights()
        assertNotSame(changed, restored)
        assertTrue(restored.isEmpty)
        assertFalse(adapter.hasPendingChanges())
        WriteCommandAction.runWriteCommandAction(project) { DiagramXmlText.write(child, "name", "Changed again") }
        assertTrue(adapter.hasPendingChanges())
        adapter.rollback()
        assertFalse(adapter.hasPendingChanges())
        assertTrue(adapter.changeHighlights().isEmpty)
    }

    private lateinit var previous: ModelEditorState
    override fun setUp() {
        super.setUp()
        val settings = ModelEditorSettings.getInstance(project)
        previous = settings.state
        settings.loadState(ModelEditorState())
    }
    override fun tearDown() {
        try { ModelEditorSettings.getInstance(project).loadState(previous) } finally { super.tearDown() }
    }
    private fun xml() = """<service name="original-service" initial="Router" xmlns="http://www.poscoict.com/glueframework/service" extra="keep">
        <!-- keep this comment -->
        <transaction-manager id="test-tx" commit="true" custom="tx"/>
        <activity name="Router" class="sample.Router" extra="activity">
            <transition name="go" value="Next" custom="transition"/>
            <property name="sql-key" value="sample.select" custom="property"/>
        </activity>
        <activity name="Next" class="sample.Next"><transition value="end"/></activity>
        <activity name="Detached" class="sample.Detached"/>
        <extra-element keep="yes"/>
    </service>""".trimIndent()
    private fun source(file: com.intellij.openapi.vfs.VirtualFile) = PsiManager.getInstance(project).findFile(file) as XmlFile


    fun testServiceProjectionAndGeometryChangesDoNotModifySourceOrCreateModelFiles() {
        val file = myFixture.addFileToProject("services/direct-service.xml", xml()).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        val original = document.text
        val model = ModelDiagramParser.parse(view)
        assertTrue(model.wires.all { it.explicitContacts })
        assertFalse(view.text.contains("explicitContacts"))
        assertEquals(listOf("Router", "Next", "Detached"), model.nodes.filter { it.isActivity }.map { it.name })
        assertEquals(3, model.wires.size)
        assertEquals("Router", model.wires.first().end!!.name)
        assertEquals(DiagramNodeKind.FinalState, model.wires.last().end!!.kind)
        WriteCommandAction.runWriteCommandAction(project) {
            val child = view.rootTag!!.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Router" }
            child.setAttribute("location", "333,444")
            child.setAttribute("size", "230,-1")
            adapter.apply(view)
        }
        assertEquals(original, document.text)
        assertFalse(FileDocumentManager.getInstance().isDocumentUnsaved(document))
        assertEquals(model, ModelDiagramParser.parse(ServiceDiagramAdapter(project, file).read(source(file))))
        assertNull(file.parent.findChild("direct.glue_uml_ad"))
    }

    fun testRenameConditionsPropertiesAndTransactionsUpdateOnlyServiceSemantics() {
        val file = myFixture.addFileToProject("edit-service.xml", xml()).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        WriteCommandAction.runWriteCommandAction(project) {
            val root = view.rootTag!!
            val router = root.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Router" }
            DiagramXmlText.write(router, "name", "Renamed")
            val property = router.subTags.single { DiagramXmlText.read(it, "key") == "sql-key" }
            DiagramXmlText.write(property, "value", "한글 & <value>\nline")
            root.subTags.filter { it.localName == "wire" }[1].setAttribute("guardcondition", "changed")
            root.subTags.single { it.localName == "property" }.setAttribute("value", "false")
            adapter.apply(view)
        }
        PsiDocumentManager.getInstance(project).commitDocument(document)
        val root = source(file).rootTag!!
        assertEquals("original-service", root.getAttributeValue("name"))
        assertEquals("Renamed", root.getAttributeValue("initial"))
        assertEquals("keep", root.getAttributeValue("extra"))
        assertTrue(document.text.contains("keep this comment"))
        assertTrue(document.text.contains("extra-element"))
        val activity = root.subTags.first { it.localName == "activity" }
        assertEquals("activity", activity.getAttributeValue("extra"))
        val transition = activity.subTags.single { it.localName == "transition" }
        assertEquals("changed", transition.getAttributeValue("name"))
        assertEquals("transition", transition.getAttributeValue("custom"))
        val property = activity.subTags.single { it.localName == "property" }
        assertEquals("한글 & <value>\nline", DiagramXmlText.read(property, "value"))
        assertEquals("property", property.getAttributeValue("custom"))
        val tx = root.subTags.single { it.localName == "transaction-manager" }
        assertEquals("false", tx.getAttributeValue("commit"))
        assertEquals("tx", tx.getAttributeValue("custom"))
        assertFalse(document.text.contains("location="))
        assertFalse(document.text.contains("bendpoint"))
    }



    fun testDeletingActivityAndAddingAndRemovingPropertiesKeepsProjectionMappingsValid() {
        val file = myFixture.addFileToProject("delete-service.xml", xml()).virtualFile
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        WriteCommandAction.runWriteCommandAction(project) {
            val root = view.rootTag!!
            root.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Detached" }.delete()
            val router = root.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Router" }
            router.subTags.first { it.getAttributeValue("key") == "sql-key" }.delete()
            router.addSubTag(com.intellij.psi.XmlElementFactory.getInstance(project).createTagFromText("<property key=\"new-key\" value=\"first\"/>"), false)
            adapter.apply(view)
        }
        WriteCommandAction.runWriteCommandAction(project) {
            val router = view.rootTag!!.subTags.first { it.localName == "child" && it.getAttributeValue("name") == "Router" }
            router.subTags.first { it.getAttributeValue("key") == "new-key" }.setAttribute("value", "second")
            adapter.apply(view)
        }
        val root = source(file).rootTag!!
        assertEquals(2, root.subTags.count { it.localName == "activity" })
        val actual = root.subTags.first { it.localName == "activity" }.subTags.single { it.localName == "property" }
        assertEquals("new-key", actual.getAttributeValue("name"))
        assertEquals("second", actual.getAttributeValue("value"))
    }

    fun testLegacyTrueUsesModelFilesAndDoesNotEnableServiceEditors() {
        val model = myFixture.addFileToProject("flows/target.glue_uml_ad", "<GlueDiagram/>").virtualFile
        val service = myFixture.addFileToProject("services/target-service.xml", xml()).virtualFile
        val other = myFixture.addFileToProject("services/other.xml", "<root/>").virtualFile
        val settings = ModelEditorSettings.getInstance(project)
        settings.loadState(ModelEditorState(serviceDirectory = "services", noUseGlueUmlAdFile = true))
        assertFalse(settings.state.noUseGlueUmlAdFile)
        assertEquals(model, ServiceFiles.resolve(project, "target-service"))
        assertEquals(listOf("other", "target-service"), ServiceFiles.names(project))
        assertFalse(ModelDesignEditorProvider().accept(project, service))
        assertFalse(ModelSourceEditorProvider().accept(project, service))
        assertTrue(ModelDesignEditorProvider().accept(project, model))
        assertTrue(ModelSourceEditorProvider().accept(project, model))
        assertFalse(ModelDesignEditorProvider().accept(project, other))
        var opened: com.intellij.openapi.vfs.VirtualFile? = null
        var selected = ""
        val manager = object : com.intellij.mock.Mock.MyFileEditorManager() {
            override fun openFile(file: com.intellij.openapi.vfs.VirtualFile, focusEditor: Boolean): Array<FileEditor> { opened = file; return emptyArray() }
            override fun setSelectedEditor(file: com.intellij.openapi.vfs.VirtualFile, editorTypeId: String) { selected = editorTypeId }
        }
        project.replaceService(FileEditorManager::class.java, manager, testRootDisposable)
        assertTrue(ServiceFiles.open(project, "target-service.xml"))
        assertEquals(model, opened)
        assertEquals("glue-model-design", selected)
        settings.loadState(ModelEditorState())
        assertEquals(model, ServiceFiles.resolve(project, "target-service"))
        assertFalse(ModelDesignEditorProvider().accept(project, service))
        assertEquals(xml(), FileDocumentManager.getInstance().getDocument(service)!!.text)
    }

    fun testForeignNamespaceMetadataAndParallelAndLoopRoutesArePreserved() {
        val text = xml().replace("<extra-element keep=\"yes\"/>", """<x:activity xmlns:x="urn:extension" keep="yes"/>""")
            .replace("<property name=\"sql-key\"", """<x:property xmlns:x="urn:extension" name="opaque"/><property name="sql-key"""")
            .replace("<transition name=\"go\" value=\"Next\" custom=\"transition\"/>", """<transition name="go" value="Next"/><transition name="other" value="Next"/><transition name="loop" value="Router"/>""")
        val file = myFixture.addFileToProject("routes-service.xml", text).virtualFile
        val adapter = ServiceDiagramAdapter(project, file)
        val view = adapter.read(source(file))
        val model = ModelDiagramParser.parse(view)
        assertEquals(3, model.nodes.count { it.isActivity })
        assertEquals(3, model.wires.count { it.start?.name == "Router" })
        assertTrue(model.wires.single { it.condition == "loop" }.bendpoints.isNotEmpty())
        model.wires.forEach { wire ->
            val path = DiagramGeometry.path(wire)
            assertTrue(path.size >= 2)
            path.zipWithNext().forEach { (a, b) -> assertTrue("직각이 아닌 표시 경로: $a → $b", a.x == b.x || a.y == b.y) }
        }
        assertEquals(emptySet<Pair<Int, Int>>(), sharedWireSegments(model.wires.map(DiagramGeometry::path)))
        assertEquals(model, ModelDiagramParser.parse(ServiceDiagramAdapter(project, file).read(source(file))))
        WriteCommandAction.runWriteCommandAction(project) {
            val property = view.rootTag!!.subTags.first { it.getAttributeValue("name") == "Router" }.subTags.first { it.getAttributeValue("key") == "sql-key" }
            property.setAttribute("value", "changed")
            adapter.apply(view)
        }
        val actual = source(file).rootTag!!
        assertEquals(1, actual.subTags.count { it.localName == "activity" && it.namespace == "urn:extension" })
        val router = actual.subTags.first { it.localName == "activity" && it.namespace == ServiceDiagramAdapter.SERVICE_NAMESPACE }
        assertEquals(1, router.subTags.count { it.localName == "property" && it.namespace == "urn:extension" })
    }



}
