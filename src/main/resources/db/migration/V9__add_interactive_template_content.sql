ALTER TABLE livrables_attendus
    ADD COLUMN template_configuration TEXT NULL;

ALTER TABLE livrables
    ADD COLUMN contenu MEDIUMTEXT NULL;

ALTER TABLE livrable_versions
    ADD COLUMN contenu MEDIUMTEXT NULL;
