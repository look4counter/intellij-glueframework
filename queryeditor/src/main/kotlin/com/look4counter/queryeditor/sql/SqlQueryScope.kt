package com.look4counter.queryeditor.sql

import com.look4counter.queryeditor.database.MetadataSnapshot
import java.util.Collections
import java.util.IdentityHashMap

internal fun sqlIdentifier(name: String): String =
    if (name.isNotEmpty() && !name.first().isDigit() && name.all(SqlLexer::isWord)) name
    else "\"${name.replace("\"", "\"\"")}\""

internal fun sqlPath(tokens: List<SqlToken>): List<SqlIdentifier>? {
    if (tokens.isEmpty() || tokens.size % 2 == 0) return null
    val names = mutableListOf<SqlIdentifier>()
    tokens.forEachIndexed { index, token ->
        if (index % 2 == 0) names.add(token.identifier ?: return null)
        else if (!token.symbol(".")) return null
    }
    return names
}

/** Name resolution is confined to one completion request; no JDBC or document writes. */
internal class SqlQueryScope(private val metadata: MetadataSnapshot?) {
    private val cache = IdentityHashMap<Any, List<String>>()
    private val visiting = Collections.newSetFromMap(IdentityHashMap<Any, Boolean>())

    private fun resolve(key: Any, action: () -> List<String>): List<String> {
        cache[key]?.let { return it }
        if (key in visiting) return emptyList()
        if (visiting.size >= 48) throw SqlAnalysisLimit()
        visiting.add(key)
        try {
            return action().distinct().also { cache[key] = it }
        } finally { visiting.remove(key) }
    }

    fun columns(source: SqlSource): List<String> = resolve(source) {
        val actual = when {
            source.cte != null -> cteColumns(source.cte)
            source.query != null -> output(source.query)
            source.callable -> emptyList()
            else -> metadata?.tables.orEmpty().filter { table ->
                val actual = listOfNotNull(table.catalog, table.schema, table.name)
                source.path.isNotEmpty() && actual.size >= source.path.size &&
                    actual.takeLast(source.path.size).zip(source.path).all { (value, name) -> name.matches(SqlIdentifier(value, name.quoted)) }
            }.flatMap { it.columns }
        }
        source.columnNames?.let { declared -> declared.map { it.value } + actual.drop(declared.size) } ?: actual
    }

    private fun cteColumns(cte: SqlCte): List<String> = resolve(cte) {
        cte.columnNames?.map { it.value } ?: cte.query?.let(::output).orEmpty()
    }

    private fun output(query: SqlQuery): List<String> = resolve(query) {
        query.scopes.firstOrNull()?.let(::output).orEmpty()
    }

    fun output(scope: SqlScope): List<String> = resolve(scope) {
        scope.projections.flatMap { item ->
            val top = topTokens(item)
            val asIndex = top.indexOfLast { it.keyword("AS") }
            when {
                asIndex >= 0 && asIndex + 2 == top.size -> listOfNotNull(top.last().identifier?.value)
                item.size == 1 && item.single().symbol("*") -> scope.sources.flatMap(::columns)
                item.size >= 3 && item.last().symbol("*") && item[item.lastIndex - 1].symbol(".") ->
                    sqlPath(item.dropLast(2))?.let { sources(scope, it).flatMap(::columns) }.orEmpty()
                sqlPath(item) != null -> listOf(sqlPath(item)!!.last().value)
                implicitAlias(top) != null -> listOf(implicitAlias(top)!!)
                else -> emptyList()
            }
        }
    }

    private fun implicitAlias(tokens: List<SqlToken>): String? {
        val last = tokens.lastOrNull()?.identifier ?: return null
        if (tokens.size < 2) return null
        val previous = tokens[tokens.lastIndex - 1]
        if (previous.symbol(".")) return null
        return if (previous.identifier != null || previous.symbol(")") ||
            previous.text.firstOrNull()?.let { it.isDigit() || it == '\'' } == true) last.value else null
    }

    private fun topTokens(tokens: List<SqlToken>): List<SqlToken> {
        var depth = 0
        return tokens.filter { token ->
            if (token.symbol(")")) depth--
            val include = depth == 0
            if (token.symbol("(")) depth++
            include
        }
    }

    fun sources(scope: SqlScope, qualifier: List<SqlIdentifier>): List<SqlSource> {
        val local = scope.sources.filter { it.matches(qualifier) }
        return local.ifEmpty { scope.outer?.let { sources(it, qualifier) }.orEmpty() }
    }

    fun visible(scope: SqlScope): List<SqlSource> {
        val outer = scope.outer?.let(::visible).orEmpty()
        return scope.sources + outer.filter { inherited ->
            val name = inherited.alias ?: inherited.path.lastOrNull()
            name == null || scope.sources.none { it.matches(listOf(name)) }
        }
    }
}
