package sn.jappo.jappo_backend.ia.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Body de PATCH /api/conversations/{id}/contexte
 * Tous les champs sont optionnels. null efface le champ correspondant.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record UpdateContexteRequest(
        UUID cohorteId,
        UUID projetId,
        UUID entrepreneurId
) {}
