package sn.jappo.jappo_backend.mission.dto;

import java.time.LocalDate;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;

public record UpdateMissionRequest(
        String titre,
        String description,
        LocalDate dateEcheance,
        PrioriteMission priorite
) {}