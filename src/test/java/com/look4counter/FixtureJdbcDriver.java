package com.look4counter;

import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

/** Isolated, network-free JDBC implementation used by the loader integration test. */
public class FixtureJdbcDriver implements Driver {
    static {
        try {
            DriverManager.registerDriver(new FixtureJdbcDriver(), () -> System.setProperty("glue.test.deregistered", "true"));
        } catch (SQLException e) { throw new ExceptionInInitializerError(e); }
    }

    @Override public Connection connect(String url, Properties info) {
        if (!acceptsURL(url)) return null;
        if (!url.endsWith("public") && !"secret-value".equals(info.getProperty("password"))) throw new IllegalStateException("Password not supplied");
        if (Thread.currentThread().getContextClassLoader() != getClass().getClassLoader()) throw new IllegalStateException("Wrong context loader");
        DatabaseMetaData metadata = (DatabaseMetaData) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{DatabaseMetaData.class}, (proxy, method, args) -> {
            return switch (method.getName()) {
                case "getTables" -> rows(false);
                case "getColumns" -> {
                    if (Boolean.getBoolean("glue.test.fail")) throw new SQLException("Do not expose secret-value");
                    yield rows(true);
                }
                case "getSearchStringEscape" -> "\\";
                default -> throw new UnsupportedOperationException(method.getName());
            };
        });
        return (Connection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{Connection.class}, (proxy, method, args) -> switch (method.getName()) {
            case "getMetaData" -> metadata;
            case "getCatalog" -> "db";
            case "close" -> { System.setProperty("glue.test.closed", "true"); yield null; }
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    private ResultSet rows(boolean columns) {
        int[] index = {-1};
        return (ResultSet) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[]{ResultSet.class}, (proxy, method, args) -> switch (method.getName()) {
            case "next" -> ++index[0] == 0;
            case "getString" -> switch ((String) args[0]) {
                case "TABLE_CAT" -> "db";
                case "TABLE_SCHEM" -> "public";
                case "TABLE_NAME" -> "EMP";
                case "COLUMN_NAME" -> "ID";
                default -> null;
            };
            case "close" -> null;
            default -> throw new UnsupportedOperationException(method.getName());
        });
    }

    @Override public boolean acceptsURL(String url) { return url.startsWith("jdbc:glue-test:"); }
    @Override public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) { return new DriverPropertyInfo[0]; }
    @Override public int getMajorVersion() { return 1; }
    @Override public int getMinorVersion() { return 0; }
    @Override public boolean jdbcCompliant() { return false; }
    @Override public Logger getParentLogger() { return Logger.getGlobal(); }
}
