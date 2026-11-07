package com.look4counter.queryeditor.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.look4counter.queryeditor.sql.SqlFormattingOptions

data class QueryFormattingState(var alignWhereEquals: Boolean = false, var alignSelectAs: Boolean = false)

@Service(Service.Level.PROJECT)
@State(name = "com.look4counter.GlueQueryEditorFormatting", storages = [Storage("glue-query-editor.xml")])
class QueryFormattingSettings : PersistentStateComponent<QueryFormattingState> {
    @Volatile private var saved = QueryFormattingState()

    override fun getState(): QueryFormattingState = saved.copy()
    override fun loadState(state: QueryFormattingState) { saved = state.copy() }

    val options: SqlFormattingOptions get() = saved.let { SqlFormattingOptions(it.alignWhereEquals, it.alignSelectAs) }

    companion object {
        fun getInstance(project: Project): QueryFormattingSettings = project.getService(QueryFormattingSettings::class.java)
    }
}
