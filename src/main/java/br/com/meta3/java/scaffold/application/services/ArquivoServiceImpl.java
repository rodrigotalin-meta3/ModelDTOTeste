package br.com.meta3.java.scaffold.application.services;

import br.com.meta3.java.scaffold.api.dtos.ArquivoDTO;
import br.com.meta3.java.scaffold.api.dtos.ListArquivosRequestDTO;
import br.com.meta3.java.scaffold.domain.entities.Arquivo;
import br.com.meta3.java.scaffold.domain.repositories.ArquivoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of the ArquivoService interface.
 */
@Service
public class ArquivoServiceImpl implements ArquivoService {

    private final ArquivoRepository arquivoRepository;

    public ArquivoServiceImpl(ArquivoRepository arquivoRepository) {
        this.arquivoRepository = arquivoRepository;
    }

    /**
     * Lists Arquivo records by school code and date range.
     *
     * @param requestDTO contains school code and date range for filtering
     * @return list of ArquivoDTO matching the filter
     */
    @Override
    @Transactional(readOnly = true)
    public List<ArquivoDTO> listArquivos(ListArquivosRequestDTO requestDTO) {
        // Convert LocalDate to LocalDateTime boundaries for querying between
        // TODO: (REVIEW) Ensure that finalData includes the full day till 23:59:59.999
        LocalDateTime startDateTime = requestDTO.getInicialData().atStartOfDay();
        LocalDateTime endDateTime = requestDTO.getFinalData()
                .atTime(LocalTime.MAX);

        List<Arquivo> arquivos = arquivoRepository
                .findByCodigoEscolaAndDataUploadBetween(
                        requestDTO.getCodigoEscola(),
                        startDateTime,
                        endDateTime
                );

        // Map entities to DTOs
        return arquivos.stream()
                .map(ArquivoDTO::fromEntity)
                .collect(Collectors.toList());
    }
}
