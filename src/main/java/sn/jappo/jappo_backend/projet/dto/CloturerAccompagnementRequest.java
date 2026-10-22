package sn.jappo.jappo_backend.projet.dto;

import sn.jappo.jappo_backend.projet.entity.StatutProjet;

/**
 * Clôture de l'accompagnement d'un projet.
 * issue  : DIPLOME (fin du parcours) ou ABANDONNE.
 * motif  : obligatoire pour un abandon et pour un diplôme forcé.
 * forcer : diplômer un projet qui n'est pas dans la dernière phase du parcours
 *          ou dont des missions obligatoires ne sont pas validées.
 */
public record CloturerAccompagnementRequest(
        StatutProjet issue,
        String motif,
        boolean forcer
) {}
