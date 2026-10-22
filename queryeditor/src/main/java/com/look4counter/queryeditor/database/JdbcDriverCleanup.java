package com.look4counter.queryeditor.database;

import java.sql.DriverManager;
import java.sql.SQLException;

/** Loaded inside the isolated driver loader so JDBC's caller visibility permits deregistration. */
public final class JdbcDriverCleanup {
    public static void closeDrivers() throws SQLException {
        var loader = JdbcDriverCleanup.class.getClassLoader();
        var drivers = DriverManager.getDrivers();
        while (drivers.hasMoreElements()) {
            var driver = drivers.nextElement();
            if (driver.getClass().getClassLoader() == loader) {
                DriverManager.deregisterDriver(driver);
            }
        }
    }
}
