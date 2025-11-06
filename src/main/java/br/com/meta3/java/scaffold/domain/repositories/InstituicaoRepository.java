filetype
package br.com.meta3.java.scaffold.domain.repositories;

import br.com.meta3.java.scaffold.domain.entities.Instituicao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository to access Instituicao data using Spring Data JPA.
 *
 * Purpose:
 * - Provide a typed JpaRepository for a future/domain Instituicao entity to replace native-query usage in RecadastramentoDAO.
 * - Expose both derived query signatures (when an Instituicao entity exists and is mapped) and native-query helpers
 *   that return raw Object[] rows to ease incremental migration from legacy native queries.
 *
 * Design decisions / notes:
 * - We reference a domain entity Instituicao (br.com.meta3.java.scaffold.domain.entities.Instituicao).
 *   If that entity isn't implemented yet, this file documents the intended repository API for the migration.
 * - Two styles of access are provided:
 *     1) findByUsuarioLogin(String login) -> idiomatic Spring Data derived query (requires Instituicao entity mapping
 *        with a column/property 'usuarioLogin' or appropriate @Column / @Join mapping).
 *     2) findInstituicoesNativeByLogin(...) and findInstituicoesFromAluMecTacom(...) -> native queries returning List<Object[]>
 *        so that the migration can progressively adopt typed mappings. These helpers mirror the native SQL used previously
 *        in RecadastramentoDAO and make it straightforward to port logic to a repository-backed implementation.
 *
 * - The legacy fallback against alunos.alu_mec_tacom is preserved as a native query method that returns Object[] rows
 *   (codigosec, nome, bairro). Callers should adapt/parse the returned rows into DTOs (InstituicaoDTO) as needed.
 *
 * TODO: (REVIEW)
 * - Implement br.com.meta3.java.scaffold.domain.entities.Instituicao and migrate RecadastramentoDAO to use this repository's
 *   typed methods (remove native queries and Object[] handling).
 * - Consider introducing a projection interface (e.g., InstituicaoProjection) to return typed results from native queries
 *   without requiring a full entity mapping during the early migration phase.
 */
@Repository
public interface InstituicaoRepository extends JpaRepository<Instituicao, Long> {

    /**
     * Derived query to find instituicoes by the associated user login.
     *
     * Migration note:
     * - This method expects an Instituicao entity with a property/column that can be filtered by 'usuarioLogin'.
     * - Prefer this method once the entity and proper mapping are in place (it avoids native SQL and eases testing).
     */
    List<Instituicao> findByUsuarioLogin(String login);

    /**
     * Native query that mirrors the primary SQL used in RecadastramentoDAO.
     *
     * Returns raw Object[] rows where:
     *  - row[0] -> id
     *  - row[1] -> nome
     *  - row[2] -> estado
     *  - row[3] -> municipio
     *
     * This method is intentionally kept to help staged migration: callers can parse Object[] -> InstituicaoDTO
     * without requiring the Instituicao entity to exist yet.
     */
    @Query(value = "SELECT id, nome, estado, municipio FROM instituicoes WHERE usuario_login = :login", nativeQuery = true)
    List<Object[]> findInstituicoesNativeByLogin(@Param("login") String login);

    /**
     * Legacy-table fallback against alunos.alu_mec_tacom.
     *
     * Returns raw Object[] rows where:
     *  - row[0] -> codigosec
     *  - row[1] -> nome
     *  - row[2] -> bairro (mapped to municipio by callers)
     *
     * The method accepts a boolean flag 'stateLevel' to reproduce the legacy branching:
     *  - stateLevel == true  -> length(codigosec) = 7
     *  - stateLevel == false -> length(codigosec) < 7
     *
     * NOTE:
     * - Some databases may handle boolean parameters differently in native queries. This method is intended
     *   as a migration aid; if portability issues arise replace it with two explicit methods or build the SQL
     *   dynamically in a DAO/service layer.
     */
    @Query(value = "SELECT codigosec, nome, bairro FROM alunos.alu_mec_tacom " +
            "WHERE ((:stateLevel = true AND LENGTH(codigosec) = 7) OR (:stateLevel = false AND LENGTH(codigosec) < 7)) " +
            "AND cod_titular IS NOT NULL " +
            "AND ano_base_exclusao IS NULL " +
            "ORDER BY codigosec ASC",
            nativeQuery = true)
    List<Object[]> findInstituicoesFromAluMecTacom(@Param("stateLevel") boolean stateLevel);

    /**
     * Convenience default method to reproduce legacy login-based branching.
     *
     * - If login equals "9999" -> state-level (length = 7)
     * - Otherwise -> municipality-level (length < 7)
     *
     * This default method delegates to the native query above and centralizes the branching decision to simplify callers.
     *
     * TODO: (REVIEW) If repositories must remain purely declarative, move this helper into a service/DAO class.
     */
    default List<Object[]> findInstituicoesFromAluMecTacomForLogin(String login) {
        boolean stateLevel = "9999".equals(login);
        return findInstituicoesFromAluMecTacom(stateLevel);
    }
}