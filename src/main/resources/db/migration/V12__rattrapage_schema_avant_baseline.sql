-- Rattrapage du schéma avant l'activation de Flyway (baseline en version 11).
--
-- Jusqu'ici Flyway était désactivé et le schéma était créé par Hibernate (ddl-auto=update).
-- Les colonnes et tables de V1 à V11 existent, mais trois objets n'ont jamais été créés :
--   - V1 : contrainte uk_mission_projet_unique
--   - V4 : index idx_conv_derniere_activite et idx_conv_archivee
--   - V8 : contrainte uk_mission_etape_projet_structure
-- Ce script les ajoute uniquement s'ils manquent. Il ne modifie aucune donnée.
--
-- Vérification préalable (les ALTER échouent sans rien supprimer s'il existe des doublons) :
-- SELECT mission_cohorte_id, projet_id, structure_id, COUNT(*) FROM missions_projet
-- WHERE mission_cohorte_id IS NOT NULL GROUP BY 1, 2, 3 HAVING COUNT(*) > 1;
-- SELECT mission_etape_id, projet_id, structure_id, COUNT(*) FROM missions_projet
-- WHERE mission_etape_id IS NOT NULL GROUP BY 1, 2, 3 HAVING COUNT(*) > 1;

-- 1. V1 : unicité (mission_cohorte_id, projet_id, structure_id)
SET @existe = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'missions_projet'
      AND CONSTRAINT_NAME = 'uk_mission_projet_unique'
);
SET @sql = IF(@existe = 0,
    'ALTER TABLE `missions_projet` ADD CONSTRAINT `uk_mission_projet_unique` UNIQUE (`mission_cohorte_id`, `projet_id`, `structure_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. V4 : index des conversations
SET @existe = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversations'
      AND INDEX_NAME = 'idx_conv_derniere_activite'
);
SET @sql = IF(@existe = 0,
    'CREATE INDEX `idx_conv_derniere_activite` ON `conversations` (`date_derniere_activite` DESC)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @existe = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversations'
      AND INDEX_NAME = 'idx_conv_archivee'
);
SET @sql = IF(@existe = 0,
    'CREATE INDEX `idx_conv_archivee` ON `conversations` (`archivee`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. V8 : unicité (mission_etape_id, projet_id, structure_id)
--    (mission_cohorte_id est déjà nullable dans le schéma généré par Hibernate)
SET @existe = (
    SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'missions_projet'
      AND CONSTRAINT_NAME = 'uk_mission_etape_projet_structure'
);
SET @sql = IF(@existe = 0,
    'ALTER TABLE `missions_projet` ADD CONSTRAINT `uk_mission_etape_projet_structure` UNIQUE (`mission_etape_id`, `projet_id`, `structure_id`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
