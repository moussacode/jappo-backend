-- ============================================================
-- V7 : Ajout du mode (ONLINE/PRESENTIEL) et des champs de localisation
-- ============================================================

-- Ajouter la colonne mode (ONLINE par défaut pour les réunions existantes)
ALTER TABLE meetings
ADD COLUMN mode VARCHAR(20) NOT NULL DEFAULT 'ONLINE';

-- Ajouter les colonnes de localisation pour les réunions présentielles
ALTER TABLE meetings
ADD COLUMN location VARCHAR(255);

ALTER TABLE meetings
ADD COLUMN address VARCHAR(500);
