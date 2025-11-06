filetype
package br.com.meta3.java.scaffold.domain.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the legacy-compatible UsuarioLegacy entity focusing on the 'codigo' property.
 *
 * Goals:
 * - Ensure getCodigo()/setCodigo(Integer) preserve the assigned Integer value.
 * - Ensure the setter accepts null and the getter returns null in that case.
 * - Ensure value semantics are preserved (Integer immutability) so reflection-based extraction used
 *   in migration helpers (e.g., RecadastramentoController.extractUsuarioCodigo) remains safe and predictable.
 *
 * Notes / decisions:
 * - The UsuarioLegacy class may be introduced as part of the migration. Keeping these tests now documents
 *   the expected API (getCodigo/setCodigo) required by reflection-based extraction strategies used across the codebase.
 * - If the UsuarioLegacy class is not present yet, these tests will intentionally cause compilation failures to
 *   signal that the entity must implement getCodigo()/setCodigo(Integer).
 *
 * TODO: (REVIEW) Once reflection-based extraction is removed in favor of typed session DTOs, update or remove these tests.
 */
public class UsuarioLegacyTest {

    @Test
    public void testSetAndGetCodigo_shouldStoreAndReturnValue() {
        UsuarioLegacy usuario = new UsuarioLegacy();
        Integer expected = Integer.valueOf(12345);

        usuario.setCodigo(expected);

        assertEquals(expected, usuario.getCodigo(), "setCodigo/getCodigo should preserve the assigned Integer value");
    }

    @Test
    public void testSetCodigo_allowsNullAndRetrievesNull() {
        UsuarioLegacy usuario = new UsuarioLegacy();

        usuario.setCodigo(null);

        assertNull(usuario.getCodigo(), "setCodigo should accept null and getCodigo should return null");
    }

    @Test
    public void testSetCodigo_preservesValueSemantics_whenOriginalVariableChanges() {
        UsuarioLegacy usuario = new UsuarioLegacy();
        Integer original = Integer.valueOf(1000);

        usuario.setCodigo(original);

        // Mutate the local reference to a different Integer instance to assert stored value is immutable and preserved.
        original = Integer.valueOf(2000);

        // The stored value must remain the original one (Integer is immutable, so this verifies value semantics).
        assertEquals(Integer.valueOf(1000), usuario.getCodigo(),
                "UsuarioLegacy should preserve the stored Integer value regardless of later changes to the original reference");
    }

    // TODO: (REVIEW) Consider adding tests that validate reflective extraction helpers (e.g., invoking getCodigo via reflection)
    // once UsuarioLegacy is present and reflection-based code paths are stabilized. This will ensure migration helpers remain robust.
}