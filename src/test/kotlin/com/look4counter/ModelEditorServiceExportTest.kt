package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.modeleditor.*
import org.jdom.Element
import java.io.StringReader

class ModelEditorServiceExportTest : BasePlatformTestCase() {
    private lateinit var previous: ModelEditorState
    override fun setUp() {
        super.setUp()
        previous = ModelEditorSettings.getInstance(project).state
        ModelEditorSettings.getInstance(project).loadState(ModelEditorState())
    }
    override fun tearDown() {
        try { ModelEditorSettings.getInstance(project).loadState(previous) } finally { super.tearDown() }
    }
    private fun sample(): String = javaClass.getResource("/model/sample11.glue_uml_ad")!!.readText()
    private fun convert(text: String): String {
        val xml = PsiFileFactory.getInstance(project).createFileFromText("sample11.glue_uml_ad", GlueUmlAdFileType.INSTANCE, text) as XmlFile
        return ModelServiceXmlExporter.export(xml, "sample11")
    }
    private fun parse(text: String): Element = com.intellij.openapi.util.JDOMUtil.load(StringReader(text))
    private fun children(element: Element, name: String) = element.getChildren(name, element.namespace)

    fun testUserExampleExportPreservesExactActivityTransitionAndPropertyOrder() {
        val propertySources = listOf(
            "class=com.Test|description=설명|sql-key=sample.emp.select.named|service-name=sample2-service",
            "class=com.poscoict.glueframework.biz.activity.GlueJdbcInsert|chk-name=chk_insert|dao=test-dao|sql-key=sample.emp.insert|param-count=4|param0=empno_insert|param1=ename_insert|param2=sal_insert|param3=deptno_insert|result-key=insertCnt|description=설명",
            "class=com.Test|chk-name=chk|dao=test-dao|sql-key=sample.emp.delete|param-count=1|param0=EMPNO|description=설명",
            "class=com.poscoict.glueframework.biz.activity.GlueJdbcModify|chk-name=chk|dao=test-dao|param-count=3|result-key=updateCnt|sql-key=sample.emp.update|param0=SAL|param1=ENAME|param2=EMPNO|description=",
            "class=com.poscoict.glueframework.biz.activity.GlueJdbcSearch|dao=test-dao|param-count=1|result-key=EmpList|sql-key=sample.emp.select|param0=deptno|description=",
            "class=sample.activity.SearchEmp2|description=hjk",
        ).map { source -> source.split('|').map { it.substringBefore('=') to it.substringAfter('=') } }
        val names = listOf("Router", "등록", "삭제", "수정", "Search[조회]", "SeachEmp-SQL2")
        val ids = listOf("2", "8", "9", "10", "6152", "6181")
        val inputs = listOf("0 12", "4", "2", "3", "1 6 7 8", "10 11")
        val outputs = listOf("5 1 2 4 3 10 11", "7", "6", "8", "9", "12")
        fun refs(indices: String) = indices.split(' ').joinToString(" ") { "//@wire.$it" }
        val xml = buildString {
            append("""<Glue:GlueDiagram xmlns:Glue="http://www.glueframework.com/activitydiagram/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">""")
            append("""<child xsi:type="Glue:InitialState" id="0" output="//@wire.0"/>""")
            append("""<child xsi:type="Glue:FinalState" id="1" input="//@wire.5 //@wire.9"/>""")
            names.forEachIndexed { i, name ->
                append("""<child xsi:type="Glue:Activity" id="${ids[i]}" name="$name" input="${refs(inputs[i])}" output="${refs(outputs[i])}">""")
                propertySources[i].forEach { (key, value) -> append("""<property key="$key" value="$value"/>""") }
                append("</child>")
            }
            append("""<property key="test-tx" value="true"/><property key="description" value="説明"/>""")
            listOf("success", "find", "delete", "update2", "insert", "success", "success", "success", "success", "success", "custom", null, null)
                .forEachIndexed { i, condition -> append("<wire id=\"$i\"" + (condition?.let { " guardcondition=\"$it\"" } ?: "") + "/>") }
            append("</Glue:GlueDiagram>")
        }
        val source = PsiFileFactory.getInstance(project).createFileFromText("sample.glue_uml_ad", GlueUmlAdFileType.INSTANCE, xml) as XmlFile
        val text = ModelServiceXmlExporter.export(source, "sample")
        val service = parse(text)
        assertEquals("sample-service", service.getAttributeValue("name"))
        assertEquals("Router", service.getAttributeValue("initial"))
        assertEquals("http://www.poscoict.com/glueframework/service", service.namespaceURI)
        assertEquals("test-tx", children(service, "transaction-manager").single().getAttributeValue("id"))
        assertEquals("true", children(service, "transaction-manager").single().getAttributeValue("commit"))
        val activities = children(service, "activity")
        assertEquals(names, activities.map { it.getAttributeValue("name") })
        val expectedTransitions = listOf(
            listOf("success" to "SeachEmp-SQL2", "find" to "Search[조회]", "custom" to "SeachEmp-SQL2", "insert" to "등록", "delete" to "삭제", "update2" to "수정"),
            listOf("success" to "Search[조회]"), listOf("success" to "Search[조회]"), listOf("success" to "Search[조회]"),
            listOf("success" to "end"), listOf("success" to "Router"),
        )
        val expectedProperties = listOf("service-name sql-key", "sql-key param-count chk-name result-key param0 param1 dao param2 param3",
            "sql-key dao chk-name param-count param0", "param-count chk-name param0 param1 result-key param2 dao sql-key",
            "result-key sql-key dao param-count param0", "")
        activities.forEachIndexed { i, activity ->
            assertEquals(propertySources[i].first().second, activity.getAttributeValue("class"))
            assertEquals(expectedTransitions[i], children(activity, "transition").map { it.getAttributeValue("name") to it.getAttributeValue("value") })
            val properties = children(activity, "property")
            assertEquals(expectedProperties[i], properties.joinToString(" ") { it.getAttributeValue("name") })
            val original = propertySources[i].toMap()
            properties.forEach { assertEquals(original[it.getAttributeValue("name")], it.getAttributeValue("value")) }
            assertEquals(List(expectedTransitions[i].size) { "transition" } + List(properties.size) { "property" }, activity.children.map { it.name })
        }
        assertEquals(text, ModelServiceXmlExporter.export(source, "sample"))
    }

    fun testUserSampleConvertsAllServiceContentsWithoutGeometry() {
        val output = convert(sample())
        assertTrue(output.startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"))
        val root = parse(output)
        assertEquals("http://www.poscoict.com/glueframework/service", root.namespaceURI)
        assertEquals("sample11-service", root.getAttributeValue("name"))
        assertEquals("분기[Router]", root.getAttributeValue("initial"))
        val tx = children(root, "transaction-manager").single()
        assertEquals("test-tx", tx.getAttributeValue("id")); assertEquals("true", tx.getAttributeValue("commit"))
        val activities = children(root, "activity")
        assertEquals(listOf("분기[Router]", "삭제[Delete]", "등록[Insert]", "수정[Modify]", "조회[Search]"), activities.map { it.getAttributeValue("name") })
        assertEquals("com.poscoict.glueframework.biz.activity.GlueDefaultRouter", activities[0].getAttributeValue("class"))
        val transitions = children(activities[0], "transition").associate { it.getAttributeValue("name") to it.getAttributeValue("value") }
        assertEquals(mapOf("success" to "end", "delete" to "삭제[Delete]", "insert" to "등록[Insert]", "update" to "수정[Modify]", "find" to "조회[Search]"), transitions)
        for (activity in activities.drop(1)) {
            val transition = children(activity, "transition").single()
            assertEquals("success", transition.getAttributeValue("name"))
            assertEquals(if (activity == activities.last()) "end" else "조회[Search]", transition.getAttributeValue("value"))
        }
        val insert = children(activities[2], "property").associate { it.getAttributeValue("name") to it.getAttributeValue("value") }
        assertEquals(mapOf("chk-name" to "chk", "dao" to "test-dao", "result-key" to "insertCnt", "sql-key" to "emp.mapper.insertEmp",
            "param-bindings" to "empno=empno_insert|ename=ename_insert|sal=sal_insert|deptno=deptno_insert"), insert)
        assertFalse(output.contains("description")); assertFalse(output.contains("bendpoint")); assertFalse(output.contains("location="))
        assertEquals(output, convert(sample()))
    }
    fun testFalseTransactionAndEscapedValuesAndDescriptionAreHandled() {
        val text = sample().replace("key=\"test-tx\" value=\"true\"", "key=\"test-tx\" value=\"false\"")
            .replace("value=\"test-dao\"", "value=\"한글 &amp; &quot;값&quot;&#10;줄\"")
            .replace("key=\"description\" value=\"\"", "key=\"description\" value=\"제외할 설명\"")
        val root = parse(convert(text))
        assertEquals("false", children(root, "transaction-manager").single().getAttributeValue("commit"))
        val properties = children(children(root, "activity")[1], "property")
        assertEquals("한글 & \"값\"\n줄", properties.single { it.getAttributeValue("name") == "dao" }.getAttributeValue("value"))
        assertFalse(convert(text).contains("제외할 설명"))
    }
    fun testInvalidGraphsAreRejectedBeforeOverwritingOutput() {
        val source = myFixture.addFileToProject("sample11.glue_uml_ad", sample()).virtualFile
        val target = myFixture.addFileToProject("services/sample11-service.xml", "keep").virtualFile
        ModelEditorSettings.getInstance(project).loadState(ModelEditorState(serviceDirectory = "services"))
        for (invalid in listOf(sample().replace("//@wire.0", "//@wire.999"), sample().replace("name=\"삭제[Delete]\"", "name=\"등록[Insert]\""),
            sample().replace("key=\"class\"", "key=\"missing-class\""), sample().replace("key=\"test-tx\" value=\"true\"", "key=\"test-tx\" value=\"bad\""), "<GlueDiagram><child")) {
            try { ModelServiceSaveListener().save(project, source, invalid); fail("잘못된 모델을 거부해야 합니다.") }
            catch (_: IllegalArgumentException) { }
            assertEquals("keep", VfsUtil.loadText(target))
        }
    }
    fun testSavingDocumentCreatesThenUpdatesOpenOutputWithoutChangingSource() {
        val source = myFixture.addFileToProject("flows/sample11.glue_uml_ad", sample()).virtualFile
        myFixture.addFileToProject("services/placeholder.txt", "")
        ModelEditorSettings.getInstance(project).loadState(ModelEditorState(serviceDirectory = "services"))
        val manager = FileDocumentManager.getInstance()
        val document = manager.getDocument(source)!!
        WriteCommandAction.runWriteCommandAction(project) { document.insertString(document.textLength, "\n") }
        manager.saveDocument(document)
        val target = source.parent.parent.findFileByRelativePath("services/sample11-service.xml")
        assertNotNull("저장 이벤트로 Service 파일을 생성해야 합니다.", target)
        assertEquals("sample11-service", parse(VfsUtil.loadText(target!!)).getAttributeValue("name"))
        val outputDocument = manager.getDocument(target)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText(document.text.replace("value=\"true\"", "value=\"false\"")) }
        val sourceText = document.text
        manager.saveDocument(document)
        assertEquals("false", children(parse(outputDocument.text), "transaction-manager").single().getAttributeValue("commit"))
        assertEquals(outputDocument.text, VfsUtil.loadText(target))
        assertEquals(sourceText, document.text)
        assertFalse(manager.isDocumentUnsaved(outputDocument))
        val lastValid = outputDocument.text
        WriteCommandAction.runWriteCommandAction(project) { document.setText("<GlueDiagram><child") }
        manager.saveDocument(document)
        assertEquals(lastValid, outputDocument.text)
        assertEquals(lastValid, VfsUtil.loadText(target))
    }
    fun testMissingDirectoryDoesNotGenerateFiles() {
        val source = myFixture.addFileToProject("sample11.glue_uml_ad", sample()).virtualFile
        assertNull(ModelServiceSaveListener().save(project, source, sample()))
        assertNull(source.parent.findChild("sample11-service.xml"))
    }
}
