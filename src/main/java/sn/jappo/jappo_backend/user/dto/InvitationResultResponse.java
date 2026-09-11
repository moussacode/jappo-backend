package sn.jappo.jappo_backend.user.dto;

import java.util.List;

public record InvitationResultResponse(
        List<String> invites,        // Emails invités avec succès
        List<String> dejaMembres,    // Emails déjà membres de la structure
        int totalEnvoyes
) {}