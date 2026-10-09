package sn.jappo.jappo_backend.programme.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public record ProgrammeDTO(
    UUID id,
    String nom,
    String description,
    boolean archive,
    UUID structureId,
    LocalDateTime dateCreation,
    LocalDateTime dateModification,
    List<UUID> parcoursIds
) {
    public ProgrammeDTO {
        if (parcoursIds == null) {
            parcoursIds = List.of();
        }
    }
}
