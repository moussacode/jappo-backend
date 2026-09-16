package sn.jappo.jappo_backend.ia.action;

/**
 * Cycle de vie d'une proposition d'action IA :
 *
 *   EN_ATTENTE  --confirmer()-->  CONFIRMEE  --exécution réussie-->  (reste CONFIRMEE)
 *               --rejeter()-->    REJETEE
 *               --expiration-->   EXPIREE
 *
 * Une action CONFIRMEE dont l'exécution échoue reste CONFIRMEE (la confirmation humaine
 * a bien eu lieu) mais l'erreur d'exécution est renvoyée à l'appelant — voir AiActionService.
 */
public enum AiActionStatus {
    EN_ATTENTE,
    CONFIRMEE,
    REJETEE,
    EXPIREE
}