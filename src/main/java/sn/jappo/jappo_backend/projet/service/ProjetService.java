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
import sn.jappo.jappo_backend.projet.dto.CreateProjetRequest;
import sn.jappo.jappo_backend.projet.dto.ProjetResponse;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@Service
public class ProjetService {

    private final ProjetRepository projetRepository;
    private final StructureRepository structureRepository;
    private final CohorteRepository cohorteRepository;
    private final UserRepository userRepository;

    public ProjetService(
            ProjetRepository projetRepository,
            StructureRepository structureRepository,
            CohorteRepository cohorteRepository,
            UserRepository userRepository
    ) {
        this.projetRepository = projetRepository;
        this.structureRepository = structureRepository;
        this.cohorteRepository = cohorteRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public ProjetResponse createProjet(CreateProjetRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Structure structure = structureRepository.findById(activeStructureId)
                .orElseThrow(() -> new RuntimeException("Structure introuvable pour l'ID: " + activeStructureId));

        Projet projet = new Projet();
        projet.setNom(request.nom());
        projet.setDescription(request.description());
        projet.setSecteur(request.secteur());
        projet.setStatut(StatutProjet.EN_INCUBATION);
        projet.setScoreMaturite(0);
        projet.setStructure(structure);

        if (request.cohorteId() != null) {
            Cohorte cohorte = cohorteRepository.findByIdAndStructureId(request.cohorteId(), activeStructureId)
                    .orElseThrow(() -> new RuntimeException("Cohorte introuvable pour cet incubateur"));
            projet.setCohorte(cohorte);
        }

        if (request.entrepreneurId() != null) {
            User entrepreneur = userRepository.findById(request.entrepreneurId())
                    .orElseThrow(() -> new RuntimeException("Utilisateur introuvable"));
            projet.setEntrepreneur(entrepreneur);
        }

        Projet saved = projetRepository.save(projet);
        return mapToResponse(saved);
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

    // 🟢 AJOUT : Récupération du projet d'un entrepreneur pour la structure active
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
}