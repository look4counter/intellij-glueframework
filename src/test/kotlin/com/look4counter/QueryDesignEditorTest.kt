package com.look4counter

import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.ex.FileEditorProviderManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.ui.EditorTextField
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.UIUtil
import java.awt.Component
import java.awt.Container
import java.nio.file.Files
import java.nio.file.Path
import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import javax.swing.JCheckBox
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JTextField

class QueryDesignEditorTest : BasePlatformTestCase() {
    private lateinit var design: FileEditor

    private fun openDesign(xml: String): FileEditor {
        val file = myFixture.addFileToProject("design.glue_sql", xml).virtualFile
        design = FileEditorProviderManager.getInstance().getProviderList(project, file)
            .single { it.editorTypeId == "glue-query-design" }.createEditor(project, file)
        drain()
        return design
    }

    override fun tearDown() {
        try {
            if (::design.isInitialized) Disposer.dispose(design)
        } finally {
            super.tearDown()
        }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private inline fun <reified T : Component> control(name: String): T =
        descendants(design.component).filterIsInstance<T>().single { it.name == name }

    private fun list(): JList<*> = control("query.list")
    private fun sql(): EditorTextField = control("query.sql")
    private fun input(name: String): JTextField = control("query.$name")
    private fun drain() = UIUtil.dispatchAllInvocationEvents()
    private fun document() = FileDocumentManager.getInstance().getDocument(design.file!!)!!

    private fun root(): XmlTag {
        val manager = PsiDocumentManager.getInstance(project)
        manager.commitDocument(document())
        return (manager.getPsiFile(document()) as XmlFile).rootTag!!
    }

    private fun queries(): Array<XmlTag> = root().subTags
    private fun sqlOf(tag: XmlTag): String = tag.value.textElements.joinToString("") { it.value }

    private fun editSql(value: String) {
        WriteCommandAction.runWriteCommandAction(project) { sql().document.setText(value) }
        drain()
    }

    private fun inWriteUnsafeContext(action: () -> Unit) {
        val guard = com.intellij.openapi.application.TransactionGuard.getInstance() as
            com.intellij.openapi.application.TransactionGuardImpl
        guard.performActivity(false) {
            assertFalse(guard.isWritingAllowed)
            action()
        }
    }

    fun testCreationDefersCommitFromWriteUnsafeContext() {
        val file = myFixture.addFileToProject("unsafe.glue_sql", XML).virtualFile
        val source = FileDocumentManager.getInstance().getDocument(file)!!
        val latest = XML.replace("id=\"first\"", "id=\"latest\"")
        WriteCommandAction.runWriteCommandAction(project) { source.setText(latest) }
        val manager = PsiDocumentManager.getInstance(project)
        assertFalse(manager.isCommitted(source))
        inWriteUnsafeContext {
            design = FileEditorProviderManager.getInstance().getProviderList(project, file)
                .single { it.editorTypeId == "glue-query-design" }.createEditor(project, file)
            assertFalse(manager.isCommitted(source))
        }
        drain()
        assertEquals("latest", input("id").text)
        assertEquals(latest, source.text)
    }

    fun testActivationDefersCommitFromWriteUnsafeContext() {
        openDesign(XML)
        val latest = XML.replace("id=\"first\"", "id=\"latest\"")
        WriteCommandAction.runWriteCommandAction(project) { document().setText(latest) }
        val manager = PsiDocumentManager.getInstance(project)
        assertFalse(manager.isCommitted(document()))
        inWriteUnsafeContext {
            design.selectNotify()
            assertFalse(manager.isCommitted(document()))
        }
        drain()
        assertEquals("latest", input("id").text)
        assertEquals(latest, document().text)
    }

    fun testDeactivationDefersPendingWritesFromWriteUnsafeContext() {
        openDesign(XML)
        val original = document().text
        input("desc").text = "pending"
        inWriteUnsafeContext {
            design.deselectNotify()
            assertEquals(original, document().text)
        }
        drain()
        assertEquals("pending", queries()[0].getAttributeValue("desc"))
    }

    fun testDisposedEditorSkipsScheduledInitialization() {
        val file = myFixture.addFileToProject("disposed.glue_sql", XML).virtualFile
        inWriteUnsafeContext {
            design = FileEditorProviderManager.getInstance().getProviderList(project, file)
                .single { it.editorTypeId == "glue-query-design" }.createEditor(project, file)
            Disposer.dispose(design)
        }
        drain()
        assertEquals(XML, FileDocumentManager.getInstance().getDocument(file)!!.text)
    }

    fun testUserSampleSelectionAndLayout() {
        val sample = Files.readString(Path.of("queryeditor", "sample-query.glue_sql"))
        openDesign(sample)
        assertEquals("sample", control<JTextField>("queryMap.desc").text)
        assertEquals(7, list().model.size)
        assertEquals(0, list().selectedIndex)
        for (index in queries().indices) {
            list().selectedIndex = index
            val query = queries()[index]
            assertEquals(query.getAttributeValue("id"), list().selectedValue.toString())
            assertEquals(query.getAttributeValue("id"), input("id").text)
            assertEquals(query.getAttributeValue("desc"), input("desc").text)
            assertEquals(query.getAttributeValue("resultType"), control<EditorTextField>("query.resultType").text)
            assertEquals(query.getAttributeValue("isNamed").toBoolean(), control<JCheckBox>("query.isNamed").isSelected)
            assertEquals(sqlOf(query), sql().text)
        }
        assertFalse(design.isModified)
        val sidebar = control<Container>("query.sidebar")
        assertEquals(320, sidebar.preferredSize.width)
        assertEquals(220, sidebar.minimumSize.width)
        val splitter = control<OnePixelSplitter>("query.splitter")
        splitter.proportion = 0.55f
        assertEquals(0.55f, splitter.proportion)
    }

    fun testSqlAndAllAttributesWriteOnlySelectedQuery() {
        openDesign(XML)
        list().selectedIndex = 1
        val firstBefore = queries()[0].text
        input("id").text = "renamed"
        input("desc").text = "A & B \"설명\""
        WriteCommandAction.runWriteCommandAction(project) { control<EditorTextField>("query.resultType").text = "sample.Result" }
        control<JCheckBox>("query.isNamed").doClick()
        editSql("select '<&', ']]>'\nfrom sample")
        val selected = queries()[1]
        assertEquals("renamed", selected.getAttributeValue("id"))
        val parsed = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(document().text.toByteArray(Charsets.UTF_8)))
        assertEquals("A & B \"설명\"", parsed.getElementsByTagName("query").item(1).attributes.getNamedItem("desc").nodeValue)
        assertEquals("A & B \"설명\"", input("desc").text)
        assertEquals("sample.Result", selected.getAttributeValue("resultType"))
        assertEquals("true", selected.getAttributeValue("isNamed"))
        assertEquals("select '<&', ']]>'\nfrom sample", sqlOf(selected))
        assertEquals(firstBefore, queries()[0].text)
        assertEquals("urn:query", root().namespace)
        assertTrue(document().text.contains("<!-- preserve -->"))
        assertEquals("renamed", list().selectedValue.toString())
        assertEquals(1, list().selectedIndex)
        assertTrue(design.isModified)
        FileDocumentManager.getInstance().saveDocument(document())
        assertEquals(document().text, String(design.file!!.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
    }

    fun testQuickSelectionDoesNotMovePendingEditsToAnotherQuery() {
        openDesign(XML)
        input("desc").text = "first updated"
        WriteCommandAction.runWriteCommandAction(project) { sql().document.setText("select first") }
        list().selectedIndex = 1
        drain()
        assertEquals("first updated", queries()[0].getAttributeValue("desc"))
        assertEquals("select first", sqlOf(queries()[0]))
        assertEquals("select 2", sqlOf(queries()[1]))
        assertEquals("second", input("id").text)
        assertEquals("select 2", sql().text)
    }

    fun testQueryListHorizontalPaddingAndGutterSpace() {
        openDesign(XML)
        @Suppress("UNCHECKED_CAST")
        val ids = list() as JList<Any>
        val baseline = javax.swing.DefaultListCellRenderer()
            .getListCellRendererComponent(ids, ids.model.getElementAt(0), 0, false, false) as javax.swing.JComponent
        val baselineInsets = baseline.insets
        val cell = ids.cellRenderer.getListCellRendererComponent(ids, ids.model.getElementAt(0), 0, false, false) as javax.swing.JComponent
        assertEquals(baselineInsets.left + com.intellij.util.ui.JBUI.scale(8), cell.insets.left)
        assertEquals(baselineInsets.right + com.intellij.util.ui.JBUI.scale(8), cell.insets.right)
        val firstInsets = cell.insets
        val selectedCell = ids.cellRenderer.getListCellRendererComponent(ids, ids.model.getElementAt(0), 0, true, false) as javax.swing.JComponent
        assertEquals(firstInsets, selectedCell.insets)
        assertEquals(ids.selectionBackground, selectedCell.background)
        sql().ensureWillComputePreferredSize()
        sql().preferredSize
        val editor = sql().editor as com.intellij.openapi.editor.ex.EditorEx
        val gutter = editor.gutterComponentEx
        assertTrue(gutter.preferredSize.width - gutter.lineNumberAreaOffset - gutter.lineNumberAreaWidth >= com.intellij.util.ui.JBUI.scale(18))
        val panel = design.component
        panel.setSize(1066, 500)
        repeat(3) { descendants(panel).filterIsInstance<Container>().forEach { it.doLayout() } }
        val image = java.awt.image.BufferedImage(panel.width, panel.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        try { panel.paint(graphics) } finally { graphics.dispose() }
        val path = Path.of(".gradle-validation", "query-layout-padding.png")
        Files.createDirectories(path.parent)
        javax.imageio.ImageIO.write(image, "png", path.toFile())
    }

    fun testSavingTrimsSqlAndSynchronizesDesignWithUndo() {
        openDesign(XML)
        list().selectedIndex = 1
        val padded = " \n\tdelete from emp\n  where empno=?\n "
        editSql(padded)
        val source = FileEditorProviderManager.getInstance().getProviderList(project, design.file!!)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design.file!!) as TextEditor
        try {
            assertEquals(padded, sql().text)
            FileDocumentManager.getInstance().saveDocument(document())
            drain()
            assertEquals("delete from emp\n  where empno=?", sql().text)
            assertEquals(sql().text, sqlOf(queries()[1]))
            assertEquals("select 1", sqlOf(queries()[0]))
            assertEquals(1, list().selectedIndex)
            UndoManager.getInstance(project).undo(source)
            drain()
            assertEquals(padded, sql().text)
            assertEquals(padded, sqlOf(queries()[1]))
        } finally {
            Disposer.dispose(source)
        }
    }

    fun testSourceUpdatesAndUndoRedoRefreshDesign() {
        openDesign(XML)
        val file = design.file!!
        val source = FileEditorProviderManager.getInstance().getProviderList(project, file)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, file) as TextEditor
        try {
            input("desc").text = "changed"
            drain()
            val undo = UndoManager.getInstance(project)
            undo.undo(source)
            drain()
            assertEquals("one", input("desc").text)
            undo.redo(source)
            drain()
            assertEquals("changed", input("desc").text)
            WriteCommandAction.runWriteCommandAction(project) {
                document().setText("<queryMap desc=\"source\"><query id=\"new\"><![CDATA[new sql]]></query></queryMap>")
            }
            design.selectNotify()
            drain()
            assertEquals("source", control<JTextField>("queryMap.desc").text)
            assertEquals(1, list().model.size)
            assertEquals("new", input("id").text)
            assertEquals("new sql", sql().text)
        } finally {
            Disposer.dispose(source)
        }
    }

    fun testDuplicateAndMissingIdsRemainSeparate() {
        openDesign("<queryMap><query id=\"same\">first</query><query id=\"same\">second</query><query/></queryMap>")
        assertEquals(3, list().model.size)
        list().selectedIndex = 1
        editSql("only second")
        assertEquals("first", sqlOf(queries()[0]))
        assertEquals("only second", sqlOf(queries()[1]))
        list().selectedIndex = 2
        input("id").text = "added.id"
        editSql("empty query body")
        assertEquals("added.id", queries()[2].getAttributeValue("id"))
        assertEquals("empty query body", sqlOf(queries()[2]))
    }

    fun testSqlSourceUndoRedoKeepsBufferInSync() {
        openDesign(XML)
        val file = design.file!!
        val source = FileEditorProviderManager.getInstance().getProviderList(project, file)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, file) as TextEditor
        try {
            editSql("select changed")
            val undo = UndoManager.getInstance(project)
            undo.undo(source)
            drain()
            assertEquals("select 1", sql().text)
            assertEquals("select 1", sqlOf(queries()[0]))
            undo.redo(source)
            drain()
            assertEquals("select changed", sql().text)
            assertEquals("select changed", sqlOf(queries()[0]))
        } finally {
            Disposer.dispose(source)
        }
    }

    fun testPrefixedMapAndEscapedAttributeValues() {
        openDesign("<q:queryMap xmlns:q=\"urn:query\" desc=\"A &amp; B\"><q:query id=\"a&amp;b\" desc=\"&quot;quoted&quot;\"/></q:queryMap>")
        assertEquals("A & B", control<JTextField>("queryMap.desc").text)
        assertEquals("a&b", input("id").text)
        assertEquals("\"quoted\"", input("desc").text)
        editSql("select 1 < 2")
        assertEquals("q:query", queries()[0].name)
        assertEquals("urn:query", queries()[0].namespace)
        assertEquals("select 1 < 2", sqlOf(queries()[0]))
    }

    fun testInvalidAndEmptyMapsDoNotWriteSource() {
        openDesign("<queryMap desc=\"empty\"/>")
        assertEquals(0, list().model.size)
        assertFalse(input("id").isEnabled)
        assertEquals("", sql().text)
        for (text in listOf("", "<queryMap><query>", "<other/>")) {
            WriteCommandAction.runWriteCommandAction(project) { document().setText(text) }
            design.selectNotify()
            drain()
            assertEquals(0, list().model.size)
            assertFalse(input("id").isEnabled)
            assertEquals(text, document().text)
        }
    }

    fun testEntitiesWhitespaceAndNestedBodySafety() {
        openDesign("<queryMap><query id=\"plain\">  a &lt; b &amp; c  </query><query id=\"nested\">a<!-- keep --><if/>b</query></queryMap>")
        assertEquals("  a < b & c  ", sql().text)
        list().selectedIndex = 1
        val before = document().text
        WriteCommandAction.runWriteCommandAction(project) { sql().document.setText("must not replace") }
        drain()
        assertEquals(before, document().text)
        assertTrue(control<JLabel>("query.status").text.contains("Source"))
    }

    fun testReadOnlyAndDisposedEditorCannotWrite() {
        openDesign(XML)
        val file = design.file!!
        WriteCommandAction.runWriteCommandAction(project) { file.isWritable = false }
        try {
            design.selectNotify()
            drain()
            assertFalse(input("id").isEnabled)
            val before = document().text
            input("id").text = "blocked"
            drain()
            assertEquals(before, document().text)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { file.isWritable = true }
        }
        design.selectNotify()
        drain()
        input("desc").text = "pending"
        val before = document().text
        Disposer.dispose(design)
        drain()
        assertEquals(before, document().text)
    }

    fun testMapDescriptionEditingPreservesQueriesAndSupportsUndoSaveAndSource() {
        openDesign(XML)
        list().selectedIndex = 1
        val before = queries().map { it.text }
        val value = "A & B \"설명\" <map>"
        control<JTextField>("queryMap.desc").text = value
        drain()
        val parsed = DocumentBuilderFactory.newInstance().newDocumentBuilder()
            .parse(ByteArrayInputStream(document().text.toByteArray(Charsets.UTF_8)))
        assertEquals(value, parsed.documentElement.getAttribute("desc"))
        assertEquals(before, queries().map { it.text })
        assertEquals(1, list().selectedIndex)
        assertTrue(document().text.contains("<!-- preserve -->"))
        assertEquals("urn:query", root().namespace)
        val source = FileEditorProviderManager.getInstance().getProviderList(project, design.file!!)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design.file!!) as TextEditor
        try {
            val undo = UndoManager.getInstance(project)
            undo.undo(source)
            drain()
            assertEquals("map", control<JTextField>("queryMap.desc").text)
            undo.redo(source)
            drain()
            assertEquals(value, control<JTextField>("queryMap.desc").text)
            FileDocumentManager.getInstance().saveDocument(document())
            assertEquals(document().text, String(design.file!!.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
            WriteCommandAction.runWriteCommandAction(project) { root().setAttribute("desc", "source") }
            drain()
            assertEquals("source", control<JTextField>("queryMap.desc").text)
            control<JTextField>("queryMap.desc").text = ""
            drain()
            assertEquals("", root().getAttributeValue("desc"))
        } finally {
            Disposer.dispose(source)
        }
    }

    fun testEmptyMapDescriptionAndWriteRestrictions() {
        openDesign("<q:queryMap xmlns:q=\"urn:query\"/>")
        val description = control<JTextField>("queryMap.desc")
        assertTrue(description.isEnabled)
        description.text = "empty map"
        drain()
        assertEquals("empty map", root().getAttributeValue("desc"))
        assertEquals("q:queryMap", root().name)
        assertEquals(0, list().model.size)
        WriteCommandAction.runWriteCommandAction(project) { design.file!!.isWritable = false }
        try {
            design.selectNotify()
            drain()
            assertFalse(description.isEnabled)
            val before = document().text
            description.text = "blocked"
            drain()
            assertEquals(before, document().text)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { design.file!!.isWritable = true }
        }
        WriteCommandAction.runWriteCommandAction(project) { document().setText("<other/>") }
        drain()
        assertFalse(description.isEnabled)
        description.text = "blocked"
        drain()
        assertEquals("<other/>", document().text)
    }

    fun testNamedBesideResultTypeAndNoCountStatus() = verifyInputAppearance("light")

    fun testResultTypeAppearanceUnderDarcula() {
        val previousLaf = javax.swing.UIManager.getLookAndFeel()
        val wasDark = !com.intellij.ui.JBColor.isBright()
        try {
            javax.swing.UIManager.setLookAndFeel(com.intellij.ide.ui.laf.darcula.DarculaLaf())
            com.intellij.ui.JBColor.setDark(true)
            verifyInputAppearance("dark")
        } finally {
            javax.swing.UIManager.setLookAndFeel(previousLaf)
            com.intellij.ui.JBColor.setDark(wasDark)
        }
    }

    private fun verifyInputAppearance(theme: String) {
        openDesign(XML)
        val resultType = control<EditorTextField>("query.resultType")
        val named = control<JCheckBox>("query.isNamed")
        resultType.ensureWillComputePreferredSize()
        assertEquals(input("id").preferredSize.height, resultType.preferredSize.height)
        assertEquals(input("id").font, resultType.font)
        assertEquals(input("id").background, resultType.background)
        val resultEditor = resultType.editor as com.intellij.openapi.editor.ex.EditorEx
        resultType.setSize(240, resultType.preferredSize.height)
        fun layoutInput() {
            descendants(resultType).filterIsInstance<Container>().forEach { it.doLayout() }
        }
        WriteCommandAction.runWriteCommandAction(project) { resultType.text = "sample.Result" }
        drain()
        layoutInput()
        val bounds = resultEditor.component.bounds
        assertTrue("Text must have a visible left inset", bounds.x > 0)
        assertTrue(resultType.width - bounds.x - bounds.width > 0)
        assertTrue(kotlin.math.abs(resultType.height - (2 * bounds.y + bounds.height)) <= 1)
        assertEquals(resultEditor.lineHeight, bounds.height)
        fun renderInput(state: String): java.awt.image.BufferedImage {
            layoutInput()
            val rendered = java.awt.image.BufferedImage(resultType.width, resultType.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val graphics = rendered.createGraphics()
            try { resultType.paint(graphics) } finally { graphics.dispose() }
            val output = Path.of(".gradle-validation", "result-type-$theme-$state.png")
            Files.createDirectories(output.parent)
            javax.imageio.ImageIO.write(rendered, "png", output.toFile())
            return rendered
        }
        fun captureComparison(state: String) {
            val panel = resultType.parent
            panel.setSize(520, 140)
            panel.doLayout()
            descendants(panel).filterIsInstance<Container>().forEach { it.doLayout() }
            val id = input("id")
            val description = input("desc")
            assertEquals(id.x, resultType.x)
            assertEquals(id.height, resultType.height)
            assertEquals(description.x, resultType.x)
            assertEquals(description.height, resultType.height)
            val expectedTextX = id.ui.modelToView2D(id, 0, javax.swing.text.Position.Bias.Forward).x.toInt()
            val actualTextX = javax.swing.SwingUtilities.convertPoint(
                resultEditor.contentComponent, resultEditor.offsetToXY(0), resultType,
            ).x
            assertEquals("Actual text start under $theme", expectedTextX, actualTextX)
            val rendered = java.awt.image.BufferedImage(panel.width, panel.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
            val graphics = rendered.createGraphics()
            try { panel.paint(graphics) } finally { graphics.dispose() }
            javax.imageio.ImageIO.write(rendered, "png", Path.of(".gradle-validation", "input-comparison-$theme-$state.png").toFile())
        }
        input("id").text = "sample.Result"
        input("desc").text = "sample.Result"
        drain()
        captureComparison("normal")
        val normal = renderInput("normal")
        val reference = input("id")
        reference.setSize(resultType.size)
        val referenceImage = java.awt.image.BufferedImage(reference.width, reference.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        val referenceGraphics = referenceImage.createGraphics()
        try { reference.paint(referenceGraphics) } finally { referenceGraphics.dispose() }
        // Compare full top/bottom edge bands; missing themed borders must fail.
        val edgeBand = maxOf(1, reference.insets.top)
        for (x in 0 until normal.width) {
            for (y in 0 until edgeBand) {
                assertEquals("Top border pixel at $x,$y under $theme", referenceImage.getRGB(x, y), normal.getRGB(x, y))
                val bottom = normal.height - 1 - y
                assertEquals("Bottom border pixel at $x,$bottom under $theme", referenceImage.getRGB(x, bottom), normal.getRGB(x, bottom))
            }
        }
        resultType.focusGained(java.awt.event.FocusEvent(resultEditor.contentComponent, java.awt.event.FocusEvent.FOCUS_GAINED))
        renderInput("focused")
        captureComparison("focused")
        resultType.focusLost(java.awt.event.FocusEvent(resultEditor.contentComponent, java.awt.event.FocusEvent.FOCUS_LOST))
        assertSame(resultType.parent, named.parent)
        val layout = resultType.parent.layout as java.awt.GridBagLayout
        val inputPosition = layout.getConstraints(resultType)
        val namedPosition = layout.getConstraints(named)
        val idPosition = layout.getConstraints(input("id"))
        assertEquals(idPosition.insets.left, inputPosition.insets.left)
        assertEquals(idPosition.insets.right, inputPosition.insets.right)
        assertEquals(inputPosition.gridy, namedPosition.gridy)
        assertTrue(namedPosition.gridx > inputPosition.gridx)
        assertFalse(control<JLabel>("query.status").isVisible)
        assertEquals("", control<JLabel>("query.status").text)
        named.doClick()
        drain()
        assertEquals("true", queries()[0].getAttributeValue("isNamed"))
        assertEquals("false", queries()[1].getAttributeValue("isNamed"))
    }

    fun testCapitalizedLabelsKeepXmlAttributeNames() {
        openDesign(XML)
        val labels = descendants(design.component).filterIsInstance<JLabel>().map { it.text }.toSet()
        assertTrue(labels.containsAll(listOf("ID", "Description", "ResultType")))
        assertTrue(control<JCheckBox>("query.isNamed").text.isNullOrEmpty())
        assertEquals("isNamed", control<JCheckBox>("query.isNamed").accessibleContext.accessibleName)
        assertEquals("isNamed", control<JCheckBox>("query.isNamed").toolTipText)
        assertFalse(labels.any { it in listOf("id", "desc", "resultType", "Id", "Desc", "IsNamed") })
        assertFalse(design.isModified)
    }

    fun testResultTypeJavaCompletionStoresQualifiedClassName() {
        myFixture.addFileToProject("alpha/QueryResult.java", "package alpha; public class QueryResult {}")
        myFixture.addFileToProject("beta/QueryResult.java", "package beta; public class QueryResult {}")
        openDesign(XML)
        list().selectedIndex = 1
        val field = control<EditorTextField>("query.resultType")
        WriteCommandAction.runWriteCommandAction(project) { field.text = "QueryRes" }
        drain()
        val psi = PsiDocumentManager.getInstance(project).getPsiFile(field.document)!!
        myFixture.configureFromExistingVirtualFile(psi.virtualFile)
        assertSame(field.document, myFixture.editor.document)
        myFixture.editor.caretModel.moveToOffset(field.text.length)
        val variants = myFixture.completeBasic()!!
        val candidates = variants.filter { it.lookupString == "QueryResult" }
        assertEquals(setOf("alpha.QueryResult", "beta.QueryResult"), candidates.map {
            (it.psiElement as com.intellij.psi.PsiClass).qualifiedName
        }.toSet())
        myFixture.lookup.currentItem = candidates.single {
            (it.psiElement as com.intellij.psi.PsiClass).qualifiedName == "beta.QueryResult"
        }
        myFixture.finishLookup('\n')
        drain()
        assertEquals("beta.QueryResult", field.text)
        assertEquals("beta.QueryResult", queries()[1].getAttributeValue("resultType"))
        assertEquals("", queries()[0].getAttributeValue("resultType"))
        list().selectedIndex = 0
        list().selectedIndex = 1
        assertEquals("beta.QueryResult", field.text)
    }

    fun testResultTypeDirectInputSourceUndoAndReadOnly() {
        openDesign(XML)
        val field = control<EditorTextField>("query.resultType")
        WriteCommandAction.runWriteCommandAction(project) { field.text = "missing.CustomType" }
        list().selectedIndex = 1
        drain()
        assertEquals("missing.CustomType", queries()[0].getAttributeValue("resultType"))
        assertEquals("", field.text)
        val source = FileEditorProviderManager.getInstance().getProviderList(project, design.file!!)
            .single { it.editorTypeId == "glue-query-source" }.createEditor(project, design.file!!) as TextEditor
        try {
            WriteCommandAction.runWriteCommandAction(project) { field.text = "alpha.Result" }
            drain()
            val undo = UndoManager.getInstance(project)
            undo.undo(source)
            drain()
            assertEquals("", field.text)
            undo.redo(source)
            drain()
            assertEquals("alpha.Result", field.text)
            WriteCommandAction.runWriteCommandAction(project) { queries()[1].setAttribute("resultType", "source.Result") }
            drain()
            assertEquals("source.Result", field.text)
            WriteCommandAction.runWriteCommandAction(project) { field.text = "" }
            drain()
            assertEquals("", queries()[1].getAttributeValue("resultType"))
            WriteCommandAction.runWriteCommandAction(project) { design.file!!.isWritable = false }
            design.selectNotify()
            drain()
            assertFalse(field.isEnabled)
            val before = document().text
            WriteCommandAction.runWriteCommandAction(project) { field.text = "blocked.Result" }
            drain()
            assertEquals(before, document().text)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { design.file!!.isWritable = true }
            Disposer.dispose(source)
        }
        WriteCommandAction.runWriteCommandAction(project) { document().setText("<other/>") }
        drain()
        assertFalse(field.isEnabled)
    }

    companion object {
        private const val XML = "<queryMap xmlns=\"urn:query\" desc=\"map\"><!-- preserve -->" +
            "<query id=\"first\" desc=\"one\" resultType=\"\" isNamed=\"false\"><![CDATA[select 1]]></query>" +
            "<query id=\"second\" desc=\"two\" resultType=\"\" isNamed=\"false\"><![CDATA[select 2]]></query></queryMap>"
    }
}
