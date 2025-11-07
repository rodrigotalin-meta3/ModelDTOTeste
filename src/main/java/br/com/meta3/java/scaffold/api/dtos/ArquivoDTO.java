package br.com.meta3.java.scaffold.api.dtos;

import br.com.meta3.java.scaffold.domain.entities.Arquivo;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty; // Added for backward JSON compatibility
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * DTO for transferring Arquivo entity data in API responses.
 */
public class ArquivoDTO {

    private Long id;
    private Long codigoEscola;
    private String nomeArquivo;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataUpload;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    // Expose legacy finalData in API responses
    private LocalDateTime finalData;

    // New fields migrated from legacy code
    private Integer quantidadeRegistro;
    private Integer aptos;
    private Integer semDocumento;
    private Integer comCodigoSetps;
    private Integer comErro;

    public ArquivoDTO() {
    }

    public ArquivoDTO(Long id, Long codigoEscola, String nomeArquivo, LocalDateTime dataUpload) {
        this.id = id;
        this.codigoEscola = codigoEscola;
        this.nomeArquivo = nomeArquivo;
        this.dataUpload = dataUpload;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // TODO: (REVIEW) Maintain backward JSON compatibility for legacy 'codigodaescola' field
    @JsonProperty("codigodaescola")
    public Long getCodigoEscola() {
        return codigoEscola;
    }

    public void setCodigoEscola(Long codigoEscola) {
        this.codigoEscola = codigoEscola;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public LocalDateTime getDataUpload() {
        return dataUpload;
    }

    public void setDataUpload(LocalDateTime dataUpload) {
        this.dataUpload = dataUpload;
    }

    public LocalDateTime getFinalData() {
        return finalData;
    }

    public void setFinalData(LocalDateTime finalData) {
        this.finalData = finalData;
    }

    public Integer getQuantidadeRegistro() {
        return quantidadeRegistro;
    }

    public void setQuantidadeRegistro(Integer quantidadeRegistro) {
        this.quantidadeRegistro = quantidadeRegistro;
    }

    public Integer getAptos() {
        return aptos;
    }

    public void setAptos(Integer aptos) {
        this.aptos = aptos;
    }

    public Integer getSemDocumento() {
        return semDocumento;
    }

    public void setSemDocumento(Integer semDocumento) {
        this.semDocumento = semDocumento;
    }

    public Integer getComCodigoSetps() {
        return comCodigoSetps;
    }

    public void setComCodigoSetps(Integer comCodigoSetps) {
        this.comCodigoSetps = comCodigoSetps;
    }

    public Integer getComErro() {
        return comErro;
    }

    public void setComErro(Integer comErro) {
        this.comErro = comErro;
    }

    /**
     * Factory method to create ArquivoDTO from Arquivo entity.
     * @param arquivo the source entity
     * @return populated ArquivoDTO or null if source is null
     */
    public static ArquivoDTO fromEntity(Arquivo arquivo) {
        if (Objects.isNull(arquivo)) {
            return null;
        }
        ArquivoDTO dto = new ArquivoDTO(
            arquivo.getId(),
            arquivo.getCodigoEscola(),
            arquivo.getNomeArquivo(),
            arquivo.getDataUpload()
        );
        // TODO: (REVIEW) Exposing legacy finalData, ensure Arquivo entity has getFinalData()
        dto.setFinalData(arquivo.getFinalData());

        // Map migrated fields from Arquivo entity using proper camelCase getters
        dto.setQuantidadeRegistro(arquivo.getQuantidadeRegistro());
        dto.setAptos(arquivo.getAptos());
        dto.setSemDocumento(arquivo.getSemDocumento());
        dto.setComCodigoSetps(arquivo.getComCodigoSetps());
        dto.setComErro(arquivo.getComErro());

        return dto;
    }
}
