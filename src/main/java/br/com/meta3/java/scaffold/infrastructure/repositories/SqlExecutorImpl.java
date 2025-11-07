package br.com.meta3.java.scaffold.infrastructure.repositories;

import br.com.meta3.java.scaffold.application.exceptions.DataBaseOperationException;
import br.com.meta3.java.scaffold.domain.repositories.SqlExecutor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

/**
 * Implementation of SqlExecutor using Spring's JdbcTemplate.
 * Supersedes legacy manual connection logic (conectarBanco) by leveraging
 * automatic connection management, statement preparation, and resource cleanup.
 */
@Repository
public class SqlExecutorImpl implements SqlExecutor {

    private final JdbcTemplate jdbcTemplate;

    public SqlExecutorImpl(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes the provided SQL query and maps each row into a Map of column names to values.
     *
     * @param sql  the SQL query to execute
     * @param args optional query parameters
     * @return list of rows, each represented as a Map<String, Object>
     * @throws DataBaseOperationException if any SQL error occurs
     */
    @Override
    public List<Map<String, Object>> executeQuery(String sql, Object... args) {
        try {
            // TODO: (REVIEW) Using JdbcTemplate#queryForList to handle query execution and mapping
            return jdbcTemplate.queryForList(sql, args);
        } catch (DataAccessException ex) {
            // TODO: (REVIEW) Wrap DataAccessException into domain-specific unchecked exception
            throw new DataBaseOperationException("Erro ao executar consulta SQL: " + sql, ex);
        }
    }

    /**
     * Executes the provided SQL update (INSERT, UPDATE, DELETE).
     *
     * @param sql  the SQL statement to execute
     * @param args optional statement parameters
     * @return number of rows affected
     * @throws DataBaseOperationException if any SQL error occurs
     */
    @Override
    public int executeUpdate(String sql, Object... args) {
        try {
            // TODO: (REVIEW) Using JdbcTemplate#update for executing DML statements with automatic resource cleanup
            return jdbcTemplate.update(sql, args);
        } catch (DataAccessException ex) {
            // TODO: (REVIEW) Wrap DataAccessException into domain-specific unchecked exception
            throw new DataBaseOperationException("Erro ao executar atualização SQL: " + sql, ex);
        }
    }
}
