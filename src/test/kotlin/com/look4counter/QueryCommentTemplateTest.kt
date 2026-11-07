package com.look4counter

import com.look4counter.queryeditor.QueryCommentTemplate
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime

class QueryCommentTemplateTest {
    private val now = LocalDateTime.of(2026, 11, 8, 13, 24, 56)

    @Test fun replacesBothVariableStylesAndDateTimeInOnePass() {
        val template = "/* ${'$'}{QUERY_ID} / #{QUERY_DESCRIPTION} / #{QUERY_ID} / ${'$'}{QUERY_DESCRIPTION}\n" +
            "Created by : 작성자\n${'$'}{yyyy-MM-dd} #{yyyy-MM-dd HH:mm:ss} */"
        assertEquals("/* find / 설명 / find / 설명\nCreated by : 작성자\n2026-11-08 2026-11-08 13:24:56 */\nSELECT 1",
            QueryCommentTemplate.prepend(template, "SELECT 1", "find", "설명", now))
        assertEquals("${'$'}{yyyy-MM-dd}\nSELECT 1",
            QueryCommentTemplate.prepend("${'$'}{QUERY_ID}", "SELECT 1", "${'$'}{yyyy-MM-dd}", "", now))
    }

    @Test fun preservesUnknownVariablesInvalidPatternsAndSql() {
        val template = "/* ${'$'}{UNKNOWN} ${'$'}{MMMMMM} ${'$'}{yyyy-bad} */\r\n"
        val sql = "\n/* original */\nSELECT '<&]]>'"
        assertEquals(template.replace("\r\n", "\n") + sql,
            QueryCommentTemplate.prepend(template, sql, "id", "", now))
        assertEquals(sql, QueryCommentTemplate.prepend(" \r\n", sql, "id", "", now))
        assertEquals("/*  */\n", QueryCommentTemplate.prepend("/* #{QUERY_DESCRIPTION} */", "", "id", "", now))
    }

    @Test fun replacesResultTypeWithoutReinterpretingItsValue() {
        val template = "/* ${'$'}{QUERY_RESULT_TYPE} / #{QUERY_RESULT_TYPE} */"
        assertEquals("/* sample.Result / sample.Result */\nSELECT 1",
            QueryCommentTemplate.prepend(template, "SELECT 1", "id", "", now, "sample.Result"))
        assertEquals("/*  /  */\nSELECT 1",
            QueryCommentTemplate.prepend(template, "SELECT 1", "id", "", now))
        assertEquals("/* ${'$'}{QUERY_ID} / ${'$'}{QUERY_ID} */\nSELECT 1",
            QueryCommentTemplate.prepend(template, "SELECT 1", "id", "", now, "${'$'}{QUERY_ID}"))
    }
}
