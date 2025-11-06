filetype
package br.com.meta3.java.scaffold.api;

/**
 * Holder for legacy message keys and related defaults used across the migrated API.
 *
 * Purpose:
 * - Centralize message keys (i18n codes) that were previously scattered as string literals
 *   across controllers and services (e.g., "legacy.node.repeated").
 * - Provide a single place to find/modify legacy keys and their literal fallbacks to ease
 *   future refactoring and tests.
 *
 * Usage:
 * - Replace occurrences of hard-coded keys with LegacyKeys.LEGACY_NODE_REPEATED
 *   and use LegacyKeys.DEFAULT_LEGACY_NODE_REPEATED when a literal fallback is required.
 *
 * Design decisions / notes:
 * - We include both the message key constant and the legacy literal default for convenience.
 *   Controllers should still prefer calling MessageSource.getMessage(key, ..., defaultMessage, locale)
 *   to keep i18n behavior and explicit fallbacks.
 * - Keeping defaults close to keys helps reviewers understand the intended fallback text during migration.
 * - This holder is intentionally minimal; if the number of legacy keys grows, consider grouping keys by domain
 *   or moving to a typed enum or a generated constants class from message bundles.
 *
 * TODO: (REVIEW)
 * - Replace occurrences of string literals (e.g., "legacy.node.repeated" and "Nó repetido") across controllers
 *   with these constants. Prefer injecting/reading keys from config or resource bundles for greater flexibility.
 * - Consider annotating constants with metadata (e.g., source file/legacy id) if tracing back to legacy code is required.
 */
public final class LegacyKeys {

    private LegacyKeys() {
        // utility class - prevent instantiation
    }

    /**
     * Message key for the legacy "node repeated" message.
     *
     * Legacy controllers/services read this key from MessageSource.
     * Keep the key in sync with any entries added to src/main/resources/messages*.properties.
     */
    public static final String LEGACY_NODE_REPEATED = "legacy.node.repeated";

    /**
     * Literal fallback used by legacy code when the message key is not present in bundles.
     *
     * NOTE:
     * - The literal contains non-ASCII characters; ensure message bundle files are encoded in UTF-8.
     * - Controllers currently pass explicit defaults when resolving messages; prefer keeping that explicit
     *   call site rather than using MessageSource.useCodeAsDefaultMessage to preserve legacy semantics.
     */
    public static final String DEFAULT_LEGACY_NODE_REPEATED = "Nó repetido";

    // ---------------------------------------------------------------------
    // Add other legacy message keys / defaults below as migration progresses.
    // ---------------------------------------------------------------------

    // Example placeholder (uncomment and use when needed):
    // public static final String LEGACY_SOME_OTHER_KEY = "legacy.some.other.key";
    // public static final String DEFAULT_LEGACY_SOME_OTHER_KEY = "Texto legado para outro caso";

    // TODO: (REVIEW) If more keys are added, consider grouping constants into nested classes
    // by feature (e.g., RecadastramentoKeys, ArquivoKeys) to improve discoverability.
}