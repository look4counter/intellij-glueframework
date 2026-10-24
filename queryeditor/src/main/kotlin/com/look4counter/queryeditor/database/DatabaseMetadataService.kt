package com.look4counter.queryeditor.database

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.look4counter.queryeditor.settings.DataSourceDefinition
import com.look4counter.queryeditor.settings.DataSourcePasswordManager
import com.look4counter.queryeditor.settings.DataSourceSettingsService
import com.look4counter.queryeditor.settings.detached
import java.util.concurrent.CopyOnWriteArrayList

@Service(Service.Level.PROJECT)
class DatabaseMetadataService(private val project: Project) : Disposable {
    private val settings get() = DataSourceSettingsService.getInstance(project)
    private data class Cached(val source: DataSourceDefinition, val snapshot: MetadataSnapshot)
    data class Request(val source: DataSourceDefinition, val generation: Long)
    private val cache = mutableMapOf<String, Cached>()
    private var generation = 0L
    private val listeners = CopyOnWriteArrayList<() -> Unit>()
    private var running: ProgressIndicator? = null
    @Volatile private var disposed = false
    @Volatile private var started = false
    @Volatile var loading = false
        private set
    @Volatile var failureCount = 0
        private set

    init {
        settings.modificationListener = {
            synchronized(this) {
                generation++
                running?.cancel()
                cache.clear()
                loading = false
                failureCount = 0
            }
            changed()
            if (started) refreshAll()
        }
    }

    fun addListener(parent: Disposable, listener: () -> Unit) {
        listeners.add(listener)
        Disposer.register(parent) { listeners.remove(listener) }
    }

    private fun changed() { listeners.forEach { it() } }

    @Synchronized fun beginRefresh(): List<Request> {
        generation++
        return settings.dataSources.map { Request(it.detached(), generation) }
    }

    fun complete(request: Request, snapshot: MetadataSnapshot): Boolean {
        synchronized(this) {
            if (disposed || request.generation != generation || settings.dataSources.none { it == request.source }) return false
            cache[request.source.id] = Cached(request.source.detached(), snapshot)
        }
        changed()
        return true
    }

    @Synchronized fun snapshot(): MetadataSnapshot? {
        val tables = settings.dataSources.flatMap { source ->
            cache[source.id]?.takeIf { it.source == source }?.snapshot?.tables.orEmpty()
        }
        return if (cache.isEmpty()) null else MetadataSnapshot(tables.distinct())
    }

    fun refreshAll() {
        // Schedule all lifecycle transitions on the UI thread; JDBC runs in the background task.
        if (!ApplicationManager.getApplication().isDispatchThread) {
            ApplicationManager.getApplication().invokeLater { if (!disposed && !project.isDisposed) refreshAll() }
            return
        }
        if (disposed || project.isDisposed) return
        started = true
        val requests: List<Request>
        val token: Long
        synchronized(this) {
            running?.cancel()
            requests = beginRefresh()
            token = generation
            loading = requests.isNotEmpty()
            failureCount = 0
        }
        changed()
        if (requests.isEmpty()) return
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "전체 DB 메타데이터 조회", true) {
            override fun run(indicator: ProgressIndicator) {
                synchronized(this@DatabaseMetadataService) {
                    if (disposed || token != generation) indicator.cancel() else running = indicator
                }
                indicator.isIndeterminate = true
                for (request in requests) {
                    indicator.checkCanceled()
                    try {
                        val secret = if (request.source.passwordStored)
                            DataSourcePasswordManager(project.locationHash).readPassword(request.source.id) else null
                        val result = JdbcMetadataReader().read(request.source, secret) {
                            indicator.checkCanceled()
                            if (disposed || project.isDisposed) throw ProcessCanceledException()
                        }
                        complete(request, result)
                    } catch (cancelled: ProcessCanceledException) { throw cancelled
                    } catch (_: Exception) { failed()
                    } catch (_: LinkageError) { failed() }
                }
            }
            private fun failed() {
                // Driver errors can contain credentials. Only publish an anonymous failure count.
                synchronized(this@DatabaseMetadataService) { if (token == generation) failureCount++ }
                changed()
            }
            private fun finish() {
                synchronized(this@DatabaseMetadataService) {
                    if (disposed || token != generation) return
                    running = null
                    loading = false
                }
                changed()
            }
            override fun onSuccess() = finish()
            override fun onCancel() = finish()
            override fun onThrowable(error: Throwable) { failed(); finish() }
        })
    }

    override fun dispose() {
        synchronized(this) {
            disposed = true
            generation++
            running?.cancel()
            cache.clear()
        }
        settings.modificationListener = null
        listeners.clear()
    }

    companion object {
        fun getInstance(project: Project): DatabaseMetadataService = project.getService(DatabaseMetadataService::class.java)
    }
}
