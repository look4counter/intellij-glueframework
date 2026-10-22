package com.look4counter.queryeditor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.vfs.VirtualFile

class QueryOpenListener : FileEditorManagerListener {
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        if (!file.isValid || file.fileType != GlueSqlFileType.INSTANCE) return
        val project = source.project
        ApplicationManager.getApplication().invokeLater({
            if (!project.isDisposed && file.isValid && source.isFileOpen(file)) {
                val document = FileDocumentManager.getInstance().getDocument(file) ?: return@invokeLater
                QuerySqlNormalizer.normalize(project, document)
            }
        }, ModalityState.nonModal())
    }
}
