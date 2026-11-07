package com.look4counter.queryeditor.settings

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project

data class QueryCommentState(var template: String = DEFAULT_TEMPLATE) {
    companion object {
        val DEFAULT_TEMPLATE = """
            /*
                QueryId : ${'$'}{QUERY_ID} / ${'$'}{QUERY_DESCRIPTION}
                Created by : 작성자

                -- 수정이력 --
                ${'$'}{yyyy-MM-dd}
            */
        """.trimIndent()
    }
}

@Service(Service.Level.PROJECT)
@State(name = "com.look4counter.GlueQueryEditorComment", storages = [Storage("glue-query-editor.xml")])
class QueryCommentSettings : PersistentStateComponent<QueryCommentState> {
    @Volatile private var saved = QueryCommentState()

    override fun getState(): QueryCommentState = saved.copy()
    override fun loadState(state: QueryCommentState) { saved = state.copy() }

    companion object {
        fun getInstance(project: Project): QueryCommentSettings = project.getService(QueryCommentSettings::class.java)
    }
}
