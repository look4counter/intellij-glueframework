package com.look4counter.queryeditor.settings

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.look4counter.queryeditor.database.DatabaseMetadataService
import java.awt.FlowLayout
import javax.swing.JButton

internal class DataSourceMetadataToolbar(private val project: Project, parent: Disposable) :
    JBPanel<DataSourceMetadataToolbar>(FlowLayout(FlowLayout.LEFT, JBUI.scale(6), JBUI.scale(4))) {
    private val metadata = DatabaseMetadataService.getInstance(project)
    private val refresh = JButton("메타데이터 새로고침").apply {
        name = "query.metadata.refresh"
        toolTipText = "저장된 전체 Datasource를 조회합니다. 변경한 설정은 먼저 적용하세요."
    }
    private val status = JBLabel().apply { name = "query.metadata.status" }
    @Volatile private var disposed = false

    init {
        add(refresh)
        add(JBLabel("변경한 설정은 먼저 적용하세요."))
        add(status)
        refresh.addActionListener { metadata.refreshAll(); update() }
        metadata.addListener(parent) {
            ApplicationManager.getApplication().invokeLater({ if (!disposed && !project.isDisposed) update() }, ModalityState.any())
        }
        Disposer.register(parent) { disposed = true }
        update()
    }

    private fun update() {
        refresh.isEnabled = DataSourceSettingsService.getInstance(project).dataSources.isNotEmpty() && !metadata.loading
        val snapshot = metadata.snapshot()
        val counts = snapshot?.let { "테이블 ${it.tableNames.size} · 컬럼 ${it.columnNames.size}" }.orEmpty()
        status.text = when {
            metadata.loading -> "메타데이터 조회 중…"
            metadata.failureCount > 0 -> "조회 실패: ${metadata.failureCount}개 DB의 종류·JAR·접속 정보·권한을 확인하세요. $counts"
            else -> counts
        }
    }
}
