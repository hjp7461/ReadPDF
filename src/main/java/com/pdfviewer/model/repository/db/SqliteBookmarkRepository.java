package com.pdfviewer.model.repository.db;

import com.pdfviewer.model.entity.Bookmark;
import com.pdfviewer.model.repository.BookmarkRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * SQLite implementation of the BookmarkRepository interface.
 * This class is responsible for persisting and retrieving Bookmark entities from the SQLite database.
 */
public class SqliteBookmarkRepository implements BookmarkRepository {
    private static final Logger logger = LoggerFactory.getLogger(SqliteBookmarkRepository.class);
    private final SqliteConnectionManager connectionManager;
    private final TransactionManager transactionManager;

    /**
     * Creates a new SqliteBookmarkRepository with the specified connection manager.
     *
     * @param connectionManager the connection manager to use
     */
    public SqliteBookmarkRepository(SqliteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
        this.transactionManager = new TransactionManager(connectionManager);
    }

    @Override
    public Optional<Bookmark> findById(String id) throws SQLException {
        String sql = "SELECT * FROM bookmarks WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapResultSetToBookmark(rs));
                }
            }
        }

        return Optional.empty();
    }

    @Override
    public List<Bookmark> findAll() throws SQLException {
        String sql = "SELECT * FROM bookmarks ORDER BY created_at DESC";
        List<Bookmark> bookmarks = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                bookmarks.add(mapResultSetToBookmark(rs));
            }
        }

        return bookmarks;
    }

    @Override
    public Bookmark save(Bookmark bookmark) throws SQLException {
        if (existsById(bookmark.getId())) {
            return update(bookmark);
        } else {
            return insert(bookmark);
        }
    }

    @Override
    public void deleteById(String id) throws SQLException {
        String sql = "DELETE FROM bookmarks WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            int rowsAffected = stmt.executeUpdate();

            logger.debug("Deleted {} bookmark(s) with ID: {}", rowsAffected, id);
        }
    }

    @Override
    public boolean existsById(String id) throws SQLException {
        String sql = "SELECT 1 FROM bookmarks WHERE id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);

            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    @Override
    public List<Bookmark> findAllByFileId(String fileId) throws SQLException {
        String sql = "SELECT * FROM bookmarks WHERE file_id = ? ORDER BY page_number, created_at";
        List<Bookmark> bookmarks = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fileId);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bookmarks.add(mapResultSetToBookmark(rs));
                }
            }
        }

        return bookmarks;
    }

    @Override
    public List<Bookmark> findAllByFileIdAndPageNumber(String fileId, int pageNumber) throws SQLException {
        String sql = "SELECT * FROM bookmarks WHERE file_id = ? AND page_number = ? ORDER BY created_at";
        List<Bookmark> bookmarks = new ArrayList<>();

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fileId);
            stmt.setInt(2, pageNumber);

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    bookmarks.add(mapResultSetToBookmark(rs));
                }
            }
        }

        return bookmarks;
    }

    @Override
    public int deleteAllByFileId(String fileId) throws SQLException {
        String sql = "DELETE FROM bookmarks WHERE file_id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fileId);
            int rowsAffected = stmt.executeUpdate();

            logger.debug("Deleted {} bookmark(s) for file ID: {}", rowsAffected, fileId);
            return rowsAffected;
        }
    }

    @Override
    public int countByFileId(String fileId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM bookmarks WHERE file_id = ?";

        try (Connection conn = connectionManager.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, fileId);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }

        return 0;
    }

    /**
     * Inserts a new bookmark into the database.
     *
     * @param bookmark the bookmark to insert
     * @return the inserted bookmark
     * @throws SQLException if a database access error occurs
     */
    private Bookmark insert(Bookmark bookmark) throws SQLException {
        String sql = "INSERT INTO bookmarks (id, file_id, page_number, name, description, created_at) VALUES (?, ?, ?, ?, ?, ?)";

        return transactionManager.executeInTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, bookmark.getId());
                stmt.setString(2, bookmark.getFileId());
                stmt.setInt(3, bookmark.getPageNumber());
                stmt.setString(4, bookmark.getName());
                stmt.setString(5, bookmark.getDescription());
                stmt.setTimestamp(6, Timestamp.valueOf(bookmark.getCreatedAt()));

                int rowsAffected = stmt.executeUpdate();
                logger.debug("Inserted {} bookmark(s) with ID: {}", rowsAffected, bookmark.getId());

                return bookmark;
            } catch (SQLException e) {
                logger.error("Failed to insert bookmark: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Updates an existing bookmark in the database.
     *
     * @param bookmark the bookmark to update
     * @return the updated bookmark
     * @throws SQLException if a database access error occurs
     */
    private Bookmark update(Bookmark bookmark) throws SQLException {
        String sql = "UPDATE bookmarks SET file_id = ?, page_number = ?, name = ?, description = ? WHERE id = ?";

        return transactionManager.executeInTransaction(conn -> {
            try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, bookmark.getFileId());
                stmt.setInt(2, bookmark.getPageNumber());
                stmt.setString(3, bookmark.getName());
                stmt.setString(4, bookmark.getDescription());
                stmt.setString(5, bookmark.getId());

                int rowsAffected = stmt.executeUpdate();
                logger.debug("Updated {} bookmark(s) with ID: {}", rowsAffected, bookmark.getId());

                return bookmark;
            } catch (SQLException e) {
                logger.error("Failed to update bookmark: {}", e.getMessage(), e);
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Maps a ResultSet row to a Bookmark entity.
     *
     * @param rs the ResultSet to map
     * @return the mapped Bookmark entity
     * @throws SQLException if a database access error occurs
     */
    private Bookmark mapResultSetToBookmark(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String fileId = rs.getString("file_id");
        int pageNumber = rs.getInt("page_number");
        String name = rs.getString("name");
        String description = rs.getString("description");
        LocalDateTime createdAt = rs.getTimestamp("created_at").toLocalDateTime();

        return new Bookmark.Builder()
                .id(id)
                .fileId(fileId)
                .pageNumber(pageNumber)
                .name(name)
                .description(description)
                .createdAt(createdAt)
                .build();
    }
}