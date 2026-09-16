package sn.jappo.jappo_backend.ia.service;

import sn.jappo.jappo_backend.ia.context.AiContext;
import sn.jappo.jappo_backend.ia.entity.Message;

import java.util.List;
import java.util.Map;

/**
 * Abstraction du service IA.
 *
 * Permet de remplacer FakeAiService par un vrai orchestrateur
 * (FastAPI + LLM + RAG) sans modifier le pipeline Spring.
 *
 * Le futur LLM ne doit JAMAIS accéder directement à MySQL :
 * il reçoit uniquement l'AiContext (données déjà filtrées et sérialisées).
 */
public interface AiService {

    /**
     * Réponse IA complète.
     *
     * @param content    texte à afficher au coach — TOUJOURS un message fonctionnel,
     *                   jamais une trace technique (voir {@code success=false}).
     * @param model      identifiant du modèle réellement utilisé ("unavailable" si en échec).
     * @param success    false si la génération a échoué (timeout, panne, erreur amont) ;
     *                   dans ce cas {@code content} contient un message de repli destiné au coach.
     * @param sources    sources citées par l'IA pour construire sa réponse (peut être vide).
     * @param actions    propositions d'action structurées (ACTION_PROPOSAL) — jamais exécutées
     *                   directement, seulement transmises pour confirmation humaine (voir AiActionService).
     */
    record AiResponse(
            String content,
            String model,
            boolean success,
            List<Map<String, Object>> sources,
            List<Map<String, Object>> actions
    ) {
        /** Raccourci pour une réponse réussie sans sources ni actions (cas FakeAiService). */
        public static AiResponse ok(String content, String model) {
            return new AiResponse(content, model, true, List.of(), List.of());
        }

        /** Raccourci pour un échec — content doit déjà être un message adapté au coach. */
        public static AiResponse failure(String content) {
            return new AiResponse(content, "unavailable", false, List.of(), List.of());
        }
    }

    /**
     * Génère une réponse textuelle à partir de la question, du contexte métier
     * et de l'historique de la conversation.
     *
     * @param question  texte de la question posée par le coach
     * @param context   données métier réelles récupérées par AiContextBuilder
     * @param history   historique des messages précédents de la conversation
     * @return          réponse IA avec contenu et modèle utilisé
     */
    AiResponse generateResponse(String question, AiContext context, List<Message> history);
}