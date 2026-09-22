package sn.jappo.jappo_backend.ressource.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.common.service.FileStorageService;
import sn.jappo.jappo_backend.common.service.StoredFile;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.ressource.dto.CreateRessourceRequest;
import sn.jappo.jappo_backend.ressource.dto.RessourceResponse;
import sn.jappo.jappo_backend.ressource.dto.UpdateRessourceRequest;
import sn.jappo.jappo_backend.ressource.entity.PorteeRessource;
import sn.jappo.jappo_backend.ressource.entity.Ressource;
import sn.jappo.jappo_backend.ressource.entity.TypeRessource;
import sn.jappo.jappo_backend.ressource.repository.RessourceRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

@Service
public class RessourceService {

    private final RessourceRepository ressourceRepository;
    private final StructureRepository structureRepository;
    private final CohorteRepository cohorteRepository;
    private final ParcoursRepository parcoursRepository;
    private final ParcoursPhaseRepository parcoursPhaseRepository;
    private final PhaseRepository phaseRepository;
    private final MissionCohorteRepository missionCohorteRepository;
    private final FileStorageService fileStorageService;

    public RessourceService(
            RessourceRepository ressourceRepository,
            StructureRepository structureRepository,
            CohorteRepository cohorteRepository,
            ParcoursRepository parcoursRepository,
            ParcoursPhaseRepository parcoursPhaseRepository,
            PhaseRepository phaseRepository,
            MissionCohorteRepository missionCohorteRepository,
            FileStorageService fileStorageService) {

        this.ressourceRepository = ressourceRepository;
        this.structureRepository = structureRepository;
        this.cohorteRepository = cohorteRepository;
        this.parcoursRepository = parcoursRepository;
        this.parcoursPhaseRepository = parcoursPhaseRepository;
        this.phaseRepository = phaseRepository;
        this.missionCohorteRepository = missionCohorteRepository;
        this.fileStorageService = fileStorageService;
    }

    // -------------------------------------------------------
    // CRUD
    // -------------------------------------------------------

    @Transactional
    public RessourceResponse create(CreateRessourceRequest req) {
        UUID structureId = requireTenant();
        Structure structure = loadStructure(structureId);

        Ressource r = new Ressource();
        r.setStructure(structure);
        r.setTitre(req.titre());
        r.setDescription(req.description());
        r.setType(req.type() != null ? req.type() : TypeRessource.LIEN);
        r.setPortee(req.portee() != null ? req.portee() : PorteeRessource.STRUCTURE);
        r.setUrl(req.url());

        resolveRelations(
                r,
                req.portee(),
                req.cohorteId(),
                req.parcoursId(),
                req.phaseId(),
                structureId
        );

        Ressource saved = ressourceRepository.save(r);

        if (req.missionCohorteIds() != null
                && !req.missionCohorteIds().isEmpty()) {

            attachToMissions(
                    saved,
                    req.missionCohorteIds(),
                    structureId
            );
        }

        return toResponse(saved);
    }

    @Transactional
    public RessourceResponse createWithFichier(
            String titre,
            String description,
            PorteeRessource portee,
            UUID cohorteId,
            UUID parcoursId,
            UUID phaseId,
            MultipartFile file) {

        UUID structureId = requireTenant();
        Structure structure = loadStructure(structureId);

        StoredFile stored = fileStorageService.storeWithMeta(
                file,
                structureId,
                "ressources"
        );

        Ressource r = new Ressource();
        r.setStructure(structure);
        r.setTitre(titre);
        r.setDescription(description);
        r.setType(
                detectType(
                        stored.mimeType(),
                        stored.nomOriginal()
                )
        );
        r.setPortee(
                portee != null
                        ? portee
                        : PorteeRessource.STRUCTURE
        );
        r.setUrl(stored.url());
        r.setNomFichier(stored.nomOriginal());
        r.setTaille(stored.taille());
        r.setMimeType(stored.mimeType());

        resolveRelations(
                r,
                portee,
                cohorteId,
                parcoursId,
                phaseId,
                structureId
        );

        return toResponse(
                ressourceRepository.save(r)
        );
    }

    @Transactional(readOnly = true)
    public RessourceResponse getById(UUID id) {
        return toResponse(
                loadRessource(
                        id,
                        requireTenant()
                )
        );
    }

    @Transactional(readOnly = true)
    public List<RessourceResponse> getAll(boolean archivee) {
        return ressourceRepository
                .findAllByStructureIdAndArchiveeOrderByCreatedAtDesc(
                        requireTenant(),
                        archivee
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RessourceResponse> getByCohorte(UUID cohorteId) {
        UUID structureId = requireTenant();

        checkCohorteAccess(
                cohorteId,
                structureId
        );

        return ressourceRepository
                .findAllByStructureIdAndCohorteIdAndArchiveeFalse(
                        structureId,
                        cohorteId
                )
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RessourceResponse> getByMissionCohorte(
            UUID missionCohorteId) {

        UUID structureId = requireTenant();

        MissionCohorte mc =
                missionCohorteRepository
                        .findByIdAndStructureId(
                                missionCohorteId,
                                structureId
                        )
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Mission introuvable"
                                )
                        );

        return mc.getRessources()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public RessourceResponse update(
            UUID id,
            UpdateRessourceRequest req) {

        UUID structureId = requireTenant();

        Ressource r = loadRessource(
                id,
                structureId
        );

        if (req.titre() != null
                && !req.titre().isBlank()) {

            r.setTitre(req.titre());
        }

        if (req.description() != null) {
            r.setDescription(req.description());
        }

        if (req.type() != null) {
            r.setType(req.type());
        }

        if (req.url() != null) {
            r.setUrl(req.url());
        }

        if (req.portee() != null) {
            r.setPortee(req.portee());

            resolveRelations(
                    r,
                    req.portee(),
                    req.cohorteId(),
                    req.parcoursId(),
                    req.phaseId(),
                    structureId
            );
        }

        if (req.missionCohorteIds() != null) {

            detachFromAllMissions(r);

            if (!req.missionCohorteIds().isEmpty()) {
                attachToMissions(
                        r,
                        req.missionCohorteIds(),
                        structureId
                );
            }
        }

        return toResponse(
                ressourceRepository.save(r)
        );
    }

    @Transactional
    public RessourceResponse archiver(UUID id) {

        UUID structureId = requireTenant();

        Ressource r = loadRessource(
                id,
                structureId
        );

        if (r.isArchivee()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La ressource est déjà archivée."
            );
        }

        r.setArchivee(true);
        r.setDateArchivage(LocalDateTime.now());

        return toResponse(
                ressourceRepository.save(r)
        );
    }

    @Transactional
    public RessourceResponse restaurer(UUID id) {

        UUID structureId = requireTenant();

        Ressource r = loadRessource(
                id,
                structureId
        );

        if (!r.isArchivee()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La ressource n'est pas archivée."
            );
        }

        r.setArchivee(false);
        r.setDateArchivage(null);

        return toResponse(
                ressourceRepository.save(r)
        );
    }

    // -------------------------------------------------------
    // Helpers privés
    // -------------------------------------------------------

    private void resolveRelations(
            Ressource r,
            PorteeRessource portee,
            UUID cohorteId,
            UUID parcoursId,
            UUID phaseId,
            UUID structureId) {

        if (portee == null) {
            return;
        }

        switch (portee) {

            case COHORTE -> {

                if (cohorteId != null) {

                    Cohorte c = cohorteRepository
                            .findById(cohorteId)
                            .filter(co ->
                                    co.getStructure()
                                            .getId()
                                            .equals(structureId)
                            )
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Cohorte introuvable"
                                    )
                            );

                    r.setCohorte(c);
                }
            }

            case PARCOURS -> {

                if (parcoursId != null) {

                    Parcours p = parcoursRepository
                            .findById(parcoursId)
                            .filter(pa ->
                                    pa.getStructure()
                                            .getId()
                                            .equals(structureId)
                            )
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Parcours introuvable"
                                    )
                            );

                    r.setParcours(p);
                }
            }

            case PHASE -> {

                if (phaseId != null) {

                    Phase ph = phaseRepository
                            .findByIdAndStructureId(
                                    phaseId,
                                    structureId
                            )
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Phase introuvable"
                                    )
                            );

                    /*
                     * Une Phase est globale à la structure et peut être
                     * utilisée dans plusieurs Parcours.
                     *
                     * Si un parcours est fourni, on vérifie que cette
                     * phase est bien associée à ce parcours via
                     * ParcoursPhase.
                     */
                    if (parcoursId != null) {

                        parcoursRepository
                                .findByIdAndStructureId(
                                        parcoursId,
                                        structureId
                                )
                                .orElseThrow(() ->
                                        new ResponseStatusException(
                                                HttpStatus.NOT_FOUND,
                                                "Parcours introuvable"
                                        )
                                );

                        parcoursPhaseRepository
                                .findByParcours_IdAndPhase_Id(
                                        parcoursId,
                                        phaseId
                                )
                                .orElseThrow(() ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST,
                                                "La phase sélectionnée "
                                                        + "n'appartient pas "
                                                        + "à ce parcours"
                                        )
                                );
                    }

                    r.setPhase(ph);
                }
            }

            default -> {
                // STRUCTURE ou MISSION :
                // pas de FK supplémentaire sur la ressource elle-même.
            }
        }
    }

    private void attachToMissions(
            Ressource r,
            List<UUID> missionIds,
            UUID structureId) {

        for (UUID missionId : missionIds) {

            MissionCohorte mc =
                    missionCohorteRepository
                            .findByIdAndStructureId(
                                    missionId,
                                    structureId
                            )
                            .orElseThrow(() ->
                                    new ResponseStatusException(
                                            HttpStatus.NOT_FOUND,
                                            "Mission cohorte introuvable : "
                                                    + missionId
                            )
                            );

            mc.getRessources().add(r);

            missionCohorteRepository.save(mc);
        }
    }

    private void detachFromAllMissions(Ressource r) {
        // Charge les missions qui référencent cette ressource et les dissocie.
        // JPA gère la table de jointure depuis le côté propriétaire
        // (MissionCohorte).
        //
        // À implémenter si nécessaire selon la stratégie de gestion
        // de la table de jointure.
    }

    private UUID requireTenant() {

        UUID id = TenantContext.getCurrentTenant();

        if (id == null) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Aucune structure active. "
                            + "En-tête X-Structure-Id manquant."
            );
        }

        return id;
    }

    private Structure loadStructure(UUID structureId) {

        return structureRepository
                .findById(structureId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Structure introuvable : "
                                        + structureId
                        )
                );
    }

    private Ressource loadRessource(
            UUID id,
            UUID structureId) {

        return ressourceRepository
                .findByIdAndStructureId(
                        id,
                        structureId
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Ressource introuvable "
                                        + "ou accès refusé."
                        )
                );
    }

    private void checkCohorteAccess(
            UUID cohorteId,
            UUID structureId) {

        cohorteRepository
                .findById(cohorteId)
                .filter(c ->
                        c.getStructure()
                                .getId()
                                .equals(structureId)
                )
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Cohorte introuvable "
                                        + "ou accès refusé."
                        )
                );
    }

    private TypeRessource detectType(
            String mimeType,
            String nomOriginal) {

        if (mimeType == null) {
            return TypeRessource.DOCUMENT;
        }

        String m = mimeType.toLowerCase();
        String n = nomOriginal != null
                ? nomOriginal.toLowerCase()
                : "";

        if (m.contains("pdf") || n.endsWith(".pdf")) {
            return TypeRessource.PDF;
        }

        if (m.contains("video")
                || n.endsWith(".mp4")
                || n.endsWith(".mov")) {

            return TypeRessource.VIDEO;
        }

        return TypeRessource.DOCUMENT;
    }

    // -------------------------------------------------------
    // Mapping
    // -------------------------------------------------------

    public RessourceResponse toResponse(Ressource r) {

        List<UUID> missionIds = List.of();

        return new RessourceResponse(
                r.getId(),
                r.getStructure().getId(),
                r.getTitre(),
                r.getDescription(),
                r.getType(),
                r.getPortee(),
                r.getUrl(),
                r.getNomFichier(),
                r.getTaille(),
                r.getMimeType(),
                r.getCohorte() != null
                        ? r.getCohorte().getId()
                        : null,
                r.getCohorte() != null
                        ? r.getCohorte().getNom()
                        : null,
                r.getParcours() != null
                        ? r.getParcours().getId()
                        : null,
                r.getParcours() != null
                        ? r.getParcours().getNom()
                        : null,
                r.getPhase() != null
                        ? r.getPhase().getId()
                        : null,
                r.getPhase() != null
                        ? r.getPhase().getNom()
                        : null,
                missionIds,
                r.isArchivee(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }
}