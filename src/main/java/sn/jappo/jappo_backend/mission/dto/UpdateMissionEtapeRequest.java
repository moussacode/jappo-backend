package sn.jappo.jappo_backend.mission.dto;

import java.util.UUID;

public record UpdateMissionEtapeRequest(
        String titre,
        String description,
        Integer ordre,
        Boolean obligatoire,
        UUID modeleId,
        Boolean clearModele
) {}
