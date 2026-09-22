package sn.jappo.jappo_backend.ressource.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import sn.jappo.jappo_backend.ressource.entity.PorteeRessource;
import sn.jappo.jappo_backend.ressource.entity.TypeRessource;

public record RessourceResponse(
        UUID id,
        UUID structureId,
        String titre,
        String description,
        TypeRessource type,
        PorteeRessource portee,
        String url,
        String nomFichier,
        Long taille,
        String mimeType,
        UUID cohorteId,
        String nomCohorte,
        UUID parcoursId,
        String nomParcours,
        UUID phaseId,
        String nomPhase,
        List<UUID> missionCohorteIds,
        boolean archivee,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
