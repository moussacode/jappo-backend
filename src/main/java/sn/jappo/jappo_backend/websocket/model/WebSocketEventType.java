package sn.jappo.jappo_backend.websocket.model;

/**
 * Types d'événements WebSocket supportés par JAPPO.
 * 
 * Ces événements permettent aux interfaces de se mettre à jour en temps réel
 * sans nécessiter de refresh manuel (F5).
 */
public enum WebSocketEventType {
    
    // Livrables
    LIVRABLE_SOUMIS,
    LIVRABLE_VALIDE,
    LIVRABLE_REJETE,
    
    // Missions
    MISSION_CREEE,
    MISSION_MODIFIEE,
    MISSION_ARCHIVEE,
    MISSION_RESTOREE,
    
    // Projets
    PROJET_CREE,
    PROJET_MODIFIE,
    PROJET_ARCHIVE,
    PROJET_RESTORE,
    
    // Cohortes
    COHORTE_CREEE,
    COHORTE_MODIFIEE,
    COHORTE_ARCHIVEE,
    COHORTE_RESTOREE,
    
    // Entrepreneurs
    ENTREPRENEUR_INVITE,
    ENTREPRENEUR_ACCEPTE,
    
    // Réunions
    REUNION_CREEE,
    REUNION_MODIFIEE,
    
    // Notifications
    NOTIFICATION
}
