package com.look4counter.modeleditor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.util.messages.Topic

data class ModelSnapState(var enabled: Boolean = false)

interface ModelSnapSettingsListener {
    fun snapChanged(enabled: Boolean)

    companion object {
        val TOPIC = Topic.create("Glue model Snap state", ModelSnapSettingsListener::class.java)
    }
}

@Service(Service.Level.APP)
@State(name = "com.look4counter.GlueModelSnap", storages = [Storage("glue-model-snap.xml")])
class ModelSnapSettings : PersistentStateComponent<ModelSnapState> {
    @Volatile private var current = false

    var enabled: Boolean
        get() = current
        set(value) {
            if (current == value) return
            current = value
            ApplicationManager.getApplication().messageBus.syncPublisher(ModelSnapSettingsListener.TOPIC)
                .snapChanged(value)
        }

    override fun getState() = ModelSnapState(enabled)

    override fun loadState(state: ModelSnapState) {
        enabled = state.enabled
    }

    companion object {
        fun getInstance(): ModelSnapSettings = ApplicationManager.getApplication().getService(ModelSnapSettings::class.java)
    }
}
