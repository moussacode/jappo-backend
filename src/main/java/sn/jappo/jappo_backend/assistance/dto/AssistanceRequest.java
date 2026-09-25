
package sn.jappo.jappo_backend.assistance.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record AssistanceRequest(

    @NotBlank(message = "L'email est obligatoire")
    @Email(message = "L'adresse email est invalide")
    String email,

    @NotBlank(message = "Le message est obligatoire")
    String message

) {
}

