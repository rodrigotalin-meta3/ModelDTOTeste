package br.com.meta3.java.scaffold.infrastructure.repositories;

import br.com.meta3.java.scaffold.api.dtos.InstituicaoDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Placeholder DAO class to preserve legacy dependency:
 * RecadastramentoDAO.mostraInstituicoesEstadoMunicipio(String login)
 *
 * This implementation provides a method that returns a List<InstituicaoDTO>.
 * Behavior:
 * - Attempts a best-effort native SQL query against a possible "instituicoes" table.
 *   The expected columns used are: id, nome, estado, municipio.
 * - If the schema/table/columns are not present or any exception occurs, the method
 *   logs the problem and returns an empty list as a safe fallback.
 *
 * Additionally (migration task):
 * - If the primary query returns no results, attempt a legacy-table fallback against
 *   alunos.alu_mec_tacom using the original legacy filters:
 *     - If login equals "9999" then length(codigosec) = 7 (state-level)
 *     - Else length(codigosec) < 7 (municipality-level)
 *     - cod_titular is not null
 *     - ano_base_exclusao is null
 *   The fallback maps:
 *     codigosec -> id (attempt to parse to Long; null if parsing fails)
 *     nome      -> nome
 *     bairro    -> municipio
 *     estado    -> null
 *
 * TODO: (REVIEW) Replace native-query approach with a proper JPA repository and an
 * entity class for Instituicao when the domain model is introduced. Also align
 * column names and filtering conditions with the real legacy schema.
 */
@Repository
public class RecadastramentoDAO {

    private static final Logger log = LoggerFactory.getLogger(RecadastramentoDAO.class);

    @PersistenceContext
    private EntityManager entityManager;

    public RecadastramentoDAO() {
    }

    /**
     * Legacy-compatible method signature.
     *
     * The legacy code expected mostraInstituicoesEstadoMunicipio(cod) to return a List
     * (often of Maps or result rows). To simplify migration we return a strongly-typed
     * List<InstituicaoDTO>.
     *
     * Behavior implemented:
     * 1) Try to read from modern/expected "instituicoes" table using a native query.
     * 2) If that yields no rows (empty list) or if the primary query cannot run, attempt
     *    the legacy-table fallback against alunos.alu_mec_tacom using provided filters.
     * 3) If everything fails, return an empty list (preserve legacy empty-list-on-error behavior).
     *
     * @param login the user/login code used to filter institutions (legacy used session "login")
     * @return list of InstituicaoDTO (may be empty if no data or on error)
     */
    @SuppressWarnings("unchecked")
    public List<InstituicaoDTO> mostraInstituicoesEstadoMunicipio(String login) {
        // Defensive: if login is null behave consistently with earlier conservative approach.
        if (login == null) {
            return Collections.emptyList();
        }

        // 1) Primary attempt: modern/expected table
        try {
            String sql = "SELECT id, nome, estado, municipio FROM instituicoes WHERE usuario_login = :login";
            Query q = entityManager.createNativeQuery(sql);
            q.setParameter("login", login);

            List<Object[]> rows = q.getResultList();
            if (rows != null && !rows.isEmpty()) {
                List<InstituicaoDTO> result = new ArrayList<>(rows.size());

                for (Object[] row : rows) {
                    Long id = null;
                    if (row.length > 0 && row[0] != null) {
                        if (row[0] instanceof Number) {
                            id = ((Number) row[0]).longValue();
                        } else {
                            try {
                                id = Long.valueOf(row[0].toString());
                            } catch (NumberFormatException ex) {
                                // keep id as null if it cannot be parsed
                            }
                        }
                    }

                    String nome = row.length > 1 && row[1] != null ? row[1].toString() : null;
                    String estado = row.length > 2 && row[2] != null ? row[2].toString() : null;
                    String municipio = row.length > 3 && row[3] != null ? row[3].toString() : null;

                    InstituicaoDTO dto = new InstituicaoDTO(id, nome, estado, municipio);
                    result.add(dto);
                }

                return result;
            }
            // If primary returned empty, fall through to legacy-table fallback below.
        } catch (Exception ex) {
            // Log and continue to fallback strategy
            log.debug("Primary query for instituicoes failed - will attempt legacy-table fallback. login={}, reason={}",
                    login, ex.getMessage());
        }

        // 2) Legacy-table fallback: alunos.alu_mec_tacom
        try {
            // Build the legacy SQL mirroring the original logic.
            StringBuilder sb = new StringBuilder();
            sb.append("SELECT codigosec, nome, bairro FROM alunos.alu_mec_tacom ");
            // Apply length filter according to login value
            if ("9999".equals(login)) {
                sb.append(" WHERE length(codigosec) = 7 ");
            } else {
                sb.append(" WHERE length(codigosec) < 7 ");
            }
            sb.append(" AND cod_titular IS NOT NULL ");
            sb.append(" AND ano_base_exclusao IS NULL ");
            sb.append(" ORDER BY codigosec ASC");

            String legacySql = sb.toString();

            Query qLegacy = entityManager.createNativeQuery(legacySql);
            // Note: legacy query does not use parameters for login; kept as literal behavior from original code.

            List<Object[]> rowsLegacy = qLegacy.getResultList();
            if (rowsLegacy == null || rowsLegacy.isEmpty()) {
                // No rows found in legacy table either -> return empty list (preserve legacy behavior)
                return Collections.emptyList();
            }

            List<InstituicaoDTO> fallbackResult = new ArrayList<>(rowsLegacy.size());
            for (Object[] row : rowsLegacy) {
                Long id = null;
                if (row.length > 0 && row[0] != null) {
                    // codigosec in legacy schema might be numeric or textual.
                    // Try Number then parse it; if fails leave id null.
                    if (row[0] instanceof Number) {
                        id = ((Number) row[0]).longValue();
                    } else {
                        try {
                            id = Long.valueOf(row[0].toString());
                        } catch (NumberFormatException nfe) {
                            // id remains null; legacy codigosec might be non-numeric (keep null)
                        }
                    }
                }

                String nome = row.length > 1 && row[1] != null ? row[1].toString() : null;
                // Per migration decision, map bairro -> municipio, leave estado null
                String municipio = row.length > 2 && row[2] != null ? row[2].toString() : null;

                InstituicaoDTO dto = new InstituicaoDTO(id, nome, null, municipio);
                fallbackResult.add(dto);
            }

            return fallbackResult;
        } catch (Exception ex) {
            // Any failure during fallback -> preserve legacy behavior and return empty list
            log.debug("Legacy-table fallback query failed for alunos.alu_mec_tacom. login={}, reason={}",
                    login, ex.getMessage());
            // TODO: (REVIEW) Consider more detailed logging or capturing SQL/state for further migration troubleshooting.
            return Collections.emptyList();
        }
    }
}
