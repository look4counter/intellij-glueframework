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

internal enum class QueryListAction(val label: String) {
    Add("추가"), Delete("삭제"), Duplicate("복제"), Up("위로"), Down("아래로")
}

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

    fun changeQueries(action: QueryListAction, target: SmartPsiElementPointer<XmlTag>?, data: NewQueryData? = null): SmartPsiElementPointer<XmlTag>? {
        if (!writable) return null
        val manager = PsiDocumentManager.getInstance(project)
        manager.commitDocument(document)
        val snapshot = snapshot()
        val root = snapshot.target?.element ?: return null
        if (snapshot.problem != null) return null
        val queries = snapshot.queries.mapNotNull { it.target.element }
        val tag = target?.element
        val index = queries.indexOf(tag)
        if (action != QueryListAction.Add && index < 0) return null
        val ids = snapshot.queries.map { it.id }.toSet()
        if (action == QueryListAction.Add && (data == null || data.validationError(ids) != null)) return null
        if (action == QueryListAction.Up && index == 0 || action == QueryListAction.Down && index == queries.lastIndex) return target
        var result: XmlTag? = null
        WriteCommandAction.writeCommandAction(project).withName("Query ${action.label}").run<RuntimeException> {
            result = when (action) {
                QueryListAction.Add -> {
                    val draft = root.createChildTag("query", root.namespace, "", false)
                    draft.setAttribute("id", StringUtil.escapeXmlEntities(data!!.id.trim()))
                    draft.setAttribute("desc", StringUtil.escapeXmlEntities(data.description))
                    draft.setAttribute("resultType", StringUtil.escapeXmlEntities(data.resultType))
                    draft.setAttribute("isNamed", data.isNamed.toString())
                    root.addSubTag(draft, false)
                }
                QueryListAction.Delete -> {
                    tag!!.delete()
                    queries.filter { it !== tag }.getOrNull(index.coerceAtMost(queries.size - 2))
                }
                QueryListAction.Duplicate -> {
                    val base = attribute(tag!!, "id").ifBlank { "query" } + "_copy"
                    var id = base
                    var suffix = 2
                    while (id in ids) id = base + suffix++
                    val copy = root.addAfter(tag.copy(), tag) as XmlTag
                    copy.setAttribute("id", StringUtil.escapeXmlEntities(id))
                    copy
                }
                QueryListAction.Up, QueryListAction.Down -> {
                    val adjacent = queries[index + if (action == QueryListAction.Up) -1 else 1]
                    val copy = (if (action == QueryListAction.Up) root.addBefore(tag!!.copy(), adjacent)
                        else root.addAfter(tag!!.copy(), adjacent)) as XmlTag
                    tag.delete()
                    copy
                }
            }
            manager.doPostponedOperationsAndUnblockDocument(document)
            manager.commitDocument(document)
        }
        return result?.let { SmartPointerManager.getInstance(project).createSmartPsiElementPointer(it) }
    }

    private fun sql(tag: XmlTag): String = tag.value.textElements.joinToString("") { it.value }
    private fun attribute(tag: XmlTag, name: String): String = tag.getAttribute(name)?.displayValue.orEmpty()
}

internal fun querySqlCdata(value: String): String = "<![CDATA[${value.replace("]]>", "]]]]><![CDATA[>")}]]>"
