package sn.jappo.jappo_backend.auth.dto;

public record InvitationInfoResponse(
        String nomUser,
        String email,
        String nomStructure,
        String logoStructure,
        boolean compteExiste
) {}