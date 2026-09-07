package sn.jappo.jappo_backend.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jappo.jappo_backend.user.dto.RegisterRequest;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
    }

    @Transactional
    public User register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Cet email est déjà utilisé");
        }

        User user = new User();

        user.setPrenom(request.prenom());
        user.setNom(request.nom());
        user.setEmail(request.email());

        user.setPassword(
                passwordEncoder.encode(request.password()));

        user.setEmailVerified(false);

        User savedUser = userRepository.save(user);

        emailVerificationService.generateCode(user.getEmail());

        return savedUser;
    }

    public User login(String email, String password) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Email ou mot de passe incorrect"));

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Email ou mot de passe incorrect");
        }

        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "Veuillez vérifier votre adresse email avant de vous connecter");
        }

        return user;
    }

    @Transactional
public void changePassword(
        User user,
        String ancienMotDePasse,
        String nouveauMotDePasse
) {

    if (!passwordEncoder.matches(
            ancienMotDePasse,
            user.getPassword()
    )) {
        throw new RuntimeException(
                "Ancien mot de passe incorrect"
        );
    }

    user.setPassword(
            passwordEncoder.encode(nouveauMotDePasse)
    );

    userRepository.save(user);
}

}