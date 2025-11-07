package br.com.meta3.java.scaffold.infrastructure.repositories;

import br.com.meta3.java.scaffold.domain.entities.Arquivo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Spring Data JPA repository for {@link Arquivo} entities.
 * Provides CRUD operations and custom finder for codigoEscola and dataUpload range queries.
 */
@Repository
public interface ArquivoRepository extends JpaRepository<Arquivo, Long> {

    /**
     * Finds all Arquivo entities matching the given school code and whose dataUpload
     * timestamp is between the specified start and end date-times (inclusive).
     *
     * @param codigoEscola the school code to filter by, must not be null
     * @param start        the inclusive lower bound of dataUpload
     * @param end          the inclusive upper bound of dataUpload
     * @return list of matching Arquivo entities
     */
    List<Arquivo> findByCodigoEscolaAndDataUploadBetween(
            Long codigoEscola,
            LocalDateTime start,
            LocalDateTime end
    );
}
