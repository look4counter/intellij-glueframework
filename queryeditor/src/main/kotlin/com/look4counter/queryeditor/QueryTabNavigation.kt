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
    data class SqlCaret(val target: SmartPsiElementPointer<XmlTag>, val offset: Int)
    val designQuery = Key.create<SmartPsiElementPointer<XmlTag>>("glue.query.tabs.design.query")
    val designCaret = Key.create<SqlCaret>("glue.query.tabs.design.caret")
    val sourceOffset = Key.create<Int>("glue.query.tabs.source.offset")

    fun sqlOffset(tag: XmlTag, sourceOffset: Int): Int? {
        var offset = 0
        for (text in tag.value.textElements) {
            val range = text.textRange
            if (sourceOffset in range.startOffset..range.endOffset) {
                val local = text.physicalToDisplay(sourceOffset - range.startOffset)
                return if (local >= 0) offset + local.coerceAtMost(text.value.length) else null
            }
            offset += text.value.length
        }
        return null
    }

    fun sourceOffset(tag: XmlTag, sqlOffset: Int): Int {
        val texts = tag.value.textElements
        var offset = sqlOffset.coerceIn(0, texts.sumOf { it.value.length })
        for (text in texts) {
            if (offset <= text.value.length) {
                return text.textRange.startOffset + text.displayToPhysical(offset).coerceAtLeast(0)
            }
            offset -= text.value.length
        }
        return tag.textRange.startOffset
    }

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
