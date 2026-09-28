package sn.jappo.jappo_backend.auth.dto;

import java.util.UUID;
import sn.jappo.jappo_backend.user.entity.RoleGlobal;

public record AuthResponse(
        String token,
        UUID id,
        String prenom,
        String nom,
        String email,
        boolean emailVerified,
        RoleGlobal roleGlobal
) {}