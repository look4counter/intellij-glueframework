package com.look4counter.queryeditor.database

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

class DatabaseMetadataStartupActivity : ProjectActivity {
    override suspend fun execute(project: Project) {
        DatabaseMetadataService.getInstance(project).refreshAll()
    }
}
