package sn.jappo.jappo_backend.progression.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HistoriqueProgressionDTO(
    UUID id,
    UUID progressionId,
    UUID etapeEntreeId,
    UUID etapeSortieId,
    UUID phaseEntreeId,
    UUID phaseSortieId,
    LocalDateTime dateEntree,
    LocalDateTime dateSortie,
    String raison,
    String typeChangement,
    UUID effectueParId
) {}
