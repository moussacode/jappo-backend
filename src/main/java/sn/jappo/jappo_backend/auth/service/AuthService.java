package sn.jappo.jappo_backend.auth.service;

import java.time.LocalDateTime;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.auth.dto.AccepterInvitationRequest;
import sn.jappo.jappo_backend.auth.dto.AuthResponse;
import sn.jappo.jappo_backend.auth.dto.InvitationInfoResponse;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.dto.RegisterRequest;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;
import sn.jappo.jappo_backend.projet.service.ProjetService;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.Collections;

import sn.jappo.jappo_backend.structure.entity.InvitationLink;
import sn.jappo.jappo_backend.structure.repository.InvitationLinkRepository;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final MembreStructureRepository membreStructureRepository;
    private final InvitationLinkRepository invitationLinkRepository;
    private final JwtService jwtService;
    private final ProjetService projetService;


    @Value("${app.google.client-id}")
private String googleClientId;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            MembreStructureRepository membreStructureRepository,
            InvitationLinkRepository invitationLinkRepository,
            JwtService jwtService,
            ProjetService projetService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.membreStructureRepository = membreStructureRepository;
        this.invitationLinkRepository = invitationLinkRepository;
        this.jwtService = jwtService;
        this.projetService = projetService;
    }

    @Transactional
    public User register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
           throw new ResponseStatusException(HttpStatus.CONFLICT, "Cet email est déjà utilisé");
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
                .orElseThrow(() -> new  BadCredentialsException("Email ou mot de passe incorrect"));
        if (user.getPassword() == null) {
    throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
            "Ce compte a été créé avec Google. Utilisez le bouton \"Se connecter avec Google\".");
}


        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BadCredentialsException("Email ou mot de passe incorrect");
        }

        if (!user.isEmailVerified()) {
            throw new ResponseStatusException(
    HttpStatus.FORBIDDEN,
    "Veuillez vérifier votre adresse email avant de vous connecter"
);
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

    @Transactional(readOnly = true)
    public InvitationInfoResponse getInvitationInfo(String token) {
        var membreOpt = membreStructureRepository.findByInvitationToken(token);
        if (membreOpt.isPresent()) {
            MembreStructure membre = membreOpt.get();
            if (membre.getInvitationTokenExpiresAt() != null && membre.getInvitationTokenExpiresAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Ce lien d'invitation a expiré.");
            }

            User user = membre.getUser();
            boolean compteExiste = user.isEmailVerified();
            String nomUser = (user.getPrenom() != null ? user.getPrenom() + " " : "") + (user.getNom() != null ? user.getNom() : "");

            return new InvitationInfoResponse(
                    nomUser.trim().isEmpty() ? null : nomUser.trim(),
                    user.getEmail(),
                    membre.getStructure().getNom(),
                    membre.getStructure().getLogo(),
                    compteExiste,
                    membre.getRole() != null ? membre.getRole().name() : null
            );
        }

        var linkOpt = invitationLinkRepository.findByToken(token);
        if (linkOpt.isPresent()) {
            InvitationLink link = linkOpt.get();
            if (!link.isActif() || link.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Ce lien d'invitation est expiré ou a été révoqué.");
            }

            return new InvitationInfoResponse(
                    null,
                    null,
                    link.getStructure().getNom(),
                    link.getStructure().getLogo(),
                    false,
                    link.getRole() != null ? link.getRole().name() : null
            );
        }

        throw new IllegalArgumentException("Lien d'invitation invalide ou expiré.");
    }

    @Transactional
    public AuthResponse accepterInvitation(AccepterInvitationRequest request) {
        var membreOpt = membreStructureRepository.findByInvitationToken(request.token());
        if (membreOpt.isPresent()) {
            MembreStructure membre = membreOpt.get();
            if (membre.getInvitationTokenExpiresAt() != null && membre.getInvitationTokenExpiresAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Ce lien d'invitation a expiré.");
            }

            User user = membre.getUser();

            boolean aUnCompteActif = user.isEmailVerified();
            boolean unNouveauMotDePasseEstFourni = request.nouveauMotDePasse() != null && !request.nouveauMotDePasse().isBlank();

            // 1. CONTRAINTE STRICTE : Si l'utilisateur n'a pas encore validé son compte, le mot de passe est OBLIGATOIRE
            if (!aUnCompteActif && !unNouveauMotDePasseEstFourni) {
                throw new IllegalArgumentException("Un mot de passe est obligatoire pour finaliser la création de votre compte.");
            }

            // Validation de la taille minimale
            if (!aUnCompteActif && request.nouveauMotDePasse().trim().length() < 8) {
                throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères.");
            }

            // Définition du vrai mot de passe utilisateur
            if (unNouveauMotDePasseEstFourni) {
                user.setPassword(passwordEncoder.encode(request.nouveauMotDePasse().trim()));
            }

            if (request.prenom() != null && !request.prenom().isBlank()) {
                user.setPrenom(request.prenom().trim());
            }
            if (request.nom() != null && !request.nom().isBlank()) {
                user.setNom(request.nom().trim());
            }

            user.setEmailVerified(true);
            userRepository.save(user);

            // 2. Activation de l'adhésion à la nouvelle structure
            membre.setStatut(StatutMembre.ACCEPTE);
            membre.setInvitationToken(null); // Invalidation du token d'invitation
            membreStructureRepository.save(membre);

            // 3. Création automatique du projet si l'invité est ENTREPRENEUR
            if (membre.getRole() == RoleMembreStructure.ENTREPRENEUR) {
                projetService.creerProjetPourEntrepreneur(user, membre);
            }

            // 4. Génération du JWT de connexion
            String jwtToken = jwtService.generateToken(user);
            return new AuthResponse(
                    jwtToken,
                    user.getId(),
                    user.getPrenom(),
                    user.getNom(),
                    user.getEmail(),
                    user.isEmailVerified(),
                    user.getRoleGlobal()
            );
        }

        // Cas du lien partagé (InvitationLink)
        var linkOpt = invitationLinkRepository.findByToken(request.token());
        if (linkOpt.isPresent()) {
            InvitationLink link = linkOpt.get();
            if (!link.isActif() || link.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new IllegalArgumentException("Ce lien d'invitation est expiré ou a été révoqué.");
            }

            if (request.email() == null || request.email().isBlank()) {
                throw new IllegalArgumentException("L'adresse email est requise.");
            }
            String email = request.email().trim().toLowerCase();

            User user = userRepository.findByEmail(email).orElseGet(() -> {
                User newUser = new User();
                newUser.setEmail(email);
                return newUser;
            });

            boolean aUnCompteActif = user.isEmailVerified();
            boolean unNouveauMotDePasseEstFourni = request.nouveauMotDePasse() != null && !request.nouveauMotDePasse().isBlank();

            if (!aUnCompteActif && !unNouveauMotDePasseEstFourni) {
                throw new IllegalArgumentException("Un mot de passe est obligatoire pour finaliser la création de votre compte.");
            }
            if (!aUnCompteActif && request.nouveauMotDePasse().trim().length() < 8) {
                throw new IllegalArgumentException("Le mot de passe doit contenir au moins 8 caractères.");
            }

            if (unNouveauMotDePasseEstFourni) {
                user.setPassword(passwordEncoder.encode(request.nouveauMotDePasse().trim()));
            }
            if (request.prenom() != null && !request.prenom().isBlank()) {
                user.setPrenom(request.prenom().trim());
            }
            if (request.nom() != null && !request.nom().isBlank()) {
                user.setNom(request.nom().trim());
            }

            user.setEmailVerified(true);
            user = userRepository.save(user);

            var existantMembre = membreStructureRepository.findByUserIdAndStructureId(user.getId(), link.getStructure().getId());
            MembreStructure membre;
            if (existantMembre.isPresent()) {
                membre = existantMembre.get();
                membre.setRole(link.getRole());
                membre.setStatut(StatutMembre.ACCEPTE);
            } else {
                membre = new MembreStructure();
                membre.setUser(user);
                membre.setStructure(link.getStructure());
                membre.setRole(link.getRole());
                membre.setStatut(StatutMembre.ACCEPTE);
            }
            membreStructureRepository.save(membre);

            if (membre.getRole() == RoleMembreStructure.ENTREPRENEUR) {
                projetService.creerProjetPourEntrepreneur(user, membre);
            }

            String jwtToken = jwtService.generateToken(user);
            return new AuthResponse(
                    jwtToken,
                    user.getId(),
                    user.getPrenom(),
                    user.getNom(),
                    user.getEmail(),
                    user.isEmailVerified(),
                    user.getRoleGlobal()
            );
        }

        throw new IllegalArgumentException("Lien d'invitation invalide ou expiré.");
    }


/** Connexion Google — le compte doit déjà exister (utilisateur déjà invité). */
@Transactional
public User loginGoogle(String idTokenString) {
    GoogleIdToken.Payload payload = verifierTokenGoogle(idTokenString);
    String email = payload.getEmail();

    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Aucun compte associé à cet email. Vous devez d'abord être invité par une structure."
            ));

    if (!user.isEmailVerified()) {
        user.setEmailVerified(true);
        userRepository.save(user);
    }

    return user;
}

/** Inscription Google — utilisée pour créer le compte d'un fondateur de structure. */
@Transactional
public User inscriptionGoogle(String idTokenString) {
    GoogleIdToken.Payload payload = verifierTokenGoogle(idTokenString);
    String email = payload.getEmail();
    String prenom = (String) payload.get("given_name");
    String nom = (String) payload.get("family_name");

    return userRepository.findByEmail(email)
            .map(existant -> {
                if (!existant.isEmailVerified()) {
                    existant.setEmailVerified(true);
                    userRepository.save(existant);
                }
                return existant;
            })
            .orElseGet(() -> {
                User nouveau = new User();
                nouveau.setEmail(email);
                nouveau.setPrenom(prenom);
                nouveau.setNom(nom);
                nouveau.setPassword(null);
                nouveau.setEmailVerified(true);
                return userRepository.save(nouveau);
            });
}

private GoogleIdToken.Payload verifierTokenGoogle(String idTokenString) {
    try {
        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken idToken = verifier.verify(idTokenString);
        if (idToken == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Jeton Google invalide.");
        }
        return idToken.getPayload();
    } catch (GeneralSecurityException | IOException e) {
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Impossible de vérifier le jeton Google.");
    }
}

}