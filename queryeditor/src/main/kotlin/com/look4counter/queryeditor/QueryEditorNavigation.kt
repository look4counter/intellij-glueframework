package com.look4counter.queryeditor

interface QueryEditorNavigation {
    /** 안전한 문맥에서는 선택 결과를, 지연 실행이 필요하면 요청 수락 여부를 반환한다. */
    fun selectQuery(id: String): Boolean
    /** 안전한 문맥에서는 선택 결과를, 지연 실행이 필요하면 요청 수락 여부를 반환한다. */
    fun selectQueryAtOffset(offset: Int): Boolean
}
