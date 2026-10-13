-- Remplace les colonnes ENUM de plan par une clé étrangère vers plans_abonnement.
--   abonnements.plan              -> abonnements.plan_id
--   historique_abonnements.plan   -> historique_abonnements.plan_id
--   transactions_paiement.plan_vise -> transactions_paiement.plan_vise_id
-- Les anciennes colonnes sont conservées (rendues nullables) et ne sont plus lues ni écrites.
-- Elles seront supprimées dans un lot ultérieur.
-- Script idempotent : chaque étape vérifie l'état courant avant d'agir.
-- Les codes de plan sont comparés en BINARY : ils sont en ASCII, et les tables
-- existantes n'ont pas forcément la même collation que plans_abonnement.

-- ════════════════════════════════════════════════════════════════
-- 1. abonnements.plan_id
-- ════════════════════════════════════════════════════════════════
SET @existe = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'abonnements' AND COLUMN_NAME = 'plan_id');
SET @sql = IF(@existe = 0, 'ALTER TABLE `abonnements` ADD COLUMN `plan_id` BINARY(16) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Correction des lignes « FREEMIUM + EXPIRE » : l'ancien job d'expiration écrasait le plan.
-- On retrouve le plan réellement souscrit via la dernière transaction réussie de la structure.
SET @ancienne_colonne = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'abonnements' AND COLUMN_NAME = 'plan');
SET @sql = IF(@ancienne_colonne = 1,
    'UPDATE `abonnements` a
     JOIN `plans_abonnement` p ON BINARY p.`code` = BINARY (
         SELECT t.`plan_vise` FROM `transactions_paiement` t
         WHERE t.`structure_id` = a.`structure_id` AND t.`statut` = ''SUCCES''
         ORDER BY t.`date_confirmation` DESC, t.`date_creation` DESC
         LIMIT 1)
     SET a.`plan_id` = p.`id`
     WHERE a.`plan_id` IS NULL AND a.`plan` = ''FREEMIUM'' AND a.`statut` = ''EXPIRE''',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Cas général : même code de plan.
SET @sql = IF(@ancienne_colonne = 1,
    'UPDATE `abonnements` a JOIN `plans_abonnement` p ON BINARY p.`code` = BINARY a.`plan`
     SET a.`plan_id` = p.`id` WHERE a.`plan_id` IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Filet de sécurité : toute ligne restante prend le plan par défaut.
UPDATE `abonnements`
SET `plan_id` = (SELECT `id` FROM `plans_abonnement` WHERE `est_defaut` = TRUE ORDER BY `ordre` LIMIT 1)
WHERE `plan_id` IS NULL;

ALTER TABLE `abonnements` MODIFY COLUMN `plan_id` BINARY(16) NOT NULL;

SET @existe = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'abonnements' AND CONSTRAINT_NAME = 'fk_abonnements_plan');
SET @sql = IF(@existe = 0,
    'ALTER TABLE `abonnements` ADD CONSTRAINT `fk_abonnements_plan` FOREIGN KEY (`plan_id`) REFERENCES `plans_abonnement` (`id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- L'ancienne colonne n'est plus écrite par l'application : elle doit accepter NULL.
SET @sql = IF(@ancienne_colonne = 1,
    'ALTER TABLE `abonnements` MODIFY COLUMN `plan` VARCHAR(50) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ════════════════════════════════════════════════════════════════
-- 2. historique_abonnements.plan_id
-- ════════════════════════════════════════════════════════════════
SET @existe = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'historique_abonnements' AND COLUMN_NAME = 'plan_id');
SET @sql = IF(@existe = 0, 'ALTER TABLE `historique_abonnements` ADD COLUMN `plan_id` BINARY(16) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ancienne_colonne = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'historique_abonnements' AND COLUMN_NAME = 'plan');
SET @sql = IF(@ancienne_colonne = 1,
    'UPDATE `historique_abonnements` h JOIN `plans_abonnement` p ON BINARY p.`code` = BINARY h.`plan`
     SET h.`plan_id` = p.`id` WHERE h.`plan_id` IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `historique_abonnements`
SET `plan_id` = (SELECT `id` FROM `plans_abonnement` WHERE `est_defaut` = TRUE ORDER BY `ordre` LIMIT 1)
WHERE `plan_id` IS NULL;

ALTER TABLE `historique_abonnements` MODIFY COLUMN `plan_id` BINARY(16) NOT NULL;

SET @existe = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'historique_abonnements' AND CONSTRAINT_NAME = 'fk_historique_abonnements_plan');
SET @sql = IF(@existe = 0,
    'ALTER TABLE `historique_abonnements` ADD CONSTRAINT `fk_historique_abonnements_plan` FOREIGN KEY (`plan_id`) REFERENCES `plans_abonnement` (`id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@ancienne_colonne = 1,
    'ALTER TABLE `historique_abonnements` MODIFY COLUMN `plan` VARCHAR(50) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- ════════════════════════════════════════════════════════════════
-- 3. transactions_paiement.plan_vise_id
-- ════════════════════════════════════════════════════════════════
SET @existe = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transactions_paiement' AND COLUMN_NAME = 'plan_vise_id');
SET @sql = IF(@existe = 0, 'ALTER TABLE `transactions_paiement` ADD COLUMN `plan_vise_id` BINARY(16) NULL', 'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @ancienne_colonne = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transactions_paiement' AND COLUMN_NAME = 'plan_vise');
SET @sql = IF(@ancienne_colonne = 1,
    'UPDATE `transactions_paiement` t JOIN `plans_abonnement` p ON BINARY p.`code` = BINARY t.`plan_vise`
     SET t.`plan_vise_id` = p.`id` WHERE t.`plan_vise_id` IS NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

UPDATE `transactions_paiement`
SET `plan_vise_id` = (SELECT `id` FROM `plans_abonnement` WHERE `est_defaut` = TRUE ORDER BY `ordre` LIMIT 1)
WHERE `plan_vise_id` IS NULL;

ALTER TABLE `transactions_paiement` MODIFY COLUMN `plan_vise_id` BINARY(16) NOT NULL;

SET @existe = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'transactions_paiement' AND CONSTRAINT_NAME = 'fk_transactions_paiement_plan');
SET @sql = IF(@existe = 0,
    'ALTER TABLE `transactions_paiement` ADD CONSTRAINT `fk_transactions_paiement_plan` FOREIGN KEY (`plan_vise_id`) REFERENCES `plans_abonnement` (`id`)',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SET @sql = IF(@ancienne_colonne = 1,
    'ALTER TABLE `transactions_paiement` MODIFY COLUMN `plan_vise` VARCHAR(50) NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
