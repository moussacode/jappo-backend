package sn.jappo.jappo_backend.ia.context;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.UUID;

/**
 * Représente le périmètre métier sélectionné par le coach dans l'interface.
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
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ConversationContexte {

    private UUID cohorteId;
    private UUID projetId;
    private UUID entrepreneurId;

    // ── Constructeurs ──

    public ConversationContexte() {}

    public ConversationContexte(UUID cohorteId, UUID projetId, UUID entrepreneurId) {
        this.cohorteId = cohorteId;
        this.projetId = projetId;
        this.entrepreneurId = entrepreneurId;
    }

    // ── Helpers ──

    /** Retourne vrai si aucun périmètre spécifique n'a été sélectionné. */
    @com.fasterxml.jackson.annotation.JsonIgnore
    public boolean isEmpty() {
        return cohorteId == null && projetId == null && entrepreneurId == null;
    }

    // ── Getters / Setters ──

    public UUID getCohorteId() { return cohorteId; }
    public void setCohorteId(UUID cohorteId) { this.cohorteId = cohorteId; }

    public UUID getProjetId() { return projetId; }
    public void setProjetId(UUID projetId) { this.projetId = projetId; }

    public UUID getEntrepreneurId() { return entrepreneurId; }
    public void setEntrepreneurId(UUID entrepreneurId) { this.entrepreneurId = entrepreneurId; }
}
