package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.editor.colors.EditorColorsManager
import com.intellij.openapi.editor.ex.EditorEx
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.EditorTextField
import com.intellij.ui.JBColor
import com.intellij.util.ui.UIUtil
import java.awt.Color
import java.awt.Component
import java.awt.Container
import java.nio.file.Files
import java.nio.file.Path
import javax.swing.JList

class QuerySqlEditorTest : BasePlatformTestCase() {
    private var design: FileEditor? = null

    override fun tearDown() {
        try {
            design?.let(Disposer::dispose)
        } finally {
            super.tearDown()
        }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private fun openSql(text: String, secondQuery: Boolean = false): EditorTextField {
        fun query(id: String, sql: String) = "<query id=\"$id\"><![CDATA[${sql.replace("]]>", "]]]]><![CDATA[>")}]]></query>"
        val xml = "<queryMap>" + (if (secondQuery) query("first", "select 100") else "") + query("target", text) + "</queryMap>"
        val file = myFixture.addFileToProject("syntax.glue_sql", xml).virtualFile
        design = FileEditorProviderManager.getInstance().getProviderList(project, file)
            .single { it.editorTypeId == "glue-query-design" }.createEditor(project, file)
        UIUtil.dispatchAllInvocationEvents()
        if (secondQuery) descendants(design!!.component).filterIsInstance<JList<*>>().single().selectedIndex = 1
        val field = descendants(design!!.component).filterIsInstance<EditorTextField>().single { it.name == "query.sql" }
        field.ensureWillComputePreferredSize()
        field.preferredSize
        return field
    }

    private data class Token(val text: String, val type: String, val color: Color?)

    private fun tokens(field: EditorTextField): List<Token> {
        val iterator = (field.editor as EditorEx).highlighter.createIterator(0)
        return buildList {
            while (!iterator.atEnd()) {
                add(Token(field.text.substring(iterator.start, iterator.end), iterator.tokenType?.toString().orEmpty(), iterator.textAttributes?.foregroundColor))
                iterator.advance()
            }
        }
    }

    fun testSqlTokenBoundariesAndRequestedColors() {
        val field = openSql("sElEcT table1, 12, 3.5, .25, 1e-2, 0xFF, 'it''s SELECT 99', \"EMP\"\"NAME\"\n" +
            "from emp where empno=:select\n-- SELECT 99 'ignored'\n/* UPDATE 42 */\n'open SELECT 77")
        val values = tokens(field)
        assertEquals("GLUE_SQL_KEYWORD", values.first().type)
        assertEquals(setOf("12", "3.5", ".25", "1e-2", "0xFF"), values.filter { it.type == "GLUE_SQL_NUMBER" }.map { it.text }.toSet())
        assertEquals(listOf("'it''s SELECT 99'", "\"EMP\"\"NAME\"", "'open SELECT 77"),
            values.filter { it.type == "GLUE_SQL_QUOTED" }.map { it.text })
        assertEquals(2, values.count { it.type == "GLUE_SQL_COMMENT" })
        assertEquals("GLUE_SQL_IDENTIFIER", values.single { it.text == "table1" }.type)
        assertEquals("GLUE_SQL_PARAMETER", values.single { it.text == ":select" }.type)
        assertEquals(Color(0x008B9A).rgb, values.single { it.text == ":select" }.color!!.rgb)
        assertEquals(Color(0xC46600).rgb, values.first().color!!.rgb)
        assertEquals(Color(0x175BC1).rgb, values.first { it.type == "GLUE_SQL_NUMBER" }.color!!.rgb)
        assertEquals(Color(0x26823B).rgb, values.first { it.type == "GLUE_SQL_QUOTED" }.color!!.rgb)
        assertEquals(field.text, values.joinToString("") { it.text })
    }

    private fun configureCompletion(field: EditorTextField, offset: Int) {
        val manager = PsiDocumentManager.getInstance(project)
        manager.commitDocument(field.document)
        val psi = manager.getPsiFile(field.document)
        assertNotNull("SQL buffer must expose a PSI document for completion", psi)
        assertEquals(true, psi!!.getUserData(com.intellij.util.textCompletion.TextCompletionUtil.AUTO_POPUP_KEY))
        assertTrue(com.intellij.util.textCompletion.TextCompletionContributor().invokeAutoPopup(psi.findElementAt(0)!!, 'e'))
        myFixture.configureFromExistingVirtualFile(psi.virtualFile)
        assertSame(field.document, myFixture.editor.document)
        myFixture.editor.caretModel.moveToOffset(offset)
    }

    fun testKeywordCompletionUpdatesOnlySelectedQueryAndSupportsUndo() {
        val field = openSql("se", secondQuery = true)
        configureCompletion(field, 2)
        val variants = myFixture.completeBasic()!!
        assertTrue(variants.any { it.lookupString == "SELECT" })
        assertTrue(variants.all { it.lookupString.startsWith("SE", ignoreCase = true) })
        myFixture.lookup.currentItem = variants.single { it.lookupString == "SELECT" }
        myFixture.finishLookup('\n')
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("SELECT", field.text.uppercase())
        val rootDocument = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
        val manager = PsiDocumentManager.getInstance(project)
        manager.commitDocument(rootDocument)
        val queries = (manager.getPsiFile(rootDocument) as XmlFile).rootTag!!.subTags
        assertEquals("select 100", queries[0].value.textElements.joinToString("") { it.value })
        assertEquals(field.text, queries[1].value.textElements.joinToString("") { it.value })
        assertEquals("GLUE_SQL_KEYWORD", tokens(field).single().type)
        val source = FileEditorProviderManager.getInstance().getProviderList(project, design!!.file!!)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design!!.file!!) as TextEditor
        try {
            UndoManager.getInstance(project).undo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("se", field.text)
            UndoManager.getInstance(project).redo(source)
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("SELECT", field.text.uppercase())
            val list = descendants(design!!.component).filterIsInstance<JList<*>>().single()
            list.selectedIndex = 0
            assertEquals("GLUE_SQL_KEYWORD", tokens(field).first().type)
            list.selectedIndex = 1
            assertEquals("GLUE_SQL_KEYWORD", tokens(field).first().type)
            configureCompletion(field, 2)
            assertTrue(myFixture.completeBasic()!!.any { it.lookupString == "SELECT" })
        } finally {
            Disposer.dispose(source)
        }
    }

    fun testCompletionIsSuppressedInsideQuotesCommentsAndNumbers() {
        val field = openSql("select 1")
        val cases = listOf("SELECT example_name, 'se<caret>'", "SELECT \"se<caret>\"", "SELECT 1 -- se<caret>",
            "SELECT 1 /* se<caret> */", "SELECT 'open se<caret>", "SELECT 12<caret>", "SELECT :se<caret>")
        for (sample in cases) {
            val text = sample.replace("<caret>", "")
            WriteCommandAction.runWriteCommandAction(project) { field.document.setText(text) }
            UIUtil.dispatchAllInvocationEvents()
            configureCompletion(field, sample.indexOf("<caret>"))
            val variants = myFixture.completeBasic()
            assertTrue("Unexpected candidates inside $sample", variants.isNullOrEmpty())
            assertEquals(text, field.text)
        }
    }

    fun testIncrementalHighlightingReclassifiesClosedQuotesAndNestedComments() {
        val field = openSql("SELECT 'open\nFROM emp")
        assertEquals(listOf("SELECT"), tokens(field).filter { it.type == "GLUE_SQL_KEYWORD" }.map { it.text })
        val updated = "SELECT 'it\\'s' 42 /* outer /* SELECT */ 99 */ END-EXEC"
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText(updated) }
        UIUtil.dispatchAllInvocationEvents()
        val values = tokens(field)
        assertEquals(listOf("SELECT", "END-EXEC"), values.filter { it.type == "GLUE_SQL_KEYWORD" }.map { it.text })
        assertEquals(listOf("42"), values.filter { it.type == "GLUE_SQL_NUMBER" }.map { it.text })
        assertEquals(1, values.count { it.type == "GLUE_SQL_COMMENT" })
        assertEquals(updated, values.joinToString("") { it.text })
    }

    fun testParametersAreDistinctFromCastsAssignmentsQuotesAndComments() {
        val field = openSql("select :id, :_name2, :한글, value::type, :target := 12, ':hidden', \"x:quoted\" -- :comment")
        val values = tokens(field)
        assertEquals(listOf(":id", ":_name2", ":한글", ":target"), values.filter { it.type == "GLUE_SQL_PARAMETER" }.map { it.text })
        assertEquals(field.text, values.joinToString("") { it.text })
    }

    fun testReadOnlySqlEditorDoesNotOfferCompletion() {
        val field = openSql("se")
        val file = design!!.file!!
        WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
        try {
            design!!.selectNotify()
            UIUtil.dispatchAllInvocationEvents()
            field.preferredSize
            val editor = field.editor as EditorEx
            assertTrue(editor.isViewer)
            editor.caretModel.moveToOffset(2)
            com.intellij.codeInsight.completion.CodeCompletionHandlerBase(com.intellij.codeInsight.completion.CompletionType.BASIC)
                .invokeCompletion(project, editor)
            assertNull(com.intellij.codeInsight.lookup.LookupManager.getActiveLookup(editor))
            assertEquals("se", field.text)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { file.isWritable = true }
        }
    }

    fun testOpenAndSaveNormalizationPreserveHighlightingAndCompletion() {
        val field = openSql(" \nselect 12\n ")
        val file = design!!.file!!
        com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFile(file, false)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("select 12", field.text)
        assertEquals("GLUE_SQL_KEYWORD", tokens(field).first().type)
        WriteCommandAction.runWriteCommandAction(project) { field.document.setText(" \nselect 34\n ") }
        UIUtil.dispatchAllInvocationEvents()
        FileDocumentManager.getInstance().saveDocument(FileDocumentManager.getInstance().getDocument(file)!!)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals("select 34", field.text)
        assertEquals(listOf("34"), tokens(field).filter { it.type == "GLUE_SQL_NUMBER" }.map { it.text })
        configureCompletion(field, 2)
        assertTrue(myFixture.completeBasic()!!.any { it.lookupString == "SELECT" })
    }

    private fun captureSql(dark: Boolean) {
        val colors = EditorColorsManager.getInstance()
        val previousScheme = colors.globalScheme
        val previousLaf = javax.swing.UIManager.getLookAndFeel()
        val wasDark = !JBColor.isBright()
        try {
            if (dark) {
                javax.swing.UIManager.setLookAndFeel(com.intellij.ide.ui.laf.darcula.DarculaLaf())
                JBColor.setDark(true)
                colors.setGlobalScheme(colors.getScheme("Darcula")!!)
            }
            val sample = "select empno, 123, 3.14, 'hello SQL', \"EMP_NAME\"\nfrom emp\nwhere empno = :empno and ename = :name;\n-- SELECT 99 ':comment'\n/* UPDATE 5 */"
            val field = openSql(sample)
            field.setSize(720, 220)
            repeat(3) { descendants(field).filterIsInstance<Container>().forEach { it.doLayout() } }
            val image = java.awt.image.BufferedImage(field.width, field.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val graphics = image.createGraphics()
            try { field.paint(graphics) } finally { graphics.dispose() }
            val output = Path.of(".gradle-validation", "sql-syntax-${if (dark) "dark" else "light"}.png")
            Files.createDirectories(output.parent)
            javax.imageio.ImageIO.write(image, "png", output.toFile())
            val values = tokens(field)
            assertEquals(Color(if (dark) 0xFFA657 else 0xC46600).rgb, values.first().color!!.rgb)
            assertEquals(Color(if (dark) 0x6CA8F5 else 0x175BC1).rgb, values.first { it.type == "GLUE_SQL_NUMBER" }.color!!.rgb)
            assertEquals(Color(if (dark) 0x78C78B else 0x26823B).rgb, values.first { it.type == "GLUE_SQL_QUOTED" }.color!!.rgb)
            assertEquals(Color(if (dark) 0x56D4DD else 0x008B9A).rgb, values.first { it.type == "GLUE_SQL_PARAMETER" }.color!!.rgb)
        } finally {
            design?.let(Disposer::dispose)
            design = null
            colors.setGlobalScheme(previousScheme)
            javax.swing.UIManager.setLookAndFeel(previousLaf)
            JBColor.setDark(wasDark)
        }
    }

    fun testLightSyntaxCapture() = captureSql(false)
    fun testDarkSyntaxCapture() = captureSql(true)

    fun testAllDatabaseMetadataCompletionIsDistinctAndInsertsIntoQuery() {
        val settings = com.look4counter.queryeditor.settings.DataSourceSettingsService.getInstance(project)
        settings.replaceDataSources(listOf(com.look4counter.queryeditor.settings.DataSourceDefinition("one", "Local", "jdbc:postgresql://localhost/db")))
        val metadata = com.look4counter.queryeditor.database.DatabaseMetadataService.getInstance(project)
        try {
            val snapshot = com.look4counter.queryeditor.database.MetadataSnapshot(listOf(
                com.look4counter.queryeditor.database.TableMetadata(null, "public", "emp_table", listOf("emp_id", "emp_name", "emp_id")),
                com.look4counter.queryeditor.database.TableMetadata(null, "other", "emp_table", listOf("emp_id", "SELECT"))))
            metadata.complete(metadata.beginRefresh().single(), snapshot)
            val field = openSql("select emp_")
            configureCompletion(field, field.text.length)
            val variants = myFixture.completeBasic()!!
            assertEquals(setOf("emp_table", "emp_id", "emp_name"), variants.map { it.lookupString }.toSet())
            assertEquals(3, variants.size)
            myFixture.lookup.currentItem = variants.single { it.lookupString == "emp_id" }
            myFixture.finishLookup('\n')
            UIUtil.dispatchAllInvocationEvents()
            assertEquals("select emp_id", field.text)
            val root = FileDocumentManager.getInstance().getDocument(design!!.file!!)!!
            assertTrue(root.text.contains("select emp_id"))
            settings.replaceDataSources(emptyList())
            WriteCommandAction.runWriteCommandAction(project) { field.document.setText("emp_") }
            configureCompletion(field, 4)
            assertTrue(myFixture.completeBasic().isNullOrEmpty())
        } finally { settings.replaceDataSources(emptyList()) }
    }

    fun testSettingsRefreshReadsLocalDriverAndEditorHasNoMetadataToolbar() {
        val directory = Files.createTempDirectory("glue-toolbar-driver")
        val jar = directory.resolve("driver.jar")
        val settings = com.look4counter.queryeditor.settings.DataSourceSettingsService.getInstance(project)
        val configurable = com.look4counter.queryeditor.settings.GlueQueryEditorConfigurable(project)
        try {
            val resource = "com/look4counter/FixtureJdbcDriver.class"
            java.util.jar.JarOutputStream(Files.newOutputStream(jar)).use { output ->
                output.putNextEntry(java.util.zip.ZipEntry(resource))
                requireNotNull(javaClass.classLoader.getResourceAsStream(resource)).use { it.copyTo(output) }
                output.closeEntry()
            }
            settings.replaceDataSources(listOf(com.look4counter.queryeditor.settings.DataSourceDefinition(
                "toolbar", "Local", "jdbc:glue-test:public", "com.look4counter.FixtureJdbcDriver", "", false,
                "POSTGRESQL", mutableListOf(jar.toString()))))
            openSql("select emp")
            val editorControls = descendants(design!!.component).toList()
            assertTrue(editorControls.none { it.name in setOf("query.datasource", "query.metadata.refresh", "query.metadata.status") })
            val settingsComponent = configurable.createComponent()
            configurable.reset()
            val controls = descendants(settingsComponent).toList()
            val button = controls.filterIsInstance<javax.swing.JButton>().single { it.name == "query.metadata.refresh" }
            val status = controls.filterIsInstance<javax.swing.JLabel>().single { it.name == "query.metadata.status" }
            button.doClick()
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("metadata refresh", { button.isEnabled }, 10)
            assertEquals("테이블 1 · 컬럼 1", status.text)
            val metadata = com.look4counter.queryeditor.database.DatabaseMetadataService.getInstance(project)
            assertEquals(listOf("EMP"), metadata.snapshot()!!.tableNames)
            System.setProperty("glue.test.fail", "true")
            button.doClick()
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("failed metadata refresh", { button.isEnabled }, 10)
            assertTrue(status.text.startsWith("조회 실패"))
            assertFalse(status.text.contains("secret-value"))
            assertEquals(listOf("EMP"), metadata.snapshot()!!.tableNames)
        } finally {
            configurable.disposeUIResources()
            settings.replaceDataSources(emptyList())
            listOf("glue.test.closed", "glue.test.deregistered", "glue.test.fail").forEach(System::clearProperty)
            Files.delete(jar)
            Files.delete(directory)
        }
    }
}
