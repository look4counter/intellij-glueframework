package com.look4counter.queryeditor.sql

import com.look4counter.queryeditor.database.MetadataSnapshot

object SqlTrace {
    fun analyze(text: String, offset: Int, metadata: MetadataSnapshot?): SqlTraceResult =
        SqlTableTrace.analyze(text, offset) ?: SqlColumnTrace.analyze(text, offset, metadata)
}
