package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.modeleditor.*
import java.awt.Color

class ModelEditorSettingsTest : BasePlatformTestCase() {
    fun testNoUseModelFileCheckboxApplyResetAndPersistence() {
        val settings = ModelEditorSettings.getInstance(project)
        val configurable = GlueModelEditorConfigurable(project)
        try {
            configurable.createComponent()
            var panel = configurable.panel!!
            assertEquals("No use .glue_uml_ad file", panel.noUseGlueUmlAdFile.text)
            assertFalse(panel.noUseGlueUmlAdFile.isSelected)
            assertFalse(configurable.isModified())
            panel.noUseGlueUmlAdFile.doClick()
            assertTrue(configurable.isModified())
            assertFalse(settings.state.noUseGlueUmlAdFile)
            configurable.apply()
            assertTrue(settings.state.noUseGlueUmlAdFile)
            assertFalse(configurable.isModified())
            panel.noUseGlueUmlAdFile.doClick()
            assertTrue(configurable.isModified())
            configurable.reset()
            assertTrue(panel.noUseGlueUmlAdFile.isSelected)
            assertFalse(configurable.isModified())
            val saved = com.intellij.util.xmlb.XmlSerializer.serialize(settings.state)
            val restored = com.intellij.util.xmlb.XmlSerializer.deserialize(saved, ModelEditorState::class.java)
            settings.loadState(restored)
            configurable.disposeUIResources()
            configurable.createComponent()
            panel = configurable.panel!!
            assertTrue(panel.noUseGlueUmlAdFile.isSelected)
            assertFalse(configurable.isModified())
            panel.noUseGlueUmlAdFile.doClick()
            configurable.apply()
            assertFalse(settings.state.noUseGlueUmlAdFile)
            val legacy = com.intellij.util.xmlb.XmlSerializer.deserialize(org.jdom.Element("ModelEditorState"), ModelEditorState::class.java)
            assertFalse(legacy.noUseGlueUmlAdFile)
        } finally { configurable.disposeUIResources() }
    }

    fun testLegacyPresetBackgroundKeepsDefaultForeground() {
        val old = org.jdom.Element("ActivityPreset").apply {
            addContent(org.jdom.Element("option").setAttribute("name", "color").setAttribute("value", "#80C0FF"))
        }
        val preset = com.intellij.util.xmlb.XmlSerializer.deserialize(old, ActivityPreset::class.java)
        assertEquals("#80C0FF", preset.color)
        assertEquals("#919191", preset.foreground)
    }
    fun testOpenServiceFindsProjectFileOutsideConfiguredDirectory() {
        myFixture.addFileToProject("services/sample2-service.xml", "<service/>")
        val target = myFixture.addFileToProject("flows/nested/sample2.glue_uml_ad", "<GlueDiagram/>").virtualFile
        assertEquals(target, ServiceFiles.resolve(project, "sample2-service"))
        ModelEditorSettings.getInstance(project).loadState(ModelEditorState(serviceDirectory = "services"))
        assertEquals(target, ServiceFiles.resolve(project, "sample2-service.xml"))
        assertTrue(ServiceFiles.open(project, "sample2-service"))
        com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).closeFile(target)
        val another = myFixture.addFileToProject("other/sample2.glue_uml_ad", "<GlueDiagram/>").virtualFile
        assertEquals(setOf(target, another), ServiceFiles.find(project, "sample2-service").toSet())
        assertTrue(ServiceFiles.find(project, "../sample2-service").isEmpty())
    }
    private lateinit var previous: ModelEditorState
    override fun setUp() {
        super.setUp()
        previous = ModelEditorSettings.getInstance(project).state
        ModelEditorSettings.getInstance(project).loadState(ModelEditorState())
    }
    override fun tearDown() {
        try { ModelEditorSettings.getInstance(project).loadState(previous) } finally { super.tearDown() }
    }
    fun testSettingsApplyResetAndPresetColorAreDetached() {
        val settings = ModelEditorSettings.getInstance(project)
        val configurable = GlueModelEditorConfigurable(project)
        configurable.createComponent()
        configurable.reset()
        assertFalse(configurable.isModified())
        configurable.panel!!.addPreset()
        val panel = configurable.panel!!
        panel.editor!!.nameField.text = "Preset"
        panel.editor!!.propertiesModel.addRow()
        panel.editor!!.propertiesModel.setValueAt("class", 0, 0)
        panel.editor!!.propertiesModel.setValueAt("sample.Activity", 0, 1)
        panel.color.selectedColor = Color(20, 100, 200)
        assertEquals(Color(145, 145, 145), panel.foregroundColor.selectedColor)
        panel.foregroundColor.selectedColor = Color(10, 20, 30)
        assertTrue(configurable.isModified())
        assertTrue(settings.state.presets.isEmpty())
        configurable.apply()
        assertEquals("Preset", settings.state.presets.single().name)
        assertEquals("#1464C8", settings.state.presets.single().color)
        assertEquals("#0A141E", settings.state.presets.single().foreground)
        assertEquals(Color(10, 20, 30), settings.foregroundForClass("sample.Activity"))
        val serialized = com.intellij.util.xmlb.XmlSerializer.serialize(settings.state)
        assertEquals(settings.state, com.intellij.util.xmlb.XmlSerializer.deserialize(serialized, ModelEditorState::class.java))
        val detached = settings.state
        detached.presets[0].name = "mutated"
        assertEquals("Preset", settings.state.presets.single().name)
        panel.editor!!.nameField.text = "discard"
        configurable.reset()
        assertEquals("Preset", configurable.panel!!.editor!!.nameField.text)
        assertEquals(Color(10, 20, 30), panel.foregroundColor.selectedColor)
        assertFalse(configurable.panel!!.editor!!.openClassButton.isVisible)
        assertFalse(configurable.panel!!.editor!!.openQueryButton.isVisible)
        assertFalse(configurable.panel!!.editor!!.openServiceButton.isVisible)
        panel.setSize(680, 440)
        fun layout(component: java.awt.Component) {
            if (component is java.awt.Container) { component.doLayout(); component.components.forEach(::layout) }
        }
        layout(panel)
        fun descendants(component: java.awt.Component): Sequence<java.awt.Component> = sequence {
            yield(component)
            if (component is java.awt.Container) component.components.forEach { yieldAll(descendants(it)) }
        }
        descendants(panel).filterIsInstance<javax.swing.JScrollPane>().forEach {
            assertFalse(it.horizontalScrollBar.isVisible)
            assertFalse(it.verticalScrollBar.isVisible)
        }
        val image = java.awt.image.BufferedImage(panel.width, panel.height, java.awt.image.BufferedImage.TYPE_INT_ARGB)
        image.createGraphics().let { try { panel.paint(it) } finally { it.dispose() } }
        val output = java.nio.file.Path.of("build/reports/model-settings.png")
        java.nio.file.Files.createDirectories(output.parent)
        javax.imageio.ImageIO.write(image, "png", output.toFile())
        configurable.disposeUIResources()
        assertFalse(configurable.isModified())
    }
    fun testServiceCandidatesUseXmlButOpenUsesUmlExtension() {
        myFixture.addFileToProject("services/alpha.xml", "<service/>")
        val model = myFixture.addFileToProject("services/alpha.glue_uml_ad", "<GlueDiagram/>").virtualFile
        myFixture.addFileToProject("services/ignored.txt", "")
        myFixture.addFileToProject("services/nested/hidden.xml", "<service/>")
        val directory = model.parent
        ModelEditorSettings.getInstance(project).loadState(ModelEditorState(serviceDirectory = directory.url))
        assertEquals(listOf("alpha"), ServiceFiles.names(project))
        assertEquals(model, ServiceFiles.resolve(project, "alpha"))
        assertNull(ServiceFiles.resolve(project, "../alpha"))
        assertNull(ServiceFiles.resolve(project, "missing"))
        assertTrue(ServiceFiles.open(project, "alpha"))
        com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).closeFile(model)
    }
    fun testServiceNameCompletionAndOpenButtonsUseCurrentCellValues() {
        myFixture.addFileToProject("services/alpha-service.xml", "<service/>")
        val file = myFixture.addFileToProject("services/alpha.glue_uml_ad", "<GlueDiagram/>").virtualFile
        ModelEditorSettings.getInstance(project).loadState(ModelEditorState(serviceDirectory = file.parent.url))
        var openedQuery = ""
        var openedService = ""
        val original = ActivityPropertiesData("A", listOf(ActivityProperty("service-name", "alpha-"), ActivityProperty("sql-key", "old")), "")
        val panel = ActivityPropertiesPanel(original, project, onOpenQuery = { openedQuery = it; true }, onOpenService = { openedService = it; true })
        assertTrue(panel.propertiesTable.getCellEditor(0, 1) is ServiceNameCellEditor)
        panel.propertiesTable.editCellAt(0, 1)
        val input = panel.propertiesTable.editorComponent as com.intellij.ui.EditorTextField
        assertTrue(input is com.intellij.util.textCompletion.TextFieldWithCompletion)
        val psi = com.intellij.psi.PsiDocumentManager.getInstance(project).getPsiFile(input.document)!!
        myFixture.configureFromExistingVirtualFile(psi.virtualFile)
        myFixture.editor.caretModel.moveToOffset(input.text.length)
        val variants = myFixture.completeBasic()
        if (!variants.isNullOrEmpty()) {
            com.intellij.codeInsight.lookup.LookupManager.getActiveLookup(myFixture.editor)!!.currentItem = variants.first { it.lookupString == "alpha-service" }
            myFixture.finishLookup('\n')
        }
        assertEquals("alpha-service", input.text)
        panel.openServiceButton.doClick()
        assertEquals("alpha-service", openedService)
        panel.propertiesTable.editCellAt(1, 1)
        val query = panel.propertiesTable.editorComponent as com.intellij.ui.EditorTextField
        com.intellij.openapi.command.WriteCommandAction.runWriteCommandAction(project) { query.text = "new-query" }
        panel.openQueryButton.doClick()
        assertEquals("new-query", openedQuery)
        assertEquals("old", original.properties[1].value)
    }

    fun testProjectRelativeDirectoryAndServiceSuffixes() {
        val model = myFixture.addFileToProject("services/order.glue_uml_ad", "<GlueDiagram/>").virtualFile
        myFixture.addFileToProject("services/order-service.xml", "<service/>")
        ModelEditorSettings.getInstance(project).loadState(ModelEditorState(serviceDirectory = "services"))
        assertEquals(model.parent, ServiceFiles.directory(project))
        assertEquals("services", ServiceFiles.relativeDirectory(project, model.parent.url))
        assertEquals(listOf("order-service"), ServiceFiles.names(project))
        listOf("order-service.xml", "order-service", "order.XML", "order-SERVICE.XML").forEach {
            assertEquals(model, ServiceFiles.resolve(project, it))
        }
        assertNull(ServiceFiles.directory(project, "../outside"))
        val configurable = GlueModelEditorConfigurable(project)
        configurable.createComponent()
        assertTrue(configurable is com.intellij.openapi.options.Configurable.NoScroll)
        configurable.panel!!.serviceDirectory.text = "../outside"
        assertThrows(com.intellij.openapi.options.ConfigurationException::class.java) { configurable.apply() }
        configurable.panel!!.serviceDirectory.text = model.parent.url
        configurable.apply()
        assertEquals("services", ModelEditorSettings.getInstance(project).state.serviceDirectory)
        configurable.disposeUIResources()
    }

    fun testExistingXmlServiceValueCanCompleteToCanonicalName() {
        myFixture.addFileToProject("services/order-service.xml", "<service/>")
        val model = myFixture.addFileToProject("services/order.glue_uml_ad", "<GlueDiagram/>").virtualFile
        ModelEditorSettings.getInstance(project).loadState(ModelEditorState(serviceDirectory = "services"))
        val panel = ActivityPropertiesPanel(ActivityPropertiesData("A", listOf(ActivityProperty("service-name", "order-service.xml")), ""), project)
        panel.propertiesTable.editCellAt(0, 1)
        val field = panel.propertiesTable.editorComponent as com.intellij.ui.EditorTextField
        val file = com.intellij.psi.PsiDocumentManager.getInstance(project).getPsiFile(field.document)!!
        myFixture.configureFromExistingVirtualFile(file.virtualFile)
        myFixture.editor.caretModel.moveToOffset(field.text.length)
        val variants = myFixture.completeBasic()
        if (!variants.isNullOrEmpty()) {
            com.intellij.codeInsight.lookup.LookupManager.getActiveLookup(myFixture.editor)!!.currentItem = variants.first { it.lookupString == "order-service" }
            myFixture.finishLookup('\n')
        }
        assertEquals("order-service", field.text)
        assertEquals(model, ServiceFiles.resolve(project, field.text))
    }
}
