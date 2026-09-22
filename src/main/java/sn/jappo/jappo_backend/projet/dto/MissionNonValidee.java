package sn.jappo.jappo_backend.projet.dto;

/**
 * Mission de la cohorte actuelle qui n'est pas encore validée (corps d'erreur 409 promotion).
 */
public record MissionNonValidee(
        String titre,
        String statut
) {}
