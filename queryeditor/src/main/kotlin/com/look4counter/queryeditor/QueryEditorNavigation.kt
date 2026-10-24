package com.look4counter.queryeditor

interface QueryEditorNavigation {
    fun selectQuery(id: String): Boolean
    fun selectQueryAtOffset(offset: Int): Boolean
}
