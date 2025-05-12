package com.pdfviewer.model.repository.db;

import com.pdfviewer.model.entity.RecentFile;
import com.pdfviewer.model.repository.RecentFileRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of the RecentFileRepository interface.
 * This class is responsible for persisting and retrieving RecentFile entities from the SQLite database.
 */
public class SqliteRecentFileRepository implements RecentFileRepository {
    private static final Logger logger = LoggerFactory.getLogger(SqliteRecentFileRepository.class);
    private final SqliteConnectionManager connectionManager;
    private final TransactionManager transactionManager;

    /**
     * Creates a new SqliteRecentFileRepository with the specified connection manager.
     *
     * @param connectionManager the connection manager to use
     */
    public SqliteRecentFileRepository(SqliteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.transactionManager = new TransactionManager(connectionManager);
    }

    @Override
    public Optional<RecentFile> findById(String id) throws SQLException {
        String sql = "SELECT * FROM recent_files WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToRecentFile(rs));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public Optional<RecentFile> findByFilePath(String filePath) throws SQLException {
        String sql = "SELECT * FROM recent_files WHERE file_path = ?";
        logger.debug("Looking for recent file with path: {}", filePath);

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, filePath);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    RecentFile recentFile = mapResultSetToRecentFile(rs);
                    logger.debug("Found recent file: {}", recentFile);
                    return Optional.of(recentFile);
                } else {
                    logger.debug("No recent file found for path: {}", filePath);
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public List<RecentFile> findAll() throws SQLException {
        String sql = "SELECT * FROM recent_files ORDER BY last_opened DESC";
        List<RecentFile> recentFiles = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                recentFiles.add(mapResultSetToRecentFile(rs));
            }
        }

        return recentFiles;
    }

    @Override
    public List<RecentFile> findAllOrderByLastOpenedDesc(int limit) throws SQLException {
        String sql = "SELECT * FROM recent_files ORDER BY last_opened DESC LIMIT ?";
        List<RecentFile> recentFiles = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, limit);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    recentFiles.add(mapResultSetToRecentFile(rs));
                }
            }
        }

        return recentFiles;
    }

    @Override
    public RecentFile save(RecentFile recentFile) throws SQLException {
        if (existsById(recentFile.getId())) {
            return update(recentFile);
        } else {
            return insert(recentFile);
        }
    }

    @Override
    public void deleteById(String id) throws SQLException {
        String sql = "DELETE FROM recent_files WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            int rowsAffected = stmt.executeUpdate();

            logger.debug("Deleted {} recent file(s) with ID: {}", rowsAffected, id);
        }
    }

    @Override
    public boolean existsById(String id) throws SQLException {
        String sql = "SELECT 1 FROM recent_files WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public boolean updateLastViewedInfo(String fileId, int lastPageViewed, float lastZoomLevel) throws SQLException {
        String sql = "UPDATE recent_files SET last_page_viewed = ?, last_zoom_level = ?, last_opened = ? WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, lastPageViewed);
            stmt.setFloat(2, lastZoomLevel);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setString(4, fileId);

            int rowsAffected = stmt.executeUpdate();
            logger.debug("Updated last viewed info for file ID {}: page={}, zoom={}, rows affected={}", 
                        fileId, lastPageViewed, lastZoomLevel, rowsAffected);

            return rowsAffected > 0;
        }
    }

    @Override
    public int deleteOldFiles(int keepCount) throws SQLException {
        String sql = "DELETE FROM recent_files WHERE id NOT IN (SELECT id FROM recent_files ORDER BY last_opened DESC LIMIT ?)";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, keepCount);
            int rowsAffected = stmt.executeUpdate();

            logger.debug("Deleted {} old recent file(s), keeping {} most recent", rowsAffected, keepCount);
            return rowsAffected;
        }
    }

    /**
     * Inserts a new recent file into the database.
     *
     * @param recentFile the recent file to insert
     * @return the inserted recent file
     * @throws SQLException if a database access error occurs
     */
    private RecentFile insert(RecentFile recentFile) throws SQLException {
        String sql = "INSERT INTO recent_files (id, file_path, file_name, last_opened, page_count, last_page_viewed, last_zoom_level) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?)";

        return transactionManager.executeInTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, recentFile.getId());
                stmt.setString(2, recentFile.getFilePath());
                stmt.setString(3, recentFile.getFileName());
                stmt.setTimestamp(4, Timestamp.valueOf(recentFile.getLastOpened()));
                stmt.setInt(5, recentFile.getPageCount());
                stmt.setInt(6, recentFile.getLastPageViewed());
                stmt.setFloat(7, recentFile.getLastZoomLevel());

                int rowsAffected = stmt.executeUpdate();
                logger.debug("Inserted {} recent file(s) with ID: {}", rowsAffected, recentFile.getId());

                return recentFile;
            } catch (SQLException e) {
                logger.error("Failed to insert recent file: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Updates an existing recent file in the database.
     *
     * @param recentFile the recent file to update
     * @return the updated recent file
     * @throws SQLException if a database access error occurs
     */
    private RecentFile update(RecentFile recentFile) throws SQLException {
        String sql = "UPDATE recent_files SET file_path = ?, file_name = ?, last_opened = ?, " +
                     "page_count = ?, last_page_viewed = ?, last_zoom_level = ? WHERE id = ?";

        return transactionManager.executeInTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, recentFile.getFilePath());
                stmt.setString(2, recentFile.getFileName());
                stmt.setTimestamp(3, Timestamp.valueOf(recentFile.getLastOpened()));
                stmt.setInt(4, recentFile.getPageCount());
                stmt.setInt(5, recentFile.getLastPageViewed());
                stmt.setFloat(6, recentFile.getLastZoomLevel());
                stmt.setString(7, recentFile.getId());

                int rowsAffected = stmt.executeUpdate();
                logger.debug("Updated {} recent file(s) with ID: {}", rowsAffected, recentFile.getId());

                return recentFile;
            } catch (SQLException e) {
                logger.error("Failed to update recent file: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Maps a ResultSet row to a RecentFile entity.
     *
     * @param rs the ResultSet to map
     * @return the mapped RecentFile entity
     * @throws SQLException if a database access error occurs
     */
    private RecentFile mapResultSetToRecentFile(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String filePath = rs.getString("file_path");
        String fileName = rs.getString("file_name");
        LocalDateTime lastOpened = rs.getTimestamp("last_opened").toLocalDateTime();
        int pageCount = rs.getInt("page_count");
        int lastPageViewed = rs.getInt("last_page_viewed");
        float lastZoomLevel = rs.getFloat("last_zoom_level");

        return new RecentFile.Builder()
                .id(id)
                .filePath(filePath)
                .fileName(fileName)
                .lastOpened(lastOpened)
                .pageCount(pageCount)
                .lastPageViewed(lastPageViewed)
                .lastZoomLevel(lastZoomLevel)
                .build();
    }
}