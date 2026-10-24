package com.look4counter.queryeditor.queryindex

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class QueryIdIndexStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        QueryIdIndexService.getInstance(project)
    }
}
