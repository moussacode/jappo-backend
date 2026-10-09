package sn.jappo.jappo_backend.parcours.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.parcours.dto.CreateEtapeRequest;
import sn.jappo.jappo_backend.parcours.dto.EtapeDTO;
import sn.jappo.jappo_backend.parcours.dto.UpdateEtapeRequest;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.exception.EtapeNotFoundException;
import sn.jappo.jappo_backend.parcours.exception.InvalidEtapeOrderException;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;

@Service
@RequiredArgsConstructor
public class EtapeService {

    private final EtapeRepository etapeRepository;
    private final PhaseRepository phaseRepository;

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant non défini");
        }
        return tenantId;
    }

    private void validateNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom de l'étape est obligatoire");
        }
        if (nom.length() > 150) {
            throw new IllegalArgumentException("Le nom de l'étape ne peut pas dépasser 150 caractères");
        }
    }

    private void validateOrdre(Integer ordre) {
        if (ordre == null || ordre < 1) {
            throw new IllegalArgumentException("L'ordre doit être un entier positif");
        }
    }

    @Transactional(readOnly = true)
    public List<EtapeDTO> getAllEtapes() {
        UUID structureId = getRequiredTenantId();
        return etapeRepository.findByStructureId(structureId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<EtapeDTO> getEtapesByPhaseId(UUID phaseId) {
        UUID structureId = getRequiredTenantId();
        
        Phase phase = phaseRepository.findById(phaseId)
                .orElseThrow(() -> new IllegalArgumentException("Phase introuvable"));
        
        if (!phase.getStructure().getId().equals(structureId)) {
            throw new IllegalArgumentException("Phase introuvable dans votre structure");
        }
        
        return etapeRepository.findAllByPhaseIdOrderByOrdreAsc(phaseId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public EtapeDTO getEtapeById(UUID id) {
        UUID structureId = getRequiredTenantId();
        
        Etape etape = etapeRepository.findById(id)
                .orElseThrow(() -> new EtapeNotFoundException("Étape introuvable"));
        
        if (!etape.getStructure().getId().equals(structureId)) {
            throw new EtapeNotFoundException("Étape introuvable dans votre structure");
        }
        
        return mapToDTO(etape);
    }

    @Transactional
    public EtapeDTO createEtape(CreateEtapeRequest request) {
        UUID structureId = getRequiredTenantId();
        
        validateNom(request.nom());
        validateOrdre(request.ordre());
        
        Phase phase = phaseRepository.findById(request.phaseId())
                .orElseThrow(() -> new IllegalArgumentException("Phase introuvable"));
        
        if (!phase.getStructure().getId().equals(structureId)) {
            throw new IllegalArgumentException("Phase introuvable dans votre structure");
        }
        
        // Vérifier que l'ordre est unique dans la phase
        if (etapeRepository.existsByPhaseIdAndOrdre(request.phaseId(), request.ordre())) {
            throw new InvalidEtapeOrderException("Une étape avec cet ordre existe déjà dans cette phase");
        }
        
        Etape etape = new Etape();
        etape.setNom(request.nom().trim());
        etape.setDescription(request.description());
        etape.setOrdre(request.ordre());
        etape.setObligatoire(request.obligatoire() != null ? request.obligatoire() : true);
        etape.setArchive(false);
        etape.setPhase(phase);
        etape.setStructure(phase.getStructure());
        
        Etape saved = etapeRepository.save(etape);
        return mapToDTO(saved);
    }

    @Transactional
    public EtapeDTO updateEtape(UUID id, UpdateEtapeRequest request) {
        UUID structureId = getRequiredTenantId();
        
        Etape etape = etapeRepository.findById(id)
                .orElseThrow(() -> new EtapeNotFoundException("Étape introuvable"));
        
        if (!etape.getStructure().getId().equals(structureId)) {
            throw new EtapeNotFoundException("Étape introuvable dans votre structure");
        }
        
        if (request.nom() != null) {
            validateNom(request.nom());
            etape.setNom(request.nom().trim());
        }
        
        if (request.description() != null) {
            etape.setDescription(request.description());
        }
        
        if (request.ordre() != null) {
            validateOrdre(request.ordre());
            if (!request.ordre().equals(etape.getOrdre())) {
                if (etapeRepository.existsByPhaseIdAndOrdre(etape.getPhase().getId(), request.ordre())) {
                    throw new InvalidEtapeOrderException("Une étape avec cet ordre existe déjà dans cette phase");
                }
                etape.setOrdre(request.ordre());
            }
        }
        
        if (request.obligatoire() != null) {
            etape.setObligatoire(request.obligatoire());
        }
        
        if (request.archive() != null) {
            etape.setArchive(request.archive());
        }
        
        Etape saved = etapeRepository.save(etape);
        return mapToDTO(saved);
    }

    @Transactional
    public void deleteEtape(UUID id) {
        UUID structureId = getRequiredTenantId();

        Etape etape = etapeRepository.findById(id)
                .orElseThrow(() -> new EtapeNotFoundException("Étape introuvable"));

        if (!etape.getStructure().getId().equals(structureId)) {
            throw new EtapeNotFoundException("Étape introuvable dans votre structure");
        }

        if (etape.isObligatoire() && !etape.isArchive()) {
            throw new IllegalStateException("Impossible de supprimer une étape obligatoire active");
        }

        etapeRepository.delete(etape);
    }

    @Transactional
    public void archiverEtape(UUID id) {
        UUID structureId = getRequiredTenantId();

        Etape etape = etapeRepository.findById(id)
                .orElseThrow(() -> new EtapeNotFoundException("Étape introuvable"));

        if (!etape.getStructure().getId().equals(structureId)) {
            throw new EtapeNotFoundException("Étape introuvable dans votre structure");
        }

        etape.setArchive(true);
        etapeRepository.save(etape);
    }

    @Transactional
    public void restaurerEtape(UUID id) {
        UUID structureId = getRequiredTenantId();

        Etape etape = etapeRepository.findById(id)
                .orElseThrow(() -> new EtapeNotFoundException("Étape introuvable"));

        if (!etape.getStructure().getId().equals(structureId)) {
            throw new EtapeNotFoundException("Étape introuvable dans votre structure");
        }

        etape.setArchive(false);
        etapeRepository.save(etape);
    }

    private EtapeDTO mapToDTO(Etape etape) {
        List<UUID> missionIds = etape.getMissions() != null
                ? etape.getMissions().stream().map(m -> m.getId()).collect(Collectors.toList())
                : List.of();
        
        return new EtapeDTO(
                etape.getId(),
                etape.getNom(),
                etape.getDescription(),
                etape.getOrdre(),
                etape.isObligatoire(),
                etape.isArchive(),
                etape.getPhase().getId(),
                etape.getStructure().getId(),
                etape.getDateCreation(),
                etape.getDateModification(),
                missionIds
        );
    }
}
