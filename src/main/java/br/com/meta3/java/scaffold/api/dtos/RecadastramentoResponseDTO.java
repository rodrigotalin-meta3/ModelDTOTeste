filetype
package br.com.meta3.java.scaffold.api.dtos;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Response DTO for recadastramento-related endpoints.
 *
 * This DTO replaces the legacy behavior where the controller/action set two session attributes:
 * - "listainstituicoes" (a list of institutions)
 * - "anobase" (a string representing the base year)
 *
 * Exposing both as a single response object simplifies REST usage and mirrors legacy data that
 * was previously stored in the HttpSession.
 */
public class RecadastramentoResponseDTO {

    private List<InstituicaoDTO> instituicoes;
    private String anobase;

    public RecadastramentoResponseDTO() {
    }

    public RecadastramentoResponseDTO(List<InstituicaoDTO> instituicoes, String anobase) {
        this.instituicoes = instituicoes;
        this.anobase = anobase;
    }

    public List<InstituicaoDTO> getInstituicoes() {
        return instituicoes;
    }

    public void setInstituicoes(List<InstituicaoDTO> instituicoes) {
        this.instituicoes = instituicoes;
    }

    public String getAnobase() {
        return anobase;
    }

    public void setAnobase(String anobase) {
        this.anobase = anobase;
    }

    /**
     * Simple factory method to create the response DTO.
     */
    public static RecadastramentoResponseDTO of(List<InstituicaoDTO> instituicoes, String anobase) {
        return new RecadastramentoResponseDTO(instituicoes, anobase);
    }

    /**
     * Helper factory to adapt legacy DAO outputs into the new response structure.
     *
     * Many legacy DAOs returned raw lists containing Map<String,Object> or other structures.
     * To ease migration we provide a factory that accepts a list of unknown objects and will
     * attempt to convert Map instances into InstituicaoDTO via InstituicaoDTO.fromMap(...).
     *
     * TODO: (REVIEW) This method tries a best-effort conversion; if non-map elements are encountered
     * and cannot be converted they will be skipped (nulls removed). If a stronger contract is
     * required, the calling service/controller should adapt legacy results before calling this factory.
     */
    @SuppressWarnings("unchecked")
    public static RecadastramentoResponseDTO fromLegacyInstituicoes(List<?> legacyList, String anobase) {
        if (legacyList == null) {
            return new RecadastramentoResponseDTO(null, anobase);
        }

        List<InstituicaoDTO> dtos = legacyList.stream()
                .map(item -> {
                    if (item == null) return null;

                    // Common legacy return type: Map<String, Object>
                    if (item instanceof Map) {
                        try {
                            return InstituicaoDTO.fromMap((Map<String, Object>) item);
                        } catch (ClassCastException ex) {
                            // TODO: (REVIEW) If legacy DAOs return other map-like types, adapt here.
                            return null;
                        }
                    }

                    // If the item is already an InstituicaoDTO, reuse it
                    if (item instanceof InstituicaoDTO) {
                        return (InstituicaoDTO) item;
                    }

                    // TODO: (REVIEW) Other legacy result structures (Object[] or custom beans) can be handled here if needed.
                    return null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        return new RecadastramentoResponseDTO(dtos, anobase);
    }

    @Override
    public String toString() {
        return "RecadastramentoResponseDTO{" +
                "instituicoes=" + instituicoes +
                ", anobase='" + anobase + '\'' +
                '}';
    }
}