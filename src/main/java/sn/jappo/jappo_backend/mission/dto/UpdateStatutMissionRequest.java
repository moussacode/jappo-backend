package sn.jappo.jappo_backend.mission.dto;

import sn.jappo.jappo_backend.mission.entity.StatutMission;

public record UpdateStatutMissionRequest(
        StatutMission statut
) {}