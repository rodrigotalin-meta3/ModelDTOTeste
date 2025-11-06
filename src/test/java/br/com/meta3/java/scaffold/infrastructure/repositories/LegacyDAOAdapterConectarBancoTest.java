filetype
package br.com.meta3.java.scaffold.infrastructure.repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.env.Environment;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Properties;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LegacyDAOAdapter.conectarBanco().
 *
 * Goals / coverage:
 * - Verify the adapter emulates a connection (connected flag) when Environment is absent or no tipoBanco configured.
 * - Verify the adapter handles missing JDBC drivers / "no suitable driver" scenarios gracefully:
 *     - prints the legacy-style message to System.out
 *     - throws UnsupportedOperationException with legacy message
 *     - does not leave JDBC resources set
 * - Verify a successful connection path using a test-registered fake JDBC Driver so no external DB is required.
 *
 * Notes / test decisions:
 * - We use Mockito to mock Spring's Environment and to provide properties for the adapter.
 * - To simulate a successful JDBC connection without an external DB we register a lightweight fake Driver
 *   with DriverManager that responds to a test URL and returns a Mockito Connection whose createStatement()
 *   returns a Mockito Statement. This avoids needing Mockito's inline mocking of DriverManager static calls.
 * - Reflection is used to inspect private adapter fields (con, stmt) to ensure resources are set/cleared.
 *
 * TODO: (REVIEW) When LegacyDBProperties or a typed configuration bean is introduced, prefer wiring that bean
 * instead of mocking Environment in tests.
 */
public class LegacyDAOAdapterConectarBancoTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    public void testConectarBanco_emulatedWhenEnvNull_setsConnectedAndNoJdbcResources() throws Exception {
        LegacyDAOAdapter adapter = new LegacyDAOAdapter(); // env == null path

        // Sanity: ensure no exception and emulated connection is established
        assertDoesNotThrow(adapter::conectarBanco, "conectarBanco should not throw when env is null (emulated)");

        assertTrue(adapter.isConnected(), "Adapter should be marked connected in emulated mode");

        // Inspect private fields to ensure no JDBC Connection/Statement were created
        Field conField = LegacyDAOAdapter.class.getDeclaredField("con");
        Field stmtField = LegacyDAOAdapter.class.getDeclaredField("stmt");
        conField.setAccessible(true);
        stmtField.setAccessible(true);
        Object internalCon = conField.get(adapter);
        Object internalStmt = stmtField.get(adapter);

        assertNull(internalCon, "Internal Connection should be null in emulated mode");
        assertNull(internalStmt, "Internal Statement should be null in emulated mode");
    }

    @Test
    public void testConectarBanco_withEmptyTipoBanco_property_emulatesConnection() throws Exception {
        Environment env = mock(Environment.class);
        when(env.getProperty("legacydb.tipobanco", "")).thenReturn("   "); // empty after trim

        LegacyDAOAdapter adapter = new LegacyDAOAdapter(env);

        assertDoesNotThrow(adapter::conectarBanco, "conectarBanco should not throw when tipoBanco is empty (emulated)");
        assertTrue(adapter.isConnected(), "Adapter should be connected (emulated) when legacydb.tipobanco is not configured");

        // Internal JDBC resources must not be allocated in emulation path
        Field conField = LegacyDAOAdapter.class.getDeclaredField("con");
        Field stmtField = LegacyDAOAdapter.class.getDeclaredField("stmt");
        conField.setAccessible(true);
        stmtField.setAccessible(true);
        assertNull(conField.get(adapter), "Connection should be null for emulated connection");
        assertNull(stmtField.get(adapter), "Statement should be null for emulated connection");
    }

    @Test
    public void testConectarBanco_oracleMissingDriver_printsLegacyMessage_andThrowsUnsupportedOperation() throws Exception {
        // Configure Environment to request Oracle connection with an invalid/unresolvable URL.
        Environment env = mock(Environment.class);
        when(env.getProperty("legacydb.tipobanco", "")).thenReturn("oracle");
        // Provide a URL that will not match any driver on the test classpath -> DriverManager.getConnection will throw SQLException
        when(env.getProperty("legacydb.oracle.url")).thenReturn("jdbc:oracle:thin:@invalidhost:1521:XE");
        when(env.getProperty("legacydb.oracle.user")).thenReturn("user");
        when(env.getProperty("legacydb.oracle.password")).thenReturn("pass");

        LegacyDAOAdapter adapter = new LegacyDAOAdapter(env);

        // Capture System.out to assert legacy-style message printed
        System.setOut(new PrintStream(outContent));

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, adapter::conectarBanco,
                "Expected UnsupportedOperationException when no suitable Oracle driver is available or connection fails");

        String printed = outContent.toString();
        // Legacy message text included in the adapter's catch paths
        assertTrue(printed.contains("Não foi possivel conectar com o banco de dados") ||
                   printed.contains("Não foi possivel conectar com o banco de dados:"), "System.out should contain legacy connection failure text");

        // Adapter should not be marked connected after a failed attempt
        assertFalse(adapter.isConnected(), "Adapter should not be connected after failed connection attempt");

        // Ensure internal JDBC resources are still null
        Field conField = LegacyDAOAdapter.class.getDeclaredField("con");
        Field stmtField = LegacyDAOAdapter.class.getDeclaredField("stmt");
        conField.setAccessible(true);
        stmtField.setAccessible(true);
        assertNull(conField.get(adapter), "Connection should remain null after failed connection attempt");
        assertNull(stmtField.get(adapter), "Statement should remain null after failed connection attempt");

        // Exception message should include the legacy Portuguese text (per adapter behavior)
        assertTrue(ex.getMessage().contains("Não foi possivel conectar com o banco de dados"),
                "Thrown exception should contain legacy connection error message");
    }

    @Test
    public void testConectarBanco_sqlserverMissingDriver_printsLegacyMessage_andThrowsUnsupportedOperation() throws Exception {
        // Configure Environment to request SQL Server connection with an invalid/unresolvable URL.
        Environment env = mock(Environment.class);
        when(env.getProperty("legacydb.tipobanco", "")).thenReturn("sqlserver");
        // Intentionally do not provide a valid URL; adapter will build a URL and DriverManager will fail to find a driver
        when(env.getProperty("legacydb.sqlserver.url")).thenReturn("jdbc:sqlserver://invalidserver:1434;databaseName=nonexistent");
        when(env.getProperty("legacydb.sqlserver.user")).thenReturn("sa");
        when(env.getProperty("legacydb.sqlserver.password")).thenReturn("pass");

        LegacyDAOAdapter adapter = new LegacyDAOAdapter(env);

        // Capture System.out
        System.setOut(new PrintStream(outContent));

        UnsupportedOperationException ex = assertThrows(UnsupportedOperationException.class, adapter::conectarBanco,
                "Expected UnsupportedOperationException when no suitable SQLServer driver is available or connection fails");

        String printed = outContent.toString();
        assertTrue(printed.contains("Não foi possivel conectar com o banco de dados"),
                "System.out should contain legacy connection failure text for SQLServer");

        assertFalse(adapter.isConnected(), "Adapter should not be connected after failed SQLServer connection attempt");

        Field conField = LegacyDAOAdapter.class.getDeclaredField("con");
        Field stmtField = LegacyDAOAdapter.class.getDeclaredField("stmt");
        conField.setAccessible(true);
        stmtField.setAccessible(true);
        assertNull(conField.get(adapter), "Connection should remain null after failed SQLServer connection attempt");
        assertNull(stmtField.get(adapter), "Statement should remain null after failed SQLServer connection attempt");

        assertTrue(ex.getMessage().contains("Não foi possivel conectar com o banco de dados"),
                "Thrown exception should contain legacy connection error message for SQLServer");
    }

    @Test
    public void testConectarBanco_successfulSqlServerConnection_setsConnectionAndStatement_andCleansOnDesconectar() throws Exception {
        // We'll simulate a successful JDBC connection by registering a lightweight fake Driver that
        // accepts a specific URL and returns a Mockito Connection. This avoids external DB dependency.
        final String testUrl = "jdbc:sqlserver://testserver:1434;databaseName=testdb";

        // Prepare a mock Connection and Statement to be returned by our fake driver
        Connection mockConn = mock(Connection.class);
        Statement mockStmt = mock(Statement.class);
        when(mockConn.createStatement()).thenReturn(mockStmt);

        // Create a fake Driver implementation that delegates connect() to return our mock Connection when URL matches
        Driver fakeDriver = new Driver() {
            @Override
            public Connection connect(String url, Properties info) throws SQLException {
                if (acceptsURL(url)) {
                    return mockConn;
                }
                return null;
            }

            @Override
            public boolean acceptsURL(String url) throws SQLException {
                return testUrl.equals(url);
            }

            @Override
            public DriverPropertyInfo[] getPropertyInfo(String url, Properties info) throws SQLException {
                return new DriverPropertyInfo[0];
            }

            @Override
            public int getMajorVersion() {
                return 1;
            }

            @Override
            public int getMinorVersion() {
                return 0;
            }

            @Override
            public boolean jdbcCompliant() {
                return false;
            }

            // Java 7+ method
            @Override
            public Logger getParentLogger() {
                return Logger.getLogger("LegacyDAOAdapterTestFakeDriver");
            }
        };

        // Register the fake driver with DriverManager
        DriverManager.registerDriver(fakeDriver);
        try {
            // Mock Environment to request sqlserver and provide the exact URL our fake driver accepts
            Environment env = mock(Environment.class);
            when(env.getProperty("legacydb.tipobanco", "")).thenReturn("sqlserver");
            when(env.getProperty("legacydb.sqlserver.url")).thenReturn(testUrl);
            when(env.getProperty("legacydb.sqlserver.user")).thenReturn(null);
            when(env.getProperty("legacydb.sqlserver.password")).thenReturn(null);

            LegacyDAOAdapter adapter = new LegacyDAOAdapter(env);

            // Should not throw and should mark adapter connected and set internal connection/statement
            assertDoesNotThrow(adapter::conectarBanco, "conectarBanco should succeed using the registered fake Driver");

            assertTrue(adapter.isConnected(), "Adapter should be marked connected after successful connection");

            // Inspect private fields
            Field conField = LegacyDAOAdapter.class.getDeclaredField("con");
            Field stmtField = LegacyDAOAdapter.class.getDeclaredField("stmt");
            conField.setAccessible(true);
            stmtField.setAccessible(true);

            Object internalCon = conField.get(adapter);
            Object internalStmt = stmtField.get(adapter);

            assertNotNull(internalCon, "Internal Connection should be set after successful conectarBanco");
            assertNotNull(internalStmt, "Internal Statement should be set after successful conectarBanco");

            // Ensure the Connection/Statement set are the same instances produced by our mocks
            assertSame(mockConn, internalCon, "Internal Connection should be the same instance returned by fake Driver");
            assertSame(mockStmt, internalStmt, "Internal Statement should be the same instance created from the Connection");

            // Now call desconectarBanco which should attempt to close resources. Ensure no exception.
            assertDoesNotThrow(adapter::desconectarBanco, "desconectarBanco should succeed and close resources");

            // Verify the adapter attempted to close the connection and statement (we can at least verify connection.close() was called)
            verify(mockStmt, atLeastOnce()).close();
            verify(mockConn, atLeastOnce()).close();

            // After disconnect, adapter state must be cleared
            assertFalse(adapter.isConnected(), "Adapter should not be connected after desconectarBanco");
            assertNull(conField.get(adapter), "Internal Connection should be null after desconectarBanco");
            assertNull(stmtField.get(adapter), "Internal Statement should be null after desconectarBanco");
        } finally {
            // Unregister our fake driver to avoid interfering with other tests / driver list
            try {
                DriverManager.deregisterDriver(fakeDriver);
            } catch (SQLException ignore) {
            }
        }
    }
}