package br.com.meta3.java.scaffold.api.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

/**
 * DTO for requesting a list of Arquivo entities filtered by school code and date range.
 */
public class ListArquivosRequestDTO {

    @NotNull(message = "School code must be provided")
    @Positive(message = "School code must be a positive number")
    private Long codigoEscola;

    @NotNull(message = "Start date (inicialData) must be provided")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate inicialData;

    @NotNull(message = "End date (finalData) must be provided")
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate finalData;

    /**
     * Cross-field validation to ensure the start date is on or before the end date.
     * TODO: (REVIEW) We skip null checks here as @NotNull annotations handle those cases.
     */
    @AssertTrue(message = "inicialData must be on or before finalData")
    public boolean isDateRangeValid() {
        if (inicialData == null || finalData == null) {
            return true;
        }
        return !inicialData.isAfter(finalData);
    }

    public Long getCodigoEscola() {
        return codigoEscola;
    }

    public void setCodigoEscola(Long codigoEscola) {
        this.codigoEscola = codigoEscola;
    }

    public LocalDate getInicialData() {
        return inicialData;
    }

    public void setInicialData(LocalDate inicialData) {
        this.inicialData = inicialData;
    }

    public LocalDate getFinalData() {
        return finalData;
    }

    public void setFinalData(LocalDate finalData) {
        this.finalData = finalData;
    }
}
