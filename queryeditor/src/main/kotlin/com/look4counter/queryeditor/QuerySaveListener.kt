package com.look4counter.queryeditor

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.ProjectLocator

class QuerySaveListener : FileDocumentManagerListener {
    override fun beforeDocumentSaving(document: Document) {
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (file.fileType != GlueSqlFileType.INSTANCE) return
        val project = ProjectLocator.getInstance().guessProjectForFile(file) ?: return
        QuerySqlNormalizer.normalize(project, document)
    }
}
