package sn.jappo.jappo_backend.ressource.entity;

/**
 * Niveau de visibilité d'une ressource, sans explosion de tables.
 * Les identifiants cibles (cohorte, parcours, phase, missions) sont portés
 * par l'entité elle-même + la table de jointure mission_cohorte_ressources.
 */
public enum PorteeRessource {
    STRUCTURE,
    COHORTE,
    PARCOURS,
    PHASE,
    MISSION
}
