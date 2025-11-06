package br.com.meta3.java.scaffold.infrastructure.repositories;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import java.time.LocalDate;
import java.time.MonthDay;
import java.util.List;

/**
 * Placeholder DAO to preserve legacy dependency:
 * - UsuarioDAO.retornaAnoBase(Integer codigoUsuario)
 * - optional lookup by login to aid migration
 *
 * Behavior:
 * - Attempts to read a numeric "anobase" column from a hypothetical "usuarios" table.
 * - If no value is found (null / missing column / missing row) it falls back to a legacy-compatible
 *   default computation:
 *     If today's date is between Nov 7 and Dec 31 (inclusive), return current year + 1,
 *     otherwise return current year.
 *
 * - Also exposes a convenience method findLoginByCodigo(Integer codigoUsuario) which tries to
 *   read a "login" column from the same table. This preserves possible legacy usages where code
 *   mapped login and user information.
 *
 * Notes / decisions:
 * - TODO: (REVIEW) The actual users table name and column names ("anobase", "codigo", "login")
 *   are assumptions made to provide a best-effort compatibility layer. Replace with proper JPA
 *   entity and repository when the domain model is available.
 * - We use native queries with EntityManager to avoid creating new domain entities in this migration step.
 * - All DB access failures are caught and a safe fallback is used to avoid throwing runtime exceptions
 *   to legacy callers during incremental migration.
 */
@Repository
public class UsuarioDAO {

    private static final Logger log = LoggerFactory.getLogger(UsuarioDAO.class);

    @PersistenceContext
    private EntityManager entityManager;

    public UsuarioDAO() {
    }

    /**
     * Return the configured "anobase" for the given user code (codigoUsuario).
     *
     * Legacy expectations:
     * - If anobase is stored in the DB return it.
     * - If not parametrized, and current date is between 07/11 and 31/12, return current year + 1.
     * - Otherwise return current year.
     *
     * @param codigoUsuario the user code (may be null)
     * @return the ano base as Integer (never null)
     */
    public Integer retornaAnoBase(Integer codigoUsuario) {
        // Attempt to read from database first. If anything goes wrong, fallback to computed default.
        try {
            if (codigoUsuario != null) {
                String sql = "SELECT anobase FROM usuarios WHERE codigo = :codigo";
                Query q = entityManager.createNativeQuery(sql);
                q.setParameter("codigo", codigoUsuario);

                Object single = null;
                try {
                    single = q.getSingleResult();
                } catch (Exception ex) {
                    // No result or non-unique result etc. We'll handle below by falling back.
                    log.debug("retornaAnoBase: query did not return a single result for codigo={} - reason={}",
                            codigoUsuario, ex.getMessage());
                    single = null;
                }

                if (single != null) {
                    if (single instanceof Number) {
                        return ((Number) single).intValue();
                    } else {
                        try {
                            return Integer.parseInt(single.toString());
                        } catch (NumberFormatException nfe) {
                            log.debug("retornaAnoBase: could not parse anobase value='{}' for codigo={}",
                                    single, codigoUsuario);
                            // fall through to compute default
                        }
                    }
                }
            }
        } catch (Exception ex) {
            // If any unexpected DB error happens, log debug and fallback to computed default.
            log.debug("retornaAnoBase: DB access error while fetching anobase for codigo={} - reason={}",
                    codigoUsuario, ex.getMessage());
        }

        // Fallback logic (legacy behavior):
        LocalDate today = LocalDate.now();
        MonthDay start = MonthDay.of(11, 7); // Nov 7
        MonthDay end = MonthDay.of(12, 31);  // Dec 31
        MonthDay md = MonthDay.from(today);

        if (!md.isBefore(start) && !md.isAfter(end)) {
            // between Nov 7 and Dec 31 inclusive
            return today.getYear() + 1;
        } else {
            return today.getYear();
        }
    }

    /**
     * Return the configured "anobase" for the given school code (codEscola).
     *
     * This method reproduces the legacy logic from the older DAO:
     *  1) Query global parameter table (alunos.parametros_sis_digitacao) for a default anobase.
     *  2) Query individual parameter table (alunos.parametros_sis_digitacao_ind) for a school-specific anobase
     *     using date bounds (dt_ini_parametro <= current_date AND dt_fim_parametro >= current_date) and cod_escola filter.
     *     If multiple rows are returned, the last non-null ano_base from the result set is used (preserving legacy loop behavior).
     *  3) If no anobase is found (or value is 0), compute fallback using legacy date-window: if today between Nov 17 and Dec 31 (inclusive)
     *     then currentYear + 1 else currentYear.
     *
     * Implementation notes:
     * - Native SQL is used for compatibility with legacy schemas. We intentionally use COALESCE and CURRENT_DATE to improve
     *   portability across DBs (COALESCE preferred to NVL).
     * - The method is defensive: any exception during DB access is caught, logged at debug level and the method will continue to
     *   the fallback computation. The method never throws and always returns a non-null Integer.
     *
     * TODO: (REVIEW) Replace native queries with a typed JPA entity or repository when the parameter tables are mapped.
     *
     * @param codEscola the school code (may be null)
     * @return the ano base as Integer (never null)
     */
    @SuppressWarnings("unchecked")
    public Integer retornaAnoBasePorEscola(Integer codEscola) {
        Integer anoBase = 0;

        // 1) Try to read global anobase parameter from alunos.parametros_sis_digitacao
        try {
            // Use COALESCE to be portable (COALESCE works in many DBs; legacy used NVL)
            String sqlGlobal = "SELECT COALESCE(par.ano_base, 0) FROM alunos.parametros_sis_digitacao par WHERE par.status = 1";
            Query qGlobal = entityManager.createNativeQuery(sqlGlobal);

            Object single = null;
            try {
                single = qGlobal.getSingleResult();
            } catch (Exception ex) {
                // No result or other issue -> proceed to fallback/individual query
                log.debug("retornaAnoBasePorEscola: global parametros query returned no single result - reason={}", ex.getMessage());
                single = null;
            }

            if (single != null) {
                if (single instanceof Number) {
                    anoBase = ((Number) single).intValue();
                } else {
                    try {
                        anoBase = Integer.parseInt(single.toString());
                    } catch (NumberFormatException nfe) {
                        log.debug("retornaAnoBasePorEscola: could not parse global anobase='{}' - reason={}", single, nfe.getMessage());
                        // keep anoBase as 0
                    }
                }
            }
        } catch (Exception ex) {
            // Preserve non-throwing behavior: log and continue to individual parameter lookup / fallback
            log.debug("retornaAnoBasePorEscola: error while querying global parametros - reason={}", ex.getMessage());
        }

        // 2) Try to read school-specific anobase from alunos.parametros_sis_digitacao_ind
        try {
            // If codEscola is null we still attempt query but it will likely return nothing; keep behavior resilient
            String sqlInd = "SELECT pi.ano_base FROM alunos.parametros_sis_digitacao_ind pi "
                    + "WHERE pi.status = 1 "
                    + "AND pi.dt_ini_parametro <= CURRENT_DATE "
                    + "AND pi.dt_fim_parametro >= CURRENT_DATE "
                    + "AND pi.cod_escola = :cod "
                    + "ORDER BY pi.data_movimeto_par";

            Query qInd = entityManager.createNativeQuery(sqlInd);
            qInd.setParameter("cod", codEscola);

            List<Object> rows = qInd.getResultList();
            if (rows != null && !rows.isEmpty()) {
                // Legacy code iterated through resultset and assigned anoBase when value not null,
                // resulting in the last non-null value taking precedence. Preserve that behavior.
                for (Object r : rows) {
                    if (r == null) continue;
                    Integer parsed = null;
                    if (r instanceof Number) {
                        parsed = ((Number) r).intValue();
                    } else {
                        try {
                            parsed = Integer.parseInt(r.toString());
                        } catch (NumberFormatException nfe) {
                            // skip unparsable values
                            log.debug("retornaAnoBasePorEscola: skipped unparsable individual anobase='{}' for codEscola={}", r, codEscola);
                            continue;
                        }
                    }
                    if (parsed != null) {
                        anoBase = parsed;
                    }
                }
            }
        } catch (Exception ex) {
            // Log and continue to fallback computation
            log.debug("retornaAnoBasePorEscola: error while querying individual parametros for codEscola={} - reason={}", codEscola, ex.getMessage());
        }

        // 3) If still no value (0), apply legacy fallback: Nov 17 -> Dec 31 inclusive yields currentYear + 1
        if (anoBase == null || anoBase == 0) {
            LocalDate today = LocalDate.now();
            MonthDay start = MonthDay.of(11, 17); // Nov 17 per legacy requirement for school-based fallback
            MonthDay end = MonthDay.of(12, 31);
            MonthDay md = MonthDay.from(today);

            if (!md.isBefore(start) && !md.isAfter(end)) {
                anoBase = today.getYear() + 1;
            } else {
                anoBase = today.getYear();
            }
        }

        return anoBase;
    }

    /**
     * Optional helper to lookup a user's login by codigo.
     *
     * This preserves possible legacy usages that required translating between user code and login.
     *
     * @param codigoUsuario the user code (may be null)
     * @return login string or null if not found or on error
     */
    public String findLoginByCodigo(Integer codigoUsuario) {
        if (codigoUsuario == null) {
            return null;
        }

        try {
            String sql = "SELECT login FROM usuarios WHERE codigo = :codigo";
            Query q = entityManager.createNativeQuery(sql);
            q.setParameter("codigo", codigoUsuario);

            Object single = null;
            try {
                single = q.getSingleResult();
            } catch (Exception ex) {
                log.debug("findLoginByCodigo: query did not return a single result for codigo={} - reason={}",
                        codigoUsuario, ex.getMessage());
                single = null;
            }

            if (single != null) {
                return single.toString();
            }
        } catch (Exception ex) {
            log.debug("findLoginByCodigo: DB access error for codigo={} - reason={}", codigoUsuario, ex.getMessage());
        }

        return null;
    }

    // TODO: (REVIEW) Add additional legacy-compatible methods (e.g., retornaUsuarioByLogin) if other
    // legacy code paths require them during further migration steps.
}