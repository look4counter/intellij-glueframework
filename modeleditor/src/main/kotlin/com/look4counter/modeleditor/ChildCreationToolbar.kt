package com.look4counter.modeleditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.SplitButtonAction
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.project.DumbAware
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

class ChildCreationToolbar(
    target: JComponent,
    initialKind: DiagramNodeKind,
    private val canEdit: () -> Boolean,
    private val isActive: () -> Boolean,
    private val onToggle: (Boolean, DiagramNodeKind) -> Unit,
    private val onKindChanged: (DiagramNodeKind) -> Unit,
    private val magnetState: () -> Boolean = { false },
    private val onMagnetChanged: (Boolean) -> Unit = {},
    private val onDiagramProperties: () -> Unit = {},
    private val canEditProperties: () -> Boolean = canEdit,
    private val presets: () -> List<ActivityPreset> = { emptyList() },
    private val onPresetChanged: (ActivityPreset?) -> Unit = {},
    private val wireCreationState: () -> Boolean = { false },
    private val onWireCreationChanged: (Boolean) -> Unit = {},
    private val allowedKinds: Set<DiagramNodeKind> = DiagramNodeKind.entries.toSet(),
    private val showCommit: Boolean = false,
    private val canCommit: () -> Boolean = { false },
    private val onCommit: () -> Unit = {},
    private val canRollback: () -> Boolean = { false },
    private val onRollback: () -> Unit = {},
) : JPanel(BorderLayout()) {
    var onCreationTrace: ((String) -> Unit)? = null
    var selectedPreset: ActivityPreset? = null
        private set
    var selectedKind = initialKind
        set(value) {
            field = value
            selectedPreset = null
            onPresetChanged(null)
            onKindChanged(value)
            refresh()
        }

    val toggleAction: ToggleAction = object : ToggleAction(), DumbAware {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isSelected(event: AnActionEvent) = isActive()
        override fun setSelected(event: AnActionEvent, selected: Boolean) {
            onCreationTrace?.invoke("TOOLBAR_TOGGLE selected=$selected kind=$selectedKind editable=${canEdit()}")
            if (!canEdit()) return
            onToggle(selected, selectedKind)
            refresh()
        }
        override fun update(event: AnActionEvent) {
            super.update(event)
            event.presentation.text = selectedPreset?.name ?: label(selectedKind)
            event.presentation.description = "${label(selectedKind)} 추가 모드 켜기/끄기 · 화살표: 종류 선택"
            event.presentation.icon = null
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
            event.presentation.isEnabled = canEdit()
        }
    }

    val kindActions: List<ToggleAction> = allowedKinds.map { kind ->
        object : ToggleAction(label(kind), "추가할 Child 종류 선택", icon(kind)), DumbAware {
            override fun getActionUpdateThread() = ActionUpdateThread.EDT
            override fun isSelected(event: AnActionEvent) = selectedKind == kind && selectedPreset == null
            override fun setSelected(event: AnActionEvent, selected: Boolean) {
                onCreationTrace?.invoke("TOOLBAR_SELECT kind=$kind editable=${canEdit()}")
                if (canEdit()) {
                    selectedKind = kind
                    onToggle(true, kind)
                    refresh()
                }
            }
            override fun update(event: AnActionEvent) {
                super.update(event)
                event.presentation.keepPopupOnPerform = com.intellij.openapi.actionSystem.KeepPopupOnPerform.Never
                event.presentation.isEnabled = canEdit()
            }
        }
    }

    private val kindGroup = object : DefaultActionGroup(), DumbAware {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun update(event: AnActionEvent) { event.presentation.isEnabled = canEdit() }
        override fun getChildren(event: AnActionEvent?): Array<AnAction> {
            val registered = presets()
            val choices = registered.map { preset ->
                object : ToggleAction(preset.name, "프리셋 Activity 선택", AllIcons.Nodes.Class), DumbAware {
                    override fun getActionUpdateThread() = ActionUpdateThread.EDT
                    override fun isSelected(event: AnActionEvent) = selectedPreset == preset
                    override fun setSelected(event: AnActionEvent, selected: Boolean) { if (canEdit()) selectPreset(preset) }
                    override fun update(event: AnActionEvent) {
                        super.update(event)
                        event.presentation.keepPopupOnPerform = com.intellij.openapi.actionSystem.KeepPopupOnPerform.Never
                        event.presentation.isEnabled = canEdit()
                    }
                }
            }
            return (kindActions + if (choices.isEmpty()) emptyList() else listOf(com.intellij.openapi.actionSystem.Separator.create("Activity 프리셋")) + choices).toTypedArray()
        }
    }

    fun selectPreset(preset: ActivityPreset) {
        onCreationTrace?.invoke("TOOLBAR_PRESET editable=${canEdit()}")
        selectedKind = DiagramNodeKind.Activity
        selectedPreset = preset.detached()
        onPresetChanged(selectedPreset)
        onToggle(true, selectedKind)
        refresh()
    }

    fun refreshPresets() {
        selectedPreset?.let { old ->
            selectedPreset = presets().firstOrNull { it.name == old.name }?.detached()
            onPresetChanged(selectedPreset)
        }
        refresh()
    }

    val splitAction: SplitButtonAction = object : SplitButtonAction(kindGroup) {
        override fun getMainAction(event: AnActionEvent): AnAction = toggleAction
        override fun useDynamicSplitButton() = false
        override fun isGroupPresentationDependsOnSelectedAction() = true
        override fun createCustomComponent(presentation: com.intellij.openapi.actionSystem.Presentation, place: String): JComponent {
            val menu = object : com.intellij.openapi.project.DumbAwareAction("종류 선택", "추가할 Child 선택", AllIcons.General.ArrowDown) {
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
                override fun update(event: AnActionEvent) { event.presentation.isEnabled = canEdit() }
                override fun actionPerformed(event: AnActionEvent) {
                    val popup = com.intellij.openapi.ui.popup.JBPopupFactory.getInstance().createActionGroupPopup(
                        null, kindGroup, event.dataContext,
                        com.intellij.openapi.ui.popup.JBPopupFactory.ActionSelectionAid.SPEEDSEARCH, false)
                    popup.addListener(object : com.intellij.openapi.ui.popup.JBPopupListener {
                        override fun beforeShown(event: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                            onCreationTrace?.invoke("POPUP_SHOW")
                        }
                        override fun onClosed(event: com.intellij.openapi.ui.popup.LightweightWindowEvent) {
                            onCreationTrace?.invoke("POPUP_CLOSED accepted=${event.isOk}")
                        }
                    })
                    popup.showUnderneathOf(event.inputEvent?.component as? JComponent ?: this@ChildCreationToolbar)
                }
            }
            return ActionManager.getInstance().createActionToolbar("GlueModelEditor.ChildChoice",
                DefaultActionGroup(toggleAction, menu), true).apply {
                targetComponent = target
                setReservePlaceAutoPopupIcon(false)
            }.component
        }
    }

    val magnetAction: ToggleAction = object : ToggleAction("Magnetic", "Activity 중심과 30px 간격에 Wire 접점 맞추기", null), DumbAware {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isSelected(event: AnActionEvent) = magnetState()
        override fun setSelected(event: AnActionEvent, selected: Boolean) { onMagnetChanged(selected); refresh() }
        override fun update(event: AnActionEvent) {
            super.update(event)
            event.presentation.text = "Magnetic"
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
        }
    }
    val diagramPropertiesAction: AnAction = object : com.intellij.openapi.project.DumbAwareAction(
        "Properties", "GlueDiagram Properties와 Description 편집", null) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun update(event: AnActionEvent) {
            event.presentation.text = "Properties"
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
            event.presentation.isEnabled = canEditProperties()
        }
        override fun actionPerformed(event: AnActionEvent) { if (canEditProperties()) onDiagramProperties() }
    }
    val wireCreationAction: ToggleAction = object : ToggleAction("Wire", "시작 Child와 끝 Child를 클릭해 Wire 추가", null), DumbAware {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isSelected(event: AnActionEvent) = wireCreationState()
        override fun setSelected(event: AnActionEvent, selected: Boolean) { if (canEdit()) onWireCreationChanged(selected); refresh() }
        override fun update(event: AnActionEvent) {
            super.update(event)
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
            event.presentation.isEnabled = canEdit()
        }
    }
    val commitAction: AnAction = object : com.intellij.openapi.project.DumbAwareAction("Commit", "변경사항을 서비스 XML에 반영", null) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun update(event: AnActionEvent) {
            super.update(event)
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
            event.presentation.isEnabled = canCommit()
        }
        override fun actionPerformed(event: AnActionEvent) { if (canCommit()) onCommit() }
    }
    val rollbackAction: AnAction = object : com.intellij.openapi.project.DumbAwareAction("Rollback", "Commit하지 않은 변경사항 폐기", null) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun update(event: AnActionEvent) {
            super.update(event)
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
            event.presentation.isEnabled = canRollback()
        }
        override fun actionPerformed(event: AnActionEvent) { if (canRollback()) onRollback() }
    }
    private val actions = DefaultActionGroup().apply {
        add(splitAction)
        add(wireCreationAction)
        add(magnetAction)
        add(diagramPropertiesAction)
        if (showCommit) { add(commitAction); add(rollbackAction) }
    }
    val toolbar: ActionToolbar = ActionManager.getInstance().createActionToolbar(
        "GlueModelEditor.Creation", actions, true).apply {
        targetComponent = target
        setReservePlaceAutoPopupIcon(false)
    }

    init {
        name = "model.child.creationToolbar"
        add(toolbar.component, BorderLayout.CENTER)
        refresh()
    }

    fun refresh() {
        toolbar.updateActionsImmediately()
    }

    private fun label(kind: DiagramNodeKind) = when (kind) {
        DiagramNodeKind.Activity -> "빈 Activity"
        DiagramNodeKind.InitialState -> "InitialState"
        DiagramNodeKind.FinalState -> "FinalState"
    }
    private fun icon(kind: DiagramNodeKind) = when (kind) {
        DiagramNodeKind.Activity -> AllIcons.Nodes.Class
        DiagramNodeKind.InitialState -> AllIcons.Actions.Execute
        DiagramNodeKind.FinalState -> AllIcons.RunConfigurations.TestTerminated
    }
}
