package sn.jappo.jappo_backend.ressource.dto;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import sn.jappo.jappo_backend.ressource.entity.PorteeRessource;
import sn.jappo.jappo_backend.ressource.entity.TypeRessource;

public record CreateRessourceRequest(
        @NotBlank(message = "Le titre de la ressource est obligatoire")
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
