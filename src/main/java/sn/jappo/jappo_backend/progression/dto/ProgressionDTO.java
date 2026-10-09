package sn.jappo.jappo_backend.progression.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProgressionDTO(
    UUID id,
    UUID projetId,
    UUID parcoursId,
    UUID phaseId,
    UUID etapeId,
    String statut,
    Integer pourcentageAvancement,
    LocalDateTime dateDebut,
    LocalDateTime dateFin,
    LocalDateTime dateDerniereMaj
) {}
