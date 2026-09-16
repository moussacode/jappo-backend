package sn.jappo.jappo_backend.ia.action;

/**
 * Types d'actions que l'IA peut PROPOSER (jamais exécuter directement).
 *
 * Ajouter un type ici ne suffit pas à le rendre exécutable : il faut aussi
 * implémenter son exécuteur dans {@link AiActionExecutor}. C'est volontaire —
 * cf. cahier des charges : "ne développe pas tous les CRUD immédiatement,
 * prépare une architecture extensible". Un type présent ici sans exécuteur
 * associé restera bloqué à l'état EN_ATTENTE même après confirmation, avec
 * une erreur explicite plutôt qu'un comportement silencieux.
 *
 * Ordre d'implémentation prévu (section 12 du cahier des charges) :
 *   1. Cohorte  : CREATE / UPDATE / ARCHIVE
 *   2. Mission  : CREATE / UPDATE / ARCHIVE
 *   3. Projet   : UPDATE / ARCHIVE (la partie "humain via Angular" existe déjà,
 *                 voir ProjetController — il ne reste que le chemin "via IA" à construire)
 *   4. Réunion  : CREATE / UPDATE
 */
public enum AiActionType {
    CREATE_COHORTE,
    UPDATE_COHORTE,
    ARCHIVE_COHORTE,

    CREATE_MISSION,
    UPDATE_MISSION,
    ARCHIVE_MISSION,

    UPDATE_PROJET,
    ARCHIVE_PROJET,

    CREATE_REUNION,
    UPDATE_REUNION
}