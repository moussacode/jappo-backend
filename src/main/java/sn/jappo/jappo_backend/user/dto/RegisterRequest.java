package sn.jappo.jappo_backend.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record RegisterRequest(

        @NotBlank
        String prenom,

        @NotBlank
        String nom,

        @NotBlank
        @Email
        String email,

        @NotBlank
        String password

) {
}