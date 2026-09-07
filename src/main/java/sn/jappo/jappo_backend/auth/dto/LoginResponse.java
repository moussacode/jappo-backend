package sn.jappo.jappo_backend.auth.dto;

import java.util.UUID;

public record LoginResponse(
        UUID id,
        String prenom,
        String nom,
        String email,
        boolean emailVerified,
        String token
) {
}