package br.com.meta3.java.scaffold.api.controllers;

import br.com.meta3.java.scaffold.api.dtos.ArquivoDTO;
import br.com.meta3.java.scaffold.api.dtos.ListArquivosRequestDTO;
import br.com.meta3.java.scaffold.domain.entities.Arquivo;
import br.com.meta3.java.scaffold.domain.services.ArquivoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller exposing endpoints for Arquivo operations.
 */
@RestController
@RequestMapping("/arquivos")
public class ArquivoController {

    private final ArquivoService arquivoService;

    public ArquivoController(ArquivoService arquivoService) {
        this.arquivoService = arquivoService;
    }

    /**
     * GET /arquivos
     * List all Arquivos filtered by school code and date range.
     *
     * @param filterDto request parameters for filtering (codigoEscola, inicialData, finalData)
     * @return list of ArquivoDTO matching the filter
     */
    @GetMapping
    public ResponseEntity<List<ArquivoDTO>> listArquivos(
            @Valid @ModelAttribute ListArquivosRequestDTO filterDto
    ) {
        // Use getCodigoEscola (replacing any legacy getCodigodaescola) on DTO for filter
        List<Arquivo> arquivos = arquivoService.listArquivosCarregados(
                filterDto.getCodigoEscola(),
                filterDto.getInicialData(),
                filterDto.getFinalData()
        );

        // Map domain entities to DTOs for API response
        List<ArquivoDTO> dtos = arquivos.stream()
                .map(ArquivoDTO::fromEntity)
                .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }
}
