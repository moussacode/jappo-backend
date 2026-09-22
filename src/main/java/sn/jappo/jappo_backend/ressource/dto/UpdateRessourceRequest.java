package sn.jappo.jappo_backend.ressource.dto;

import java.util.List;
import java.util.UUID;

import sn.jappo.jappo_backend.ressource.entity.PorteeRessource;
import sn.jappo.jappo_backend.ressource.entity.TypeRessource;

public record UpdateRessourceRequest(
        String titre,
        String description,
        TypeRessource type,
        String url,
        PorteeRessource portee,
        UUID cohorteId,
        UUID parcoursId,
        UUID phaseId,
        List<UUID> missionCohorteIds
) {}
