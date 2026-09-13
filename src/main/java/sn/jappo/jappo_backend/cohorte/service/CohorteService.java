package sn.jappo.jappo_backend.cohorte.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.cohorte.dto.CohorteResponse;
import sn.jappo.jappo_backend.cohorte.dto.CreateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.dto.UpdateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

import java.util.List;
import java.util.UUID;

@Service
public class CohorteService {

    private final CohorteRepository cohorteRepository;
    private final StructureRepository structureRepository;

    public CohorteService(CohorteRepository cohorteRepository, StructureRepository structureRepository) {
        this.cohorteRepository = cohorteRepository;
        this.structureRepository = structureRepository;
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

    // NOUVEAU : Récupérer une cohorte par son ID pour la structure active
    @Transactional(readOnly = true)
    public CohorteResponse getCohorteById(UUID id) {
        UUID activeStructureId = getRequiredTenantId();
        Cohorte cohorte = cohorteRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Cohorte introuvable ou accès non autorisé pour l'ID : " + id));
        return mapToResponse(cohorte);
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
                cohorte.getStructure().getId()
        );
    }



    @Transactional
public CohorteResponse updateCohorte(UUID id, UpdateCohorteRequest request) {
    UUID activeStructureId = getRequiredTenantId();
    Cohorte cohorte = cohorteRepository.findByIdAndStructureId(id, activeStructureId)
            .orElseThrow(() -> new RuntimeException("Cohorte introuvable"));

    if (request.nom() != null) cohorte.setNom(request.nom());
    if (request.description() != null) cohorte.setDescription(request.description());
    if (request.dateDebut() != null) cohorte.setDateDebut(request.dateDebut());
    if (request.dateFin() != null) cohorte.setDateFin(request.dateFin());
    if (request.statut() != null) cohorte.setStatut(request.statut());

    return mapToResponse(cohorteRepository.save(cohorte));
}

@Transactional
public void archiverCohorte(UUID id) {
    UUID activeStructureId = getRequiredTenantId();
    Cohorte cohorte = cohorteRepository.findByIdAndStructureId(id, activeStructureId)
            .orElseThrow(() -> new RuntimeException("Cohorte introuvable"));
    cohorte.setStatut(StatutCohorte.ARCHIVEE);
    cohorteRepository.save(cohorte);
}
}