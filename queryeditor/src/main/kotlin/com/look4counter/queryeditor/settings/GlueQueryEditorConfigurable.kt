package com.look4counter.queryeditor.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import java.util.Locale
import javax.swing.JComponent

open class GlueQueryEditorConfigurable(private val project: Project) : SearchableConfigurable, Configurable.NoScroll {
    private var panel: DataSourceSettingsPanel? = null
    private val settings get() = DataSourceSettingsService.getInstance(project)

    override fun getId(): String = "com.look4counter.glueframework.queryeditor"
    override fun getDisplayName(): String = "GlueQueryEditor"
    override fun createComponent(): JComponent = panel ?: DataSourceSettingsPanel().also { panel = it }

    override fun reset() {
        panel?.reset(settings.dataSources)
    }

    override fun isModified(): Boolean {
        val drafts = panel?.snapshot() ?: return false
        return drafts.map { it.definition } != settings.dataSources || drafts.any { it.password != null || it.clearPassword }
    }

    protected open fun createPasswordManager(): DataSourcePasswordManager = DataSourcePasswordManager(project.locationHash)

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
        settings.replaceDataSources(definitions)
        panel?.reset(definitions)
    }

    override fun disposeUIResources() {
        panel?.clear()
        panel = null
    }
}
