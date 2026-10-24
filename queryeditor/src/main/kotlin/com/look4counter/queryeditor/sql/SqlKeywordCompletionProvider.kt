package com.look4counter.queryeditor.sql

import com.intellij.codeInsight.completion.CompletionParameters
import com.intellij.codeInsight.completion.CompletionResultSet
import com.intellij.codeInsight.lookup.CharFilter
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.look4counter.queryeditor.database.DatabaseMetadataService
import com.intellij.util.textCompletion.TextCompletionProvider

internal class SqlKeywordCompletionProvider(private val project: Project) : TextCompletionProvider, DumbAware {
    override fun getAdvertisement(): String? = null

    override fun getPrefix(text: String, offset: Int): String {
        val end = offset.coerceIn(0, text.length)
        var start = end
        while (start > 0 && SqlLexer.isWord(text[start - 1])) start--
        return text.substring(start, end)
    }

    override fun applyPrefixMatcher(result: CompletionResultSet, prefix: String): CompletionResultSet =
        result.withPrefixMatcher(prefix).caseInsensitive()

    override fun acceptChar(char: Char): CharFilter.Result =
        if (SqlLexer.isWord(char)) CharFilter.Result.ADD_TO_PREFIX else CharFilter.Result.HIDE_LOOKUP

    override fun fillCompletionVariants(parameters: CompletionParameters, prefix: String, result: CompletionResultSet) {
        val text = parameters.originalFile.text
        if (!parameters.editor.isViewer && parameters.editor.document.isWritable &&
            SqlLexer.completionAllowed(text, parameters.offset.coerceAtMost(text.length)) &&
            (prefix.isNotEmpty() || parameters.invocationCount > 0)
        ) {
            val candidates = linkedMapOf<String, MutableSet<String>>()
            fun add(names: Iterable<String>, kind: String) {
                names.filter { it.startsWith(prefix, ignoreCase = true) }.forEach { candidates.getOrPut(it) { linkedSetOf() }.add(kind) }
            }
            add(SqlKeywords.words, "SQL 키워드")
            DatabaseMetadataService.getInstance(project).snapshot()?.let {
                add(it.tableNames, "테이블")
                add(it.columnNames, "컬럼")
            }
            candidates.forEach { (name, kinds) ->
                result.addElement(LookupElementBuilder.create(name).withCaseSensitivity(false).withTypeText(kinds.joinToString(" / "), true))
            }
        }
        result.stopHere()
    }
}
