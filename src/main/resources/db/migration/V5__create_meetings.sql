-- ============================================================
-- V5 : Création des tables meetings et meeting_participants
-- ============================================================
-- Ces tables soutiennent le module de réunions vidéo avec LiveKit.
-- Les UUID du projet Jappo sont stockés en BINARY(16).
-- ============================================================

CREATE TABLE meetings (
    id                 BINARY(16)   NOT NULL PRIMARY KEY,
    structure_id       BINARY(16)   NOT NULL,
    coach_id           BINARY(16)   NOT NULL,
    title              VARCHAR(255) NOT NULL,
    description        VARCHAR(1000),
    type               VARCHAR(20)  NOT NULL,
    cohort_id          BINARY(16),
    scheduled_at       DATETIME(6)  NOT NULL,
    duration_minutes   INT          NOT NULL,
    status             VARCHAR(20)  NOT NULL DEFAULT 'PLANNED',
    room_identifier    VARCHAR(100) NOT NULL UNIQUE,
    created_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6)
                                      ON UPDATE CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_meeting_structure
        FOREIGN KEY (structure_id)
        REFERENCES structures(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_meeting_coach
        FOREIGN KEY (coach_id)
        REFERENCES membres_structures(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_meeting_cohort
        FOREIGN KEY (cohort_id)
        REFERENCES cohortes(id)
        ON DELETE SET NULL,

    INDEX idx_meeting_structure (structure_id),
    INDEX idx_meeting_coach (coach_id),
    INDEX idx_meeting_cohort (cohort_id),
    INDEX idx_meeting_status (status),
    INDEX idx_meeting_scheduled (scheduled_at),
    INDEX idx_meeting_room (room_identifier)
);


CREATE TABLE meeting_participants (
    id                 BINARY(16)  NOT NULL PRIMARY KEY,
    meeting_id         BINARY(16)  NOT NULL,
    user_id            BINARY(16)  NOT NULL,
    role               VARCHAR(20) NOT NULL,
    joined_at          DATETIME(6),
    left_at            DATETIME(6),
    created_at         DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),

    CONSTRAINT fk_participant_meeting
        FOREIGN KEY (meeting_id)
        REFERENCES meetings(id)
        ON DELETE CASCADE,

    CONSTRAINT fk_participant_user
        FOREIGN KEY (user_id)
        REFERENCES membres_structures(id)
        ON DELETE CASCADE,

    CONSTRAINT uk_meeting_user
        UNIQUE (meeting_id, user_id),

    INDEX idx_participant_meeting (meeting_id),
    INDEX idx_participant_user (user_id),
    INDEX idx_participant_role (role)
);