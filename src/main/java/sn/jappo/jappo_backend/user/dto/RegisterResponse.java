package sn.jappo.jappo_backend.user.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID id,
        String prenom,
        String nom,
        String email,
        boolean emailVerified,
        String token
) {
}