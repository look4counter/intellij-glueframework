package com.look4counter

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.EditorTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Container
import javax.swing.JList
import javax.swing.JTextField
import com.intellij.ui.EditorTextField

class QueryTabNavigationTest : BasePlatformTestCase() {
    private val xml = """
        <queryMap desc="queries">
          <query id="first"><![CDATA[select 1]]></query>
          <!-- between queries -->
          <query id="second"><![CDATA[select 2]]></query>
          <query id="third"><![CDATA[select 3]]></query>
        </queryMap>
    """.trimIndent()
    private var count = 0
    private data class Views(val design: FileEditor, val source: TextEditor, val list: JList<*>)

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private fun open(text: String = xml): Views {
        val file = myFixture.addFileToProject("tabs-${count++}.glue_sql", text).virtualFile
        val providers = FileEditorProviderManager.getInstance().getProviderList(project, file)
        val design = providers.single { it.editorTypeId == "glue-query-design" }.createEditor(project, file)
        Disposer.register(testRootDisposable, design)
        val source = providers.single { it.editorTypeId == "glue-query-source" }.createEditor(project, file) as TextEditor
        Disposer.register(testRootDisposable, source)
        EditorTestUtil.waitForLoading(source.editor)
        drain()
        return Views(design, source, descendants(design.component).filterIsInstance<JList<*>>().single { it.name == "query.list" })
    }

    private fun drain() = UIUtil.dispatchAllInvocationEvents()
    private fun sql(views: Views): EditorTextField = descendants(views.design.component)
        .filterIsInstance<EditorTextField>().single { it.name == "query.sql" }.also {
            it.ensureWillComputePreferredSize()
            it.preferredSize
        }
    private fun toSource(views: Views) {
        views.design.deselectNotify()
        views.source.selectNotify()
        drain()
    }
    private fun toDesign(views: Views, offset: Int) {
        views.source.editor.caretModel.moveToOffset(offset)
        views.source.deselectNotify()
        views.design.selectNotify()
        drain()
    }
    private fun queryStarts(views: Views): List<Int> {
        val manager = PsiDocumentManager.getInstance(project)
        manager.commitDocument(views.source.editor.document)
        return (manager.getPsiFile(views.source.editor.document) as XmlFile).rootTag!!.subTags
            .filter { it.localName == "query" }.map { tag ->
                tag.value.textElements.firstOrNull()?.let { it.textRange.startOffset + it.displayToPhysical(0) }
                    ?: tag.textRange.startOffset
            }
    }

    fun testDesignSelectionMovesSourceCaretAndRoundTripUsesCurrentQuery() {
        val views = open()
        val original = views.source.editor.document.text
        views.list.selectedIndex = 1
        drain()
        toSource(views)
        assertEquals(queryStarts(views)[1], views.source.editor.caretModel.offset)
        toDesign(views, original.indexOf("select 3") + 3)
        assertEquals(2, views.list.selectedIndex)
        toSource(views)
        assertEquals(original.indexOf("select 3") + 3, views.source.editor.caretModel.offset)
        assertEquals(original, views.source.editor.document.text)
    }

    fun testSourceAttributesCdataClosingTagsAndOutsideQueryFallback() {
        val views = open()
        val text = views.source.editor.document.text
        for (offset in listOf(text.indexOf("second"), text.indexOf("select 2"), text.indexOf("</query>", text.indexOf("second")) + 3)) {
            toDesign(views, offset)
            assertEquals("offset=$offset", 1, views.list.selectedIndex)
        }
        for (offset in listOf(0, text.indexOf("between queries"), text.indexOf("</queryMap>"), text.length)) {
            toDesign(views, offset)
            assertEquals("fallback offset=$offset", 0, views.list.selectedIndex)
        }
    }

    fun testDuplicateAndEmptyIdsAreMatchedByXmlElement() {
        for (id in listOf("same", "")) {
            val views = open("<queryMap><query id=\"$id\">one</query><query id=\"$id\">two</query></queryMap>")
            views.list.selectedIndex = 1
            drain()
            toSource(views)
            assertEquals(queryStarts(views)[1], views.source.editor.caretModel.offset)
            toDesign(views, views.source.editor.document.text.indexOf("two"))
            assertEquals(1, views.list.selectedIndex)
        }
    }

    fun testPendingDesignEditAndUncommittedSourceEditUseLatestDocument() {
        val views = open()
        views.list.selectedIndex = 1
        drain()
        val id = descendants(views.design.component).filterIsInstance<JTextField>().single { it.name == "query.id" }
        id.text = "renamed-second"
        toSource(views)
        assertTrue(views.source.editor.document.text.contains("renamed-second"))
        assertEquals(queryStarts(views)[1], views.source.editor.caretModel.offset)
        WriteCommandAction.runWriteCommandAction(project) {
            views.source.editor.document.setText(xml.replace("select 3", "select a_much_longer_value"))
        }
        toDesign(views, views.source.editor.document.text.indexOf("a_much_longer_value") + 5)
        assertEquals(2, views.list.selectedIndex)
    }

    fun testMissingSelectionReadOnlyAndFilesRemainIndependent() {
        val first = open()
        val second = open()
        first.list.selectedIndex = 2
        second.list.selectedIndex = 1
        drain()
        WriteCommandAction.runWriteCommandAction(project) { first.source.file!!.isWritable = false }
        try {
            toSource(first)
            assertEquals(queryStarts(first)[2], first.source.editor.caretModel.offset)
            toSource(second)
            assertEquals(queryStarts(second)[1], second.source.editor.caretModel.offset)
            toDesign(first, 0)
            assertEquals(0, first.list.selectedIndex)
            first.list.clearSelection()
            toSource(first)
            assertEquals(queryStarts(first)[0], first.source.editor.caretModel.offset)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { first.source.file!!.isWritable = true }
        }
    }

    fun testEmptyMalformedAndDeletedQueriesDoNotThrow() {
        for (text in listOf("", "<queryMap/>", "<queryMap><query")) {
            val views = open(text)
            toSource(views)
            toDesign(views, text.length)
            assertEquals(-1, views.list.selectedIndex)
            assertEquals(text, views.source.editor.document.text)
        }
        val views = open()
        views.list.selectedIndex = 2
        drain()
        WriteCommandAction.runWriteCommandAction(project) { views.source.editor.document.setText("<queryMap/>") }
        toSource(views)
        toDesign(views, 0)
        assertEquals(-1, views.list.selectedIndex)
    }

    fun testSwitchWithoutWriteIntentAndDisposeBeforeDeferredNavigation() {
        val views = open()
        views.list.selectedIndex = 1
        drain()
        val app = ApplicationManager.getApplication() as com.intellij.openapi.application.impl.ApplicationImpl
        app.threadingSupport.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack {
            views.design.deselectNotify()
            views.source.selectNotify()
        }
        drain()
        assertEquals(queryStarts(views)[1], views.source.editor.caretModel.offset)
        app.threadingSupport.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack {
            views.source.deselectNotify()
            views.design.selectNotify()
            Disposer.dispose(views.design)
            Disposer.dispose(views.source)
        }
        drain()
    }

    fun testSqlCharacterOffsetsRoundTripAcrossCdataEntitiesAndSplitText() {
        for ((body, expectedSql) in listOf(
            "<![CDATA[select 한글\nfrom items where name = 'tail']] >".replace("]] >", "]]>") to "select 한글\nfrom items where name = 'tail'",
            "select a &lt; b and x = &amp;tail" to "select a < b and x = &tail",
            "<![CDATA[select ]]]]><![CDATA[> tail]]>" to "select ]]> tail",
            "select &#xD55C;&#xAE00; tail" to "select 한글 tail"
        )) {
            val views = open("<queryMap><query id=\"a\">$body</query></queryMap>")
            val field = sql(views)
            val sourceOffset = views.source.editor.document.text.indexOf("tail") + 2
            toDesign(views, sourceOffset)
            assertEquals(expectedSql, field.text)
            assertEquals(expectedSql.indexOf("tail") + 2, field.editor!!.caretModel.offset)
            repeat(3) {
                toSource(views)
                assertEquals(sourceOffset, views.source.editor.caretModel.offset)
                toDesign(views, sourceOffset)
                assertEquals(expectedSql.indexOf("tail") + 2, field.editor!!.caretModel.offset)
            }
        }
    }

    fun testDesignEntityAndEndOffsetsMapToSourceCharacters() {
        val views = open("<queryMap><query id=\"a\">select a &lt; b &amp; c</query></queryMap>")
        val field = sql(views)
        for (offset in listOf(field.text.indexOf('<'), field.text.indexOf('&'), field.text.length)) {
            field.editor!!.caretModel.moveToOffset(offset)
            toSource(views)
            val sourceOffset = views.source.editor.caretModel.offset
            if (offset < field.text.length) {
                val entity = if (field.text[offset] == '<') "&lt;" else "&amp;"
                assertTrue(views.source.editor.document.text.substring(sourceOffset).startsWith(entity))
            }
            toDesign(views, sourceOffset)
            assertEquals(offset, field.editor!!.caretModel.offset)
        }
    }

    fun testPendingSqlEditsUseLatestCdataAndPreserveCaret() {
        val views = open()
        val field = sql(views)
        val edited = "select 한글\nfrom items where x = 'tail' and y = ']]>'"
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText(edited) }
        val offset = edited.indexOf("tail") + 2
        field.editor!!.caretModel.moveToOffset(offset)
        toSource(views)
        val sourceOffset = views.source.editor.document.text.indexOf("tail") + 2
        assertEquals(sourceOffset, views.source.editor.caretModel.offset)
        toDesign(views, sourceOffset)
        assertEquals(offset, field.editor!!.caretModel.offset)
        drain()
        assertEquals(offset, field.editor!!.caretModel.offset)
    }

    fun testQuerySpecificCaretsAndNonSqlSourceLocationsArePreserved() {
        val views = open()
        val field = sql(views)
        field.editor!!.caretModel.moveToOffset(3)
        views.list.selectedIndex = 1
        drain()
        field.editor!!.caretModel.moveToOffset(6)
        views.list.selectedIndex = 0
        drain()
        assertEquals(3, field.editor!!.caretModel.offset)
        toDesign(views, views.source.editor.document.text.indexOf("second"))
        assertEquals(6, field.editor!!.caretModel.offset)
        val original = views.source.editor.document.text
        toSource(views)
        assertEquals(original.indexOf("select 2") + 6, views.source.editor.caretModel.offset)
        assertEquals(original, views.source.editor.document.text)
    }

    fun testUncommittedSourceSqlRefreshRestoresMappedCharacterInsteadOfEnd() {
        val views = open()
        val field = sql(views)
        field.editor!!.caretModel.moveToOffset(2)
        WriteCommandAction.runWriteCommandAction(project) {
            views.source.editor.document.setText(xml.replace("select 1", "select a_longer_value from items"))
        }
        val offset = views.source.editor.document.text.indexOf("longer") + 3
        toDesign(views, offset)
        assertEquals(field.text.indexOf("longer") + 3, field.editor!!.caretModel.offset)
        drain()
        toSource(views)
        assertEquals(offset, views.source.editor.caretModel.offset)
    }
}
