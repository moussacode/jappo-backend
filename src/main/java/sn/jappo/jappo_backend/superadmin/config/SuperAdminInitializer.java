package sn.jappo.jappo_backend.superadmin.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import sn.jappo.jappo_backend.user.entity.RoleGlobal;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@Component
public class SuperAdminInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${jappo.super-admin.email}")
    private String email;

    @Value("${jappo.super-admin.password}")
    private String password;

    @Value("${jappo.super-admin.prenom:Admin}")
    private String prenom;

    @Value("${jappo.super-admin.nom:JAPPO}")
    private String nom;

    public SuperAdminInitializer(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {

        if (userRepository.existsByEmail(email)) {
            return;
        }

        User user = new User();

        user.setPrenom(prenom);
        user.setNom(nom);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        user.setEmailVerified(true);
        user.setRoleGlobal(RoleGlobal.SUPER_ADMIN);

        userRepository.save(user);

        System.out.println(
                "SUPER_ADMIN initialisé : " + email
        );
    }
}