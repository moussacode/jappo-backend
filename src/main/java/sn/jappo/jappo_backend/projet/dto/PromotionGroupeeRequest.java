package sn.jappo.jappo_backend.projet.dto;

import java.util.List;
import java.util.UUID;

public record PromotionGroupeeRequest(
    List<UUID> projetIds,
    UUID cohorteCibleId,
    String raison,
    boolean forcer
) {}
