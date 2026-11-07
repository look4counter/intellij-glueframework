package com.look4counter

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.actionSystem.Toggleable
import com.intellij.openapi.actionSystem.UpdateSession
import com.intellij.openapi.actionSystem.ex.CustomComponentAction
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.modeleditor.ActivityPreset
import com.look4counter.modeleditor.ChildCreationToolbar
import com.look4counter.modeleditor.DiagramNodeKind
import javax.swing.JPanel

class ChildCreationToolbarTest : BasePlatformTestCase() {
    private class State(var editable: Boolean = true, var active: Boolean = false)

    private fun toolbar(state: State) = ChildCreationToolbar(JPanel(), DiagramNodeKind.Activity,
        canEdit = { state.editable }, isActive = { state.active },
        onToggle = { active, _ -> state.active = active }, onKindChanged = {})

    private fun event(action: AnAction): AnActionEvent = AnActionEvent.createFromAnAction(
        action, null, ActionPlaces.TOOLBAR, DataContext.EMPTY_CONTEXT).apply {
        updateSession = object : UpdateSession {
            override fun presentation(action: AnAction): Presentation {
                val child = AnActionEvent.createFromAnAction(action, null, ActionPlaces.TOOLBAR, DataContext.EMPTY_CONTEXT)
                action.update(child)
                return child.presentation
            }
        }
    }

    fun testRegisteredComponentsSurviveRepeatedUpdatesInTwoToolbars() {
        val first = toolbar(State())
        val second = toolbar(State())
        val firstEvent = event(first.splitAction)
        val secondEvent = event(second.splitAction)
        val firstComponent = (first.splitAction as CustomComponentAction).createCustomComponent(firstEvent.presentation, ActionPlaces.TOOLBAR)
        val secondComponent = (second.splitAction as CustomComponentAction).createCustomComponent(secondEvent.presentation, ActionPlaces.TOOLBAR)
        firstEvent.presentation.putClientProperty(CustomComponentAction.COMPONENT_KEY, firstComponent)
        secondEvent.presentation.putClientProperty(CustomComponentAction.COMPONENT_KEY, secondComponent)
        first.selectedKind = DiagramNodeKind.FinalState
        second.selectedKind = DiagramNodeKind.InitialState
        repeat(10) {
            first.splitAction.update(firstEvent)
            second.splitAction.update(secondEvent)
            assertSame(firstComponent, firstEvent.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY))
            assertSame(secondComponent, secondEvent.presentation.getClientProperty(CustomComponentAction.COMPONENT_KEY))
            assertEquals("FinalState", firstEvent.presentation.text)
            assertEquals("InitialState", secondEvent.presentation.text)
            assertEquals("Activity 추가", firstEvent.presentation.description)
            assertEquals("Activity 추가", secondEvent.presentation.description)
        }
        assertNotSame(firstComponent, secondComponent)
    }

    fun testCustomControlDelegatesToggleAndUpdatesPresetAndReadonlyState() {
        val state = State()
        val toolbar = toolbar(state)
        val event = event(toolbar.splitAction)
        assertEquals(ActionUpdateThread.EDT, toolbar.splitAction.actionUpdateThread)
        toolbar.splitAction.update(event)
        assertEquals("Empty Activity", event.presentation.text)
        assertEquals("Activity 추가", event.presentation.description)
        assertEquals("Wire 추가", toolbar.wireCreationAction.templatePresentation.description)
        assertEquals("정렬 토글", toolbar.snapAction.templatePresentation.description)
        assertEquals("직각 Wire 토글", toolbar.orthogonalAction.templatePresentation.description)
        assertEquals("파일 속성", toolbar.diagramPropertiesAction.templatePresentation.description)
        assertTrue(toolbar.kindActions.all { it.templatePresentation.description == "Activity 선택" })
        assertFalse(Toggleable.isSelected(event.presentation))
        toolbar.splitAction.actionPerformed(event)
        toolbar.splitAction.update(event)
        assertTrue(state.active)
        assertTrue(Toggleable.isSelected(event.presentation))
        toolbar.selectPreset(ActivityPreset("Preset A"))
        toolbar.splitAction.update(event)
        assertEquals("Preset A", event.presentation.text)
        assertEquals("Activity 추가", event.presentation.description)
        assertNull(event.presentation.icon)
        state.editable = false
        toolbar.splitAction.update(event)
        assertFalse(event.presentation.isEnabled)
        toolbar.splitAction.actionPerformed(event)
        assertTrue(state.active)
        state.editable = true
        toolbar.splitAction.update(event)
        assertTrue(event.presentation.isEnabled)
        toolbar.splitAction.actionPerformed(event)
        toolbar.splitAction.update(event)
        assertFalse(state.active)
        assertFalse(Toggleable.isSelected(event.presentation))
    }
}
