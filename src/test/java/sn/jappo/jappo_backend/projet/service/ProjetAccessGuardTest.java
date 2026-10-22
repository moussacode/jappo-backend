package sn.jappo.jappo_backend.projet.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.user.entity.User;

class ProjetAccessGuardTest {

    private final ProjetAccessGuard guard = new ProjetAccessGuard();

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void gestionnaireDeLaStructureActive_peutLireNimporteQuelProjet() {
        authentifier(user(), "ROLE_ADMIN_STRUCTURE");
        assertDoesNotThrow(() -> guard.verifierLecture(projetDe(user())));

        authentifier(user(), "COACH");
        assertDoesNotThrow(() -> guard.verifierLecture(projetDe(user())));
        assertDoesNotThrow(() -> guard.verifierLectureEntrepreneur(UUID.randomUUID()));
    }

    @Test
    void entrepreneur_peutLireSonProjetUniquement() {
        User entrepreneur = user();
        authentifier(entrepreneur, "ENTREPRENEUR", "ROLE_ENTREPRENEUR");

        assertDoesNotThrow(() -> guard.verifierLecture(projetDe(entrepreneur)));
        assertDoesNotThrow(() -> guard.verifierLectureEntrepreneur(entrepreneur.getId()));

        ResponseStatusException refus = assertThrows(ResponseStatusException.class,
                () -> guard.verifierLecture(projetDe(user())));
        assertEquals(HttpStatus.NOT_FOUND, refus.getStatusCode());
        assertThrows(ResponseStatusException.class,
                () -> guard.verifierLectureEntrepreneur(UUID.randomUUID()));
    }

    @Test
    void projetSansEntrepreneur_estRefuseAUnNonGestionnaire() {
        authentifier(user(), "ENTREPRENEUR");
        assertThrows(ResponseStatusException.class, () -> guard.verifierLecture(new Projet()));
    }

    @Test
    void sansAuthentification_toutEstRefuse() {
        assertThrows(ResponseStatusException.class, () -> guard.verifierLecture(projetDe(user())));
        assertThrows(ResponseStatusException.class, () -> guard.verifierLectureEntrepreneur(UUID.randomUUID()));
    }

    private void authentifier(User user, String... autorites) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(
                user, null, Arrays.stream(autorites).map(SimpleGrantedAuthority::new).toList()));
    }

    private User user() {
        User user = new User();
        user.setId(UUID.randomUUID());
        return user;
    }

    private Projet projetDe(User entrepreneur) {
        Projet projet = new Projet();
        projet.setEntrepreneur(entrepreneur);
        return projet;
    }
}
