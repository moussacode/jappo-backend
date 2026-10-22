package sn.jappo.jappo_backend.parcours.entity;

/**
 * Stade d'accompagnement porté par une phase.
 *
 * Les noms de phases restent libres pour chaque structure ; le stade leur donne une
 * signification commune exploitable par les rapports et l'assistant IA.
 * Le stade d'un projet se déduit de la phase de sa cohorte active : on y entre
 * par promotion vers une cohorte de phase supérieure.
 */
public enum StadeAccompagnement {
    PRE_INCUBATION,
    INCUBATION,
    ACCELERATION,
    POST_INCUBATION
}
