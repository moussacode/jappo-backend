package sn.jappo.jappo_backend.user.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;
import java.util.UUID;

public record InviterEntrepreneurRequest(
        @NotEmpty(message = "Au moins une adresse email doit être fournie")
        List<String> emails,

        UUID cohorteId
) {}