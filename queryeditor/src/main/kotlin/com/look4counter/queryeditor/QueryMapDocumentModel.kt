package com.look4counter.queryeditor

import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.SmartPsiElementPointer
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.psi.xml.XmlTag
import com.intellij.psi.xml.XmlText

internal data class QueryEntry(
    val target: SmartPsiElementPointer<XmlTag>,
    val id: String,
    val description: String,
    val resultType: String,
    val isNamed: String,
    val sql: String,
    val sqlEditable: Boolean,
) {
    override fun toString(): String = id.ifEmpty { "(id 없음)" }

    fun attribute(name: String): String = when (name) {
        "id" -> id
        "desc" -> description
        "resultType" -> resultType
        "isNamed" -> isNamed
        else -> error("Unsupported query attribute: $name")
    }
}

internal data class QueryMapSnapshot(
    val description: String = "",
    val queries: List<QueryEntry> = emptyList(),
    val problem: String? = null,
    val target: SmartPsiElementPointer<XmlTag>? = null,
)

internal data class QueryEdit(
    val target: SmartPsiElementPointer<XmlTag>,
    val attribute: String?,
    val original: String,
    val value: String,
)

internal class QueryMapDocumentModel(private val project: Project, private val file: VirtualFile) {
    val document = requireNotNull(FileDocumentManager.getInstance().getDocument(file))
    val writable: Boolean get() = file.isValid && file.isWritable && document.isWritable

    fun snapshot(): QueryMapSnapshot {
        val manager = PsiDocumentManager.getInstance(project)
        return ReadAction.compute<QueryMapSnapshot, RuntimeException> {
            val xml = manager.getPsiFile(document) as? XmlFile
            val root = xml?.rootTag
            if (root == null || root.localName != "queryMap" || PsiTreeUtil.findChildOfType(xml, PsiErrorElement::class.java) != null) {
                QueryMapSnapshot(problem = "올바른 queryMap XML이 필요합니다. Source 탭에서 수정하세요.")
            } else {
                val pointers = SmartPointerManager.getInstance(project)
                QueryMapSnapshot(attribute(root, "desc"), root.subTags.filter {
                    it.localName == "query" && it.namespace == root.namespace
                }.map { tag ->
                    QueryEntry(
                        pointers.createSmartPsiElementPointer(tag),
                        attribute(tag, "id"),
                        attribute(tag, "desc"),
                        attribute(tag, "resultType"),
                        attribute(tag, "isNamed"),
                        sql(tag),
                        tag.value.children.all { it is XmlText },
                    )
                }, target = pointers.createSmartPsiElementPointer(root))
            }
        }
    }

    fun apply(edits: List<QueryEdit>): Boolean {
        if (edits.isEmpty()) return true
        if (!writable) return false
        val manager = PsiDocumentManager.getInstance(project)
        manager.commitDocument(document)
        var accepted = true
        WriteCommandAction.writeCommandAction(project).withName("쿼리 수정").run<RuntimeException> {
            for (edit in edits) {
                manager.commitDocument(document)
                val tag = edit.target.element
                val root = (manager.getPsiFile(document) as? XmlFile)?.rootTag
                val isMapDescription = tag != null && tag == root && edit.attribute == "desc"
                val isQuery = tag != null && root != null && tag.parent == root &&
                    tag.localName == "query" && tag.namespace == root.namespace
                if (tag == null || root == null || root.localName != "queryMap" ||
                    (!isMapDescription && !isQuery) ||
                    PsiTreeUtil.findChildOfType(root, PsiErrorElement::class.java) != null
                ) {
                    accepted = false
                    continue
                }
                val current = if (edit.attribute == null) sql(tag) else attribute(tag, edit.attribute)
                if (current == edit.value) continue
                if (current != edit.original || (edit.attribute == null && tag.value.children.any { it !is XmlText })) {
                    accepted = false
                    continue
                }
                if (edit.attribute != null) {
                    tag.setAttribute(edit.attribute, StringUtil.escapeXmlEntities(edit.value))
                    manager.doPostponedOperationsAndUnblockDocument(document)
                } else {
                    val cdata = querySqlCdata(edit.value)
                    if (tag.text.endsWith("/>")) {
                        document.replaceString(tag.textRange.endOffset - 2, tag.textRange.endOffset, ">$cdata</${tag.name}>")
                    } else {
                        val range = tag.value.textRange
                        document.replaceString(range.startOffset, range.endOffset, cdata)
                    }
                }
            }
            manager.commitDocument(document)
        }
        return accepted
    }

    private fun sql(tag: XmlTag): String = tag.value.textElements.joinToString("") { it.value }
    private fun attribute(tag: XmlTag, name: String): String = tag.getAttribute(name)?.displayValue.orEmpty()
}

internal fun querySqlCdata(value: String): String = "<![CDATA[${value.replace("]]>", "]]]]><![CDATA[>")}]]>"
