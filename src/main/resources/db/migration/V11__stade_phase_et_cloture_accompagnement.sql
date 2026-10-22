-- Lot 2 : stade d'accompagnement des phases et clôture de l'accompagnement d'un projet.
-- Script additif : aucune donnée existante n'est modifiée ni supprimée.

-- 1. Stade optionnel sur les phases (NULL pour les phases existantes).
SET @stade_existe = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'phases' AND COLUMN_NAME = 'stade'
);
SET @ajout_stade = IF(@stade_existe = 0,
    'ALTER TABLE `phases` ADD COLUMN `stade` ENUM(''PRE_INCUBATION'', ''INCUBATION'', ''ACCELERATION'', ''POST_INCUBATION'') NULL',
    'SELECT 1');
PREPARE stmt FROM @ajout_stade;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. La clôture écrit DIPLOME / ABANDONNE et le motif de sortie TERMINE.
--    Ces valeurs existent dans le code depuis l'origine mais n'ont jamais été écrites :
--    on garantit que les colonnes ENUM natives les acceptent (sur-ensemble des valeurs actuelles).
-- Vérification préalable conseillée :
-- SELECT COLUMN_NAME, COLUMN_TYPE FROM INFORMATION_SCHEMA.COLUMNS
-- WHERE TABLE_SCHEMA = DATABASE()
--   AND ((TABLE_NAME = 'projets' AND COLUMN_NAME = 'statut')
--     OR (TABLE_NAME = 'participations_cohorte' AND COLUMN_NAME = 'motif_sortie'));
ALTER TABLE `projets`
    MODIFY COLUMN `statut` ENUM('ACTIF', 'DIPLOME', 'ABANDONNE') NOT NULL;

ALTER TABLE `participations_cohorte`
    MODIFY COLUMN `motif_sortie` ENUM('PROMU', 'RETIRE', 'TERMINE') NULL;
