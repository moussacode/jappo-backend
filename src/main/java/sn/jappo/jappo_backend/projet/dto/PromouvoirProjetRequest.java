package sn.jappo.jappo_backend.projet.dto;

import java.util.UUID;

/**
 * Requête de promotion d'un projet vers une cohorte cible.
 * cohorteCibleId : cohorte vers laquelle promouvoir (obligatoire).
 * raison : raison de la promotion (obligatoire si forcer=true).
 * forcer : si true, force la promotion même avec des missions non validées (avertissement uniquement).
 */
public record PromouvoirProjetRequest(
    UUID cohorteCibleId,
    String raison,
    boolean forcer
) {}