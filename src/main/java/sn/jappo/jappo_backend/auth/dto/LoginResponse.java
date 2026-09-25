package sn.jappo.jappo_backend.auth.dto;

import java.util.UUID;

import sn.jappo.jappo_backend.user.entity.RoleGlobal;

public record LoginResponse(
        UUID id,
        String prenom,
        String nom,
        String email,
        boolean emailVerified,
        RoleGlobal roleGlobal,
        String token
) {
}