package br.com.meta3.java.scaffold.domain.repositories;

import br.com.meta3.java.scaffold.domain.entities.AluMecTacom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Spring Data JPA repository to access the legacy alunos.alu_mec_tacom table.
 *
 * Purpose:
 * - Provide a typed repository to simplify migrating RecadastramentoDAO legacy fallback logic to JPA.
 * - Expose native queries that mirror the legacy filters used by the original DAO so migration code can
 *   gradually replace raw native EntityManager usage with repository calls.
 *
 * Important notes / decisions:
 * - The AluMecTacom entity is expected to be created under:
 *     br.com.meta3.java.scaffold.domain.entities.AluMecTacom
 *   with mappings for at least the columns: codigosec, nome, bairro, cod_titular, ano_base_exclusao.
 *   TODO: (REVIEW) Create the AluMecTacom entity with appropriate @Table(schema = "alunos", name = "alu_mec_tacom")
 *         and column mappings. This repository currently references that entity and will not compile until the entity exists.
 *
 * - We expose nativeQuery methods that return List<Object[]> because legacy schema columns and types may be heterogeneous
 *   and to avoid prematurely coupling to a domain-level projection. When the AluMecTacom entity is implemented, these
 *   methods can be migrated to return List<AluMecTacom> or a typed projection interface.
 *
 * - Two native query helpers are provided:
 *     - findByCodigosecLengthEquals(len): intended for the "state-level" case (length = 7)
 *     - findByCodigosecLengthLessThan(len): intended for the "municipality-level" case (length < 7)
 *
 * - A convenience default method findForLogin(login) reproduces the legacy branching used in RecadastramentoDAO:
 *     if login == "9999" then length(codigosec) = 7 else length(codigosec) < 7
 *
 * - Using LENGTH(...) in native queries is portable across many DBs (Oracle, H2, SQLServer use LENGTH/len differences).
 *   If portability issues arise for a target DB, adapt the query or provide DB-specific query definitions.
 *
 * TODO: (REVIEW)
 * - Replace List<Object[]> return types with a typed projection (interface-based) or entity mapping once
 *   AluMecTacom entity is implemented.
 * - Consider adding methods that return InstituicaoDTO (or a domain-level Instituicao) directly to simplify DAO migration.
 */
public interface AluMecTacomRepository extends JpaRepository<AluMecTacom, Long> {

    /**
     * Native query that selects the legacy columns used by fallback logic:
     * codigosec, nome, bairro
     *
     * Note: returns Object[] rows where:
     *  - row[0] -> codigosec
     *  - row[1] -> nome
     *  - row[2] -> bairro
     *
     * @param len the exact length to match for codigosec (e.g. 7 for state-level)
     * @return list of result rows as Object[]
     */
    @Query(value = "SELECT codigosec, nome, bairro FROM alunos.alu_mec_tacom " +
                   "WHERE LENGTH(codigosec) = :len " +
                   "AND cod_titular IS NOT NULL " +
                   "AND ano_base_exclusao IS NULL " +
                   "ORDER BY codigosec ASC",
           nativeQuery = true)
    List<Object[]> findByCodigosecLengthEquals(@Param("len") int len);

    /**
     * Native query for codigosec length less-than comparison (municipality-level).
     *
     * @param len upper bound length (exclusive)
     * @return list of result rows as Object[]
     */
    @Query(value = "SELECT codigosec, nome, bairro FROM alunos.alu_mec_tacom " +
                   "WHERE LENGTH(codigosec) < :len " +
                   "AND cod_titular IS NOT NULL " +
                   "AND ano_base_exclusao IS NULL " +
                   "ORDER BY codigosec ASC",
           nativeQuery = true)
    List<Object[]> findByCodigosecLengthLessThan(@Param("len") int len);

    /**
     * Convenience helper that reproduces the legacy RecadastramentoDAO branching:
     * - if login equals "9999" -> return rows where length(codigosec) = 7 (state-level)
     * - otherwise -> return rows where length(codigosec) < 7 (municipality-level)
     *
     * Returning List<Object[]> keeps the method simple for the migration step; callers are expected to
     * adapt each Object[] into the desired DTO (e.g., InstituicaoDTO) as done in RecadastramentoDAO.
     *
     * TODO: (REVIEW) Consider moving this logic into a service or DAO layer if richer transformation or
     * error handling is required. Also consider returning typed projections/entities in the future.
     *
     * @param login legacy login code used to decide length criteria
     * @return list of result rows as Object[]
     */
    default List<Object[]> findForLogin(String login) {
        if ("9999".equals(login)) {
            return findByCodigosecLengthEquals(7);
        } else {
            // Use 7 as the threshold to match legacy "length(codigosec) < 7"
            return findByCodigosecLengthLessThan(7);
        }
    }
}