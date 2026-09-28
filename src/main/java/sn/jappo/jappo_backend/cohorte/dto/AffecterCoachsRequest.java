package sn.jappo.jappo_backend.cohorte.dto;

import java.util.List;
import java.util.UUID;

public record AffecterCoachsRequest(List<UUID> coachIds) {}
