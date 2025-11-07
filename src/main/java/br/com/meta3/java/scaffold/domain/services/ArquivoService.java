package br.com.meta3.java.scaffold.domain.services;

import br.com.meta3.java.scaffold.domain.entities.Arquivo;
import java.time.LocalDate;
import java.util.List;

/**
 * Domain service interface for managing Arquivo entities.
 * Provides method to list Arquivos filtered by school code and a date range.
 */
public interface ArquivoService {

    /**
     * Retrieves a list of Arquivo entities filtered by school code and date range.
     *
     * @param codigoEscola the school code to filter by, must not be null
     * @param inicialData  the start date (inclusive) for filtering (based on Arquivo.dataUpload)
     * @param finalData    the end date (inclusive) for filtering (based on Arquivo.dataUpload)
     * @return list of Arquivo matching the given criteria
     */
    List<Arquivo> listArquivosCarregados(Long codigoEscola, LocalDate inicialData, LocalDate finalData);

    // TODO: (REVIEW) consider support for paging and sorting parameters if result sets grow large
}
