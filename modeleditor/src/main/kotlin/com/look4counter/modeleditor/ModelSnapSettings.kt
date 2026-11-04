package com.look4counter.modeleditor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic

data class ModelSnapState(var enabled: Boolean = false, var orthogonalEnabled: Boolean = false)

interface ModelSnapSettingsListener {
    fun snapChanged(enabled: Boolean)
    fun orthogonalChanged(enabled: Boolean) {}

    companion object {
        val TOPIC = Topic.create("Glue model Snap state", ModelSnapSettingsListener::class.java)
    }
}

@Service(Service.Level.APP)
@State(name = "com.look4counter.GlueModelSnap", storages = [Storage("glue-model-snap.xml")])
class ModelSnapSettings : PersistentStateComponent<ModelSnapState> {
    @Volatile private var current = false
    @Volatile private var orthogonal = false

    var orthogonalEnabled: Boolean
        get() = orthogonal
        set(value) {
            if (orthogonal == value) return
            orthogonal = value
            ApplicationManager.getApplication().messageBus.syncPublisher(ModelSnapSettingsListener.TOPIC)
                .orthogonalChanged(value)
        }

    var enabled: Boolean
        get() = current
        set(value) {
            if (current == value) return
            current = value
            ApplicationManager.getApplication().messageBus.syncPublisher(ModelSnapSettingsListener.TOPIC)
                .snapChanged(value)
        }

    override fun getState() = ModelSnapState(enabled, orthogonalEnabled)

    override fun loadState(state: ModelSnapState) {
        enabled = state.enabled
        orthogonalEnabled = state.orthogonalEnabled
    }

    companion object {
        fun getInstance(): ModelSnapSettings = ApplicationManager.getApplication().getService(ModelSnapSettings::class.java)
    }
}
