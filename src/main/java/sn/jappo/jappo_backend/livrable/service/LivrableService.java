package sn.jappo.jappo_backend.livrable.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.livrable.dto.CreateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.EvaluateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.LivrableResponse;
import sn.jappo.jappo_backend.livrable.entity.Livrable;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.mission.dto.UpdateStatutMissionRequest;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.mission.service.MissionService;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

@Service
public class LivrableService {

    private final LivrableRepository livrableRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final StructureRepository structureRepository;
    private final MissionService missionService;

    public LivrableService(
            LivrableRepository livrableRepository,
            MissionProjetRepository missionProjetRepository,
            StructureRepository structureRepository,
            MissionService missionService
    ) {
        this.livrableRepository = livrableRepository;
        this.missionProjetRepository = missionProjetRepository;
        this.structureRepository = structureRepository;
        this.missionService = missionService;
    }

    @Transactional
    public LivrableResponse createLivrable(CreateLivrableRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Structure structure = structureRepository.findById(activeStructureId)
                .orElseThrow(() -> new RuntimeException("Structure introuvable"));

        MissionProjet missionProjet = missionProjetRepository.findByIdAndStructureId(request.missionProjetId(), activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission projet introuvable"));

        Livrable livrable = new Livrable();
        livrable.setNom(request.nom());
        livrable.setUrl(request.url());
        livrable.setTypePiece(request.typePiece() != null ? request.typePiece() : TypeLivrable.FICHIER);
        livrable.setStatut(StatutLivrable.EN_ATTENTE);
        livrable.setMissionProjet(missionProjet);
        livrable.setProjet(missionProjet.getProjet());
        livrable.setStructure(structure);

        Livrable saved = livrableRepository.save(livrable);

        // Passer automatiquement la mission au statut SOUMIS lors du dépôt du livrable
        if (missionProjet.getStatut() == StatutMission.A_FAIRE || missionProjet.getStatut() == StatutMission.A_REVOIR) {
            missionService.updateStatut(missionProjet.getId(), new UpdateStatutMissionRequest(StatutMission.SOUMIS));
        }

        return mapToResponse(saved);
    }

    @Transactional
    public LivrableResponse evaluateLivrable(UUID id, EvaluateLivrableRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Livrable livrable = livrableRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Livrable introuvable"));

        livrable.setStatut(request.statut());
        livrable.setNote(request.note());
        livrable.setCommentaireCoach(request.commentaireCoach());

        Livrable saved = livrableRepository.save(livrable);

        // Synchroniser le statut de la mission avec l'évaluation du coach
        if (request.statut() == StatutLivrable.VALIDE) {
            missionService.updateStatut(livrable.getMissionProjet().getId(), new UpdateStatutMissionRequest(StatutMission.VALIDE));
        } else if (request.statut() == StatutLivrable.A_CORRIGER) {
            missionService.updateStatut(livrable.getMissionProjet().getId(), new UpdateStatutMissionRequest(StatutMission.A_REVOIR));
        }

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<LivrableResponse> getLivrablesByMission(UUID missionProjetId) {
        UUID activeStructureId = getRequiredTenantId();
        return livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjetId, activeStructureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LivrableResponse> getLivrablesByProjet(UUID projetId) {
        UUID activeStructureId = getRequiredTenantId();
        return livrableRepository.findAllByProjetIdAndStructureId(projetId, activeStructureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Aucune structure active sélectionnée (en-tête X-Structure-Id manquant)");
        }
        return tenantId;
    }

    private LivrableResponse mapToResponse(Livrable livrable) {
        return new LivrableResponse(
                livrable.getId(),
                livrable.getNom(),
                livrable.getUrl(),
                livrable.getTypePiece(),
                livrable.getStatut(),
                livrable.getNote(),
                livrable.getCommentaireCoach(),
                livrable.getDateDepot(),
                livrable.getMissionProjet().getId(),
                livrable.getMissionProjet().getMissionCohorte().getTitre(),
                livrable.getProjet().getId(),
                livrable.getProjet().getNom(),
                livrable.getStructure().getId()
        );
    }
}