package sn.jappo.jappo_backend.auth.service;

import java.time.LocalDateTime;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.auth.entity.PasswordResetToken;
import sn.jappo.jappo_backend.auth.repository.PasswordResetTokenRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@Service
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(
            PasswordResetTokenRepository tokenRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.tokenRepository = tokenRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    public void requestReset(String email) {

        User user = userRepository.findByEmail(email)
                .orElse(null);

        /*
         * On ne révèle pas si l'email existe.
         */
        if (user == null) {
            return;
        }

        String code = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        PasswordResetToken token =
                new PasswordResetToken();

        token.setEmail(email);
        token.setCode(code);
        token.setExpiresAt(
                LocalDateTime.now().plusMinutes(10)
        );
        token.setUsed(false);

        tokenRepository.save(token);

        emailService.sendPasswordResetCode(
                email,
                code
        );
    }

    @Transactional
    public void resetPassword(
            String email,
            String code,
            String nouveauMotDePasse
    ) {

        PasswordResetToken token =
                tokenRepository
                        .findTopByEmailAndUsedFalseOrderByExpiresAtDesc(
                                email
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Code de réinitialisation introuvable"
                                )
                        );

        if (token.getExpiresAt()
                .isBefore(LocalDateTime.now())) {

            throw new RuntimeException(
                    "Le code de réinitialisation a expiré"
            );
        }

        if (!token.getCode().equals(code)) {

            throw new RuntimeException(
                    "Code de réinitialisation incorrect"
            );
        }

        User user = userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Utilisateur introuvable"
                        )
                );

        user.setPassword(
                passwordEncoder.encode(
                        nouveauMotDePasse
                )
        );

        userRepository.save(user);

        token.setUsed(true);

        tokenRepository.save(token);
    }
}