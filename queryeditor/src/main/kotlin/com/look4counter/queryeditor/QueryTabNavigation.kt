package com.look4counter.queryeditor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.TransactionGuard
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.xml.XmlTag

internal object QueryTabNavigation {
    val designQuery = Key.create<SmartPsiElementPointer<XmlTag>>("glue.query.tabs.design.query")
    val sourceOffset = Key.create<Int>("glue.query.tabs.source.offset")

    fun whenWriteSafe(project: Project, file: VirtualFile, alive: () -> Boolean, action: () -> Unit) {
        if (project.isDisposed || !file.isValid || !alive()) return
        val application = ApplicationManager.getApplication()
        val guard = TransactionGuard.getInstance()
        if (application.isDispatchThread && application.isWriteIntentLockAcquired && guard.isWritingAllowed) {
            action()
        } else {
            val requested = ModalityState.defaultModalityState()
            application.invokeLater({ whenWriteSafe(project, file, alive, action) },
                if (guard.isWriteSafeModality(requested)) requested else ModalityState.nonModal())
        }
    }
}
