package com.look4counter

import com.look4counter.modeleditor.ActivityProperty
import com.look4counter.modeleditor.ModelServiceOutputOrder
import junit.framework.TestCase

class ModelServiceOutputOrderTest : TestCase() {
    fun testUserExamplePropertyOrderForEveryActivity() {
        val cases = listOf(
            "class description sql-key service-name" to "service-name sql-key",
            "class chk-name dao sql-key param-count param0 param1 param2 param3 result-key description" to
                "sql-key param-count chk-name result-key param0 param1 dao param2 param3",
            "class chk-name dao sql-key param-count param0 description" to "sql-key dao chk-name param-count param0",
            "class chk-name dao param-count result-key sql-key param0 param1 param2 description" to
                "param-count chk-name param0 param1 result-key param2 dao sql-key",
            "class dao param-count result-key sql-key param0 description" to "result-key sql-key dao param-count param0",
            "class description" to "",
        )
        for ((source, expected) in cases) {
            val properties = source.split(' ').map { ActivityProperty(it, "value:$it") }
            val result = ModelServiceOutputOrder.properties(properties)
            assertEquals(source, expected, result.joinToString(" ") { it.key })
            assertTrue(result.all { it.value == "value:${it.key}" })
            assertEquals(result, ModelServiceOutputOrder.properties(properties))
        }
    }

    fun testRouterTransitionsMergeDefaultSuccessAndMatchUserOrder() {
        val source = listOf("success" to "end", "find" to "Search[조회]", "delete" to "삭제",
            "insert" to "등록", "update2" to "수정", "custom" to "SeachEmp-SQL2", "" to "SeachEmp-SQL2")
        val expected = listOf("success" to "SeachEmp-SQL2", "find" to "Search[조회]", "custom" to "SeachEmp-SQL2",
            "insert" to "등록", "delete" to "삭제", "update2" to "수정")
        assertEquals(expected, ModelServiceOutputOrder.transitions(source))
        assertEquals(expected, ModelServiceOutputOrder.transitions(source))
        assertEquals(listOf("success" to "Router"), ModelServiceOutputOrder.transitions(listOf("" to "Router")))
    }

    fun testPropertyBucketCollisionsKeepSourceOrder() {
        val properties = listOf(ActivityProperty("class", "type"), ActivityProperty("dao", "test-dao"),
            ActivityProperty("param-count", "1"), ActivityProperty("description", ""), ActivityProperty("param0", "deptno"))
        val result = ModelServiceOutputOrder.properties(properties)
        assertTrue(result.indexOf(properties[1]) < result.indexOf(properties[2]))
        assertEquals(emptyList<ActivityProperty>(), ModelServiceOutputOrder.properties(emptyList()))
        assertEquals(emptyList<Pair<String, String>>(), ModelServiceOutputOrder.transitions(emptyList()))
    }
}
