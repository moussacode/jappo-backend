package sn.jappo.jappo_backend.auth.service;

import org.springframework.stereotype.Service;

@Service
public class EmailService {

    public void sendVerificationCode(String email, String code) {
        System.out.println("\n========================================================================");
        System.out.println("[CONSOLE ONLY] Code de vérification pour : " + email);
        System.out.println("Code : " + code);
        System.out.println("========================================================================\n");
    }

    public void sendPasswordResetCode(String email, String code) {
        System.out.println("\n========================================================================");
        System.out.println("[CONSOLE ONLY] Code de réinitialisation pour : " + email);
        System.out.println("Code : " + code);
        System.out.println("========================================================================\n");
    }

    // Uniquement affichage du lien d'invitation dans le terminal (aucun envoi d'email)
    public void sendInvitationEmail(String email, String nom, String token, String nomStructure) {
        String magicLink = "http://localhost:4200/auth/accept-invitation?token=" + token;

        System.out.println("\n========================================================================");
        System.out.println("📩 [CONSOLE ONLY] Invitation créée pour : " + email);
        System.out.println("🏛️ Structure : " + nomStructure);
        System.out.println("🔗 Lien direct d'activation (Magic Link) :");
        System.out.println(magicLink);
        System.out.println("========================================================================\n");
    }
}


