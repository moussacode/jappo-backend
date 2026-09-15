package sn.jappo.jappo_backend.structure.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MembreEquipeResponse(
        UUID id,
        String prenom,
        String nom,
        String email,
        String role,
        String statut,
        boolean estProprietaire,
        LocalDateTime dateInvitation,
        LocalDateTime invitationTokenExpiresAt
) {}
