package com.look4counter.queryeditor.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBCheckBox
import com.intellij.ui.TitledSeparator
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.GridLayout
import java.util.Locale
import javax.swing.JComponent

open class GlueQueryEditorConfigurable(private val project: Project) : SearchableConfigurable, Configurable.NoScroll {
    private var panel: DataSourceSettingsPanel? = null
    private var component: JComponent? = null
    private var uiDisposable: Disposable? = null
    private val settings get() = DataSourceSettingsService.getInstance(project)
    private val formattingSettings get() = QueryFormattingSettings.getInstance(project)
    private var alignWhereEquals: JBCheckBox? = null
    private var alignSelectAs: JBCheckBox? = null

    override fun getId(): String = "com.look4counter.glueframework.queryeditor"
    override fun getDisplayName(): String = "GlueQueryEditor"
    override fun createComponent(): JComponent = component ?: run {
        val lifetime = Disposer.newDisposable("Datasource settings UI")
        uiDisposable = lifetime
        val settingsPanel = DataSourceSettingsPanel(::testDraftConnection).also { panel = it }
        JBPanel<JBPanel<*>>(BorderLayout()).apply {
            add(JBPanel<JBPanel<*>>(BorderLayout(0, JBUI.scale(8))).apply {
                border = JBUI.Borders.emptyBottom(12)
                add(TitledSeparator("SQL 포매팅 스타일"), BorderLayout.NORTH)
                add(JBPanel<JBPanel<*>>(GridLayout(0, 1, 0, JBUI.scale(4))).apply {
                    border = JBUI.Borders.emptyLeft(20)
                    add(JBCheckBox("WHERE절 등호에 맞춰 정렬").apply {
                        name = "format.alignWhereEquals"
                        isSelected = formattingSettings.state.alignWhereEquals
                        alignWhereEquals = this
                    })
                    add(JBCheckBox("SELECT절 AS에 맞춰 정렬").apply {
                        name = "format.alignSelectAs"
                        isSelected = formattingSettings.state.alignSelectAs
                        alignSelectAs = this
                    })
                }, BorderLayout.CENTER)
            }, BorderLayout.NORTH)
            add(settingsPanel, BorderLayout.CENTER)
            add(DataSourceMetadataToolbar(project, lifetime), BorderLayout.SOUTH)
        }.also { component = it }
    }

    override fun reset() {
        panel?.reset(settings.dataSources)
        alignWhereEquals?.isSelected = formattingSettings.state.alignWhereEquals
        alignSelectAs?.isSelected = formattingSettings.state.alignSelectAs
    }

    override fun isModified(): Boolean {
        val drafts = panel?.snapshot() ?: return false
        return drafts.map { it.definition } != settings.dataSources || drafts.any { it.password != null || it.clearPassword } ||
            alignWhereEquals?.isSelected != formattingSettings.state.alignWhereEquals ||
            alignSelectAs?.isSelected != formattingSettings.state.alignSelectAs
    }

    protected open fun createPasswordManager(): DataSourcePasswordManager = DataSourcePasswordManager(project.locationHash)

    protected open fun testConnection(source: DataSourceDefinition, password: String?) {
        com.look4counter.queryeditor.database.JdbcMetadataReader().testConnection(source, password)
    }

    protected open fun showConnectionTestResult(success: Boolean) {
        if (success) com.intellij.openapi.ui.Messages.showInfoMessage(project, "DB 연결에 성공했습니다.", "연결 테스트")
        else com.intellij.openapi.ui.Messages.showErrorDialog(project,
            "DB에 연결하지 못했습니다. DB 종류·JDBC URL·드라이버 JAR·사용자명·비밀번호와 네트워크를 확인하세요.", "연결 테스트")
    }

    private fun testDraftConnection(draft: DataSourceDraft) {
        val source = draft.definition.copy(jdbcUrl = draft.definition.jdbcUrl.trim(),
            username = draft.definition.username.trim(), driverClass = draft.definition.driverClass.trim())
        if (!source.jdbcUrl.startsWith("jdbc:") || (draft.password != null && draft.clearPassword)) {
            showConnectionTestResult(false)
            return
        }
        var success = false
        val completed = ProgressManager.getInstance().runProcessWithProgressSynchronously(Runnable {
            try {
                val password = when {
                    draft.password != null -> draft.password
                    draft.clearPassword -> null
                    source.passwordStored -> createPasswordManager().readPassword(source.id)
                    else -> null
                }
                ProgressManager.checkCanceled()
                testConnection(source, password)
                ProgressManager.checkCanceled()
                success = true
            } catch (canceled: com.intellij.openapi.progress.ProcessCanceledException) {
                throw canceled
            } catch (_: Exception) {
                success = false
            }
        }, "DB 연결 테스트", true, project)
        if (completed && !project.isDisposed && panel != null) showConnectionTestResult(success)
    }

    override fun apply() {
        val drafts = panel?.snapshot() ?: return
        val definitions = drafts.map { row ->
            row.definition.copy(name = row.definition.name.trim(), jdbcUrl = row.definition.jdbcUrl.trim(),
                driverClass = row.definition.driverClass.trim(), username = row.definition.username.trim(),
                passwordStored = when { row.clearPassword -> false; row.password != null -> true; else -> row.definition.passwordStored },
                databaseKind = DatabaseKind.of(row.definition).name,
                driverJars = row.definition.driverJars.map { it.trim() }.filter { it.isNotEmpty() }.distinct().toMutableList())
        }
        if (definitions.any { it.name.isEmpty() }) throw ConfigurationException("DataSource 이름을 입력하세요.")
        if (definitions.any { !it.jdbcUrl.startsWith("jdbc:") || it.jdbcUrl.length <= 5 }) {
            throw ConfigurationException("jdbc:로 시작하는 JDBC URL을 입력하세요.")
        }
        if (definitions.map { it.name.lowercase(Locale.ROOT) }.toSet().size != definitions.size) {
            throw ConfigurationException("DataSource 이름은 중복될 수 없습니다.")
        }
        if (drafts.any { it.password != null && it.clearPassword }) {
            throw ConfigurationException("비밀번호 입력과 삭제 중 하나만 선택하세요.")
        }
        val retainedIds = definitions.map { it.id }.toSet()
        val passwords = linkedMapOf<String, String?>()
        settings.dataSources.filter { it.passwordStored && it.id !in retainedIds }.forEach { passwords[it.id] = null }
        drafts.forEach { if (it.clearPassword || it.password != null) passwords[it.definition.id] = if (it.clearPassword) null else it.password }
        if (passwords.isNotEmpty()) {
            var failure: Exception? = null
            ProgressManager.getInstance().runProcessWithProgressSynchronously(Runnable {
                try { createPasswordManager().applyChanges(passwords) } catch (error: Exception) { failure = error }
            }, "DataSource 비밀번호 저장", false, project)
            if (failure != null) throw ConfigurationException("비밀번호를 저장하지 못했습니다. 보안 저장소 상태를 확인하고 다시 적용하세요.")
        }
        if (definitions != settings.dataSources || passwords.isNotEmpty()) settings.replaceDataSources(definitions)
        formattingSettings.loadState(QueryFormattingState(alignWhereEquals?.isSelected == true, alignSelectAs?.isSelected == true))
        panel?.reset(definitions)
    }

    override fun disposeUIResources() {
        uiDisposable?.let { Disposer.dispose(it) }
        uiDisposable = null
        panel?.clear()
        panel = null
        component = null
        alignWhereEquals = null
        alignSelectAs = null
    }
}
