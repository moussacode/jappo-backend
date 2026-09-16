package sn.jappo.jappo_backend.cohorte.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.cohorte.dto.CohorteResponse;
import sn.jappo.jappo_backend.cohorte.dto.CreateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.dto.UpdateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.events.CohortCreatedEvent;
import sn.jappo.jappo_backend.events.CohortCompletedEvent;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.cohorte.entity.PhaseParcours;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class CohorteService {

    private final CohorteRepository cohorteRepository;
    private final StructureRepository structureRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CohorteService(
            CohorteRepository cohorteRepository,
            StructureRepository structureRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.cohorteRepository = cohorteRepository;
        this.structureRepository = structureRepository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CohorteResponse createCohorte(CreateCohorteRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Structure structure = structureRepository.findById(activeStructureId)
                .orElseThrow(() -> new RuntimeException("Structure non trouvée pour l'ID: " + activeStructureId));

        Cohorte cohorte = new Cohorte();
        cohorte.setNom(request.nom());
        cohorte.setDescription(request.description());
        cohorte.setDateDebut(request.dateDebut());
        cohorte.setDateFin(request.dateFin());
        cohorte.setStatut(StatutCohorte.PLANIFIEE);
        cohorte.setStructure(structure);

        Cohorte saved = cohorteRepository.save(cohorte);

        // Publier COHORT_CREATED après sauvegarde réussie (ID généré garanti)
        eventPublisher.publishEvent(new CohortCreatedEvent(
                activeStructureId,
                saved.getId(),
                saved.getNom(),
                Instant.now()
        ));

        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<CohorteResponse> getCohortesForActiveStructure() {
        UUID activeStructureId = getRequiredTenantId();
        return cohorteRepository.findAllByStructureId(activeStructureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CohorteResponse> getActiveCohortesForActiveStructure() {
        UUID activeStructureId = getRequiredTenantId();
        return cohorteRepository.findAllByStructureId(activeStructureId)
                .stream()
                .filter(cohorte -> cohorte.getStatut() != StatutCohorte.ARCHIVEE)
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public CohorteResponse getCohorteById(UUID id) {
        UUID activeStructureId = getRequiredTenantId();
        Cohorte cohorte = cohorteRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Cohorte introuvable ou accès non autorisé pour l'ID : " + id));
        return mapToResponse(cohorte);
    }

    @Transactional
    public CohorteResponse updateCohorte(UUID id, UpdateCohorteRequest request) {
        UUID activeStructureId = getRequiredTenantId();
        Cohorte cohorte = cohorteRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Cohorte introuvable"));

        // Capturer l'ancien statut pour détecter le passage en TERMINEE
        StatutCohorte ancienStatut = cohorte.getStatut();

        if (request.nom() != null) cohorte.setNom(request.nom());
        if (request.description() != null) cohorte.setDescription(request.description());
        if (request.dateDebut() != null) cohorte.setDateDebut(request.dateDebut());
        if (request.dateFin() != null) cohorte.setDateFin(request.dateFin());
        if (request.statut() != null) cohorte.setStatut(request.statut());
        if (request.phase() != null) cohorte.setPhase(request.phase());

        Cohorte saved = cohorteRepository.save(cohorte);

        // Publier COHORT_COMPLETED uniquement au premier passage en TERMINEE
        if (request.statut() == StatutCohorte.TERMINEE && ancienStatut != StatutCohorte.TERMINEE) {
            eventPublisher.publishEvent(new CohortCompletedEvent(
                    activeStructureId,
                    saved.getId(),
                    saved.getNom(),
                    Instant.now()
            ));
        }

        return mapToResponse(saved);
    }

    @Transactional
    public void archiverCohorte(UUID id) {
        UUID activeStructureId = getRequiredTenantId();
        Cohorte cohorte = cohorteRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Cohorte introuvable"));
        cohorte.setStatut(StatutCohorte.ARCHIVEE);
        cohorteRepository.save(cohorte);
    }

    /**
     * Restaure une cohorte archivée.
     * Idempotent : peut être appelé plusieurs fois sans erreur.
     */
    @Transactional
    public void restaurerCohorte(UUID id) {
        UUID activeStructureId = getRequiredTenantId();
        Cohorte cohorte = cohorteRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Cohorte introuvable"));

        if (cohorte.getStatut() != StatutCohorte.ARCHIVEE) {
            return; // Déjà non archivée, idempotent
        }

        // Restaurer à EN_COURS par défaut (ou pourrait être un paramètre)
        cohorte.setStatut(StatutCohorte.EN_COURS);
        cohorteRepository.save(cohorte);
    }

    /**
     * Récupérer les cohortes par statut (actives ou archivées).
     */
    @Transactional(readOnly = true)
    public List<CohorteResponse> getCohortesByStatut(StatutCohorte statut) {
        UUID activeStructureId = getRequiredTenantId();
        return cohorteRepository.findAllByStructureIdAndStatut(activeStructureId, statut)
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

    private CohorteResponse mapToResponse(Cohorte cohorte) {
        return new CohorteResponse(
                cohorte.getId(),
                cohorte.getNom(),
                cohorte.getDescription(),
                cohorte.getDateDebut(),
                cohorte.getDateFin(),
                cohorte.getStatut(),
                cohorte.getPhase(),
                cohorte.getStructure().getId()
        );
    }
}
