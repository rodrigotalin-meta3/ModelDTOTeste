package br.com.meta3.java.scaffold.domain.entities;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "arquivos")
public class Arquivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long codigoEscola;

    @Column(nullable = false)
    private String nomeArquivo;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dataUpload;

    @Column(nullable = false)
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime finalData;

    @Column(nullable = false)
    private Integer quantidadeRegistro;

    @Column(nullable = false)
    private Integer aptos;

    @Column(nullable = false)
    private Integer semDocumento;

    @Column(nullable = false)
    private Integer comCodigoSetps;

    @Column(nullable = false)
    private Integer comErro;

    public Arquivo() {
    }

    public Arquivo(Long codigoEscola, String nomeArquivo, LocalDateTime dataUpload) {
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
}
