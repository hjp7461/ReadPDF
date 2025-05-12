package com.pdfviewer.model.repository.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Manages connections to the SQLite database.
 * This class is responsible for creating and managing database connections,
 * as well as initializing the database schema.
 */
public class SqliteConnectionManager {
    private static final Logger logger = LoggerFactory.getLogger(SqliteConnectionManager.class);
    private static final String DB_FILE_NAME = "pdfviewer.db";
    private static final AtomicBoolean initialized = new AtomicBoolean(false);

    private final String dbUrl;
    private final Path dbPath;

    /**
     * Creates a new SqliteConnectionManager with the default database path.
     * The default path is the user's home directory + .pdfviewer/pdfviewer.db
     */
    public SqliteConnectionManager() {
        this(Paths.get(System.getProperty("user.home"), ".pdfviewer", DB_FILE_NAME));
    }

    /**
     * Creates a new SqliteConnectionManager with the specified database path.
     * 
     * @param dbPath the path to the database file
     */
    public SqliteConnectionManager(Path dbPath) {
        this.dbPath = dbPath;
        this.dbUrl = "jdbc:sqlite:" + dbPath.toString();

        // Ensure the parent directory exists
        if (!dbPath.getParent().toFile().exists()) {
            if (!dbPath.getParent().toFile().mkdirs()) {
                logger.error("Failed to create directory for database: {}", dbPath.getParent());
                throw new RuntimeException("Failed to create directory for database");
            }
        }

        // Initialize the database if it hasn't been initialized yet
        if (initialized.compareAndSet(false, true)) {
            try {
                initializeDatabase();
            } catch (SQLException e) {
                logger.error("Failed to initialize database", e);
                throw new RuntimeException("Failed to initialize database", e);
            }
        }
    }

    /**
     * Gets a connection to the database.
     * 
     * @return a connection to the database
     * @throws SQLException if a database access error occurs
     */
    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(dbUrl);
    }

    /**
     * Initializes the database schema.
     * 
     * @throws SQLException if a database access error occurs
     */
    private void initializeDatabase() throws SQLException {
        logger.info("Initializing database at {}", dbPath);

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            // Create recent_files table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS recent_files (
                    id VARCHAR PRIMARY KEY,
                    file_path VARCHAR NOT NULL,
                    file_name VARCHAR NOT NULL,
                    last_opened TIMESTAMP NOT NULL,
                    page_count INTEGER NOT NULL,
                    last_page_viewed INTEGER NOT NULL DEFAULT 1,
                    last_zoom_level FLOAT NOT NULL DEFAULT 100.0
                )
            """);

            // Create bookmarks table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bookmarks (
                    id VARCHAR PRIMARY KEY,
                    file_id VARCHAR NOT NULL,
                    page_number INTEGER NOT NULL,
                    name VARCHAR NOT NULL,
                    description VARCHAR,
                    created_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (file_id) REFERENCES recent_files(id)
                )
            """);

            // Create annotations table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS annotations (
                    id VARCHAR PRIMARY KEY,
                    file_id VARCHAR NOT NULL,
                    page_number INTEGER NOT NULL,
                    type VARCHAR NOT NULL,
                    content VARCHAR,
                    x FLOAT NOT NULL,
                    y FLOAT NOT NULL,
                    width FLOAT,
                    height FLOAT,
                    color VARCHAR,
                    created_at TIMESTAMP NOT NULL,
                    FOREIGN KEY (file_id) REFERENCES recent_files(id)
                )
            """);

            // Create user_settings table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS user_settings (
                    key VARCHAR PRIMARY KEY,
                    value VARCHAR NOT NULL,
                    updated_at TIMESTAMP NOT NULL
                )
            """);

            // Create ocr_cache table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS ocr_cache (
                    file_id VARCHAR NOT NULL,
                    page_number INTEGER NOT NULL,
                    text_content TEXT,
                    created_at TIMESTAMP NOT NULL,
                    PRIMARY KEY (file_id, page_number),
                    FOREIGN KEY (file_id) REFERENCES recent_files(id)
                )
            """);

            logger.info("Database initialization completed successfully");
        }
    }

    /**
     * Gets the path to the database file.
     * 
     * @return the path to the database file
     */
    public Path getDbPath() {
        return dbPath;
    }
}