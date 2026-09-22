package sn.jappo.jappo_backend.livrable.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.livrable.dto.CreateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.EvaluateLivrableRequest;
import sn.jappo.jappo_backend.livrable.dto.LivrableResponse;
import sn.jappo.jappo_backend.livrable.dto.LivrableVersionResponse;
import sn.jappo.jappo_backend.livrable.dto.SoumettreVersionRequest;
import sn.jappo.jappo_backend.livrable.dto.UpdateLivrableRequest;
import sn.jappo.jappo_backend.livrable.entity.Livrable;
import sn.jappo.jappo_backend.livrable.entity.LivrableVersion;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.mission.dto.UpdateStatutMissionRequest;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.mission.service.MissionService;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.events.DeliverableSubmittedEvent;
import sn.jappo.jappo_backend.events.DeliverableEvaluatedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class LivrableService {

    private final LivrableRepository livrableRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final StructureRepository structureRepository;
    private final MissionService missionService;
    private final ApplicationEventPublisher eventPublisher;
    private final MissionCohorteRepository missionCohorteRepository;

    public LivrableService(
            LivrableRepository livrableRepository,
            MissionProjetRepository missionProjetRepository,
            StructureRepository structureRepository,
            MissionService missionService,
            ApplicationEventPublisher eventPublisher,
            MissionCohorteRepository missionCohorteRepository
    ) {
        this.livrableRepository = livrableRepository;
        this.missionProjetRepository = missionProjetRepository;
        this.structureRepository = structureRepository;
        this.missionService = missionService;
        this.eventPublisher = eventPublisher;
        this.missionCohorteRepository = missionCohorteRepository;
    }

    @Transactional
    public LivrableResponse createLivrable(CreateLivrableRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Structure structure = structureRepository.findById(activeStructureId)
                .orElseThrow(() -> new RuntimeException("Structure introuvable"));

        MissionProjet missionProjet = missionProjetRepository.findByIdAndStructureId(request.missionProjetId(), activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission projet introuvable"));

        Livrable livrable = new Livrable();
        livrable.setNom(request.nom());
        livrable.setUrl(request.url());
        livrable.setTypePiece(request.typePiece() != null ? request.typePiece() : TypeLivrable.FICHIER);
        livrable.setStatut(StatutLivrable.EN_ATTENTE);
        livrable.setNumeroVersion(1);
        livrable.setDateDepot(LocalDateTime.now());
        livrable.setMissionProjet(missionProjet);
        livrable.setProjet(missionProjet.getProjet());
        livrable.setStructure(structure);

        // Version 1 initiale
        LivrableVersion v1 = new LivrableVersion();
        v1.setNumeroVersion(1);
        v1.setNom(livrable.getNom());
        v1.setUrl(livrable.getUrl());
        v1.setTypePiece(livrable.getTypePiece());
        v1.setStatut(StatutLivrable.EN_ATTENTE);
        v1.setDateDepot(LocalDateTime.now());
        livrable.addVersion(v1);

        Livrable saved = livrableRepository.save(livrable);

        // Passer automatiquement la mission au statut SOUMIS lors du dépôt du livrable
        if (missionProjet.getStatut() == StatutMission.A_FAIRE || missionProjet.getStatut() == StatutMission.A_REVOIR) {
            missionService.updateStatut(missionProjet.getId(), new UpdateStatutMissionRequest(StatutMission.SOUMIS));
        }

        // Verrouillage structural de la MissionCohorte à la première soumission
        MissionCohorte mc = missionProjet.getMissionCohorte();
        if (mc != null && !mc.isVerrouillee()) {
            mc.setVerrouillee(true);
            mc.setDateVerrouillage(LocalDateTime.now());
            missionCohorteRepository.save(mc);
        }

        // Publier l'événement DELIVERABLE_SUBMITTED
        UUID entrepreneurId = missionProjet.getProjet().getEntrepreneur() != null 
                ? missionProjet.getProjet().getEntrepreneur().getId() 
                : null;
        
        eventPublisher.publishEvent(new DeliverableSubmittedEvent(
                activeStructureId,
                saved.getId(),
                missionProjet.getId(),
                missionProjet.getProjet().getId(),
                entrepreneurId,
                saved.getNumeroVersion() != null ? saved.getNumeroVersion() : 1,
                Instant.now()
        ));

        return mapToResponse(saved);
    }

    @Transactional
    public LivrableResponse soumettreNouvelleVersion(UUID id, SoumettreVersionRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Livrable livrable = livrableRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livrable introuvable"));

        // Rétrocompatibilité : s'assurer que l'historique contient au moins la version précédente
        if (livrable.getVersions() == null || livrable.getVersions().isEmpty()) {
            LivrableVersion vOld = new LivrableVersion();
            vOld.setNumeroVersion(livrable.getNumeroVersion() != null ? livrable.getNumeroVersion() : 1);
            vOld.setNom(livrable.getNom());
            vOld.setUrl(livrable.getUrl());
            vOld.setTypePiece(livrable.getTypePiece());
            vOld.setStatut(livrable.getStatut());
            vOld.setNote(livrable.getNote());
            vOld.setCommentaireCoach(livrable.getCommentaireCoach());
            vOld.setMotifRefus(livrable.getMotifRefus());
            vOld.setPointsACorriger(livrable.getPointsACorriger());
            vOld.setRessourceRecommandee(livrable.getRessourceRecommandee());
            vOld.setDateEcheanceCorrection(livrable.getDateEcheanceCorrection());
            vOld.setDateDepot(livrable.getDateDepot() != null ? livrable.getDateDepot() : LocalDateTime.now());
            vOld.setDateEvaluation(livrable.getDateEvaluation());
            livrable.addVersion(vOld);
        }

        int nouvelleVersionNum = (livrable.getNumeroVersion() != null ? livrable.getNumeroVersion() : 1) + 1;

        // Nouvelle entrée dans l'historique
        LivrableVersion nouvelleVersion = new LivrableVersion();
        nouvelleVersion.setNumeroVersion(nouvelleVersionNum);
        nouvelleVersion.setNom(request.nom() != null && !request.nom().isBlank() ? request.nom() : livrable.getNom());
        nouvelleVersion.setUrl(request.url());
        nouvelleVersion.setTypePiece(request.typePiece() != null ? request.typePiece() : livrable.getTypePiece());
        nouvelleVersion.setStatut(StatutLivrable.EN_ATTENTE);
        nouvelleVersion.setDateDepot(LocalDateTime.now());
        nouvelleVersion.setCommentaireEntrepreneur(request.commentaireEntrepreneur());
        livrable.addVersion(nouvelleVersion);

        // Mise à jour de l'état courant du livrable
        livrable.setNumeroVersion(nouvelleVersionNum);
        if (request.nom() != null && !request.nom().isBlank()) {
            livrable.setNom(request.nom());
        }
        livrable.setUrl(request.url());
        if (request.typePiece() != null) {
            livrable.setTypePiece(request.typePiece());
        }
        livrable.setStatut(StatutLivrable.EN_ATTENTE);
        livrable.setDateDepot(LocalDateTime.now());
        // Réinitialisation des retours de validation sur la version active (préservés dans l'historique vOld)
        livrable.setNote(null);
        livrable.setCommentaireCoach(null);
        livrable.setMotifRefus(null);
        livrable.setPointsACorriger(null);
        livrable.setRessourceRecommandee(null);
        livrable.setDateEcheanceCorrection(null);
        livrable.setDateEvaluation(null);

        Livrable saved = livrableRepository.save(livrable);

        // Mettre à jour le statut global de la mission
        synchroniserStatutMission(saved.getMissionProjet().getId(), activeStructureId);

        // Publier l'événement DELIVERABLE_SUBMITTED pour la nouvelle version
        UUID entrepreneurId = saved.getProjet().getEntrepreneur() != null 
                ? saved.getProjet().getEntrepreneur().getId() 
                : null;
        
        eventPublisher.publishEvent(new DeliverableSubmittedEvent(
                activeStructureId,
                saved.getId(),
                saved.getMissionProjet().getId(),
                saved.getProjet().getId(),
                entrepreneurId,
                nouvelleVersionNum,
                Instant.now()
        ));

        return mapToResponse(saved);
    }

    @Transactional
    public LivrableResponse evaluateLivrable(UUID id, EvaluateLivrableRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        Livrable livrable = livrableRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livrable introuvable"));

        // Capturer l'ancien statut avant modification
        StatutLivrable ancienStatut = livrable.getStatut();

        // Rétrocompatibilité : s'assurer qu'au moins une version existe
        if (livrable.getVersions() == null || livrable.getVersions().isEmpty()) {
            LivrableVersion v1 = new LivrableVersion();
            v1.setNumeroVersion(livrable.getNumeroVersion() != null ? livrable.getNumeroVersion() : 1);
            v1.setNom(livrable.getNom());
            v1.setUrl(livrable.getUrl());
            v1.setTypePiece(livrable.getTypePiece());
            v1.setDateDepot(livrable.getDateDepot() != null ? livrable.getDateDepot() : LocalDateTime.now());
            livrable.addVersion(v1);
        }

        LocalDateTime dateEvaluation = LocalDateTime.now();

        // 1. Mise à jour de l'entité Livrable principale
        livrable.setStatut(request.statut());
        livrable.setNote(request.note());
        livrable.setCommentaireCoach(request.commentaireCoach());
        livrable.setMotifRefus(request.motifRefus());
        livrable.setPointsACorriger(request.pointsACorriger());
        livrable.setRessourceRecommandee(request.ressourceRecommandee());
        livrable.setDateEcheanceCorrection(request.dateEcheanceCorrection());
        livrable.setDateEvaluation(dateEvaluation);

        // 2. Mise à jour de la version correspondante dans l'historique
        int currentVersionNum = livrable.getNumeroVersion() != null ? livrable.getNumeroVersion() : 1;
        LivrableVersion targetVersion = livrable.getVersions().stream()
                .filter(v -> v.getNumeroVersion() == currentVersionNum)
                .findFirst()
                .orElse(null);

        if (targetVersion != null) {
            targetVersion.setStatut(request.statut());
            targetVersion.setNote(request.note());
            targetVersion.setCommentaireCoach(request.commentaireCoach());
            targetVersion.setMotifRefus(request.motifRefus());
            targetVersion.setPointsACorriger(request.pointsACorriger());
            targetVersion.setRessourceRecommandee(request.ressourceRecommandee());
            targetVersion.setDateEcheanceCorrection(request.dateEcheanceCorrection());
            targetVersion.setDateEvaluation(dateEvaluation);
        }

        Livrable saved = livrableRepository.save(livrable);

        synchroniserStatutMission(livrable.getMissionProjet().getId(), activeStructureId);

        // Publier l'événement DELIVERABLE_EVALUATED uniquement si le statut a changé
        if (ancienStatut != request.statut()) {
            UUID entrepreneurId = saved.getProjet().getEntrepreneur() != null 
                    ? saved.getProjet().getEntrepreneur().getId() 
                    : null;
            
            eventPublisher.publishEvent(new DeliverableEvaluatedEvent(
                    activeStructureId,
                    saved.getId(),
                    saved.getProjet().getId(),
                    entrepreneurId,
                    request.statut(),
                    Instant.now()
            ));
        }

        return mapToResponse(saved);
    }

    private void synchroniserStatutMission(UUID missionProjetId, UUID structureId) {
        List<Livrable> livrables = livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjetId, structureId);
        if (livrables.isEmpty()) return;

        boolean tousValides = livrables.stream().allMatch(l -> l.getStatut() == StatutLivrable.VALIDE);
        boolean auMoinsUnACorriger = livrables.stream()
                .anyMatch(l -> l.getStatut() == StatutLivrable.A_CORRIGER || l.getStatut() == StatutLivrable.REJETE);
        boolean auMoinsUnEnAttente = livrables.stream()
                .anyMatch(l -> l.getStatut() == StatutLivrable.EN_ATTENTE);

        if (tousValides) {
            missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.VALIDE));
        } else if (auMoinsUnACorriger) {
            missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.A_REVOIR));
        } else if (auMoinsUnEnAttente) {
            missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.SOUMIS));
        }
    }

    @Transactional(readOnly = true)
    public LivrableResponse getLivrableById(UUID id) {
        UUID activeStructureId = getRequiredTenantId();
        Livrable livrable = livrableRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Livrable introuvable"));
        return mapToResponse(livrable);
    }

    @Transactional(readOnly = true)
    public List<LivrableResponse> getLivrablesByMission(UUID missionProjetId) {
        UUID activeStructureId = getRequiredTenantId();
        return livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjetId, activeStructureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LivrableResponse> getLivrablesByProjet(UUID projetId) {
        UUID activeStructureId = getRequiredTenantId();
        return livrableRepository.findAllByProjetIdAndStructureId(projetId, activeStructureId)
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

    private LivrableResponse mapToResponse(Livrable livrable) {
        List<LivrableVersionResponse> historique = new ArrayList<>();

        if (livrable.getVersions() != null && !livrable.getVersions().isEmpty()) {
            historique = livrable.getVersions().stream()
                    .map(v -> new LivrableVersionResponse(
                            v.getId(),
                            v.getNumeroVersion(),
                            v.getNom(),
                            v.getUrl(),
                            v.getTypePiece(),
                            v.getStatut(),
                            v.getNote(),
                            v.getCommentaireCoach(),
                            v.getMotifRefus(),
                            v.getPointsACorriger(),
                            v.getRessourceRecommandee(),
                            v.getDateEcheanceCorrection(),
                            v.getDateDepot(),
                            v.getDateEvaluation(),
                            v.getCommentaireEntrepreneur()
                    ))
                    .toList();
        } else {
            // Rétrocompatibilité données existantes sans entrée LivrableVersion
            historique = List.of(new LivrableVersionResponse(
                    livrable.getId(),
                    livrable.getNumeroVersion() != null ? livrable.getNumeroVersion() : 1,
                    livrable.getNom(),
                    livrable.getUrl(),
                    livrable.getTypePiece(),
                    livrable.getStatut(),
                    livrable.getNote(),
                    livrable.getCommentaireCoach(),
                    livrable.getMotifRefus(),
                    livrable.getPointsACorriger(),
                    livrable.getRessourceRecommandee(),
                    livrable.getDateEcheanceCorrection(),
                    livrable.getDateDepot(),
                    livrable.getDateEvaluation(),
                    null
            ));
        }

        return new LivrableResponse(
                livrable.getId(),
                livrable.getNom(),
                livrable.getUrl(),
                livrable.getTypePiece(),
                livrable.getStatut(),
                livrable.getNumeroVersion() != null ? livrable.getNumeroVersion() : 1,
                livrable.getNote(),
                livrable.getCommentaireCoach(),
                livrable.getMotifRefus(),
                livrable.getPointsACorriger(),
                livrable.getRessourceRecommandee(),
                livrable.getDateEcheanceCorrection(),
                livrable.getDateDepot(),
                livrable.getDateEvaluation(),
                livrable.getMissionProjet().getId(),
                livrable.getMissionProjet().getMissionCohorte().getTitre(),
                livrable.getProjet().getId(),
                livrable.getProjet().getNom(),
                livrable.getStructure().getId(),
                historique
        );
    }

    @Transactional
    public LivrableResponse updateLivrable(UUID id, UpdateLivrableRequest request) {
        UUID activeStructureId = getRequiredTenantId();
        Livrable livrable = livrableRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Livrable introuvable"));

        if (livrable.getStatut() != StatutLivrable.EN_ATTENTE) {
            throw new IllegalStateException("Ce livrable a déjà été évalué, il ne peut plus être modifié.");
        }

        if (request.nom() != null) livrable.setNom(request.nom());
        if (request.url() != null) livrable.setUrl(request.url());

        return mapToResponse(livrableRepository.save(livrable));
    }

    @Transactional
    public void deleteLivrable(UUID id) {
        UUID activeStructureId = getRequiredTenantId();
        Livrable livrable = livrableRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Livrable introuvable"));

        if (livrable.getStatut() != StatutLivrable.EN_ATTENTE) {
            throw new IllegalStateException("Ce livrable a déjà été évalué, il ne peut plus être supprimé.");
        }

        livrableRepository.delete(livrable);
    }
}