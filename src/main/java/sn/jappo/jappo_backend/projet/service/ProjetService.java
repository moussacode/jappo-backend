package sn.jappo.jappo_backend.projet.service;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jappo.jappo_backend.projet.dto.UpdateProjetRequest;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.projet.dto.CreateProjetRequest;
import sn.jappo.jappo_backend.projet.dto.ProjetResponse;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;



import sn.jappo.jappo_backend.cohorte.entity.PhaseParcours;

import sn.jappo.jappo_backend.mission.entity.StatutMission;

import sn.jappo.jappo_backend.projet.dto.PromouvoirProjetRequest;

import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;

@Service
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final StructureRepository structureRepository;
    private final CohorteRepository cohorteRepository;
    private final UserRepository userRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final MembreStructureRepository membreStructureRepository;

    public ProjetService(
            ProjetRepository projetRepository,
            StructureRepository structureRepository,
            CohorteRepository cohorteRepository,
            UserRepository userRepository,
            MissionProjetRepository missionProjetRepository,
             MembreStructureRepository membreStructureRepository

            
    ) {
        this.projetRepository = projetRepository;
        this.structureRepository = structureRepository;
        this.cohorteRepository = cohorteRepository;
        this.userRepository = userRepository;
        this.missionProjetRepository = missionProjetRepository;
         this.membreStructureRepository = membreStructureRepository;
    }

@Transactional
public ProjetResponse createProjet(CreateProjetRequest request) {
    UUID activeStructureId = getRequiredTenantId();

    Structure structure = structureRepository.findById(activeStructureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Structure introuvable"));

    Projet projet = new Projet();
    projet.setNom(request.nom());
    projet.setDescription(request.description());
    projet.setSecteur(request.secteur());
    projet.setScoreMaturite(0);
    projet.setStructure(structure);

    Cohorte cohorte = null;
    if (request.cohorteId() != null) {
        cohorte = cohorteRepository.findByIdAndStructureId(request.cohorteId(), activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohorte introuvable pour cet incubateur"));
        projet.setCohorte(cohorte);
    }

    if (request.entrepreneurId() != null) {
        MembreStructure membre = membreStructureRepository
                .findByUserIdAndStructureId(request.entrepreneurId(), activeStructureId)
                .filter(m -> m.getRole() == RoleMembreStructure.ENTREPRENEUR)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Cet utilisateur n'est pas un entrepreneur membre de cette structure."));
        projet.setEntrepreneur(membre.getUser());
    }

    projet.setStatut(cohorte != null ? statutPourPhase(cohorte.getPhase()) : StatutProjet.IDEE);

    return mapToResponse(projetRepository.save(projet));
}

private StatutProjet statutPourPhase(PhaseParcours phase) {
    return switch (phase) {
        case PRE_INCUBATION -> StatutProjet.IDEE;
        case INCUBATION -> StatutProjet.EN_INCUBATION;
        case POST_INCUBATION -> StatutProjet.EN_ACCELERATION;
    };
}
    /**
     * @param filtreArchivage "ACTIFS" (défaut), "ARCHIVES" ou "TOUS" — insensible à la casse.
     *                        Défaut sur "TOUS" en interne pour ne pas casser silencieusement
     *                        les écrans existants qui appellent GET /api/projets sans paramètre :
     *                        avant ce correctif, aucun projet n'était jamais filtré. Le frontend
     *                        doit passer explicitement ACTIFS ou ARCHIVES pour bénéficier du filtre.
     */
    @Transactional(readOnly = true)
    public List<ProjetResponse> getProjetsForActiveStructure(String filtreArchivage) {
        UUID activeStructureId = getRequiredTenantId();
        String filtre = filtreArchivage == null ? "TOUS" : filtreArchivage.toUpperCase();

        return projetRepository.findAllByStructureId(activeStructureId)
                .stream()
                .filter(p -> switch (filtre) {
                    case "ACTIFS" -> !p.isArchive();
                    case "ARCHIVES" -> p.isArchive();
                    default -> true; // "TOUS" ou valeur inconnue
                })
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ProjetResponse> getProjetsByCohorte(UUID cohorteId) {
        UUID activeStructureId = getRequiredTenantId();
        return projetRepository.findAllByCohorteIdAndStructureId(cohorteId, activeStructureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProjetResponse getProjetById(UUID id) {
        UUID activeStructureId = getRequiredTenantId();
        Projet projet = projetRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Projet introuvable pour cet ID : " + id));
        return mapToResponse(projet);
    }

    // AJOUT : Récupération du projet d'un entrepreneur pour la structure active
    @Transactional(readOnly = true)
public ProjetResponse getProjetPrincipalByEntrepreneur(UUID entrepreneurId) {
    UUID activeStructureId = getRequiredTenantId();
    
    Projet projet = projetRepository.findByEntrepreneurIdAndStructureId(entrepreneurId, activeStructureId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, 
                    "Aucun projet trouvé pour cet entrepreneur (" + entrepreneurId + ") dans la structure active"
            ));

    return mapToResponse(projet);
}
    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Aucune structure active sélectionnée (en-tête X-Structure-Id manquant)");
        }
        return tenantId;
    }

    private ProjetResponse mapToResponse(Projet projet) {
        String nomEntrepreneur = null;
        UUID entrepreneurId = null;
        if (projet.getEntrepreneur() != null) {
            entrepreneurId = projet.getEntrepreneur().getId();
            nomEntrepreneur = (projet.getEntrepreneur().getPrenom() != null ? projet.getEntrepreneur().getPrenom() : "")
                    + " "
                    + (projet.getEntrepreneur().getNom() != null ? projet.getEntrepreneur().getNom() : "");
        }

        UUID cohorteId = projet.getCohorte() != null ? projet.getCohorte().getId() : null;
        String nomCohorte = projet.getCohorte() != null ? projet.getCohorte().getNom() : null;

        // Calculer les statistiques de missions pour le projet
        UUID structureId = projet.getStructure().getId();
        Integer nombreMissionsTotal = (int) missionProjetRepository.countByProjetIdAndStructureId(projet.getId(), structureId);
        Integer nombreMissionsValidees = (int) missionProjetRepository.countByProjetIdAndStructureIdAndStatut(
                projet.getId(), structureId, StatutMission.VALIDE)
                + (int) missionProjetRepository.countByProjetIdAndStructureIdAndStatut(
                projet.getId(), structureId, StatutMission.VALIDEE);

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
                nombreMissionsValidees
        );
    }

    /**
     * Vérifie que l'utilisateur courant a le droit d'agir sur ce projet :
     *   - un ADMIN_STRUCTURE ou un COACH de la structure active peut agir sur tout projet ;
     *   - un ENTREPRENEUR ne peut agir QUE sur son propre projet.
     * Sans ce contrôle, un entrepreneur qui devine l'UUID d'un projet d'un autre
     * entrepreneur de la même structure pourrait le modifier — c'est le trou de sécurité
     * identifié dans l'audit (section 3 du cahier des charges).
     */
    private void verifierDroitModification(Projet projet, User currentUser, UUID activeStructureId) {
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Utilisateur non authentifié");
        }

        MembreStructure membre = membreStructureRepository
                .findByUserIdAndStructureId(currentUser.getId(), activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Vous n'êtes pas membre de cette structure"));

        boolean estEncadrant = membre.getRole() == RoleMembreStructure.ADMIN_STRUCTURE
                || membre.getRole() == RoleMembreStructure.COACH;
        boolean estProprietaire = projet.getEntrepreneur() != null
                && projet.getEntrepreneur().getId().equals(currentUser.getId());

        if (!estEncadrant && !estProprietaire) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Vous n'êtes pas autorisé à modifier ce projet");
        }
    }

    private void verifierNonArchive(Projet projet) {
        if (projet.isArchive()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce projet est archivé et ne peut plus être modifié");
        }
    }





    @Transactional
public ProjetResponse updateNomProjet(UUID id, String nouveauNom, User currentUser) {
    UUID activeStructureId = getRequiredTenantId();
    Projet projet = projetRepository.findByIdAndStructureId(id, activeStructureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable"));

    verifierDroitModification(projet, currentUser, activeStructureId);
    verifierNonArchive(projet);

    projet.setNom(nouveauNom);
    Projet updated = projetRepository.save(projet);
    return mapToResponse(updated);
}


@Transactional
public ProjetResponse updateProjet(UUID id, UpdateProjetRequest request, User currentUser) {
    UUID activeStructureId = getRequiredTenantId();
    Projet projet = projetRepository.findByIdAndStructureId(id, activeStructureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable"));

    verifierDroitModification(projet, currentUser, activeStructureId);
    verifierNonArchive(projet);

    if (request.nom() != null) projet.setNom(request.nom());
    if (request.description() != null) projet.setDescription(request.description());
    if (request.secteur() != null) projet.setSecteur(request.secteur());

    return mapToResponse(projetRepository.save(projet));
}

/**
 * Archive le projet (soft — les données associées ne sont jamais supprimées).
 * Idempotent : archiver un projet déjà archivé ne fait rien de plus (pas d'erreur),
 * pour que l'action reste sûre à rejouer depuis Angular en cas de double-clic.
 */
@Transactional
public void archiverProjet(UUID id, User currentUser) {
    UUID activeStructureId = getRequiredTenantId();
    Projet projet = projetRepository.findByIdAndStructureId(id, activeStructureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable"));

    verifierDroitModification(projet, currentUser, activeStructureId);

    if (!projet.isArchive()) {
        projet.setArchive(true);
        projet.setDateArchivage(java.time.LocalDateTime.now());
        projetRepository.save(projet);
    }
}

/**
 * Restaure le projet (soft — les données associées ne sont jamais supprimées).
 * Idempotent : restaurer un projet déjà restauré ne fait rien de plus (pas d'erreur),
 * pour que l'action reste sûre à rejouer depuis Angular en cas de double-clic.
 */
@Transactional
public void restaurerProjet(UUID id, User currentUser) {
    UUID activeStructureId = getRequiredTenantId();
    Projet projet = projetRepository.findByIdAndStructureId(id, activeStructureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable"));

    verifierDroitModification(projet, currentUser, activeStructureId);

    if (projet.isArchive()) {
        projet.setArchive(false);
        projet.setDateArchivage(null);
        projetRepository.save(projet);
    }
}


@Transactional
public ProjetResponse promouvoirProjet(UUID projetId, PromouvoirProjetRequest request) {
    UUID activeStructureId = getRequiredTenantId();
    Projet projet = projetRepository.findByIdAndStructureId(projetId, activeStructureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable"));

    // 1. Le projet doit avoir validé toutes les missions de SA cohorte actuelle
    if (projet.getCohorte() != null) {
        List<String> missionsNonValidees = missionProjetRepository
                .findAllByProjetIdAndStructureId(projetId, activeStructureId).stream()
                .filter(mp -> mp.getMissionCohorte().getCohorte() != null
                        && mp.getMissionCohorte().getCohorte().getId().equals(projet.getCohorte().getId()))
                .filter(mp -> mp.getStatut() != StatutMission.VALIDE && mp.getStatut() != StatutMission.VALIDEE)
                .map(mp -> mp.getMissionCohorte().getTitre())
                .toList();

        if (!missionsNonValidees.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Missions non validées avant de pouvoir promouvoir ce projet : "
                            + String.join(", ", missionsNonValidees));
        }
    }

    // 2. Résoudre la cohorte cible (optionnelle) et vérifier la cohérence de phase
    Cohorte nouvelleCohorte = null;
    if (request.nouvelleCohorteId() != null) {
        nouvelleCohorte = cohorteRepository.findByIdAndStructureId(request.nouvelleCohorteId(), activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cohorte cible introuvable"));

        PhaseParcours phaseActuelle = projet.getCohorte() != null ? projet.getCohorte().getPhase() : null;

        if (phaseActuelle != null && nouvelleCohorte.getPhase().ordinal() <= phaseActuelle.ordinal()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La cohorte cible doit être dans une phase ultérieure à la phase actuelle du projet.");
        }
    }

    // 3. Appliquer le transfert
    projet.setCohorte(nouvelleCohorte);
    projet.setStatut(determinerNouveauStatut(nouvelleCohorte, projet.getStatut()));

    return mapToResponse(projetRepository.save(projet));
}

private StatutProjet determinerNouveauStatut(Cohorte nouvelleCohorte, StatutProjet statutActuel) {
    if (nouvelleCohorte == null) {
        return switch (statutActuel) {
            case IDEE -> StatutProjet.EN_INCUBATION;
            case EN_INCUBATION -> StatutProjet.EN_ACCELERATION;
            case EN_ACCELERATION -> StatutProjet.DIPLOME;
            default -> statutActuel;
        };
    }
    return statutPourPhase(nouvelleCohorte.getPhase());
}
}