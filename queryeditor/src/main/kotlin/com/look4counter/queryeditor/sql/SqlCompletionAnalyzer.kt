package com.look4counter.queryeditor.sql

import com.look4counter.queryeditor.database.MetadataSnapshot
import java.util.Locale

enum class SqlCompletionKind { TABLE, COLUMN, QUALIFIED_COLUMN, FALLBACK, NONE }

data class SqlNameCandidate(val text: String, val lookupName: String = text, val kind: String)
data class SqlCompletionContext(
    val kind: SqlCompletionKind,
    val candidates: List<SqlNameCandidate>,
    val includeKeywords: Boolean = true,
)

object SqlCompletionAnalyzer {
    fun analyze(text: String, offset: Int, metadata: MetadataSnapshot?): SqlCompletionContext {
        val caret = offset.coerceIn(0, text.length)
        if (!SqlLexer.completionAllowed(text, caret)) return SqlCompletionContext(SqlCompletionKind.NONE, emptyList(), false)
        return try {
            val parser = SqlQueryParser(text)
            if (parser.tokens.any { it.identifier?.quoted == true && it.start < caret && caret <= it.end }) {
                return SqlCompletionContext(SqlCompletionKind.NONE, emptyList(), false)
            }
            parser.parse(caret)
            var prefixStart = caret
            while (prefixStart > 0 && SqlLexer.isWord(text[prefixStart - 1])) prefixStart--
            val scope = parser.scopes.filter { it.start <= prefixStart && prefixStart <= it.end }.minByOrNull { it.end - it.start }
                ?: return fallback(metadata)
            val clause = scope.clause(prefixStart) ?: return fallback(metadata)
            val qualifier = qualifier(parser.tokens, prefixStart)
            if (clause == "FROM") tables(scope, qualifier, metadata)
            else if (qualifier != null) {
                val resolver = SqlQueryScope(metadata)
                val names = resolver.sources(scope, qualifier).flatMap(resolver::columns).distinct()
                SqlCompletionContext(SqlCompletionKind.QUALIFIED_COLUMN,
                    names.map { SqlNameCandidate(sqlIdentifier(it), it, "컬럼 · ${qualifier.joinToString(".") { part -> part.value }}") }, false)
            } else columns(scope, clause, metadata)
        } catch (_: SqlAnalysisLimit) { fallback(metadata) }
    }

    private fun qualifier(tokens: List<SqlToken>, prefixStart: Int): List<SqlIdentifier>? {
        var index = tokens.indexOfLast { it.end <= prefixStart }
        if (index < 0 || !tokens[index].symbol(".")) return null
        val result = mutableListOf<SqlIdentifier>()
        while (index > 0 && tokens[index].symbol(".")) {
            result.add(0, tokens[index - 1].identifier ?: return null)
            index -= 2
        }
        return result.takeIf { it.isNotEmpty() }
    }

    private fun tables(scope: SqlScope, qualifier: List<SqlIdentifier>?, metadata: MetadataSnapshot?): SqlCompletionContext {
        val tables = metadata?.tables.orEmpty().filter { table ->
            if (qualifier == null) true else {
                val actual = listOfNotNull(table.catalog, table.schema)
                actual.size >= qualifier.size && actual.takeLast(qualifier.size).zip(qualifier).all { (value, name) ->
                    name.matches(SqlIdentifier(value, name.quoted))
                }
            }
        }.map { SqlNameCandidate(sqlIdentifier(it.name), it.name, "테이블") }.toMutableList()
        if (qualifier == null) {
            scope.ctes.forEach { cte ->
                tables.removeAll { it.lookupName.equals(cte.name.value, ignoreCase = !cte.name.quoted) }
                tables.add(SqlNameCandidate(cte.name.spelling, cte.name.value, "CTE"))
            }
        }
        return SqlCompletionContext(SqlCompletionKind.TABLE, tables.distinctBy { it.text }, qualifier == null)
    }

    private fun columns(scope: SqlScope, clause: String, metadata: MetadataSnapshot?): SqlCompletionContext {
        val resolver = SqlQueryScope(metadata)
        val relations = resolver.visible(scope)
        val byName = linkedMapOf<String, MutableList<Pair<SqlSource, String>>>()
        relations.forEach { source -> resolver.columns(source).forEach { name ->
            byName.getOrPut(name.uppercase(Locale.ROOT)) { mutableListOf() }.add(source to name)
        } }
        val candidates = byName.values.flatMap { entries ->
            val duplicate = entries.map { it.first }.distinct().size > 1
            entries.map { (source, name) ->
                val insertion = if (duplicate && source.reference.isNotEmpty()) "${source.reference}.${sqlIdentifier(name)}" else sqlIdentifier(name)
                SqlNameCandidate(insertion, name, "컬럼 · ${source.reference}")
            }
        }.toMutableList()
        if (candidates.isEmpty()) candidates.addAll(metadata?.columnNames.orEmpty().map { SqlNameCandidate(sqlIdentifier(it), it, "컬럼") })
        if (clause == "ORDER" || clause == "GROUP") {
            candidates.addAll(resolver.output(scope).map { SqlNameCandidate(sqlIdentifier(it), it, "출력 컬럼") })
        }
        return SqlCompletionContext(SqlCompletionKind.COLUMN, candidates.distinctBy { it.text })
    }

    private fun fallback(metadata: MetadataSnapshot?) = SqlCompletionContext(SqlCompletionKind.FALLBACK,
        metadata?.tableNames.orEmpty().map { SqlNameCandidate(sqlIdentifier(it), it, "테이블") } +
            metadata?.columnNames.orEmpty().map { SqlNameCandidate(sqlIdentifier(it), it, "컬럼") })
}
