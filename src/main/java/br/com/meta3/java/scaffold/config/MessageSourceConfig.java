filetype
package br.com.meta3.java.scaffold.config;

import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.ReloadableResourceBundleMessageSource;

import java.nio.charset.StandardCharsets;

/**
 * MessageSource configuration for resolving i18n message bundles.
 *
 * Purpose:
 * - Provide a MessageSource bean configured with basename 'messages' and UTF-8 encoding
 *   so migrated controllers (e.g., LegacyController) can resolve legacy keys such as
 *   "legacy.node.repeated" while still falling back to literal defaults when keys are absent.
 *
 * Decisions / notes:
 * - We use ReloadableResourceBundleMessageSource to allow reloading of message files during development
 *   (cacheSeconds controls how often they are refreshed). This is convenient for iterative migration work.
 * - Basename is configured as "classpath:messages" to align with standard Spring Boot message file
 *   locations (e.g., src/main/resources/messages.properties or messages_{locale}.properties).
 * - Default encoding is explicitly set to UTF-8 to ensure legacy strings with non-ASCII characters
 *   (e.g., "Nó repetido") are handled correctly.
 * - We set fallbackToSystemLocale(false) to avoid unexpected system-locale based resolution; controllers
 *   already pass Locale explicitly via LocaleContextHolder, and LegacyController uses a default message
 *   when keys are missing, so strict control over locale resolution is preferred during migration.
 *
 * TODO: (REVIEW)
 * - Consider exposing cacheSeconds via configuration (application.properties) for easier tuning between
 *   development and production environments.
 * - If an existing MessageSource bean is added elsewhere in the project, ensure only one primary MessageSource
 *   exists or mark the preferred one with @Primary. Currently this configuration provides a safe, explicit bean.
 */
@Configuration
public class MessageSourceConfig {

    @Bean
    public MessageSource messageSource() {
        ReloadableResourceBundleMessageSource msgSrc = new ReloadableResourceBundleMessageSource();
        // Basename 'messages' per migration checklist: resolves classpath:messages(.properties / _<locale>.properties)
        msgSrc.setBasename("classpath:messages");
        // Ensure UTF-8 encoding to correctly handle legacy literals with accents/diacritics
        msgSrc.setDefaultEncoding(StandardCharsets.UTF_8.name());
        // Cache for 1 hour to allow reloads during development without restarting the app
        msgSrc.setCacheSeconds(3600);
        // Do not fallback to system locale to keep resolution predictable; controllers pass Locale explicitly
        msgSrc.setFallbackToSystemLocale(false);

        // Note: We intentionally do not set 'useCodeAsDefaultMessage' here because legacy controllers
        // often provide an explicit default message when calling getMessage(...). Keeping this behavior
        // preserves the original fallback semantics (e.g., "Nó repetido").
        return msgSrc;
    }
}