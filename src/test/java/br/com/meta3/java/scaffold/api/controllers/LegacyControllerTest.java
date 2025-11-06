filetype
package br.com.meta3.java.scaffold.api.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration test for LegacyController to verify legacy message fallback is preserved.
 *
 * Intent:
 * - Load Spring context and perform an HTTP GET against /api/legacy/node
 * - Assert the returned JSON contains { "message": "Nó repetido" }
 *
 * Decisions / notes:
 * - We use @SpringBootTest with @AutoConfigureMockMvc to start the full web context so the controller
 *   is exercised with real MessageSource wiring. This mirrors real runtime environment and avoids
 *   needing to mock MessageSource behavior.
 * - The LegacyController intentionally falls back to the literal "Nó repetido" when the message key
 *   is absent in bundles. This test verifies that fallback is effective in the migrated application.
 * - If future localization bundles provide the key "legacy.node.repeated", this test may need to be
 *   adapted to configure a MessageSource for predictable results (e.g., using @TestConfiguration).
 *
 * TODO: (REVIEW) If tests run in environments where default locale or message bundles differ, consider
 * adding a dedicated MessageSource test bean to guarantee deterministic localization behavior.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public class LegacyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getLegacyNode_whenNoMessageBundleProvided_returnsLegacyDefaultText() throws Exception {
        // Perform GET /api/legacy/node and expect the legacy fallback string "Nó repetido"
        mockMvc.perform(get("/api/legacy/node"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/json"))
                .andExpect(jsonPath("$.message", is("Nó repetido")));
    }
}