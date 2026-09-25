package sn.jappo.jappo_backend.superadmin.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record SuperAdminTransactionResponse(
        UUID id,
        String structureNom,
        UUID structureId,
        long montant,
        String devise,
        String statut,
        String moyenPaiement,
        String telephoneClient,
        String refCommand,
        String tokenPaiement,
        String planVise,
        LocalDateTime dateCreation,
        LocalDateTime dateConfirmation,
        String payloadIpnBrut
) {
}