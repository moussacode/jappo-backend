package sn.jappo.jappo_backend.projet.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Résumé d'une participation à une cohorte (historique d'un projet).
 */
public record ParticipationCohorteResponse(
    UUID id,
    UUID cohorteId,
    String nomCohorte,
    UUID parcoursId,
    String nomParcours,
    UUID phaseId,
    String nomPhase,
    Integer phaseOrdre,
    LocalDateTime dateEntree,
    LocalDateTime dateSortie,
    String motifSortie,
    String raison,
    boolean active
) {}
