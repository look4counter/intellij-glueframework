package com.look4counter.queryeditor.settings

enum class DatabaseKind(val label: String, val driverClass: String, val urlPrefix: String, val downloadUrl: String) {
    ORACLE("Oracle", "oracle.jdbc.OracleDriver", "jdbc:oracle:",
        "https://repo.maven.apache.org/maven2/com/oracle/database/jdbc/ojdbc11/23.6.0.24.10/ojdbc11-23.6.0.24.10.jar"),
    MYSQL("MySQL", "com.mysql.cj.jdbc.Driver", "jdbc:mysql:",
        "https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.6.0/mysql-connector-j-9.6.0.jar"),
    MARIADB("MariaDB", "org.mariadb.jdbc.Driver", "jdbc:mariadb:",
        "https://repo.maven.apache.org/maven2/org/mariadb/jdbc/mariadb-java-client/3.5.7/mariadb-java-client-3.5.7.jar"),
    POSTGRESQL("PostgreSQL", "org.postgresql.Driver", "jdbc:postgresql:",
        "https://repo.maven.apache.org/maven2/org/postgresql/postgresql/42.7.10/postgresql-42.7.10.jar"),
    IMPALA("Impala", "com.cloudera.impala.jdbc.Driver", "jdbc:impala:", "");

    override fun toString(): String = label

    companion object {
        fun of(source: DataSourceDefinition): DatabaseKind = entries.firstOrNull { it.name == source.databaseKind }
            ?: entries.firstOrNull { source.jdbcUrl.startsWith(it.urlPrefix, ignoreCase = true) } ?: ORACLE
    }
}
