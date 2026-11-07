package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.sql.SqlColumnTrace

class SqlColumnTraceTest : BasePlatformTestCase() {
    fun testUserMergeShowsSourceDeclarationAndSelectedColumnItself() {
        val sql = """
            MERGE
             INTO emp a
            USING (
                SELECT 'B' AS TEST
                     , 'c' AS c
                  FROM DUAL
            ) b
            ON (
                a.empno = 7788
            )
            WHEN MATCHED THEN
            UPDATE
               SET a.deptno = b.TEST
                 , a.aa = b.c
            WHEN NOT MATCHED THEN
            INSERT (
                  a.empno
                , a.ename
                , a.deptno
            )
            VALUES (
                  7788
                , 'SCOTT'
                , 20
            )
        """.trimIndent()
        for (position in listOf(sql.indexOf("a.deptno") + 2, sql.lastIndexOf("a.deptno") + 2)) {
            val result = com.look4counter.queryeditor.sql.SqlTrace.analyze(sql, position + 1, null)
            assertEquals(listOf(2, 13, 19), result.entries.map { it.line })
            assertEquals(position, result.selectedOffset)
            assertEquals(listOf(sql.indexOf("emp a"), sql.indexOf("a.deptno") + 2, sql.lastIndexOf("a.deptno") + 2), result.entries.map { it.offset })
            assertFalse(result.entries.any { it.line == 4 })
        }
        val output = sql.indexOf("AS TEST") + 3
        for ((position, selected) in listOf(output + 1 to output, sql.indexOf("'B'") + 1 to output,
            sql.indexOf("b.TEST") + 3 to sql.indexOf("b.TEST") + 2)) {
            val result = com.look4counter.queryeditor.sql.SqlTrace.analyze(sql, position, null)
            assertEquals(listOf(4, 13), result.entries.map { it.line })
            assertEquals(selected, result.selectedOffset)
            assertEquals(selected, result.graph.nodes[result.graph.selectedNodeId!!].entry.offset)
            assertFalse(result.entries.any { it.line == 2 })
        }
    }

    fun testUpdateTargetsAndInputsTraceTheirOwnSourcesWithoutAssignmentEdges() {
        val sql = "WITH s AS (\n SELECT pay, bonus FROM payroll\n)\nUPDATE emp e\nSET salary=s.pay, extra=s.bonus\nFROM s\nWHERE e.id=1"
        val target = sql.indexOf("salary")
        assertEquals(listOf(sql.indexOf("emp e"), target),
            SqlColumnTrace.analyze(sql, target + 1, null).entries.map { it.offset })
        val input = sql.indexOf("s.pay") + 2
        assertEquals(listOf(sql.indexOf("pay,"), input),
            SqlColumnTrace.analyze(sql, input + 1, null).entries.map { it.offset })
        assertEquals(listOf(sql.indexOf("pay,"), input),
            SqlColumnTrace.analyze(sql, sql.indexOf("pay,") + 1, null).entries.map { it.offset })
    }

    fun testQuotedDerivedReferenceIncludesCurrentLocationAndExcludesPredicateSubquery() {
        val sql = "UPDATE dst SET \"Id\"=s.\"Value\"\nFROM (SELECT old_id AS \"Value\" FROM src) s\nWHERE EXISTS (SELECT hidden FROM other)"
        val input = sql.indexOf("s.\"Value\"") + 2
        assertEquals(listOf(sql.indexOf("AS \"Value\"") + 3, input),
            SqlColumnTrace.analyze(sql, input + 2, null).entries.map { it.offset })
        val target = sql.indexOf("\"Id\"")
        assertEquals(listOf(sql.indexOf("dst"), target), SqlColumnTrace.analyze(sql, target + 2, null).entries.map { it.offset })
        assertTrue(SqlColumnTrace.analyze(sql, sql.indexOf("hidden") + 1, null).entries.isEmpty())
    }

    fun testInsertSelectKeepsCtePathSeparateFromTargetColumns() {
        val sql = "WITH s AS (SELECT old_id AS id, label FROM src)\nINSERT INTO dst (new_id, title)\nSELECT id, label FROM s\nUNION ALL\nSELECT other_id, other_label FROM other"
        for (position in listOf(sql.indexOf("old_id"), sql.indexOf("SELECT id") + 7)) {
            assertEquals(listOf(sql.indexOf("AS id") + 3, sql.indexOf("SELECT id") + 7),
                SqlColumnTrace.analyze(sql, position + 1, null).entries.map { it.offset })
        }
        val target = sql.indexOf("new_id")
        assertEquals(listOf(sql.indexOf("dst"), target), SqlColumnTrace.analyze(sql, target + 1, null).entries.map { it.offset })
        assertEquals(listOf(sql.indexOf("other_id")),
            SqlColumnTrace.analyze(sql, sql.indexOf("other_id") + 1, null).entries.map { it.offset })
    }

    fun testPlainChangesTraceOnlyTableAndCurrentColumnNotAssignedValues() {
        for (sql in listOf("UPDATE emp SET salary=1", "INSERT INTO emp (salary) VALUES (1), (2)", "DELETE FROM emp WHERE salary=1")) {
            val column = sql.indexOf("salary")
            val result = SqlColumnTrace.analyze(sql, column + 1, null)
            assertEquals(sql, listOf(sql.indexOf("emp"), column), result.entries.map { it.offset })
            assertEquals(column, result.selectedOffset)
        }
    }

    fun testScalarSelectKeepsInnerCtePathSeparateFromUpdateTarget() {
        val sql = "UPDATE emp SET salary=(WITH s AS (SELECT old_pay AS pay FROM payroll) SELECT pay FROM s)\nWHERE EXISTS (SELECT hidden FROM other)"
        for (position in listOf(sql.indexOf("old_pay"), sql.indexOf("SELECT pay") + 7)) {
            assertEquals(listOf(sql.indexOf("AS pay") + 3, sql.indexOf("SELECT pay") + 7),
                SqlColumnTrace.analyze(sql, position + 1, null).entries.map { it.offset })
        }
        val target = sql.indexOf("salary")
        assertEquals(listOf(sql.indexOf("emp"), target), SqlColumnTrace.analyze(sql, target + 1, null).entries.map { it.offset })
        assertTrue(SqlColumnTrace.analyze(sql, sql.indexOf("hidden") + 1, null).entries.isEmpty())
    }

    fun testCteWriteTargetsTraceOwnSelectSourceWithoutAssignmentValue() {
        for (sql in listOf("WITH s AS (SELECT salary FROM payroll) UPDATE s SET salary=1",
            "WITH s AS (SELECT salary FROM payroll) INSERT INTO s (salary) VALUES (1)")) {
            val target = sql.lastIndexOf("salary")
            assertEquals(listOf(sql.indexOf("salary"), target), SqlColumnTrace.analyze(sql, target + 1, null).entries.map { it.offset })
        }
    }

    fun testUnqualifiedTargetUsesTargetTableEvenWhenInputHasSameColumnName() {
        val sql = "MERGE INTO emp a USING (SELECT salary FROM payroll) b ON (a.id=1) WHEN MATCHED THEN UPDATE SET salary=b.salary"
        val target = sql.indexOf("SET salary") + 4
        assertEquals(listOf(sql.indexOf("emp a"), target), SqlColumnTrace.analyze(sql, target + 1, null).entries.map { it.offset })
        val input = sql.indexOf("b.salary") + 2
        assertEquals(listOf(sql.indexOf("SELECT salary") + 7, input),
            SqlColumnTrace.analyze(sql, input + 1, null).entries.map { it.offset })
    }
    fun testSourceAliasIsNotCollectedAsAColumnReference() {
        val sql = "UPDATE emp salary SET salary=1"
        val column = sql.lastIndexOf("salary")
        assertEquals(listOf(sql.indexOf("emp"), column),
            SqlColumnTrace.analyze(sql, column + 1, null).entries.map { it.offset })
    }

    fun testSelectValuesAndExpressionsReachAllDmlReferencesWithoutCrossingOrigins() {
        for (expression in listOf("'B'", "42", ":value", "CASE WHEN flag=1 THEN pay ELSE 0 END")) {
            val sql = "WITH s AS (SELECT $expression AS result FROM src)\nMERGE INTO dst a\nUSING (SELECT result FROM s) b\nON (a.id=0)\nWHEN MATCHED THEN UPDATE SET a.value=b.result\nWHEN NOT MATCHED THEN INSERT (value) VALUES (b.result);\nSELECT result FROM unrelated"
            val expected = listOf(sql.indexOf("AS result") + 3, sql.indexOf("SELECT result") + 7,
                sql.indexOf("b.result") + 2, sql.lastIndexOf("b.result") + 2)
            val positions = listOf(sql.indexOf(expression) + 1, expected[0] + 1, expected[2] + 1)
            for (position in positions) {
                assertEquals(expression, expected, SqlColumnTrace.analyze(sql, position, null).entries.map { it.offset })
            }
            if (expression.startsWith("CASE")) {
                assertEquals(expected, SqlColumnTrace.analyze(sql, sql.indexOf("THEN pay") + 6, null).entries.map { it.offset })
            }
        }
        val outside = "INSERT INTO dst (value) VALUES ('B'); SELECT 'B' AS value FROM src -- 'B'"
        assertTrue(SqlColumnTrace.analyze(outside, outside.indexOf("'B'") + 1, null).entries.isEmpty())
        assertTrue(SqlColumnTrace.analyze(outside, outside.lastIndexOf("'B'") + 1, null).entries.isEmpty())
    }
    fun testGraphKeepsColumnBranchesAndRejoiningSeparate() {
        val sql = "WITH a AS (SELECT id FROM physical), b AS (SELECT id AS b_id FROM a), " +
            "c AS (SELECT id AS c_id FROM a) SELECT b.b_id+c.c_id AS total FROM b JOIN c ON b.b_id=c.c_id"
        val result = SqlColumnTrace.analyze(sql, sql.indexOf("id") + 1, null)
        val graph = result.graph
        fun children(name: String) = graph.nodes.single { it.name == "컬럼 $name" }.children.map { graph.nodes[it].name }
        assertEquals(listOf("컬럼 id"), graph.roots.map { graph.nodes[it].name })
        assertEquals(listOf("컬럼 b_id", "컬럼 c_id"), children("id"))
        assertEquals(listOf("컬럼 total"), children("b_id"))
        assertEquals(listOf("컬럼 total"), children("c_id"))
        assertTrue(children("total").isEmpty())
    }

    fun testGraphDoesNotMergeDifferentStarSlotsAtTheSameOffset() {
        val sql = "WITH t AS (SELECT id, name FROM physical) SELECT * FROM t"
        val result = SqlColumnTrace.analyze(sql, sql.indexOf('*') + 1, null)
        val graph = result.graph
        val stars = graph.nodes.filter { it.entry.offset == sql.indexOf('*') }
        assertEquals(listOf("컬럼 t.id", "컬럼 t.name"), stars.map { it.name })
        assertEquals(2, stars.map { it.id }.toSet().size)
        for (name in listOf("id", "name")) {
            val origin = graph.nodes.single { it.name == "컬럼 $name" }
            assertEquals(listOf("컬럼 t.$name"), origin.children.map { graph.nodes[it].name })
        }
    }

    fun testSelectionAnchorFollowsCurrentProjectionWithoutSelectingConditionStage() {
        val sql = "WITH t AS (\n SELECT id FROM physical\n)\nSELECT id FROM t\nWHERE id=1"
        for (position in listOf(sql.indexOf("id"), sql.indexOf("id FROM t"))) {
            val result = SqlColumnTrace.analyze(sql, position + 1, null)
            assertEquals(position, result.selectedOffset)
            assertTrue(result.entries.any { it.offset == result.selectedOffset })
        }
        val condition = sql.lastIndexOf("id")
        val result = SqlColumnTrace.analyze(sql, condition + 1, null)
        assertEquals(condition, result.selectedOffset)
        assertEquals(listOf(2, 4), result.entries.map { it.line })
    }

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
