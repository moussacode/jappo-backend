package sn.jappo.jappo_backend.mission.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.mission.dto.CreateMissionModeleRequest;
import sn.jappo.jappo_backend.mission.dto.MissionModeleResponse;
import sn.jappo.jappo_backend.mission.entity.MissionModele;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;
import sn.jappo.jappo_backend.mission.repository.MissionModeleRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

@Service
public class MissionModeleService {

    private final MissionModeleRepository missionModeleRepository;
    private final StructureRepository structureRepository;

    public MissionModeleService(
            MissionModeleRepository missionModeleRepository,
            StructureRepository structureRepository
    ) {
        this.missionModeleRepository = missionModeleRepository;
        this.structureRepository = structureRepository;
    }

    @Transactional(readOnly = true)
    public List<MissionModeleResponse> getMissionsModelesForActiveStructure() {
        UUID activeStructureId = getRequiredTenantId();
        return missionModeleRepository.findAllByStructureIdOrderByDateCreationDesc(activeStructureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public MissionModeleResponse createMissionModele(CreateMissionModeleRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Structure structure = structureRepository.findById(activeStructureId)
                .orElseThrow(() -> new RuntimeException("Structure introuvable pour l'ID: " + activeStructureId));

        MissionModele modele = new MissionModele();
        modele.setTitre(request.titre().trim());
        modele.setDescription(request.description() != null ? request.description().trim() : null);
        modele.setPrioriteParDefaut(request.prioriteParDefaut() != null ? request.prioriteParDefaut() : PrioriteMission.MOYENNE);
        modele.setStructure(structure);

        MissionModele saved = missionModeleRepository.save(modele);
        return mapToResponse(saved);
    }

    public MissionModeleResponse mapToResponse(MissionModele m) {
        return new MissionModeleResponse(
                m.getId(),
                m.getTitre(),
                m.getDescription(),
                m.getPrioriteParDefaut(),
                m.getStructure() != null ? m.getStructure().getId() : null,
                m.getDateCreation()
        );
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Aucune structure active sélectionnée (en-tête X-Structure-Id manquant)");
        }
        return tenantId;
    }
}
