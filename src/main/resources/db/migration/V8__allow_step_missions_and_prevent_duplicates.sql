-- Verify existing step-instance duplicates before deployment:
-- SELECT mission_etape_id, projet_id, structure_id, COUNT(*) AS occurrences
-- FROM missions_projet
-- WHERE mission_etape_id IS NOT NULL
-- GROUP BY mission_etape_id, projet_id, structure_id
-- HAVING COUNT(*) > 1;
-- The ALTER is atomic on MySQL and fails without deleting rows if duplicates exist.

SET @mission_cohorte_type = (
    SELECT CONCAT(
        COLUMN_TYPE,
        IF(
            CHARACTER_SET_NAME IS NOT NULL,
            CONCAT(' CHARACTER SET ', CHARACTER_SET_NAME, ' COLLATE ', COLLATION_NAME),
            ''
        )
    )
    FROM INFORMATION_SCHEMA.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'missions_projet'
      AND COLUMN_NAME = 'mission_cohorte_id'
);

SET @mission_step_migration = CONCAT(
    'ALTER TABLE `missions_projet` ',
    'MODIFY COLUMN `mission_cohorte_id` ', @mission_cohorte_type, ' NULL, ',
    'ADD CONSTRAINT `uk_mission_etape_projet_structure` ',
    'UNIQUE (`mission_etape_id`, `projet_id`, `structure_id`)'
);

PREPARE mission_step_statement FROM @mission_step_migration;
EXECUTE mission_step_statement;
DEALLOCATE PREPARE mission_step_statement;