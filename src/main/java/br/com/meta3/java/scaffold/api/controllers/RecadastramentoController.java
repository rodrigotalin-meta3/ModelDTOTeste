filetype
package br.com.meta3.java.scaffold.api.controllers;

import br.com.meta3.java.scaffold.api.dtos.InstituicaoDTO;
import br.com.meta3.java.scaffold.api.dtos.RecadastramentoResponseDTO;
import br.com.meta3.java.scaffold.domain.services.RecadastramentoService;
import br.com.meta3.java.scaffold.domain.services.UsuarioService;
import jakarta.servlet.http.HttpSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * REST controller that exposes legacy-compatible recadastramento endpoints.
 *
 * Endpoint:
 *  - GET /api/recadastramento/instituicoes
 *
 * Behavior:
 *  - Reads HttpSession attributes "informacoesusuario" and "login" to preserve legacy contract.
 *  - Delegates to RecadastramentoService.mostraInstituicoesEstadoMunicipio(login) to obtain institutions list.
 *  - Delegates to UsuarioService.retornaAnoBase(codigoUsuario) to compute/lookup the anobase.
 *  - Returns RecadastramentoResponseDTO containing the institutions list and the anobase string.
 *
 * Notes / Decisions:
 *  - The legacy "informacoesusuario" session attribute may be a domain object, a map or simply a numeric/string code.
 *    To remain robust during migration we attempt several strategies to extract a numeric user code:
 *      1) handle Number and String directly
 *      2) handle Map with common keys ("codigo", "codigoUsuario", "id")
 *      3) attempt reflective invocation of "getCodigo", "getCodigoUsuario", "getId" if present
 *
 *  - Reflection is used as a pragmatic migration aid to avoid creating rigid domain types now.
 *    TODO: (REVIEW) Replace reflective extraction with a well-defined session DTO once legacy session population
 *    is migrated to typed objects.
 *
 *  - If no user code can be extracted, we pass null to UsuarioService.retornaAnoBase(...) so the service
 *    will apply its fallback logic (preserves legacy behavior).
 */
@RestController
@RequestMapping("/api/recadastramento")
public class RecadastramentoController {

    private static final Logger log = LoggerFactory.getLogger(RecadastramentoController.class);

    private final RecadastramentoService recadastramentoService;
    private final UsuarioService usuarioService;

    public RecadastramentoController(RecadastramentoService recadastramentoService,
                                     UsuarioService usuarioService) {
        this.recadastramentoService = Objects.requireNonNull(recadastramentoService, "recadastramentoService must not be null");
        this.usuarioService = Objects.requireNonNull(usuarioService, "usuarioService must not be null");
    }

    /**
     * Legacy-compatible endpoint to obtain institutions and anobase.
     *
     * Reads session attributes:
     *  - "login" -> used to filter institutions
     *  - "informacoesusuario" -> used to obtain usuario.codigo to compute anobase
     *
     * Returns a JSON payload containing:
     *  {
     *    "instituicoes": [ ... ],
     *    "anobase": "2025"
     *  }
     */
    @GetMapping("/instituicoes")
    public ResponseEntity<RecadastramentoResponseDTO> getInstituicoes(HttpSession session) {
        Object informacoesUsuario = session.getAttribute("informacoesusuario");
        Object loginObj = session.getAttribute("login");

        String login = loginObj != null ? loginObj.toString() : null;

        Integer usuarioCodigo = extractUsuarioCodigo(informacoesUsuario);

        if (log.isDebugEnabled()) {
            log.debug("getInstituicoes called - login='{}', usuarioCodigo='{}'", login, usuarioCodigo);
        }

        // Fetch institutions (legacy DAO returns empty list on errors; service contracts preserve that)
        List<InstituicaoDTO> instituicoes = recadastramentoService.mostraInstituicoesEstadoMunicipio(login);

        // Fetch/compute anobase via domain service - it MUST return a non-null Integer per contract
        Integer anobaseInt = usuarioService.retornaAnoBase(usuarioCodigo);
        if (anobaseInt == null) {
            // Defensive fallback — should not happen if UsuarioService contract is respected
            log.debug("UsuarioService.retornaAnoBase returned null for codigo={}; using current year fallback", usuarioCodigo);
            anobaseInt = java.time.LocalDate.now().getYear();
        }

        RecadastramentoResponseDTO response = RecadastramentoResponseDTO.of(instituicoes, anobaseInt.toString());
        return ResponseEntity.ok(response);
    }

    /**
     * Try multiple strategies to extract an Integer "codigo" from the legacy session attribute.
     *
     * Strategies (in order):
     *  - If attribute is null -> null
     *  - If Number -> intValue
     *  - If String -> parseInt (if possible)
     *  - If Map -> check common keys: "codigo", "codigoUsuario", "id"
     *  - Reflection -> try methods: getCodigo(), getCodigoUsuario(), getId()
     *
     * TODO: (REVIEW) Remove reflection and Map handling when session population is migrated to a typed DTO.
     */
    @SuppressWarnings("unchecked")
    private Integer extractUsuarioCodigo(Object informacoesUsuario) {
        if (informacoesUsuario == null) {
            return null;
        }

        // Direct numeric types
        if (informacoesUsuario instanceof Number) {
            return ((Number) informacoesUsuario).intValue();
        }

        // String value (possibly numeric)
        if (informacoesUsuario instanceof String) {
            try {
                return Integer.parseInt(((String) informacoesUsuario).trim());
            } catch (NumberFormatException ex) {
                // Not a numeric string; continue to other extraction strategies
            }
        }

        // Map-based legacy object (common when DAOs returned maps)
        if (informacoesUsuario instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) informacoesUsuario;
            Object[] candidateKeys = new Object[] { "codigo", "codigoUsuario", "id", "codigo_usr" };
            for (Object keyObj : candidateKeys) {
                String key = keyObj.toString();
                if (map.containsKey(key)) {
                    Object val = map.get(key);
                    Integer parsed = tryParseToInteger(val);
                    if (parsed != null) {
                        return parsed;
                    }
                }
            }
        }

        // Reflection fallback: try common getter names on the legacy user object
        String[] methodNames = new String[] { "getCodigo", "getCodigoUsuario", "getId", "codigo", "codigoUsuario" };
        for (String methodName : methodNames) {
            try {
                Method m = informacoesUsuario.getClass().getMethod(methodName);
                if (m != null) {
                    Object val = m.invoke(informacoesUsuario);
                    Integer parsed = tryParseToInteger(val);
                    if (parsed != null) {
                        return parsed;
                    }
                }
            } catch (NoSuchMethodException nsme) {
                // ignore and try next
            } catch (Exception ex) {
                // Any reflective error should not break the flow; log debug and continue
                log.debug("Reflection while extracting usuarioCodigo failed for method='{}' on type='{}' - reason={}",
                        methodName, informacoesUsuario.getClass().getName(), ex.getMessage());
            }
        }

        // Could not extract a numeric codigo
        return null;
    }

    private Integer tryParseToInteger(Object val) {
        if (val == null) {
            return null;
        }
        if (val instanceof Number) {
            return ((Number) val).intValue();
        }
        if (val instanceof String) {
            try {
                return Integer.parseInt(((String) val).trim());
            } catch (NumberFormatException ex) {
                return null;
            }
        }
        return null;
    }
}