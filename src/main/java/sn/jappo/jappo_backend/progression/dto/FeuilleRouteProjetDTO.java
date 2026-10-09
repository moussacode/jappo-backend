package sn.jappo.jappo_backend.progression.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record FeuilleRouteProjetDTO(
        UUID projetId,
        UUID programmeId,
        String nomProgramme,
        UUID parcoursId,
        String nomParcours,
        UUID progressionId,
        String statutProgression,
        Integer pourcentageAvancement,
        List<PhaseDTO> phases
) {
    public record PhaseDTO(
            UUID id,
            String nom,
            int ordre,
            String statut,
            String motif,
            int pourcentageAvancement,
            List<EtapeDTO> etapes
    ) {}

    public record EtapeDTO(
            UUID id,
            String nom,
            String description,
            int ordre,
            boolean obligatoire,
            String statut,
            String motif,
            int pourcentageAvancement,
            List<MissionDTO> missions
    ) {}

    public record MissionDTO(
            UUID id,
            String origine,
            String titre,
            String description,
            boolean obligatoire,
            String statut,
            List<LivrableAttenduDTO> livrablesAttendus,
            List<LivrableDeposeDTO> livrablesDeposes
    ) {}

    public record LivrableAttenduDTO(
            UUID id,
            String titre,
            String description,
            boolean obligatoire,
            String type,
            Integer ordre
    ) {}

    public record LivrableDeposeDTO(
            UUID id,
            String nom,
            String statut,
            int numeroVersion,
            LocalDateTime dateDepot
    ) {}
}