package sn.jappo.jappo_backend.auth.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "email_verification_codes")
@Getter
@Setter
public class EmailVerificationCode {

    @Id
    @GeneratedValue
    private UUID id;

    private String email;

    private String code;

    private LocalDateTime expiresAt;

    private boolean used = false;
}