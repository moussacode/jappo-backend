package sn.jappo.jappo_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(

        @NotBlank
        String ancienMotDePasse,

        @NotBlank
        @Size(min = 8)
        String nouveauMotDePasse
) {}