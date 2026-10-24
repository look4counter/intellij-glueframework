package com.look4counter.modeleditor

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.fileEditor.FileDocumentManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.xml.XmlFile

class ModelServiceSaveListener : FileDocumentManagerListener {
    override fun afterDocumentSaved(document: Document) {
        val file = FileDocumentManager.getInstance().getFile(document) ?: return
        if (!file.extension.equals("glue_uml_ad", true)) return
        ProjectManager.getInstance().openProjects.filter { !it.isDisposed && ProjectFileIndex.getInstance(it).isInContent(file) }
            .forEach { project ->
                try { save(project, file, document.text) }
                catch (error: IllegalArgumentException) { notifyFailure(project, file, error.message.orEmpty()) }
                catch (error: java.io.IOException) { notifyFailure(project, file, error.message.orEmpty()) }
            }
    }

    fun save(project: Project, source: VirtualFile, text: String): VirtualFile? {
        if (ModelEditorSettings.getInstance(project).state.noUseGlueUmlAdFile) return null
        val directory = ServiceFiles.directory(project) ?: return null
        val content = ReadAction.compute<String, RuntimeException> {
            val xml = PsiFileFactory.getInstance(project).createFileFromText(source.name, GlueUmlAdFileType.INSTANCE, text) as XmlFile
            ModelServiceXmlExporter.export(xml, source.nameWithoutExtension)
        }
        var output: VirtualFile? = null
        ApplicationManager.getApplication().runWriteAction {
            val name = "${source.nameWithoutExtension}-service.xml"
            val target = directory.findChild(name) ?: directory.createChildData(this, name)
            require(!target.isDirectory && target.isWritable) { "Service 출력 파일을 쓸 수 없습니다: ${target.presentableUrl}" }
            val manager = FileDocumentManager.getInstance()
            val document = manager.getCachedDocument(target)
            if (document != null) {
                if (document.text != content) document.setText(content)
                manager.saveDocument(document)
            } else if (VfsUtil.loadText(target) != content) VfsUtil.saveText(target, content)
            output = target
        }
        return output
    }

    private fun notifyFailure(project: Project, source: VirtualFile, reason: String) {
        NotificationGroupManager.getInstance().getNotificationGroup("Glue Model Service Export")
            .createNotification("Service XML 생성 실패: ${source.name}", reason, NotificationType.WARNING).notify(project)
    }
}
