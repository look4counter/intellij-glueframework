package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase

class QuerySaveListenerTest : BasePlatformTestCase() {
    private fun save(name: String, text: String): VirtualFile {
        val file = myFixture.addFileToProject(name, "").virtualFile
        val manager = FileDocumentManager.getInstance()
        val document = manager.getDocument(file)!!
        WriteCommandAction.runWriteCommandAction(project) { document.setText(text) }
        manager.saveDocument(document)
        assertEquals(document.text, file.contentsToByteArray().toString(Charsets.UTF_8).replace("\r\n", "\n"))
        return file
    }

    fun testBothUserExamplesTrimOnSourceOnlySaveWithDuplicateIds() {
        val first = "<query id=\"sample.emp.delete\" desc=\"EMP 삭제\" resultType=\"\" isNamed=\"false\">\n" +
            "        <![CDATA[delete from emp\nwhere empno=?]]>\n    </query>"
        val second = "<query id=\"sample.emp.delete\" desc=\"EMP 삭제\" resultType=\"\" isNamed=\"false\"><![CDATA[\n" +
            "        delete from emp\nwhere empno=?\n]]></query>"
        val trimmed = "<query id=\"sample.emp.delete\" desc=\"EMP 삭제\" resultType=\"\" isNamed=\"false\">" +
            "<![CDATA[delete from emp\nwhere empno=?]]></query>"
        val file = save("examples.glue_sql", "<queryMap>\n    $first\n    $second\n</queryMap>")
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        assertEquals("<queryMap>\n    $trimmed\n    $trimmed\n</queryMap>", document.text)
        val stamp = document.modificationStamp
        FileDocumentManager.getInstance().saveDocument(document)
        assertEquals(stamp, document.modificationStamp)
    }

    fun testInternalWhitespaceNamespaceAndCdataTerminatorArePreserved() {
        val file = save("escaped.glue_sql", "<q:queryMap xmlns:q=\"urn:query\" desc=\"keep\">" +
            "<!-- keep --><q:query id=\"x\"> \n\tselect '&lt;&amp;', ']]&gt;'\n  from emp \t</q:query>" +
            "<query xmlns=\"other\"> keep </query></q:queryMap>")
        assertEquals("<q:queryMap xmlns:q=\"urn:query\" desc=\"keep\">" +
            "<!-- keep --><q:query id=\"x\"><![CDATA[select '<&', ']]]]><![CDATA[>'\n  from emp]]></q:query>" +
            "<query xmlns=\"other\"> keep </query></q:queryMap>",
            FileDocumentManager.getInstance().getDocument(file)!!.text)
    }

    fun testWhitespaceOnlySqlBecomesEmpty() {
        val file = save("empty-sql.glue_sql", "<queryMap><query><![CDATA[ \n\t ]]></query><query/></queryMap>")
        assertEquals("<queryMap><query><![CDATA[]]></query><query/></queryMap>",
            FileDocumentManager.getInstance().getDocument(file)!!.text)
    }

    fun testUnrelatedMalformedAndMixedContentAreNotRewritten() {
        val cases = mapOf(
            "other.xml" to "<queryMap><query><![CDATA[ select 1 ]]></query></queryMap>",
            "wrong-root.glue_sql" to "<other><query> select 1 </query></other>",
            "malformed.glue_sql" to "<queryMap><query><![CDATA[ select 1 ]]></queryMap>",
            "mixed.glue_sql" to "<queryMap><query> a<!-- keep --><if/> b </query></queryMap>",
        )
        cases.forEach { (name, text) ->
            val file = save(name, text)
            assertEquals(text, FileDocumentManager.getInstance().getDocument(file)!!.text)
        }
    }

    fun testReadOnlyDocumentIsNotRewritten() {
        val text = "<queryMap><query><![CDATA[ select 1 ]]></query></queryMap>"
        val file = myFixture.addFileToProject("readonly.glue_sql", text).virtualFile
        val document = FileDocumentManager.getInstance().getDocument(file)!!
        document.setReadOnly(true)
        try {
            com.look4counter.queryeditor.QuerySaveListener().beforeDocumentSaving(document)
            assertEquals(text, document.text)
        } finally {
            document.setReadOnly(false)
        }
    }
}
