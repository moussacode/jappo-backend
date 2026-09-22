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
import sn.jappo.jappo_backend.parcours.dto.CreatePhaseRequest;
import sn.jappo.jappo_backend.parcours.dto.PhaseResponse;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

@Service
@RequiredArgsConstructor
public class PhaseService {

    private final PhaseRepository phaseRepository;
    private final StructureRepository structureRepository;

    // -----------------------------------------------------------------------
    // LECTURE
    // -----------------------------------------------------------------------

    /**
     * Récupérer toutes les phases de la structure courante.
     */
    @Transactional(readOnly = true)
    public List<PhaseResponse> getAllPhases() {

        UUID structureId = getRequiredTenantId();

        return phaseRepository
                .findAllByStructureIdOrderByNom(structureId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer uniquement les phases actives.
     *
     * Utile notamment pour permettre à l'administrateur
     * de sélectionner des phases dans un parcours.
     */
    @Transactional(readOnly = true)
    public List<PhaseResponse> getActivePhases() {

        UUID structureId = getRequiredTenantId();

        return phaseRepository
                .findAllByStructureIdAndArchiveFalseOrderByNom(structureId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer une phase par son ID.
     */
    @Transactional(readOnly = true)
    public PhaseResponse getPhaseById(UUID phaseId) {

        UUID structureId = getRequiredTenantId();

        Phase phase = getPhaseForTenant(
                phaseId,
                structureId
        );

        return mapToResponse(phase);
    }

    // -----------------------------------------------------------------------
    // CRÉATION
    // -----------------------------------------------------------------------

    /**
     * Créer une nouvelle phase dans la bibliothèque
     * de la structure courante.
     *
     * Une phase n'appartient plus à un parcours.
     */
    @Transactional
    public PhaseResponse createPhase(
            CreatePhaseRequest request
    ) {

        UUID structureId = getRequiredTenantId();

        Structure structure = structureRepository
                .findById(structureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Structure introuvable"
                ));

        validateNom(request.getNom());

        String nom = request.getNom().trim();

        if (phaseRepository.existsByNomIgnoreCaseAndStructureId(
                nom,
                structureId
        )) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Une phase portant ce nom existe déjà"
            );
        }

        Phase phase = new Phase();

        phase.setNom(nom);
        phase.setDescription(request.getDescription());
        phase.setStructure(structure);
        phase.setArchive(false);

        Phase saved = phaseRepository.save(phase);

        return mapToResponse(saved);
    }

    // -----------------------------------------------------------------------
    // MODIFICATION
    // -----------------------------------------------------------------------

    /**
     * Modifier une phase.
     *
     * La modification concerne uniquement la phase elle-même.
     * L'ordre dans un parcours est géré par ParcoursPhase.
     */
    @Transactional
    public PhaseResponse updatePhase(
            UUID phaseId,
            CreatePhaseRequest request
    ) {

        UUID structureId = getRequiredTenantId();

        Phase phase = getPhaseForTenant(
                phaseId,
                structureId
        );

        if (request.getNom() != null) {

            validateNom(request.getNom());

            String nouveauNom = request.getNom().trim();

            boolean nomUtilise = phaseRepository
                    .existsByNomIgnoreCaseAndStructureId(
                            nouveauNom,
                            structureId
                    );

            if (nomUtilise
                    && !phase.getNom().equalsIgnoreCase(nouveauNom)) {

                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Une phase portant ce nom existe déjà"
                );
            }

            phase.setNom(nouveauNom);
        }

        if (request.getDescription() != null) {
            phase.setDescription(
                    request.getDescription()
            );
        }

        return mapToResponse(
                phaseRepository.save(phase)
        );
    }

    // -----------------------------------------------------------------------
    // ARCHIVAGE
    // -----------------------------------------------------------------------

    /**
     * Archiver une phase.
     *
     * On ne supprime jamais la phase physiquement.
     *
     * Une phase archivée peut continuer d'exister dans
     * l'historique des parcours, mais elle ne sera plus
     * proposée comme nouvelle phase disponible.
     */
    @Transactional
    public void archivePhase(UUID phaseId) {

        UUID structureId = getRequiredTenantId();

        Phase phase = getPhaseForTenant(
                phaseId,
                structureId
        );

        phase.setArchive(true);

        phaseRepository.save(phase);
    }

    // -----------------------------------------------------------------------
    // DÉSARCHIVAGE
    // -----------------------------------------------------------------------

    /**
     * Restaurer une phase archivée.
     */
    @Transactional
    public void unarchivePhase(UUID phaseId) {

        UUID structureId = getRequiredTenantId();

        Phase phase = getPhaseForTenant(
                phaseId,
                structureId
        );

        phase.setArchive(false);

        phaseRepository.save(phase);
    }

    // -----------------------------------------------------------------------
    // MAPPING
    // -----------------------------------------------------------------------

    private PhaseResponse mapToResponse(
            Phase phase
    ) {

        PhaseResponse response = new PhaseResponse();

        response.setId(phase.getId());
        response.setNom(phase.getNom());
        response.setDescription(phase.getDescription());

        /*
         * IMPORTANT :
         *
         * L'ordre n'appartient plus à Phase.
         * Il appartient à ParcoursPhase.
         *
         * Pour une phase globale, il n'y a donc pas d'ordre.
         */
        response.setOrdre(null);

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
                    "Le nom de la phase est obligatoire"
            );
        }
    }

    // -----------------------------------------------------------------------
    // TENANT
    // -----------------------------------------------------------------------

    private Phase getPhaseForTenant(
            UUID phaseId,
            UUID structureId
    ) {

        return phaseRepository
                .findByIdAndStructureId(
                        phaseId,
                        structureId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Phase introuvable"
                ));
    }

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