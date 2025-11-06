filetype
package br.com.meta3.java.scaffold.domain.services;

import java.time.LocalDate;
import java.time.MonthDay;

/**
 * Abstraction for user-related operations required by legacy-compatible flows.
 *
 * Purpose:
 * - Decouple controllers and higher layers from the concrete DAO/Repository implementation (UsuarioDAO).
 * - Centralize business rules for computing the "ano base" (anobase) fallback so implementations
 *   share consistent behavior.
 *
 * Legacy expectations (preserved here):
 * - retornaAnoBase(Integer codigoUsuario) must return a non-null Integer.
 * - If an "anobase" value is persisted for the user, implementations should return that value.
 * - If not parametrized in the DB (or DB read fails), the fallback rule is:
 *     If today's date is between Nov 7 and Dec 31 (inclusive), return current year + 1,
 *     otherwise return current year.
 *
 * Implementation notes:
 * - Prefer implementing classes should call the static helper computeFallbackAnoBase() when a DB value
 *   is not available to guarantee consistent legacy behavior across the application.
 * - This interface intentionally does not depend on infrastructure details (EntityManager, JDBC, etc.).
 *   Those are the responsibility of concrete implementations in the infrastructure layer.
 *
 * TODO: (REVIEW) Consider moving this contract to a domain-level User entity/service if more user-related
 *       use-cases are consolidated in the domain. For now this remains a minimal, legacy-focused abstraction.
 */
public interface UsuarioService {

    /**
     * Return the configured "anobase" for the given user code (codigoUsuario).
     *
     * Legacy contract to preserve:
     * - If storage contains an explicit anobase for the user, return it.
     * - If not parametrized or storage access fails, use the fallback described above.
     *
     * Implementations MUST return a non-null Integer.
     *
     * @param codigoUsuario the user code (may be null)
     * @return the ano base (never null)
     */
    Integer retornaAnoBase(Integer codigoUsuario);

    /**
     * Helper to compute the legacy fallback ano base when no persisted value is available.
     *
     * This replicates the legacy logic:
     * - If today's date is between Nov 7 and Dec 31 (inclusive), return current year + 1.
     * - Otherwise return current year.
     *
     * Providing this helper in the interface ensures all implementations behave consistently
     * when falling back from persistence layer failures or missing configuration.
     *
     * TODO: (REVIEW) If future localization/timezone concerns arise, consider passing a Clock or LocalDate
     *       parameter to make this method testable and timezone-safe. For now use LocalDate.now().
     *
     * @return computed fallback ano base (never null)
     */
    static Integer computeFallbackAnoBase() {
        LocalDate today = LocalDate.now();
        MonthDay start = MonthDay.of(11, 7); // Nov 7
        MonthDay end = MonthDay.of(12, 31);  // Dec 31
        MonthDay md = MonthDay.from(today);

        if (!md.isBefore(start) && !md.isAfter(end)) {
            return today.getYear() + 1;
        } else {
            return today.getYear();
        }
    }
}