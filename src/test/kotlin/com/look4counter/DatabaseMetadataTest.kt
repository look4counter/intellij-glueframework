package com.look4counter

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.look4counter.queryeditor.database.DatabaseMetadataService
import com.look4counter.queryeditor.database.JdbcMetadataReader
import com.look4counter.queryeditor.database.MetadataSnapshot
import com.look4counter.queryeditor.database.TableMetadata
import com.look4counter.queryeditor.settings.DataSourceDefinition
import com.look4counter.queryeditor.settings.DataSourceSettingsService
import java.lang.reflect.Proxy
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.ResultSet

class DatabaseMetadataTest : BasePlatformTestCase() {
    private val settings get() = DataSourceSettingsService.getInstance(project)
    override fun tearDown() {
        try { settings.replaceDataSources(emptyList()) } finally { super.tearDown() }
    }

    private inline fun <reified T> proxy(crossinline call: (String, Array<out Any?>) -> Any?): T =
        Proxy.newProxyInstance(T::class.java.classLoader, arrayOf(T::class.java)) { _, method, args ->
            call(method.name, args ?: emptyArray())
        } as T

    fun testMetadataKeepsRelationsEscapesPatternsDeduplicatesAndClosesResults() {
        val tableRows = listOf(
            mapOf("TABLE_CAT" to "db", "TABLE_SCHEM" to "a_b", "TABLE_NAME" to "EMP_%"),
            mapOf("TABLE_CAT" to "db", "TABLE_SCHEM" to "a_b", "TABLE_NAME" to "EMP_%"),
            mapOf("TABLE_CAT" to "db", "TABLE_SCHEM" to "other", "TABLE_NAME" to "EMP_%"))
        var closed = 0
        fun result(rows: List<Map<String, String>>): ResultSet {
            var index = -1
            return proxy { name, args -> when (name) {
                "next" -> ++index < rows.size
                "getString" -> rows[index][args[0]]
                "close" -> { closed++; null }
                else -> error("Unexpected result call: $name")
            } }
        }
        val metadata = proxy<DatabaseMetaData> { name, args -> when (name) {
            "getTables" -> result(tableRows)
            "getSearchStringEscape" -> "\\"
            "getColumns" -> {
                assertEquals("EMP\\_\\%", args[2])
                val schema = if (args[1] == "a\\_b") "a_b" else "other"
                result(listOf("ID", "ID", "NAME").map {
                    mapOf("TABLE_CAT" to "db", "TABLE_SCHEM" to schema, "TABLE_NAME" to "EMP_%", "COLUMN_NAME" to it)
                } + mapOf("TABLE_CAT" to "db", "TABLE_SCHEM" to schema, "TABLE_NAME" to "EMP_X", "COLUMN_NAME" to "WRONG"))
            }
            else -> error("Unexpected metadata call: $name")
        } }
        val connection = proxy<Connection> { name, _ -> when (name) {
            "getMetaData" -> metadata
            "getCatalog" -> "db"
            else -> error("Unexpected connection call: $name")
        } }
        val snapshot = JdbcMetadataReader().readConnection(connection) {}
        assertEquals(2, snapshot.tables.size)
        assertEquals(listOf("EMP_%"), snapshot.tableNames)
        assertEquals(listOf("ID", "NAME"), snapshot.columnNames)
        assertTrue(snapshot.tables.all { it.columns == listOf("ID", "NAME") })
        assertEquals(3, closed)
        var checks = 0
        assertThrows(IllegalStateException::class.java) {
            JdbcMetadataReader().readConnection(connection) { if (++checks == 2) error("cancel") }
        }
        assertEquals(4, closed)
    }

    fun testCacheSelectionInvalidationAndStaleRefresh() {
        val first = DataSourceDefinition("one", "One", "jdbc:postgresql://one/db")
        val second = DataSourceDefinition("two", "Two", "jdbc:mysql://two/db")
        settings.replaceDataSources(listOf(first, second))
        val service = DatabaseMetadataService.getInstance(project)
        service.selectedId = "one"
        val snapshot = MetadataSnapshot(listOf(TableMetadata(null, null, "EMP", listOf("ID"))))
        assertTrue(service.complete(service.beginRefresh()!!, snapshot))
        assertEquals(snapshot, service.selectedSnapshot())
        val stale = service.beginRefresh()!!
        service.selectedId = "two"
        assertFalse(service.complete(stale, snapshot))
        assertNull(service.selectedSnapshot())
        service.selectedId = "one"
        assertEquals(snapshot, service.selectedSnapshot())
        // A failed/canceled refresh never calls complete and retains the last successful snapshot.
        service.beginRefresh()
        assertEquals(snapshot, service.selectedSnapshot())
        val older = service.beginRefresh()!!
        val newer = service.beginRefresh()!!
        assertFalse(service.complete(older, snapshot))
        assertTrue(service.complete(newer, snapshot))
        settings.replaceDataSources(listOf(first.copy(username = "changed"), second))
        assertNull(service.selectedSnapshot())
        assertFalse(service.complete(newer, snapshot))
        settings.replaceDataSources(listOf(second))
        assertEquals("", service.selectedId)
        assertNull(service.beginRefresh())
    }

    fun testLocalJarDriverConnectionPasswordCleanupAndFailure() {
        val directory = java.nio.file.Files.createTempDirectory("glue-local-driver")
        val jar = directory.resolve("driver.jar")
        try {
            val classPath = "com/look4counter/FixtureJdbcDriver.class"
            java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(jar)).use { output ->
                output.putNextEntry(java.util.zip.ZipEntry(classPath))
                requireNotNull(javaClass.classLoader.getResourceAsStream(classPath)).use { it.copyTo(output) }
                output.closeEntry()
            }
            val source = DataSourceDefinition("local", "Local", "jdbc:glue-test:db", "com.look4counter.FixtureJdbcDriver",
                "user", true, "POSTGRESQL", mutableListOf(jar.toString()))
            val originalLoader = Thread.currentThread().contextClassLoader
            val reader = JdbcMetadataReader()
            val snapshot = reader.read(source, "secret-value") {}
            assertEquals(listOf("EMP"), snapshot.tableNames)
            assertEquals(listOf("ID"), snapshot.columnNames)
            assertEquals("true", System.getProperty("glue.test.closed"))
            assertEquals("true", System.getProperty("glue.test.deregistered"))
            assertSame(originalLoader, Thread.currentThread().contextClassLoader)
            System.clearProperty("glue.test.closed")
            System.clearProperty("glue.test.deregistered")
            System.setProperty("glue.test.fail", "true")
            assertThrows(java.sql.SQLException::class.java) { reader.read(source, "secret-value") {} }
            assertEquals("true", System.getProperty("glue.test.closed"))
            assertEquals("true", System.getProperty("glue.test.deregistered"))
            assertSame(originalLoader, Thread.currentThread().contextClassLoader)
        } finally {
            listOf("glue.test.closed", "glue.test.deregistered", "glue.test.fail").forEach(System::clearProperty)
            java.nio.file.Files.delete(jar)
            java.nio.file.Files.delete(directory)
        }
    }
}
