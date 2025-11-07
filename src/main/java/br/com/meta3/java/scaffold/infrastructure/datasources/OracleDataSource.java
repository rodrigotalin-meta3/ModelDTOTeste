package br.com.meta3.java.scaffold.infrastructure.datasources;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Stub placeholder for legacy OracleDataSource with minimal setter and getConnection signature.
 * TODO: (REVIEW) Replace with proper OracleDataSource implementation or JNDI lookup when integrating legacy DB.
 */
public class OracleDataSource {

    private String url;

    /**
     * Sets the JDBC URL for the Oracle database.
     * @param url the JDBC connection URL
     */
    public void setURL(String url) {
        this.url = url;
    }

    /**
     * Retrieves a connection to the Oracle database using the provided credentials.
     * Fallbacks to DriverManager for legacy integration.
     *
     * @param user     the database username
     * @param password the database password
     * @return a new Connection instance
     * @throws SQLException if a database access error occurs or URL is not set
     */
    public Connection getConnection(String user, String password) throws SQLException {
        if (this.url == null || this.url.isEmpty()) {
            throw new SQLException("OracleDataSource URL is not configured");
        }
        // TODO: (REVIEW) Enhance with connection pooling or vendor DataSource when migrating fully
        return DriverManager.getConnection(this.url, user, password);
    }
}
