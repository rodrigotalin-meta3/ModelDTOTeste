package br.com.meta3.java.scaffold.domain.services;

import br.com.meta3.java.scaffold.api.dtos.InstituicaoDTO;
import java.util.List;

/**
 * Abstraction for recadastramento-related operations.
 *
 * Purpose:
 * - Decouple controllers and higher layers from the concrete DAO/Repository implementation.
 * - Provide a stable contract that can be implemented by application/services classes which
 *   orchestrate infrastructure/repositories (e.g., RecadastramentoDAO).
 *
 * Note:
 * - Currently this interface uses InstituicaoDTO from the api layer as the return type to
 *   preserve a pragmatic migration path: existing infrastructure code (RecadastramentoDAO)
 *   already produces InstituicaoDTO instances. This creates an upward dependency from
 *   domain -> api which is acceptable for an incremental migration but should be revisited.
 *
 * TODO: (REVIEW) Consider introducing a domain-level Instituicao value object/entity in
 *       br.com.meta3.java.scaffold.domain.* and change this interface to return that type.
 *       That will better respect layering and remove the domain -> api dependency.
 */
public interface RecadastramentoService {

    /**
     * Retrieve the list of institutions (instituições) filtered for the given login/user.
     *
     * Legacy compatibility:
     * - Mirrors the legacy signature mostraInstituicoesEstadoMunicipio(String login).
     * - Implementations should return an empty list (not null) when no data is available or
     *   when underlying persistence access fails, to preserve legacy callers' expectations.
     *
     * @param login user login used to filter institutions (may be null)
     * @return list of InstituicaoDTO (never null; empty if no data)
     */
    List<InstituicaoDTO> mostraInstituicoesEstadoMunicipio(String login);
}