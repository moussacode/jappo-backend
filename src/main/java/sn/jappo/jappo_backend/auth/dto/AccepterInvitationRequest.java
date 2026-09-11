package sn.jappo.jappo_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AccepterInvitationRequest(
        @NotBlank(message = "Le token d'invitation est obligatoire")
        String token,

        
        @Size(min = 8, message = "Le mot de passe doit contenir au moins 8 caractères")
        String nouveauMotDePasse
) {}