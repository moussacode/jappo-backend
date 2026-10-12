package sn.jappo.jappo_backend.ressource.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.ia.context.AiResourceContextService;
import sn.jappo.jappo_backend.ia.context.AiResourceContextService.ResourceFile;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.ressource.dto.RessourceResponse;
import sn.jappo.jappo_backend.ressource.entity.Ressource;
import sn.jappo.jappo_backend.ressource.repository.RessourceRepository;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

import java.util.List;
import java.util.UUID;

@Service
public class EntrepreneurRessourceAccessService {

    private static final UUID NO_SCOPE = new UUID(0, 0);

    private final MembreStructureRepository membreStructureRepository;
    private final ProjetRepository projetRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final RessourceRepository ressourceRepository;
    private final RessourceService ressourceService;
    private final AiResourceContextService aiResourceContextService;

    public EntrepreneurRessourceAccessService(
            MembreStructureRepository membreStructureRepository,
            ProjetRepository projetRepository,
            MissionProjetRepository missionProjetRepository,
            RessourceRepository ressourceRepository,
            RessourceService ressourceService,
            AiResourceContextService aiResourceContextService) {
        this.membreStructureRepository = membreStructureRepository;
        this.projetRepository = projetRepository;
        this.missionProjetRepository = missionProjetRepository;
        this.ressourceRepository = ressourceRepository;
        this.ressourceService = ressourceService;
        this.aiResourceContextService = aiResourceContextService;
    }

    @Transactional(readOnly = true)
    public List<RessourceResponse> getAccessibleResources(UUID userId, UUID structureId) {
        return findAccessibleResources(userId, structureId).stream()
                .map(ressourceService::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ResourceFile getAccessibleFile(UUID userId, UUID structureId, UUID resourceId) {
        boolean accessible = findAccessibleResources(userId, structureId).stream()
                .anyMatch(resource -> resource.getId().equals(resourceId));
        if (!accessible) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ressource introuvable");
        }
        return aiResourceContextService.loadAccessibleResourceFile(structureId, resourceId);
    }

    private List<Ressource> findAccessibleResources(UUID userId, UUID structureId) {
        membreStructureRepository
                .findByUserIdAndStructureId(userId, structureId)
                .filter(member -> member.getRole() == RoleMembreStructure.ENTREPRENEUR)
                .filter(member -> member.getStatut() == StatutMembre.ACCEPTE)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.FORBIDDEN, "Accès réservé aux entrepreneurs actifs de cette structure"));

        List<Projet> projects = projetRepository.findAllByEntrepreneurIdAndStructureId(userId, structureId);
        List<UUID> cohortIds = projects.stream()
                .map(Projet::getCohorte)
                .filter(cohort -> isInStructure(cohort, structureId))
                .map(Cohorte::getId)
                .distinct()
                .toList();
        List<UUID> parcoursIds = projects.stream()
                .map(Projet::getCohorte)
                .filter(cohort -> isInStructure(cohort, structureId))
                .map(Cohorte::getParcours)
                .filter(parcours -> parcours != null && structureId.equals(parcours.getStructure().getId()))
                .map(parcours -> parcours.getId())
                .distinct()
                .toList();
        List<UUID> phaseIds = projects.stream()
                .map(Projet::getCohorte)
                .filter(cohort -> isInStructure(cohort, structureId))
                .map(Cohorte::getPhase)
                .filter(phase -> phase != null && structureId.equals(phase.getStructure().getId()))
                .map(phase -> phase.getId())
                .distinct()
                .toList();

        List<UUID> missionResourceIds = projects.stream()
                .flatMap(project -> missionProjetRepository
                        .findAllByProjetIdAndStructureId(project.getId(), structureId).stream())
                .filter(mission -> !mission.isArchive())
                .map(MissionProjet::getMissionCohorte)
                .filter(mission -> mission != null && !mission.isArchive()
                        && mission.getStructure() != null
                        && structureId.equals(mission.getStructure().getId()))
                .flatMap(mission -> mission.getRessources().stream())
                .filter(resource -> !resource.isArchivee()
                        && resource.getStructure() != null
                        && structureId.equals(resource.getStructure().getId()))
                .map(Ressource::getId)
                .distinct()
                .toList();

        return ressourceRepository.findAccessiblesEntrepreneur(
                structureId,
                orPlaceholder(cohortIds),
                orPlaceholder(parcoursIds),
                orPlaceholder(phaseIds),
                orPlaceholder(missionResourceIds));
    }

    private boolean isInStructure(Cohorte cohort, UUID structureId) {
        return cohort != null && cohort.getStructure() != null
                && structureId.equals(cohort.getStructure().getId());
    }

    private List<UUID> orPlaceholder(List<UUID> ids) {
        return ids.isEmpty() ? List.of(NO_SCOPE) : ids;
    }
}
