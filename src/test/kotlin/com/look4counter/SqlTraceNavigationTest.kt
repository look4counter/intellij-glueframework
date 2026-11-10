package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.sql.SqlTrace

class SqlTraceNavigationTest : BasePlatformTestCase() {
    fun testColumnNavigatesOneCteStageAtATime() {
        val sql = "WITH a AS (SELECT id FROM physical), b AS (SELECT id FROM a) SELECT id FROM b"
        assertEquals(listOf(sql.indexOf("id FROM a")), SqlTrace.predecessors(sql, sql.lastIndexOf("id") + 1, null).map { it.entry.offset })
        assertEquals(listOf(sql.indexOf("id FROM physical")), SqlTrace.predecessors(sql, sql.indexOf("id FROM a") + 1, null).map { it.entry.offset })
    }

    fun testTableQualifierGoesToDeclarationThenCteDefinition() {
        val sql = "WITH t AS (SELECT id FROM physical) SELECT a.id FROM t a"
        assertEquals(listOf(sql.lastIndexOf("t a")), SqlTrace.predecessors(sql, sql.indexOf("a.id"), null).map { it.entry.offset })
        assertEquals(listOf(sql.indexOf("t AS")), SqlTrace.predecessors(sql, sql.lastIndexOf("t a"), null).map { it.entry.offset })
        assertTrue(SqlTrace.predecessors(sql, sql.indexOf("t AS"), null).isEmpty())
        assertTrue(SqlTrace.predecessors(sql, sql.indexOf("physical"), null).isEmpty())
    }

    fun testMultipleDirectInputsAreNotReducedToAnArbitrarySource() {
        val sql = "WITH a AS (SELECT x FROM first_table), b AS (SELECT x FROM second_table) SELECT a.x+b.x AS x FROM a JOIN b ON 1=1"
        assertEquals(listOf(sql.indexOf("x FROM first_table"), sql.indexOf("x FROM second_table")),
            SqlTrace.predecessors(sql, sql.indexOf("x FROM a JOIN"), null).map { it.entry.offset })
    }

    fun testRenamedColumnStopsTraceAndDoesNotOfferALink() {
        val sql = "WITH a AS (SELECT id AS renamed FROM physical) SELECT renamed FROM a"
        assertEquals(listOf(sql.indexOf("renamed")), SqlTrace.predecessors(sql, sql.lastIndexOf("renamed"), null).map { it.entry.offset })
        assertTrue(SqlTrace.predecessors(sql, sql.indexOf("renamed"), null).isEmpty())
        assertNull(SqlTrace.sourceLink(sql, sql.indexOf("renamed"), null))
        val link = SqlTrace.sourceLink(sql, sql.lastIndexOf("renamed") + 2, null)!!
        assertEquals("renamed", sql.substring(link.range.startOffset, link.range.endOffset))
    }

    fun testOccurrencesIncludeMatchingConditionsAndExcludeOtherSourcesAndNames() {
        val sql = "WITH a AS (SELECT id FROM first_table), b AS (SELECT id FROM second_table) SELECT a.id FROM a JOIN b ON a.id=b.id WHERE a.id=1 AND b.id=2; SELECT id FROM other_table"
        val result = SqlTrace.analyze(sql, sql.indexOf("SELECT a.id") + 10, null)
        val actual = result.occurrences.map { it.startOffset }.toSet()
        assertEquals(setOf(sql.indexOf("id FROM first_table"), sql.indexOf("a.id") + 2,
            sql.indexOf("ON a.id") + 5, sql.indexOf("WHERE a.id") + 8), actual)
        assertTrue(result.occurrences.all { sql.substring(it.startOffset, it.endOffset) == "id" })
        assertEquals(listOf(sql.indexOf("first_table")), SqlTrace.analyze(sql, sql.indexOf("first_table"), null).occurrences.map { it.startOffset })
    }

    fun testInvalidLocationsAndUnrelatedStatementsHaveNoPredecessor() {
        val sql = "SELECT 'literal', missing FROM physical; -- missing\nSELECT 1"
        for (offset in listOf(sql.indexOf("--") + 4, sql.lastIndexOf('1'), sql.indexOf("SELECT"), sql.indexOf(';'))) {
            assertTrue("offset $offset", SqlTrace.predecessors(sql, offset, null).isEmpty())
        }
    }

    fun testQuotedTableAndSelectValueKeepExistingTraceMeaning() {
        val sql = "WITH \"T\" AS (SELECT 'value' AS \"C\" FROM \"Physical\") SELECT a.\"C\" FROM \"T\" a"
        assertEquals(listOf(sql.indexOf("\"C\"")), SqlTrace.predecessors(sql, sql.lastIndexOf("\"C\""), null).map { it.entry.offset })
        assertEquals(listOf(sql.lastIndexOf("\"T\"")), SqlTrace.predecessors(sql, sql.indexOf("a.\"C\""), null).map { it.entry.offset })
        assertTrue(SqlTrace.predecessors(sql, sql.indexOf("'value'"), null).isEmpty())
    }
}
