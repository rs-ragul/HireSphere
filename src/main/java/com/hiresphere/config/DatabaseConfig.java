package com.hiresphere.config;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database configuration manager.
 * Supports embedded SQLite (zero installation, instant execution)
 * and MySQL (XAMPP / standard production DB).
 */
public class DatabaseConfig {

    public enum DatabaseType {
        SQLITE,
        MYSQL
    }

    private static DatabaseType currentType = DatabaseType.SQLITE;
    private static String sqliteUrl = "jdbc:sqlite:hiresphere.db";
    private static String mysqlUrl = "jdbc:mysql://localhost:3306/hiresphere_db?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    private static String mysqlUser = "root";
    private static String mysqlPassword = "";

    static {
        loadProperties();
    }

    private static void loadProperties() {
        try (InputStream in = DatabaseConfig.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in != null) {
                Properties props = new Properties();
                props.load(in);
                String typeStr = props.getProperty("db.type", "sqlite").trim().toLowerCase();
                if ("mysql".equals(typeStr)) {
                    currentType = DatabaseType.MYSQL;
                } else {
                    currentType = DatabaseType.SQLITE;
                }
                sqliteUrl = props.getProperty("db.sqlite.url", sqliteUrl);
                mysqlUrl = props.getProperty("db.mysql.url", mysqlUrl);
                mysqlUser = props.getProperty("db.mysql.user", mysqlUser);
                mysqlPassword = props.getProperty("db.mysql.password", mysqlPassword);
            }
        } catch (Exception e) {
            System.err.println("[WARN] Could not load db.properties, using SQLite defaults: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        if (currentType == DatabaseType.MYSQL) {
            try {
                Class.forName("com.mysql.cj.jdbc.Driver");
            } catch (ClassNotFoundException e) {
                throw new SQLException("MySQL JDBC Driver not found. Ensure mysql-connector-j.jar is in classpath.", e);
            }
            return DriverManager.getConnection(mysqlUrl, mysqlUser, mysqlPassword);
        } else {
            try {
                Class.forName("org.sqlite.JDBC");
            } catch (ClassNotFoundException e) {
                throw new SQLException("SQLite JDBC Driver not found. Ensure sqlite-jdbc.jar is in classpath.", e);
            }
            return DriverManager.getConnection(sqliteUrl);
        }
    }

    public static DatabaseType getCurrentType() {
        return currentType;
    }

    public static void setDatabaseType(DatabaseType type) {
        currentType = type;
    }

    public static String getActiveDatabaseInfo() {
        if (currentType == DatabaseType.MYSQL) {
            return "MySQL (URL: " + mysqlUrl + ", User: " + mysqlUser + ")";
        } else {
            return "SQLite Embedded (File: hiresphere.db)";
        }
    }
}

