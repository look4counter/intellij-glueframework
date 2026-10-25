package com.look4counter

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.xmlb.XmlSerializer
import com.look4counter.modeleditor.*
import java.awt.Component
import java.awt.Container
import java.awt.event.MouseEvent

class ModelSnapSettingsTest : BasePlatformTestCase() {
    private var previous = false
    private val editors = mutableListOf<FileEditor>()

    override fun setUp() {
        super.setUp()
        previous = ModelSnapSettings.getInstance().enabled
        ModelSnapSettings.getInstance().enabled = false
    }

    override fun tearDown() {
        try {
            editors.asReversed().forEach(Disposer::dispose)
            editors.clear()
            ModelSnapSettings.getInstance().enabled = previous
        } finally { super.tearDown() }
    }

    private fun descendants(component: Component): Sequence<Component> = sequence {
        yield(component)
        if (component is Container) component.components.forEach { yieldAll(descendants(it)) }
    }

    private fun file(name: String): VirtualFile = myFixture.addFileToProject(name, """
        <GlueDiagram xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance">
          <child xsi:type="Activity" id="1" name="A" location="20,40" size="100,-1"/>
          <child xsi:type="Activity" id="2" name="B" location="220,140" size="100,-1"/>
        </GlueDiagram>
    """.trimIndent()).virtualFile

    private fun open(file: VirtualFile): FileEditor = ModelDesignEditorProvider().createEditor(project, file)
        .also { editors.add(it) }

    private fun canvas(editor: FileEditor) = descendants(editor.component).filterIsInstance<ModelDiagramCanvas>().single()
    private fun toolbar(editor: FileEditor) = descendants(editor.component).filterIsInstance<ChildCreationToolbar>().single()
    private fun event(editor: FileEditor) = AnActionEvent.createFromAnAction(toolbar(editor).snapAction, null,
        ActionPlaces.UNKNOWN, DataContext.EMPTY_CONTEXT)
    private fun toggle(editor: FileEditor, enabled: Boolean) = toolbar(editor).snapAction.setSelected(event(editor), enabled)

    fun testAllOpenNewAndReopenedEditorsShareSnapInBothDirections() {
        val firstFile = file("snap-first.glue_uml_ad")
        val first = open(firstFile)
        val second = open(file("snap-second.glue_uml_ad"))
        assertFalse(canvas(first).snapEnabled)
        assertFalse(canvas(second).snapEnabled)
        toggle(first, true)
        assertTrue(ModelSnapSettings.getInstance().enabled)
        assertTrue(canvas(first).snapEnabled)
        assertTrue(canvas(second).snapEnabled)
        assertTrue(toolbar(second).snapAction.isSelected(event(second)))
        val third = open(file("snap-third.glue_uml_ad"))
        assertTrue(canvas(third).snapEnabled)
        Disposer.dispose(first)
        editors.remove(first)
        val reopened = open(firstFile)
        assertTrue(canvas(reopened).snapEnabled)
        toggle(second, false)
        for (editor in listOf(second, third, reopened)) {
            assertFalse(canvas(editor).snapEnabled)
            assertFalse(toolbar(editor).snapAction.isSelected(event(editor)))
        }
    }

    fun testStateRestoreUpdatesOpenEditorsAndIsDetachedAndPersistent() {
        val settings = ModelSnapSettings.getInstance()
        assertSame(settings, ApplicationManager.getApplication().getService(ModelSnapSettings::class.java))
        assertFalse(XmlSerializer.deserialize(org.jdom.Element("ModelSnapState"), ModelSnapState::class.java).enabled)
        val first = open(file("restore-first.glue_uml_ad"))
        val second = open(file("restore-second.glue_uml_ad"))
        settings.state.enabled = true
        assertFalse(settings.enabled)
        settings.enabled = true
        val serialized = XmlSerializer.serialize(settings.state)
        settings.enabled = false
        settings.loadState(XmlSerializer.deserialize(serialized, ModelSnapState::class.java))
        assertTrue(settings.enabled)
        assertTrue(canvas(first).snapEnabled)
        assertTrue(canvas(second).snapEnabled)
        assertTrue(toolbar(second).snapAction.isSelected(event(second)))
        assertTrue(canvas(open(file("restored-new.glue_uml_ad"))).snapEnabled)
    }

    fun testChangeNotificationsOnlyForChangesAndStopAfterDisposal() {
        val settings = ModelSnapSettings.getInstance()
        val lifetime = Disposer.newDisposable()
        val changes = mutableListOf<Boolean>()
        try {
            ApplicationManager.getApplication().messageBus.connect(lifetime)
                .subscribe(ModelSnapSettingsListener.TOPIC, object : ModelSnapSettingsListener {
                    override fun snapChanged(enabled: Boolean) { changes.add(enabled) }
                })
            settings.enabled = true
            settings.enabled = true
            settings.loadState(ModelSnapState(false))
            assertEquals(listOf(true, false), changes)
        } finally { Disposer.dispose(lifetime) }
        settings.enabled = true
        assertEquals(listOf(true, false), changes)
    }

    fun testTurningOffInAnotherEditorClearsActiveAlignmentPreview() {
        val first = open(file("drag-first.glue_uml_ad"))
        val second = open(file("drag-second.glue_uml_ad"))
        toggle(second, true)
        val canvas = canvas(first)
        val offset = canvas.screenOffset
        fun mouse(type: Int, x: Int, y: Int) = canvas.dispatchEvent(MouseEvent(canvas, type, 0,
            MouseEvent.BUTTON1_DOWN_MASK, x + offset.x.toInt(), y + offset.y.toInt(), 1, false, MouseEvent.BUTTON1))
        mouse(MouseEvent.MOUSE_PRESSED, 70, 55)
        mouse(MouseEvent.MOUSE_DRAGGED, 167, 153)
        assertFalse(canvas.alignmentGuides.isEmpty())
        toggle(second, false)
        assertFalse(canvas.snapEnabled)
        assertTrue(canvas.alignmentGuides.isEmpty())
        assertEquals(DiagramPoint(117.0, 138.0), canvas.diagram.nodes.first().location)
        canvas.cancelDrag()
    }
}
