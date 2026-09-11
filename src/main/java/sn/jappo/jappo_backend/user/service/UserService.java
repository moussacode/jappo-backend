package sn.jappo.jappo_backend.user.service;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.List;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jappo.jappo_backend.user.dto.EntrepreneurResponse;
import sn.jappo.jappo_backend.auth.service.EmailService;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.dto.InviterEntrepreneurRequest;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;
import sn.jappo.jappo_backend.user.dto.InvitationResultResponse;
import java.util.ArrayList;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final StructureRepository structureRepository;
    private final CohorteRepository cohorteRepository;
    private final MembreStructureRepository membreStructureRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public UserService(
            UserRepository userRepository,
            StructureRepository structureRepository,
            CohorteRepository cohorteRepository,
            MembreStructureRepository membreStructureRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService
    ) {
        this.userRepository = userRepository;
        this.structureRepository = structureRepository;
        this.cohorteRepository = cohorteRepository;
        this.membreStructureRepository = membreStructureRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }

   @Transactional
public InvitationResultResponse inviterEntrepreneurs(InviterEntrepreneurRequest request) {
    UUID activeStructureId = getRequiredTenantId();

    Structure structure = structureRepository.findById(activeStructureId)
            .orElseThrow(() -> new RuntimeException("Structure introuvable"));

    Cohorte cohorte = null;
    if (request.cohorteId() != null) {
        cohorte = cohorteRepository.findByIdAndStructureId(request.cohorteId(), activeStructureId)
                .orElseThrow(() -> new IllegalArgumentException("Cohorte introuvable pour cette structure."));
    }

    List<String> invites = new ArrayList<>();
    List<String> dejaMembres = new ArrayList<>();

    for (String email : request.emails()) {
        String cleanEmail = email.trim().toLowerCase();
        if (cleanEmail.isEmpty()) continue;

        // 1. Récupérer ou créer l'utilisateur global
        User user = userRepository.findByEmail(cleanEmail)
                .orElseGet(() -> {
                    User newUser = new User();
                    newUser.setNom(cleanEmail.split("@")[0]);
                    newUser.setEmail(cleanEmail);
                    newUser.setPassword(passwordEncoder.encode(UUID.randomUUID().toString()));
                    newUser.setDateCreation(LocalDateTime.now());
                    newUser.setEmailVerified(false);
                    return userRepository.save(newUser);
                });

        // 2. Vérifier si l'utilisateur est déjà membre de CETTE structure
        if (membreStructureRepository.existsByUserAndStructure(user, structure)) {
            dejaMembres.add(cleanEmail);
            continue; // On passe au suivant sans lever d'exception globale !
        }

        // 3. Créer le membre et envoyer le token
        String invitationToken = UUID.randomUUID().toString();
        MembreStructure membre = new MembreStructure();
        membre.setUser(user);
        membre.setStructure(structure);
        membre.setCohorte(cohorte);
        membre.setRole(RoleMembreStructure.ENTREPRENEUR);
        membre.setStatut(StatutMembre.EN_ATTENTE);
        membre.setInvitationToken(invitationToken);
        membre.setInvitationTokenExpiresAt(LocalDateTime.now().plusDays(7));
        membre.setDateInvitation(LocalDateTime.now());

        membreStructureRepository.save(membre);
        invites.add(cleanEmail);

        // Envoi effectif de l'email
        emailService.sendInvitationEmail(user.getEmail(), user.getNom(), invitationToken, structure.getNom());
    }

    return new InvitationResultResponse(invites, dejaMembres, invites.size());
}
    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Aucune structure active sélectionnée");
        }
        return tenantId;
    }



    @Transactional(readOnly = true)
    public List<EntrepreneurResponse> getEntrepreneursByActiveStructure() {
        UUID activeStructureId = getRequiredTenantId();

        List<MembreStructure> membres = membreStructureRepository.findByStructureIdAndRole(
                activeStructureId, 
                RoleMembreStructure.ENTREPRENEUR
        );

        return membres.stream()
                .map(m -> new EntrepreneurResponse(
                        m.getUser().getId(),
                        m.getUser().getPrenom(),
                        m.getUser().getNom(),
                        m.getUser().getEmail(),
                        m.getCohorte() != null ? m.getCohorte().getId() : null,
                        m.getCohorte() != null ? m.getCohorte().getNom() : null,
                        m.getStatut().name(),
                        m.getDateInvitation()
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public EntrepreneurResponse getEntrepreneurById(UUID userId) {
        UUID activeStructureId = getRequiredTenantId();

        MembreStructure membre = membreStructureRepository.findByUserIdAndStructureId(userId, activeStructureId)
                .orElseThrow(() -> new IllegalArgumentException("Entrepreneur introuvable dans cette structure."));

        return new EntrepreneurResponse(
                membre.getUser().getId(),
                membre.getUser().getPrenom(),
                membre.getUser().getNom(),
                membre.getUser().getEmail(),
                membre.getCohorte() != null ? membre.getCohorte().getId() : null,
                membre.getCohorte() != null ? membre.getCohorte().getNom() : null,
                membre.getStatut().name(),
                membre.getDateInvitation()
        );
    }
}