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
    @Transactional(readOnly = true)
    public List<ProjetResponse> getProjetsForActiveStructure() {
        UUID activeStructureId = getRequiredTenantId();
        return projetRepository.findAllByStructureId(activeStructureId)
                .stream()
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
                projet.getDateCreation()
        );
    }





    @Transactional
public ProjetResponse updateNomProjet(UUID id, String nouveauNom) {
    UUID activeStructureId = getRequiredTenantId();
    Projet projet = projetRepository.findByIdAndStructureId(id, activeStructureId)
            .orElseThrow(() -> new RuntimeException("Projet introuvable"));

    projet.setNom(nouveauNom);
    Projet updated = projetRepository.save(projet);
    return mapToResponse(updated);
}


@Transactional
public ProjetResponse updateProjet(UUID id, UpdateProjetRequest request) {
    UUID activeStructureId = getRequiredTenantId();
    Projet projet = projetRepository.findByIdAndStructureId(id, activeStructureId)
            .orElseThrow(() -> new RuntimeException("Projet introuvable"));

    if (request.nom() != null) projet.setNom(request.nom());
    if (request.description() != null) projet.setDescription(request.description());
    if (request.secteur() != null) projet.setSecteur(request.secteur());

    return mapToResponse(projetRepository.save(projet));
}

@Transactional
public void archiverProjet(UUID id) {
    UUID activeStructureId = getRequiredTenantId();
    Projet projet = projetRepository.findByIdAndStructureId(id, activeStructureId)
            .orElseThrow(() -> new RuntimeException("Projet introuvable"));
    projet.setStatut(StatutProjet.ABANDONNE);
    projetRepository.save(projet);
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