package br.com.meta3.java.scaffold.application.services;

import br.com.meta3.java.scaffold.api.dtos.ArquivoDTO;
import br.com.meta3.java.scaffold.api.dtos.ListArquivosRequestDTO;
import java.util.List;

/**
 * Application service interface for Arquivo-related business operations.
 */
public interface ArquivoService {

    /**
     * Lists Arquivo records according to the given filter criteria.
     *
     * @param requestDTO contains school code and date range for filtering
     * @return a list of ArquivoDTO matching the filter
     */
    List<ArquivoDTO> listArquivos(ListArquivosRequestDTO requestDTO);

    // TODO: (REVIEW) Consider adding pagination/support for large datasets if needed
}
