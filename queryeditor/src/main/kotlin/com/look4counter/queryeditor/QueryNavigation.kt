package com.look4counter.queryeditor

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.openapi.util.text.StringUtil
import com.look4counter.queryeditor.queryindex.QueryIdIndexService

data class QueryLocation(val id: String, val file: VirtualFile, val offset: Int) {
    override fun toString(): String = "${file.presentableUrl}  ($id)"
}

object QueryNavigation {
    internal val requestedQuery = com.intellij.openapi.util.Key.create<String>("glue.query.navigation.id")
    fun find(project: Project, id: String): List<QueryLocation> {
        if (project.isDisposed || id.isBlank()) return emptyList()
        PsiDocumentManager.getInstance(project).commitAllDocuments()
        return QueryIdIndexService.getInstance(project).snapshot().filter { it.id == id }.mapNotNull { entry ->
            val file = VirtualFileManager.getInstance().findFileByUrl(entry.fileUrl) ?: return@mapNotNull null
            ReadAction.compute<QueryLocation?, RuntimeException> {
                queryTags(project, file).firstOrNull { StringUtil.unescapeXmlEntities(it.getAttributeValue("id").orEmpty()) == id }
                    ?.let { QueryLocation(id, file, it.textRange.startOffset) }
            }
        }
    }

    fun queryAtOffset(project: Project, file: VirtualFile, offset: Int): XmlTag? =
        queryTags(project, file).firstOrNull { offset >= it.textRange.startOffset && offset < it.textRange.endOffset }

    private fun queryTags(project: Project, file: VirtualFile): List<XmlTag> {
        val root = (PsiManager.getInstance(project).findFile(file) as? XmlFile)?.rootTag ?: return emptyList()
        if (root.localName != "queryMap") return emptyList()
        return root.subTags.filter { it.localName == "query" && it.namespace == root.namespace }
    }

    fun open(project: Project, id: String, file: VirtualFile? = null): Boolean {
        val matches = find(project, id).filter { file == null || it.file == file }
        if (matches.isEmpty()) return false
        if (matches.size == 1) return open(project, matches.single())
        JBPopupFactory.getInstance().createPopupChooserBuilder(matches).setTitle("Query 파일 선택")
            .setItemChosenCallback { open(project, it) }.createPopup().showInFocusCenter()
        return true
    }

    fun open(project: Project, location: QueryLocation): Boolean {
        if (project.isDisposed || !location.file.isValid) return false
        val manager = FileEditorManager.getInstance(project)
        location.file.putUserData(requestedQuery, location.id)
        val editors = manager.openFile(location.file, true)
        manager.setSelectedEditor(location.file, "glue-query-design")
        if (editors.filterIsInstance<QueryEditorNavigation>().any { it.selectQuery(location.id) })
            location.file.putUserData(requestedQuery, null)
        return true
    }
}
