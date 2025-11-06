filetype
package br.com.meta3.java.scaffold.api.dtos;

import br.com.meta3.java.scaffold.domain.entities.Instituicao;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Objects;

/**
 * DTO representing an institution (Instituição) returned to the frontend.
 *
 * Fields:
 * - id: unique identifier of the institution (nullable if legacy data has no id)
 * - nome: institution name
 * - estado: state
 * - municipio: municipality
 * - cnpj: legal identifier for the institution (may be null)
 *
 * This DTO is used to migrate results from legacy RecadastramentoDAO.mostraInstituicoesEstadoMunicipio(...)
 * into the new Spring Boot API responses.
 */
public class InstituicaoDTO {

    private Long id;
    private String nome;
    private String estado;
    private String municipio;

    /**
     * New typed field mapped from domain entity Instituicao.
     * Kept nullable to preserve backward compatibility with legacy data sources.
     */
    private String cnpj;

    public InstituicaoDTO() {
    }

    public InstituicaoDTO(Long id, String nome, String estado, String municipio) {
        this.id = id;
        this.nome = nome;
        this.estado = estado;
        this.municipio = municipio;
    }

    /**
     * Overloaded constructor including cnpj for convenience when creating DTOs from typed entities.
     */
    public InstituicaoDTO(Long id, String nome, String estado, String municipio, String cnpj) {
        this.id = id;
        this.nome = nome;
        this.estado = estado;
        this.municipio = municipio;
        this.cnpj = cnpj;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public String getMunicipio() {
        return municipio;
    }

    public void setMunicipio(String municipio) {
        this.municipio = municipio;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    /**
     * Factory method to create InstituicaoDTO from a legacy DAO result map.
     *
     * Many legacy DAOs (including the original RecadastramentoDAO) may return lists of maps or
     * object arrays. To avoid coupling the DTO to a specific legacy type, we provide a mapping
     * from a Map<String, Object>. Consumers can adapt legacy results into a Map and call this method.
     *
     * Expected map keys (best-effort):
     * - "id" or "codigo" -> Long or Number
     * - "nome" or "descricao" -> String
     * - "estado" or "uf" -> String
     * - "municipio" or "cidade" -> String
     * - "cnpj" -> String (optional)
     *
     * TODO: (REVIEW) If a domain entity for Instituicao is introduced, prefer the typed fromEntity(...) method.
     */
    public static InstituicaoDTO fromMap(Map<String, Object> map) {
        if (Objects.isNull(map)) {
            return null;
        }

        Long id = null;
        Object idVal = map.getOrDefault("id", map.get("codigo"));
        if (idVal instanceof Number) {
            id = ((Number) idVal).longValue();
        } else if (idVal instanceof String) {
            try {
                id = Long.valueOf((String) idVal);
            } catch (NumberFormatException ex) {
                // TODO: (REVIEW) Legacy data might contain non-numeric ids; keep null in that case.
            }
        }

        // Attempt several common legacy keys for each field to maximize compatibility with legacy DAO outputs
        String nome = toStringOrNull(map.getOrDefault("nome", map.get("descricao")));
        String estado = toStringOrNull(map.getOrDefault("estado", map.get("uf")));
        String municipio = toStringOrNull(map.getOrDefault("municipio", map.get("cidade")));
        String cnpj = toStringOrNull(map.get("cnpj"));

        return new InstituicaoDTO(id, nome, estado, municipio, cnpj);
    }

    private static String toStringOrNull(Object o) {
        if (o == null) return null;
        return o.toString();
    }

    /**
     * Create InstituicaoDTO from a domain entity instance.
     *
     * This typed factory is the preferred migration path when a stable domain entity is available.
     * It maps known properties directly and is type-safe (no reflection).
     *
     * Mapping:
     * - id -> entity.getId()
     * - nome -> entity.getNome()
     * - estado -> entity.getEstado()
     * - municipio -> entity.getMunicipio()
     * - cnpj -> entity.getCnpj()  (nullable)
     *
     * NOTE:
     * - This method requires the Instituicao entity to expose the getters above. If the entity's API
     *   changes during migration, update this method accordingly.
     *
     * TODO: (REVIEW) When Instituicao entity stabilizes, prefer this method in callers and remove the reflective fallback.
     */
    public static InstituicaoDTO fromEntity(Instituicao entity) {
        if (Objects.isNull(entity)) {
            return null;
        }

        Long id = null;
        try {
            id = entity.getId();
        } catch (Exception ex) {
            // Defensive: if entity API differs, keep id null and allow later callers to use reflective factory as fallback.
        }

        String nome = null;
        try {
            nome = entity.getNome();
        } catch (Exception ex) {
            // ignore - preserve backward compatibility
        }

        String estado = null;
        try {
            estado = entity.getEstado();
        } catch (Exception ex) {
            // ignore
        }

        String municipio = null;
        try {
            municipio = entity.getMunicipio();
        } catch (Exception ex) {
            // ignore
        }

        String cnpj = null;
        try {
            cnpj = entity.getCnpj();
        } catch (Exception ex) {
            // If entity does not expose cnpj, leave as null. This keeps migration resilient.
        }

        return new InstituicaoDTO(id, nome, estado, municipio, cnpj);
    }

    /**
     * Create InstituicaoDTO from a domain entity instance using reflection.
     *
     * This factory tries to be resilient to incremental migration: the domain entity class name and
     * getter names may vary during the migration. To avoid tight coupling and to preserve legacy
     * "nome" mapping semantics, we attempt several common getter names via reflection.
     *
     * Mapping strategy (best-effort):
     * - id: try getId(), getCodigo(), getCodigoInstituicao(), getCodigoInst()
     * - nome: try getNome(), getDescricao(), getRazaoSocial(), getNomeFantasia()
     * - estado: try getEstado(), getUf()
     * - municipio: try getMunicipio(), getCidade(), getBairro()
     * - cnpj: try getCnpj(), getCnpjFormatted(), getDocumento()
     *
     * If a method is not present or a value cannot be parsed, the corresponding DTO field will be null.
     *
     * NOTE:
     * - Using reflection here is a pragmatic migration aid to avoid compile-time coupling until a stable
     *   Instituicao entity is finalized. Once the entity stabilizes, replace this method with a direct
     *   typed mapping (entity.getX()) and remove reflective logic.
     *
     * TODO: (REVIEW) Replace reflection with a direct mapping when br.com.meta3.java.scaffold.domain.entities.Instituicao
     *       has a known stable API.
     *
     * @param entity domain entity instance (may be null)
     * @return mapped InstituicaoDTO or null if entity is null
     */
    public static InstituicaoDTO fromEntity(Object entity) {
        if (Objects.isNull(entity)) {
            return null;
        }

        // id extraction
        Long id = null;
        Object idVal = invokeFirstAvailableGetter(entity,
                "getId", "getCodigo", "getCodigoInstituicao", "getCodigoInst");
        if (idVal instanceof Number) {
            id = ((Number) idVal).longValue();
        } else if (idVal instanceof String) {
            try {
                id = Long.valueOf(((String) idVal).trim());
            } catch (NumberFormatException ex) {
                // leave id as null
            }
        }

        // nome extraction - preserve legacy "nome" mapping priority
        Object nomeVal = invokeFirstAvailableGetter(entity,
                "getNome", "getDescricao", "getRazaoSocial", "getNomeFantasia");
        String nome = nomeVal != null ? nomeVal.toString() : null;

        // estado extraction
        Object estadoVal = invokeFirstAvailableGetter(entity,
                "getEstado", "getUf");
        String estado = estadoVal != null ? estadoVal.toString() : null;

        // municipio extraction
        Object municipioVal = invokeFirstAvailableGetter(entity,
                "getMunicipio", "getCidade", "getBairro");
        String municipio = municipioVal != null ? municipioVal.toString() : null;

        // cnpj extraction - additional try for common getter names
        Object cnpjVal = invokeFirstAvailableGetter(entity,
                "getCnpj", "getCnpjFormatted", "getDocumento");
        String cnpj = cnpjVal != null ? cnpjVal.toString() : null;

        return new InstituicaoDTO(id, nome, estado, municipio, cnpj);
    }

    /**
     * Helper that uses reflection to try multiple getter names and returns the first non-null result.
     *
     * We deliberately swallow reflection exceptions and return null on failures to keep the mapping robust
     * during incremental migration. All reflection usage is contained here to simplify future removal.
     */
    private static Object invokeFirstAvailableGetter(Object target, String... methodNames) {
        if (target == null || methodNames == null) return null;
        for (String methodName : methodNames) {
            try {
                Method m = target.getClass().getMethod(methodName);
                if (m != null) {
                    Object val = m.invoke(target);
                    if (val != null) {
                        return val;
                    }
                }
            } catch (NoSuchMethodException nsme) {
                // try next method name
            } catch (Exception ex) {
                // Any reflective error should not break the mapping; log decision as TODO for reviewers.
                // TODO: (REVIEW) Consider logging reflective exceptions via a logger when entity mapping issues are investigated.
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return "InstituicaoDTO{" +
                "id=" + id +
                ", nome='" + nome + '\'' +
                ", estado='" + estado + '\'' +
                ", municipio='" + municipio + '\'' +
                ", cnpj='" + cnpj + '\'' +
                '}';
    }
}