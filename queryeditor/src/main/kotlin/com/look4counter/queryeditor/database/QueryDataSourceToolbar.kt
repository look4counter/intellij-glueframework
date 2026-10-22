package com.look4counter.queryeditor.database

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.ComboBox
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.look4counter.queryeditor.settings.DataSourcePasswordManager
import com.look4counter.queryeditor.settings.DataSourceSettingsService
import java.awt.FlowLayout
import javax.swing.JButton

internal class QueryDataSourceToolbar(private val project: Project, parent: Disposable) :
    JBPanel<QueryDataSourceToolbar>(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(4))) {
    private data class Choice(val id: String, val label: String) { override fun toString(): String = label }
    private val metadata = DatabaseMetadataService.getInstance(project)
    private val sources = ComboBox<Choice>().apply { name = "query.datasource"; accessibleContext.accessibleName = "자동완성 DataSource" }
    private val refresh = JButton("메타데이터 새로고침").apply { name = "query.metadata.refresh" }
    private val status = JBLabel().apply { name = "query.metadata.status" }
    private var updating = false
    private var busy = false
    @Volatile private var disposed = false
    @Volatile private var running: ProgressIndicator? = null

    init {
        add(JBLabel("DataSource"))
        add(sources)
        add(refresh)
        add(status)
        sources.addActionListener {
            if (!updating) {
                running?.cancel()
                metadata.selectedId = (sources.selectedItem as? Choice)?.id.orEmpty()
                update()
            }
        }
        refresh.addActionListener { refreshMetadata() }
        metadata.addListener(parent) {
            ApplicationManager.getApplication().invokeLater({ if (!disposed && !project.isDisposed) update() }, ModalityState.nonModal())
        }
        Disposer.register(parent) { disposed = true; running?.cancel() }
        update()
    }

    private fun update() {
        updating = true
        sources.removeAllItems()
        sources.addItem(Choice("", "선택 안 함"))
        DataSourceSettingsService.getInstance(project).dataSources.forEach { sources.addItem(Choice(it.id, it.name)) }
        sources.selectedItem = (0 until sources.itemCount).map { sources.getItemAt(it) }.firstOrNull { it.id == metadata.selectedId }
            ?: sources.getItemAt(0)
        updating = false
        refresh.isEnabled = metadata.selectedId.isNotEmpty() && !busy
        val snapshot = metadata.selectedSnapshot()
        status.text = if (snapshot == null) "" else "테이블 ${snapshot.tableNames.size} · 컬럼 ${snapshot.columnNames.size}"
    }

    private fun refreshMetadata() {
        val request = metadata.beginRefresh() ?: return
        busy = true
        refresh.isEnabled = false
        status.text = "메타데이터 조회 중…"
        ProgressManager.getInstance().run(object : Task.Backgroundable(project, "DB 메타데이터 조회", true) {
            private var snapshot: MetadataSnapshot? = null
            private var failed = false
            override fun run(indicator: ProgressIndicator) {
                running = indicator
                indicator.isIndeterminate = true
                if (disposed || metadata.selectedId != request.source.id) indicator.cancel()
                indicator.checkCanceled()
                try {
                    val secret = if (request.source.passwordStored) DataSourcePasswordManager(project.locationHash).readPassword(request.source.id) else null
                    snapshot = JdbcMetadataReader().read(request.source, secret) { indicator.checkCanceled() }
                } catch (cancelled: com.intellij.openapi.progress.ProcessCanceledException) { throw cancelled
                } catch (_: Exception) { failed = true
                } catch (_: LinkageError) { failed = true }
            }
            override fun onSuccess() {
                if (disposed || project.isDisposed) return
                snapshot?.let { metadata.complete(request, it) }
                running = null
                busy = false
                update()
                if (failed && metadata.selectedId == request.source.id) showFailure()
            }
            override fun onThrowable(error: Throwable) {
                if (disposed || project.isDisposed) return
                running = null
                busy = false
                update()
                if (metadata.selectedId == request.source.id) {
                    showFailure()
                }
            }
            override fun onCancel() {
                if (disposed || project.isDisposed) return
                running = null
                busy = false
                update()
            }
        })
    }

    private fun showFailure() {
        // Driver exceptions may contain passwords or JDBC URLs; never display or log them.
        status.text = "조회 실패: DB 종류·JAR·접속 정보·권한을 확인하세요."
    }
}
