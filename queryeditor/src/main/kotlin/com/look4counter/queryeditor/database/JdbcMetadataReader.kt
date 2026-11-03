package com.look4counter.queryeditor.database

import com.intellij.openapi.progress.ProgressManager
import com.look4counter.queryeditor.settings.DataSourceDefinition
import com.look4counter.queryeditor.settings.DatabaseKind
import java.net.URLClassLoader
import java.nio.file.Files
import java.nio.file.Path
import java.sql.Connection
import java.sql.DatabaseMetaData
import java.sql.Driver
import java.util.Properties

data class TableMetadata(val catalog: String?, val schema: String?, val name: String, val columns: List<String>)
data class MetadataSnapshot(val tables: List<TableMetadata>) {
    val tableNames: List<String> = tables.map { it.name }.distinct().sorted()
    val columnNames: List<String> = tables.flatMap { it.columns }.distinct().sorted()
}

class JdbcMetadataReader {
    fun read(source: DataSourceDefinition, password: String?, checkCanceled: () -> Unit = { ProgressManager.checkCanceled() }): MetadataSnapshot =
        withConnection(source, password, checkCanceled) { readConnection(it, DatabaseKind.of(source), checkCanceled) }

    fun testConnection(source: DataSourceDefinition, password: String?, checkCanceled: () -> Unit = { ProgressManager.checkCanceled() }) {
        withConnection(source, password, checkCanceled) { checkCanceled() }
    }

    private fun <T> withConnection(source: DataSourceDefinition, password: String?, checkCanceled: () -> Unit,
                                   action: (Connection) -> T): T {
        checkCanceled()
        require(source.driverJars.isNotEmpty()) { "JDBC JAR required" }
        val urls = source.driverJars.map { Path.of(it).also { path -> require(Files.isRegularFile(path)) }.toUri().toURL() }.toTypedArray()
        val cleanupClass = JdbcDriverCleanup::class.java
        val cleanupBytes = requireNotNull(cleanupClass.getResourceAsStream("JdbcDriverCleanup.class")).use { it.readBytes() }
        val isolated = object : URLClassLoader(urls, Driver::class.java.classLoader) {
            override fun findClass(name: String): Class<*> = if (name == cleanupClass.name) {
                defineClass(name, cleanupBytes, 0, cleanupBytes.size)
            } else super.findClass(name)
        }
        isolated.use { loader ->
            val thread = Thread.currentThread()
            val previous = thread.contextClassLoader
            thread.contextClassLoader = loader
            try {
                val className = source.driverClass.ifBlank { DatabaseKind.of(source).driverClass }
                val driver = Class.forName(className, true, loader).getDeclaredConstructor().newInstance() as Driver
                val properties = Properties().apply {
                    if (source.username.isNotBlank()) setProperty("user", source.username)
                    if (password != null) setProperty("password", password)
                    when (DatabaseKind.of(source)) {
                        DatabaseKind.ORACLE -> { setProperty("oracle.net.CONNECT_TIMEOUT", "15000"); setProperty("oracle.jdbc.ReadTimeout", "30000") }
                        DatabaseKind.MYSQL, DatabaseKind.MARIADB -> { setProperty("connectTimeout", "15000"); setProperty("socketTimeout", "30000") }
                        DatabaseKind.POSTGRESQL -> { setProperty("connectTimeout", "15"); setProperty("socketTimeout", "30") }
                        DatabaseKind.IMPALA -> setProperty("SocketTimeout", "30")
                    }
                }
                checkCanceled()
                val connection = requireNotNull(driver.connect(source.jdbcUrl, properties)) { "Driver does not accept JDBC URL" }
                connection.use { checkCanceled(); return action(it) }
            } finally {
                try {
                    // Some drivers self-register from static initializers. Release those global references too.
                    runCatching { loader.loadClass(cleanupClass.name).getMethod("closeDrivers").invoke(null) }
                } finally { thread.contextClassLoader = previous }
            }
        }
    }

    fun readConnection(connection: Connection, kind: DatabaseKind,
                       checkCanceled: () -> Unit = { ProgressManager.checkCanceled() }): MetadataSnapshot =
        if (kind == DatabaseKind.ORACLE) readOracleConnection(connection, checkCanceled) else readConnection(connection, checkCanceled)

    private fun readOracleConnection(connection: Connection, checkCanceled: () -> Unit): MetadataSnapshot {
        val objects = linkedMapOf<Pair<String, String>, LinkedHashSet<String>>()
        fun query(sql: String, row: (java.sql.ResultSet) -> Unit) {
            checkCanceled()
            connection.createStatement().use { statement ->
                statement.queryTimeout = 30
                statement.fetchSize = 1000
                statement.executeQuery(sql).use { result ->
                    while (result.next()) { checkCanceled(); row(result) }
                }
            }
        }
        query("SELECT OWNER, TABLE_NAME FROM ALL_TABLES UNION SELECT OWNER, VIEW_NAME AS TABLE_NAME FROM ALL_VIEWS") {
            val owner = it.getString("OWNER") ?: return@query
            val name = it.getString("TABLE_NAME") ?: return@query
            objects.getOrPut(owner to name) { linkedSetOf() }
        }
        query("SELECT OWNER, TABLE_NAME, COLUMN_NAME FROM ALL_TAB_COLUMNS ORDER BY OWNER, TABLE_NAME, COLUMN_ID") {
            val key = it.getString("OWNER") to it.getString("TABLE_NAME")
            it.getString("COLUMN_NAME")?.let { column -> objects[key]?.add(column) }
        }
        checkCanceled()
        return MetadataSnapshot(objects.map { (key, columns) -> TableMetadata(null, key.first, key.second, columns.toList()) })
    }

    fun readConnection(connection: Connection, checkCanceled: () -> Unit = { ProgressManager.checkCanceled() }): MetadataSnapshot {
        val metadata = connection.metaData
        val catalog = connection.catalog
        val keys = linkedSetOf<Triple<String?, String?, String>>()
        metadata.getTables(catalog, null, "%", arrayOf("TABLE", "VIEW")).use { tables ->
            while (tables.next()) {
                checkCanceled()
                val name = tables.getString("TABLE_NAME") ?: continue
                keys.add(Triple(tables.getString("TABLE_CAT"), tables.getString("TABLE_SCHEM"), name))
            }
        }
        val tables = keys.map { (tableCatalog, schema, name) ->
            checkCanceled()
            val columns = linkedSetOf<String>()
            metadata.getColumns(tableCatalog, pattern(metadata, schema), pattern(metadata, name), "%").use { result ->
                while (result.next()) {
                    checkCanceled()
                    if (result.getString("TABLE_NAME") == name && result.getString("TABLE_CAT") == tableCatalog &&
                        result.getString("TABLE_SCHEM") == schema) result.getString("COLUMN_NAME")?.let(columns::add)
                }
            }
            TableMetadata(tableCatalog, schema, name, columns.toList())
        }
        checkCanceled()
        return MetadataSnapshot(tables)
    }

    private fun pattern(metadata: DatabaseMetaData, name: String?): String? {
        if (name == null) return null
        val escape = metadata.searchStringEscape.orEmpty()
        if (escape.isEmpty()) return name
        return name.replace(escape, escape + escape).replace("%", escape + "%").replace("_", escape + "_")
    }
}
