-- Ajouter les colonnes d'archivage pour les missions de cohorte
ALTER TABLE missions_cohorte 
ADD COLUMN archive BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN date_archivage TIMESTAMP NULL;

-- Ajouter les colonnes d'archivage pour les missions projet
ALTER TABLE missions_projet 
ADD COLUMN archive BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN date_archivage TIMESTAMP NULL;
