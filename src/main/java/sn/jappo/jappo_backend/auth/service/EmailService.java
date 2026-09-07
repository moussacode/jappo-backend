package sn.jappo.jappo_backend.auth.service;

import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
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

    public void sendPasswordResetCode(
        String email,
        String code
) {

    //  implémentation d'envoi email
    SimpleMailMessage message = new SimpleMailMessage();

        message.setTo(email);

        message.setSubject(
                "JAPPO - Réinitialisation de votre mot de passe"
        );

        message.setText("""
                Bonjour,

                Vous avez demandé la réinitialisation de votre
                mot de passe JAPPO.

                Votre code de réinitialisation est :

                %s

                Ce code est valable pendant 10 minutes.

                Si vous n'êtes pas à l'origine de cette demande,
                ignorez simplement cet email.

                L'équipe JAPPO
                """.formatted(code));

        mailSender.send(message);

}
}