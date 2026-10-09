package sn.jappo.jappo_backend.parcours.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record EtapeDTO(
    UUID id,
    String nom,
    String description,
    Integer ordre,
    boolean obligatoire,
    boolean archive,
    UUID phaseId,
    UUID structureId,
    LocalDateTime dateCreation,
    LocalDateTime dateModification,
    List<UUID> missionIds
) {
    public EtapeDTO {
        if (missionIds == null) {
            missionIds = List.of();
        }
    }
}
