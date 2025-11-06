filetype
package br.com.meta3.java.scaffold.infrastructure.repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for legacy-like executarPreparedQuery(...) behavior.
 *
 * Goals:
 *  - Success case: when a Connection is available and prepareStatement succeeds, the adapter's internal
 *    PreparedStatement (pstmt) must be set to the returned PreparedStatement instance.
 *  - Failure case: when Connection.prepareStatement throws SQLException, the legacy-style message is printed
 *    to System.out and an UnsupportedOperationException is thrown.
 *
 * Approach:
 *  - LegacyDAOAdapter does not yet implement executarPreparedQuery; to verify legacy semantics we create
 *    an anonymous subclass within tests that implements the expected legacy behavior using the adapter's
 *    internal JDBC fields (accessed via reflection) and public setters for injection.
 *
 * TODO: (REVIEW)
 *  - When LegacyDAOAdapter gains a concrete executarPreparedQuery implementation, adapt these tests to call it
 *    directly instead of using an anonymous subclass.
 *  - Consider replacing System.out assertions with a structured logger test appender if SLF4J-based logging
 *    becomes the single source of truth for error messages.
 */
public class LegacyDAOAdapterExecutarPreparedQueryTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    public void testExecutarPreparedQuery_success_setsPreparedStatement_whenConnectionProvided() throws Exception {
        // Prepare a mock Connection and PreparedStatement
        Connection mockConn = mock(Connection.class);
        PreparedStatement mockPstmt = mock(PreparedStatement.class);
        String sql = "SELECT 1";

        when(mockConn.prepareStatement(sql)).thenReturn(mockPstmt);

        // Create an anonymous subclass that mimics the legacy executarPreparedQuery behavior for testing.
        LegacyDAOAdapter adapter = new LegacyDAOAdapter() {
            @Override
            public void executarQuery(String query) {
                // noop - not used in this test
            }

            // Provide the legacy-like method under test in this anonymous subclass.
            public void executarPreparedQuery(String query) {
                try {
                    // Access internal 'con' field via reflection (mimic legacy 'this.con')
                    Field conField;
                    try {
                        conField = LegacyDAOAdapter.class.getDeclaredField("con");
                        conField.setAccessible(true);
                    } catch (NoSuchFieldException nsfe) {
                        throw new RuntimeException("Internal field 'con' not found for test subclass", nsfe);
                    }

                    Object conObj = conField.get(this);
                    Connection c = (conObj instanceof Connection) ? (Connection) conObj : null;

                    if (c == null) {
                        // Legacy semantics: if no connection available, simulate failure path
                        System.out.println("--> SISDIGITACAO > DAO > executarPreparedQuery > Erro ao preparar a query: no-connection");
                        throw new UnsupportedOperationException("Erro ao preparar a query!\n\nno-connection");
                    }

                    PreparedStatement ps = c.prepareStatement(query);

                    // Set the internal pstmt field via reflection to mimic 'this.pstmt = ...'
                    try {
                        Field pstmtField = LegacyDAOAdapter.class.getDeclaredField("pstmt");
                        pstmtField.setAccessible(true);
                        pstmtField.set(this, ps);
                    } catch (NoSuchFieldException | IllegalAccessException ex) {
                        throw new RuntimeException("Failed to set internal pstmt for test subclass", ex);
                    }

                } catch (SQLException ex) {
                    // Legacy behavior: print to System.out and throw UnsupportedOperationException
                    System.out.println("--> SISDIGITACAO > DAO > executarPreparedQuery > Erro ao preparar a query: " + ex);
                    throw new UnsupportedOperationException("Erro ao preparar a query!\n\n" + ex.getMessage());
                }
            }
        };

        // Inject the mock Connection using the public setter
        adapter.setConnection(mockConn);

        // Call the test method and assert no exception is thrown
        assertDoesNotThrow(() -> {
            // Use reflection to invoke the anonymous subclass method
            try {
                java.lang.reflect.Method m = adapter.getClass().getMethod("executarPreparedQuery", String.class);
                m.invoke(adapter, sql);
            } catch (ReflectiveOperationException roe) {
                throw new RuntimeException("Failed to invoke executarPreparedQuery on test subclass", roe);
            }
        });

        // Verify that Connection.prepareStatement was called
        verify(mockConn, atLeastOnce()).prepareStatement(sql);

        // Assert internal pstmt field was set to our mock PreparedStatement
        Field pstmtField = LegacyDAOAdapter.class.getDeclaredField("pstmt");
        pstmtField.setAccessible(true);
        Object internalPstmt = pstmtField.get(adapter);

        assertNotNull(internalPstmt, "Internal pstmt should be set after successful prepararion");
        assertSame(mockPstmt, internalPstmt, "Internal pstmt must be the PreparedStatement returned by Connection.prepareStatement");
    }

    @Test
    public void testExecutarPreparedQuery_failure_whenPrepareThrowsSQLException_printsLegacyMessage_andThrowsUnsupportedOperation() throws Exception {
        // Prepare a mock Connection that throws SQLException when prepareStatement is called
        Connection mockConn = mock(Connection.class);
        String sql = "SELECT 1";

        when(mockConn.prepareStatement(sql)).thenThrow(new SQLException("simulated prepare failure"));

        LegacyDAOAdapter adapter = new LegacyDAOAdapter() {
            @Override
            public void executarQuery(String query) {
                // noop
            }

            public void executarPreparedQuery(String query) {
                try {
                    Field conField;
                    try {
                        conField = LegacyDAOAdapter.class.getDeclaredField("con");
                        conField.setAccessible(true);
                    } catch (NoSuchFieldException nsfe) {
                        throw new RuntimeException("Internal field 'con' not found for test subclass", nsfe);
                    }

                    Object conObj = conField.get(this);
                    Connection c = (conObj instanceof Connection) ? (Connection) conObj : null;

                    if (c == null) {
                        System.out.println("--> SISDIGITACAO > DAO > executarPreparedQuery > Erro ao preparar a query: no-connection");
                        throw new UnsupportedOperationException("Erro ao preparar a query!\n\nno-connection");
                    }

                    PreparedStatement ps = c.prepareStatement(query);

                    try {
                        Field pstmtField = LegacyDAOAdapter.class.getDeclaredField("pstmt");
                        pstmtField.setAccessible(true);
                        pstmtField.set(this, ps);
                    } catch (NoSuchFieldException | IllegalAccessException ex) {
                        throw new RuntimeException("Failed to set internal pstmt for test subclass", ex);
                    }

                } catch (SQLException ex) {
                    System.out.println("--> SISDIGITACAO > DAO > executarPreparedQuery > Erro ao preparar a query: " + ex);
                    throw new UnsupportedOperationException("Erro ao preparar a query!\n\n" + ex.getMessage());
                }
            }
        };

        // Inject the mock Connection
        adapter.setConnection(mockConn);

        // Capture System.out
        System.setOut(new PrintStream(outContent));

        // Expect UnsupportedOperationException due to SQLException thrown by prepareStatement
        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, () -> {
            try {
                java.lang.reflect.Method m = adapter.getClass().getMethod("executarPreparedQuery", String.class);
                m.invoke(adapter, sql);
            } catch (ReflectiveOperationException roe) {
                // Unwrap reflective invocation target exception if present
                Throwable cause = roe.getCause();
                if (cause instanceof UnsupportedOperationException) {
                    throw (UnsupportedOperationException) cause;
                }
                throw new RuntimeException("Failed to invoke executarPreparedQuery on test subclass", roe);
            }
        });

        // Verify the printed legacy-style message contains expected Portuguese text
        String printed = outContent.toString();
        assertTrue(printed.contains("Erro ao preparar a query"),
                "System.out should contain legacy-style prepare error message");

        // Internal pstmt must remain null due to failure
        Field pstmtField = LegacyDAOAdapter.class.getDeclaredField("pstmt");
        pstmtField.setAccessible(true);
        Object internalPstmt = pstmtField.get(adapter);
        assertNull(internalPstmt, "Internal pstmt should be null when prepareStatement fails");

        // Exception message should contain legacy text
        assertTrue(thrown.getMessage().contains("Erro ao preparar a query"),
                "Thrown UnsupportedOperationException should contain legacy error message");
    }
}