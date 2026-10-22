package com.look4counter.queryeditor.database

import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.look4counter.queryeditor.settings.DataSourceDefinition
import com.look4counter.queryeditor.settings.DataSourceSettingsService
import com.look4counter.queryeditor.settings.detached
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class DatabaseMetadataService(private val project: Project) {
    private val settings get() = DataSourceSettingsService.getInstance(project)
    private data class Cached(val source: DataSourceDefinition, val snapshot: MetadataSnapshot)
    data class Request(val source: DataSourceDefinition, val generation: Long)
    private val cache = mutableMapOf<String, Cached>()
    private var generation = 0L
    private val listeners = CopyOnWriteArrayList<() -> Unit>()

    init { settings.modificationListener = { invalidate(); listeners.forEach { it() } } }

    var selectedId: String
        get() = settings.selectedDataSourceId
        set(value) {
            synchronized(this) { generation++ }
            settings.selectedDataSourceId = value.takeIf { id -> settings.dataSources.any { it.id == id } }.orEmpty()
            listeners.forEach { it() }
        }

    fun addListener(parent: Disposable, listener: () -> Unit) {
        listeners.add(listener)
        Disposer.register(parent) { listeners.remove(listener) }
    }

    @Synchronized private fun invalidate() {
        generation++
        // Password changes are deliberately absent from the metadata definition.
        cache.clear()
    }

    @Synchronized fun beginRefresh(): Request? {
        val source = settings.dataSources.singleOrNull { it.id == selectedId } ?: return null
        // Only the latest explicit refresh may replace a snapshot.
        generation++
        return Request(source.detached(), generation)
    }

    @Synchronized fun complete(request: Request, snapshot: MetadataSnapshot): Boolean {
        if (request.generation != generation || settings.dataSources.none { it == request.source }) return false
        cache[request.source.id] = Cached(request.source.detached(), snapshot)
        listeners.forEach { it() }
        return true
    }

    @Synchronized fun selectedSnapshot(): MetadataSnapshot? {
        val source = settings.dataSources.singleOrNull { it.id == selectedId } ?: return null
        return cache[source.id]?.takeIf { it.source == source }?.snapshot
    }

    companion object {
        fun getInstance(project: Project): DatabaseMetadataService = project.getService(DatabaseMetadataService::class.java)
    }
}
