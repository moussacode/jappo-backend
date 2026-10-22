package sn.jappo.jappo_backend.projet.service;

import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.user.entity.User;

/**
 * Contrôle d'accès en lecture à un projet à l'intérieur de la structure active.
 *
 * Le filtrage par structure (tenant) reste à la charge des services ; ce garde ajoute
 * la règle intra-structure : un ADMIN_STRUCTURE ou un COACH voit tous les projets de la
 * structure active, un ENTREPRENEUR uniquement les siens.
 *
 * Les autorités sont celles calculées par JwtAuthenticationFilter pour la structure
 * active (X-Structure-Id) : un rôle détenu dans une autre structure n'est jamais pris en compte.
 *
 * Refus par défaut : sans utilisateur authentifié, l'accès est refusé.
 * Un refus renvoie 404 pour ne pas révéler l'existence du projet.
 */
@Component
public class ProjetAccessGuard {

    private static final Set<String> AUTORITES_GESTIONNAIRE = Set.of(
            "ADMIN_STRUCTURE", "ROLE_ADMIN_STRUCTURE", "COACH", "ROLE_COACH");

    public boolean estGestionnaire() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> AUTORITES_GESTIONNAIRE.contains(authority.getAuthority()));
    }

    public void verifierLecture(Projet projet) {
        if (estGestionnaire()) {
            return;
        }
        User user = utilisateurCourant();
        if (user == null || projet == null || projet.getEntrepreneur() == null
                || !user.getId().equals(projet.getEntrepreneur().getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable");
        }
    }

    /** Un entrepreneur ne peut consulter que ses propres données ; un gestionnaire, celles de tous. */
    public void verifierLectureEntrepreneur(UUID entrepreneurId) {
        if (estGestionnaire()) {
            return;
        }
        User user = utilisateurCourant();
        if (user == null || entrepreneurId == null || !user.getId().equals(entrepreneurId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable");
        }
    }

    private User utilisateurCourant() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getPrincipal() instanceof User user ? user : null;
    }
}
