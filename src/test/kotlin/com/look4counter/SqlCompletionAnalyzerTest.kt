package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.database.MetadataSnapshot
import com.look4counter.queryeditor.database.TableMetadata
import com.look4counter.queryeditor.sql.SqlCompletionAnalyzer
import com.look4counter.queryeditor.sql.SqlCompletionContext
import com.look4counter.queryeditor.sql.SqlCompletionKind

class SqlCompletionAnalyzerTest : BasePlatformTestCase() {
    fun testSecondProjectionResolvesAdjacentDerivedAlias() {
        val sql = """
            SELECT A.DEPT
                 , B.<caret>
              FROM (
                       SELECT DEPT
                            , JOB
                            , SALARY
                         FROM employee
                   ) A
                   (
                       SELECT DEPT2
                            , JOB
                            , SALARY
                         FROM employee
                   ) B
        """.trimIndent()
        assertEquals(setOf("DEPT2", "JOB", "SALARY"), names(sql, null))
        assertEquals(SqlCompletionKind.QUALIFIED_COLUMN, analyze(sql, null).kind)
    }

    fun testAdjacentDerivedQueriesDoNotBecomeAliasColumnLists() {
        val tail = " FROM (SELECT DEPT,JOB,SALARY FROM employee) A (SELECT DEPT2,JOB,SALARY FROM employee) B"
        assertEquals(setOf("DEPT", "JOB", "SALARY"), names("SELECT A.<caret>$tail", null))
        assertEquals(setOf("DEPT2", "JOB", "SALARY"), names("SELECT B.<caret>$tail", null))
        assertEquals(setOf("renamed", "JOB"), names("SELECT A.<caret> FROM (SELECT DEPT,JOB FROM employee) A(renamed,JOB)", null))
        assertEquals(setOf("id"), names("SELECT B.<caret> FROM (SELECT 1 AS old_id) A (WITH t AS (SELECT 2 AS id) SELECT id FROM t) B", null))
    }
    private val metadata = MetadataSnapshot(listOf(
        TableMetadata(null, "public", "EMP", listOf("EMPNO", "ENAME", "DEPTNO")),
        TableMetadata(null, "public", "DEPT", listOf("DEPTNO", "DNAME")),
        TableMetadata(null, "public", "AUDIT", listOf("AUDIT_ID")),
    ))
    private val emp = setOf("EMPNO", "ENAME", "DEPTNO")

    private fun analyze(sql: String, snapshot: MetadataSnapshot? = metadata): SqlCompletionContext {
        val offset = sql.indexOf("<caret>")
        require(offset >= 0)
        return SqlCompletionAnalyzer.analyze(sql.replace("<caret>", ""), offset, snapshot)
    }

    private fun names(sql: String, snapshot: MetadataSnapshot? = metadata) =
        analyze(sql, snapshot).candidates.map { it.text }.toSet()

    fun testColumnClausesReadTablesAfterCaretAndExcludeUnrelatedMetadata() {
        for (sql in listOf("SELECT <caret> FROM EMP", "SELECT * FROM EMP WHERE <caret>",
            "SELECT * FROM EMP JOIN (SELECT 1 AS X) q ON <caret>",
            "SELECT * FROM EMP GROUP BY <caret>", "SELECT * FROM EMP HAVING <caret>",
            "SELECT * FROM EMP ORDER BY <caret>")) {
            val result = names(sql)
            assertTrue(sql, result.containsAll(emp))
            assertFalse(sql, result.contains("AUDIT_ID"))
            assertFalse(sql, result.contains("EMP"))
        }
    }

    fun testFromJoinAndCommaOfferTablesOnly() {
        for (sql in listOf("SELECT * FROM <caret>", "SELECT * FROM EMP e JOIN <caret>",
            "SELECT * FROM EMP e, <caret>", "SELECT * FROM EMP e LEFT OUTER JOIN <caret>")) {
            assertEquals(sql, setOf("EMP", "DEPT", "AUDIT"), names(sql))
            assertEquals(SqlCompletionKind.TABLE, analyze(sql).kind)
        }
    }

    fun testAliasesWithAndWithoutAsResolveOnlyTheirOwnColumns() {
        for (asKeyword in listOf("", "AS ")) {
            val result = analyze("SELECT e.<caret> FROM EMP ${asKeyword}e JOIN DEPT d ON e.DEPTNO=d.DEPTNO")
            assertEquals(emp, result.candidates.map { it.text }.toSet())
            assertFalse(result.includeKeywords)
        }
        assertEquals(setOf("DEPTNO", "DNAME"), names("SELECT * FROM EMP e JOIN DEPT d ON d.<caret>"))
        assertEmpty(names("SELECT EMP.<caret> FROM EMP e"))
        assertEmpty(names("SELECT missing.<caret> FROM EMP e"))
    }

    fun testDuplicateColumnsAreQualifiedButRemainSearchableByColumnName() {
        val result = analyze("SELECT <caret> FROM EMP e JOIN DEPT d ON e.DEPTNO=d.DEPTNO")
        val duplicate = result.candidates.filter { it.lookupName == "DEPTNO" }
        assertEquals(setOf("e.DEPTNO", "d.DEPTNO"), duplicate.map { it.text }.toSet())
        assertTrue(result.candidates.any { it.text == "EMPNO" })
        assertFalse(result.candidates.any { it.text == "DEPTNO" })
    }

    fun testSchemaAndCatalogQualificationNarrowMetadata() {
        val snapshot = MetadataSnapshot(metadata.tables + TableMetadata("db", "other", "EMP", listOf("OTHER_ID")))
        assertEquals(setOf("OTHER_ID"), names("SELECT e.<caret> FROM db.other.EMP e", snapshot))
        assertEquals(emp, names("SELECT public.EMP.<caret> FROM public.EMP", snapshot))
        assertEquals(setOf("EMP"), names("SELECT * FROM other.<caret>", snapshot))
        assertEquals(setOf("EMP", "DEPT", "AUDIT"), names("SELECT * FROM public.<caret>", snapshot))
    }

    fun testAlreadyQuotedTablesAndAliasesAreRecognized() {
        for (sql in listOf("SELECT \"e\".<caret> FROM \"public\".\"EMP\" AS \"e\"",
            "SELECT `e`.<caret> FROM `public`.`EMP` `e`",
            "SELECT [e].<caret> FROM [public].[EMP] AS [e]")) {
            assertEquals(sql, emp, names(sql))
        }
    }

    fun testInnerAliasShadowsOuterAndCorrelatedAliasRemainsVisible() {
        assertEquals(setOf("DEPTNO", "DNAME"), names("SELECT (SELECT e.<caret> FROM DEPT e) FROM EMP e"))
        assertEquals(emp, names("SELECT * FROM EMP e WHERE EXISTS (SELECT e.<caret> FROM DEPT d)"))
    }

    fun testSiblingDerivedAndSetOperationScopesDoNotLeakAliases() {
        assertEmpty(names("SELECT (SELECT x.<caret> FROM DEPT d), (SELECT 1 FROM EMP x)"))
        assertEmpty(names("SELECT * FROM EMP e JOIN (SELECT e.<caret> FROM DEPT d) q ON 1=1"))
        assertEmpty(names("SELECT x.EMPNO FROM EMP x UNION ALL SELECT x.<caret> FROM DEPT d"))
    }

    fun testStatementsDoNotShareAliasesOrCtes() {
        assertEmpty(names("WITH t(a) AS (SELECT 1) SELECT * FROM t; SELECT t.<caret>"))
        assertEquals(setOf("EMP", "DEPT", "AUDIT"), names("WITH t(a) AS (SELECT 1) SELECT * FROM t; SELECT * FROM <caret>"))
    }

    fun testCteNamesAndProjectedAliasesAreAvailable() {
        val sql = "WITH active_emp AS (SELECT EMPNO, ENAME AS employee_name FROM EMP) "
        assertTrue(names(sql + "SELECT * FROM <caret>").contains("active_emp"))
        assertEquals(setOf("EMPNO", "employee_name"), names(sql + "SELECT a.<caret> FROM active_emp a"))
    }

    fun testExplicitCteColumnsOverrideOutputNamesWithoutMetadata() {
        assertEquals(setOf("dept_id", "total"), names(
            "WITH summary(dept_id,total) AS (SELECT 1 AS old_name, COUNT(*)) SELECT s.<caret> FROM summary s", null))
    }

    fun testPriorCteAndWildcardOutputAreResolved() {
        val sql = "WITH first_emp AS (SELECT e.* FROM EMP e), second_emp AS (SELECT * FROM first_emp) "
        assertEquals(emp, names(sql + "SELECT s.<caret> FROM second_emp s"))
    }

    fun testCteBodiesCannotSeeLaterDefinitionsOrTheirOwnNonRecursiveName() {
        val result = names("WITH first_emp AS (SELECT * FROM <caret>), later_emp AS (SELECT 1 AS X) SELECT * FROM first_emp")
        assertFalse(result.contains("first_emp"))
        assertFalse(result.contains("later_emp"))
        val later = names("WITH first_emp AS (SELECT 1 AS X), later_emp AS (SELECT * FROM <caret>) SELECT * FROM later_emp")
        assertTrue(later.contains("first_emp"))
        assertFalse(later.contains("later_emp"))
    }

    fun testNestedWithShadowsOuterDefinitionAndDoesNotEscape() {
        assertEquals(setOf("inner_id"), names("WITH t(outer_id) AS (SELECT 1) " +
            "SELECT (WITH t(inner_id) AS (SELECT 2) SELECT x.<caret> FROM t x) FROM t"))
        assertEquals(setOf("outer_id"), names("WITH t(outer_id) AS (SELECT 1) " +
            "SELECT x.<caret>, (WITH t(inner_id) AS (SELECT 2) SELECT * FROM t) FROM t x"))
    }

    fun testCteShadowsPhysicalTableOnlyWithoutSchema() {
        assertEquals(setOf("virtual_id"), names("WITH EMP(virtual_id) AS (SELECT 1) SELECT e.<caret> FROM EMP e"))
        assertEquals(emp, names("WITH EMP(virtual_id) AS (SELECT 1) SELECT e.<caret> FROM public.EMP e"))
    }

    fun testDerivedTableExposesOnlyItsOutputNames() {
        assertEquals(setOf("id", "ENAME"), names("SELECT q.<caret> FROM (SELECT e.EMPNO AS id, e.ENAME FROM EMP e) q"))
        assertEmpty(names("SELECT e.<caret> FROM (SELECT e.EMPNO FROM EMP e) q"))
        assertEquals(setOf("a", "b"), names("SELECT q.<caret> FROM (SELECT EMPNO,ENAME FROM EMP) AS q(a,b)"))
    }

    fun testProjectionAliasesAndUnaliasedExpressionsAreHandledConservatively() {
        assertEquals(setOf("doubled", "label"), names("WITH t AS (SELECT EMPNO*2 doubled, 'text' AS label, " +
            "COUNT(*), EMPNO+DEPTNO FROM EMP) SELECT t.<caret> FROM t"))
        assertTrue(names("SELECT EMPNO AS employee_id FROM EMP ORDER BY <caret>").contains("employee_id"))
        assertFalse(names("SELECT EMPNO AS employee_id FROM EMP WHERE <caret>").contains("employee_id"))
    }

    fun testRecursiveCteDoesNotLoopAndUsesDeclaredOrAnchorOutput() {
        assertEquals(setOf("id"), names("WITH RECURSIVE t(id) AS (SELECT 1 UNION ALL SELECT id+1 FROM t) " +
            "SELECT r.<caret> FROM t r", null))
        assertEquals(setOf("id"), names("WITH RECURSIVE t AS (SELECT 1 AS id UNION ALL SELECT id+1 FROM t) " +
            "SELECT r.<caret> FROM t r", null))
    }

    fun testIncompleteQueriesAndParenthesesStillProvideKnownColumns() {
        assertEquals(emp, names("SELECT e.<caret> FROM EMP e WHERE ("))
        assertEquals(emp, names("WITH t AS (SELECT e.<caret> FROM EMP e"))
        assertEquals(emp, names("SELECT COALESCE(e.<caret>,0) FROM EMP e"))
    }

    fun testCommentsAndStringContentsCannotDeclareRelations() {
        assertEquals(emp, names("SELECT e.<caret>, 'FROM AUDIT e' FROM EMP e /* JOIN DEPT e */ -- FROM AUDIT e"))
        assertEquals(emp, names("SELECT e.<caret> FROM EMP e /* outer /* FROM AUDIT */ JOIN DEPT */"))
    }

    fun testProtectedTokensOfferNothing() {
        for (sql in listOf("SELECT 'e.<caret>' FROM EMP e", "SELECT \"e<caret>\" FROM EMP",
            "SELECT 1 -- e.<caret>", "SELECT 1 /* e.<caret> */", "SELECT 12<caret>", "SELECT :e<caret>")) {
            val result = analyze(sql)
            assertEquals(sql, SqlCompletionKind.NONE, result.kind)
            assertEmpty(result.candidates)
            assertFalse(result.includeKeywords)
        }
    }

    fun testMissingMetadataStillOffersCteNamesAndOutputAliases() {
        assertTrue(names("WITH t AS (SELECT 1 AS id) SELECT * FROM <caret>", null).contains("t"))
        assertEquals(setOf("id"), names("WITH t AS (SELECT 1 AS id) SELECT t.<caret> FROM t", null))
        assertTrue(analyze("SELECT <caret>", null).includeKeywords)
    }

    fun testUnresolvedContextsUseGeneralFallbackButSelectOnlyUsesColumns() {
        val result = analyze("unsupported <caret>")
        assertEquals(SqlCompletionKind.FALLBACK, result.kind)
        assertTrue(result.candidates.any { it.text == "EMP" })
        assertTrue(result.candidates.any { it.text == "AUDIT_ID" })
        assertFalse(names("SELECT <caret>").contains("EMP"))
        assertTrue(names("SELECT <caret>").contains("AUDIT_ID"))
    }

    fun testDeepLongAndLargeTokenInputsFallBackWithoutRecursionFailure() {
        for (sql in listOf("SELECT " + "(".repeat(80) + "<caret>" + ")".repeat(80),
            "SELECT " + " ".repeat(262_144) + "<caret>",
            "SELECT " + "x,".repeat(10_100) + "<caret>")) {
            assertEquals(SqlCompletionKind.FALLBACK, analyze(sql).kind)
        }
    }
}
