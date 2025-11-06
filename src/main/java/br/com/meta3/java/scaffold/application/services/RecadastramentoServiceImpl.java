filetype
package br.com.meta3.java.scaffold.application.services;

import br.com.meta3.java.scaffold.api.dtos.InstituicaoDTO;
import br.com.meta3.java.scaffold.api.dtos.RecadastramentoResponseDTO;
import br.com.meta3.java.scaffold.domain.services.RecadastramentoService;
import br.com.meta3.java.scaffold.infrastructure.repositories.RecadastramentoDAO;
import br.com.meta3.java.scaffold.infrastructure.repositories.UsuarioDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Application-level implementation of RecadastramentoService.
 *
 * Responsibilities:
 * - Delegate to the legacy-compatible RecadastramentoDAO to obtain institution data.
 * - Ensure a stable contract (never return null; return empty lists when no data).
 * - Provide a convenience method that encapsulates the core logic previously present in the legacy
 *   Action class: fetching the list of institutions for a login and computing the "anobase"
 *   using UsuarioDAO. This method returns a RecadastramentoResponseDTO which mirrors the old
 *   session attributes ("listainstituicoes" and "anobase").
 *
 * Notes / Decisions:
 * - TODO: (REVIEW) We directly depend on infrastructure UsuarioDAO here for the anobase lookup.
 *   Ideally a domain-level UsuarioService implementation should be injected. This direct dependency
 *   is pragmatic for the incremental migration and preserves the legacy DB-access behavior.
 * - The RecadastramentoDAO already returns InstituicaoDTO instances. We still guard against nulls
 *   and convert to empty lists to preserve legacy expectations.
 */
@Service
public class RecadastramentoServiceImpl implements RecadastramentoService {

    private static final Logger log = LoggerFactory.getLogger(RecadastramentoServiceImpl.class);

    private final RecadastramentoDAO recadastramentoDAO;
    private final UsuarioDAO usuarioDAO;

    public RecadastramentoServiceImpl(RecadastramentoDAO recadastramentoDAO, UsuarioDAO usuarioDAO) {
        this.recadastramentoDAO = Objects.requireNonNull(recadastramentoDAO, "recadastramentoDAO must not be null");
        this.usuarioDAO = Objects.requireNonNull(usuarioDAO, "usuarioDAO must not be null");
    }

    /**
     * Legacy-compatible contract implementation.
     *
     * Delegates to RecadastramentoDAO and guarantees a non-null (possibly empty) list result.
     *
     * @param login user login used to filter institutions (may be null)
     * @return list of InstituicaoDTO (never null; empty if no data)
     */
    @Override
    public List<InstituicaoDTO> mostraInstituicoesEstadoMunicipio(String login) {
        try {
            List<InstituicaoDTO> result = recadastramentoDAO.mostraInstituicoesEstadoMunicipio(login);
            if (CollectionUtils.isEmpty(result)) {
                return Collections.emptyList();
            }
            return result;
        } catch (Exception ex) {
            // Defensive: preserve legacy behavior (don't propagate exceptions to callers)
            log.debug("Error while fetching instituicoes for login='{}' - returning empty list. reason={}",
                    login, ex.getMessage());
            return Collections.emptyList();
        }
    }

    /**
     * Convenience method to reproduce the legacy Action behavior:
     * - fetch list of institutions for the given login
     * - compute/lookup the "anobase" for the given usuarioCodigo
     *
     * This combines the two session attributes previously set by the legacy action into a single
     * response DTO suitable for a REST endpoint.
     *
     * NOTE:
     * - This method is not part of the RecadastramentoService interface to avoid changing the domain
     *   contract; controllers can cast/inject this implementation type when they need the combined response.
     *
     * @param login the user login used to filter institutions (may be null)
     * @param usuarioCodigo the numeric user code used to lookup anobase (may be null)
     * @return RecadastramentoResponseDTO with instituicoes (never null list) and anobase (string)
     */
    public RecadastramentoResponseDTO getRecadastramentoForUser(String login, Integer usuarioCodigo) {
        List<InstituicaoDTO> instituicoes = mostraInstituicoesEstadoMunicipio(login);

        // Retrieve anobase from UsuarioDAO. The DAO already implements the legacy fallback logic.
        Integer anobaseInt;
        try {
            anobaseInt = usuarioDAO.retornaAnoBase(usuarioCodigo);
            if (anobaseInt == null) {
                // Defensive: ensure non-null as legacy callers expected a value
                log.debug("UsuarioDAO.retornaAnoBase returned null for codigo={}, using current-year fallback", usuarioCodigo);
                anobaseInt = java.time.LocalDate.now().getYear();
            }
        } catch (Exception ex) {
            // If DB error occurs, fall back to computed default (matches legacy behavior in UsuarioDAO).
            log.debug("Error while fetching anobase for codigo={} - falling back to computed default. reason={}",
                    usuarioCodigo, ex.getMessage());
            anobaseInt = br.com.meta3.java.scaffold.domain.services.UsuarioService.computeFallbackAnoBase();
        }

        String anobaseStr = anobaseInt.toString();

        // Use the simple factory to build the response DTO.
        return RecadastramentoResponseDTO.of(instituicoes, anobaseStr);
    }

    // TODO: (REVIEW) If multiple callers require the combined response, consider adding this method to the
    // RecadastramentoService interface and introducing a domain-level Instituicao value object to remove
    // the upward dependency from domain -> api layers.
}