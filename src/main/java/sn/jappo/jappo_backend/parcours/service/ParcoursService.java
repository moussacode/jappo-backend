package sn.jappo.jappo_backend.parcours.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.parcours.dto.CreateParcoursRequest;
import sn.jappo.jappo_backend.parcours.dto.ParcoursResponse;
import sn.jappo.jappo_backend.parcours.dto.PhaseResponse;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

@Service
@RequiredArgsConstructor
public class ParcoursService {

    private final ParcoursRepository parcoursRepository;
    private final StructureRepository structureRepository;

    // -----------------------------------------------------------------------
    // LECTURE
    // -----------------------------------------------------------------------

    /**
     * Récupérer tous les parcours de la structure courante.
     */
    @Transactional(readOnly = true)
    public List<ParcoursResponse> getAllParcours() {

        UUID structureId = getRequiredTenantId();

        return parcoursRepository
                .findAllByStructureId(structureId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer un parcours par son ID.
     */
    @Transactional(readOnly = true)
    public ParcoursResponse getParcoursById(UUID id) {

        UUID structureId = getRequiredTenantId();

        Parcours parcours = parcoursRepository
                .findByIdAndStructureId(id, structureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parcours introuvable"
                ));

        return mapToResponse(parcours);
    }

    // -----------------------------------------------------------------------
    // CRÉATION
    // -----------------------------------------------------------------------

    /**
     * Créer un parcours.
     *
     * Les phases sont sélectionnées séparément.
     */
    @Transactional
    public ParcoursResponse createParcours(
            CreateParcoursRequest request
    ) {

        UUID structureId = getRequiredTenantId();

        Structure structure = structureRepository
                .findById(structureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Structure introuvable"
                ));

        validateNom(request.getNom());

        Parcours parcours = new Parcours();

        parcours.setNom(request.getNom().trim());
        parcours.setDescription(request.getDescription());
        parcours.setStructure(structure);
        parcours.setArchive(false);

        Parcours saved = parcoursRepository.save(parcours);

        return mapToResponse(saved);
    }

    // -----------------------------------------------------------------------
    // MODIFICATION
    // -----------------------------------------------------------------------

    /**
     * Modifier un parcours.
     *
     * Les phases ne sont pas modifiées ici.
     */
    @Transactional
    public ParcoursResponse updateParcours(
            UUID id,
            CreateParcoursRequest request
    ) {

        UUID structureId = getRequiredTenantId();

        Parcours parcours = parcoursRepository
                .findByIdAndStructureId(id, structureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parcours introuvable"
                ));

        if (request.getNom() != null) {

            validateNom(request.getNom());

            parcours.setNom(request.getNom().trim());
        }

        if (request.getDescription() != null) {

            parcours.setDescription(
                    request.getDescription()
            );
        }

        /*
         * IMPORTANT :
         *
         * Les phases du parcours sont gérées via ParcoursPhase.
         * On ne les modifie jamais ici.
         */

        return mapToResponse(
                parcoursRepository.save(parcours)
        );
    }

    // -----------------------------------------------------------------------
    // ARCHIVAGE
    // -----------------------------------------------------------------------

    /**
     * Archiver un parcours.
     */
    @Transactional
    public void archiverParcours(UUID id) {

        UUID structureId = getRequiredTenantId();

        Parcours parcours = parcoursRepository
                .findByIdAndStructureId(id, structureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parcours introuvable"
                ));

        parcours.setArchive(true);

        parcoursRepository.save(parcours);
    }

    // -----------------------------------------------------------------------
    // MAPPING
    // -----------------------------------------------------------------------

    /**
     * Entity → Response.
     *
     * Un parcours contient maintenant des ParcoursPhase.
     * On transforme chaque association en PhaseResponse.
     */
    public ParcoursResponse mapToResponse(
            Parcours parcours
    ) {

        ParcoursResponse response = new ParcoursResponse();

        response.setId(parcours.getId());
        response.setNom(parcours.getNom());
        response.setDescription(parcours.getDescription());
        response.setArchive(parcours.isArchive());
        response.setDateCreation(parcours.getDateCreation());
        response.setDateModification(parcours.getDateModification());

        if (parcours.getPhases() != null) {

            List<PhaseResponse> phaseResponses = parcours
                    .getPhases()
                    .stream()
                    .map(this::mapToPhaseResponse)
                    .collect(Collectors.toList());

            response.setPhases(phaseResponses);
        }

        return response;
    }

    /**
     * ParcoursPhase → PhaseResponse.
     *
     * L'ordre vient maintenant de ParcoursPhase,
     * et non plus directement de Phase.
     */
    private PhaseResponse mapToPhaseResponse(
            ParcoursPhase parcoursPhase
    ) {

        Phase phase = parcoursPhase.getPhase();

        PhaseResponse response = new PhaseResponse();

        response.setId(phase.getId());
        response.setNom(phase.getNom());
        response.setDescription(phase.getDescription());

        // L'ordre appartient à la relation Parcours ↔ Phase.
        response.setOrdre(parcoursPhase.getOrdre());

        response.setArchive(phase.isArchive());
        response.setDateCreation(phase.getDateCreation());
        response.setDateModification(phase.getDateModification());

        return response;
    }

    // -----------------------------------------------------------------------
    // VALIDATION
    // -----------------------------------------------------------------------

    private void validateNom(String nom) {

        if (nom == null || nom.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le nom du parcours est obligatoire"
            );
        }
    }

    // -----------------------------------------------------------------------
    // TENANT
    // -----------------------------------------------------------------------

    private UUID getRequiredTenantId() {

        UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "En-tête X-Structure-Id manquant"
            );
        }

        return tenantId;
    }
}