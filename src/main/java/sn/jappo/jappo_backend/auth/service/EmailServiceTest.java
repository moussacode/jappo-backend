
package sn.jappo.jappo_backend.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceTest {

    private final JavaMailSender mailSender;

    public EmailServiceTest(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("JAPPO - Vérification de votre adresse email");
        message.setText("""
                Bonjour,

                Votre code de vérification JAPPO est :

                %s

                Ce code est valable pendant 10 minutes.

                Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet email.

                L'équipe JAPPO
                """.formatted(code));

        mailSender.send(message);
    }

    public void sendPasswordResetCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("JAPPO - Réinitialisation de votre mot de passe");
        message.setText("""
                Bonjour,

                Vous avez demandé la réinitialisation de votre mot de passe JAPPO.

                Votre code de réinitialisation est :

                %s

                Ce code est valable pendant 10 minutes.

                Si vous n'êtes pas à l'origine de cette demande, ignorez simplement cet email.

                L'équipe JAPPO
                """.formatted(code));

        mailSender.send(message);
    }

    // NOUVELLE MÉTHODE : Envoi du Magic Link d'invitation
    public void sendInvitationEmail(String email, String nom, String token, String nomStructure) {
        String magicLink = "http://localhost:4200/auth/accept-invitation?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("JAPPO - Invitation à rejoindre " + nomStructure);
        message.setText("""
                Bonjour %s,

                Vous avez été invité(e) à rejoindre la structure %s sur la plateforme JAPPO.

                Pour accepter l'invitation et finaliser la création de votre compte, cliquez sur le lien ci-dessous :

                %s

                Ce lien d'invitation est valable pendant 7 jours.

                Si vous pensez qu'il s'agit d'une erreur, vous pouvez ignorer ce message.

                L'équipe JAPPO
                """.formatted(nom != null ? nom : "", nomStructure, magicLink));

        mailSender.send(message);
    }
}