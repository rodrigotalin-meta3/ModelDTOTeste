package br.com.meta3.java.scaffold.application.services;

import br.com.meta3.java.scaffold.domain.entities.Arquivo;
import br.com.meta3.java.scaffold.domain.services.ArquivoService;
import br.com.meta3.java.scaffold.infrastructure.repositories.ArquivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Service implementation for managing Arquivo entities.
 */
@Service
public class ArquivoServiceImpl implements ArquivoService {

    private final ArquivoRepository arquivoRepository;

    public ArquivoServiceImpl(ArquivoRepository arquivoRepository) {
        this.arquivoRepository = arquivoRepository;
    }

    /**
     * Retrieves a list of Arquivo entities filtered by school code and date range.
     *
     * NOTE: Using 'codigoEscola' consistently in place of legacy 'codigodaescola'.
     *
     * @param codigoEscola the school code to filter by, must not be null
     * @param inicialData  the start date (inclusive) for filtering based on dataUpload
     * @param finalData    the end date (inclusive) for filtering based on dataUpload
     * @return list of Arquivo matching the criteria
     */
    @Override
    @Transactional(readOnly = true)
    public List<Arquivo> listArquivosCarregados(Long codigoEscola, LocalDate inicialData, LocalDate finalData) {
        // Convert LocalDate to LocalDateTime for range query
        LocalDateTime startDateTime = inicialData.atStartOfDay();
        // Include the entire final date up to the last nanosecond
        LocalDateTime endDateTime = finalData.atTime(LocalTime.MAX);

        // TODO: (REVIEW) Consider adding pagination and sorting if the result set grows large
        return arquivoRepository.findByCodigoEscolaAndDataUploadBetween(
                codigoEscola,
                startDateTime,
                endDateTime
        );
    }
}
