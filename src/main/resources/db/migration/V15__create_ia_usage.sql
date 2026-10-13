-- Consommation réelle de tokens IA, une ligne par réponse générée avec succès.
-- La consommation du mois d'une structure est la somme sur le mois calendaire courant.
-- Script idempotent.

CREATE TABLE IF NOT EXISTS `ia_usage` (
    `id`              BINARY(16)   NOT NULL PRIMARY KEY,
    `structure_id`    BINARY(16)   NOT NULL,
    `user_id`         BINARY(16)   NOT NULL,
    `role`            VARCHAR(30)  NOT NULL,      -- rôle dans la structure au moment de l'appel
    `tokens_entree`   INT          NOT NULL DEFAULT 0,
    `tokens_sortie`   INT          NOT NULL DEFAULT 0,
    `modele`          VARCHAR(150) NULL,          -- modèle réellement utilisé, renvoyé par OpenRouter
    `created_at`      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT `fk_ia_usage_structure`
        FOREIGN KEY (`structure_id`) REFERENCES `structures` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ia_usage_user`
        FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE,

    INDEX `idx_ia_usage_structure_date` (`structure_id`, `created_at`),
    INDEX `idx_ia_usage_user_date` (`user_id`, `created_at`)
);

-- conversation_id reprend exactement le type de conversations.id (CHAR(36) dans V2,
-- VARCHAR(36) dans les bases créées par Hibernate), jeu de caractères et collation compris.
SET @type_conversation = (
    SELECT CONCAT(
        COLUMN_TYPE,
        IF(CHARACTER_SET_NAME IS NOT NULL,
           CONCAT(' CHARACTER SET ', CHARACTER_SET_NAME, ' COLLATE ', COLLATION_NAME),
           '')
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'conversations' AND COLUMN_NAME = 'id'
);
SET @existe = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ia_usage' AND COLUMN_NAME = 'conversation_id');
SET @sql = IF(@existe = 0,
    CONCAT('ALTER TABLE `ia_usage` ADD COLUMN `conversation_id` ', @type_conversation, ' NULL AFTER `role`'),
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- Supprimer une conversation ne doit pas « rendre » les tokens déjà consommés.
SET @existe = (SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'ia_usage' AND CONSTRAINT_NAME = 'fk_ia_usage_conversation');
SET @sql = IF(@existe = 0,
    'ALTER TABLE `ia_usage` ADD CONSTRAINT `fk_ia_usage_conversation` FOREIGN KEY (`conversation_id`) REFERENCES `conversations` (`id`) ON DELETE SET NULL',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;
