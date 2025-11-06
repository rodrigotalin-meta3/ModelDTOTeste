package br.com.meta3.java.scaffold.api.controllers;

import br.com.meta3.java.scaffold.api.LegacyKeys;
import br.com.meta3.java.scaffold.api.dtos.LegacyMessageDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Locale;
import java.util.Objects;

/**
 * Controller exposing legacy-compatible endpoints.
 *
 * Exposes:
 *  - GET /api/legacy/node
 *
 * Behavior:
 *  - Reads the message key defined in LegacyKeys.LEGACY_NODE_REPEATED from the injected MessageSource using the current Locale.
 *  - If the message is not present, falls back to the original legacy text LegacyKeys.DEFAULT_LEGACY_NODE_REPEATED.
 *  - Returns a LegacyMessageDTO containing the resolved string.
 *
 * Decisions / notes:
 *  - Message key and default literal are centralized in LegacyKeys to avoid scattering string literals across the codebase.
 *  - We continue to use LocaleContextHolder.getLocale() to resolve messages according to the current request locale.
 *  - Default message is the legacy literal preserved in LegacyKeys.DEFAULT_LEGACY_NODE_REPEATED to preserve behavior
 *    when no message bundle entry is provided during migration.
 *  - We intentionally do not throw an error if the key is missing; returning the default preserves legacy robustness.
 *
 * TODO: (REVIEW) Consider centralizing legacy keys in a config-backed source if keys grow significantly.
 */
@RestController
@RequestMapping("/api/legacy")
public class LegacyController {

    private static final Logger log = LoggerFactory.getLogger(LegacyController.class);

    private final MessageSource messageSource;

    public LegacyController(MessageSource messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource must not be null");
    }

    /**
     * GET /api/legacy/node
     *
     * Reads the message key defined in LegacyKeys and returns it wrapped in LegacyMessageDTO.
     */
    @GetMapping("/node")
    public ResponseEntity<LegacyMessageDTO> getLegacyNode() {
        Locale locale = LocaleContextHolder.getLocale();
        // Use centralized constants for key and default to avoid hard-coded literals across the codebase.
        String key = LegacyKeys.LEGACY_NODE_REPEATED;
        String defaultMessage = LegacyKeys.DEFAULT_LEGACY_NODE_REPEATED;

        String message;
        try {
            // Use getMessage with an explicit default to avoid throwing NoSuchMessageException when key is missing.
            message = messageSource.getMessage(key, null, defaultMessage, locale);
        } catch (Exception ex) {
            // Defensive: in case of unexpected MessageSource errors, log and use default
            // TODO: (REVIEW) Consider capturing more detailed diagnostic info for i18n resolution failures.
            log.debug("Error resolving message '{}' for locale {}: {}. Using default.", key, locale, ex.getMessage());
            message = defaultMessage;
        }

        LegacyMessageDTO dto = LegacyMessageDTO.of(message);
        return ResponseEntity.ok(dto);
    }
}
