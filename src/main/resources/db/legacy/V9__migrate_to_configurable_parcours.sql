-- ============================================================
-- V9 : Migration vers le système de parcours configurable
-- ============================================================

-- Renommer l'ancienne colonne 'phase' en 'phase_legacy' pour éviter les conflits
ALTER TABLE cohortes
RENAME COLUMN phase TO phase_legacy;

-- phase_id a déjà été ajouté dans V8, mais nous devons créer la contrainte si elle n'existe pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_cohorte_phase'
    ) THEN
        ALTER TABLE cohortes
        ADD CONSTRAINT fk_cohorte_phase FOREIGN KEY (phase_id) REFERENCES phases(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_cohortes_phase ON cohortes(phase_id);

-- parcours_id et phase_actuelle_id ont déjà été ajoutés dans V8 aux projets
-- mais nous devons créer les contraintes si elles n'existent pas
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint 
        WHERE conname = 'fk_projet_parcours'
    ) THEN
        ALTER TABLE projets
        ADD CONSTRAINT fk_projet_parcours FOREIGN KEY (parcours_id) REFERENCES parcours(id) ON DELETE SET NULL;
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_projets_parcours ON projets(parcours_id);

-- Corriger TransitionPhase pour utiliser UUID au lieu de VARCHAR
-- Supprimer d'abord les contraintes existantes
ALTER TABLE transitions_phase DROP CONSTRAINT IF EXISTS fk_transition_projet;

-- Recréer la table avec les bons types (plus simple que de modifier les colonnes)
DROP TABLE transitions_phase;

CREATE TABLE transitions_phase (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    projet_id UUID NOT NULL,
    phase_source_id UUID,
    phase_cible_id UUID,
    date_transition TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    type_transition VARCHAR(20) NOT NULL DEFAULT 'MANUEL',
    raison TEXT,
    effectue_par UUID,
    CONSTRAINT fk_transition_projet FOREIGN KEY (projet_id) REFERENCES projets(id) ON DELETE CASCADE,
    CONSTRAINT fk_transition_phase_source FOREIGN KEY (phase_source_id) REFERENCES phases(id) ON DELETE SET NULL,
    CONSTRAINT fk_transition_phase_cible FOREIGN KEY (phase_cible_id) REFERENCES phases(id) ON DELETE SET NULL,
    CONSTRAINT fk_transition_user FOREIGN KEY (effectue_par) REFERENCES users(id) ON DELETE SET NULL
);

CREATE INDEX idx_transitions_projet ON transitions_phase(projet_id);
CREATE INDEX idx_transitions_date ON transitions_phase(date_transition);

-- etape_id a déjà été ajouté dans V8 aux missions_cohorte et missions_modeles
-- mais nous devons l'ajouter aux missions_projet
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns 
        WHERE table_name = 'missions_projet' AND column_name = 'etape_id'
    ) THEN
        ALTER TABLE missions_projet
        ADD COLUMN etape_id UUID;
        
        ALTER TABLE missions_projet
        ADD CONSTRAINT fk_mission_projet_etape FOREIGN KEY (etape_id) REFERENCES etapes(id) ON DELETE SET NULL;
        
        CREATE INDEX idx_missions_projet_etape ON missions_projet(etape_id);
    END IF;
END $$;

-- Données de migration : créer un parcours par défaut si aucun n'existe
INSERT INTO parcours (id, nom, description, actif, structure_id, date_creation)
SELECT 
    gen_random_uuid(),
    'Parcours par défaut',
    'Parcours par défaut créé lors de la migration',
    true,
    id,
    CURRENT_TIMESTAMP
FROM structures
WHERE NOT EXISTS (SELECT 1 FROM parcours);

-- Données de migration : créer les phases par défaut si aucune n'existe
INSERT INTO phases (id, nom, description, ordre, couleur, icone, duree_estimee_jours, actif, parcours_id, date_creation)
SELECT 
    gen_random_uuid(),
    'Pré-incubation',
    'Phase de pré-incubation par défaut',
    1,
    '#3B82F6',
    'rocket',
    90,
    true,
    p.id,
    CURRENT_TIMESTAMP
FROM parcours p
WHERE NOT EXISTS (SELECT 1 FROM phases WHERE parcours_id = p.id);

INSERT INTO phases (id, nom, description, ordre, couleur, icone, duree_estimee_jours, actif, parcours_id, date_creation)
SELECT 
    gen_random_uuid(),
    'Incubation',
    'Phase d''incubation par défaut',
    2,
    '#10B981',
    'trending-up',
    180,
    true,
    p.id,
    CURRENT_TIMESTAMP
FROM parcours p
WHERE NOT EXISTS (SELECT 1 FROM phases WHERE parcours_id = p.id AND nom = 'Incubation');

INSERT INTO phases (id, nom, description, ordre, couleur, icone, duree_estimee_jours, actif, parcours_id, date_creation)
SELECT 
    gen_random_uuid(),
    'Post-incubation',
    'Phase de post-incubation par défaut',
    3,
    '#F59E0B',
    'award',
    90,
    true,
    p.id,
    CURRENT_TIMESTAMP
FROM parcours p
WHERE NOT EXISTS (SELECT 1 FROM phases WHERE parcours_id = p.id AND nom = 'Post-incubation');

-- Données de migration : lier les cohortes existantes aux phases par défaut
UPDATE cohortes c
SET phase_id = (
    SELECT p.id 
    FROM phases p 
    JOIN parcours par ON p.parcours_id = par.id 
    WHERE p.nom = CASE 
        WHEN c.phase_legacy = 'PRE_INCUBATION' THEN 'Pré-incubation'
        WHEN c.phase_legacy = 'INCUBATION' THEN 'Incubation'
        WHEN c.phase_legacy = 'POST_INCUBATION' THEN 'Post-incubation'
    END
    AND par.structure_id = c.structure_id
    LIMIT 1
)
WHERE phase_id IS NULL;

-- Données de migration : lier les projets existants aux parcours par défaut
UPDATE projets p
SET parcours_id = (
    SELECT par.id 
    FROM parcours par 
    WHERE par.structure_id = p.structure_id 
    LIMIT 1
)
WHERE parcours_id IS NULL;

-- Données de migration : lier les projets existants aux phases actuelles
UPDATE projets p
SET phase_actuelle_id = (
    SELECT c.phase_id 
    FROM cohortes c 
    WHERE c.id = p.cohorte_id
)
WHERE phase_actuelle_id IS NULL AND p.cohorte_id IS NOT NULL;
