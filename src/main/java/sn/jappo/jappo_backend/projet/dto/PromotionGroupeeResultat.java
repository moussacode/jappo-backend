package sn.jappo.jappo_backend.projet.dto;

import java.util.List;
import java.util.UUID;

/**
 * Résultat d'une promotion groupée.
 * Contient un résultat par projet traité.
 */
public record PromotionGroupeeResultat(
    UUID projetId,
    String nomProjet,
    boolean succes,
    String message,
    List<String> missionsNonValidees
) {}
