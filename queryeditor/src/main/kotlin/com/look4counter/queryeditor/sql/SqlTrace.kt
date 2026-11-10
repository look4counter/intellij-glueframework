package com.look4counter.queryeditor.sql

import com.look4counter.queryeditor.database.MetadataSnapshot
import com.intellij.openapi.util.TextRange

data class SqlSourceLink(val range: TextRange, val sources: List<SqlTraceNode>)

object SqlTrace {
    fun analyze(text: String, offset: Int, metadata: MetadataSnapshot?): SqlTraceResult =
        SqlTableTrace.analyze(text, offset) ?: SqlColumnTrace.analyze(text, offset, metadata)

    fun predecessors(text: String, offset: Int, metadata: MetadataSnapshot?): List<SqlTraceNode> =
        sourceLink(text, offset, metadata)?.sources.orEmpty()

    fun sourceLink(text: String, offset: Int, metadata: MetadataSnapshot?): SqlSourceLink? {
        if (text.length > SqlQueryParser.MAX_LENGTH || offset !in text.indices) return null
        val lexer = SqlLexer()
        lexer.start(text)
        var steps = 0
        while (lexer.tokenType != null && offset >= lexer.tokenEnd) {
            if (++steps % 256 == 0) com.intellij.openapi.progress.ProgressManager.checkCanceled()
            lexer.advance()
        }
        if (lexer.tokenType == null || lexer.tokenType == com.intellij.psi.TokenType.WHITE_SPACE ||
            lexer.tokenType == SqlTokens.COMMENT || lexer.tokenType == SqlTokens.SYMBOL) return null
        val table = SqlTableTrace.analyze(text, offset)
        val result = table ?: SqlColumnTrace.analyze(text, offset, metadata)
        val selected = result.graph.nodes.firstOrNull { it.id == result.graph.selectedNodeId } ?: return null
        val sources = if (table != null && selected.entry.offset !in lexer.tokenStart until lexer.tokenEnd) listOf(selected)
        else result.graph.nodes.filter { selected.id in it.children && it.entry.offset != selected.entry.offset }
            .distinctBy { it.entry.offset }.sortedBy { it.entry.offset }
        return sources.takeIf { it.isNotEmpty() }?.let { SqlSourceLink(TextRange(lexer.tokenStart, lexer.tokenEnd), it) }
    }
}
