package br.com.meta3.java.scaffold.domain.entities;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the migrated Instituicao entity focusing on the cnpj setter semantics.
 *
 * Goals:
 * - Ensure setCnpj stores the provided value.
 * - Ensure setCnpj accepts null and getCnpj returns null.
 * - Ensure whitespace provided to setCnpj is preserved (legacy semantics).
 *
 * Notes / decisions:
 * - The Instituicao entity may be introduced later in the migration. Keeping these tests now
 *   documents the expected API and behavior for the cnpj property.
 * - If the Instituicao class is not yet present, these tests will intentionally cause compilation
 *   failures to signal that the entity must implement getCnpj()/setCnpj(String).
 *
 * TODO: (REVIEW) If business rules require trimming/validation of cnpj in the future, update tests
 * accordingly and add validation-focused test cases.
 */
public class InstituicaoTest {

    @Test
    public void testSetAndGetCnpj_shouldStoreAndReturnValue() {
        Instituicao inst = new Instituicao();
        String expected = "12.345.678/0001-99";

        inst.setCnpj(expected);

        assertEquals(expected, inst.getCnpj(), "setCnpj/getCnpj should preserve the assigned value");
    }

    @Test
    public void testSetCnpj_allowsNullAndRetrievesNull() {
        Instituicao inst = new Instituicao();

        inst.setCnpj(null);

        assertNull(inst.getCnpj(), "setCnpj should accept null and getCnpj should return null");
    }

    @Test
    public void testSetCnpj_preservesWhitespaceExact() {
        Instituicao inst = new Instituicao();
        String valueWithSpaces = "  12.345.678/0001-99  ";

        // Legacy semantics: setter should store the exact string provided unless business rules demand trimming.
        // This test asserts current expected behavior is to preserve the exact provided value.
        inst.setCnpj(valueWithSpaces);

        assertEquals(valueWithSpaces, inst.getCnpj(),
                "setCnpj should preserve the exact string given (trimming not applied by default)");
    }
}
