package com.look4counter.modeleditor

import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.Project
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.fileChooser.FileChooser
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.ColorPanel
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.components.JBScrollPane
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Dimension
import java.awt.GridLayout
import javax.swing.DefaultListModel
import javax.swing.DefaultListCellRenderer
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JLabel
import javax.swing.JList
import javax.swing.JPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.FormBuilder

class GlueModelEditorConfigurable(private val project: Project) : SearchableConfigurable, Configurable.NoScroll {
    var panel: ModelEditorSettingsPanel? = null
        private set
    override fun getId() = "com.look4counter.glueframework.modeleditor"
    override fun getDisplayName() = "GlueModelEditor"
    override fun createComponent(): JComponent = panel ?: ModelEditorSettingsPanel(project).also { panel = it; reset() }
    override fun isModified() = panel?.let {
        val saved = ModelEditorSettings.getInstance(project).state
        saved.serviceDirectory = ServiceFiles.relativeDirectory(project, saved.serviceDirectory) ?: saved.serviceDirectory
        it.value() != saved
    } ?: false
    override fun reset() { panel?.load(ModelEditorSettings.getInstance(project).state) }
    override fun apply() {
        val state = panel?.value() ?: return
        if (state.serviceDirectory.isNotBlank() && ServiceFiles.directory(project, state.serviceDirectory) == null)
            throw ConfigurationException("프로젝트 내부의 Service 저장 디렉터리를 선택하세요.")
        state.presets.forEach { preset ->
            if (preset.name.isBlank()) throw ConfigurationException("프리셋 Name을 입력하세요.")
            ActivityPropertiesData(preset.name, preset.properties.map { ActivityProperty(it.key, it.value) }, preset.description)
                .validationError()?.let { throw ConfigurationException(it) }
        }
        ModelEditorSettings.getInstance(project).loadState(state)
    }
    override fun disposeUIResources() { panel = null }
}

class ModelEditorSettingsPanel(private val project: Project) : JPanel(BorderLayout(8, 8)) {
    val serviceDirectory = TextFieldWithBrowseButton()
    val noUseGlueUmlAdFile = JBCheckBox("No use .glue_uml_ad file")
    val color = ColorPanel()
    val foregroundColor = ColorPanel()
    private val model = DefaultListModel<ActivityPreset>()
    val list = JBList(model)
    private val detail = JPanel(BorderLayout(0, 8))
    var editor: ActivityPropertiesPanel? = null
        private set
    private var selected = -1
    private var loading = false

    init {
        border = JBUI.Borders.empty(8)
        minimumSize = Dimension(0, 0)
        serviceDirectory.textField.columns = 24
        val chooser = FileChooserDescriptorFactory.createSingleFolderDescriptor().withTitle("프로젝트의 Service 저장 경로")
        chooser.isForcedToUseIdeaFileChooser = true
        ServiceFiles.projectDirectory(project)?.let { chooser.withRoots(it) }
        serviceDirectory.addActionListener {
            val chosen = FileChooser.chooseFile(chooser, project, ServiceFiles.directory(project, serviceDirectory.text))
            if (chosen != null) ServiceFiles.relativeDirectory(project, chosen.url)?.let { serviceDirectory.text = it }
        }
        add(FormBuilder.createFormBuilder().addLabeledComponent("Service 저장 경로", serviceDirectory)
            .addComponent(noUseGlueUmlAdFile).panel, BorderLayout.NORTH)
        list.cellRenderer = object : DefaultListCellRenderer() {
            override fun getListCellRendererComponent(list: JList<*>, value: Any?, index: Int, selected: Boolean, focus: Boolean): java.awt.Component {
                super.getListCellRendererComponent(list, (value as? ActivityPreset)?.name.orEmpty(), index, selected, focus)
                return this
            }
        }
        val buttons = JPanel(GridLayout(1, 2, 6, 0)).apply {
            add(JButton("Add").apply { addActionListener { addPreset() } })
            add(JButton("Delete").apply { addActionListener {
                val index = list.selectedIndex
                if (index >= 0) { saveCurrent(); loading = true; this@ModelEditorSettingsPanel.model.remove(index); selected = -1; loading = false
                    list.selectedIndex = minOf(index, this@ModelEditorSettingsPanel.model.size() - 1); showCurrent() }
            } })
        }
        val left = JPanel(BorderLayout(0, 6)).apply {
            add(JLabel("Activity presets"), BorderLayout.NORTH); add(JBScrollPane(list)); add(buttons, BorderLayout.SOUTH)
            minimumSize = Dimension(120, 0)
            preferredSize = Dimension(170, 200)
        }
        detail.border = JBUI.Borders.emptyLeft(12)
        detail.minimumSize = Dimension(0, 0)
        add(left, BorderLayout.WEST)
        add(detail, BorderLayout.CENTER)
        list.addListSelectionListener { if (!loading && !it.valueIsAdjusting) { saveCurrent(); showCurrent() } }
        preferredSize = Dimension(680, 440)
    }
    fun addPreset() {
        saveCurrent()
        model.addElement(ActivityPreset())
        list.selectedIndex = model.size() - 1
    }
    private fun saveCurrent() {
        val current = editor ?: return
        if (selected !in 0 until model.size()) return
        val value = current.result()
        val chosen = color.selectedColor ?: Color.WHITE
        model.set(selected, ActivityPreset(value.name, value.properties.map { PresetProperty(it.key, it.value) }.toMutableList(),
            value.description, String.format("#%06X", chosen.rgb and 0xffffff),
            String.format("#%06X", (foregroundColor.selectedColor ?: Color(145, 145, 145)).rgb and 0xffffff)))
    }
    private fun showCurrent() {
        selected = list.selectedIndex
        detail.removeAll()
        editor = if (selected in 0 until model.size()) {
            val preset = model.getElementAt(selected)
            color.selectedColor = runCatching { Color.decode(preset.color) }.getOrDefault(Color.WHITE)
            foregroundColor.selectedColor = runCatching { Color.decode(preset.foreground) }.getOrDefault(Color(145, 145, 145))
            val current = ActivityPropertiesPanel(ActivityPropertiesData(preset.name,
                preset.properties.map { ActivityProperty(it.key, it.value) }, preset.description), project, showOpenButtons = false, compact = true)
            detail.add(JPanel(GridLayout(1, 2, 12, 0)).apply {
                add(JPanel(BorderLayout(8, 0)).apply { add(JLabel("Background"), BorderLayout.WEST); add(color) })
                add(JPanel(BorderLayout(8, 0)).apply { add(JLabel("Foreground"), BorderLayout.WEST); add(foregroundColor) })
            }, BorderLayout.NORTH)
            detail.add(current)
            current
        } else null
        detail.revalidate(); detail.repaint()
    }
    fun value(): ModelEditorState {
        saveCurrent()
        val directory = serviceDirectory.text.trim()
        return ModelEditorState(ServiceFiles.relativeDirectory(project, directory) ?: directory,
            (0 until model.size()).map { model.getElementAt(it) }.toMutableList(), noUseGlueUmlAdFile.isSelected).detached()
    }
    fun load(state: ModelEditorState) {
        loading = true
        selected = -1; editor = null
        serviceDirectory.text = ServiceFiles.relativeDirectory(project, state.serviceDirectory) ?: state.serviceDirectory
        noUseGlueUmlAdFile.isSelected = state.noUseGlueUmlAdFile
        model.clear()
        state.detached().presets.forEach(model::addElement)
        list.selectedIndex = if (model.isEmpty) -1 else 0
        loading = false
        showCurrent()
    }
}
