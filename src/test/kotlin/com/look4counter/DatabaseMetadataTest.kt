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

    fun testAllSourcesDistinctInvalidationAndStaleRefresh() {
        val first = DataSourceDefinition("one", "One", "jdbc:postgresql://one/db")
        val second = DataSourceDefinition("two", "Two", "jdbc:mysql://two/db")
        settings.replaceDataSources(listOf(first, second))
        val service = DatabaseMetadataService.getInstance(project)
        val snapshot = MetadataSnapshot(listOf(TableMetadata(null, null, "EMP", listOf("ID"))))
        val requests = service.beginRefresh()
        assertEquals(listOf("one", "two"), requests.map { it.source.id })
        assertTrue(service.complete(requests[0], snapshot))
        assertTrue(service.complete(requests[1], MetadataSnapshot(listOf(
            TableMetadata(null, null, "EMP", listOf("ID", "NAME")),
            TableMetadata(null, null, "DEPT", listOf("ID"))))))
        assertEquals(listOf("DEPT", "EMP"), service.snapshot()!!.tableNames)
        assertEquals(listOf("ID", "NAME"), service.snapshot()!!.columnNames)
        // A failed/canceled refresh never calls complete and retains the last successful snapshot.
        service.beginRefresh()
        assertEquals(listOf("DEPT", "EMP"), service.snapshot()!!.tableNames)
        val older = service.beginRefresh().first()
        val newer = service.beginRefresh().first()
        assertFalse(service.complete(older, snapshot))
        assertTrue(service.complete(newer, snapshot))
        settings.replaceDataSources(listOf(first.copy(username = "changed"), second))
        assertNull(service.snapshot())
        assertFalse(service.complete(newer, snapshot))
        settings.replaceDataSources(listOf(second))
        assertEquals(listOf("two"), service.beginRefresh().map { it.source.id })
        settings.replaceDataSources(emptyList())
        assertEmpty(service.beginRefresh())
    }

    fun testStartupWithoutEditorLoadsAllSourcesAndIsolatesFailures() {
        val directory = java.nio.file.Files.createTempDirectory("glue-startup-driver")
        val jar = directory.resolve("driver.jar")
        try {
            val resource = "com/look4counter/FixtureJdbcDriver.class"
            java.util.jar.JarOutputStream(java.nio.file.Files.newOutputStream(jar)).use { output ->
                output.putNextEntry(java.util.zip.ZipEntry(resource))
                requireNotNull(javaClass.classLoader.getResourceAsStream(resource)).use { it.copyTo(output) }
                output.closeEntry()
            }
            val good = DataSourceDefinition("one", "One", "jdbc:glue-test:public",
                "com.look4counter.FixtureJdbcDriver", "", false, "POSTGRESQL", mutableListOf(jar.toString()))
            settings.replaceDataSources(listOf(good.copy(id = "broken", driverClass = "missing.Driver"),
                good, good.copy(id = "two")))
            val service = DatabaseMetadataService.getInstance(project)
            kotlinx.coroutines.runBlocking {
                com.look4counter.queryeditor.database.DatabaseMetadataStartupActivity().execute(project)
            }
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching(
                "startup metadata", { !service.loading && service.snapshot() != null }, 10)
            assertEquals(1, service.failureCount)
            assertEquals(listOf("EMP"), service.snapshot()!!.tableNames)
            assertEquals(listOf("ID"), service.snapshot()!!.columnNames)
            assertEquals(0, com.intellij.openapi.fileEditor.FileEditorManager.getInstance(project).openFiles.size)

            settings.replaceDataSources(listOf(good.copy(id = "replacement")))
            com.intellij.testFramework.PlatformTestUtil.waitWithEventsDispatching(
                "settings metadata", { !service.loading && service.snapshot() != null }, 10)
            assertEquals(0, service.failureCount)
            assertEquals(listOf("EMP"), service.snapshot()!!.tableNames)
            settings.replaceDataSources(emptyList())
            assertFalse(service.loading)
            assertNull(service.snapshot())
        } finally {
            settings.replaceDataSources(emptyList())
            listOf("glue.test.closed", "glue.test.deregistered").forEach(System::clearProperty)
            java.nio.file.Files.deleteIfExists(jar)
            java.nio.file.Files.delete(directory)
        }
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
