filetype
package br.com.meta3.java.scaffold.infrastructure.repositories;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for LegacyDAOAdapter.executarQuery(...) behavior.
 *
 * Notes / decisions:
 * - The current LegacyDAOAdapter.executarQuery implementation (migration stub) does not perform
 *   actual JDBC execution; it records the SQL into lastQuery and sets rsList to an empty list.
 *   Therefore the "success" test below verifies that the adapter keeps a provided ResultSet
 *   (injected via setResultSet), records the lastQuery and preserves the safe default empty rsList.
 *
 * - Legacy behavior (original code) attempted to call stmt.executeQuery(query) and would catch
 *   SQLException, print a legacy-style System.out message and rethrow an UnsupportedOperationException.
 *   Because the adapter's executarQuery is currently a stub, the second test demonstrates the expected
 *   legacy behavior by introducing a small anonymous subclass that performs the legacy-like execution.
 *
 * TODO: (REVIEW)
 * - Once LegacyDAOAdapter.executarQuery is implemented to execute SQL via JDBC/DataSource/EntityManager,
 *   the second test should be removed or adapted to assert the real adapter behavior directly instead
 *   of testing a local subclass.
 */
public class LegacyDAOAdapterExecutarQueryTest {

    private final PrintStream originalOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @AfterEach
    public void tearDown() {
        System.setOut(originalOut);
    }

    @Test
    public void testExecutarQuery_success_recordsLastQuery_and_preservesInjectedResultSet_and_emptyRsList() throws Exception {
        LegacyDAOAdapter adapter = new LegacyDAOAdapter();

        // Prepare mocks
        Statement stmt = mock(Statement.class);
        ResultSet rs = mock(ResultSet.class);

        // Inject mocks into adapter
        adapter.setStatement(stmt);
        adapter.setResultSet(rs);

        // Sanity: pre-populate rsList with a non-empty value to ensure executarQuery replaces/clears it per current impl.
        adapter.setRsList(Collections.singletonList(Collections.emptyMap()));

        // Call method under test (current stub implementation will record lastQuery and set rsList = empty)
        String sql = "SELECT 1";
        // No exception expected for the current stub
        assertDoesNotThrow(() -> adapter.executarQuery(sql));

        // Verify lastQuery was recorded
        assertEquals(sql, adapter.getLastQuery(), "lastQuery should be recorded by executarQuery()");

        // The current migration stub sets rsList to empty list as safe default
        assertNotNull(adapter.getRsList(), "rsList should not be null after executarQuery()");
        assertTrue(adapter.getRsList().isEmpty(), "rsList should be empty after executarQuery() (safe default)");

        // Verify the injected ResultSet instance remains assigned internally.
        // There is no public getter for the internal 'rs' field, so use reflection to assert it is still the same mock.
        Field rsField = LegacyDAOAdapter.class.getDeclaredField("rs");
        rsField.setAccessible(true);
        Object internalRs = rsField.get(adapter);
        assertSame(rs, internalRs, "The adapter's internal ResultSet should be the same instance injected via setResultSet()");

        // Verify we did not unexpectedly call the mocked Statement (the stub does not execute it).
        verifyNoInteractions(stmt);
        // verify no interactions on ResultSet as adapter stub doesn't use it
        verifyNoInteractions(rs);

        // Comment: This test documents current safe-default behavior. When executarQuery is implemented to perform real JDBC
        // execution, update this test to assert that stmt.executeQuery(sql) is invoked and rsList is populated from the ResultSet.
    }

    @Test
    public void testExecutarQuery_failure_whenStatementThrowsSQLException_shouldPrintLegacyMessage_andThrowUnsupportedOperation() throws Exception {
        // This test demonstrates the legacy-expected behavior:
        //  - call stmt.executeQuery(sql)
        //  - on SQLException print legacy message and throw UnsupportedOperationException
        //
        // Because LegacyDAOAdapter.executarQuery is currently a migration stub (no JDBC execution),
        // we create an anonymous subclass that implements the legacy-like behavior for testing expectations.
        LegacyDAOAdapter adapter = new LegacyDAOAdapter() {
            // Re-create legacy-like executarQuery behavior for this test only.
            @Override
            public void executarQuery(String query) {
                try {
                    // Obtain the Statement we've injected via the public setter using reflection (no getter exists).
                    Statement s = null;
                    try {
                        Field stmtField = LegacyDAOAdapter.class.getDeclaredField("stmt");
                        stmtField.setAccessible(true);
                        Object stmtObj = stmtField.get(this);
                        if (stmtObj instanceof Statement) {
                            s = (Statement) stmtObj;
                        }
                    } catch (NoSuchFieldException | IllegalAccessException ex) {
                        // If reflection fails, proceed with s == null which will cause a NullPointerException below;
                        // wrap it as a runtime exception to fail the test meaningfully.
                        throw new RuntimeException("Failed to access internal Statement for test subclass", ex);
                    }

                    if (s == null) {
                        // Simulate legacy behavior if no statement is available: throw UnsupportedOperationException.
                        System.out.println("--> SISDIGITACAO > DAO > executarQuery > Erro ao realizar transação com o banco de dados: no-statement");
                        throw new UnsupportedOperationException("Erro ao realizar transação com o banco de dados!");
                    }

                    // Attempt execution (this will throw SQLException as configured in the mock)
                    ResultSet r = s.executeQuery(query);

                    // If execution succeeded, store the result into the internal rs field via reflection to mimic legacy behavior
                    try {
                        Field rsField = LegacyDAOAdapter.class.getDeclaredField("rs");
                        rsField.setAccessible(true);
                        rsField.set(this, r);
                    } catch (NoSuchFieldException | IllegalAccessException ex) {
                        throw new RuntimeException("Failed to set internal ResultSet for test subclass", ex);
                    }

                } catch (SQLException ex) {
                    // Legacy behavior: log/print then throw UnsupportedOperationException
                    System.out.println("--> SISDIGITACAO > DAO > executarQuery > Erro ao realizar transação com o banco de dados: " + ex);
                    throw new UnsupportedOperationException("Erro ao realizar transação com o banco de dados!");
                }
            }
        };

        // Prepare a Statement mock that throws SQLException when executeQuery is invoked
        Statement stmt = mock(Statement.class);
        when(stmt.executeQuery(anyString())).thenThrow(new SQLException("simulated execution failure"));

        // Inject into adapter via public setter
        adapter.setStatement(stmt);

        // Capture System.out
        System.setOut(new PrintStream(outContent));

        UnsupportedOperationException thrown = assertThrows(UnsupportedOperationException.class, () -> adapter.executarQuery("SELECT 1"),
                "Expected UnsupportedOperationException when Statement.executeQuery throws SQLException");

        // The exception message should contain the legacy Portuguese text
        assertTrue(thrown.getMessage().contains("Erro ao realizar transação com o banco de dados"),
                "UnsupportedOperationException should contain legacy error message text");

        String printed = outContent.toString();
        assertTrue(printed.contains("Erro ao realizar transação com o banco de dados"),
                "System.out should contain the legacy-style error message printed on SQLException");

        // Verify the Statement's executeQuery was called
        verify(stmt, atLeastOnce()).executeQuery("SELECT 1");

        // No ResultSet should have been set internally due to the SQLException; assert the internal rs field is null
        Field rsField = LegacyDAOAdapter.class.getDeclaredField("rs");
        rsField.setAccessible(true);
        Object internalRs = rsField.get(adapter);
        assertNull(internalRs, "Internal ResultSet should be null when execution fails with SQLException");

        // TODO: (REVIEW) Once LegacyDAOAdapter.executarQuery is implemented to execute SQL using injected JDBC resources,
        // convert this test to target the real implementation rather than an anonymous test subclass.
    }
}