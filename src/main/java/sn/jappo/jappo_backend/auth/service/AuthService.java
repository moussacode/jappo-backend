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
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.dto.RegisterRequest;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;


import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import org.springframework.beans.factory.annotation.Value;

import java.security.GeneralSecurityException;
import java.io.IOException;
import java.util.Collections;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailVerificationService emailVerificationService;
    private final MembreStructureRepository membreStructureRepository;
    private final ProjetRepository projetRepository;
    private final JwtService jwtService;

    @Value("${app.google.client-id}")
private String googleClientId;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailVerificationService emailVerificationService,
            MembreStructureRepository membreStructureRepository,
            ProjetRepository projetRepository,
        
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailVerificationService = emailVerificationService;
        this.membreStructureRepository = membreStructureRepository;
        this.projetRepository = projetRepository;
        this.jwtService = jwtService;
        
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
        MembreStructure membre = membreStructureRepository.findByInvitationToken(token)
                .orElseThrow(() -> new IllegalArgumentException("Lien d'invitation invalide ou expiré."));

        if (membre.getInvitationTokenExpiresAt() != null && membre.getInvitationTokenExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Ce lien d'invitation a expiré.");
        }

        User user = membre.getUser();
        
        // 🔒 FIX : Le compte est réellement configuré uniquement si l'email a été vérifié / activé.
        // Un utilisateur pré-créé par invitation a emailVerified = false.
        boolean compteExiste = user.isEmailVerified();

        return new InvitationInfoResponse(
                user.getNom(),
                user.getEmail(),
                membre.getStructure().getNom(),
                membre.getStructure().getLogo(),
                compteExiste
        );
    }

    @Transactional
    public AuthResponse accepterInvitation(AccepterInvitationRequest request) {
        MembreStructure membre = membreStructureRepository.findByInvitationToken(request.token())
                .orElseThrow(() -> new IllegalArgumentException("Lien d'invitation invalide ou expiré."));

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

        user.setEmailVerified(true);
        userRepository.save(user);

        // 2. Activation de l'adhésion à la nouvelle structure
        membre.setStatut(StatutMembre.ACCEPTE);
        membre.setInvitationToken(null); // Invalidation du token d'invitation
        membreStructureRepository.save(membre);

        // 3. Création automatique du projet "Mon Projet" si l'invité est ENTREPRENEUR
        if (membre.getRole() == RoleMembreStructure.ENTREPRENEUR) {
            creerProjetParDefautSiInexistant(user, membre.getStructure());
        }

        // 4. Génération du JWT de connexion
        String jwtToken = jwtService.generateToken(user);
        return new AuthResponse(jwtToken, user.getId(), user.getNom(), user.getEmail());
    }

    private void creerProjetParDefautSiInexistant(User entrepreneur, Structure structure) {
    boolean existe = projetRepository.existsByEntrepreneurIdAndStructureId(entrepreneur.getId(), structure.getId());

    if (!existe) {
        // Nom personnalisé et friendly
        String prenom = (entrepreneur.getPrenom() != null && !entrepreneur.getPrenom().isBlank()) 
                ? entrepreneur.getPrenom() 
                : "";
        
        String nomProjet = prenom.isEmpty() ? "Mon premier projet" : "Le projet de " + prenom;

        Projet projet = new Projet();
        projet.setNom(nomProjet);
        
        // Description chaleureuse et motivante
        projet.setDescription(
            "Bienvenue dans l'aventure !  Ce projet a été créé pour te démarrer dans l'incubateur " 
            + structure.getNom() + ". N'hésite pas à personnaliser son nom, son secteur et sa description quand tu es prêt(e) !"
        );
        
        projet.setSecteur("En cours de définition");
        projet.setStatut(StatutProjet.IDEE);
        projet.setScoreMaturite(0);
        projet.setEntrepreneur(entrepreneur);
        projet.setStructure(structure);

        projetRepository.save(projet);
    }
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