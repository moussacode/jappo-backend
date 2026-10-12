package sn.jappo.jappo_backend.ia.context;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Représente le périmètre métier sélectionné dans l'interface.
 *
 * Ce DTO est sérialisé/désérialisé en JSON dans Conversation.contexteJson.
 * Il ne contient que des identifiants choisis par l'utilisateur.
 *
 * IMPORTANT : ne pas confondre avec AiContext, qui représente
 * les données réelles récupérées depuis la BDD à partir de ces IDs.
 *
 * Tous les champs sont optionnels :
 *   {}                             => structure globale (Cas A)
 *   {"cohorteId":"..."}            => Cas B — cohorte sélectionnée
 *   {"projetId":"..."}             => Cas C — projet sélectionné
 *   {"cohorteId":"...","projetId":"..."} => Cas D — cohorte + projet
 *   {"projetId":"...","missionId":"..."} => projet et mission d'instance sélectionnés
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationContexte {

    private UUID cohorteId;
    private UUID projetId;
    private UUID entrepreneurId;
    private UUID missionId;

    // ── Constructeurs ──

    public ConversationContexte() {}

    public ConversationContexte(UUID cohorteId, UUID projetId, UUID entrepreneurId) {
        this(cohorteId, projetId, entrepreneurId, null);
    }

    public ConversationContexte(UUID cohorteId, UUID projetId, UUID entrepreneurId, UUID missionId) {
        this.cohorteId = cohorteId;
        this.projetId = projetId;
        this.entrepreneurId = entrepreneurId;
        this.missionId = missionId;
    }

    // ── Helpers ──

    /** Retourne vrai si aucun périmètre spécifique n'a été sélectionné. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isEmpty() {
        return cohorteId == null && projetId == null && entrepreneurId == null && missionId == null;
    }

    // ── Getters / Setters ──

    public UUID getCohorteId() { return cohorteId; }
    public void setCohorteId(UUID cohorteId) { this.cohorteId = cohorteId; }

    public UUID getProjetId() { return projetId; }
    public void setProjetId(UUID projetId) { this.projetId = projetId; }

    public UUID getEntrepreneurId() { return entrepreneurId; }
    public void setEntrepreneurId(UUID entrepreneurId) { this.entrepreneurId = entrepreneurId; }

    public UUID getMissionId() { return missionId; }
    public void setMissionId(UUID missionId) { this.missionId = missionId; }
}
