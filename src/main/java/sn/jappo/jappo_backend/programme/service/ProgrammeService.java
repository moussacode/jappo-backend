package sn.jappo.jappo_backend.programme.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.programme.dto.CreateProgrammeRequest;
import sn.jappo.jappo_backend.programme.dto.ProgrammeDTO;
import sn.jappo.jappo_backend.programme.dto.UpdateProgrammeRequest;
import sn.jappo.jappo_backend.programme.entity.Programme;
import sn.jappo.jappo_backend.programme.exception.DuplicateProgrammeNameException;
import sn.jappo.jappo_backend.programme.exception.ProgrammeNotFoundException;
import sn.jappo.jappo_backend.programme.repository.ProgrammeRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

@Service
@RequiredArgsConstructor
public class ProgrammeService {

    private final ProgrammeRepository programmeRepository;
    private final StructureRepository structureRepository;

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Tenant non défini");
        }
        return tenantId;
    }

    private void validateNom(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            throw new IllegalArgumentException("Le nom du programme est obligatoire");
        }
        if (nom.length() > 150) {
            throw new IllegalArgumentException("Le nom du programme ne peut pas dépasser 150 caractères");
        }
    }

    @Transactional(readOnly = true)
    public List<ProgrammeDTO> getAllProgrammes() {
        UUID structureId = getRequiredTenantId();
        return programmeRepository.findAllByStructureId(structureId)
                .stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ProgrammeDTO getProgrammeById(UUID id) {
        UUID structureId = getRequiredTenantId();
        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new ProgrammeNotFoundException("Programme introuvable"));
        
        if (!programme.getStructure().getId().equals(structureId)) {
            throw new ProgrammeNotFoundException("Programme introuvable dans votre structure");
        }
        
        return mapToDTO(programme);
    }

    @Transactional
    public ProgrammeDTO createProgramme(CreateProgrammeRequest request) {
        UUID structureId = getRequiredTenantId();
        
        validateNom(request.nom());
        
        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalStateException("Structure introuvable"));
        
        // Vérifier l'unicité du nom dans la structure
        if (programmeRepository.existsByStructureIdAndNom(structureId, request.nom().trim())) {
            throw new DuplicateProgrammeNameException("Un programme avec ce nom existe déjà");
        }
        
        Programme programme = new Programme();
        programme.setNom(request.nom().trim());
        programme.setDescription(request.description());
        programme.setStructure(structure);
        programme.setArchive(false);
        
        Programme saved = programmeRepository.save(programme);
        return mapToDTO(saved);
    }

    @Transactional
    public ProgrammeDTO updateProgramme(UUID id, UpdateProgrammeRequest request) {
        UUID structureId = getRequiredTenantId();
        
        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new ProgrammeNotFoundException("Programme introuvable"));
        
        if (!programme.getStructure().getId().equals(structureId)) {
            throw new ProgrammeNotFoundException("Programme introuvable dans votre structure");
        }
        
        if (request.nom() != null) {
            validateNom(request.nom());
            if (!request.nom().trim().equals(programme.getNom())) {
                if (programmeRepository.existsByStructureIdAndNom(structureId, request.nom().trim())) {
                    throw new DuplicateProgrammeNameException("Un programme avec ce nom existe déjà");
                }
                programme.setNom(request.nom().trim());
            }
        }
        
        if (request.description() != null) {
            programme.setDescription(request.description());
        }
        
        if (request.archive() != null) {
            programme.setArchive(request.archive());
        }
        
        Programme saved = programmeRepository.save(programme);
        return mapToDTO(saved);
    }

    @Transactional
    public void deleteProgramme(UUID id) {
        UUID structureId = getRequiredTenantId();

        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new ProgrammeNotFoundException("Programme introuvable"));

        if (!programme.getStructure().getId().equals(structureId)) {
            throw new ProgrammeNotFoundException("Programme introuvable dans votre structure");
        }

        if (!programme.getParcours().isEmpty()) {
            throw new IllegalStateException("Impossible de supprimer un programme qui contient des parcours");
        }

        programmeRepository.delete(programme);
    }

    @Transactional
    public void archiverProgramme(UUID id) {
        UUID structureId = getRequiredTenantId();

        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new ProgrammeNotFoundException("Programme introuvable"));

        if (!programme.getStructure().getId().equals(structureId)) {
            throw new ProgrammeNotFoundException("Programme introuvable dans votre structure");
        }

        programme.setArchive(true);
        programmeRepository.save(programme);
    }

    @Transactional
    public void restaurerProgramme(UUID id) {
        UUID structureId = getRequiredTenantId();

        Programme programme = programmeRepository.findById(id)
                .orElseThrow(() -> new ProgrammeNotFoundException("Programme introuvable"));

        if (!programme.getStructure().getId().equals(structureId)) {
            throw new ProgrammeNotFoundException("Programme introuvable dans votre structure");
        }

        programme.setArchive(false);
        programmeRepository.save(programme);
    }

    private ProgrammeDTO mapToDTO(Programme programme) {
        List<UUID> parcoursIds = programme.getParcours() != null
                ? programme.getParcours().stream().map(p -> p.getId()).collect(Collectors.toList())
                : List.of();
        
        return new ProgrammeDTO(
                programme.getId(),
                programme.getNom(),
                programme.getDescription(),
                programme.isArchive(),
                programme.getStructure().getId(),
                programme.getDateCreation(),
                programme.getDateModification(),
                parcoursIds
        );
    }
}
