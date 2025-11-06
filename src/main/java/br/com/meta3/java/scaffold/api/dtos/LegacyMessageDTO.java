filetype
package br.com.meta3.java.scaffold.api.dtos;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Objects;

/**
 * Simple DTO to carry legacy-style messages in API responses.
 *
 * Purpose:
 * - Provide a structured JSON object for controllers/services that need to return a single
 *   human-readable message (eg. migration notices, legacy error messages, informational text).
 *
 * Notes / Decisions:
 * - Kept intentionally minimal (single 'message' property) to match the migration task requirements.
 * - We use Jackson's @JsonProperty to ensure the JSON field remains "message" even if refactoring occurs.
 * - TODO: (REVIEW) Consider replacing ad-hoc message DTOs with a standardized problem/details representation
 *   (e.g., RFC 7807 Problem JSON) if the project adopts a richer error-handling strategy.
 */
public class LegacyMessageDTO {

    @JsonProperty("message")
    private String message;

    public LegacyMessageDTO() {
    }

    public LegacyMessageDTO(String message) {
        this.message = message;
    }

    /**
     * Convenience factory method for clearer controller/service usage.
     *
     * TODO: (REVIEW) If localization is required, consider adding locale/message-key support instead of raw messages.
     */
    public static LegacyMessageDTO of(String message) {
        return new LegacyMessageDTO(message);
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // Basic equals/hashCode to ease testing and comparisons in service/controller unit tests.
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LegacyMessageDTO)) return false;
        LegacyMessageDTO that = (LegacyMessageDTO) o;
        return Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(message);
    }

    @Override
    public String toString() {
        return "LegacyMessageDTO{message='" + message + "'}";
    }
}