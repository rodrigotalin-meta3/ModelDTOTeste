package br.com.meta3.java.scaffold.infrastructure.datasources;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * Stub placeholder for legacy SQLServerDataSource with minimal setters and getConnection signature.
 * TODO: (REVIEW) Replace with proper SQLServer DataSource implementation or JNDI lookup when integrating legacy DB.
 */
public class SQLServerDataSource {

    private String user;
    private String password;
    private int portNumber;
    private String serverName;
    private String databaseName;

    /**
     * Sets the database username.
     * @param user the SQL Server username
     */
    public void setUser(String user) {
        this.user = user;
    }

    /**
     * Sets the database password.
     * @param password the SQL Server password
     */
    public void setPassword(String password) {
        this.password = password;
    }

    /**
     * Sets the TCP port number for the SQL Server instance.
     * @param portNumber the port number (e.g., 1433)
     */
    public void setPortNumber(int portNumber) {
        this.portNumber = portNumber;
    }

    /**
     * Sets the hostname or IP address of the SQL Server instance.
     * @param serverName the server name or IP
     */
    public void setServerName(String serverName) {
        this.serverName = serverName;
    }

    /**
     * Sets the target database name on the SQL Server instance.
     * @param databaseName the database name
     */
    public void setDatabaseName(String databaseName) {
        this.databaseName = databaseName;
    }

    /**
     * Retrieves a connection to the SQL Server database using the provided configuration.
     * Falls back to DriverManager for legacy integration.
     *
     * @return a new Connection instance
     * @throws SQLException if a database access error occurs or any config is missing
     */
    public Connection getConnection() throws SQLException {
        // TODO: (REVIEW) Validate fields and provide more descriptive errors
        if (serverName == null || serverName.isEmpty()) {
            throw new SQLException("SQLServerDataSource serverName is not configured");
        }
        if (databaseName == null || databaseName.isEmpty()) {
            throw new SQLException("SQLServerDataSource databaseName is not configured");
        }
        String url = String.format("jdbc:sqlserver://%s:%d;databaseName=%s",
                serverName, portNumber, databaseName);
        // TODO: (REVIEW) Consider supporting integratedSecurity and other connection properties
        return DriverManager.getConnection(url, user, password);
    }
}
