package sn.jappo.jappo_backend.projet.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.cohorte.repository.ParticipationCohorteRepository;
import sn.jappo.jappo_backend.cohorte.service.ParticipationService;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.projet.dto.CreateProjetRequest;
import sn.jappo.jappo_backend.projet.dto.ProjetResponse;
import sn.jappo.jappo_backend.projet.dto.UpdateProjetRequest;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@Service
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final StructureRepository structureRepository;
    private final CohorteRepository cohorteRepository;
    private final UserRepository userRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final MembreStructureRepository membreStructureRepository;
    private final ParticipationService participationService;
    private final ParticipationCohorteRepository participationCohorteRepository;
    private final ParcoursPhaseRepository parcoursPhaseRepository;

    public ProjetService(
            ProjetRepository projetRepository,
            StructureRepository structureRepository,
            CohorteRepository cohorteRepository,
            UserRepository userRepository,
            MissionProjetRepository missionProjetRepository,
            MembreStructureRepository membreStructureRepository,
            ParticipationService participationService,
            ParticipationCohorteRepository participationCohorteRepository,
            ParcoursPhaseRepository parcoursPhaseRepository
    ) {
        this.projetRepository = projetRepository;
        this.structureRepository = structureRepository;
        this.cohorteRepository = cohorteRepository;
        this.userRepository = userRepository;
        this.missionProjetRepository = missionProjetRepository;
        this.membreStructureRepository = membreStructureRepository;
        this.participationService = participationService;
        this.participationCohorteRepository = participationCohorteRepository;
        this.parcoursPhaseRepository = parcoursPhaseRepository;
    }

    // -----------------------------------------------------------------------
    // Création
    // -----------------------------------------------------------------------

    @Transactional
    public ProjetResponse createProjet(CreateProjetRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Structure structure = structureRepository
                .findById(activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Structure introuvable"
                ));

        Projet projet = new Projet();
        projet.setNom(request.nom());
        projet.setDescription(request.description());
        projet.setSecteur(request.secteur());
        projet.setScoreMaturite(0);
        projet.setStructure(structure);
        projet.setStatut(StatutProjet.ACTIF);

        Cohorte cohorte = null;

        if (request.cohorteId() != null) {
            cohorte = cohorteRepository
                    .findByIdAndStructureId(
                            request.cohorteId(),
                            activeStructureId
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.NOT_FOUND,
                            "Cohorte introuvable pour cet incubateur"
                    ));
        }

        if (request.entrepreneurId() != null) {
            MembreStructure membre = membreStructureRepository
                    .findByUserIdAndStructureId(
                            request.entrepreneurId(),
                            activeStructureId
                    )
                    .filter(m ->
                            m.getRole() == RoleMembreStructure.ENTREPRENEUR
                    )
                    .orElseThrow(() -> new ResponseStatusException(
                            HttpStatus.BAD_REQUEST,
                            "Cet utilisateur n'est pas un entrepreneur membre de cette structure."
                    ));

            projet.setEntrepreneur(membre.getUser());
        }

        Projet saved = projetRepository.save(projet);

        // Si une cohorte est spécifiée, ouvrir une participation
        // (assigne aussi les missions)
        if (cohorte != null) {
            User currentUser = null;
            participationService.ouvrirParticipation(
                    saved,
                    cohorte,
                    currentUser
            );
        }

        return mapToResponse(saved);
    }

    /**
     * Crée un projet pour un entrepreneur lors de l'acceptation d'invitation.
     */
    @Transactional
    public void creerProjetPourEntrepreneur(
            User entrepreneur,
            MembreStructure membre
    ) {
        Structure structure = membre.getStructure();

        boolean existe = projetRepository
                .existsByEntrepreneurIdAndStructureId(
                        entrepreneur.getId(),
                        structure.getId()
                );

        if (!existe) {
            String prenom =
                    (entrepreneur.getPrenom() != null
                            && !entrepreneur.getPrenom().isBlank())
                            ? entrepreneur.getPrenom()
                            : "";

            String nomProjet = prenom.isEmpty()
                    ? "Mon premier projet"
                    : "Le projet de " + prenom;

            Projet projet = new Projet();

            projet.setNom(nomProjet);
            projet.setDescription(
                    "Bienvenue dans l'aventure ! Ce projet a été créé pour te démarrer "
                            + "dans l'incubateur "
                            + structure.getNom()
                            + ". N'hésite pas à le personnaliser !"
            );
            projet.setSecteur("En cours de définition");
            projet.setStatut(StatutProjet.ACTIF);
            projet.setScoreMaturite(0);
            projet.setEntrepreneur(entrepreneur);
            projet.setStructure(structure);

            Projet saved = projetRepository.save(projet);

            // Si le membre a une cohorte d'invitation,
            // ouvrir la participation.
            if (membre.getCohorte() != null) {
                participationService.ouvrirParticipation(
                        saved,
                        membre.getCohorte(),
                        null
                );
            }
        }
    }

    // -----------------------------------------------------------------------
    // Lecture
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ProjetResponse> getProjetsForActiveStructure(
            String filtreArchivage
    ) {
        UUID activeStructureId = getRequiredTenantId();

        String filtre = filtreArchivage == null
                ? "TOUS"
                : filtreArchivage.toUpperCase();

        return projetRepository
                .findAllByStructureId(activeStructureId)
                .stream()
                .filter(p -> switch (filtre) {
                    case "ACTIFS" -> !p.isArchive();
                    case "ARCHIVES" -> p.isArchive();
                    default -> true;
                })
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> getProjetsByCohorte(
            UUID cohorteId
    ) {
        UUID activeStructureId = getRequiredTenantId();

        return projetRepository
                .findAllByCohorteIdAndStructureId(
                        cohorteId,
                        activeStructureId
                )
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjetResponse getProjetById(UUID id) {
        UUID activeStructureId = getRequiredTenantId();

        Projet projet = projetRepository
                .findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Projet introuvable pour cet ID : " + id
                ));

        return mapToResponse(projet);
    }

    @Transactional(readOnly = true)
    public ProjetResponse getProjetPrincipalByEntrepreneur(
            UUID entrepreneurId
    ) {
        UUID activeStructureId = getRequiredTenantId();

        Projet projet = projetRepository
                .findByEntrepreneurIdAndStructureId(
                        entrepreneurId,
                        activeStructureId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Aucun projet trouvé pour cet entrepreneur dans la structure active"
                ));

        return mapToResponse(projet);
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> getProjetsForEntrepreneur(
            UUID entrepreneurId
    ) {
        UUID activeStructureId = getRequiredTenantId();

        membreStructureRepository
                .findByUserIdAndStructureId(
                        entrepreneurId,
                        activeStructureId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Entrepreneur non trouvé dans cette structure"
                ));

        return projetRepository
                .findAllByStructureId(activeStructureId)
                .stream()
                .filter(p -> !p.isArchive())
                .filter(p ->
                        p.getEntrepreneur() != null
                                && p.getEntrepreneur()
                                        .getId()
                                        .equals(entrepreneurId)
                )
                .map(this::mapToResponse)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Mise à jour
    // -----------------------------------------------------------------------

    @Transactional
    public ProjetResponse updateNomProjet(
            UUID id,
            String nouveauNom,
            User currentUser
    ) {
        UUID activeStructureId = getRequiredTenantId();

        Projet projet = projetRepository
                .findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Projet introuvable"
                ));

        verifierDroitModification(
                projet,
                currentUser,
                activeStructureId
        );

        verifierNonArchive(projet);

        projet.setNom(nouveauNom);

        return mapToResponse(
                projetRepository.save(projet)
        );
    }

    @Transactional
    public ProjetResponse updateProjet(
            UUID id,
            UpdateProjetRequest request,
            User currentUser
    ) {
        UUID activeStructureId = getRequiredTenantId();

        Projet projet = projetRepository
                .findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Projet introuvable"
                ));

        verifierDroitModification(
                projet,
                currentUser,
                activeStructureId
        );

        verifierNonArchive(projet);

        if (request.nom() != null) {
            projet.setNom(request.nom());
        }

        if (request.description() != null) {
            projet.setDescription(request.description());
        }

        if (request.secteur() != null) {
            projet.setSecteur(request.secteur());
        }

        return mapToResponse(
                projetRepository.save(projet)
        );
    }

    // -----------------------------------------------------------------------
    // Archivage
    // -----------------------------------------------------------------------

    @Transactional
    public void archiverProjet(
            UUID id,
            User currentUser
    ) {
        UUID activeStructureId = getRequiredTenantId();

        Projet projet = projetRepository
                .findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Projet introuvable"
                ));

        verifierDroitModification(
                projet,
                currentUser,
                activeStructureId
        );

        if (!projet.isArchive()) {
            participationCohorteRepository
                    .findByProjetIdAndDateSortieIsNull(id)
                    .ifPresent(p ->
                            participationService.fermerParticipation(
                                    p,
                                    sn.jappo.jappo_backend.cohorte.entity
                                            .ParticipationCohorte
                                            .MotifSortie
                                            .RETIRE,
                                    "Archivage du projet",
                                    currentUser
                            )
                    );

            projet.setArchive(true);
            projet.setDateArchivage(LocalDateTime.now());

            projetRepository.save(projet);
        }
    }

    @Transactional
    public void restaurerProjet(
            UUID id,
            User currentUser
    ) {
        UUID activeStructureId = getRequiredTenantId();

        Projet projet = projetRepository
                .findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Projet introuvable"
                ));

        verifierDroitModification(
                projet,
                currentUser,
                activeStructureId
        );

        if (projet.isArchive()) {
            projet.setArchive(false);
            projet.setDateArchivage(null);

            projetRepository.save(projet);
        }
    }

    // -----------------------------------------------------------------------
    // Helpers sécurité
    // -----------------------------------------------------------------------

    private void verifierDroitModification(
            Projet projet,
            User currentUser,
            UUID activeStructureId
    ) {
        if (currentUser == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Utilisateur non authentifié"
            );
        }

        MembreStructure membre = membreStructureRepository
                .findByUserIdAndStructureId(
                        currentUser.getId(),
                        activeStructureId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN,
                        "Vous n'êtes pas membre de cette structure"
                ));

        boolean estEncadrant =
                membre.getRole() == RoleMembreStructure.ADMIN_STRUCTURE
                        || membre.getRole() == RoleMembreStructure.COACH;

        boolean estProprietaire =
                projet.getEntrepreneur() != null
                        && projet.getEntrepreneur()
                                .getId()
                                .equals(currentUser.getId());

        if (!estEncadrant && !estProprietaire) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Vous n'êtes pas autorisé à modifier ce projet"
            );
        }
    }

    private void verifierNonArchive(Projet projet) {
        if (projet.isArchive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Ce projet est archivé et ne peut plus être modifié"
            );
        }
    }

    // -----------------------------------------------------------------------
    // Mapping
    // -----------------------------------------------------------------------

    private ProjetResponse mapToResponse(Projet projet) {
        String nomEntrepreneur = null;
        UUID entrepreneurId = null;

        if (projet.getEntrepreneur() != null) {
            entrepreneurId = projet.getEntrepreneur().getId();

            nomEntrepreneur =
                    (
                            (projet.getEntrepreneur().getPrenom() != null
                                    ? projet.getEntrepreneur().getPrenom()
                                    : "")
                                    + " "
                                    + (projet.getEntrepreneur().getNom() != null
                                            ? projet.getEntrepreneur().getNom()
                                            : "")
                    ).trim();
        }

        UUID cohorteId =
                projet.getCohorte() != null
                        ? projet.getCohorte().getId()
                        : null;

        String nomCohorte =
                projet.getCohorte() != null
                        ? projet.getCohorte().getNom()
                        : null;

        UUID parcoursId = null;
        String nomParcours = null;
        UUID phaseId = null;
        String nomPhase = null;
        Integer phaseOrdre = null;

        if (projet.getCohorte() != null) {

            Cohorte cohorte = projet.getCohorte();

            if (cohorte.getParcours() != null) {
                parcoursId = cohorte.getParcours().getId();
                nomParcours = cohorte.getParcours().getNom();
            }

            if (cohorte.getPhase() != null) {
                phaseId = cohorte.getPhase().getId();
                nomPhase = cohorte.getPhase().getNom();

                /*
                 * L'ordre n'est plus stocké dans Phase.
                 * Il est porté par ParcoursPhase.
                 */
                if (parcoursId != null) {
                    phaseOrdre = parcoursPhaseRepository
                            .findByParcours_IdAndPhase_Id(
                                    parcoursId,
                                    phaseId
                            )
                            .map(ParcoursPhase::getOrdre)
                            .orElse(null);
                }
            }
        }

        UUID structureId = projet.getStructure().getId();

        int nombreMissionsTotal =
                (int) missionProjetRepository
                        .countByProjetIdAndStructureId(
                                projet.getId(),
                                structureId
                        );

        int nombreMissionsValidees =
                (int) missionProjetRepository
                        .countByProjetIdAndStructureIdAndStatut(
                                projet.getId(),
                                structureId,
                                StatutMission.VALIDE
                        );

        return new ProjetResponse(
                projet.getId(),
                projet.getNom(),
                projet.getDescription(),
                projet.getSecteur(),
                projet.getScoreMaturite(),
                projet.getStatut(),
                entrepreneurId,
                nomEntrepreneur,
                cohorteId,
                nomCohorte,
                projet.getStructure().getId(),
                projet.getDateCreation(),
                projet.isArchive(),
                projet.getDateArchivage(),
                nombreMissionsTotal,
                nombreMissionsValidees,
                parcoursId,
                nomParcours,
                phaseId,
                nomPhase,
                phaseOrdre
        );
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "En-tête X-Structure-Id manquant"
            );
        }

        return tenantId;
    }
}

