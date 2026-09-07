package sn.jappo.jappo_backend.auth.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.jappo.jappo_backend.auth.entity.EmailVerificationCode;

public interface EmailVerificationCodeRepository
        extends JpaRepository<EmailVerificationCode, UUID> {

    Optional<EmailVerificationCode> findTopByEmailAndUsedFalseOrderByExpiresAtDesc(
            String email
    );
}