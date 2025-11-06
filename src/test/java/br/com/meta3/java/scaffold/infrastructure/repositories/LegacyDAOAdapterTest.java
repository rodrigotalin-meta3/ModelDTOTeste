filetype
package br.com.meta3.java.scaffold.infrastructure.repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LegacyDAOAdapter.desconectarBanco().
 *
 * Tests:
 *  1) verify resources are closed (via interaction) and adapter state cleared after successful call.
 *  2) simulate a resource close throwing SQLException and assert UnsupportedOperationException is thrown
 *     and an error message is printed to System.out (mimics legacy behavior).
 *
 * Notes/decisions:
 * - We use Mockito to create lightweight JDBC mocks and verify interactions.
 * - The adapter exposes helpers to inject JDBC resources for migration/tests; we rely on those.
 * - The adapter prints an error to System.out in addition to logging via SLF4J. Capturing System.out is
 *   a pragmatic way to validate "logged" legacy-style output without introducing a logging test appender.
 *
 * TODO: (REVIEW) If stronger verification of SLF4J logs is required, replace System.out capture with a
 *       TestAppender bound to the adapter's logger.
 */
public class LegacyDAOAdapterTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @AfterEach
    public void tearDown() {
        // Restore System.out after each test to avoid interfering with other tests
        System.setOut(originalOut);
    }

    @Test
    public void testDesconectarBanco_closesResourcesAndClearsState_onSuccess() throws Exception {
        LegacyDAOAdapter adapter = new LegacyDAOAdapter();

        // Create JDBC mocks
        Connection con = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        PreparedStatement pstmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        // Inject mocks
        adapter.setConnection(con);
        adapter.setStatement(stmt);
        adapter.setPreparedStatement(pstmt);
        adapter.setResultSet(rs);

        // Emulate having executed a query (lastQuery / rsList)
        adapter.executarQuery("SELECT 1");
        adapter.setRsList(Collections.singletonList(Collections.emptyMap()));

        // Ensure connected flag is set by calling conectarBanco (adapter emulates this)
        adapter.conectarBanco();
        assertTrue(adapter.isConnected(), "Adapter should be connected after conectarBanco()");

        // Call method under test - should close resources and clear state without throwing
        assertDoesNotThrow(adapter::desconectarBanco);

        // Verify close() was invoked on each JDBC resource in the expected order (calls verified individually)
        verify(rs, atLeastOnce()).close();
        verify(pstmt, atLeastOnce()).close();
        verify(stmt, atLeastOnce()).close();
        verify(con, atLeastOnce()).close();

        // Adapter state must be cleared: connected false, lastQuery null, rsList empty
        assertFalse(adapter.isConnected(), "Adapter should not be connected after desconectarBanco()");
        assertNull(adapter.getLastQuery(), "lastQuery should be cleared after desconectarBanco()");
        assertNotNull(adapter.getRsList(), "rsList should not be null after desconectarBanco()");
        assertTrue(adapter.getRsList().isEmpty(), "rsList should be empty after desconectarBanco()");
    }

    @Test
    public void testDesconectarBanco_throwsUnsupportedOperationException_whenResourceCloseFails() throws Exception {
        LegacyDAOAdapter adapter = new LegacyDAOAdapter();

        // Create JDBC mocks
        Connection con = mock(Connection.class);
        Statement stmt = mock(Statement.class);
        PreparedStatement pstmt = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);

        // Configure the ResultSet.close() to throw SQLException to simulate failure during cleanup
        doThrow(new SQLException("simulated close failure")).when(rs).close();

        // Inject mocks
        adapter.setConnection(con);
        adapter.setStatement(stmt);
        adapter.setPreparedStatement(pstmt);
        adapter.setResultSet(rs);

        // Ensure connected flag is set
        adapter.conectarBanco();
        assertTrue(adapter.isConnected(), "Adapter should be connected before desconectarBanco()");

        // Capture System.out to assert legacy-style error message is printed
        System.setOut(new PrintStream(outContent));

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, adapter::desconectarBanco,
                "Expected UnsupportedOperationException when a resource close throws SQLException");

        // Basic assertion on exception message content to ensure legacy wrapping
        assertTrue(thrown.getMessage().contains("Erro ao desconectar do Banco"),
                "Exception message should contain legacy disconnection text");

        // Ensure the System.out printed the error message indicating ResultSet close problem
        String printed = outContent.toString();
        assertTrue(printed.contains("Erro ao desconectar ResultSet") || printed.contains("Erro ao desconectar do Banco"),
                "System.out should contain an error message about failing to disconnect the ResultSet / DB");

        // Because ResultSet.close() failed and the adapter rethrows, the subsequent resources should not be closed.
        verify(rs, atLeastOnce()).close();
        verifyNoInteractions(pstmt); // pstmt.close() should not have been attempted
        verifyNoInteractions(stmt);
        verifyNoInteractions(con);
    }
}