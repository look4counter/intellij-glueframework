package com.look4counter.queryeditor

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.patterns.PlatformPatterns
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementResolveResult
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiLiteralExpression
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiPolyVariantReferenceBase
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceContributor
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.ResolveResult
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.psi.xml.XmlFile
import com.intellij.util.ProcessingContext
import com.look4counter.queryeditor.queryindex.QueryIdIndexService

class JavaQueryReferenceContributor : PsiReferenceContributor() {
    override fun registerReferenceProviders(registrar: PsiReferenceRegistrar) {
        registrar.registerReferenceProvider(PlatformPatterns.psiElement(PsiLiteralExpression::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val literal = element as PsiLiteralExpression
                    val id = literal.value as? String ?: return PsiReference.EMPTY_ARRAY
                    if (id.isBlank() || QueryIdIndexService.getInstance(element.project).snapshot(id).none { it.id == id }) {
                        return PsiReference.EMPTY_ARRAY
                    }
                    return arrayOf(JavaQueryReference(literal))
                }
            })
    }
}

private class JavaQueryReference(literal: PsiLiteralExpression) : PsiPolyVariantReferenceBase<PsiLiteralExpression>(
    literal, ElementManipulators.getValueTextRange(literal), true,
) {
    override fun multiResolve(incompleteCode: Boolean): Array<ResolveResult> {
        val id = element.value as? String ?: return ResolveResult.EMPTY_ARRAY
        val project = element.project
        if (project.isDisposed) return ResolveResult.EMPTY_ARRAY
        val manager = PsiManager.getInstance(project)
        val documents = PsiDocumentManager.getInstance(project)
        val results = mutableListOf<ResolveResult>()
        val urls = QueryIdIndexService.getInstance(project).snapshot(id).filter { it.id == id }.map { it.fileUrl }.distinct()
        for (url in urls) {
            ProgressManager.checkCanceled()
            val file = VirtualFileManager.getInstance().findFileByUrl(url)?.takeIf { it.isValid } ?: continue
            val document = FileDocumentManager.getInstance().getCachedDocument(file)
            if (document != null && !documents.isCommitted(document)) continue
            val xml = manager.findFile(file) as? XmlFile ?: continue
            val root = xml.rootTag ?: continue
            if (root.localName != "queryMap" || PsiTreeUtil.findChildOfType(xml, PsiErrorElement::class.java) != null) continue
            root.subTags.filter { tag ->
                tag.localName == "query" && tag.namespace == root.namespace &&
                    StringUtil.unescapeXmlEntities(tag.getAttributeValue("id").orEmpty()) == id
            }.forEach { tag ->
                tag.getAttribute("id")?.valueElement?.let { results += PsiElementResolveResult(it) }
            }
        }
        return results.toTypedArray()
    }

    override fun getVariants(): Array<Any> = emptyArray()
}
