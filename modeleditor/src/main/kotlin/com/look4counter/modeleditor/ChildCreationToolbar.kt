package com.look4counter.modeleditor

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionToolbar
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.openapi.actionSystem.CustomShortcutSet
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.IconLoader
import java.awt.BorderLayout
import java.awt.event.KeyEvent
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.KeyStroke

class ChildCreationToolbar(
    target: JComponent,
    initialKind: DiagramNodeKind,
    private val canEdit: () -> Boolean,
    private val isActive: () -> Boolean,
    private val onToggle: (Boolean, DiagramNodeKind) -> Unit,
    private val onKindChanged: (DiagramNodeKind) -> Unit,
    private val snapState: () -> Boolean = { false },
    private val onSnapChanged: (Boolean) -> Unit = {},
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
    private val orthogonalState: () -> Boolean = { false },
    private val onOrthogonalChanged: (Boolean) -> Unit = {},
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

    private val activityAddIcon = IconLoader.getIcon("/icons/activity-add.svg", ChildCreationToolbar::class.java)

    val toggleAction: ToggleAction = object : ToggleAction("Activity", "Activity 추가 (a)", activityAddIcon), DumbAware {
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
            event.presentation.description = "Activity 추가 (a)"
            event.presentation.icon = activityAddIcon
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
            event.presentation.isEnabled = canEdit()
        }
    }

    val kindActions: List<ToggleAction> = allowedKinds.map { kind ->
        object : ToggleAction(label(kind), "Activity 선택", icon(kind)), DumbAware {
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
                object : ToggleAction(preset.name, "Activity 선택", AllIcons.Nodes.Class), DumbAware {
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

    class CreationControlAction(
        val actionGroup: ActionGroup,
        private val mainAction: ToggleAction,
        private val componentFactory: () -> JComponent,
    ) : DumbAwareAction(), CustomComponentAction {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun update(event: AnActionEvent) = mainAction.update(event)
        override fun actionPerformed(event: AnActionEvent) = mainAction.actionPerformed(event)
        override fun createCustomComponent(presentation: Presentation, place: String) = componentFactory()
    }

    val splitAction = CreationControlAction(kindGroup, toggleAction) {
        val menu = object : com.intellij.openapi.project.DumbAwareAction("Activity 선택", "Activity 선택", AllIcons.General.ArrowDown) {
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
        ActionManager.getInstance().createActionToolbar("GlueModelEditor.ChildChoice",
            DefaultActionGroup(toggleAction, menu), true).apply {
            targetComponent = target
            setReservePlaceAutoPopupIcon(false)
        }.component
    }

    val snapAction: ToggleAction = object : ToggleAction("Snap", "정렬 토글 (s)",
        IconLoader.getIcon("/icons/magnet.svg", ChildCreationToolbar::class.java)), DumbAware {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isSelected(event: AnActionEvent) = snapState()
        override fun setSelected(event: AnActionEvent, selected: Boolean) { onSnapChanged(selected); refresh() }
        override fun update(event: AnActionEvent) {
            super.update(event)
            event.presentation.text = "Snap"
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
        }
    }
    val orthogonalAction: ToggleAction = object : ToggleAction("Orthogonal",
        "직각 Wire 토글 (o)", IconLoader.getIcon("/icons/orthogonal.svg", ChildCreationToolbar::class.java)), DumbAware {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun isSelected(event: AnActionEvent) = orthogonalState()
        override fun setSelected(event: AnActionEvent, selected: Boolean) { onOrthogonalChanged(selected); refresh() }
        override fun update(event: AnActionEvent) {
            super.update(event)
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
        }
    }
    val diagramPropertiesAction: AnAction = object : com.intellij.openapi.project.DumbAwareAction(
        "Properties", "파일 속성 (p)", AllIcons.Actions.Annotate) {
        override fun getActionUpdateThread() = ActionUpdateThread.EDT
        override fun update(event: AnActionEvent) {
            event.presentation.text = "Properties"
            event.presentation.putClientProperty(com.intellij.openapi.actionSystem.ex.ActionUtil.SHOW_TEXT_IN_TOOLBAR, true)
            event.presentation.isEnabled = canEditProperties()
        }
        override fun actionPerformed(event: AnActionEvent) { if (canEditProperties()) onDiagramProperties() }
    }
    val wireCreationAction: ToggleAction = object : ToggleAction("Wire", "Wire 추가 (w)",
        IconLoader.getIcon("/icons/wire-add.svg", ChildCreationToolbar::class.java)), DumbAware {
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
        add(snapAction)
        add(orthogonalAction)
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
        fun shortcut(key: Int, button: AnAction, perform: (AnActionEvent) -> Unit = button::actionPerformed) {
            object : DumbAwareAction(button.templatePresentation.text) {
                override fun getActionUpdateThread() = ActionUpdateThread.EDT
                override fun update(event: AnActionEvent) = button.update(event)
                override fun actionPerformed(event: AnActionEvent) {
                    button.update(event)
                    if (event.presentation.isEnabled) perform(event)
                }
            }.registerCustomShortcutSet(CustomShortcutSet(KeyStroke.getKeyStroke(key, 0)), target)
        }
        shortcut(KeyEvent.VK_A, toggleAction) { event ->
            val active = isActive() && selectedKind == DiagramNodeKind.Activity
            if (selectedKind != DiagramNodeKind.Activity) selectedKind = DiagramNodeKind.Activity
            toggleAction.setSelected(event, !active)
        }
        shortcut(KeyEvent.VK_W, wireCreationAction)
        shortcut(KeyEvent.VK_S, snapAction)
        shortcut(KeyEvent.VK_O, orthogonalAction)
        shortcut(KeyEvent.VK_P, diagramPropertiesAction)
        refresh()
    }

    fun refresh() {
        toolbar.updateActionsImmediately()
    }

    private fun label(kind: DiagramNodeKind) = when (kind) {
        DiagramNodeKind.Activity -> "Empty Activity"
        DiagramNodeKind.InitialState -> "InitialState"
        DiagramNodeKind.FinalState -> "FinalState"
    }
    private fun icon(kind: DiagramNodeKind) = when (kind) {
        DiagramNodeKind.Activity -> AllIcons.Nodes.Class
        DiagramNodeKind.InitialState -> AllIcons.Actions.Execute
        DiagramNodeKind.FinalState -> AllIcons.RunConfigurations.TestTerminated
    }
}
