package br.com.meta3.java.scaffold.application.services;

import br.com.meta3.java.scaffold.config.LegacyDataSourceProperties;
import br.com.meta3.java.scaffold.infrastructure.datasources.OracleDataSource;
import br.com.meta3.java.scaffold.infrastructure.datasources.SQLServerDataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Service responsible for establishing connections to the legacy database
 * (Oracle or SQL Server) based on configured properties.
 */
@Service
public class LegacyDatabaseService {

    private static final Logger logger = LoggerFactory.getLogger(LegacyDatabaseService.class);

    private final LegacyDataSourceProperties legacyProps;

    public LegacyDatabaseService(LegacyDataSourceProperties legacyProps) {
        this.legacyProps = legacyProps;
    }

    /**
     * Create and return a Connection to the legacy database.
     *
     * @return a live {@link Connection}
     * @throws IllegalStateException if unable to connect or unsupported type
     */
    public Connection getConnection() {
        try {
            String tipo = legacyProps.getTipobanco();
            if ("oracle".equalsIgnoreCase(tipo)) {
                OracleDataSource ds = new OracleDataSource();
                ds.setURL(legacyProps.getUrloracle());
                // TODO: (REVIEW) Placeholder OracleDataSource: no pooling, falls back to DriverManager
                return ds.getConnection(legacyProps.getUseroracle(), legacyProps.getPasswordoracle());

            } else if ("sqlserver".equalsIgnoreCase(tipo)) {
                try {
                    // Load legacy driver; may be required by placeholder implementation
                    Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
                } catch (ClassNotFoundException e) {
                    logger.error("SQLServer Driver class not found, proceeding with DriverManager fallback", e);
                }

                SQLServerDataSource ds = new SQLServerDataSource();
                ds.setUser(legacyProps.getUsersqlserver());
                ds.setPassword(legacyProps.getPasswordsqlserver());
                // TODO: (REVIEW) Hardcoded SQLServer connection details; consider externalizing port/server/db
                ds.setPortNumber(1434);
                ds.setServerName("172.30.0.4");
                ds.setDatabaseName("seutransporte");
                return ds.getConnection();

            } else {
                throw new IllegalStateException("Unsupported legacy database type: " + tipo);
            }
        } catch (SQLException ex) {
            logger.error("Failed to connect to legacy database", ex);
            throw new IllegalStateException("Could not connect to legacy database", ex);
        }
    }
}
