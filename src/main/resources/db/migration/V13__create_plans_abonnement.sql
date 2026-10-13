-- Plans d'abonnement configurables par le SUPER_ADMIN (remplacent l'enum PlanAbonnement).
-- Colonnes VARCHAR (pas d'ENUM MySQL) : un nouveau plan ne demande aucune migration.
-- Script idempotent : la table et les plans initiaux ne sont créés que s'ils n'existent pas.

CREATE TABLE IF NOT EXISTS `plans_abonnement` (
    `id`                     BINARY(16)    NOT NULL PRIMARY KEY,
    `code`                   VARCHAR(50)   NOT NULL,
    `libelle`                VARCHAR(100)  NOT NULL,
    `description`            VARCHAR(1000) NULL,
    `prix`                   BIGINT        NOT NULL DEFAULT 0,
    `devise`                 VARCHAR(3)    NOT NULL DEFAULT 'XOF',
    `duree_jours`            INT           NULL,      -- NULL = sans fin
    `max_cohortes`           INT           NULL,      -- NULL = illimité
    `ia_coach_active`        BOOLEAN       NOT NULL DEFAULT FALSE, -- coachs et admins de structure
    `ia_entrepreneur_active` BOOLEAN       NOT NULL DEFAULT FALSE,
    `ia_tokens_mensuels`     BIGINT        NULL,      -- pot commun de la structure, NULL = illimité
    `est_defaut`             BOOLEAN       NOT NULL DEFAULT FALSE,
    `actif`                  BOOLEAN       NOT NULL DEFAULT TRUE,
    `ordre`                  INT           NOT NULL DEFAULT 0,
    `date_creation`          DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    `date_modification`      DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT `uk_plans_abonnement_code` UNIQUE (`code`)
);

-- Plans initiaux, avec les limites appliquées par le code jusqu'ici.
-- Le quota de tokens du Freemium est une valeur de départ, modifiable par le SUPER_ADMIN.
INSERT INTO `plans_abonnement`
    (`id`, `code`, `libelle`, `description`, `prix`, `devise`, `duree_jours`, `max_cohortes`,
     `ia_coach_active`, `ia_entrepreneur_active`, `ia_tokens_mensuels`, `est_defaut`, `actif`, `ordre`)
SELECT UUID_TO_BIN(UUID()), 'FREEMIUM', 'Freemium',
       'Pour démarrer : une cohorte et l''assistant IA pour les coachs.',
       0, 'XOF', NULL, 1,
       TRUE, FALSE, 200000, TRUE, TRUE, 1
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `plans_abonnement` WHERE `code` = 'FREEMIUM');

INSERT INTO `plans_abonnement`
    (`id`, `code`, `libelle`, `description`, `prix`, `devise`, `duree_jours`, `max_cohortes`,
     `ia_coach_active`, `ia_entrepreneur_active`, `ia_tokens_mensuels`, `est_defaut`, `actif`, `ordre`)
SELECT UUID_TO_BIN(UUID()), 'PREMIUM', 'Premium',
       'Cohortes illimitées et assistant IA pour toute la structure, entrepreneurs compris.',
       90000, 'XOF', 365, NULL,
       TRUE, TRUE, NULL, FALSE, TRUE, 2
FROM DUAL
WHERE NOT EXISTS (SELECT 1 FROM `plans_abonnement` WHERE `code` = 'PREMIUM');
