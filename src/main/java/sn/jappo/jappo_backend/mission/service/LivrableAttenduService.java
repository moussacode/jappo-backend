package sn.jappo.jappo_backend.mission.service;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;
import sn.jappo.jappo_backend.mission.dto.CreateLivrableAttenduRequest;
import sn.jappo.jappo_backend.mission.dto.LivrableAttenduResponse;
import sn.jappo.jappo_backend.mission.dto.TemplateField;
import sn.jappo.jappo_backend.mission.dto.TemplateFieldCodec;
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
        List<TemplateField> templateFields = normalizeTemplateFields(
                request.templateFields(), type, templateType);

        LivrableAttendu livrableAttendu = new LivrableAttendu();
        livrableAttendu.setTitre(request.titre().trim());
        livrableAttendu.setDescription(request.description() != null && !request.description().isBlank() ? request.description().trim() : null);
        livrableAttendu.setOrdre(ordre);
        livrableAttendu.setObligatoire(request.obligatoire() != null ? request.obligatoire() : true);
        livrableAttendu.setType(type);
        livrableAttendu.setTemplateType(templateType);
        livrableAttendu.setTemplateConfiguration(TemplateFieldCodec.encode(templateFields));
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

        TypeLivrable ancienType = livrableAttendu.getType();
        String ancienTemplateType = livrableAttendu.getTemplateType();

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

        if (request.templateType() != null) {
            livrableAttendu.setTemplateType(normalizeTemplateType(request.templateType(), livrableAttendu.getType()));
        }

        if (livrableAttendu.getType() != TypeLivrable.TEMPLATE) {
            livrableAttendu.setTemplateType(null);
            livrableAttendu.setTemplateConfiguration(null);
        } else if (request.templateFields() != null) {
            List<TemplateField> templateFields = normalizeTemplateFields(
                    request.templateFields(), livrableAttendu.getType(), livrableAttendu.getTemplateType());
            livrableAttendu.setTemplateConfiguration(TemplateFieldCodec.encode(templateFields));
        } else if (ancienType != livrableAttendu.getType()
                || !java.util.Objects.equals(ancienTemplateType, livrableAttendu.getTemplateType())) {
            livrableAttendu.setTemplateConfiguration(null);
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
        String normalized = templateType.trim();
        return normalized.equalsIgnoreCase("BMC") ? "BMC" : normalized;
    }

    private List<TemplateField> normalizeTemplateFields(
            List<TemplateField> fields,
            TypeLivrable type,
            String templateType
    ) {
        if (fields == null) {
            return "BMC".equalsIgnoreCase(templateType)
                    ? TemplateFieldCodec.defaultBmcFields()
                    : List.of();
        }
        if (type != TypeLivrable.TEMPLATE || templateType == null) {
            throw new IllegalArgumentException("Les champs ne peuvent être configurés que pour un template nommé.");
        }
        if (fields.isEmpty()) {
            if (templateType.equalsIgnoreCase("BMC")) {
                throw new IllegalArgumentException("Le template BMC doit contenir au moins une rubrique.");
            }
            return List.of();
        }
        if (!templateType.equalsIgnoreCase("BMC")) {
            throw new IllegalArgumentException("La configuration interactive disponible actuellement est le template BMC.");
        }
        if (fields.size() > 40) {
            throw new IllegalArgumentException("Un template ne peut pas contenir plus de 40 champs.");
        }

        Set<String> ids = new HashSet<>();
        for (TemplateField field : fields) {
            if (field == null || field.id() == null || field.id().isBlank()
                    || field.id().trim().length() > 80
                    || field.label() == null || field.label().isBlank()
                    || field.label().trim().length() > 120
                    || !ids.add(field.id().trim())) {
                throw new IllegalArgumentException("Chaque champ du template doit avoir un identifiant unique et un libellé de 1 à 120 caractères.");
            }
        }
        return fields.stream()
                .map(field -> new TemplateField(field.id().trim(), field.label().trim()))
                .toList();
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
                TemplateFieldCodec.decode(
                        livrableAttendu.getTemplateConfiguration(),
                        livrableAttendu.getTemplateType()),
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
