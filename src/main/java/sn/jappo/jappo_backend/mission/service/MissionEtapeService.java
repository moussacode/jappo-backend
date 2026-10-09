package sn.jappo.jappo_backend.mission.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.mission.dto.CreateMissionEtapeRequest;
import sn.jappo.jappo_backend.mission.dto.MissionEtapeResponse;
import sn.jappo.jappo_backend.mission.dto.UpdateMissionEtapeRequest;
import sn.jappo.jappo_backend.mission.entity.MissionEtape;
import sn.jappo.jappo_backend.mission.entity.MissionModele;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionEtapeRepository;
import sn.jappo.jappo_backend.mission.repository.MissionModeleRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

@Service
public class MissionEtapeService {

    private final MissionEtapeRepository missionEtapeRepository;
    private final MissionModeleRepository missionModeleRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final ProjetRepository projetRepository;
    private final EtapeRepository etapeRepository;
    private final StructureRepository structureRepository;

    public MissionEtapeService(
            MissionEtapeRepository missionEtapeRepository,
            MissionModeleRepository missionModeleRepository,
            MissionProjetRepository missionProjetRepository,
            ProjetRepository projetRepository,
            EtapeRepository etapeRepository,
            StructureRepository structureRepository
    ) {
        this.missionEtapeRepository = missionEtapeRepository;
        this.missionModeleRepository = missionModeleRepository;
        this.missionProjetRepository = missionProjetRepository;
        this.projetRepository = projetRepository;
        this.etapeRepository = etapeRepository;
        this.structureRepository = structureRepository;
    }

    @Transactional(readOnly = true)
    public List<MissionEtapeResponse> getMissionsByEtapeId(UUID etapeId) {
        UUID structureId = getRequiredTenantId();
        Etape etape = loadEtapeForStructure(etapeId, structureId);

        return missionEtapeRepository
                .findByEtapeIdAndStructureIdOrderByOrdreAsc(etape.getId(), structureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MissionEtapeResponse getMissionEtapeById(UUID etapeId, UUID missionId) {
        UUID structureId = getRequiredTenantId();
        loadEtapeForStructure(etapeId, structureId);

        MissionEtape missionEtape = loadMissionForEtape(missionId, etapeId, structureId);
        return mapToResponse(missionEtape);
    }

    @Transactional
    public MissionEtapeResponse createMissionEtape(UUID etapeId, CreateMissionEtapeRequest request) {
        UUID structureId = getRequiredTenantId();

        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new IllegalArgumentException("Structure introuvable"));

        Etape etape = loadEtapeForStructure(etapeId, structureId);
        assertEtapeStructureCoherent(etape, structureId);

        validateTitre(request.titre());
        validateOrdre(request.ordre());
        assertOrdreUnique(etape.getId(), request.ordre(), null);

        MissionModele modele = resolveModele(request.modeleId(), structureId);

        MissionEtape missionEtape = new MissionEtape();
        missionEtape.setTitre(resolveTitre(request.titre(), modele));
        missionEtape.setDescription(resolveDescription(request.description(), modele));
        missionEtape.setOrdre(request.ordre());
        missionEtape.setObligatoire(resolveObligatoire(request.obligatoire(), modele));
        missionEtape.setStructure(structure);
        missionEtape.setEtape(etape);
        missionEtape.setModele(modele);

        MissionEtape saved = missionEtapeRepository.save(missionEtape);
        synchroniserMissionEtapeAvecProjetsConcernes(saved.getId());
        return mapToResponse(saved);
    }

    @Transactional
    public MissionEtapeResponse updateMissionEtape(
            UUID etapeId,
            UUID missionId,
            UpdateMissionEtapeRequest request
    ) {
        UUID structureId = getRequiredTenantId();
        loadEtapeForStructure(etapeId, structureId);

        MissionEtape missionEtape = loadMissionForEtape(missionId, etapeId, structureId);
        assertEtapeStructureCoherent(missionEtape.getEtape(), structureId);

        if (request.titre() != null) {
            validateTitre(request.titre());
            missionEtape.setTitre(request.titre().trim());
        }

        if (request.description() != null) {
            missionEtape.setDescription(
                    request.description().isBlank() ? null : request.description().trim()
            );
        }

        if (request.ordre() != null) {
            validateOrdre(request.ordre());
            if (!request.ordre().equals(missionEtape.getOrdre())) {
                assertOrdreUnique(etapeId, request.ordre(), missionEtape.getId());
                missionEtape.setOrdre(request.ordre());
            }
        }

        if (request.obligatoire() != null) {
            missionEtape.setObligatoire(request.obligatoire());
        }

        if (Boolean.TRUE.equals(request.clearModele())) {
            missionEtape.setModele(null);
        } else if (request.modeleId() != null) {
            MissionModele modele = resolveModele(request.modeleId(), structureId);
            missionEtape.setModele(modele);
        }

        return mapToResponse(missionEtapeRepository.save(missionEtape));
    }

    @Transactional
    public void deleteMissionEtape(UUID etapeId, UUID missionId) {
        UUID structureId = getRequiredTenantId();
        loadEtapeForStructure(etapeId, structureId);

        MissionEtape missionEtape = loadMissionForEtape(missionId, etapeId, structureId);
        missionEtape.getLivrablesAttendus().forEach(livrable -> livrable.setMissionEtape(null));
        missionEtape.getLivrablesAttendus().clear();
        missionEtapeRepository.delete(missionEtape);
    }

    @Transactional
    public void synchroniserMissionEtapeAvecProjetsConcernes(UUID missionEtapeId) {
        UUID structureId = getRequiredTenantId();

        MissionEtape missionEtape = missionEtapeRepository.findByIdAndStructureId(missionEtapeId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Mission étape introuvable pour cette structure"));

        if (missionEtape.getEtape() == null || missionEtape.getEtape().getPhase() == null) {
            return;
        }

        UUID phaseId = missionEtape.getEtape().getPhase().getId();

        List<Projet> projetsConcernes = projetRepository.findAllByStructureId(structureId).stream()
                .filter(projet -> projet.getCohorte() != null)
                .filter(projet -> projet.getCohorte().getPhase() != null)
                .filter(projet -> phaseId.equals(projet.getCohorte().getPhase().getId()))
                .filter(projet -> projet.getCohorte().getStructure() != null)
                .filter(projet -> projet.getCohorte().getStructure().getId().equals(structureId))
                .toList();

        for (Projet projet : projetsConcernes) {
            boolean exists = missionProjetRepository.existsByMissionEtapeIdAndProjetIdAndStructureId(
                    missionEtapeId,
                    projet.getId(),
                    structureId
            );
            if (exists) {
                continue;
            }

            MissionProjet missionProjet = new MissionProjet();
            missionProjet.setMissionEtape(missionEtape);
            missionProjet.setMissionCohorte(null);
            missionProjet.setProjet(projet);
            missionProjet.setStatut(StatutMission.A_FAIRE);
            missionProjet.setStructure(missionEtape.getStructure());
            missionProjetRepository.save(missionProjet);
        }
    }

    @Transactional
    public void synchroniserMissionEtapePourProjet(Projet projet, Cohorte cohorte) {
        if (projet == null || cohorte == null || projet.getStructure() == null || cohorte.getPhase() == null) {
            return;
        }

        UUID structureId = projet.getStructure().getId();
        if (cohorte.getStructure() == null || !cohorte.getStructure().getId().equals(structureId)) {
            return;
        }

        UUID phaseId = cohorte.getPhase().getId();

        List<MissionEtape> missionsEtupe = missionEtapeRepository.findByStructureId(structureId).stream()
                .filter(mission -> mission.getEtape() != null)
                .filter(mission -> mission.getEtape().getPhase() != null)
                .filter(mission -> phaseId.equals(mission.getEtape().getPhase().getId()))
                .toList();

        for (MissionEtape missionEtape : missionsEtupe) {
            if (missionProjetRepository.existsByMissionEtapeIdAndProjetIdAndStructureId(
                    missionEtape.getId(),
                    projet.getId(),
                    structureId
            )) {
                continue;
            }

            MissionProjet missionProjet = new MissionProjet();
            missionProjet.setMissionEtape(missionEtape);
            missionProjet.setMissionCohorte(null);
            missionProjet.setProjet(projet);
            missionProjet.setStatut(StatutMission.A_FAIRE);
            missionProjet.setStructure(projet.getStructure());
            missionProjetRepository.save(missionProjet);
        }
    }

    private Etape loadEtapeForStructure(UUID etapeId, UUID structureId) {
        Etape etape = etapeRepository.findByIdAndStructureId(etapeId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Étape introuvable pour cette structure"));

        if (etape.isArchive()) {
            throw new IllegalArgumentException("Impossible d'associer une mission à une étape archivée");
        }

        Phase phase = etape.getPhase();
        if (phase == null) {
            throw new IllegalStateException("L'étape n'est rattachée à aucune phase");
        }
        if (phase.getStructure() == null || !phase.getStructure().getId().equals(structureId)) {
            throw new IllegalArgumentException("La phase de l'étape n'appartient pas à cette structure");
        }

        return etape;
    }

    private MissionEtape loadMissionForEtape(UUID missionId, UUID etapeId, UUID structureId) {
        return missionEtapeRepository.findByIdAndEtapeIdAndStructureId(missionId, etapeId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Mission étape introuvable pour cette étape"));
    }

    private void assertEtapeStructureCoherent(Etape etape, UUID structureId) {
        if (etape.getStructure() == null || !etape.getStructure().getId().equals(structureId)) {
            throw new IllegalArgumentException("L'étape n'appartient pas à cette structure");
        }
    }

    private MissionModele resolveModele(UUID modeleId, UUID structureId) {
        if (modeleId == null) {
            return null;
        }
        return missionModeleRepository.findByIdAndStructureId(modeleId, structureId)
                .orElseThrow(() -> new IllegalArgumentException("Modèle introuvable pour cette structure"));
    }

    private void validateTitre(String titre) {
        if (titre == null || titre.isBlank()) {
            throw new IllegalArgumentException("Le titre de la mission est obligatoire");
        }
        if (titre.trim().length() > 150) {
            throw new IllegalArgumentException("Le titre de la mission ne peut pas dépasser 150 caractères");
        }
    }

    private void validateOrdre(Integer ordre) {
        if (ordre == null || ordre < 0) {
            throw new IllegalArgumentException("L'ordre doit être un entier positif ou nul");
        }
    }

    private void assertOrdreUnique(UUID etapeId, Integer ordre, UUID excludeId) {
        boolean exists = excludeId == null
                ? missionEtapeRepository.existsByEtapeIdAndOrdre(etapeId, ordre)
                : missionEtapeRepository.existsByEtapeIdAndOrdreAndIdNot(etapeId, ordre, excludeId);
        if (exists) {
            throw new IllegalArgumentException("Une mission avec cet ordre existe déjà dans cette étape");
        }
    }

    private String resolveTitre(String titre, MissionModele modele) {
        if (titre != null && !titre.isBlank()) {
            return titre.trim();
        }
        if (modele != null) {
            return modele.getTitre();
        }
        throw new IllegalArgumentException("Le titre de la mission est obligatoire");
    }

    private String resolveDescription(String description, MissionModele modele) {
        if (description != null && !description.isBlank()) {
            return description.trim();
        }
        if (modele != null && modele.getDescription() != null) {
            return modele.getDescription();
        }
        return null;
    }

    private boolean resolveObligatoire(Boolean obligatoire, MissionModele modele) {
        if (obligatoire != null) {
            return obligatoire;
        }
        if (modele != null) {
            return modele.isObligatoire();
        }
        return true;
    }

    private MissionEtapeResponse mapToResponse(MissionEtape missionEtape) {
        Etape etape = missionEtape.getEtape();
        UUID phaseId = etape != null && etape.getPhase() != null ? etape.getPhase().getId() : null;
        UUID modeleId = missionEtape.getModele() != null ? missionEtape.getModele().getId() : null;

        return new MissionEtapeResponse(
                missionEtape.getId(),
                missionEtape.getTitre(),
                missionEtape.getDescription(),
                missionEtape.getOrdre(),
                missionEtape.isObligatoire(),
                missionEtape.getStructure().getId(),
                etape != null ? etape.getId() : null,
                phaseId,
                modeleId,
                missionEtape.getDateCreation()
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
