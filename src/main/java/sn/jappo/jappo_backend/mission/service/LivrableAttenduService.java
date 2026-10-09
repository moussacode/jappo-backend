package sn.jappo.jappo_backend.mission.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;
import sn.jappo.jappo_backend.mission.dto.CreateLivrableAttenduRequest;
import sn.jappo.jappo_backend.mission.dto.LivrableAttenduResponse;
import sn.jappo.jappo_backend.mission.dto.UpdateLivrableAttenduRequest;
import sn.jappo.jappo_backend.mission.entity.LivrableAttendu;
import sn.jappo.jappo_backend.mission.entity.MissionEtape;
import sn.jappo.jappo_backend.mission.repository.LivrableAttenduRepository;
import sn.jappo.jappo_backend.mission.repository.MissionEtapeRepository;

@Service
public class LivrableAttenduService {

    private final LivrableAttenduRepository livrableAttenduRepository;
    private final MissionEtapeRepository missionEtapeRepository;

    public LivrableAttenduService(
            LivrableAttenduRepository livrableAttenduRepository,
            MissionEtapeRepository missionEtapeRepository
    ) {
        this.livrableAttenduRepository = livrableAttenduRepository;
        this.missionEtapeRepository = missionEtapeRepository;
    }

    @Transactional(readOnly = true)
    public List<LivrableAttenduResponse> getLivrablesByMissionEtape(UUID etapeId, UUID missionId) {
        UUID structureId = getRequiredTenantId();
        loadMissionForStructure(missionId, etapeId, structureId);
        return livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(missionId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public LivrableAttenduResponse getLivrableAttenduById(UUID etapeId, UUID missionId, UUID livrableId) {
        UUID structureId = getRequiredTenantId();
        MissionEtape missionEtape = loadMissionForStructure(missionId, etapeId, structureId);
        LivrableAttendu livrableAttendu = livrableAttenduRepository.findById(livrableId)
                .orElseThrow(() -> new IllegalArgumentException("Livrable attendu introuvable"));

        if (!missionEtape.getId().equals(livrableAttendu.getMissionEtape() != null ? livrableAttendu.getMissionEtape().getId() : null)) {
            throw new IllegalArgumentException("Ce livrable attendu n'appartient pas à cette mission");
        }

        return mapToResponse(livrableAttendu);
    }

    @Transactional
    public LivrableAttenduResponse createLivrableAttendu(
            UUID etapeId,
            UUID missionId,
            CreateLivrableAttenduRequest request
    ) {
        UUID structureId = getRequiredTenantId();
        MissionEtape missionEtape = loadMissionForStructure(missionId, etapeId, structureId);

        validateTitre(request.titre());
        TypeLivrable type = request.type() != null ? request.type() : TypeLivrable.FICHIER;
        Integer ordre = request.ordre() != null ? request.ordre() : nextOrdre(missionEtape);
        validateOrdre(ordre);

        String templateType = normalizeTemplateType(request.templateType(), type);

        LivrableAttendu livrableAttendu = new LivrableAttendu();
        livrableAttendu.setTitre(request.titre().trim());
        livrableAttendu.setDescription(request.description() != null && !request.description().isBlank() ? request.description().trim() : null);
        livrableAttendu.setOrdre(ordre);
        livrableAttendu.setObligatoire(request.obligatoire() != null ? request.obligatoire() : true);
        livrableAttendu.setType(type);
        livrableAttendu.setTemplateType(templateType);
        missionEtape.addLivrableAttendu(livrableAttendu);

        return mapToResponse(livrableAttenduRepository.save(livrableAttendu));
    }

    @Transactional
    public LivrableAttenduResponse updateLivrableAttendu(
            UUID etapeId,
            UUID missionId,
            UUID livrableId,
            UpdateLivrableAttenduRequest request
    ) {
        UUID structureId = getRequiredTenantId();
        MissionEtape missionEtape = loadMissionForStructure(missionId, etapeId, structureId);

        LivrableAttendu livrableAttendu = livrableAttenduRepository.findById(livrableId)
                .orElseThrow(() -> new IllegalArgumentException("Livrable attendu introuvable"));

        if (livrableAttendu.getMissionEtape() == null || !missionEtape.getId().equals(livrableAttendu.getMissionEtape().getId())) {
            throw new IllegalArgumentException("Ce livrable attendu n'appartient pas à cette mission");
        }

        if (request.titre() != null) {
            validateTitre(request.titre());
            livrableAttendu.setTitre(request.titre().trim());
        }

        if (request.description() != null) {
            livrableAttendu.setDescription(request.description().isBlank() ? null : request.description().trim());
        }

        if (request.ordre() != null) {
            validateOrdre(request.ordre());
            livrableAttendu.setOrdre(request.ordre());
        }

        if (request.obligatoire() != null) {
            livrableAttendu.setObligatoire(request.obligatoire());
        }

        if (request.type() != null) {
            livrableAttendu.setType(request.type());
        }

        if (request.templateType() != null || livrableAttendu.getType() == TypeLivrable.TEMPLATE) {
            livrableAttendu.setTemplateType(normalizeTemplateType(request.templateType(), livrableAttendu.getType()));
        }

        if (livrableAttendu.getType() != TypeLivrable.TEMPLATE) {
            livrableAttendu.setTemplateType(null);
        }

        return mapToResponse(livrableAttenduRepository.save(livrableAttendu));
    }

    @Transactional
    public void deleteLivrableAttendu(UUID etapeId, UUID missionId, UUID livrableId) {
        UUID structureId = getRequiredTenantId();
        MissionEtape missionEtape = loadMissionForStructure(missionId, etapeId, structureId);

        LivrableAttendu livrableAttendu = livrableAttenduRepository.findById(livrableId)
                .orElseThrow(() -> new IllegalArgumentException("Livrable attendu introuvable"));

        if (livrableAttendu.getMissionEtape() == null || !missionEtape.getId().equals(livrableAttendu.getMissionEtape().getId())) {
            throw new IllegalArgumentException("Ce livrable attendu n'appartient pas à cette mission");
        }

        missionEtape.removeLivrableAttendu(livrableAttendu);
        livrableAttenduRepository.delete(livrableAttendu);
    }

    private MissionEtape loadMissionForStructure(UUID missionId, UUID etapeId, UUID structureId) {
        return missionEtapeRepository.findByIdAndEtapeIdAndStructureId(missionId, etapeId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Mission introuvable pour cette étape et cette structure"));
    }

    private Integer nextOrdre(MissionEtape missionEtape) {
        return livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(missionEtape.getId()).stream()
                .map(LivrableAttendu::getOrdre)
                .filter(java.util.Objects::nonNull)
                .max(Integer::compareTo)
                .map(v -> v + 1)
                .orElse(1);
    }

    private void validateTitre(String titre) {
        if (titre == null || titre.isBlank()) {
            throw new IllegalArgumentException("Le titre du livrable attendu est obligatoire");
        }
        if (titre.trim().length() > 150) {
            throw new IllegalArgumentException("Le titre du livrable attendu ne peut pas dépasser 150 caractères");
        }
    }

    private void validateOrdre(Integer ordre) {
        if (ordre == null || ordre < 1) {
            throw new IllegalArgumentException("L'ordre doit être un entier supérieur ou égal à 1");
        }
    }

    private String normalizeTemplateType(String templateType, TypeLivrable type) {
        if (type != TypeLivrable.TEMPLATE) {
            return null;
        }
        if (templateType == null || templateType.isBlank()) {
            return null;
        }
        return templateType.trim();
    }

    private LivrableAttenduResponse mapToResponse(LivrableAttendu livrableAttendu) {
        UUID missionEtapeId = livrableAttendu.getMissionEtape() != null ? livrableAttendu.getMissionEtape().getId() : null;
        UUID etapeId = livrableAttendu.getMissionEtape() != null && livrableAttendu.getMissionEtape().getEtape() != null
                ? livrableAttendu.getMissionEtape().getEtape().getId()
                : null;
        UUID structureId = livrableAttendu.getMissionEtape() != null && livrableAttendu.getMissionEtape().getStructure() != null
                ? livrableAttendu.getMissionEtape().getStructure().getId()
                : null;

        return new LivrableAttenduResponse(
                livrableAttendu.getId(),
                livrableAttendu.getTitre(),
                livrableAttendu.getDescription(),
                livrableAttendu.getOrdre(),
                livrableAttendu.isObligatoire(),
                livrableAttendu.getType(),
                livrableAttendu.getTemplateType(),
                missionEtapeId,
                etapeId,
                structureId,
                livrableAttendu.getDateCreation() != null ? livrableAttendu.getDateCreation() : LocalDateTime.now()
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
