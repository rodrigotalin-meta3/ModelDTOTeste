filetype
package br.com.meta3.java.scaffold.infrastructure.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * LegacyDAOAdapter - placeholder adapter to contain and document legacy DAO calls.
 *
 * Purpose:
 * - Expose the minimal legacy-compatible API surface used by migrated code:
 *     - conectarBanco()
 *     - executarQuery(String sql)
 *     - desconectarBanco()
 * - Provide a simple, safe stub implementation that keeps legacy dependencies discoverable
 *   and allows incremental replacement with a production implementation later.
 *
 * Design decisions / notes:
 * - The original legacy DAO exposed a java.sql.ResultSet-like API (e.g. banco.rs.next(), banco.rs.getObject()).
 *   Recreating a full ResultSet wrapper would be non-trivial and unnecessary for the initial migration step.
 *   Therefore this adapter intentionally offers a List<Map<String,Object>>-based result holder (rsList) and
 *   a small set of helpers. This keeps the contract explicit while encouraging callers to migrate to JPA/DTOs.
 *
 * - Methods are no-ops / return empty results by default to preserve safety (no database access).
 *   Future iterations can:
 *     - inject EntityManager or DataSource and implement real native/JDBC execution
 *     - provide a ResultSet wrapper compatible with legacy code that expects banco.rs semantics
 *
 * - To support staged migration and tests that still use legacy JDBC resources, this adapter exposes
 *   optional JDBC resource fields (Connection, Statement, PreparedStatement, ResultSet) and setter helpers.
 *   These are intended for tests/migration harnesses to inject mock or real JDBC resources temporarily.
 *
 * TODO: (REVIEW)
 * - Replace this stub with a proper implementation that executes SQL via EntityManager/DataSource
 *   and maps results into a ResultSet-like wrapper or List<Map<String,Object>> depending on migration strategy.
 * - If legacy code heavily relies on ResultSet.next()/getObject(), implement a thin ResultSet-adapter that
 *   delegates to an underlying JDBC ResultSet to minimize code changes in the migrated code.
 */
@Repository
public class LegacyDAOAdapter {

    private static final Logger log = LoggerFactory.getLogger(LegacyDAOAdapter.class);

    // Simple connected flag to emulate conectarBanco / desconectarBanco lifecycle.
    private final AtomicBoolean connected = new AtomicBoolean(false);

    // Last executed query (for debugging / future use)
    private volatile String lastQuery;

    // Minimal result holder that replaces legacy ResultSet for the migration phase.
    // Each map represents a row with column-name -> value entries.
    // By default is an immutable empty list.
    private volatile List<Map<String, Object>> rsList = Collections.emptyList();

    /*
     * Optional JDBC resource fields to aid migration/tests.
     *
     * These are nullable and intended to be set by migration/test code that still operates
     * on raw JDBC resources. The adapter's desconectarBanco() will attempt to close them
     * if they are non-null.
     *
     * NOTE: These fields do not change the primary migration approach (List<Map<String,Object>> result holder).
     * They are provided only to make it easier to integrate with legacy snippets during incremental migration.
     */
    private volatile Connection con;
    private volatile Statement stmt;
    private volatile PreparedStatement pstmt;
    private volatile ResultSet rs;

    // Optional Environment to read legacy DB configuration from application properties.
    // We intentionally do not depend on a specific LegacyDBProperties type here to keep this adapter
    // compile-safe in the incremental migration (the concrete LegacyDBProperties can be introduced later).
    private final Environment env;

    public LegacyDAOAdapter() {
        // Intentionally lightweight. Do not attempt DB access here.
        this.env = null;
    }

    /**
     * Constructor used by Spring to inject environment properties.
     *
     * Note:
     * - We allow tests / legacy code to instantiate the adapter without Spring (using the default ctor).
     * - When env is null we keep the original emulation/no-op behavior to avoid forcing configuration in tests.
     */
    @Autowired
    public LegacyDAOAdapter(Environment env) {
        this.env = env;
    }

    /**
     * Migrate and enhance the legacy conectarBanco() behavior.
     *
     * Behavior introduced:
     * - If a property legacydb.tipobanco is provided in the Environment, attempt to open a real JDBC Connection
     *   using DriverManager and the configured URL/credentials.
     * - Supported values for legacydb.tipobanco (case-insensitive): "oracle", "sqlserver".
     * - For Oracle: attempt to Class.forName("oracle.jdbc.OracleDriver") (best-effort) then connect using
     *   legacydb.oracle.url / legacydb.oracle.user / legacydb.oracle.password.
     * - For SQL Server: attempt to Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver") (best-effort),
     *   then connect using legacydb.sqlserver.url / legacydb.sqlserver.user / legacydb.sqlserver.password.
     *   If sqlserver.url is not provided, try to build a URL from legacydb.sqlserver.server, port and database.
     *
     * Fallbacks / safety:
     * - If env is null or legacydb.tipobanco is not set, keep the previous emulated behavior: mark adapter as connected
     *   but do not open a real connection (safe default for tests and early migration stages).
     * - All SQLExceptions are handled to preserve legacy semantics:
     *     - log via SLF4J
     *     - print a legacy-style System.out message
     *     - rethrow UnsupportedOperationException with legacy message
     *
     * Important decisions:
     * - We opted to use DriverManager for a minimal, dependency-free approach. In further iterations this should be
     *   replaced by an injected DataSource or a proper JPA/EntityManager migration.
     * - We do not reference a concrete LegacyDBProperties class to avoid introducing new compile-time dependencies
     *   in this incremental migration step. If a dedicated configuration bean is added later, adapt this method to use it.
     *
     * TODO: (REVIEW)
     * - Replace DriverManager usage with an injected DataSource and remove direct driver Class.forName calls.
     * - Provide a strongly-typed LegacyDBProperties and prefer it over raw Environment lookups.
     */
    public void conectarBanco() {
        // If no environment provided, preserve emulated behavior (safe for tests)
        if (this.env == null) {
            connected.set(true);
            log.debug("LegacyDAOAdapter.conectarBanco() called - connection emulated because no Environment is available");
            return;
        }

        String tipoBanco = env.getProperty("legacydb.tipobanco", "").trim().toLowerCase();
        if (tipoBanco.isEmpty()) {
            // No configuration provided for legacy DB: keep the original emulated behavior to avoid forcing configuration.
            connected.set(true);
            log.debug("LegacyDAOAdapter.conectarBanco() called - no legacydb.tipobanco configured -> connection emulated (no-op)");
            return;
        }

        try {
            if ("oracle".equals(tipoBanco)) {
                // Attempt to load a commonly-used Oracle JDBC driver class - best-effort only.
                // Note: if the driver is provided via DataSource on the classpath this is often unnecessary,
                // but attempting Class.forName is a pragmatic migration step to mimic legacy behavior.
                try {
                    Class.forName("oracle.jdbc.OracleDriver");
                } catch (ClassNotFoundException cnfe) {
                    // Log at debug level and continue: DriverManager may still find a suitable driver via SPI.
                    log.debug("Oracle JDBC driver class not found via Class.forName - continuing and letting DriverManager try to resolve driver: {}", cnfe.getMessage());
                }

                String url = env.getProperty("legacydb.oracle.url");
                String user = env.getProperty("legacydb.oracle.user");
                String password = env.getProperty("legacydb.oracle.password");

                if (url == null || url.trim().isEmpty()) {
                    throw new SQLException("Oracle connection URL (legacydb.oracle.url) not configured");
                }

                // Use DriverManager to obtain a Connection to mirror legacy JDBC behavior.
                this.con = (user != null) ? DriverManager.getConnection(url, user, password)
                                          : DriverManager.getConnection(url);
                this.stmt = this.con.createStatement();
                connected.set(true);
                log.debug("LegacyDAOAdapter connected to Oracle via URL='{}' (user set={})", maskUrlForLog(url), user != null);
                return;
            }

            if ("sqlserver".equals(tipoBanco) || "mssql".equals(tipoBanco)) {
                // Attempt to load modern MS SQL Server driver
                try {
                    Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                } catch (ClassNotFoundException cnfe) {
                    // Try legacy driver class name used in some old environments
                    try {
                        Class.forName("com.microsoft.jdbc.sqlserver.SQLServerDriver");
                    } catch (ClassNotFoundException cnfe2) {
                        // Not fatal: DriverManager may still locate a driver via SPI; just log for diagnostics.
                        log.debug("SQLServer JDBC driver classes not found via Class.forName - continuing and letting DriverManager try to resolve driver. tried-modern='{}' tried-legacy='{}'",
                                "com.microsoft.sqlserver.jdbc.SQLServerDriver", "com.microsoft.jdbc.sqlserver.SQLServerDriver");
                    }
                }

                String url = env.getProperty("legacydb.sqlserver.url");
                String user = env.getProperty("legacydb.sqlserver.user");
                String password = env.getProperty("legacydb.sqlserver.password");

                if (url == null || url.trim().isEmpty()) {
                    // Build a conservative default URL using sensible defaults mirroring the legacy code
                    String server = env.getProperty("legacydb.sqlserver.server", "172.30.0.4");
                    String port = env.getProperty("legacydb.sqlserver.port", "1434");
                    String database = env.getProperty("legacydb.sqlserver.database", "seutransporte");
                    url = "jdbc:sqlserver://" + server + ":" + port + ";databaseName=" + database;
                }

                this.con = (user != null) ? DriverManager.getConnection(url, user, password)
                                          : DriverManager.getConnection(url);
                this.stmt = this.con.createStatement();
                connected.set(true);
                log.debug("LegacyDAOAdapter connected to SQLServer via URL='{}' (user set={})", maskUrlForLog(url), user != null);
                return;
            }

            // Unknown tipoBanco - emulate connection but log warning
            connected.set(true);
            log.warn("LegacyDAOAdapter.conectarBanco() - unknown legacydb.tipobanco='{}' - connection emulated (no real JDBC connection attempted)", tipoBanco);

        } catch (SQLException ex) {
            // Preserve legacy-style error handling: log, print System.out message, and rethrow UnsupportedOperationException
            log.error("LegacyDAOAdapter.conectarBanco - SQL error while attempting to connect (tipoBanco={})", tipoBanco, ex);
            System.out.println("--> SISDIGITACAO > LegacyDAOAdapter > conectarBanco > Não foi possivel conectar com o banco de dados: " + ex);
            throw new UnsupportedOperationException("Não foi possivel conectar com o banco de dados!");
        } catch (Exception ex) {
            // Defensive: any unexpected exception -> behave similarly
            log.error("LegacyDAOAdapter.conectarBanco - unexpected error while attempting to connect (tipoBanco={})", tipoBanco, ex);
            System.out.println("--> SISDIGITACAO > LegacyDAOAdapter > conectarBanco > Não foi possivel conectar com o banco de dados: " + ex);
            throw new UnsupportedOperationException("Não foi possivel conectar com o banco de dados!");
        }
    }

    /**
     * Emulate executing a SQL query.
     *
     * Current behavior:
     * - records the provided SQL into lastQuery
     * - resets rsList to an empty list (safe default)
     *
     * Note:
     * - Legacy callers expected to read banco.rs after calling executarQuery(...).
     *   During migration, callers should be adapted to use the typed DAOs in infrastructure.repositories
     *   (e.g., RecadastramentoDAO, UsuarioDAO) that return strongly-typed results.
     *
     * TODO: (REVIEW)
     * - Implement real execution: use injected EntityManager or JDBC DataSource to run the SQL and populate rsList.
     * - Optionally provide a ResultSet-like wrapper to support legacy iteration patterns without touching many call sites.
     *
     * @param sql the SQL to execute (legacy/native SQL)
     */
    public void executarQuery(String sql) {
        this.lastQuery = sql;
        // Safe default: return empty result set to preserve legacy behavior (no exceptions thrown).
        this.rsList = Collections.emptyList();
        log.debug("LegacyDAOAdapter.executarQuery() called - sql='{}' - returning empty result (stub)", sql);
    }

    /**
     * Emulate disconnecting from the legacy DB.
     *
     * Current behavior: clears connected flag and clears lastQuery/rsList to avoid holding memory.
     *
     * Updated behavior during migration:
     * - Attempt to close any injected JDBC resources (ResultSet, PreparedStatement, Statement, Connection)
     *   if they are non-null.
     * - For any SQLException encountered while closing resources: log via SLF4J, print to System.out (to mimic legacy),
     *   and rethrow an UnsupportedOperationException preserving the original message (to preserve legacy behavior).
     *
     * Implementation notes / decisions:
     * - Closing order: ResultSet -> PreparedStatement -> Statement -> Connection (typical safe order).
     * - Each close is attempted in its own try/catch to allow best-effort cleanup of remaining resources.
     * - After successful close (or even on failure) resources are nulled to avoid reuse.
     * - This method still clears internal adapter state (lastQuery, rsList, connected flag) as before.
     *
     * TODO: (REVIEW) When a proper datasource/EntityManager-based implementation is provided, remove JDBC fields.
     */
    public void desconectarBanco() {
        try {
            // Close ResultSet if present
            if (this.rs != null) {
                try {
                    this.rs.close();
                } catch (SQLException ex) {
                    // Log and rethrow as legacy behavior required
                    log.error("LegacyDAOAdapter.desconectarBanco - error closing ResultSet", ex);
                    System.out.println("--> SISDIGITACAO > LegacyDAOAdapter > desconectarBanco > Erro ao desconectar ResultSet: " + ex);
                    throw new UnsupportedOperationException("Erro ao desconectar do Banco!\n\n" + ex.getMessage());
                } finally {
                    this.rs = null;
                }
            }

            // Close PreparedStatement if present
            if (this.pstmt != null) {
                try {
                    this.pstmt.close();
                } catch (SQLException ex) {
                    log.error("LegacyDAOAdapter.desconectarBanco - error closing PreparedStatement", ex);
                    System.out.println("--> SISDIGITACAO > LegacyDAOAdapter > desconectarBanco > Erro ao desconectar PreparedStatement: " + ex);
                    throw new UnsupportedOperationException("Erro ao desconectar do Banco!\n\n" + ex.getMessage());
                } finally {
                    this.pstmt = null;
                }
            }

            // Close Statement if present
            if (this.stmt != null) {
                try {
                    this.stmt.close();
                } catch (SQLException ex) {
                    log.error("LegacyDAOAdapter.desconectarBanco - error closing Statement", ex);
                    System.out.println("--> SISDIGITACAO > LegacyDAOAdapter > desconectarBanco > Erro ao desconectar Statement: " + ex);
                    throw new UnsupportedOperationException("Erro ao desconectar do Banco!\n\n" + ex.getMessage());
                } finally {
                    this.stmt = null;
                }
            }

            // Close Connection if present
            if (this.con != null) {
                try {
                    this.con.close();
                } catch (SQLException ex) {
                    log.error("LegacyDAOAdapter.desconectarBanco - error closing Connection", ex);
                    System.out.println("--> SISDIGITACAO > LegacyDAOAdapter > desconectarBanco > Erro ao desconectar Connection: " + ex);
                    throw new UnsupportedOperationException("Erro ao desconectar do Banco!\n\n" + ex.getMessage());
                } finally {
                    this.con = null;
                }
            }

            // Clear adapter state as before
            connected.set(false);
            lastQuery = null;
            rsList = Collections.emptyList();
            log.debug("LegacyDAOAdapter.desconectarBanco() completed - resources closed / state cleared");
        } catch (UnsupportedOperationException uoe) {
            // Per legacy semantics, rethrow after logging (already logged above)
            throw uoe;
        } catch (Exception ex) {
            // Defensive: any unexpected exception -> log and wrap similarly to legacy behavior
            log.error("LegacyDAOAdapter.desconectarBanco - unexpected error during cleanup", ex);
            System.out.println("--> SISDIGITACAO > LegacyDAOAdapter > desconectarBanco > Erro ao desconectar do Banco: " + ex);
            throw new UnsupportedOperationException("Erro ao desconectar do Banco!\n\n" + ex.getMessage());
        }
    }

    /*
     * Setter helpers for injection / tests: allow test/migration harness to set JDBC resources
     * so that desconectarBanco() can clean them up properly. These helpers intentionally do
     * not attempt to close previously set resources to keep behavior explicit and predictable.
     *
     * TODO: (REVIEW) If needed, consider making these atomic swap-and-close helpers to avoid leaks when
     * a previously injected resource should be replaced.
     */

    /**
     * Inject a JDBC Connection for migration/tests.
     * @param con the Connection to set (may be null)
     */
    public void setConnection(Connection con) {
        this.con = con;
    }

    /**
     * Inject a JDBC Statement for migration/tests.
     * @param stmt the Statement to set (may be null)
     */
    public void setStatement(Statement stmt) {
        this.stmt = stmt;
    }

    /**
     * Inject a JDBC PreparedStatement for migration/tests.
     * @param pstmt the PreparedStatement to set (may be null)
     */
    public void setPreparedStatement(PreparedStatement pstmt) {
        this.pstmt = pstmt;
    }

    /**
     * Inject a JDBC ResultSet for migration/tests.
     * @param rs the ResultSet to set (may be null)
     */
    public void setResultSet(ResultSet rs) {
        this.rs = rs;
    }

    /*
     * Helper accessors to inspect adapter state during migration / tests.
     * These are intentionally public to support integration tests and incremental migration steps.
     */

    /**
     * Return whether conectarBanco() was called and the adapter is considered "connected".
     * @return true if connected (emulated)
     */
    public boolean isConnected() {
        return connected.get();
    }

    /**
     * Return the last executed query string (may be null).
     * @return lastQuery
     */
    public String getLastQuery() {
        return lastQuery;
    }

    /**
     * Return the last query results as a List of row-maps.
     * Default is an empty list. Future implementations will populate this with actual DB rows.
     *
     * Legacy note: original code exposed a ResultSet (banco.rs). Migrated code should avoid depending on
     * ResultSet semantics and instead use repository methods that return typed DTOs or entities.
     *
     * @return list of rows (each row is a Map columnName -> value)
     */
    public List<Map<String, Object>> getRsList() {
        return rsList;
    }

    /**
     * Replace the current rsList with a provided value (useful for tests or staged migration wiring).
     * @param rows the rows to set as the current result
     */
    public void setRsList(List<Map<String, Object>> rows) {
        this.rsList = rows == null ? Collections.emptyList() : rows;
    }

    /**
     * Small helper to mask potentially sensitive URL details in logs while still showing structure.
     */
    private String maskUrlForLog(String url) {
        if (url == null) return "null";
        // Very conservative masking: replace credentials-looking patterns if present
        return url.replaceAll("(?i)(user=)[^;]+", "$1****")
                  .replaceAll("(?i)(password=)[^;]+", "$1****");
    }
}