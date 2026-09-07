package sn.jappo.jappo_backend.auth.controller;
import sn.jappo.jappo_backend.auth.service.JwtService;
import sn.jappo.jappo_backend.auth.service.PasswordResetService;
import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.auth.dto.ChangePasswordRequest;
import sn.jappo.jappo_backend.auth.dto.ForgotPasswordRequest;
import sn.jappo.jappo_backend.auth.dto.LoginRequest;
import sn.jappo.jappo_backend.auth.dto.LoginResponse;
import sn.jappo.jappo_backend.auth.dto.ResetPasswordRequest;
import sn.jappo.jappo_backend.auth.dto.VerifyEmailRequest;
import sn.jappo.jappo_backend.auth.service.AuthService;
import sn.jappo.jappo_backend.auth.service.EmailVerificationService;
import sn.jappo.jappo_backend.user.dto.RegisterRequest;
import sn.jappo.jappo_backend.user.dto.RegisterResponse;
import sn.jappo.jappo_backend.user.dto.UserResponse;
import sn.jappo.jappo_backend.user.entity.User;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;
    private final JwtService jwtService;
    private final PasswordResetService passwordResetService;
    public AuthController(
            AuthService authService,
            EmailVerificationService emailVerificationService,
            JwtService jwtService,
            PasswordResetService passwordResetService
    ) {
        this.authService = authService;
        this.emailVerificationService = emailVerificationService;
        this.jwtService = jwtService;
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {

        User user = authService.register(request);
        

        String token = jwtService.generateToken(user);

        RegisterResponse response = new RegisterResponse(
                user.getId(),
                user.getPrenom(),
                user.getNom(),
                user.getEmail(),
                user.isEmailVerified(),
                token
        );

        return ResponseEntity.ok(response);
    }

    @PostMapping("/verify-email")
public ResponseEntity<String> verifyEmail(
        @Valid @RequestBody VerifyEmailRequest request,
        Authentication authentication
) {

    User user = (User) authentication.getPrincipal();

    emailVerificationService.verifyCode(
            user.getEmail(),
            request.code()
    );

    return ResponseEntity.ok("Email vérifié avec succès");
}


@PostMapping("/resend-verification-code")
public ResponseEntity<String> resendVerificationCode(
        Authentication authentication
) {

    User user = (User) authentication.getPrincipal();

    emailVerificationService.generateCode(user.getEmail());

    return ResponseEntity.ok(
            "Un nouveau code de vérification a été envoyé"
    );
}


@PostMapping("/login")
public ResponseEntity<LoginResponse> login(
        @Valid @RequestBody LoginRequest request
) {

    User user = authService.login(
            request.email(),
            request.password()
    );

    String token = jwtService.generateToken(user);

    LoginResponse response = new LoginResponse(
            user.getId(),
            user.getPrenom(),
            user.getNom(),
            user.getEmail(),
            user.isEmailVerified(),
            token
    );

    return ResponseEntity.ok(response);
}

@PostMapping("/resend-verification")
public ResponseEntity<String> resendVerification(
        Authentication authentication
) {

    User user = (User) authentication.getPrincipal();

    emailVerificationService.resendCode(
            user.getEmail()
    );

    return ResponseEntity.ok(
            "Un nouveau code de vérification a été envoyé"
    );
}

@PostMapping("/forgot-password")
public ResponseEntity<String> forgotPassword(
        @Valid @RequestBody ForgotPasswordRequest request
) {

    passwordResetService.requestReset(
            request.email()
    );

    return ResponseEntity.ok(
            "Si cette adresse existe, un code de réinitialisation a été envoyé"
    );
}

@PostMapping("/reset-password")
public ResponseEntity<String> resetPassword(
        @Valid @RequestBody ResetPasswordRequest request
) {

    passwordResetService.resetPassword(
            request.email(),
            request.code(),
            request.nouveauMotDePasse()
    );

    return ResponseEntity.ok(
            "Mot de passe réinitialisé avec succès"
    );
}

@PostMapping("/change-password")
public ResponseEntity<String> changePassword(
        @Valid @RequestBody ChangePasswordRequest request,
        Authentication authentication
) {
    User user = (User) authentication.getPrincipal();

    authService.changePassword(
            user,
            request.ancienMotDePasse(),
            request.nouveauMotDePasse()
    );

    return ResponseEntity.ok(
            "Mot de passe modifié avec succès"
    );
}

@GetMapping("/me")
public ResponseEntity<UserResponse> me(
        Authentication authentication
) {

    User user = (User) authentication.getPrincipal();

    return ResponseEntity.ok(
            UserResponse.from(user)
    );
}
}