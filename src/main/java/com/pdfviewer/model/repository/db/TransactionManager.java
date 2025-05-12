package com.pdfviewer.model.repository.db;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.function.Function;

/**
 * Utility class for managing database transactions.
 * Provides methods to execute operations within a transaction with automatic commit and rollback.
 */
public class TransactionManager {
    private static final Logger logger = LoggerFactory.getLogger(TransactionManager.class);
    private final SqliteConnectionManager connectionManager;

    /**
     * Creates a new TransactionManager with the specified connection manager.
     * 
     * @param connectionManager the connection manager to use
     */
    public TransactionManager(SqliteConnectionManager connectionManager) {
        this.connectionManager = connectionManager;
    }

    /**
     * Executes the given operation within a transaction.
     * The transaction is automatically committed if the operation completes successfully,
     * or rolled back if an exception is thrown.
     * 
     * @param operation the operation to execute within the transaction
     * @param <T> the return type of the operation
     * @return the result of the operation
     * @throws SQLException if a database access error occurs
     */
    public <T> T executeInTransaction(Function<Connection, T> operation) throws SQLException {
        Connection connection = null;
        boolean originalAutoCommit = true;

        try {
            connection = connectionManager.getConnection();
            originalAutoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);

            T result = operation.apply(connection);

            connection.commit();
            return result;
        } catch (SQLException e) {
            if (connection != null) {
                try {
                    logger.warn("Rolling back transaction due to error: {}", e.getMessage());
                    connection.rollback();
                } catch (SQLException rollbackEx) {
                    logger.error("Failed to rollback transaction", rollbackEx);
                    throw new SQLException("Failed to rollback transaction", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (connection != null) {
                try {
                    connection.setAutoCommit(originalAutoCommit);
                    connection.close();
                } catch (SQLException closeEx) {
                    logger.error("Failed to close connection", closeEx);
                }
            }
        }
    }

    /**
     * Executes the given operation within a transaction without returning a result.
     * The transaction is automatically committed if the operation completes successfully,
     * or rolled back if an exception is thrown.
     * 
     * @param operation the operation to execute within the transaction
     * @throws SQLException if a database access error occurs
     */
    public void executeInTransactionWithoutResult(SqlConsumer<Connection> operation) throws SQLException {
        executeInTransaction(connection -> {
            try {
                operation.accept(connection);
                return null;
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Functional interface for operations that accept a Connection and don't return a result.
     * 
     * @param <T> the type of the input to the operation
     */
    @FunctionalInterface
    public interface SqlConsumer<T> {
        /**
         * Performs this operation on the given argument.
         * 
         * @param t the input argument
         * @throws SQLException if a database access error occurs
         */
        void accept(T t) throws SQLException;
    }
}
