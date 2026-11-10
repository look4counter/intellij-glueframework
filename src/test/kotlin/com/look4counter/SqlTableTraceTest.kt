package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.sql.SqlTrace
import com.look4counter.queryeditor.sql.SqlTraceResult

class SqlTableTraceTest : BasePlatformTestCase() {
    fun testTableOccurrencesIncludeBoundAliasesAndStopAtDifferentCteNames() {
        val sql = "WITH t AS (SELECT e.id FROM employee e), u AS (SELECT id FROM t) SELECT a.id FROM t a JOIN t b ON a.id=b.id"
        val selected = SqlTrace.analyze(sql, sql.indexOf("a.id"), null)
        val expected = setOf(sql.indexOf("t AS"), sql.indexOf("FROM t") + 5, sql.indexOf("FROM t a") + 5,
            sql.indexOf("t a JOIN") + 2, sql.indexOf("JOIN t") + 5, sql.indexOf("t b ON") + 2,
            sql.indexOf("a.id"), sql.lastIndexOf("a.id"), sql.indexOf("b.id"))
        assertEquals(expected, selected.occurrences.map { it.startOffset }.toSet())
        assertEquals(setOf(sql.indexOf("t AS"), sql.indexOf("FROM t") + 5, sql.indexOf("FROM t a") + 5, sql.indexOf("JOIN t") + 5),
            selected.graph.nodes.map { it.entry.offset }.toSet())
        assertEquals(listOf(sql.indexOf("t AS")), SqlTrace.predecessors(sql, sql.indexOf("FROM t a") + 5, null).map { it.entry.offset })
        assertTrue(SqlTrace.predecessors(sql, sql.indexOf("t AS"), null).isEmpty())
        assertFalse(selected.occurrences.any { it.startOffset == sql.indexOf("employee") || it.startOffset == sql.indexOf("u AS") })
    }

    fun testPhysicalTableOccurrencesStayWithinTheSelectedSelfJoinBindingAndStatement() {
        val sql = "SELECT e.id,r.id FROM employee e JOIN employee r ON e.id=r.id; SELECT e.id FROM employee e"
        val expected = setOf(sql.indexOf("employee"), sql.indexOf("employee e") + 9, sql.indexOf("e.id"), sql.indexOf("ON e.id") + 3)
        for (offset in listOf(sql.indexOf("employee"), sql.indexOf("e.id"), sql.indexOf("employee e") + 9)) {
            val result = SqlTrace.analyze(sql, offset, null)
            assertEquals(expected, result.occurrences.map { it.startOffset }.toSet())
            assertEquals(1, result.graph.nodes.size)
            assertTrue(result.occurrences.all { sql.substring(it.startOffset, it.endOffset) in setOf("employee", "e") })
        }
    }

    fun testQuotedAndQualifiedTableOccurrencesDoNotIncludeColumnNames() {
        val sql = "SELECT a.\"Id\" FROM \"App\".\"Employee\" a JOIN \"Other\".\"Employee\" b ON a.\"Id\"=b.\"Id\""
        val result = SqlTrace.analyze(sql, sql.indexOf("Employee"), null)
        assertEquals(setOf(sql.indexOf("a."), sql.indexOf("\"App\""), sql.indexOf("\"Employee\""),
            sql.indexOf("\"Employee\" a") + "\"Employee\" ".length, sql.indexOf("ON a.") + 3),
            result.occurrences.map { it.startOffset }.toSet())
        assertTrue(result.occurrences.none { sql.substring(it.startOffset, it.endOffset).contains("Id") })
        val quoted = "WITH \"T\" AS (SELECT * FROM physical) SELECT \"t\".id FROM \"T\" \"t\""
        assertEquals(4, SqlTrace.analyze(quoted, quoted.indexOf("\"T\""), null).occurrences.size)
    }

    fun testGraphPreservesSameCteBranchesAndStopsBeforeRenamedConsumers() {
        val sql = "WITH a AS (SELECT * FROM origin), b AS (SELECT * FROM a), " +
            "c AS (SELECT * FROM a) SELECT * FROM b JOIN c ON 1=1 JOIN unrelated ON 1=1"
        val graph = SqlTrace.analyze(sql, sql.indexOf("a AS") + 1, null).graph
        val edges = graph.nodes.flatMap { node -> node.children.map { node.entry.offset to graph.nodes[it].entry.offset } }.toSet()
        val a = sql.indexOf("a AS")
        val firstA = sql.indexOf("FROM a") + 5
        val secondA = sql.lastIndexOf("FROM a") + 5
        assertEquals(setOf(a to firstA, a to secondA), edges)
        assertEquals(3, graph.nodes.size)
        assertTrue(graph.nodes.none { it.name.contains("unrelated") })
        assertTrue(graph.nodes.all { it.entry.line == 1 })
    }

    fun testSelectionAnchorKeepsReferenceAndQualifierSeparateFromCteDefinition() {
        val sql = "WITH t AS (\n SELECT id FROM physical\n)\nSELECT a.id\nFROM t a"
        for ((caret, anchor) in listOf(sql.indexOf("physical") to sql.indexOf("physical"),
            sql.indexOf("t AS") to sql.indexOf("t AS"),
            sql.indexOf("a.id") to sql.lastIndexOf("t a"),
            sql.lastIndexOf('a') to sql.lastIndexOf("t a"))) {
            val result = SqlTrace.analyze(sql, caret + 1, null)
            assertEquals(anchor, result.selectedOffset)
            assertEquals(if (anchor == sql.indexOf("physical")) listOf(2) else listOf(1, 5), result.entries.map { it.line })
        }
    }

    private fun trace(marked: String): SqlTraceResult =
        SqlTrace.analyze(marked.replace("<caret>", ""), marked.indexOf("<caret>"), null)

    fun testCteChainOnlyShowsTheSelectedTableNameSegment() {
        val sql = "WITH a AS (\n SELECT * FROM physical\n), b AS (\n SELECT * FROM a\n)\nSELECT b.id\nFROM b"
        for ((position, lines) in listOf(sql.indexOf("a AS") to listOf(1, 4), sql.indexOf("physical") to listOf(2),
            sql.indexOf("b AS") to listOf(3, 7), (sql.indexOf("FROM a") + 5) to listOf(1, 4),
            sql.indexOf("b.id") to listOf(3, 7), sql.lastIndexOf('b') to listOf(3, 7))) {
            val result = SqlTrace.analyze(sql, position + 1, null)
            assertNull(result.message)
            assertEquals(lines, result.entries.map { it.line })
            assertEquals(1, result.entries.map { entry ->
                sql.substring(entry.offset).takeWhile { it.isLetter() }
            }.toSet().size)
        }
    }

    fun testAllConsumersAppearWithoutPullingInOtherJoinInputs() {
        val sql = "WITH a AS (\n SELECT * FROM origin\n), b AS (\n SELECT * FROM a\n JOIN unrelated ON 1=1\n), c AS (\n SELECT * FROM a\n)\nSELECT *\nFROM b\nJOIN c ON 1=1\nJOIN final_other ON 1=1"
        val result = SqlTrace.analyze(sql, sql.indexOf("a AS") + 1, null)
        assertEquals(setOf(1, 4, 7), result.entries.map { it.line }.toSet())
        assertTrue(result.entries.none { it.text.contains("unrelated") || it.text.contains("final_other") })
        val joined = SqlTrace.analyze(sql, sql.indexOf("b AS") + 1, null)
        assertEquals(listOf(3, 10), joined.entries.map { it.line })
    }

    fun testDerivedAliasAndQualifierStopBeforeDifferentlyNamedInnerTables() {
        val sql = "SELECT q.id\nFROM (\n SELECT id\n FROM physical\n) q"
        for (position in listOf(sql.indexOf("q.id"), sql.lastIndexOf('q'))) {
            val result = SqlTrace.analyze(sql, position + 1, null)
            assertEquals(listOf(5), result.entries.map { it.line })
            assertEquals(sql.lastIndexOf('q'), result.entries.last().offset)
        }
        assertEquals(listOf(3, 1), SqlTrace.analyze(sql, sql.indexOf("q.id") + 3, null).entries.map { it.line })
    }

    fun testUnionInputTablesWithDifferentNamesDoNotBecomeCteOrigins() {
        val result = trace("WITH t AS (\n SELECT id FROM left_table\n UNION ALL\n SELECT id FROM right_table\n)\nSELECT * FROM t<caret>")
        assertEquals(listOf(1, 6), result.entries.map { it.line })
    }

    fun testIndependentPhysicalInstancesAndSelfJoinAliasesStaySeparate() {
        val sql = "WITH a AS (\n SELECT id FROM physical p\n), b AS (\n SELECT id FROM physical p\n)\nSELECT a.id, b.id\nFROM a\nJOIN b ON a.id=b.id"
        assertEquals(listOf(2), SqlTrace.analyze(sql, sql.indexOf("physical") + 1, null).entries.map { it.line })
        assertEquals(listOf(4), SqlTrace.analyze(sql, sql.lastIndexOf("physical") + 1, null).entries.map { it.line })
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
        assertEquals(listOf(7), SqlTrace.analyze(sql, sql.indexOf("inner_table") + 1, null).entries.map { it.line })
        assertEquals(listOf(2), SqlTrace.analyze(sql, sql.indexOf("outer_table") + 1, null).entries.map { it.line })
        assertEquals(listOf(6, 9), SqlTrace.analyze(sql, sql.lastIndexOf("t AS") + 1, null).entries.map { it.line })
        assertEquals(listOf(1, 11), SqlTrace.analyze(sql, sql.indexOf("t AS") + 1, null).entries.map { it.line })
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
        val result = SqlTrace.analyze(sql, sql.indexOf("a AS") + 1, null)
        assertEquals(listOf(1, 2), result.entries.map { it.line })
        assertEquals(sql.indexOf("a AS"), result.entries.first().offset)
        assertTrue(result.entries.all { it.offset in sql.indices && !it.text.endsWith('\r') })
    }

    fun testRecursiveAndIncompleteQueriesRemainBounded() {
        val recursive = trace("WITH RECURSIVE t AS (\n SELECT * FROM t\n)\nSELECT * FROM t<caret>")
        assertEquals(setOf(1, 2, 4), recursive.entries.map { it.line }.toSet())
        assertNotNull(recursive.message)
        assertEquals(listOf(4), trace("SELECT q.id\nFROM (\n SELECT * FROM employee\n) q<caret> JOIN ").entries.map { it.line })
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
