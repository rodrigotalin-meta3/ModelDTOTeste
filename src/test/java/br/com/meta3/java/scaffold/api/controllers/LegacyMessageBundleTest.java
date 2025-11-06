filetype
package br.com.meta3.java.scaffold.api.controllers;

import br.com.meta3.java.scaffold.api.LegacyKeys;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Integration test that verifies MessageSource wiring and fallback behavior for legacy message keys.
 *
 * Purpose:
 * - Ensure that resolving the key 'legacy.node.repeated' via the application's MessageSource
 *   returns the provided legacy default ("Nó repetido") when no message bundle entry is present.
 *
 * Decisions / notes:
 * - We intentionally call MessageSource.getMessage(key, null, defaultMessage, locale) with an explicit default
 *   to mirror how controllers (e.g., LegacyController) resolve legacy messages and to avoid depending on
 *   useCodeAsDefaultMessage semantics.
 * - Using @SpringBootTest loads the real MessageSource bean configured by MessageSourceConfig so this test
 *   verifies actual wiring (encoding, basename, etc.) in the application context.
 *
 * TODO: (REVIEW) If future CI environments provide message bundles containing the legacy key, adapt this test
 * to load a test-specific MessageSource or assert localized value instead of the literal default.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class LegacyMessageBundleTest {

    @Autowired
    private MessageSource messageSource;

    @Test
    public void messageSource_whenResolvingLegacyKey_withoutBundle_returnsDefaultLegacyText() {
        Locale locale = LocaleContextHolder.getLocale();

        // Use the same constants the application uses to preserve consistency with controllers.
        String resolved = messageSource.getMessage(
                LegacyKeys.LEGACY_NODE_REPEATED,
                null,
                LegacyKeys.DEFAULT_LEGACY_NODE_REPEATED,
                locale
        );

        // Assert that when no bundle provides the key, the explicit default (legacy literal) is returned.
        assertEquals(LegacyKeys.DEFAULT_LEGACY_NODE_REPEATED, resolved,
                "MessageSource should return the provided legacy default when the key is absent from bundles");
    }

    // TODO: (REVIEW) Consider adding a test variant that registers a test message bundle to assert
    //               localized values when the key is present, ensuring full i18n behavior works as expected.
}