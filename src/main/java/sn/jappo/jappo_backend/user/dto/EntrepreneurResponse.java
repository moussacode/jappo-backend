package sn.jappo.jappo_backend.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record EntrepreneurResponse(
        UUID id,
        String prenom,
        String nom,
        String email,
        UUID cohorteId,
        String nomCohorte,
        String statutInvitation,
        LocalDateTime dateInvitation
) {}