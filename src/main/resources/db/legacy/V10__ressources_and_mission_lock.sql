-- ============================================================
-- V10 : Module Ressources + verrouillage mission cohorte
-- ============================================================

-- -------------------------------------------------------
-- 1. Table ressources
-- -------------------------------------------------------
CREATE TABLE ressources (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    structure_id UUID NOT NULL,
    titre VARCHAR(200) NOT NULL,
    description TEXT,
    type VARCHAR(30) NOT NULL DEFAULT 'LIEN',
    portee VARCHAR(30) NOT NULL DEFAULT 'STRUCTURE',
    url VARCHAR(2000),
    nom_fichier VARCHAR(255),
    taille BIGINT,
    mime_type VARCHAR(150),
    cohorte_id UUID,
    parcours_id UUID,
    phase_id UUID,
    archivee BOOLEAN NOT NULL DEFAULT FALSE,
    date_archivage TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ressource_structure FOREIGN KEY (structure_id) REFERENCES structures(id) ON DELETE CASCADE,
    CONSTRAINT fk_ressource_cohorte FOREIGN KEY (cohorte_id) REFERENCES cohortes(id) ON DELETE SET NULL,
    CONSTRAINT fk_ressource_parcours FOREIGN KEY (parcours_id) REFERENCES parcours(id) ON DELETE SET NULL,
    CONSTRAINT fk_ressource_phase FOREIGN KEY (phase_id) REFERENCES phases(id) ON DELETE SET NULL
);

CREATE INDEX idx_ressources_structure ON ressources(structure_id);
CREATE INDEX idx_ressources_portee ON ressources(portee);
CREATE INDEX idx_ressources_cohorte ON ressources(cohorte_id);
CREATE INDEX idx_ressources_parcours ON ressources(parcours_id);
CREATE INDEX idx_ressources_phase ON ressources(phase_id);
CREATE INDEX idx_ressources_archivee ON ressources(archivee);

-- -------------------------------------------------------
-- 2. Table de jointure mission_cohorte ↔ ressources
-- -------------------------------------------------------
CREATE TABLE mission_cohorte_ressources (
    mission_cohorte_id UUID NOT NULL,
    ressource_id UUID NOT NULL,
    PRIMARY KEY (mission_cohorte_id, ressource_id),
    CONSTRAINT fk_mcr_mission FOREIGN KEY (mission_cohorte_id) REFERENCES missions_cohorte(id) ON DELETE CASCADE,
    CONSTRAINT fk_mcr_ressource FOREIGN KEY (ressource_id) REFERENCES ressources(id) ON DELETE CASCADE
);

CREATE INDEX idx_mcr_mission ON mission_cohorte_ressources(mission_cohorte_id);
CREATE INDEX idx_mcr_ressource ON mission_cohorte_ressources(ressource_id);

-- -------------------------------------------------------
-- 3. Verrouillage de MissionCohorte après première soumission
-- -------------------------------------------------------
ALTER TABLE missions_cohorte
ADD COLUMN verrouillee BOOLEAN NOT NULL DEFAULT FALSE,
ADD COLUMN date_verrouillage TIMESTAMP;
