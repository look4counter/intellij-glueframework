package com.look4counter.queryeditor.sql

import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileTypes.PlainTextFileType
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFileFactory
import com.intellij.util.textCompletion.TextCompletionUtil

internal object SqlDocuments {
    fun create(project: Project, text: String): Document {
        val file = PsiFileFactory.getInstance(project).createFileFromText(
            "glue-query.txt", PlainTextFileType.INSTANCE, text, System.currentTimeMillis(), true,
        )
        TextCompletionUtil.installProvider(file, SqlKeywordCompletionProvider(project), true)
        return requireNotNull(PsiDocumentManager.getInstance(project).getDocument(file))
    }
}
