package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.sql.SqlColumnValidator

class SqlColumnValidatorTest : BasePlatformTestCase() {
    private fun errors(sql: String) = SqlColumnValidator.analyze(sql).map { sql.substring(it.start, it.end) }

    fun testKnownOutputsAreCheckedAcrossAllQueryClauses() {
        val sql = "WITH t AS (SELECT id FROM physical) SELECT a.bad_select FROM t a JOIN t b ON a.bad_on=b.id WHERE a.bad_where=1 GROUP BY a.bad_group HAVING a.bad_having>0 ORDER BY a.bad_order"
        assertEquals(listOf("bad_select", "bad_on", "bad_where", "bad_group", "bad_having", "bad_order"), errors(sql))
        assertEmpty(errors(sql.replace(Regex("bad_[a-z]+"), "id")))
    }

    fun testExplicitNamesAndStarsPropagateAcrossCtesAndDerivedTables() {
        assertEquals(listOf("missing"), errors("WITH a AS (SELECT id FROM unknown), b AS (SELECT * FROM a) SELECT q.missing FROM (SELECT * FROM b) q"))
        assertEmpty(errors("WITH a(renamed) AS (SELECT 1+2 FROM unknown) SELECT renamed FROM a"))
        assertEquals(listOf("id"), errors("WITH a(renamed) AS (SELECT id FROM unknown) SELECT id FROM a"))
    }

    fun testUnknownTablesStarsAndPartialOutputNamesNeverProveAbsence() {
        for (sql in listOf("SELECT missing FROM physical", "WITH t AS (SELECT * FROM physical) SELECT t.missing FROM t",
            "WITH t AS (SELECT id, 1+2 FROM physical) SELECT missing FROM t",
            "WITH t(x) AS (SELECT * FROM physical) SELECT missing FROM t",
            "WITH t AS (SELECT id FROM physical) SELECT missing FROM t JOIN physical p ON t.id=p.id")) assertEmpty(sql, errors(sql))
        assertEquals(listOf("missing"), errors("WITH t AS (SELECT id FROM physical) SELECT t.missing FROM t JOIN physical p ON t.id=p.id"))
    }

    fun testAliasesFunctionsTypesCommentsAndParametersAreNotColumns() {
        val sql = "WITH t AS (SELECT id FROM physical) SELECT CAST(id AS custom_type) AS renamed, COALESCE(id,:value) label, CASE WHEN id=1 THEN 'missing' ELSE ? END result FROM t WHERE id=:id /* t.missing */ GROUP BY renamed HAVING label>0 ORDER BY renamed -- t.missing"
        assertEmpty(errors(sql))
    }

    fun testScopesCorrelationsAndQuotedNames() {
        assertEquals(listOf("bad"), errors("WITH t AS (SELECT id FROM physical) SELECT (SELECT a.bad FROM physical p) FROM t a"))
        assertEmpty(errors("WITH t AS (SELECT id FROM physical) SELECT (SELECT a.anything FROM physical a) FROM t a"))
        assertEmpty(errors("WITH t AS (SELECT id FROM physical) SELECT id FROM t; SELECT missing FROM t"))
        assertEquals(listOf("\"mixed\""), errors("WITH t AS (SELECT id AS \"Mixed\" FROM physical) SELECT \"mixed\" FROM t"))
        assertEmpty(errors("WITH t AS (SELECT id AS \"Mixed\" FROM physical) SELECT \"Mixed\" FROM t"))
    }

    fun testDmlReferencesToKnownRelationsAreChecked() {
        assertEquals(listOf("bad", "missing"), errors("WITH t AS (SELECT id FROM physical) UPDATE t SET bad=id WHERE missing=1"))
        assertEquals(listOf("missing"), errors("WITH t AS (SELECT id FROM physical) DELETE FROM t WHERE missing=1"))
        assertEquals(listOf("missing", "bad"), errors("WITH t AS (SELECT id FROM physical) MERGE INTO physical p USING t s ON s.missing=p.id WHEN MATCHED THEN UPDATE SET p.id=s.bad"))
        assertEquals(listOf("missing"), errors("WITH t AS (SELECT id FROM physical) INSERT INTO t(missing) VALUES (1)"))
        assertEquals(listOf("missing"), errors("WITH t AS (SELECT id FROM physical) INSERT INTO t(missing) SELECT id FROM t"))
        assertEmpty(errors("UPDATE physical SET any_column=:value WHERE missing=1"))
    }

    fun testIncompleteAndRecursiveInputsStayConservative() {
        for (sql in listOf("WITH t AS (SELECT id FROM physical) SELECT t.missing FROM (t",
            "WITH RECURSIVE t AS (SELECT * FROM t) SELECT missing FROM t", "SELECT 'open", "SELECT /* open",
            "WITH t AS (SELECT id FROM physical) SELECT missing FROM t JOIN",
            "WITH t AS (SELECT id, FROM physical) SELECT missing FROM t",
            "WITH t AS (SELECT id FROM physical) SELECT CASE WHEN id=1 THEN missing FROM t")) assertEmpty(sql, errors(sql))
        assertEmpty(errors("SELECT " + " ".repeat(262_144)))
    }

    fun testPseudoColumnsAndLiteralPrefixesAreProtected() {
        assertEmpty(errors("WITH t AS (SELECT id FROM physical) SELECT SYSDATE, ROWNUM, Q'[literal]', N'문자', id FROM t"))
        assertEmpty(errors("WITH t AS (SELECT id AS \"ID\" FROM physical) SELECT id FROM t"))
        assertEquals(listOf("missing", "missing"), errors("WITH t AS (SELECT id FROM physical) SELECT missing FROM t WHERE missing=1"))
        assertEmpty(errors("WITH t AS (SELECT event_time FROM physical) SELECT event_time AT TIME ZONE 'UTC' FROM t"))
    }

    fun testPivotOutputTransformationCannotProveMissingColumns() {
        assertEmpty(errors("WITH t AS (SELECT job,salary FROM physical) SELECT MANAGER FROM t PIVOT(SUM(salary) FOR job IN ('Manager' AS MANAGER))"))
        assertEmpty(errors("WITH t AS (SELECT job,salary FROM physical), p AS (SELECT * FROM t PIVOT(SUM(salary) FOR job IN ('Manager' AS MANAGER))) SELECT MANAGER FROM p"))
        assertEquals(listOf("missing"), errors("WITH t AS (SELECT job,salary FROM physical), p AS (SELECT MANAGER FROM t PIVOT(SUM(salary) FOR job IN ('Manager' AS MANAGER))) SELECT missing FROM p"))
    }
}
