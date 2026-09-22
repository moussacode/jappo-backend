-- ============================================================
-- V8 : Création du système de parcours configurable
-- ============================================================

-- Table parcours
CREATE TABLE parcours (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    actif BOOLEAN NOT NULL DEFAULT true,
    structure_id UUID NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP,
    CONSTRAINT fk_parcours_structure FOREIGN KEY (structure_id) REFERENCES structures(id) ON DELETE CASCADE
);

-- Index pour parcours
CREATE INDEX idx_parcours_structure ON parcours(structure_id);
CREATE INDEX idx_parcours_actif ON parcours(actif);

-- Table phases
CREATE TABLE phases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    ordre INTEGER NOT NULL,
    couleur VARCHAR(50),
    icone VARCHAR(50),
    duree_estimee_jours INTEGER,
    actif BOOLEAN NOT NULL DEFAULT true,
    parcours_id UUID NOT NULL,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP,
    CONSTRAINT fk_phase_parcours FOREIGN KEY (parcours_id) REFERENCES parcours(id) ON DELETE CASCADE
);

-- Index pour phases
CREATE INDEX idx_phases_parcours ON phases(parcours_id);
CREATE INDEX idx_phases_ordre ON phases(parcours_id, ordre);
CREATE INDEX idx_phases_actif ON phases(actif);

-- Table etapes
CREATE TABLE etapes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    nom VARCHAR(150) NOT NULL,
    description TEXT,
    ordre INTEGER NOT NULL,
    objectifs TEXT,
    duree_estimee_jours INTEGER,
    criteres_validation TEXT,
    phase_id UUID,
    actif BOOLEAN NOT NULL DEFAULT true,
    date_creation TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP,
    CONSTRAINT fk_etape_phase FOREIGN KEY (phase_id) REFERENCES phases(id) ON DELETE SET NULL
);

-- Index pour etapes
CREATE INDEX idx_etapes_phase ON etapes(phase_id);
CREATE INDEX idx_etapes_ordre ON etapes(phase_id, ordre);
CREATE INDEX idx_etapes_actif ON etapes(actif);

-- Table transitions_phase (historique des transitions de phase des projets)
CREATE TABLE transitions_phase (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projet_id UUID NOT NULL,
    phase_source_id VARCHAR(50),
    phase_cible_id VARCHAR(50),
    date_transition TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type_transition VARCHAR(20) NOT NULL DEFAULT 'MANUEL',
    raison TEXT,
    effectue_par VARCHAR(50),
    CONSTRAINT fk_transition_projet FOREIGN KEY (projet_id) REFERENCES projets(id) ON DELETE CASCADE
);

-- Index pour transitions_phase
CREATE INDEX idx_transitions_projet ON transitions_phase(projet_id);
CREATE INDEX idx_transitions_date ON transitions_phase(date_transition);

-- Ajouter parcours_id aux cohortes (nullable pour compatibilité)
ALTER TABLE cohortes
ADD COLUMN parcours_id UUID;

ALTER TABLE cohortes
ADD CONSTRAINT fk_cohorte_parcours FOREIGN KEY (parcours_id) REFERENCES parcours(id) ON DELETE SET NULL;

CREATE INDEX idx_cohortes_parcours ON cohortes(parcours_id);

-- Ajouter phase_actuelle_id aux projets (nullable pour compatibilité)
ALTER TABLE projets
ADD COLUMN phase_actuelle_id UUID;

CREATE INDEX idx_projets_phase_actuelle ON projets(phase_actuelle_id);

-- Ajouter etape_id aux missions_modeles (nullable pour compatibilité)
ALTER TABLE missions_modeles
ADD COLUMN etape_id UUID;

ALTER TABLE missions_modeles
ADD CONSTRAINT fk_mission_modele_etape FOREIGN KEY (etape_id) REFERENCES etapes(id) ON DELETE SET NULL;

CREATE INDEX idx_missions_modeles_etape ON missions_modeles(etape_id);

-- Ajouter etape_id aux missions_cohorte (nullable pour compatibilité)
ALTER TABLE missions_cohorte
ADD COLUMN etape_id UUID;

ALTER TABLE missions_cohorte
ADD CONSTRAINT fk_mission_cohorte_etape FOREIGN KEY (etape_id) REFERENCES etapes(id) ON DELETE SET NULL;

CREATE INDEX idx_missions_cohorte_etape ON missions_cohorte(etape_id);
