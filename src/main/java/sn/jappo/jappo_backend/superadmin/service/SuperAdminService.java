package sn.jappo.jappo_backend.superadmin.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.user.entity.RoleGlobal;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@Service
public class SuperAdminService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public SuperAdminService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public User createSuperAdmin(
            String prenom,
            String nom,
            String email,
            String password
    ) {

        if (userRepository.existsByEmail(email)) {
            throw new IllegalStateException(
                    "Un utilisateur existe déjà avec cet email."
            );
        }

        User user = new User();

        user.setPrenom(prenom);
        user.setNom(nom);
        user.setEmail(email);

        user.setPassword(
                passwordEncoder.encode(password)
        );

        user.setEmailVerified(true);
        user.setRoleGlobal(RoleGlobal.SUPER_ADMIN);

        return userRepository.save(user);
    }


    
}