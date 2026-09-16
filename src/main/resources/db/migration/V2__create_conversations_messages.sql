-- ============================================================
-- V2 : Création des tables conversations et messages IA
-- ============================================================
-- Ces tables soutiennent l'Assistant IA coach de JAPPO.
-- La colonne "contexte_json" stocke le périmètre métier sélectionné
-- par le coach via l'interface (cohorteId, projetId, entrepreneurId).
-- "auteur" vaut 'COACH' ou 'ASSISTANT' (ex-valeur 'entrepreneur' supprimée).
-- ============================================================

CREATE TABLE conversations (
    id            CHAR(36)     NOT NULL PRIMARY KEY,
    structure_id  CHAR(36)     NOT NULL,
    coach_id      CHAR(36)     NOT NULL,
    -- Contexte sélectionné par le coach en JSON ex: {"cohorteId":"...", "projetId":null}
    -- NULL ou '{}' = aucun contexte => périmètre structure globale
    contexte_json TEXT,
    date_creation     DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    date_modification DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    INDEX idx_conv_structure  (structure_id),
    INDEX idx_conv_coach      (coach_id),
    INDEX idx_conv_struct_coach (structure_id, coach_id)
);

CREATE TABLE messages (
    id               CHAR(36)      NOT NULL PRIMARY KEY,
    conversation_id  CHAR(36)      NOT NULL,
    -- COACH ou ASSISTANT
    auteur           VARCHAR(20)   NOT NULL,
    contenu          LONGTEXT      NOT NULL,
    date_envoi       DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    -- Identifiant du modèle utilisé (ex: 'fake', 'gpt-4o', ...) — NULL pour les messages COACH
    model            VARCHAR(100),
    -- JSON array de références métier utilisées ex: [{"type":"COHORTE","id":"..."}]
    sources_json     TEXT,
    -- JSON array d'actions proposées ex: [] pour l'instant
    actions_json     TEXT,

    CONSTRAINT fk_msg_conversation
        FOREIGN KEY (conversation_id) REFERENCES conversations (id)
        ON DELETE CASCADE,

    INDEX idx_msg_conversation  (conversation_id),
    INDEX idx_msg_date          (conversation_id, date_envoi)
);
