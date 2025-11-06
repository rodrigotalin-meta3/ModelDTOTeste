filetype
package br.com.meta3.java.scaffold.application.services;

import br.com.meta3.java.scaffold.domain.services.UsuarioService;
import br.com.meta3.java.scaffold.infrastructure.repositories.UsuarioDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

/**
 * Application-level implementation of UsuarioService.
 *
 * Responsibilities:
 * - Delegate to the legacy-compatible UsuarioDAO to obtain the persisted "anobase" when available.
 * - Ensure legacy fallback behavior is preserved (non-null return and date-based fallback rule).
 *
 * Design decisions / notes:
 * - We delegate to UsuarioDAO.retornaAnoBase(...) to reuse the existing DB-access logic implemented
 *   in the infrastructure layer. If the DAO returns null or throws, we fall back to the domain helper
 *   UsuarioService.computeFallbackAnoBase() to ensure consistent legacy behavior across the application.
 * - Constructor injection is used to make the dependency explicit and test-friendly.
 * - We catch exceptions from the DAO and log debug messages, returning the computed fallback to avoid
 *   propagating infrastructure errors to callers (preserves legacy robustness).
 *
 * TODO: (REVIEW) Consider introducing a domain-level UsuarioService interface consumer elsewhere and
 * move more user-related logic into domain services instead of delegating directly to DAOs.
 */
@Service
public class UsuarioServiceImpl implements UsuarioService {

    private static final Logger log = LoggerFactory.getLogger(UsuarioServiceImpl.class);

    private final UsuarioDAO usuarioDAO;

    public UsuarioServiceImpl(UsuarioDAO usuarioDAO) {
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO must not be null");
    }

    /**
     * Return the configured "anobase" for the given user code (codigoUsuario).
     *
     * Behavior preserved from legacy:
     * - If UsuarioDAO provides a value, return it.
     * - If DAO returns null or an exception occurs, use the domain fallback:
     *     If today's date is between Nov 7 and Dec 31 (inclusive), return current year + 1,
     *     otherwise return current year.
     *
     * Implementations MUST return a non-null Integer.
     *
     * @param codigoUsuario the user code (may be null)
     * @return the ano base (never null)
     */
    @Override
    public Integer retornaAnoBase(Integer codigoUsuario) {
        try {
            Integer anobase = usuarioDAO.retornaAnoBase(codigoUsuario);
            if (anobase == null) {
                // DAO returned no explicit value, use domain fallback to preserve legacy behavior.
                log.debug("UsuarioDAO.retornaAnoBase returned null for codigo={}, using domain fallback", codigoUsuario);
                return UsuarioService.computeFallbackAnoBase();
            }
            return anobase;
        } catch (Exception ex) {
            // Defensive: do not propagate infrastructure exceptions; use fallback consistent with legacy behavior.
            log.debug("Error while delegating retornaAnoBase to UsuarioDAO for codigo={} - falling back to computed value. reason={}",
                    codigoUsuario, ex.getMessage());
            return UsuarioService.computeFallbackAnoBase();
        }
    }

    /**
     * Convenience pass-through for other potential migration needs.
     *
     * NOTE: This method is not part of the UsuarioService interface; it is provided as a helpful utility
     * for other application services that might still need to translate between codigoUsuario and login.
     *
     * TODO: (REVIEW) If this is needed by multiple callers, consider adding it to a domain-level contract.
     *
     * @param codigoUsuario the user code (may be null)
     * @return login string or null if not found / on error
     */
    public String findLoginByCodigo(Integer codigoUsuario) {
        try {
            return usuarioDAO.findLoginByCodigo(codigoUsuario);
        } catch (Exception ex) {
            log.debug("Error while delegating findLoginByCodigo to UsuarioDAO for codigo={} - returning null. reason={}",
                    codigoUsuario, ex.getMessage());
            return null;
        }
    }

    /**
     * Convenience delegating method that returns the configured "anobase" for a given school code (codEscola).
     *
     * Behavior & guarantees:
     * - Delegates to UsuarioDAO.retornaAnoBasePorEscola(codEscola) to reuse existing DB-access logic.
     * - If the DAO returns null or an exception occurs, falls back to UsuarioService.computeFallbackAnoBase()
     *   to guarantee a non-null Integer (preserves legacy contract).
     *
     * Rationale / decisions:
     * - Providing this convenience method at the application-service layer centralizes the fallback logic
     *   so controllers and other services can call a single, robust method without duplicating try/catch.
     * - We deliberately swallow infrastructure exceptions here and log them at debug level to preserve the
     *   legacy system's tendency to return safe defaults rather than propagate DB errors to the web layer.
     *
     * TODO: (REVIEW) Consider adding this method to the UsuarioService interface if multiple consumers require it.
     *
     * @param codEscola the school code (may be null)
     * @return the ano base as Integer (never null)
     */
    public Integer retornaAnoBasePorEscola(Integer codEscola) {
        try {
            Integer anobase = usuarioDAO.retornaAnoBasePorEscola(codEscola);
            if (anobase == null) {
                log.debug("UsuarioDAO.retornaAnoBasePorEscola returned null for codEscola={}, using domain fallback", codEscola);
                return UsuarioService.computeFallbackAnoBase();
            }
            return anobase;
        } catch (Exception ex) {
            // Defensive: log and fallback to computed value to ensure callers always receive a non-null result.
            log.debug("Error while delegating retornaAnoBasePorEscola to UsuarioDAO for codEscola={} - falling back to computed value. reason={}",
                    codEscola, ex.getMessage());
            return UsuarioService.computeFallbackAnoBase();
        }
    }
}