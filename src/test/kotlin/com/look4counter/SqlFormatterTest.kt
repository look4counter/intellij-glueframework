package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.sql.SqlFormatter
import com.look4counter.queryeditor.sql.SqlFormattingOptions

class SqlFormatterTest : BasePlatformTestCase() {
    fun testInlineSubqueryClausesUseActualSelectColumn() {
        val sql = "SELECT HELLO            AS B\n     , (SELECT *\n             FROM DUAL) AS C\n  FROM FAVORITE\n WHERE TP   = :AFF\n   AND A    = H\n   AND B222 = A"
        for (options in listOf(SqlFormattingOptions(), SqlFormattingOptions(true, true))) {
            val result = SqlFormatter.format(sql, options).sql!!
            val lines = result.lines()
            assertEquals(lines[1].indexOf("SELECT") + 5, lines[2].indexOf("FROM") + 3)
            assertEquals(sql.count { it == '\n' }, result.count { it == '\n' })
            assertEquals(result, SqlFormatter.format(result, options).sql)
        }
    }

    fun testNestedInlineSubqueriesKeepIndependentClauseAnchors() {
        val sql = "SELECT (SELECT (SELECT X\nFROM T\nWHERE A=1\nAND B=2\nGROUP BY X\nORDER BY X) AS N\nFROM U\nWHERE C=3) AS V\nFROM ROOT"
        val lines = format(sql).lines()
        val deepest = lines[0].lastIndexOf("SELECT") + 5
        assertEquals(deepest, lines[1].indexOf("FROM") + 3)
        assertEquals(deepest, lines[2].indexOf("WHERE") + 4)
        assertEquals(deepest, lines[3].indexOf("AND") + 2)
        assertEquals(deepest, lines[4].indexOf("GROUP") + 4)
        assertEquals(deepest, lines[5].indexOf("ORDER") + 4)
        val middle = lines[0].indexOf("SELECT", 1) + 5
        assertEquals(middle, lines[6].indexOf("FROM") + 3)
        assertEquals(middle, lines[7].indexOf("WHERE") + 4)
        assertEquals(5, lines[8].indexOf("FROM") + 3)
        assertEquals(lines.joinToString("\n"), format(lines.joinToString("\n")))
    }

    fun testSuppressedLineBreaksDoNotAddCommaOrParenthesisPadding() {
        assertEquals("SELECT A, B FROM (SELECT X, Y FROM T) q WHERE X = 1 AND Y = 2",
            format("select A,B from (select X,Y from T) q where X=1 and Y=2"))
        assertEquals("INSERT INTO t (a, b) VALUES (1, 2)", format("insert into t(a,b) values(1,2)"))
        assertEquals("SELECT CASE WHEN A = 1 THEN B ELSE C END AS X FROM T",
            format("select case when A=1 then B else C end as X from T"))
    }
    fun testOriginalBlankLinesAndLineEndingsArePreserved() {
        val sql = "\r\nselect A,B\r\n\r\nfrom EMP\r\nwhere A=1\r\nand B=2\r\n\r\n"
        val result = format(sql)
        assertEquals(Regex("\\r\\n|\\r|\\n").findAll(sql).map { it.value }.toList(), Regex("\\r\\n|\\r|\\n").findAll(result).map { it.value }.toList())
        assertEquals(listOf("", "SELECT A, B", "", "  FROM EMP", " WHERE A = 1", "   AND B = 2", "", ""), result.split("\r\n"))
        assertEquals(result, format(result))
    }

    fun testCompoundKeywordsKeepUserBreaksAndInlineCommentsCannotSwallowSql() {
        val sql = "select A -- keep  \nfrom T\ngroup\nby A\norder\nby A"
        val result = format(sql)
        assertTrue(result.contains("-- keep  \n"))
        assertTrue(result.contains("GROUP\nBY A"))
        assertTrue(result.contains("ORDER\nBY A"))
        assertEquals(sql.count { it == '\n' }, result.count { it == '\n' })
        assertEquals(result, format(result))
    }

    fun testBreaksInsideFunctionArgumentsAndAfterCommaStayInPlace() {
        val sql = "select COALESCE(A,\n    B),\n    C from EMP"
        val result = format(sql)
        assertEquals("SELECT COALESCE(A,\n    B),\n    C FROM EMP", result)
        assertEquals(result, format(result))
    }

    fun testSelectionKeepsBoundaryBlankLinesExactlyOnce() {
        val sql = "before;\n\nselect A,B from EMP\n\n;after"
        val start = sql.indexOf('\n')
        val end = sql.indexOf(";after")
        assertEquals("\n\nSELECT A, B FROM EMP\n\n", SqlFormatter.formatSelection(sql, start, end).sql)
    }

    fun testStandaloneCommentsKeepCrlfAndTrailingSpaces() {
        val sql = "select A\r\n\t -- keep  \r\nfrom EMP\r\n"
        assertEquals("SELECT A\r\n\t -- keep  \r\n  FROM EMP\r\n", format(sql))
    }

    fun testWhereEqualsAndSelectAsAlignIndependently() {
        val sql = "select AB as A\n, LONG_NAME as B\nfrom T\nwhere AB = AB\nand HELO = HI"
        val plain = "SELECT AB AS A\n     , LONG_NAME AS B\n  FROM T\n WHERE AB = AB\n   AND HELO = HI"
        val where = plain.replace("WHERE AB =", "WHERE AB   =")
        val aliases = plain.replace("AB AS A", "AB        AS A")
        assertEquals(plain, SqlFormatter.format(sql).sql)
        assertEquals(where, SqlFormatter.format(sql, SqlFormattingOptions(alignWhereEquals = true)).sql)
        assertEquals(aliases, SqlFormatter.format(sql, SqlFormattingOptions(alignSelectAs = true)).sql)
        val both = aliases.replace("WHERE AB =", "WHERE AB   =")
        assertEquals(both, SqlFormatter.format(sql, SqlFormattingOptions(true, true)).sql)
        assertEquals(both, SqlFormatter.format(both, SqlFormattingOptions(true, true)).sql)
    }

    fun testAlignmentPreservesCommentsStringsAndNonEqualityOperators() {
        val sql = "select 'AS =' as A\n,cast(X as varchar(20)) as B\nfrom T\nwhere AB = 'x=y'\nand HELO = :value\nand BIG >= 1\nand X != 2\n  -- AB = LONG AS unchanged  \n"
        val plain = SqlFormatter.format(sql).sql!!
        val result = SqlFormatter.format(sql, SqlFormattingOptions(true, true)).sql!!
        assertTrue(result.contains("'AS ='"))
        assertTrue(result.contains("CAST(X AS varchar(20)) AS B", ignoreCase = true))
        assertTrue(result.contains("WHERE AB   = 'x=y'"))
        assertTrue(result.contains("AND BIG >= 1"))
        assertTrue(result.contains("AND X != 2"))
        assertTrue(result.endsWith("  -- AB = LONG AS unchanged  \n"))
        assertEquals(plain.substringAfter("AND BIG"), result.substringAfter("AND BIG"))
        assertEquals(result, SqlFormatter.format(result, SqlFormattingOptions(true, true)).sql)
    }

    fun testAlignmentGroupsAreIndependentAcrossCtesSubqueriesAndStatements() {
        val sql = "with C as (\nselect A as X\n,LONG_INNER as Y\nfrom T\nwhere A = 1\nand BBB = 2\n)\nselect C.X as Z\n,C.Y as Q\nfrom C\nwhere XX = 3\nand X = 4;\nselect A as X\n,B as Y\nfrom T\nwhere A = 1\nand B = 2"
        val result = SqlFormatter.format(sql, SqlFormattingOptions(true, true)).sql!!
        for (block in result.split(';')) {
            val lines = block.lines().filter { it.contains(" = ") }
            val byIndent = lines.groupBy { it.indexOfFirst { c -> !c.isWhitespace() } + it.trimStart().substringBefore(' ').length }
            byIndent.values.forEach { group -> assertEquals(1, group.map { it.indexOf('=') }.toSet().size) }
        }
        assertTrue(result.contains("SELECT C.X AS Z"))
        assertTrue(result.contains("WHERE A = 1\n   AND B = 2"))
        assertEquals(result, SqlFormatter.format(result, SqlFormattingOptions(true, true)).sql)
    }

    fun testAlignedWhereKeepsSubqueryOpeningAndClosingColumnsConsistent() {
        val sql = "select A as X\n,case when A=1 then 2 else 3 end as LONG\nfrom T\nwhere A = (\nselect ID from U\n)\nand LONG_NAME = 2"
        val result = SqlFormatter.format(sql, SqlFormattingOptions(true, true)).sql!!
        val lines = result.lines()
        val opening = lines.single { it.contains("WHERE") }
        val closing = lines.single { it.trim() == ")" }
        assertEquals(opening.indexOf('('), closing.indexOf(')'))
        assertTrue(lines.single { it.trimStart().startsWith("SELECT ID") }.indexOf("SELECT") == opening.indexOf('(') + 4)
        assertEquals(1, lines.filter { it.contains(" = ") && !it.contains("WHEN") }.map { it.indexOf('=') }.toSet().size)
        assertEquals(result, SqlFormatter.format(result, SqlFormattingOptions(true, true)).sql)
    }

    fun testWhereAlignmentHandlesParenthesesAndLeavesUpdateSetUnchanged() {
        val sql = "update T set A=1,LONG=2\nwhere (AB=AB)\nand HELO=HI"
        val plain = SqlFormatter.format(sql).sql!!
        val result = SqlFormatter.format(sql, SqlFormattingOptions(alignWhereEquals = true)).sql!!
        assertEquals(plain.substringBefore("WHERE"), result.substringBefore("WHERE"))
        assertTrue(result.contains("WHERE (AB  = AB)\n   AND HELO = HI"))
        assertEquals(result, SqlFormatter.format(result, SqlFormattingOptions(alignWhereEquals = true)).sql)
    }

    fun testPivotMatchesUserExampleIncludingAdjacentSubqueries() {
        val expected = """
            SELECT *
              FROM (
                       SELECT DEPT
                            , JOB
                            , SALARY
                         FROM employee
                   ) A
                   (
                       SELECT DEPT
                            , JOB
                            , SALARY
                         FROM employee
                   ) B
             PIVOT (
                 SUM(SALARY) FOR JOB IN (
                       'Manager' AS MANAGER
                     , 'Clerk' AS CLERK
                 )
             )
        """.trimIndent()
        assertEquals(inline(expected), format(expected.replace('\n', ' ')))
        assertEquals(expected, format(expected))
    }

    fun testUnpivotUsesBlockAndLeadingInListCommas() {
        val expected = """
            SELECT *
              FROM employee
            UNPIVOT INCLUDE NULLS (
                salary FOR job IN (
                      manager AS 'Manager'
                    , clerk AS 'Clerk'
                )
            )
        """.trimIndent()
        assertEquals(inline(expected), format(expected.replace('\n', ' ')))
        assertEquals(expected, format(expected))
        assertEquals(expected, format(expected.replace("UNPIVOT INCLUDE NULLS", "unpivot include nulls")))
        val excluded = expected.replace("INCLUDE NULLS", "EXCLUDE NULLS")
        assertEquals(excluded, format(excluded))
        val tuple = format("SELECT * FROM employee UNPIVOT ((a,b) FOR job IN ((c,d) AS 'x',(e,f) AS 'y'))")
        assertTrue(tuple.contains("(a, b) FOR job IN ("))
        assertTrue(tuple.contains("(c, d) AS 'x'"))
        assertEquals(tuple, format(tuple))
    }
    fun testSelectedCommentPreservesBoundaryNewlineAndPartialCommentIsRejected() {
        val text = "-- select A,B  \nselect C,D from EMP"
        val end = text.indexOf('\n') + 1
        assertEquals(text.substring(0, end), SqlFormatter.formatSelection(text, 0, end).sql)
        assertNull(SqlFormatter.formatSelection(text, 3, end).sql)
    }
    fun testStandaloneCommentLinesPreserveIndentAndTrailingSpaces() {
        val sql = "select A\n\t  -- keep select A,B  \n   /* keep\n      block */  \nfrom EMP"
        val result = format(sql)
        assertTrue(result.contains("\n\t  -- keep select A,B  \n"))
        assertTrue(result.contains("\n   /* keep\n      block */  \n"))
        assertEquals(result, format(result))
    }
    fun testUpdateAssignmentsUseLeadingCommasAndEAnchor() {
        val sql = "UPDATE EMP\n   SET A = 1\n     , B = COALESCE(B, 0)\n WHERE ID = :id\n   AND X = 1"
        assertEquals(inline(sql), format("update EMP set A=1,B=COALESCE(B,0) where ID=:id and X=1"))
        assertEquals(sql, format(sql))
    }

    fun testInsertColumnAndValueListsUseLeadingCommas() {
        val expected = """
            INSERT INTO emp (
                  empno
                , ename
                , sal
                , deptno
            ) VALUES (
                  ?
                , ?
                , ?
                , ?
            )
        """.trimIndent()
        assertEquals(inline(expected), format(expected.replace('\n', ' ')))
        assertEquals(expected, format(expected))
        val select = format("insert into EMP(A,B) select X,Y from SRC")
        assertTrue(select.contains(" SELECT X, Y FROM SRC"))
        assertEquals(select, format(select))
    }

    fun testMergeBranchesAndAssignmentsHaveConsistentAnchor() {
        val expected = """
            MERGE INTO emp a
            USING dual ON (
                  a.empno = 7788
                , b.aaa = 123
            ) B
            WHEN MATCHED THEN
            UPDATE
               SET a.deptno = 20
                 , a.a = b.no1
            WHEN NOT MATCHED THEN
            INSERT (
                  a.empno
                , a.ename
                , a.deptno
            )
            VALUES
            (
                  7788
                , 'SCOTT'
                , 20
            )
        """.trimIndent()
        assertEquals(inline(expected), format(expected.replace('\n', ' ')))
        assertEquals(expected, format(expected))
        assertEquals(expected, format(expected.replace("MERGE INTO", "merge into")
            .replace("WHEN MATCHED THEN", "when matched then").replace("WHEN NOT MATCHED THEN", "when not matched then")))
    }

    fun testSelectSubqueryMatchesRequestedBracketColumns() {
        val expected = """
            SELECT EMPNO
                 , ENAME
                 , JOB
                 , MGR
                 , HIREDATE
                 , SAL
                 , DEPTNO
                 , (
                       SELECT *
                         FROM DUAL
                   ) AS BB
              FROM EMP
             WHERE DEPTNO = ?
        """.trimIndent()
        assertEquals(inline(expected), format(expected.replace('\n', ' ')))
        assertEquals(expected, format(expected))
    }
    private fun format(sql: String): String {
        val result = SqlFormatter.format(sql)
        assertNull(result.problem)
        assertEquals("Formatting must preserve every token's original line", tokenLines(sql), tokenLines(result.sql!!))
        return result.sql!!
    }
    private fun tokenLines(sql: String): List<Pair<String, Int>> {
        val lexer = Class.forName("com.look4counter.queryeditor.sql.SqlLexer").getDeclaredConstructor().newInstance() as com.intellij.lexer.Lexer
        lexer.start(sql)
        return buildList {
            while (lexer.tokenType != null) {
                if (lexer.tokenType != com.intellij.psi.TokenType.WHITE_SPACE) {
                    add(sql.substring(lexer.tokenStart, lexer.tokenEnd).uppercase() to sql.substring(0, lexer.tokenStart).count { it == '\n' })
                }
                lexer.advance()
            }
        }
    }
    private fun inline(sql: String): String = sql.replace('\n', ' ').replace(Regex(" +"), " ")
        .replace(" ,", ",").replace("( ", "(").replace(" )", ")")

    private val sample = """
        WITH TB_MAST AS (
            SELECT *
              FROM DUAL A
             WHERE A.COL1 = COL
        )
        , TB_SUB AS (
            SELECT *
              FROM DUAL
        )
        SELECT COL1
             , COL2
             , COL3
             , CASE
               WHEN COL4 = COL3
               THEN CC
               ELSE 'ABC'
               END AS BB
          FROM TABLE
         WHERE COL1 = COL
           AND COL2 = COL
         GROUP BY COL1
         ORDER BY COL2
    """.trimIndent()

    fun testUserExampleMatchesExactlyAndIsIdempotent() {
        assertEquals(inline(sample), format(sample.replace('\n', ' ')))
        assertEquals(sample, format(sample))
        assertEquals(inline(sample), format(format(sample.replace('\n', ' '))))
    }

    fun testKeywordAlignmentAndLeadingCommas() {
        assertEquals("SELECT A, B FROM EMP WHERE A = :a AND B > 1 OR C < 2 GROUP BY A, B HAVING COUNT(*) > 1 ORDER BY B DESC, A",
            format("select A,B from EMP where A=:a and B>1 or C<2 group by A,B having COUNT(*)>1 order by B desc,A"))
    }

    fun testCteColumnListsRecursiveHeaderAndNestedCteIndentation() {
        val result = format("with recursive T(ID,NAME) as (with U as (select ID,NAME from EMP) select * from U) select T.ID from T")
        assertTrue(result.startsWith("WITH RECURSIVE T(ID, NAME) AS (WITH U AS (SELECT ID"))
        assertTrue(result.contains(" FROM EMP) SELECT * FROM U) SELECT T.ID"))
        assertFalse(result.contains('\n'))
        assertEquals(result, format(result))
    }

    fun testCaseBranchesAndNestedCaseUseTheirCaseColumns() {
        val result = format("select case when A=1 then case when B=2 then 'x' else 'y' end else 'z' end as VALUE from EMP")
        assertTrue(result.startsWith("SELECT CASE WHEN A = 1 THEN CASE WHEN B = 2"))
        assertTrue(result.contains(" END ELSE 'z' END AS VALUE FROM EMP"))
        assertFalse(result.contains('\n'))
        assertEquals(result, format(result))
    }

    fun testFunctionArgumentsStayInlineAndCaseInsideFunctionIsAligned() {
        val result = format("select COALESCE(A,B),COUNT(*),COALESCE(case when A=1 then B else C end,0) from EMP")
        assertTrue(result.startsWith("SELECT COALESCE(A, B), COUNT(*), COALESCE(CASE"))
        assertTrue(result.contains(" WHEN A = 1"))
        assertTrue(result.contains("END, 0) FROM EMP"))
        assertEquals(result, format(result))
    }

    fun testBetweenRangeAndIsNotSplitAsAnotherCondition() {
        assertEquals("SELECT * FROM EMP WHERE A BETWEEN 1 AND 3 AND B = 4 OR C NOT BETWEEN 5 AND 6",
            format("select * from EMP where A between 1 and 3 and B=4 or C not between 5 and 6"))
    }

    fun testJoinOnAndSetOperatorsUseTheSameKeywordAnchor() {
        val result = format("select e.ID from EMP e left outer join DEPT d on e.ID=d.ID and d.ID>0 union all select ID from OTHER")
        assertTrue(result.contains(" LEFT OUTER JOIN DEPT d ON e.ID = d.ID AND d.ID > 0 UNION ALL SELECT ID"))
        assertEquals(result, format(result))
    }

    fun testDerivedAndCorrelatedSubqueriesKeepFourSpaceQueryIndent() {
        val result = format("select q.ID from (select ID from EMP where ID in (select ID from DEPT)) q")
        assertTrue(result.contains("FROM (SELECT ID FROM EMP"))
        assertTrue(result.contains("IN (SELECT ID FROM DEPT"))
        assertFalse(result.contains('\n'))
        assertTrue(result.endsWith(") q"))
        assertEquals(result, format(result))
    }

    fun testStringsQuotedIdentifiersParametersAndLiteralPrefixesArePreserved() {
        val result = format("select 'FROM, WHERE -- ) it''s',\"Mixed Name\",`Mixed Table`,[Mixed Column],N'문자',E'escape\\\\text',:some_param,? from EMP")
        for (literal in listOf("'FROM, WHERE -- ) it''s'", "\"Mixed Name\"", "`Mixed Table`", "[Mixed Column]",
            "N'문자'", "E'escape\\\\text'", ":some_param", "?")) {
            assertTrue(literal, result.contains(literal))
        }
        assertEquals(result, format(result))
    }

    fun testLineAndNestedBlockCommentContentsAndOrderArePreserved() {
        val result = format("select A -- keep SELECT, FROM  \n,B /* outer\n  /* inner */ keep */ from EMP where A=1 -- last  ")
        assertTrue(result.contains("-- keep SELECT, FROM  \n"))
        assertTrue(result.contains("/* outer\n  /* inner */ keep */"))
        assertTrue(result.endsWith("-- last  "))
        assertTrue(result.indexOf("-- keep") < result.indexOf(", B"))
        assertTrue(result.indexOf("/* outer") < result.indexOf("FROM EMP"))
        assertEquals(result, format(result))
    }

    fun testMultipleStatementsAndSemicolonsInsideLiteralsAreKeptSeparate() {
        val result = format("select ';' as X from EMP;select ID from DEPT;")
        assertTrue(result.contains("';' AS X"))
        assertTrue(result.contains("FROM EMP; SELECT ID"))
        assertTrue(result.endsWith("DEPT;"))
        assertEquals(result, format(result))
    }

    fun testOperatorsAndQualifiedNamesPreserveTheirLexicalMeaning() {
        val result = format("select e.ID::INTEGER,e.VAL||'x',-1,COUNT(*) from EMP e where e.ID>=:id and e.ID<>0")
        assertTrue(result.contains("e.ID::INTEGER"))
        assertTrue(result.contains("e.VAL || 'x'"))
        assertTrue(result.contains(", -1"))
        assertTrue(result.contains("e.ID >= :id"))
        assertTrue(result.contains("e.ID <> 0"))
        assertEquals(result, format(result))
    }

    fun testCompoundOperatorsAreNotSplitOrMergedWithNamedParameters() {
        val result = format("select A from EMP where X<=>:value and Y!~*'abc' and Z&&:ids")
        assertTrue(result.contains("X <=> :value"))
        assertTrue(result.contains("Y !~* 'abc'"))
        assertTrue(result.contains("Z && :ids"))
        assertEquals(result, format(result))
    }

    fun testNumericAndAtParametersKeepTheirPrefixesAndCommentsStaySeparate() {
        val result = format("select :1,@value,@@session,?::INTEGER from EMP@remote where A=/* keep */:1 and B+@value>0")
        assertTrue(result.contains(":1"))
        assertTrue(result.contains("@value"))
        assertTrue(result.contains("@@session"))
        assertTrue(result.contains("?::INTEGER"))
        assertTrue(result.contains("EMP@remote"))
        assertTrue(result.contains("A = /* keep */ :1"))
        assertTrue(result.contains("B + @value > 0"))
        assertEquals(result, format(result))
    }

    fun testUnclosedQuotesCommentsParenthesesAndCaseAreRejectedWithoutPartialResult() {
        for (sql in listOf("select 'open", "select \"open", "select `open", "select [open", "select /* outer /* inner */",
            "select (A", "select A)", "select case when A=1 then B")) {
            val result = SqlFormatter.format(sql)
            assertNull(sql, result.sql)
            assertFalse(sql, result.problem.isNullOrBlank())
        }
    }

    fun testHugeAndDeepInputsAreSafelyRejected() {
        for (sql in listOf("select " + " ".repeat(262_144), "select " + "(".repeat(80) + "1" + ")".repeat(80),
            "select " + "A,".repeat(10_100))) {
            assertNull(SqlFormatter.format(sql).sql)
        }
        assertEquals(" \n\t", format(" \n\t"))
    }

    fun testIdentifierAndFunctionCaseArePreserved() {
        val result = format("select my_col,my_fn(my_col),count(*) from my_table")
        assertEquals("SELECT my_col, my_fn(my_col), count(*) FROM my_table", result)
        assertEquals("SELECT action, e.first FROM first AS last", format("select action,e.first from first as last"))
    }
}
