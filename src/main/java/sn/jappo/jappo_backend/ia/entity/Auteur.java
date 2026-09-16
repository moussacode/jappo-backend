package sn.jappo.jappo_backend.ia.entity;

/**
 * Auteur d'un message dans une conversation IA.
 * COACH   : message écrit par le coach humain.
 * ASSISTANT : réponse générée par l'IA (FakeAiService puis futur LLM).
 */
public enum Auteur {
    COACH,
    ASSISTANT
}
