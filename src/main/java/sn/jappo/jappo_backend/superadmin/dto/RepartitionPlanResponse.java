package sn.jappo.jappo_backend.superadmin.dto;

import java.util.UUID;

/** Nombre de structures dont le plan effectif est ce plan. */
public record RepartitionPlanResponse(
        UUID planId,
        String code,
        String libelle,
        long nombreStructures
) {
}
