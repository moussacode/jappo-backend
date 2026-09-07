package sn.jappo.jappo_backend.auth.service;

import java.time.LocalDateTime;


import org.springframework.stereotype.Service;

import sn.jappo.jappo_backend.auth.entity.EmailVerificationCode;
import sn.jappo.jappo_backend.auth.repository.EmailVerificationCodeRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;
import java.security.SecureRandom;

@Service
public class EmailVerificationService {

    private final EmailVerificationCodeRepository codeRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();
    
    public EmailVerificationService(
        EmailVerificationCodeRepository codeRepository,
        UserRepository userRepository,
        EmailService emailService
) {
    this.codeRepository = codeRepository;
    this.userRepository = userRepository;
    this.emailService = emailService;
}
    public String generateCode(String email) {

    String code = String.format(
            "%06d",
            secureRandom.nextInt(1_000_000)
    );

    EmailVerificationCode verificationCode =
            new EmailVerificationCode();

    verificationCode.setEmail(email);
    verificationCode.setCode(code);

    verificationCode.setExpiresAt(
            LocalDateTime.now().plusMinutes(10)
    );

    verificationCode.setUsed(false);

    codeRepository.save(verificationCode);

    // Envoi réel de l'OTP par email
    emailService.sendVerificationCode(email, code);

    return code;
}

    public void verifyCode(String email, String code) {

    EmailVerificationCode verificationCode =
            codeRepository
                    .findTopByEmailAndUsedFalseOrderByExpiresAtDesc(email)
                    .orElseThrow(() ->
                            new RuntimeException("Code OTP introuvable")
                    );

    if (verificationCode.getExpiresAt().isBefore(LocalDateTime.now())) {
        throw new RuntimeException("Le code OTP a expiré");
    }

    if (!verificationCode.getCode().equals(code)) {
        throw new RuntimeException("Code OTP incorrect");
    }

    User user = userRepository
            .findByEmail(email)
            .orElseThrow(() ->
                    new RuntimeException("Utilisateur introuvable")
            );

    user.setEmailVerified(true);

    userRepository.save(user);

    verificationCode.setUsed(true);

    codeRepository.save(verificationCode);
}

public String resendCode(String email) {

    User user = userRepository.findByEmail(email)
            .orElseThrow(() ->
                    new RuntimeException("Utilisateur introuvable")
            );

    if (user.isEmailVerified()) {
        throw new RuntimeException(
                "Cette adresse email est déjà vérifiée"
        );
    }

    return generateCode(email);
}
}