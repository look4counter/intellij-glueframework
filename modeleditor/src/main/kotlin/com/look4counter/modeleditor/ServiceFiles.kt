package com.look4counter.modeleditor

import com.intellij.openapi.project.Project
import com.intellij.openapi.project.guessProjectDir
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.fileEditor.FileEditorManager
import java.nio.file.Path

object ServiceFiles {
    fun projectDirectory(project: Project): VirtualFile? = project.guessProjectDir()
        ?: com.intellij.openapi.roots.ProjectRootManager.getInstance(project).contentRoots.firstOrNull()

    fun directory(project: Project, configured: String = ModelEditorSettings.getInstance(project).state.serviceDirectory): VirtualFile? {
        if (configured.isBlank()) return null
        val root = projectDirectory(project) ?: return null
        val value = configured.trim().replace('\\', '/')
        val target = if (value.contains("://")) com.intellij.openapi.vfs.VirtualFileManager.getInstance().findFileByUrl(value)
            else runCatching {
                val path = Path.of(value).normalize()
                if (path.isAbsolute) LocalFileSystem.getInstance().findFileByPath(path.toString().replace('\\', '/'))
                else if (path.startsWith("..")) null else if (path.toString().isEmpty() || path.toString() == ".") root
                else root.findFileByRelativePath(path.toString().replace('\\', '/'))
            }.getOrNull()
        return target?.takeIf { it.isDirectory && com.intellij.openapi.vfs.VfsUtilCore.isAncestor(root, it, false) }
    }
    fun relativeDirectory(project: Project, configured: String): String? {
        val root = projectDirectory(project) ?: return null
        val target = directory(project, configured) ?: return null
        return com.intellij.openapi.vfs.VfsUtilCore.getRelativePath(target, root, '/')?.ifEmpty { "." }
    }
    fun names(project: Project): List<String> = directory(project)?.children.orEmpty()
        .filter { !it.isDirectory && it.extension.equals("xml", true) &&
            (!ModelEditorSettings.getInstance(project).state.noUseGlueUmlAdFile || it.name.endsWith("-service.xml", true)) }
        .map { it.nameWithoutExtension }.distinct().sorted()
    fun resolve(project: Project, name: String): VirtualFile? {
        return find(project, name).firstOrNull()
    }
    fun find(project: Project, name: String): List<VirtualFile> {
        if (project.isDisposed) return emptyList()
        val base = name.trim().replace(Regex("(?i)\\.xml$"), "").replace(Regex("(?i)-service$"), "")
        if (base.isBlank() || base.any { it in "/\\:" } || base == "." || base == "..") return emptyList()
        return com.intellij.openapi.application.ReadAction.compute<List<VirtualFile>, RuntimeException> {
            val expected = if (ModelEditorSettings.getInstance(project).state.noUseGlueUmlAdFile) "$base-service.xml" else "$base.glue_uml_ad"
            val preferred = directory(project)?.findChild(expected)?.takeUnless { it.isDirectory }
            val matches = linkedSetOf<VirtualFile>()
            if (preferred != null) matches += preferred
            com.intellij.openapi.roots.ProjectFileIndex.getInstance(project).iterateContent { file ->
                com.intellij.openapi.progress.ProgressManager.checkCanceled()
                if (!file.isDirectory && file.name.equals(expected, true)) matches += file
                true
            }
            matches.sortedWith(compareBy<VirtualFile> { if (it == preferred) 0 else 1 }.thenBy { it.path })
        }
    }
    fun open(project: Project, name: String): Boolean {
        val files = find(project, name)
        if (files.isEmpty()) return false
        if (files.size == 1) openDesign(project, files.single())
        else com.intellij.openapi.ui.popup.JBPopupFactory.getInstance().createPopupChooserBuilder(files)
            .setRenderer(object : javax.swing.DefaultListCellRenderer() {
                override fun getListCellRendererComponent(list: javax.swing.JList<*>, value: Any?, index: Int,
                                                         selected: Boolean, focused: Boolean): java.awt.Component =
                    super.getListCellRendererComponent(list, (value as? VirtualFile)?.presentableUrl.orEmpty(), index, selected, focused)
            })
            .setTitle("Service 파일 선택").setItemChosenCallback { openDesign(project, it) }
            .createPopup().showInFocusCenter()
        return true
    }
    private fun openDesign(project: Project, file: VirtualFile) {
        val manager = FileEditorManager.getInstance(project)
        manager.openFile(file, true)
        manager.setSelectedEditor(file, "glue-model-design")
    }
}
