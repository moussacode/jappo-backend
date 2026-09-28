
package sn.jappo.jappo_backend.auth.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${app.frontend.url:https://recoil-reverb-carless.ngrok-free.dev}")
    private String frontendUrl;

    @Value("${spring.mail.username}")
    private String senderEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendVerificationCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(email);
        message.setSubject("Code de vérification - JAPPO");
        message.setText(
                "Bonjour,\n\n" +
                "Votre code de vérification JAPPO est :\n\n" +
                code + "\n\n" +
                "Ce code est valable pendant une durée limitée.\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.\n\n" +
                "L'équipe JAPPO"
        );

        mailSender.send(message);
    }

    public void sendPasswordResetCode(String email, String code) {
        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(email);
        message.setSubject("Réinitialisation de votre mot de passe - JAPPO");
        message.setText(
                "Bonjour,\n\n" +
                "Voici votre code pour réinitialiser votre mot de passe JAPPO :\n\n" +
                code + "\n\n" +
                "Ce code est valable pendant une durée limitée.\n\n" +
                "Si vous n'êtes pas à l'origine de cette demande, vous pouvez ignorer cet email.\n\n" +
                "L'équipe JAPPO"
        );

        mailSender.send(message);
    }

    public void sendInvitationEmail(
            String email,
            String nom,
            String token,
            String nomStructure
    ) {
        String magicLink ="https://recoil-reverb-carless.ngrok-free.dev/auth/accept-invitation?token=" + token;

        SimpleMailMessage message = new SimpleMailMessage();

        message.setFrom(senderEmail);
        message.setTo(email);
        message.setSubject("Invitation à rejoindre " + nomStructure + " - JAPPO");

        message.setText(
                "Bonjour " + nom + ",\n\n" +
                "Vous avez été invité(e) à rejoindre la structure \"" +
                nomStructure + "\" sur JAPPO.\n\n" +
                "Pour accepter l'invitation et activer votre compte, cliquez sur le lien suivant :\n\n" +
                magicLink + "\n\n" +
                "Si vous n'êtes pas à l'origine de cette invitation, vous pouvez ignorer cet email.\n\n" +
                "L'équipe JAPPO"
        );

        mailSender.send(message);
    }
}

