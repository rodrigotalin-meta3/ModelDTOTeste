package br.com.meta3.java.scaffold.api.controllers;

import br.com.meta3.java.scaffold.api.dtos.ArquivoDTO;
import br.com.meta3.java.scaffold.api.dtos.ListArquivosRequestDTO;
import br.com.meta3.java.scaffold.application.services.ArquivoService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for Arquivo operations.
 */
@RestController
@RequestMapping(path = "/arquivos", produces = MediaType.APPLICATION_JSON_VALUE)
public class ArquivoController {

    private final ArquivoService arquivoService;

    public ArquivoController(ArquivoService arquivoService) {
        this.arquivoService = arquivoService;
    }

    /**
     * Lists Arquivo records filtered by school code and date range.
     *
     * @param requestDTO filter parameters bound from query parameters
     * @return list of ArquivoDTO matching the filter
     */
    @GetMapping
    public ResponseEntity<List<ArquivoDTO>> listArquivos(
            @Valid @ModelAttribute ListArquivosRequestDTO requestDTO
    ) {
        // TODO: (REVIEW) Using @ModelAttribute to bind GET query params into DTO
        List<ArquivoDTO> arquivos = arquivoService.listArquivos(requestDTO);
        return ResponseEntity.ok(arquivos);
    }
}
