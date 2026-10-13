package sn.jappo.jappo_backend.superadmin.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record ChangerPlanRequest(
        @NotNull(message = "Le plan est obligatoire")
        UUID planId
) {
}
