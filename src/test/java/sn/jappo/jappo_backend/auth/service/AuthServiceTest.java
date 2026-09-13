package sn.jappo.jappo_backend.auth.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private MembreStructureRepository membreStructureRepository;
    @Mock private ProjetRepository projetRepository;
    @Mock private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    void loginEchoueAvecEmailInconnu() {
        when(userRepository.findByEmail("inconnu@test.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login("inconnu@test.com", "peuImporte"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginEchoueAvecMauvaisMotDePasse() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("hash");

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("mauvais", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login("test@test.com", "mauvais"))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    void loginEchoueSiEmailNonVerifie() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("hash");
        user.setEmailVerified(false);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        assertThatThrownBy(() -> authService.login("test@test.com", "bonMotDePasse"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("vérifier votre adresse email");
    }

    @Test
    void loginReussitAvecIdentifiantsValidesEtEmailVerifie() {
        User user = new User();
        user.setEmail("test@test.com");
        user.setPassword("hash");
        user.setEmailVerified(true);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("bonMotDePasse", "hash")).thenReturn(true);

        User result = authService.login("test@test.com", "bonMotDePasse");

        assertThat(result).isEqualTo(user);
    }
}