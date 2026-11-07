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

    override fun applyPrefixMatcher(result: CompletionResultSet, prefix: String): CompletionResultSet {
        result.restartCompletionOnPrefixChange(".*\\..*")
        return result.withPrefixMatcher(prefix).caseInsensitive()
    }

    override fun acceptChar(char: Char): CharFilter.Result =
        if (SqlLexer.isWord(char) || char == '.') CharFilter.Result.ADD_TO_PREFIX else CharFilter.Result.HIDE_LOOKUP

    override fun fillCompletionVariants(parameters: CompletionParameters, prefix: String, result: CompletionResultSet) {
        val text = parameters.editor.document.text
        val offset = parameters.offset.coerceIn(0, text.length)
        if (!parameters.editor.isViewer && parameters.editor.document.isWritable &&
            (prefix.isNotEmpty() || parameters.invocationCount > 0 || text.getOrNull(offset - 1) == '.')
        ) {
            val context = SqlCompletionAnalyzer.analyze(text, offset, DatabaseMetadataService.getInstance(project).snapshot())
            val candidates = linkedMapOf<String, MutableSet<String>>()
            val lookupNames = mutableMapOf<String, MutableSet<String>>()
            val names = context.candidates + if (context.includeKeywords) SqlKeywords.words.map { SqlNameCandidate(it, it, "SQL 키워드") } else emptyList()
            names.filter { it.lookupName.startsWith(prefix, ignoreCase = true) || it.text.startsWith(prefix, ignoreCase = true) }.forEach {
                candidates.getOrPut(it.text) { linkedSetOf() }.add(it.kind)
                lookupNames.getOrPut(it.text) { linkedSetOf() }.add(it.lookupName)
            }
            candidates.forEach { (name, kinds) ->
                result.addElement(LookupElementBuilder.create(name).withLookupStrings(lookupNames.getValue(name))
                    .withCaseSensitivity(false).withTypeText(kinds.joinToString(" / "), true))
            }
        }
        result.stopHere()
    }
}
