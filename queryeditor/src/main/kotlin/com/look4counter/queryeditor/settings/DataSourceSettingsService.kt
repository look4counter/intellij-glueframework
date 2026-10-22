package com.look4counter.queryeditor.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

data class DataSourceDefinition(
    var id: String = "",
    var name: String = "",
    var jdbcUrl: String = "",
    var driverClass: String = "",
    var username: String = "",
    var passwordStored: Boolean = false,
    var databaseKind: String = "",
    var driverJars: MutableList<String> = mutableListOf(),
)

fun DataSourceDefinition.detached(): DataSourceDefinition = copy(driverJars = driverJars.toMutableList())

data class DataSourcesState(
    var dataSources: MutableList<DataSourceDefinition> = mutableListOf(),
    var selectedDataSourceId: String = "",
)

@Service(Service.Level.PROJECT)
@State(name = "com.look4counter.GlueQueryEditorDataSources", storages = [Storage("glue-query-editor.xml")])
class DataSourceSettingsService : PersistentStateComponent<DataSourcesState> {
    @Volatile private var saved = DataSourcesState()

    val dataSources: List<DataSourceDefinition> get() = saved.dataSources.map { it.detached() }
    var selectedDataSourceId: String
        get() = saved.selectedDataSourceId
        set(value) { saved = DataSourcesState(dataSources.toMutableList(), value) }
    override fun getState(): DataSourcesState = DataSourcesState(dataSources.toMutableList(), selectedDataSourceId)
    override fun loadState(state: DataSourcesState) {
        saved = DataSourcesState(state.dataSources.map { it.detached() }.toMutableList(), state.selectedDataSourceId)
        modificationListener?.invoke()
    }
    fun replaceDataSources(sources: List<DataSourceDefinition>) {
        saved = DataSourcesState(sources.map { it.detached() }.toMutableList(),
            selectedDataSourceId.takeIf { id -> sources.any { it.id == id } }.orEmpty())
        modificationListener?.invoke()
    }

    internal var modificationListener: (() -> Unit)? = null

    companion object {
        fun getInstance(project: Project): DataSourceSettingsService = project.getService(DataSourceSettingsService::class.java)
    }
}
