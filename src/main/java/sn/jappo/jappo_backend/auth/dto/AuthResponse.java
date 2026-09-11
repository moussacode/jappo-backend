package sn.jappo.jappo_backend.auth.dto;

import java.util.UUID;

public record AuthResponse(
        String token,
        UUID id,
        String nom,
        String email
) {}