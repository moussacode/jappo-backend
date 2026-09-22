-- ============================================================
-- V4 : Ajout des champs de gestion des conversations
-- ============================================================
-- Ajoute la possibilité de renommer, archiver et suivre l'activité
-- des conversations pour une expérience type ChatGPT.
-- ============================================================

ALTER TABLE conversations
    ADD COLUMN titre VARCHAR(255),
    ADD COLUMN archivee BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN date_derniere_activite DATETIME(6);

-- Index pour optimiser le tri par activité récente
CREATE INDEX idx_conv_derniere_activite ON conversations(date_derniere_activite DESC);

-- Index pour filtrer les conversations archivées
CREATE INDEX idx_conv_archivee ON conversations(archivee);
