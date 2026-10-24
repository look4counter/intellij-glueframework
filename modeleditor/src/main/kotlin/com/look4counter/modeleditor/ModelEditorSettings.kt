package com.look4counter.modeleditor

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.messages.Topic
import java.awt.Color

data class PresetProperty(var key: String = "", var value: String = "")
data class ActivityPreset(var name: String = "Activity", var properties: MutableList<PresetProperty> = mutableListOf(),
                          var description: String = "", var color: String = "#FFFFFF", var foreground: String = "#919191")
data class ModelEditorState(var serviceDirectory: String = "", var presets: MutableList<ActivityPreset> = mutableListOf(),
                          var noUseGlueUmlAdFile: Boolean = false)
fun ActivityPreset.detached() = copy(properties = properties.map { it.copy() }.toMutableList())

interface ModelEditorSettingsListener {
    fun settingsChanged()
    companion object { val TOPIC = Topic.create("GlueModelEditor 설정 변경", ModelEditorSettingsListener::class.java) }
}

fun ModelEditorState.detached() = copy(presets = presets.map { preset ->
    preset.copy(properties = preset.properties.map { it.copy() }.toMutableList())
}.toMutableList())

@Service(Service.Level.PROJECT)
@State(name = "com.look4counter.GlueModelEditor", storages = [Storage("glue-model-editor.xml")])
class ModelEditorSettings(private val project: Project) : PersistentStateComponent<ModelEditorState> {
    @Volatile private var saved = ModelEditorState()
    @Volatile private var classColors = emptyMap<String, Color>()
    @Volatile private var classForegrounds = emptyMap<String, Color>()
    override fun getState(): ModelEditorState = saved.detached()
    fun colorForClass(name: String): Color? = classColors[name.trim()]
    fun foregroundForClass(name: String): Color? = classForegrounds[name.trim()]
    override fun loadState(state: ModelEditorState) {
        saved = state.detached()
        val colors = linkedMapOf<String, Color>()
        val foregrounds = linkedMapOf<String, Color>()
        saved.presets.forEach { preset ->
            val name = preset.properties.firstOrNull { it.key == "class" }?.value?.trim().orEmpty()
            if (name.isNotEmpty() && name !in colors) {
                colors[name] = runCatching { Color.decode(preset.color) }.getOrDefault(Color.WHITE)
                foregrounds[name] = runCatching { Color.decode(preset.foreground) }.getOrDefault(Color(145, 145, 145))
            }
        }
        classColors = colors
        classForegrounds = foregrounds
        if (!project.isDisposed) project.messageBus.syncPublisher(ModelEditorSettingsListener.TOPIC).settingsChanged()
    }
    companion object {
        fun getInstance(project: Project): ModelEditorSettings = project.getService(ModelEditorSettings::class.java)
    }
}
