-- Ajouter une contrainte d'unicité pour éviter les doublons de MissionProjet
-- Une MissionProjet doit être unique pour le couple (mission_cohorte_id, projet_id, structure_id)

-- D'abord, vérifier et supprimer les doublons potentiels existants
-- Cette requête identifie les doublons mais ne les supprime pas automatiquement
-- L'administrateur doit vérifier manuellement s'il y a des doublons avant d'appliquer la contrainte

-- Pour identifier les doublons (à exécuter manuellement si nécessaire):
-- SELECT mission_cohorte_id, projet_id, structure_id, COUNT(*) as count
-- FROM missions_projet
-- GROUP BY mission_cohorte_id, projet_id, structure_id
-- HAVING COUNT(*) > 1;

-- Si aucun doublon n'existe, ajouter la contrainte d'unicité
ALTER TABLE missions_projet 
ADD CONSTRAINT uk_mission_projet_unique 
UNIQUE (mission_cohorte_id, projet_id, structure_id);
