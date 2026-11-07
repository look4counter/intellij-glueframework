package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.sql.SqlTrace
import com.look4counter.queryeditor.sql.SqlTraceResult

class SqlTableTraceTest : BasePlatformTestCase() {
    private fun trace(marked: String): SqlTraceResult =
        SqlTrace.analyze(marked.replace("<caret>", ""), marked.indexOf("<caret>"), null)

    fun testEveryStageOfCteChainShowsAllTableStages() {
        val sql = "WITH a AS (\n SELECT * FROM physical\n), b AS (\n SELECT * FROM a\n)\nSELECT b.id\nFROM b"
        for (position in listOf(sql.indexOf("a AS"), sql.indexOf("physical"), sql.indexOf("b AS"),
            sql.indexOf("FROM a") + 5, sql.indexOf("b.id"), sql.lastIndexOf('b'))) {
            val result = SqlTrace.analyze(sql, position + 1, null)
            assertNull(result.message)
            assertEquals(listOf(2, 1, 4, 3, 7), result.entries.map { it.line })
            assertEquals(listOf("physical", "a", "a", "b", "b"), result.entries.map { entry ->
                sql.substring(entry.offset).takeWhile { it.isLetter() }
            })
        }
    }

    fun testAllConsumersAppearWithoutPullingInOtherJoinInputs() {
        val sql = "WITH a AS (\n SELECT * FROM origin\n), b AS (\n SELECT * FROM a\n JOIN unrelated ON 1=1\n), c AS (\n SELECT * FROM a\n)\nSELECT *\nFROM b\nJOIN c ON 1=1\nJOIN final_other ON 1=1"
        val result = SqlTrace.analyze(sql, sql.indexOf("origin") + 1, null)
        assertEquals(setOf(1, 2, 3, 4, 6, 7, 10, 11), result.entries.map { it.line }.toSet())
        assertTrue(result.entries.none { it.text.contains("unrelated") || it.text.contains("final_other") })
        val joined = SqlTrace.analyze(sql, sql.indexOf("b AS") + 1, null)
        assertTrue(joined.entries.any { it.line == 5 })
        assertTrue(joined.entries.none { it.line == 12 })
    }

    fun testDerivedAliasAndQualifierTraceInnerTables() {
        val sql = "SELECT q.id\nFROM (\n SELECT id\n FROM physical\n) q"
        for (position in listOf(sql.indexOf("q.id"), sql.lastIndexOf('q'))) {
            val result = SqlTrace.analyze(sql, position + 1, null)
            assertEquals(listOf(4, 5), result.entries.map { it.line })
            assertEquals(sql.lastIndexOf('q'), result.entries.last().offset)
        }
        assertEquals(listOf(3, 1), SqlTrace.analyze(sql, sql.indexOf("q.id") + 3, null).entries.map { it.line })
    }

    fun testUnionBranchesAreBothOriginsOfCte() {
        val result = trace("WITH t AS (\n SELECT id FROM left_table\n UNION ALL\n SELECT id FROM right_table\n)\nSELECT * FROM t<caret>")
        assertEquals(listOf(2, 4, 1, 6), result.entries.map { it.line })
    }

    fun testIndependentPhysicalInstancesAndSelfJoinAliasesStaySeparate() {
        val sql = "WITH a AS (\n SELECT id FROM physical p\n), b AS (\n SELECT id FROM physical p\n)\nSELECT a.id, b.id\nFROM a\nJOIN b ON a.id=b.id"
        assertEquals(listOf(2, 1, 7), SqlTrace.analyze(sql, sql.indexOf("physical") + 1, null).entries.map { it.line })
        assertEquals(listOf(4, 3, 8), SqlTrace.analyze(sql, sql.lastIndexOf("physical") + 1, null).entries.map { it.line })
        val selfJoin = "SELECT l.id, r.id\nFROM employee l\nJOIN employee r ON l.id=r.id"
        assertEquals(listOf(2), SqlTrace.analyze(selfJoin, selfJoin.indexOf("l.id") + 1, null).entries.map { it.line })
        assertEquals(listOf(3), SqlTrace.analyze(selfJoin, selfJoin.indexOf("r.id") + 1, null).entries.map { it.line })
    }

    fun testSchemaQuotedNamesAndColumnCaretsAreDistinct() {
        val sql = "SELECT x.\"Id\"\nFROM \"App\".\"Employee\" AS x"
        for (position in listOf(sql.indexOf("App"), sql.indexOf("Employee"), sql.lastIndexOf('x'), sql.indexOf("x."))) {
            assertEquals(listOf(2), SqlTrace.analyze(sql, position + 1, null).entries.map { it.line })
        }
        val columnCaret = sql.indexOf("Id") + 1
        assertEquals(com.look4counter.queryeditor.sql.SqlColumnTrace.analyze(sql, columnCaret, null),
            SqlTrace.analyze(sql, columnCaret, null))
        assertEquals(listOf(2), trace("SELECT \"FROM\".id\nFROM employee \"FROM<caret>\"").entries.map { it.line })
    }

    fun testNestedCteShadowingDoesNotJoinOuterDefinition() {
        val sql = "WITH t AS (\n SELECT id FROM outer_table\n)\nSELECT q.id\nFROM (\n WITH t AS (\n SELECT id FROM inner_table\n )\n SELECT id FROM t\n) q\nJOIN t ON 1=1"
        assertEquals(listOf(7, 6, 9, 10), SqlTrace.analyze(sql, sql.indexOf("inner_table") + 1, null).entries.map { it.line })
        assertEquals(listOf(2, 1, 11), SqlTrace.analyze(sql, sql.indexOf("outer_table") + 1, null).entries.map { it.line })
    }

    fun testStatementsCommentsStringsAndKeywordsAreNotTableSelections() {
        assertEquals(listOf(2), trace("WITH a AS (SELECT * FROM t) SELECT * FROM a;\nSELECT * FROM t<caret>").entries.map { it.line })
        for (sql in listOf("SELECT '<caret>employee' FROM employee", "SELECT * -- empl<caret>oyee\nFROM employee",
            "SEL<caret>ECT * FROM employee", "SELECT * FROM employee WHERE <caret> ")) {
            assertTrue(sql, trace(sql).entries.isEmpty())
        }
        assertEquals(listOf(2), trace("SELECT * FROM (SELECT * FROM hidden) q;\nSELECT * FROM other<caret>").entries.map { it.line })
    }

    fun testSameLineIsDeduplicatedAndCrlfOffsetsAreOriginal() {
        val sql = "WITH a AS (SELECT * FROM base)\r\nSELECT * FROM a x JOIN a y ON 1=1"
        val result = SqlTrace.analyze(sql, sql.indexOf("base") + 2, null)
        assertEquals(listOf(1, 2), result.entries.map { it.line })
        assertEquals(sql.indexOf("base"), result.entries.first().offset)
        assertTrue(result.entries.all { it.offset in sql.indices && !it.text.endsWith('\r') })
    }

    fun testRecursiveAndIncompleteQueriesRemainBounded() {
        val recursive = trace("WITH RECURSIVE t AS (\n SELECT * FROM t\n)\nSELECT * FROM t<caret>")
        assertEquals(setOf(1, 2, 4), recursive.entries.map { it.line }.toSet())
        assertNotNull(recursive.message)
        assertEquals(listOf(3, 4), trace("SELECT q.id\nFROM (\n SELECT * FROM employee\n) q<caret> JOIN ").entries.map { it.line })
        val limited = trace("SELECT * FROM t<caret> " + " ".repeat(262_144))
        assertTrue(limited.entries.isEmpty())
        assertNotNull(limited.message)
    }

    fun testAmbiguousQualifierIsNotAssignedArbitrarily() {
        assertTrue(trace("SELECT a<caret>.id\nFROM first_table a\nJOIN second_table a ON 1=1").entries.isEmpty())
    }

    fun testDmlTableReferencesDoNotInventWriteLineage() {
        assertEquals(listOf(1), trace("UPDATE employee<caret> SET id=1").entries.map { it.line })
        assertEquals(listOf(1), trace("DELETE FROM employee<caret> WHERE id=1").entries.map { it.line })
        val sql = "INSERT INTO target<caret> (id)\nSELECT id FROM origin"
        assertEquals(listOf(1), trace(sql).entries.map { it.line })
    }

    fun testTransformationKeywordsAreNotTableAliasesButQuotedNamesAre() {
        for (keyword in listOf("PIVOT", "UNPIVOT", "MODEL", "MATCH_RECOGNIZE")) {
            assertTrue(keyword, trace("SELECT *\nFROM employee\n$keyword<caret> (x)").entries.isEmpty())
            assertEquals(listOf(2), trace("SELECT *\nFROM employee \"$keyword<caret>\"").entries.map { it.line })
        }
    }
}
