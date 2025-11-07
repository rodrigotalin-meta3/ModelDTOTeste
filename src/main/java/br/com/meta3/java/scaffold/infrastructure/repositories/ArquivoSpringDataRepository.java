package br.com.meta3.java.scaffold.infrastructure.repositories;

import br.com.meta3.java.scaffold.domain.entities.Arquivo;
// TODO: (REVIEW) Ensure ArquivoRepository interface is created under domain.repositories
import br.com.meta3.java.scaffold.domain.repositories.ArquivoRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for Arquivo entity.
 * Extends JpaRepository for basic CRUD operations and ArquivoRepository for domain-level abstraction.
 */
@Repository
public interface ArquivoSpringDataRepository 
        extends JpaRepository<Arquivo, Long>, ArquivoRepository {

    /**
     * Migrated method from legacy ArquivoDAO.listarArquivosCarregados.
     * Lists Arquivo records filtered by school code and upload date range.
     *
     * @param codigoEscola the school code to filter by
     * @param inicialData  the start of upload date-time range
     * @param finalData    the end of upload date-time range
     * @return list of matching Arquivo entities
     */
    List<Arquivo> findByCodigoEscolaAndDataUploadBetween(
            Long codigoEscola, 
            LocalDateTime inicialData, 
            LocalDateTime finalData
    );
}
