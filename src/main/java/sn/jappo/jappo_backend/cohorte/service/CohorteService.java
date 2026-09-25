package sn.jappo.jappo_backend.cohorte.service;


import sn.jappo.jappo_backend.abonnement.service.PlanLimiteService;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.cohorte.dto.CohorteResponse;
import sn.jappo.jappo_backend.cohorte.dto.CreateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.dto.UpdateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.events.CohortCreatedEvent;
import sn.jappo.jappo_backend.events.CohortCompletedEvent;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;
import sn.jappo.jappo_backend.auth.service.EmailService;

import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CohorteService {

    private final CohorteRepository cohorteRepository;
    private final StructureRepository structureRepository;
    private final ParcoursRepository parcoursRepository;
    private final PhaseRepository phaseRepository;
    private final ParcoursPhaseRepository parcoursPhaseRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final MembreStructureRepository membreStructureRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final PlanLimiteService planLimiteService;

    public CohorteService(
            CohorteRepository cohorteRepository,
            StructureRepository structureRepository,
            ParcoursRepository parcoursRepository,
            PhaseRepository phaseRepository,
            ParcoursPhaseRepository parcoursPhaseRepository,
            ApplicationEventPublisher eventPublisher,
            MembreStructureRepository membreStructureRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            EmailService emailService,
            PlanLimiteService planLimiteService
    ) {
        this.cohorteRepository = cohorteRepository;
        this.structureRepository = structureRepository;
        this.parcoursRepository = parcoursRepository;
        this.phaseRepository = phaseRepository;
        this.parcoursPhaseRepository = parcoursPhaseRepository;
        this.eventPublisher = eventPublisher;
        this.membreStructureRepository = membreStructureRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.planLimiteService = planLimiteService;
    }

    @Transactional
    public CohorteResponse createCohorte(CreateCohorteRequest request) {
        UUID activeStructureId = getRequiredTenantId();
        planLimiteService.verifierCreationCohorte(activeStructureId);

        Structure structure = structureRepository.findById(activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Structure non trouvée"
                ));

        if (request.parcoursId() == null || request.phaseId() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "parcoursId et phaseId sont obligatoires à la création d'une cohorte"
            );
        }

        Cohorte cohorte = new Cohorte();

        cohorte.setNom(request.nom());
        cohorte.setDescription(request.description());
        cohorte.setDateDebut(request.dateDebut());
        cohorte.setDateFin(request.dateFin());
        cohorte.setStatut(StatutCohorte.PLANIFIEE);
        cohorte.setStructure(structure);

        appliquerParcoursEtPhase(
                cohorte,
                request.parcoursId(),
                request.phaseId(),
                activeStructureId
        );

        Cohorte saved = cohorteRepository.save(cohorte);

        eventPublisher.publishEvent(
                new CohortCreatedEvent(
                        activeStructureId,
                        saved.getId(),
                        saved.getNom(),
                        Instant.now()
                )
        );

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CohorteResponse> getCohortesForActiveStructure() {
        UUID activeStructureId = getRequiredTenantId();

        return cohorteRepository
                .findAllByStructureId(activeStructureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CohorteResponse> getActiveCohortesForActiveStructure() {
        UUID activeStructureId = getRequiredTenantId();

        return cohorteRepository
                .findAllByStructureIdAndStatutNot(
                        activeStructureId,
                        StatutCohorte.ARCHIVEE
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CohorteResponse getCohorteById(UUID id) {
        UUID activeStructureId = getRequiredTenantId();

        Cohorte cohorte = cohorteRepository
                .findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cohorte introuvable ou accès non autorisé pour l'ID : " + id
                ));

        return mapToResponse(cohorte);
    }

    @Transactional
    public CohorteResponse updateCohorte(
            UUID id,
            UpdateCohorteRequest request
    ) {
        UUID activeStructureId = getRequiredTenantId();

        Cohorte cohorte = cohorteRepository
                .findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cohorte introuvable"
                ));

        // Refus de changement de parcours si la cohorte
        // possède des projets actifs.
        if (request.parcoursId() != null
                && cohorte.getParcours() != null
                && !cohorte.getParcours()
                        .getId()
                        .equals(request.parcoursId())) {

            if (cohorteRepository.hasActiveProjects(id)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Impossible de changer le parcours : la cohorte a des projets actifs"
                );
            }
        }

        StatutCohorte ancienStatut = cohorte.getStatut();

        if (request.nom() != null) {
            cohorte.setNom(request.nom());
        }

        if (request.description() != null) {
            cohorte.setDescription(request.description());
        }

        if (request.dateDebut() != null) {
            cohorte.setDateDebut(request.dateDebut());
        }

        if (request.dateFin() != null) {
            cohorte.setDateFin(request.dateFin());
        }

        if (request.statut() != null) {
            cohorte.setStatut(request.statut());
        }

        if (request.parcoursId() != null || request.phaseId() != null) {

            UUID parcoursId = request.parcoursId() != null
                    ? request.parcoursId()
                    : cohorte.getParcours().getId();

            UUID phaseId = request.phaseId();

            if (phaseId != null) {
                appliquerParcoursEtPhase(
                        cohorte,
                        parcoursId,
                        phaseId,
                        activeStructureId
                );
            }
        }

        Cohorte saved = cohorteRepository.save(cohorte);

        if (request.statut() == StatutCohorte.TERMINEE
                && ancienStatut != StatutCohorte.TERMINEE) {

            eventPublisher.publishEvent(
                    new CohortCompletedEvent(
                            activeStructureId,
                            saved.getId(),
                            saved.getNom(),
                            Instant.now()
                    )
            );
        }

        return mapToResponse(saved);
    }

    @Transactional
    public void archiverCohorte(UUID id) {
        UUID activeStructureId = getRequiredTenantId();

        Cohorte cohorte = cohorteRepository
                .findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cohorte introuvable"
                ));

        cohorte.setStatut(StatutCohorte.ARCHIVEE);

        cohorteRepository.save(cohorte);
    }

    @Transactional
    public void restaurerCohorte(UUID id) {
        UUID activeStructureId = getRequiredTenantId();

        Cohorte cohorte = cohorteRepository
                .findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cohorte introuvable"
                ));

        if (cohorte.getStatut() == StatutCohorte.ARCHIVEE) {
            cohorte.setStatut(StatutCohorte.EN_COURS);
            cohorteRepository.save(cohorte);
        }
    }

    @Transactional(readOnly = true)
    public List<CohorteResponse> getCohortesByStatut(
            StatutCohorte statut
    ) {
        UUID activeStructureId = getRequiredTenantId();

        return cohorteRepository
                .findAllByStructureIdAndStatut(
                        activeStructureId,
                        statut
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Invite une liste d'entrepreneurs dans une cohorte.
     */
    @Transactional
    public void inviterEntrepreneurs(
            UUID cohorteId,
            List<String> emails
    ) {
        UUID activeStructureId = getRequiredTenantId();

        Cohorte cohorte = cohorteRepository
                .findByIdAndStructureId(
                        cohorteId,
                        activeStructureId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cohorte introuvable"
                ));

        Structure structure = cohorte.getStructure();

        for (String email : emails) {
            inviterEntrepreneurParEmail(
                    email,
                    structure,
                    cohorte
            );
        }
    }

    private void inviterEntrepreneurParEmail(
            String email,
            Structure structure,
            Cohorte cohorte
    ) {

        User user = userRepository
                .findByEmail(email)
                .orElseGet(() -> {

                    User u = new User();

                    u.setEmail(email);
                    u.setEmailVerified(false);
                    u.setPassword(
                            passwordEncoder.encode(
                                    UUID.randomUUID().toString()
                            )
                    );

                    return userRepository.save(u);
                });

        var existantOpt =
                membreStructureRepository
                        .findByUserIdAndStructureId(
                                user.getId(),
                                structure.getId()
                        );

        if (existantOpt.isPresent()) {

            MembreStructure existant = existantOpt.get();

            if (existant.getStatut() == StatutMembre.ACCEPTE) {

                // Déjà membre actif :
                // on met simplement à jour la cohorte.
                existant.setCohorte(cohorte);

                membreStructureRepository.save(existant);

                return;
            }

            // Ré-invitation avec nouvelle cohorte.
            String token = UUID.randomUUID().toString();

            existant.setRole(
                    RoleMembreStructure.ENTREPRENEUR
            );

            existant.setCohorte(cohorte);
            existant.setInvitationToken(token);
            existant.setInvitationTokenExpiresAt(
                    LocalDateTime.now().plusDays(7)
            );

            membreStructureRepository.save(existant);

            sendInvitation(
                    email,
                    user,
                    token,
                    structure.getNom()
            );

            return;
        }

        String token = UUID.randomUUID().toString();

        MembreStructure membre = new MembreStructure();

        membre.setUser(user);
        membre.setStructure(structure);
        membre.setRole(RoleMembreStructure.ENTREPRENEUR);
        membre.setStatut(StatutMembre.EN_ATTENTE);
        membre.setCohorte(cohorte);
        membre.setInvitationToken(token);
        membre.setInvitationTokenExpiresAt(
                LocalDateTime.now().plusDays(7)
        );

        membreStructureRepository.save(membre);

        sendInvitation(
                email,
                user,
                token,
                structure.getNom()
        );
    }

    private void sendInvitation(
            String email,
            User user,
            String token,
            String nomStructure
    ) {
        try {
            emailService.sendInvitationEmail(
                    email,
                    user.getPrenom() != null
                            ? user.getPrenom()
                            : "Futur entrepreneur",
                    token,
                    nomStructure
            );
        } catch (Exception e) {
            // Ne pas bloquer l'invitation si l'email échoue.
        }
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    /**
     * Associe une cohorte à un parcours et à une phase existante.
     *
     * La phase est désormais réutilisable entre plusieurs parcours.
     * L'appartenance de la phase au parcours est donc vérifiée via
     * ParcoursPhase.
     */
    private void appliquerParcoursEtPhase(
            Cohorte c,
            UUID parcoursId,
            UUID phaseId,
            UUID structureId
    ) {

        Parcours parcours = parcoursRepository
                .findByIdAndStructureId(
                        parcoursId,
                        structureId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parcours introuvable pour cette structure"
                ));

        ParcoursPhase parcoursPhase =
                parcoursPhaseRepository
                        .findByParcours_IdAndPhase_Id(
                                parcours.getId(),
                                phaseId
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.BAD_REQUEST,
                                "La phase n'appartient pas à ce parcours"
                        ));

        Phase phase = parcoursPhase.getPhase();

        if (phase == null || phase.isArchive()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La phase sélectionnée est invalide ou archivée"
            );
        }

        // Vérification supplémentaire du tenant.
        if (phase.getStructure() == null
                || !phase.getStructure()
                        .getId()
                        .equals(structureId)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La phase n'appartient pas à cette structure"
            );
        }

        c.setParcours(parcours);
        c.setPhase(phase);
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Aucune structure active sélectionnée (en-tête X-Structure-Id manquant)"
            );
        }

        return tenantId;
    }

    public CohorteResponse mapToResponse(Cohorte cohorte) {

        Parcours parcours = cohorte.getParcours();
        Phase phase = cohorte.getPhase();

        CohorteResponse.PhaseSummary phaseSummary = null;

        if (phase != null) {

            Integer ordre = null;

            if (parcours != null) {

                ordre = parcoursPhaseRepository
                        .findByParcours_IdAndPhase_Id(
                                parcours.getId(),
                                phase.getId()
                        )
                        .map(ParcoursPhase::getOrdre)
                        .orElse(null);
            }

            phaseSummary = new CohorteResponse.PhaseSummary(
                    phase.getId(),
                    phase.getNom(),
                    ordre
            );
        }

        return new CohorteResponse(
                cohorte.getId(),
                cohorte.getNom(),
                cohorte.getDescription(),
                cohorte.getDateDebut(),
                cohorte.getDateFin(),
                cohorte.getStatut(),
                parcours != null
                        ? parcours.getId()
                        : null,
                phase != null
                        ? phase.getId()
                        : null,
                phaseSummary,
                cohorte.getStructure().getId()
        );
    }
}