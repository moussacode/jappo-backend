package sn.jappo.jappo_backend.structure.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.auth.service.EmailService; // 👈 Import du service mail
import sn.jappo.jappo_backend.structure.entity.*;
import sn.jappo.jappo_backend.structure.repository.InvitationLinkRepository;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class InvitationService {

    private final InvitationLinkRepository invitationLinkRepository;
    private final MembreStructureRepository membreStructureRepository;
    private final StructureRepository structureRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService; // 👈 Déclaration du service mail

    public InvitationService(
            InvitationLinkRepository invitationLinkRepository,
            MembreStructureRepository membreStructureRepository,
            StructureRepository structureRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService // 👈 Injection dans le constructeur
    ) {
        this.invitationLinkRepository = invitationLinkRepository;
        this.membreStructureRepository = membreStructureRepository;
        this.structureRepository = structureRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

    // 1. Inviter un membre de l'équipe nominativement par email
    @Transactional
    public void inviterMembreParEmail(UUID structureId, String email, RoleMembreStructure role) {
        if (role == RoleMembreStructure.ENTREPRENEUR) {
            throw new IllegalArgumentException("Seuls les administrateurs et coachs peuvent être invités dans l'équipe.");
        }

        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Structure introuvable"));

        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User newUser = new User();
            newUser.setEmail(email);
            newUser.setEmailVerified(false);
            
            // Attribuer un mot de passe temporaire pour éviter la valeur NULL en BDD
            String tempPassword = UUID.randomUUID().toString();
            newUser.setPassword(passwordEncoder.encode(tempPassword));
            
            return userRepository.save(newUser);
        });

        var existantOpt = membreStructureRepository.findByUserIdAndStructureId(user.getId(), structureId);
        if (existantOpt.isPresent()) {
            MembreStructure existant = existantOpt.get();
            if (existant.getStatut() == StatutMembre.ACCEPTE) {
                throw new IllegalArgumentException("Cet utilisateur fait déjà partie de cette structure en tant que membre actif.");
            }
            String token = UUID.randomUUID().toString();
            existant.setRole(role);
            existant.setInvitationToken(token);
            existant.setInvitationTokenExpiresAt(LocalDateTime.now().plusDays(7));
            existant.setDateInvitation(LocalDateTime.now());
            membreStructureRepository.save(existant);

            emailService.sendInvitationEmail(
                    email,
                    user.getPrenom() != null ? user.getPrenom() : "Futur membre",
                    token,
                    structure.getNom()
            );
            return;
        }

        String token = UUID.randomUUID().toString();

        MembreStructure membre = new MembreStructure();
        membre.setUser(user);
        membre.setStructure(structure);
        membre.setRole(role);
        membre.setStatut(StatutMembre.EN_ATTENTE);
        membre.setInvitationToken(token);
        membre.setInvitationTokenExpiresAt(LocalDateTime.now().plusDays(7));

        membreStructureRepository.save(membre);

        // 📨 Affichage du Magic Link dans la console Spring Boot
        emailService.sendInvitationEmail(
                email,
                user.getPrenom() != null ? user.getPrenom() : "Futur membre",
                token,
                structure.getNom()
        );
    }

    // 2. Obtenir ou Régénérer le lien d'invitation direct
    @Transactional
    public String getOrGenerateShareLink(UUID structureId, RoleMembreStructure role, boolean regenerate) {
        if (regenerate) {
            invitationLinkRepository.desactiverAnciensLiens(structureId, role);
        } else {
            var lienExistant = invitationLinkRepository
                    .findByStructureIdAndRoleAndActifTrueAndExpiresAtAfter(structureId, role, LocalDateTime.now());
            if (lienExistant.isPresent()) {
                return construireUrlLien(lienExistant.get().getToken());
            }
        }

        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Structure introuvable"));

        InvitationLink nouveauLien = new InvitationLink();
        nouveauLien.setToken(UUID.randomUUID().toString());
        nouveauLien.setStructure(structure);
        nouveauLien.setRole(role);
        nouveauLien.setExpiresAt(LocalDateTime.now().plusDays(7));
        nouveauLien.setActif(true);

        invitationLinkRepository.save(nouveauLien);

        return construireUrlLien(nouveauLien.getToken());
    }

    // 3. Révoquer immédiatement un lien partagé
    @Transactional
    public void revoquerLienPartage(UUID structureId, RoleMembreStructure role) {
        invitationLinkRepository.desactiverAnciensLiens(structureId, role);
    }

    // 4. Récupérer les membres de l'équipe (hors entrepreneurs)
    @Transactional(readOnly = true)
    public List<MembreStructure> getMembresEquipe(UUID structureId) {
        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Structure introuvable"));

        return membreStructureRepository.findAllByStructure(structure)
                .stream()
                .filter(m -> m.getRole() == RoleMembreStructure.ADMIN_STRUCTURE || m.getRole() == RoleMembreStructure.COACH)
                .toList();
    }

    // 5. Modifier le rôle d'un membre avec vérification du Propriétaire
    @Transactional
    public void modifierRoleMembre(UUID structureId, UUID membreUserId, RoleMembreStructure nouveauRole) {
        if (nouveauRole == RoleMembreStructure.ENTREPRENEUR) {
            throw new IllegalArgumentException("Le rôle ENTREPRENEUR ne s'applique pas à l'équipe.");
        }

        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Structure introuvable"));

        // Sécurité Métier : Le rôle du propriétaire de la structure ne peut jamais être modifié
        if (structure.getProprietaire() != null && structure.getProprietaire().getId().equals(membreUserId)) {
            throw new IllegalArgumentException("Impossible de modifier le rôle du propriétaire de la structure.");
        }

        MembreStructure membre = membreStructureRepository.findByUserIdAndStructureId(membreUserId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable."));

        membre.setRole(nouveauRole);
        membreStructureRepository.save(membre);
    }

    // Méthode utilitaire pour vérifier si un membre est le propriétaire
    public boolean estProprietaire(Structure structure, User user) {
        return structure.getProprietaire() != null && structure.getProprietaire().getId().equals(user.getId());
    }

    // 6. Renvoyer l'invitation à un membre en attente / expirée
    @Transactional
    public void renvoyerInvitation(UUID structureId, UUID membreUserId) {
        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Structure introuvable"));

        MembreStructure membre = membreStructureRepository.findByUserIdAndStructureId(membreUserId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable."));

        if (membre.getStatut() == StatutMembre.ACCEPTE) {
            throw new IllegalArgumentException("Ce membre a déjà accepté son invitation.");
        }

        String token = UUID.randomUUID().toString();
        membre.setInvitationToken(token);
        membre.setInvitationTokenExpiresAt(LocalDateTime.now().plusDays(7));
        membre.setDateInvitation(LocalDateTime.now());
        membreStructureRepository.save(membre);

        User user = membre.getUser();
        emailService.sendInvitationEmail(
                user.getEmail(),
                user.getPrenom() != null ? user.getPrenom() : "Futur membre",
                token,
                structure.getNom()
        );
    }

    // 7. Annuler une invitation en attente / expirée
    @Transactional
    public void annulerInvitation(UUID structureId, UUID membreUserId) {
        MembreStructure membre = membreStructureRepository.findByUserIdAndStructureId(membreUserId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Invitation introuvable."));

        if (membre.getStatut() == StatutMembre.ACCEPTE) {
            throw new IllegalArgumentException("Impossible d'annuler une invitation déjà acceptée.");
        }

        membreStructureRepository.delete(membre);
    }

    // 8. Retirer un membre de la structure (avec protection du propriétaire)
    @Transactional
    public void retirerMembre(UUID structureId, UUID membreUserId) {
        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Structure introuvable"));

        if (structure.getProprietaire() != null && structure.getProprietaire().getId().equals(membreUserId)) {
            throw new IllegalArgumentException("Le propriétaire de la structure ne peut pas être retiré.");
        }

        MembreStructure membre = membreStructureRepository.findByUserIdAndStructureId(membreUserId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Membre introuvable dans cette structure."));

        membreStructureRepository.delete(membre);
    }

    private String construireUrlLien(String token) {
        return "http://localhost:4200/auth/accept-invitation?token=" + token;
    }
}