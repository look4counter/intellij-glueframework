package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.sql.SqlColumnTrace

class SqlColumnTraceTest : BasePlatformTestCase() {
    fun testOnlyProjectionStagesAppearForAllClauseCarets() {
        val sql = "WITH t AS (\n SELECT id FROM physical\n)\nSELECT a.id\nFROM t a JOIN t b\nON a.id=b.id\nWHERE a.id=1\nGROUP BY a.id\nHAVING a.id>0\nORDER BY a.id"
        for (position in listOf(sql.indexOf("id"), sql.indexOf("a.id") + 2)) {
            val result = SqlColumnTrace.analyze(sql, position + 1, null)
            assertNull(result.message)
            assertEquals(listOf(2, 4), result.entries.map { it.line })
        }
        for (clause in listOf("ON", "WHERE", "GROUP BY", "HAVING", "ORDER BY")) {
            assertEquals(clause, listOf(2, 4), SqlColumnTrace.analyze(sql, sql.indexOf("$clause a.id") + clause.length + 4, null).entries.map { it.line })
        }
        assertTrue(trace("SELECT id FROM physical WHERE status<caret>=1").entries.isEmpty())
    }

    fun testFilterSubqueryAndItsCtesDoNotBecomeOutputStages() {
        val sql = "WITH t AS (\n SELECT id FROM physical\n)\nSELECT t.id FROM t\nWHERE EXISTS (\n WITH q AS (\n SELECT t.id FROM t\n )\n SELECT id FROM q\n)"
        val result = SqlColumnTrace.analyze(sql, sql.indexOf("id") + 1, null)
        assertNull(result.message)
        assertEquals(listOf(2, 4), result.entries.map { it.line })
        assertTrue(SqlColumnTrace.analyze(sql, sql.indexOf("SELECT t.id", sql.indexOf("WHERE")) + 10, null).entries.isEmpty())
        assertTrue(SqlColumnTrace.analyze(sql, sql.lastIndexOf("SELECT id") + 8, null).entries.isEmpty())
    }

    fun testCaseInputsRemainLineageWhileWhereUsageIsExcluded() {
        val sql = "WITH t AS (\n SELECT amount, flag FROM physical\n)\nSELECT CASE WHEN flag=1 THEN amount ELSE 0 END AS total\nFROM t\nWHERE flag=2"
        val result = SqlColumnTrace.analyze(sql, sql.indexOf("total") + 1, null)
        assertNull(result.message)
        assertEquals(listOf(2, 2, 4), result.entries.map { it.line })
    }

    fun testSourceMiddleAndFinalColumnAllShowEntireChain() {
        val sql = "WITH a AS (\n SELECT id FROM physical\n), b AS (\n SELECT id FROM a\n)\nSELECT id FROM b"
        for (position in listOf(sql.indexOf("id"), sql.indexOf("id FROM a"), sql.lastIndexOf("id"))) {
            val result = SqlColumnTrace.analyze(sql, position + 1, null)
            assertNull(result.message)
            assertEquals(listOf(2, 4, 6), result.entries.map { it.line })
        }
    }

    fun testForwardTraceFollowsRenamingAndAllBranches() {
        val sql = "WITH a(renamed) AS (\n SELECT original FROM physical\n), b AS (\n SELECT renamed AS next_name FROM a\n), c AS (\n SELECT renamed FROM a\n)\nSELECT b.next_name, c.renamed FROM b JOIN c ON b.next_name=c.renamed"
        val result = SqlColumnTrace.analyze(sql, sql.indexOf("original") + 1, null)
        assertNull(result.message)
        assertTrue(result.entries.map { it.line }.containsAll(listOf(2, 4, 6, 8)))
        assertEquals(2, result.entries.count { it.line == 8 })
    }

    fun testSharedStarDoesNotJoinDifferentOutputColumnsOrUnrelatedCtes() {
        val sql = "WITH a AS (\n SELECT first_id, other_id FROM physical\n), b AS (\n SELECT * FROM a\n), unrelated AS (\n SELECT first_id FROM other_table\n)\nSELECT first_id, other_id FROM b"
        val result = SqlColumnTrace.analyze(sql, sql.indexOf("first_id") + 2, null)
        assertNull(result.message)
        assertEquals(listOf(2, 4, 8), result.entries.map { it.line })
        assertEquals("first_id", sql.substring(result.entries.last().offset).takeWhile { it.isLetterOrDigit() || it == '_' })
    }

    fun testStarSelectionShowsKnownColumnsThroughEnd() {
        val sql = "WITH a AS (\n SELECT id FROM physical\n), b AS (\n SELECT * FROM a\n)\nSELECT id FROM b"
        for (position in sql.indexOf('*')..sql.indexOf('*') + 1) {
            assertEquals(listOf(2, 4, 6), SqlColumnTrace.analyze(sql, position, null).entries.map { it.line })
        }
    }

    fun testIndependentAliasesOfSamePhysicalTableRemainSeparate() {
        val sql = "WITH a AS (\n SELECT id FROM physical p\n), b AS (\n SELECT id FROM physical p\n)\nSELECT a.id, b.id FROM a JOIN b ON a.id=b.id"
        val result = SqlColumnTrace.analyze(sql, sql.indexOf("id") + 1, null)
        assertNull(result.message)
        assertEquals(listOf(2, 6), result.entries.map { it.line })
        assertTrue(result.entries.drop(1).all { sql.substring(it.offset - 2, it.offset) == "a." })
    }

    fun testOutputAliasTraceExcludesOrderByUsage() {
        val sql = "WITH t AS (\n SELECT original FROM physical\n)\nSELECT original AS renamed FROM t\nORDER BY renamed"
        val result = SqlColumnTrace.analyze(sql, sql.indexOf("original") + 1, null)
        assertNull(result.message)
        assertEquals(listOf(2, 4), result.entries.map { it.line })
        assertEquals(listOf(2, 4), SqlColumnTrace.analyze(sql, sql.lastIndexOf("renamed") + 1, null).entries.map { it.line })
    }

    private fun trace(marked: String) = SqlColumnTrace.analyze(marked.replace("<caret>", ""), marked.indexOf("<caret>"), null)

    fun testCteStarChainKeepsSourceOrderAndRealLines() {
        val sql = "WITH TB_MAST AS (\n    SELECT SS\n      FROM DUAL\n)\n, TB_SUB AS (\n    SELECT *\n      FROM TB_MAST\n)\nSELECT S<caret>S\n  FROM TB_SUB A"
        val result = trace(sql)
        assertNull(result.message)
        assertEquals(listOf("2: SELECT SS", "6: SELECT *", "9: SELECT SS"), result.entries.map { it.toString() })
        assertEquals(listOf("SS", "*", "SS"), result.entries.map { sql.replace("<caret>", "").substring(it.offset).takeWhile { c -> !c.isWhitespace() } })
    }

    fun testDeclaredColumnsAndDerivedAliasesMapByPosition() {
        val sql = "WITH t(renamed) AS (\n SELECT original FROM physical\n)\nSELECT q.renamed<caret> FROM (\n SELECT t.renamed FROM t\n) q"
        assertEquals(listOf(2, 5, 4), trace(sql).entries.map { it.line })
    }

    fun testQualifiedStarAndDeclaredDerivedColumns() {
        val sql = "WITH t AS (\n SELECT original FROM physical\n)\nSELECT renamed<caret> FROM (\n SELECT t.* FROM t\n) q(renamed)"
        assertEquals(listOf(2, 5, 4), trace(sql).entries.map { it.line })
    }

    fun testCaretOnRenamedProjectionFollowsItsExpression() {
        val sql = "WITH t AS (\n SELECT original FROM physical\n)\nSELECT original AS renamed<caret> FROM t"
        val result = trace(sql)
        assertNull(result.message)
        assertEquals(listOf(2, 4), result.entries.map { it.line })
    }

    fun testCursorAtColumnEndBeforeCommaAndInsideCrlfDocument() {
        assertEquals(listOf(2, 4), trace("WITH t AS (\r\n SELECT id FROM physical\r\n)\r\nSELECT id<caret>, 1 FROM t").entries.map { it.line })
    }

    fun testExpressionTracesItsInputsBeforeOutputAlias() {
        val sql = "WITH t AS (\n SELECT amount, fee FROM physical\n)\nSELECT total<caret> FROM (\n SELECT amount + fee AS total FROM t\n) q"
        val result = trace(sql)
        assertNull(result.message)
        assertEquals(listOf(2, 2, 5, 4), result.entries.map { it.line })
    }

    fun testAmbiguousColumnDoesNotInventSource() {
        val result = trace("WITH a AS (SELECT id FROM aa), b AS (SELECT id FROM bb) SELECT i<caret>d FROM a JOIN b ON a.id=b.id")
        assertNotNull(result.message)
        assertEquals(1, result.entries.size)
    }

    fun testRecursiveQueryStopsAndExplainsPartialPath() {
        val result = trace("WITH RECURSIVE t(id) AS (SELECT id FROM t) SELECT i<caret>d FROM t")
        assertNotNull(result.message)
        assertTrue(result.entries.size <= 2)
    }

    fun testStatementsCommentsAndStringsStayIsolated() {
        assertEquals(listOf(2), trace("WITH t AS (SELECT id FROM x) SELECT id FROM t;\nSELECT i<caret>d FROM y").entries.map { it.line })
        assertTrue(trace("SELECT 'a<caret>b' FROM t").entries.isEmpty())
        assertTrue(trace("SELECT id -- i<caret>d\nFROM t").entries.isEmpty())
        assertTrue(trace("SELECT <caret> FROM t").entries.isEmpty())
    }

    fun testUnionBranchesKeepCorrespondingOutputPosition() {
        assertEquals(listOf(2, 4, 6), trace("WITH t AS (\n SELECT first_id AS id FROM a\n UNION ALL\n SELECT second_id FROM b\n)\nSELECT i<caret>d FROM t").entries.map { it.line })
    }

    fun testLengthLimitDoesNotReturnStalePath() {
        val result = trace("SELECT i<caret>d FROM t " + " ".repeat(262_144))
        assertTrue(result.entries.isEmpty())
        assertNotNull(result.message)
    }
}
