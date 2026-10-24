package com.look4counter

import com.intellij.ide.util.PropertiesComponent
import com.intellij.ide.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.IdeActions
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.ex.ActionUtil
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.command.undo.UndoManager
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.util.Disposer
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.replaceService
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.ui.UIUtil
import com.look4counter.modeleditor.*
import java.awt.Color
import java.awt.Container
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import java.nio.file.Files
import java.nio.file.Path
import javax.imageio.ImageIO
import javax.swing.JComboBox
import javax.swing.JScrollPane
import javax.swing.JTextField
import javax.swing.JToggleButton
import javax.swing.JLabel
import javax.swing.JRadioButtonMenuItem

class ModelDiagramPropertiesTest : BasePlatformTestCase() {
    fun testSqlKeyCompletionUsesMemoryIdsAndInsertsOnlyId() {
        val query = myFixture.addFileToProject("queries/sample.glue_sql", """<queryMap><query id="sample.find-person" desc="사람 조회"/></queryMap>""")
        val initial = ActivityPropertiesData("A", listOf(ActivityProperty("sql-key", "sample.find-"),
            ActivityProperty("class", "sample.Type"), ActivityProperty("other", "text")), "")
        val panel = ActivityPropertiesPanel(initial, project)
        val index = com.look4counter.queryeditor.queryindex.QueryIdIndexService.getInstance(project)
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("Query ID 최초 수집", { !index.isUpdating }, 10)
        val table = panel.propertiesTable
        assertTrue(table.getCellEditor(0, 1) is QueryIdCellEditor)
        assertFalse(table.getCellEditor(0, 0) is QueryIdCellEditor)
        assertTrue(table.getCellEditor(1, 1) is ActivityClassCellEditor)
        assertFalse(table.getCellEditor(2, 1) is QueryIdCellEditor)
        assertTrue(table.editCellAt(0, 1))
        val field = table.editorComponent as com.intellij.ui.EditorTextField
        val file = PsiDocumentManager.getInstance(project).getPsiFile(field.document)!!
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        myFixture.editor.caretModel.moveToOffset(field.document.textLength)
        val variants = myFixture.completeBasic()
        if (!variants.isNullOrEmpty()) {
            val candidate = variants.first { it.lookupString == "sample.find-person" }
            com.intellij.codeInsight.lookup.LookupManager.getActiveLookup(myFixture.editor)!!.currentItem = candidate
            myFixture.finishLookup('\n')
        }
        assertEquals("sample.find-person", field.text)
        assertEquals("sample.find-person", panel.result().properties[0].value)
        assertEquals("sample.find-", initial.properties[0].value)
        WriteCommandAction.runWriteCommandAction(project) {
            query.viewProvider.document!!.setText("""<queryMap><query id="sample.changed"/></queryMap>""")
        }
        com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching("Query ID 수정", { !index.isUpdating }, 10)
        assertEquals(listOf("sample.changed"), index.snapshot("sample.").map { it.id })
        panel.propertiesModel.setValueAt("sql-key", 2, 0)
        assertTrue(table.getCellEditor(2, 1) is QueryIdCellEditor)
        panel.propertiesModel.setValueAt("other", 0, 0)
        assertFalse(table.getCellEditor(0, 1) is QueryIdCellEditor)
    }

    fun testClassCompletionFindsShortNameAndInsertsQualifiedName() {
        myFixture.addFileToProject("sample/CompletedActivity.java", "package sample; public class CompletedActivity {}")
        val panel = ActivityPropertiesPanel(ActivityPropertiesData("A", listOf(ActivityProperty("class", "CompletedAct")), ""), project)
        panel.propertiesTable.editCellAt(0, 1)
        val field = panel.propertiesTable.editorComponent as com.intellij.ui.EditorTextField
        val fragment = PsiDocumentManager.getInstance(project).getPsiFile(field.document)!!
        myFixture.configureFromExistingVirtualFile(fragment.virtualFile)
        myFixture.editor.caretModel.moveToOffset(field.document.textLength)
        val variants = myFixture.completeBasic()
        if (!variants.isNullOrEmpty()) {
            val candidate = variants.first { it.lookupString == "CompletedActivity" }
            com.intellij.codeInsight.lookup.LookupManager.getActiveLookup(myFixture.editor)!!.currentItem = candidate
            myFixture.finishLookup('\n')
        }
        assertEquals("sample.CompletedActivity", field.text)
        assertEquals("sample.CompletedActivity", panel.result().properties.single().value)
    }

    fun testClassValueUsesJavaReferenceEditorAndCommitsOnlyIntoPanel() {
        val data = ActivityPropertiesData("A", listOf(ActivityProperty("class", "sample.Old"), ActivityProperty("other", "text")), "")
        val panel = ActivityPropertiesPanel(data, project)
        val table = panel.propertiesTable
        assertTrue(table.getCellEditor(0, 1) is ActivityClassCellEditor)
        assertFalse(table.getCellEditor(0, 0) is ActivityClassCellEditor)
        assertFalse(table.getCellEditor(1, 1) is ActivityClassCellEditor)
        assertTrue(table.editCellAt(0, 1))
        val field = table.editorComponent as com.intellij.ui.EditorTextField
        val file = PsiDocumentManager.getInstance(project).getPsiFile(field.document)!!
        assertTrue(file is com.intellij.psi.JavaCodeFragment)
        assertEquals(true, file.getUserData(com.intellij.refactoring.ui.ClassNameReferenceEditor.CLASS_NAME_REFERENCE_FRAGMENT))
        assertEquals("sample.Old", field.text)
        WriteCommandAction.runWriteCommandAction(project) { field.text = "sample.SelectedActivity" }
        assertEquals("sample.SelectedActivity", panel.result().properties[0].value)
        assertEquals("sample.Old", data.properties[0].value)
        panel.propertiesModel.setValueAt("class", 1, 0)
        assertTrue(table.getCellEditor(1, 1) is ActivityClassCellEditor)
        panel.propertiesModel.setValueAt("changed", 0, 0)
        assertFalse(table.getCellEditor(0, 1) is ActivityClassCellEditor)
    }

    private class Dialogs : ModelDiagramDialogs {
        var diagram: (ActivityPropertiesData) -> ActivityPropertiesData? = { null }
        override fun editDiagram(data: ActivityPropertiesData) = diagram(data)
        var conditions = 0
        var activities = 0
        var condition: (String) -> String? = { null }
        var activity: (ActivityPropertiesData) -> ActivityPropertiesData? = { null }
        override fun editCondition(condition: String): String? { conditions++; return this.condition(condition) }
        override fun editActivity(data: ActivityPropertiesData): ActivityPropertiesData? { activities++; return activity(data) }
    }
    private data class Session(val canvas: ModelDiagramCanvas, val source: TextEditor,
        val editor: com.intellij.openapi.fileEditor.FileEditor, val kind: ChildCreationToolbar) {
        val document get() = source.editor.document
    }
    private val sessions = mutableListOf<Session>()
    private lateinit var dialogs: Dialogs
    private var previousKind: String? = null

    override fun setUp() {
        super.setUp()
        previousKind = PropertiesComponent.getInstance(project).getValue("glue.model.lastChildKind")
        PropertiesComponent.getInstance(project).unsetValue("glue.model.lastChildKind")
        dialogs = Dialogs()
        project.replaceService(ModelDiagramDialogs::class.java, dialogs, testRootDisposable)
    }

    override fun tearDown() {
        try {
            sessions.asReversed().forEach { Disposer.dispose(it.source); Disposer.dispose(it.editor) }
            PropertiesComponent.getInstance(project).setValue("glue.model.lastChildKind", previousKind)
        } finally { super.tearDown() }
    }

    private fun fixture() = """<Glue:GlueDiagram xmlns:Glue="http://www.glueframework.com/activitydiagram/" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
        <child xsi:type="Glue:Activity" id="2" name="A" location="20,40" size="100,-1" output="//@wire.0" custom="child">
          <property key="class" value="old" custom="property"/>
          <property key="description" value="first&#10;second" custom="description"/>
          <property key="obsolete" value="remove"/>
          <extension value="untouched"/>
        </child>
        <child xsi:type="Glue:Activity" id="9" name="B" location="220,40" size="100,-1" input="//@wire.0"/>
        <child xsi:type="Glue:InitialState" id="30" location="400,160"/>
        <wire id="150" guardcondition="guard" firstRelativePoint="100,55" lastRelativePoint="220,55" custom="wire">
          <bendpoint firstDimension="100,55" secondDimension="-100,55" weight="0.2" custom="bend"/>
        </wire>
        </Glue:GlueDiagram>""".trimIndent()

    private fun descendants(container: Container): List<java.awt.Component> = container.components.flatMap {
        listOf(it) + if (it is Container) descendants(it) else emptyList()
    }

    private fun open(text: String = fixture()): Session {
        val file = myFixture.addFileToProject("properties-${sessions.size}.glue_uml_ad", text).virtualFile
        val editor = ModelDesignEditorProvider().createEditor(project, file)
        val source = ModelSourceEditorProvider().createEditor(project, file) as TextEditor
        com.intellij.testFramework.EditorTestUtil.waitForLoading(source.editor)
        val components = descendants(editor.component)
        return Session(components.filterIsInstance<ModelDiagramCanvas>().single(), source, editor,
            components.filterIsInstance<ChildCreationToolbar>().single()).also { sessions.add(it) }
    }

    private fun mouse(canvas: ModelDiagramCanvas, id: Int, x: Int, y: Int, count: Int = 1, button: Int = MouseEvent.BUTTON1) {
        val modifiers = if (id == MouseEvent.MOUSE_PRESSED || id == MouseEvent.MOUSE_DRAGGED)
            if (button == MouseEvent.BUTTON3) MouseEvent.BUTTON3_DOWN_MASK else MouseEvent.BUTTON1_DOWN_MASK else 0
        canvas.dispatchEvent(MouseEvent(canvas, id, 1, modifiers, x, y, count, false,
            if (id == MouseEvent.MOUSE_DRAGGED || id == MouseEvent.MOUSE_MOVED) MouseEvent.NOBUTTON else button))
    }

    private fun click(canvas: ModelDiagramCanvas, x: Int, y: Int, count: Int = 1) {
        mouse(canvas, MouseEvent.MOUSE_PRESSED, x, y, count)
        mouse(canvas, MouseEvent.MOUSE_RELEASED, x, y, count)
        mouse(canvas, MouseEvent.MOUSE_CLICKED, x, y, count)
        UIUtil.dispatchAllInvocationEvents()
    }

    private fun doubleClick(canvas: ModelDiagramCanvas, x: Int, y: Int) {
        click(canvas, x, y)
        click(canvas, x, y, 2)
    }

    private fun performCreationAction(action: com.intellij.openapi.actionSystem.AnAction) {
        val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, DataContext.EMPTY_CONTEXT)
        action.update(event)
        if (event.presentation.isEnabled) action.actionPerformed(event)
        UIUtil.dispatchAllInvocationEvents()
    }

    private fun toggleCreation(session: Session) = performCreationAction(session.kind.toggleAction)

    fun testWireCreationPersistsBothReferencesAndSingleUndoRedo() {
        val session = open()
        val original = session.document.text
        performCreationAction(session.kind.wireCreationAction)
        assertTrue(session.canvas.wireCreationMode)
        assertNull(session.canvas.creationKind)
        click(session.canvas, 70, 55)
        mouse(session.canvas, MouseEvent.MOUSE_MOVED, 270, 55)
        assertNotNull(session.canvas.wireCreationPreview)
        assertEquals(original, session.document.text)
        click(session.canvas, 270, 55)
        val wires = root(session).subTags.filter { it.localName == "wire" }
        assertEquals(2, wires.size)
        assertEquals("151", wires.last().getAttributeValue("id"))
        val children = root(session).subTags.filter { it.localName == "child" }
        assertEquals("//@wire.0 //@wire.1", children[0].getAttributeValue("output"))
        assertEquals("//@wire.0 //@wire.1", children[1].getAttributeValue("input"))
        assertTrue(session.canvas.wireCreationMode)
        assertNull(session.canvas.wireCreationPreview)
        UndoManager.getInstance(project).undo(session.source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, session.document.text)
        UndoManager.getInstance(project).redo(session.source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(2, session.canvas.diagram.wires.size)
        assertEquals("2", session.canvas.diagram.wires.last().start!!.id)
        assertEquals("9", session.canvas.diagram.wires.last().end!!.id)
    }

    fun testWireCreationCancelModeSwitchAndSourceChangeDoNotCreateStaleWire() {
        val session = open()
        val original = session.document.text
        performCreationAction(session.kind.wireCreationAction)
        click(session.canvas, 70, 55)
        click(session.canvas, 70, 55)
        assertEquals(original, session.document.text)
        session.canvas.cancelDrag()
        assertFalse(session.canvas.wireCreationMode)
        assertEquals(original, session.document.text)
        performCreationAction(session.kind.wireCreationAction)
        click(session.canvas, 70, 55)
        WriteCommandAction.runWriteCommandAction(project) { session.document.setText(original.replace("name=\"A\"", "name=\"Changed\"")) }
        UIUtil.dispatchAllInvocationEvents()
        assertNull(session.canvas.wireCreationPreview)
        click(session.canvas, 270, 55)
        assertEquals(1, session.canvas.diagram.wires.size)
        performCreationAction(session.kind.toggleAction)
        assertFalse(session.canvas.wireCreationMode)
        assertEquals(DiagramNodeKind.Activity, session.canvas.creationKind)
        performCreationAction(session.kind.wireCreationAction)
        assertTrue(session.canvas.wireCreationMode)
        assertNull(session.canvas.creationKind)
    }

    fun testPresetMenuCreatesPropertiesDescriptionAndMatchesClassColors() {
        val settings = ModelEditorSettings.getInstance(project)
        val previous = settings.state
        try {
            val preset = ActivityPreset("Preset A", mutableListOf(PresetProperty("class", "sample.Preset"),
                PresetProperty("service-name", "alpha-service")), "preset description", "#80C0FF")
            settings.loadState(ModelEditorState(presets = mutableListOf(preset)))
            val session = open()
            val action = session.kind.splitAction.actionGroup.getChildren(null).single { it.templatePresentation.text == "Preset A" }
            performCreationAction(action)
            assertEquals("Preset A", session.kind.selectedPreset!!.name)
            assertEquals(DiagramNodeKind.Activity, session.canvas.creationKind)
            mouse(session.canvas, MouseEvent.MOUSE_MOVED, 500, 220)
            assertEquals("Preset A", session.canvas.ghostNode!!.name)
            assertEquals("sample.Preset", session.canvas.ghostNode!!.className)
            val ghost = render(session.canvas)
            assertEquals(Color(0x80C0FF).rgb, ghost.getRGB(460, 210))
            click(session.canvas, 500, 220)
            val created = root(session).subTags.filter { it.localName == "child" }.last()
            assertEquals("Preset A", DiagramXmlText.read(created, "name"))
            assertEquals(listOf("class", "service-name", "description"), created.subTags.map { DiagramXmlText.read(it, "key") })
            assertEquals("preset description", DiagramXmlText.read(created.subTags.last(), "value"))
            assertEquals(Color(0x80C0FF), session.canvas.activityColor(session.canvas.diagram.nodes.last()))
            UndoManager.getInstance(project).undo(session.source)
            UIUtil.dispatchAllInvocationEvents()
            assertTrue(session.canvas.diagram.nodes.none { it.name == "Preset A" })
            performCreationAction(session.kind.kindActions[0])
            assertNull(session.kind.selectedPreset)
        } finally { settings.loadState(previous) }
    }

    fun testPresetClassColorsUpdateExistingActivitiesWithoutXmlEdits() {
        val settings = ModelEditorSettings.getInstance(project)
        val previous = settings.state
        try {
            val session = open()
            val original = session.document.text
            settings.loadState(ModelEditorState(presets = mutableListOf(ActivityPreset("Old", mutableListOf(PresetProperty("class", "old")), color = "#FFAA88", foreground = "#102030"))))
            UIUtil.dispatchAllInvocationEvents()
            assertEquals(Color(0xFFAA88), session.canvas.activityColor(session.canvas.diagram.nodes.first()))
            assertNull(session.canvas.activityColor(session.canvas.diagram.nodes[1]))
            assertEquals(Color(0x102030), session.canvas.activityForeground(session.canvas.diagram.nodes.first()))
            assertNull(session.canvas.activityForeground(session.canvas.diagram.nodes[1]))
            assertEquals(original, session.document.text)
            session.editor.component.setSize(640, 480)
            session.editor.component.doLayout()
            val scroll = session.editor.component.components.filterIsInstance<JScrollPane>().single()
            scroll.doLayout(); scroll.viewport.doLayout()
            val image = render(session.canvas)
            assertEquals(Color(0xFFAA88).rgb, image.getRGB(30, 45))
            assertTrue((55..85).any { x -> (48..62).any { y -> image.getRGB(x, y) == Color(0x102030).rgb } })
            val mapped = session.canvas.miniMapProjection!!.map(DiagramPoint(45.0, 47.5))
            val miniature = Color(image.getRGB(mapped.x.toInt(), mapped.y.toInt()))
            assertTrue(miniature.red > miniature.green + 20)
            Files.createDirectories(Path.of("build/reports"))
            ImageIO.write(image, "png", Path.of("build/reports/model-preset-colors.png").toFile())
        } finally { settings.loadState(previous) }
    }

    fun testRootPropertiesEditOnlyOnConfirmAndUndoPreservesChildren() {
        val rootProperties = """<property key="title" value="old" custom="keep"/><property key="description" value="root description"/>"""
        val initialXml = fixture().replaceFirst("<child", "$rootProperties\n<child")
        val session = open(initialXml)
        val original = session.document.text
        val children = root(session).subTags.filter { it.localName in listOf("child", "wire") }.map { it.text }
        dialogs.diagram = { data ->
            assertEquals("root description", data.description)
            assertEquals(listOf("title"), data.properties.map { it.key })
            assertEquals(original, session.document.text)
            data.copy(properties = data.properties.map { it.copy(value = "new & value") }, description = "edited\nroot")
        }
        performCreationAction(session.kind.diagramPropertiesAction)
        assertEquals("new & value", session.canvas.diagram.properties.first { it.key == "title" }.value)
        assertEquals("edited\nroot", session.canvas.diagram.properties.first { it.key == "description" }.value)
        assertEquals(children, root(session).subTags.filter { it.localName in listOf("child", "wire") }.map { it.text })
        assertEquals("keep", root(session).subTags.first { it.getAttributeValue("key") == "title" }.getAttributeValue("custom"))
        UndoManager.getInstance(project).undo(session.source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, session.document.text)
        dialogs.diagram = { null }
        performCreationAction(session.kind.diagramPropertiesAction)
        assertEquals(original, session.document.text)
    }

    fun testRootEditorPanelHasOnlyPropertiesAndDescription() {
        val panel = ActivityPropertiesPanel(ActivityPropertiesData("", listOf(ActivityProperty("title", "A")), "description"),
            project, showOpenButtons = false, showName = false)
        val labels = descendants(panel).filterIsInstance<JLabel>().map { it.text }
        assertFalse(labels.contains("Name"))
        assertTrue(labels.containsAll(listOf("Properties", "Description")))
        assertEquals(2, panel.buttonRow.componentCount)
    }

    fun testRootPropertiesRejectConcurrentSourceChangesAndReadOnly() {
        val session = open()
        dialogs.diagram = { data ->
            WriteCommandAction.runWriteCommandAction(project) {
                session.document.setText(session.document.text.replace("<Glue:GlueDiagram ", "<Glue:GlueDiagram custom=\"latest\" "))
            }
            data.copy(properties = listOf(ActivityProperty("stale", "discard")))
        }
        performCreationAction(session.kind.diagramPropertiesAction)
        assertEquals("latest", root(session).getAttributeValue("custom"))
        assertTrue(root(session).subTags.none { it.localName == "property" })
        var calls = 0
        dialogs.diagram = { calls++; it }
        WriteCommandAction.runWriteCommandAction(project) { session.editor.file!!.isWritable = false }
        try {
            performCreationAction(session.kind.diagramPropertiesAction)
            assertEquals(0, calls)
        } finally { WriteCommandAction.runWriteCommandAction(project) { session.editor.file!!.isWritable = true } }
    }

    private fun creationPresentation(session: Session): com.intellij.openapi.actionSystem.Presentation {
        val action = session.kind.toggleAction
        val event = AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, DataContext.EMPTY_CONTEXT)
        action.update(event)
        return event.presentation
    }

    private fun root(session: Session): com.intellij.psi.xml.XmlTag {
        PsiDocumentManager.getInstance(project).commitDocument(session.document)
        return (PsiManager.getInstance(project).findFile(session.editor.file!!) as XmlFile).rootTag!!
    }

    private fun checkUndoAndSave(session: Session, original: String) {
        val edited = session.document.text
        assertTrue(edited != original)
        UndoManager.getInstance(project).undo(session.source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(original, session.document.text)
        UndoManager.getInstance(project).redo(session.source)
        UIUtil.dispatchAllInvocationEvents()
        assertEquals(edited, session.document.text)
        FileDocumentManager.getInstance().saveDocument(session.document)
        assertEquals(edited, String(session.editor.file!!.contentsToByteArray(), Charsets.UTF_8).replace("\r\n", "\n"))
    }

    fun testWireAndConditionLabelDoubleClickApplyOnlyConfirmedCondition() {
        val session = open()
        val original = session.document.text
        val before = session.canvas.diagram.wires.single()
        dialogs.condition = { assertEquals("guard", it); "x < 3 && a > 1" }
        doubleClick(session.canvas, 195, 89)
        assertEquals(1, dialogs.conditions)
        assertEquals("x < 3 && a > 1", session.canvas.diagram.wires.single().condition)
        assertEquals(before.copy(condition = "x < 3 && a > 1"), session.canvas.diagram.wires.single())
        checkUndoAndSave(session, original)
        val edited = session.document.text
        dialogs.condition = { null }
        val center = DiagramGeometry.labelCenter(session.canvas.wirePath(session.canvas.diagram.wires.single()))!!
        doubleClick(session.canvas, center.x.toInt(), center.y.toInt())
        assertEquals(2, dialogs.conditions)
        assertEquals(edited, session.document.text)
        dialogs.condition = { it }
        val stamp = session.document.modificationStamp
        doubleClick(session.canvas, center.x.toInt(), center.y.toInt())
        assertEquals(stamp, session.document.modificationStamp)
        dialogs.condition = { "" }
        doubleClick(session.canvas, 195, 89)
        assertEquals("", session.canvas.diagram.wires.single().condition)
    }

    fun testActivityNamePropertiesDescriptionApplyTogetherAndPreserveOtherXml() {
        val session = open()
        val original = session.document.text
        dialogs.activity = { initial ->
            assertEquals("A", initial.name)
            assertEquals("first\nsecond", initial.description)
            assertEquals(listOf("class", "obsolete"), initial.properties.map { it.key })
            ActivityPropertiesData("새 이름 & <A>", listOf(initial.properties.first().copy(key = "handler", value = "λ & <value>"),
                ActivityProperty("new-key", "new-value")), "첫 줄\n둘째 줄 <&>\n끝")
        }
        doubleClick(session.canvas, 70, 55)
        assertEquals(1, dialogs.activities)
        val child = root(session).subTags.first { it.getAttributeValue("id") == "2" }
        assertEquals("새 이름 & <A>", DiagramXmlText.read(child, "name"))
        assertEquals("child", child.getAttributeValue("custom"))
        assertEquals("20,40", child.getAttributeValue("location"))
        assertEquals("100,-1", child.getAttributeValue("size"))
        assertEquals("//@wire.0", child.getAttributeValue("output"))
        val props = child.subTags.filter { it.localName == "property" }
        assertEquals(setOf("handler", "new-key", "description"), props.map { it.getAttributeValue("key") }.toSet())
        val handler = props.single { it.getAttributeValue("key") == "handler" }
        assertEquals("property", handler.getAttributeValue("custom"))
        assertEquals("λ & <value>", DiagramXmlText.read(handler, "value"))
        val description = props.single { it.getAttributeValue("key") == "description" }
        assertEquals("description", description.getAttributeValue("custom"))
        assertEquals("첫 줄\n둘째 줄 <&>\n끝", DiagramXmlText.read(description, "value"))
        assertEquals("untouched", child.subTags.single { it.localName == "extension" }.getAttributeValue("value"))
        assertEquals("새 이름 & <A>", session.canvas.diagram.nodes.first().name)
        checkUndoAndSave(session, original)
    }

    fun testActivityCancelNoChangeAndDescriptionRemovalOrAddition() {
        val session = open()
        doubleClick(session.canvas, 70, 55)
        assertEquals(fixture(), session.document.text)
        dialogs.activity = { it }
        val stamp = session.document.modificationStamp
        doubleClick(session.canvas, 70, 55)
        assertEquals(stamp, session.document.modificationStamp)
        dialogs.activity = { it.copy(description = "") }
        doubleClick(session.canvas, 70, 55)
        assertTrue(root(session).subTags.first { it.getAttributeValue("id") == "2" }.subTags.none { it.getAttributeValue("key") == "description" })
        dialogs.activity = { it.copy(description = "new\ntext") }
        doubleClick(session.canvas, 70, 55)
        assertEquals("new\ntext", DiagramXmlText.read(root(session).subTags.first { it.getAttributeValue("id") == "2" }.subTags.single {
            it.getAttributeValue("key") == "description" }, "value"))
        val count = dialogs.activities
        doubleClick(session.canvas, 410, 170)
        assertEquals(count, dialogs.activities)
    }

    fun testActivityPanelSupportsRealCellEditingRowsAndDescriptionValidation() {
        val data = ActivityPropertiesData("Original", listOf(ActivityProperty("class", "old", 0)), "first\nsecond")
        val panel = ActivityPropertiesPanel(data)
        assertEquals("Original", panel.nameField.text)
        assertEquals("first\nsecond", panel.descriptionField.text)
        panel.nameField.text = "Edited"
        assertTrue(panel.propertiesTable.editCellAt(0, 1))
        (panel.propertiesTable.editorComponent as JTextField).text = "updated"
        assertEquals("updated", panel.result().properties.single().value)
        panel.addButton.doClick()
        (panel.propertiesTable.editorComponent as JTextField).text = "extra"
        panel.descriptionField.text = "changed\nline"
        val result = panel.result()
        assertEquals("Edited", result.name)
        assertEquals(listOf("class", "extra"), result.properties.map { it.key })
        assertEquals(0, result.properties.first().sourceIndex)
        assertNull(result.properties.last().sourceIndex)
        assertNull(result.validationError())
        panel.propertiesModel.setValueAt("class", 1, 0)
        assertNotNull(panel.result().validationError())
        panel.propertiesModel.setValueAt("description", 1, 0)
        assertNotNull(panel.result().validationError())
        panel.propertiesModel.setValueAt("", 1, 0)
        assertNotNull(panel.result().validationError())
        panel.propertiesTable.setRowSelectionInterval(1, 1)
        panel.removeButton.doClick()
        assertEquals(1, panel.propertiesModel.rowCount)
        assertEquals(data.properties.single().sourceIndex, panel.result().properties.single().sourceIndex)
        assertEquals("old", data.properties.single().value)
    }

    fun testModalEditDoesNotOverwriteSourceChangedWhileOpen() {
        val session = open()
        val changed = fixture().replace("name=\"A\"", "name=\"External\"")
        dialogs.activity = {
            WriteCommandAction.runWriteCommandAction(project) { session.document.setText(changed) }
            it.copy(name = "Stale")
        }
        doubleClick(session.canvas, 70, 55)
        assertEquals(changed, session.document.text)
        dialogs.condition = {
            WriteCommandAction.runWriteCommandAction(project) { session.document.setText(changed.replace("guardcondition=\"guard\"", "guardcondition=\"external\"")) }
            "stale"
        }
        doubleClick(session.canvas, 195, 89)
        assertEquals("external", session.canvas.diagram.wires.single().condition)
    }

    fun testReadonlyPreventsDialogsAndCreationAndRejectsModalCommit() {
        val session = open()
        dialogs.activity = {
            WriteCommandAction.runWriteCommandAction(project) { session.editor.file!!.isWritable = false }
            it.copy(name = "No")
        }
        try {
            doubleClick(session.canvas, 70, 55)
            assertEquals(fixture(), session.document.text)
            assertFalse(creationPresentation(session).isEnabled)
            val calls = dialogs.activities
            doubleClick(session.canvas, 70, 55)
            doubleClick(session.canvas, 195, 89)
            assertEquals(calls, dialogs.activities)
            assertEquals(0, dialogs.conditions)
            toggleCreation(session)
            assertNull(session.canvas.creationKind)
        } finally {
            WriteCommandAction.runWriteCommandAction(project) { session.editor.file!!.isWritable = true }
        }
    }

    fun testCreationToolbarRemembersKindAndGhostDoesNotModifyXml() {
        val session = open()
        assertEquals(3, session.kind.kindActions.size)
        assertEquals(DiagramNodeKind.Activity, session.kind.selectedKind)
        session.kind.selectedKind = DiagramNodeKind.InitialState
        assertNull(session.canvas.creationKind)
        toggleCreation(session)
        mouse(session.canvas, MouseEvent.MOUSE_MOVED, 350, 250)
        assertEquals(DiagramNodeKind.InitialState, session.canvas.ghostNode!!.kind)
        assertEquals(DiagramPoint(340.0, 240.0), session.canvas.ghostNode!!.location)
        assertEquals(fixture(), session.document.text)
        session.canvas.setSize(600, 400)
        val image = BufferedImage(600, 400, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { try { session.canvas.paint(it) } finally { it.dispose() } }
        assertEquals(Color.BLACK.rgb, image.getRGB(350, 250))
        assertTrue(image.getRGB(350, 250) != session.canvas.background.rgb)
        mouse(session.canvas, MouseEvent.MOUSE_EXITED, 350, 250)
        assertNull(session.canvas.ghostNode)
        mouse(session.canvas, MouseEvent.MOUSE_ENTERED, 350, 250)
        assertNotNull(session.canvas.ghostNode)
        toggleCreation(session)
        assertNull(session.canvas.creationKind)
        assertNull(session.canvas.ghostNode)
        toggleCreation(session)
        assertEquals(DiagramNodeKind.InitialState, session.canvas.creationKind)
        session.kind.selectedKind = DiagramNodeKind.FinalState
        mouse(session.canvas, MouseEvent.MOUSE_MOVED, 350, 250)
        assertEquals(DiagramNodeKind.FinalState, session.canvas.ghostNode!!.kind)
        val other = open("<GlueDiagram/>")
        assertEquals(DiagramNodeKind.FinalState, other.kind.selectedKind)
        assertNull(other.canvas.creationKind)
    }

    fun testCreationClickMatchesGhostPreservesWiresAndIsSingleUndo() {
        val session = open()
        val original = session.document.text
        val wires = session.canvas.diagram.wires
        val wireXml = root(session).subTags.filter { it.localName == "wire" }.map { it.text }
        toggleCreation(session)
        mouse(session.canvas, MouseEvent.MOUSE_MOVED, 350, 250)
        val ghost = session.canvas.ghostNode!!
        click(session.canvas, 350, 250)
        val added = session.canvas.selectedNodes.single()
        assertEquals("151", added.id)
        assertEquals(ghost.copy(id = "151"), added)
        assertEquals(wires, session.canvas.diagram.wires)
        assertEquals(wireXml, root(session).subTags.filter { it.localName == "wire" }.map { it.text })
        assertEquals(DiagramNodeKind.Activity, session.canvas.creationKind)
        assertNotNull(session.canvas.creationKind)
        checkUndoAndSave(session, original)
        session.kind.selectedKind = DiagramNodeKind.InitialState
        click(session.canvas, 380, 250)
        assertEquals("152", session.canvas.selectedNodes.single().id)
        assertEquals(DiagramNodeKind.InitialState, session.canvas.selectedNodes.single().kind)
        session.kind.selectedKind = DiagramNodeKind.FinalState
        click(session.canvas, 410, 250)
        assertEquals("153", session.canvas.selectedNodes.single().id)
        assertEquals(DiagramNodeKind.FinalState, session.canvas.selectedNodes.single().kind)
        assertEquals("Glue:FinalState", root(session).subTags.single { it.getAttributeValue("id") == "153" }
            .getAttributeValue("type", "http://www.w3.org/2001/XMLSchema-instance"))
    }

    fun testCreationNavigationNegativePositionsEscAndActionIsolation() {
        val session = open("<GlueDiagram/>")
        session.kind.selectedKind = DiagramNodeKind.FinalState
        toggleCreation(session)
        mouse(session.canvas, MouseEvent.MOUSE_PRESSED, 200, 100, button = MouseEvent.BUTTON3)
        mouse(session.canvas, MouseEvent.MOUSE_DRAGGED, 300, 220, button = MouseEvent.BUTTON3)
        mouse(session.canvas, MouseEvent.MOUSE_RELEASED, 300, 220, button = MouseEvent.BUTTON3)
        mouse(session.canvas, MouseEvent.MOUSE_MOVED, 50, 50)
        val ghost = session.canvas.ghostNode!!
        assertEquals(DiagramPoint(-60.0, -80.0), ghost.location)
        click(session.canvas, 50, 50)
        assertEquals(ghost.copy(id = "1"), session.canvas.selectedNodes.single())
        assertEquals(DiagramPoint(100.0, 120.0), session.canvas.pan)
        assertEquals(DiagramPoint(40.0, 40.0), session.canvas.selectedNodes.single().location + session.canvas.screenOffset)
        assertFalse(session.canvas.copyProvider!!.isCopyEnabled(DataContext.EMPTY_CONTEXT))
        assertFalse(session.canvas.pasteProvider!!.isPasteEnabled(DataContext.EMPTY_CONTEXT))
        assertFalse(session.canvas.deleteProvider!!.canDeleteElement(DataContext.EMPTY_CONTEXT))
        val action = ActionUtil.getActions(session.canvas).single { it.templatePresentation.text == "드래그 취소" }
        action.actionPerformed(AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, DataContext.EMPTY_CONTEXT))
        assertNull(session.canvas.creationKind)
        assertNull(session.canvas.ghostNode)
        assertNull(session.canvas.creationKind)
        assertTrue(session.canvas.copyProvider!!.isCopyEnabled(DataContext.EMPTY_CONTEXT))
    }

    fun testInvalidSourceTurnsOffCreationAndDoubleClickDoesNotCreateTwice() {
        val session = open()
        toggleCreation(session)
        doubleClick(session.canvas, 350, 250)
        assertEquals(4, session.canvas.diagram.nodes.size)
        assertEquals(0, dialogs.activities)
        assertEquals(0, dialogs.conditions)
        WriteCommandAction.runWriteCommandAction(project) { session.document.setText("<GlueDiagram><child") }
        UIUtil.dispatchAllInvocationEvents()
        assertFalse(creationPresentation(session).isEnabled)
        assertNull(session.canvas.creationKind)
        assertNull(session.canvas.creationKind)
        click(session.canvas, 350, 250)
        assertEquals("<GlueDiagram><child", session.document.text)
    }

    private fun layoutTree(container: Container) {
        container.doLayout()
        container.components.filterIsInstance<Container>().forEach(::layoutTree)
    }

    private fun render(canvas: ModelDiagramCanvas): BufferedImage {
        canvas.setSize(600, 400)
        val image = BufferedImage(600, 400, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { try { canvas.paint(it) } finally { it.dispose() } }
        return image
    }

    fun testTransitionDialogAndActivityPanelLayout() {
        val transition = WireConditionDialog(project, "guard")
        try {
            assertEquals("Transition 편집", transition.title)
            assertEquals(22, transition.conditionField.columns)
            assertTrue(transition.conditionField.preferredSize.width < com.intellij.ui.components.JBTextField("", 45).preferredSize.width * 0.55)
            assertTrue(descendants(transition.editorPanel).filterIsInstance<JLabel>().none { it.text == "guardcondition" })
        } finally { transition.disposeIfNeeded() }
        val panel = ActivityPropertiesPanel(ActivityPropertiesData("Sample Activity", listOf(
            ActivityProperty("class", "sample.SampleActivity", 0), ActivityProperty("result-key", "result", 1)), "첫 번째 줄\n두 번째 줄")) { false }
        val labels = descendants(panel).filterIsInstance<JLabel>().map { it.text }
        assertTrue(labels.containsAll(listOf("Name", "Properties", "Description")))
        assertEquals(listOf("Add", "Delete", "Open Class", "Open Query", "Open Service"), panel.buttonRow.components.map { (it as javax.swing.JButton).text })
        assertFalse(panel.openQueryButton.isEnabled)
        assertFalse(panel.openServiceButton.isEnabled)
        panel.setSize(panel.preferredSize)
        layoutTree(panel)
        assertEquals(1, panel.buttonRow.components.map { it.width }.distinct().size)
        assertEquals(panel.width, panel.buttonRow.width)
        assertTrue(panel.propertiesTable.columnModel.getColumn(0).width < panel.propertiesTable.width * 0.35)
        assertTrue(panel.descriptionField.insets.top >= com.intellij.util.ui.JBUI.scale(8))
        val image = BufferedImage(panel.width, panel.height, BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { try { panel.paint(it) } finally { it.dispose() } }
        Files.createDirectories(Path.of("build/reports"))
        ImageIO.write(image, "png", Path.of("build/reports/model-activity-properties.png").toFile())
    }

    private fun tableAction(panel: ActivityPropertiesPanel, id: String) {
        val context = DataManager.getInstance().customizeDataContext(DataContext.EMPTY_CONTEXT, panel.propertiesTable)
        val provider = if (id == IdeActions.ACTION_COPY) PlatformDataKeys.COPY_PROVIDER.getData(context)
            else PlatformDataKeys.PASTE_PROVIDER.getData(context)
        assertNotNull(provider)
        val action = ActionManager.getInstance().getAction(id)
        action.actionPerformed(AnActionEvent.createFromAnAction(action, null, ActionPlaces.UNKNOWN, context))
    }

    fun testPropertyRowsCopyPastePreservesValuesAndCreatesNewXmlRows() {
        val initial = ActivityPropertiesData("A", listOf(ActivityProperty("first", "line1\nline2\t\"quoted\"", 0),
            ActivityProperty("middle", "skip", 2), ActivityProperty("last", "value", 4)), "description")
        val panel = ActivityPropertiesPanel(initial)
        panel.propertiesTable.addRowSelectionInterval(0, 0)
        panel.propertiesTable.addRowSelectionInterval(2, 2)
        tableAction(panel, IdeActions.ACTION_COPY)
        assertEquals(listOf("first", "last"), PropertyRowsClipboard.read()!!.map { it.key })
        assertTrue(PropertyRowsClipboard.read()!!.all { it.sourceIndex == null })
        val exported = CopyPasteManager.getInstance().getContents<String>(DataFlavor.stringFlavor)!!
        CopyPasteManager.getInstance().setContents(StringSelection(exported))
        assertEquals(listOf(initial.properties.first().copy(sourceIndex = null), initial.properties.last().copy(sourceIndex = null)), PropertyRowsClipboard.read())
        panel.propertiesTable.setRowSelectionInterval(1, 1)
        tableAction(panel, IdeActions.ACTION_PASTE)
        assertEquals(listOf("first", "middle", "first", "last", "last"), panel.result().properties.map { it.key })
        assertEquals(listOf(0, 2, null, null, 4), panel.result().properties.map { it.sourceIndex })
        assertEquals(listOf(2, 3), panel.propertiesTable.selectedRows.toList())
        assertNotNull(panel.result().validationError())
        assertEquals(3, initial.properties.size)
        val target = ActivityPropertiesPanel(ActivityPropertiesData("B", emptyList(), ""))
        tableAction(target, IdeActions.ACTION_PASTE)
        assertEquals(listOf("first", "last"), target.result().properties.map { it.key })
        assertNull(target.result().validationError())
        assertEquals("description", panel.descriptionField.text)
    }

    fun testExternalTsvAndTableClipboardDoNotCaptureCellTextEditing() {
        val panel = ActivityPropertiesPanel(ActivityPropertiesData("A", emptyList(), ""))
        CopyPasteManager.getInstance().setContents(StringSelection("external\t\"line1\nline2\t\"\"q\"\"\"\r\n"))
        tableAction(panel, IdeActions.ACTION_PASTE)
        assertEquals(ActivityProperty("external", "line1\nline2\t\"q\""), panel.result().properties.single())
        assertTrue(panel.propertiesTable.editCellAt(0, 1))
        assertFalse(panel.propertiesTable.rowsCopyProvider.isCopyEnabled(DataContext.EMPTY_CONTEXT))
        assertFalse(panel.propertiesTable.rowsPasteProvider.isPasteEnabled(DataContext.EMPTY_CONTEXT))
        panel.propertiesTable.cellEditor.stopCellEditing()
        panel.propertiesTable.clearSelection()
        val before = PropertyRowsClipboard.read()
        panel.propertiesTable.rowsCopyProvider.performCopy(DataContext.EMPTY_CONTEXT)
        assertEquals(before, PropertyRowsClipboard.read())
        for (text in listOf("plain text", "key\t\"unterminated", "a\tb\tc")) {
            CopyPasteManager.getInstance().setContents(StringSelection(text))
            assertFalse(panel.propertiesTable.rowsPasteProvider.isPasteEnabled(DataContext.EMPTY_CONTEXT))
        }
    }

    fun testOpenClassUsesCurrentCellValueAndOpensSourceWithoutApplyingActivity() {
        val type = (myFixture.addFileToProject("sample/RealActivity.java", "package sample; public class RealActivity {}")
            as com.intellij.psi.PsiJavaFile).classes.single()
        val session = open()
        var openedName: String? = null
        dialogs.activity = { data ->
            val panel = ActivityPropertiesPanel(data) { name -> openedName = name; ActivityClassNavigation.open(project, name) }
            assertTrue(panel.propertiesTable.editCellAt(0, 1))
            (panel.propertiesTable.editorComponent as JTextField).text = "  sample.RealActivity  "
            panel.openClassButton.doClick()
            assertFalse(panel.openClassMessage.isVisible)
            assertEquals(fixture(), session.document.text)
            null
        }
        doubleClick(session.canvas, 70, 55)
        assertEquals("sample.RealActivity", openedName)
        assertTrue(FileEditorManager.getInstance(project).isFileOpen(type.containingFile.virtualFile))
        assertEquals(fixture(), session.document.text)
    }

    fun testOpenClassFailureKeepsInputsAndShowsInlineMessage() {
        val panel = ActivityPropertiesPanel(ActivityPropertiesData("Edited", listOf(ActivityProperty("class", "sample.Missing")), "pending")) {
            ActivityClassNavigation.open(project, it)
        }
        panel.openClassButton.doClick()
        assertTrue(panel.openClassMessage.isVisible)
        assertTrue(panel.openClassMessage.text.contains("sample.Missing"))
        assertEquals("Edited", panel.nameField.text)
        assertEquals("pending", panel.descriptionField.text)
        panel.propertiesModel.setValueAt("", 0, 1)
        panel.openClassButton.doClick()
        assertTrue(panel.openClassMessage.isVisible)
        assertTrue(panel.openClassMessage.text.contains("입력"))
    }

    fun testCreationControlUsesNativeActionToolbarAndSplitButton() {
        val session = open()
        assertTrue(descendants(session.editor.component).none { (it is JComboBox<*> && it.name != "model.zoom.select") || it is JToggleButton })
        assertEquals(listOf(session.kind.splitAction, session.kind.wireCreationAction, session.kind.magnetAction, session.kind.diagramPropertiesAction), session.kind.toolbar.actions)
        assertSame(session.canvas, session.kind.toolbar.targetComponent)
        assertTrue(session.kind.splitAction is com.intellij.openapi.actionSystem.SplitButtonAction)
        assertTrue(session.kind.toggleAction is com.intellij.openapi.actionSystem.ToggleAction)
        assertEquals(3, session.kind.splitAction.actionGroup.getChildren(null).size)
        performCreationAction(session.kind.kindActions[2])
        assertEquals(DiagramNodeKind.FinalState, session.kind.selectedKind)
        assertEquals("FinalState", creationPresentation(session).text)
        assertNull(creationPresentation(session).icon)
        assertEquals(listOf("빈 Activity", "InitialState", "FinalState"), session.kind.kindActions.map { it.templatePresentation.text })
        val menuEvent = AnActionEvent.createFromAnAction(session.kind.kindActions[2], null, ActionPlaces.UNKNOWN, DataContext.EMPTY_CONTEXT)
        session.kind.kindActions[2].update(menuEvent)
        assertEquals(com.intellij.openapi.actionSystem.KeepPopupOnPerform.Never, menuEvent.presentation.keepPopupOnPerform)
        val wireEvent = AnActionEvent.createFromAnAction(session.kind.wireCreationAction, null, ActionPlaces.TOOLBAR, DataContext.EMPTY_CONTEXT)
        session.kind.wireCreationAction.update(wireEvent)
        assertEquals("Wire", wireEvent.presentation.text)
        assertNull(wireEvent.presentation.icon)
        assertEquals(true, wireEvent.presentation.getClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR))
        val textControl = session.kind.splitAction.createCustomComponent(creationPresentation(session), ActionPlaces.TOOLBAR)
        (textControl as com.intellij.openapi.actionSystem.ActionToolbar).updateActionsImmediately()
        UIUtil.dispatchAllInvocationEvents()
        assertTrue(descendants(textControl).any { it is com.intellij.openapi.actionSystem.impl.ActionButtonWithText })
        textControl.setSize(textControl.preferredSize)
        textControl.doLayout()
        val toolbarImage = BufferedImage(textControl.width, textControl.height, BufferedImage.TYPE_INT_ARGB)
        toolbarImage.createGraphics().let { try { textControl.paint(it) } finally { it.dispose() } }
        Files.createDirectories(Path.of("build/reports"))
        ImageIO.write(toolbarImage, "png", Path.of("build/reports/model-text-toolbar.png").toFile())
        assertEquals(DiagramNodeKind.FinalState, session.canvas.creationKind)
        toggleCreation(session)
        assertNull(session.canvas.creationKind)
        performCreationAction(session.kind.kindActions[2])
        assertEquals(DiagramNodeKind.FinalState, session.canvas.creationKind)
        performCreationAction(session.kind.kindActions[0])
        assertEquals(DiagramNodeKind.Activity, session.canvas.creationKind)
        assertTrue(com.intellij.openapi.actionSystem.Toggleable.isSelected(creationPresentation(session)))
        toggleCreation(session)
        assertNull(session.canvas.creationKind)
        assertFalse(com.intellij.openapi.actionSystem.Toggleable.isSelected(creationPresentation(session)))
    }
    fun testGhostUsesExactlyTheUnselectedCreatedNodeStyle() {
        val session = open()
        toggleCreation(session)
        for (kind in DiagramNodeKind.entries) {
            session.kind.selectedKind = kind
            mouse(session.canvas, MouseEvent.MOUSE_MOVED, 350, 250)
            val node = session.canvas.ghostNode!!
            val ghost = render(session.canvas)
            val actualCanvas = ModelDiagramCanvas()
            actualCanvas.font = session.canvas.font
            actualCanvas.foreground = session.canvas.foreground
            actualCanvas.background = session.canvas.background
            actualCanvas.display(ModelDiagram(listOf(node.copy(id = "actual")), emptyList()))
            val actual = render(actualCanvas)
            for (y in node.location.y.toInt() - 2..(node.location.y + node.height).toInt() + 2) {
                for (x in node.location.x.toInt() - 2..(node.location.x + node.width).toInt() + 2) {
                    assertEquals("$kind at $x,$y", actual.getRGB(x, y), ghost.getRGB(x, y))
                }
            }
        }
    }
}
