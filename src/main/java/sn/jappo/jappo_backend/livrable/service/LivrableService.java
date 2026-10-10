package sn.jappo.jappo_backend.livrable.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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
import sn.jappo.jappo_backend.mission.dto.TemplateField;
import sn.jappo.jappo_backend.mission.dto.TemplateFieldCodec;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.LivrableAttendu;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.LivrableAttenduRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.mission.service.MissionService;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.events.DeliverableSubmittedEvent;
import sn.jappo.jappo_backend.events.DeliverableEvaluatedEvent;
import org.springframework.context.ApplicationEventPublisher;

@Service
public class LivrableService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String TEMPLATE_URL = "";

    private final LivrableRepository livrableRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final StructureRepository structureRepository;
    private final MissionService missionService;
    private final ApplicationEventPublisher eventPublisher;
    private final MissionCohorteRepository missionCohorteRepository;
    private final ProjetRepository projetRepository;
    private final LivrableAttenduRepository livrableAttenduRepository;

    public LivrableService(
            LivrableRepository livrableRepository,
            MissionProjetRepository missionProjetRepository,
            StructureRepository structureRepository,
            MissionService missionService,
            ApplicationEventPublisher eventPublisher,
            MissionCohorteRepository missionCohorteRepository,
            ProjetRepository projetRepository,
            LivrableAttenduRepository livrableAttenduRepository
    ) {
        this.livrableRepository = livrableRepository;
        this.missionProjetRepository = missionProjetRepository;
        this.structureRepository = structureRepository;
        this.missionService = missionService;
        this.eventPublisher = eventPublisher;
        this.missionCohorteRepository = missionCohorteRepository;
        this.projetRepository = projetRepository;
        this.livrableAttenduRepository = livrableAttenduRepository;
    }

    @Transactional
    public LivrableResponse createLivrable(CreateLivrableRequest request) {
        UUID activeStructureId = getRequiredTenantId();

        MissionProjet missionProjet = missionProjetRepository.findLockedByIdAndStructureId(
                request.missionProjetId(), activeStructureId)
                .orElseThrow(() -> new RuntimeException("Mission projet introuvable"));

        Structure structure = structureRepository.findById(activeStructureId)
            .orElseThrow(() -> new RuntimeException("Structure introuvable"));

        verifierProprietaireProjet(missionProjet.getProjet());
        verifierMissionProjetActive(missionProjet);
        validerContenuDepot(request.nom(), request.url(), request.typePiece(), request.contenu());

        List<LivrableAttendu> attendusMission = chargerAttendus(missionProjet);
        LivrableAttendu livrableAttendu =
                request.livrableAttenduId() != null
                        ? livrableAttenduRepository.findById(request.livrableAttenduId())
                                .orElseThrow(() -> new ResponseStatusException(
                                        HttpStatus.NOT_FOUND, "Livrable attendu introuvable"))
                        : null;
        if (livrableAttendu != null && !appartientAMissionProjet(livrableAttendu, missionProjet)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Ce livrable attendu n'appartient pas à cette mission.");
        }
        if (!attendusMission.isEmpty() && livrableAttendu == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Sélectionnez le livrable attendu auquel cette remise répond.");
        }
        if (livrableAttendu != null) {
            validerFormatAttendu(livrableAttendu, request.typePiece());
            validerReponsesTemplate(livrableAttendu, request.typePiece(), request.contenu());
        } else if (request.typePiece() == TypeLivrable.TEMPLATE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Une réponse de template doit être rattachée à un livrable attendu.");
        }

        List<Livrable> remisesExistantes = livrableRepository
                .findAllByMissionProjetIdAndStructureId(missionProjet.getId(), activeStructureId);
        if (livrableAttendu != null) {
            Livrable remisePourAttendu = remisesExistantes.stream()
                    .filter(existing -> existing.getLivrableAttendu() != null
                            && Objects.equals(existing.getLivrableAttendu().getId(), livrableAttendu.getId()))
                    .findFirst()
                    .orElse(null);
            if (remisePourAttendu != null
                    && (!remisePourAttendu.getNom().equals(request.nom())
                            || !Objects.equals(remisePourAttendu.getUrl(), request.typePiece() == TypeLivrable.TEMPLATE
                                    ? TEMPLATE_URL : request.url())
                            || !Objects.equals(remisePourAttendu.getContenu(), request.contenu())
                            || remisePourAttendu.getTypePiece()
                                    != (request.typePiece() != null ? request.typePiece() : TypeLivrable.FICHIER))) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Une remise existe déjà pour cet attendu. Déposez une nouvelle version de cette remise.");
            }
        }

        Livrable doublon = remisesExistantes.stream()
            .filter(existing -> existing.getNom().equals(request.nom())
                && Objects.equals(existing.getUrl(), request.typePiece() == TypeLivrable.TEMPLATE
                        ? TEMPLATE_URL : request.url())
                && Objects.equals(existing.getContenu(), request.contenu())
                && existing.getTypePiece() == (request.typePiece() != null ? request.typePiece() : TypeLivrable.FICHIER)
                && Objects.equals(
                        existing.getLivrableAttendu() != null ? existing.getLivrableAttendu().getId() : null,
                        livrableAttendu != null ? livrableAttendu.getId() : null))
            .findFirst()
            .orElse(null);
        if (doublon != null) {
            return mapToResponse(doublon);
        }

        Livrable livrable = new Livrable();
        livrable.setNom(request.nom());
        livrable.setUrl(request.typePiece() == TypeLivrable.TEMPLATE ? TEMPLATE_URL : request.url());
        livrable.setContenu(request.contenu());
        livrable.setTypePiece(request.typePiece() != null ? request.typePiece() : TypeLivrable.FICHIER);
        livrable.setStatut(StatutLivrable.EN_ATTENTE);
        livrable.setNumeroVersion(1);
        livrable.setDateDepot(LocalDateTime.now());
        livrable.setMissionProjet(missionProjet);
        livrable.setProjet(missionProjet.getProjet());
        livrable.setStructure(structure);

        livrable.setLivrableAttendu(livrableAttendu);

        // Version 1 initiale
        LivrableVersion v1 = new LivrableVersion();
        v1.setNumeroVersion(1);
        v1.setNom(livrable.getNom());
        v1.setUrl(livrable.getUrl());
        v1.setContenu(livrable.getContenu());
        v1.setTypePiece(livrable.getTypePiece());
        v1.setStatut(StatutLivrable.EN_ATTENTE);
        v1.setDateDepot(LocalDateTime.now());
        livrable.addVersion(v1);

        Livrable saved = livrableRepository.save(livrable);

        // Toute nouvelle remise attend une évaluation, y compris après une validation antérieure.
        if (missionProjet.getStatut() != StatutMission.SOUMIS) {
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

        verifierProprietaireProjet(livrable.getProjet());
        verifierMissionProjetActive(livrable.getMissionProjet());
        if (livrable.getStatut() != StatutLivrable.A_CORRIGER && livrable.getStatut() != StatutLivrable.REJETE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Une nouvelle version est autorisée uniquement après une demande de correction.");
        }
        TypeLivrable typeNouvelleVersion = request.typePiece() != null
                ? request.typePiece()
                : livrable.getTypePiece();
        String contenuNouvelleVersion = request.contenu() != null ? request.contenu() : livrable.getContenu();
        validerContenuDepot(
                request.nom() != null ? request.nom() : livrable.getNom(),
                request.url(),
                typeNouvelleVersion,
                contenuNouvelleVersion);
        if (livrable.getLivrableAttendu() != null) {
            validerFormatAttendu(livrable.getLivrableAttendu(), typeNouvelleVersion);
            validerReponsesTemplate(livrable.getLivrableAttendu(), typeNouvelleVersion, contenuNouvelleVersion);
        } else if (typeNouvelleVersion == TypeLivrable.TEMPLATE) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Une réponse de template doit être rattachée à un livrable attendu.");
        }

        // Rétrocompatibilité : s'assurer que l'historique contient au moins la version précédente
        if (livrable.getVersions() == null || livrable.getVersions().isEmpty()) {
            LivrableVersion vOld = new LivrableVersion();
            vOld.setNumeroVersion(livrable.getNumeroVersion() != null ? livrable.getNumeroVersion() : 1);
            vOld.setNom(livrable.getNom());
            vOld.setUrl(livrable.getUrl());
            vOld.setContenu(livrable.getContenu());
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
        nouvelleVersion.setUrl(typeNouvelleVersion == TypeLivrable.TEMPLATE ? TEMPLATE_URL : request.url());
        nouvelleVersion.setContenu(contenuNouvelleVersion);
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
        livrable.setUrl(typeNouvelleVersion == TypeLivrable.TEMPLATE ? TEMPLATE_URL : request.url());
        livrable.setContenu(contenuNouvelleVersion);
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
        verifierAccesProjet(livrable.getProjet());
        if (livrable.getStatut() != StatutLivrable.EN_ATTENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                "Ce dépôt a déjà été évalué; l'entrepreneur doit soumettre une nouvelle version.");
        }
        if (request.statut() != StatutLivrable.VALIDE
            && request.statut() != StatutLivrable.A_CORRIGER
            && request.statut() != StatutLivrable.REJETE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Statut d'évaluation non autorisé.");
        }

        // Capturer l'ancien statut avant modification
        StatutLivrable ancienStatut = livrable.getStatut();

        // Rétrocompatibilité : s'assurer qu'au moins une version existe
        if (livrable.getVersions() == null || livrable.getVersions().isEmpty()) {
            LivrableVersion v1 = new LivrableVersion();
            v1.setNumeroVersion(livrable.getNumeroVersion() != null ? livrable.getNumeroVersion() : 1);
            v1.setNom(livrable.getNom());
            v1.setUrl(livrable.getUrl());
            v1.setContenu(livrable.getContenu());
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
        // Poser dateValidation lors de la première validation ; ne pas l'effacer si déjà validé
        if (request.statut() == StatutLivrable.VALIDE && livrable.getDateValidation() == null) {
            livrable.setDateValidation(dateEvaluation);
        }

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

        MissionProjet missionProjet = missionProjetRepository.findByIdAndStructureId(missionProjetId, structureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission introuvable"));

        List<LivrableAttendu> attendusObligatoires = List.of();
        if (missionProjet.getMissionEtape() != null) {
            attendusObligatoires = livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(missionProjet.getMissionEtape().getId())
                .stream().filter(expected -> expected.isObligatoire()).toList();
        } else if (missionProjet.getMissionCohorte() != null) {
            attendusObligatoires = livrableAttenduRepository.findByMissionCohorteIdOrderByOrdreAsc(missionProjet.getMissionCohorte().getId())
                .stream().filter(expected -> expected.isObligatoire()).toList();
        }

        if (livrables.isEmpty()) {
            // Aucun livrable : revenir à A_FAIRE si aucun attendu défini, sinon rester à SOUMIS n'a pas de sens
            missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.A_FAIRE));
            return;
        }

        List<Livrable> livrablesValides = livrables.stream()
                .filter(l -> l.getStatut() == StatutLivrable.VALIDE)
                .toList();
        boolean auMoinsUnACorriger = livrables.stream()
                .anyMatch(l -> l.getStatut() == StatutLivrable.A_CORRIGER || l.getStatut() == StatutLivrable.REJETE);
        boolean auMoinsUnEnAttente = livrables.stream()
                .anyMatch(l -> l.getStatut() == StatutLivrable.EN_ATTENTE);

        Set<UUID> attendusObligatoiresIds = attendusObligatoires.stream()
                .map(LivrableAttendu::getId)
                .filter(Objects::nonNull)
                .collect(java.util.stream.Collectors.toSet());
        Set<UUID> attendusValidesIds = new HashSet<>();
        long remisesValideesSansAttendu = 0;
        for (Livrable livrable : livrablesValides) {
            LivrableAttendu attendu = livrable.getLivrableAttendu();
            if (attendu == null) {
                // Keep counting legacy submissions that predate the expected-item link.
                remisesValideesSansAttendu++;
            } else if (attendusObligatoiresIds.contains(attendu.getId())
                    && appartientAMissionProjet(attendu, missionProjet)) {
                attendusValidesIds.add(attendu.getId());
            }
        }

        long attendusValides = attendusValidesIds.size()
                + Math.min(remisesValideesSansAttendu, attendusObligatoiresIds.size() - attendusValidesIds.size());
        boolean tousValides = livrables.stream().allMatch(l -> l.getStatut() == StatutLivrable.VALIDE)
                && (attendusObligatoiresIds.isEmpty()
                        ? !livrablesValides.isEmpty()
                        : attendusValides >= attendusObligatoiresIds.size());

        if (tousValides) {
            missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.VALIDE));
        } else if (auMoinsUnACorriger) {
            missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.A_REVOIR));
        } else if (auMoinsUnEnAttente) {
            missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.SOUMIS));
        } else {
            // Des attendus obligatoires ne sont pas encore couverts par une remise valide.
            missionService.updateStatut(missionProjetId, new UpdateStatutMissionRequest(StatutMission.SOUMIS));
        }
    }

    private boolean appartientAMissionProjet(
            LivrableAttendu attendu,
            MissionProjet missionProjet
    ) {
        return (missionProjet.getMissionEtape() != null
                        && attendu.getMissionEtape() != null
                        && Objects.equals(attendu.getMissionEtape().getId(), missionProjet.getMissionEtape().getId()))
                || (missionProjet.getMissionCohorte() != null
                        && attendu.getMissionCohorte() != null
                        && Objects.equals(attendu.getMissionCohorte().getId(), missionProjet.getMissionCohorte().getId()));
    }

    private List<LivrableAttendu> chargerAttendus(MissionProjet missionProjet) {
        List<LivrableAttendu> attendus = missionProjet.getMissionEtape() != null
                ? livrableAttenduRepository.findByMissionEtapeIdOrderByOrdreAsc(
                        missionProjet.getMissionEtape().getId())
                : missionProjet.getMissionCohorte() != null
                        ? livrableAttenduRepository.findByMissionCohorteIdOrderByOrdreAsc(
                                missionProjet.getMissionCohorte().getId())
                        : List.of();
        return attendus != null ? attendus : List.of();
    }

    private void validerFormatAttendu(LivrableAttendu attendu, TypeLivrable typeSoumis) {
        TypeLivrable type = typeSoumis != null ? typeSoumis : TypeLivrable.FICHIER;
        boolean interactiveTemplate = attendu.getType() == TypeLivrable.TEMPLATE
                && !TemplateFieldCodec.decode(attendu.getTemplateConfiguration()).isEmpty();
        boolean formatCompatible = attendu.getType() == TypeLivrable.TEMPLATE
                ? interactiveTemplate ? type == TypeLivrable.TEMPLATE
                        : type == TypeLivrable.FICHIER || type == TypeLivrable.LIEN
                : attendu.getType() == type;
        if (!formatCompatible) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Le format de la remise ne correspond pas au format attendu ("
                            + attendu.getType() + ").");
        }
    }

    private void validerReponsesTemplate(
            LivrableAttendu attendu,
            TypeLivrable type,
            String contenu
    ) {
        List<TemplateField> fields = TemplateFieldCodec.decode(attendu.getTemplateConfiguration());
        if (attendu.getType() != TypeLivrable.TEMPLATE || fields.isEmpty()) {
            return;
        }
        if (type != TypeLivrable.TEMPLATE || contenu == null || contenu.isBlank() || contenu.length() > 200_000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Complétez les champs du template avant la remise.");
        }

        try {
            JsonNode answers = OBJECT_MAPPER.readTree(contenu);
            if (answers == null || !answers.isObject() || answers.size() != fields.size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les réponses ne correspondent pas aux champs du template.");
            }
            for (TemplateField field : fields) {
                JsonNode answer = answers.get(field.id());
                if (answer == null || !answer.isTextual() || answer.asText().isBlank() || answer.asText().length() > 10_000) {
                    throw new ResponseStatusException(
                            HttpStatus.BAD_REQUEST, "Le champ « " + field.label() + " » doit être renseigné (10 000 caractères maximum).");
                }
            }
        } catch (com.fasterxml.jackson.core.JsonProcessingException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Les réponses du template sont invalides.", exception);
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
        MissionProjet missionProjet = missionProjetRepository.findByIdAndStructureId(missionProjetId, activeStructureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Mission introuvable"));
        verifierAccesProjet(missionProjet.getProjet());
        return livrableRepository.findAllByMissionProjetIdAndStructureId(missionProjetId, activeStructureId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LivrableResponse> getLivrablesByProjet(UUID projetId) {
        UUID activeStructureId = getRequiredTenantId();
        Projet projet = projetRepository.findByIdAndStructureId(projetId, activeStructureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Projet introuvable"));
        verifierAccesProjet(projet);
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
                            v.getContenu(),
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
                    livrable.getContenu(),
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
                livrable.getContenu(),
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
                livrable.getDateValidation(),
                livrable.getMissionProjet().getId(),
                titreMission(livrable.getMissionProjet()),
                livrable.getProjet().getId(),
                livrable.getProjet().getNom(),
                livrable.getStructure().getId(),
                historique,
                livrable.getLivrableAttendu() != null ? livrable.getLivrableAttendu().getId() : null
        );
    }

    @Transactional
    public LivrableResponse updateLivrable(UUID id, UpdateLivrableRequest request) {
        UUID activeStructureId = getRequiredTenantId();
        Livrable livrable = livrableRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Livrable introuvable"));
        verifierProprietaireProjet(livrable.getProjet());

        if (livrable.getStatut() != StatutLivrable.EN_ATTENTE) {
            throw new IllegalStateException("Ce livrable a déjà été évalué, il ne peut plus être modifié.");
        }

        String nom = request.nom() != null ? request.nom() : livrable.getNom();
        String url = request.url() != null ? request.url() : livrable.getUrl();
        validerContenuDepot(nom, url, livrable.getTypePiece(), livrable.getContenu());
        if (livrable.getLivrableAttendu() != null) {
            validerFormatAttendu(livrable.getLivrableAttendu(), livrable.getTypePiece());
            validerReponsesTemplate(livrable.getLivrableAttendu(), livrable.getTypePiece(), livrable.getContenu());
        }

        livrable.setNom(nom);
        livrable.setUrl(url);

        return mapToResponse(livrableRepository.save(livrable));
    }

    @Transactional
    public void deleteLivrable(UUID id) {
        UUID activeStructureId = getRequiredTenantId();
        Livrable livrable = livrableRepository.findByIdAndStructureId(id, activeStructureId)
                .orElseThrow(() -> new RuntimeException("Livrable introuvable"));
        verifierAccesProjet(livrable.getProjet());

        if (livrable.getStatut() != StatutLivrable.EN_ATTENTE) {
            throw new IllegalStateException("Ce livrable a déjà été évalué, il ne peut plus être supprimé.");
        }

        livrableRepository.delete(livrable);
    }

    private void verifierMissionProjetActive(MissionProjet missionProjet) {
        if (missionProjet.isArchive()
                || (missionProjet.getMissionCohorte() != null && missionProjet.getMissionCohorte().isArchive())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Cette mission est archivée et n'accepte plus de remise.");
        }
    }

    private void verifierProprietaireProjet(Projet projet) {
        User user = utilisateurAuthentifie();
        if (projet == null || projet.getEntrepreneur() == null || !user.getId().equals(projet.getEntrepreneur().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous ne pouvez déposer que pour votre propre projet.");
        }
    }

    private void verifierAccesProjet(Projet projet) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean gestionnaire = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ADMIN_STRUCTURE")
                        || authority.getAuthority().equals("COACH")
                        || authority.getAuthority().equals("ROLE_ADMIN_STRUCTURE")
                        || authority.getAuthority().equals("ROLE_COACH"));
        if (gestionnaire) return;
        verifierProprietaireProjet(projet);
    }

    private User utilisateurAuthentifie() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Utilisateur authentifié requis.");
    }

    private void validerContenuDepot(String nom, String url, TypeLivrable type, String contenu) {
        boolean templateValide = type == TypeLivrable.TEMPLATE && contenu != null && !contenu.isBlank();
        if (nom == null || nom.isBlank() || (!templateValide && (url == null || url.isBlank()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le nom et le contenu du livrable sont obligatoires.");
        }
        if (url != null && url.length() > 1000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le contenu ou l'URL dépasse la longueur maximale de 1000 caractères.");
        }
        if (type == TypeLivrable.LIEN && (url == null || !url.matches("https?://.+"))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Un livrable de type LIEN doit utiliser une URL HTTP ou HTTPS.");
        }
        if (type != TypeLivrable.TEMPLATE && contenu != null && !contenu.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le contenu structuré est réservé aux remises de type TEMPLATE.");
        }
    }

    private String titreMission(MissionProjet missionProjet) {
        if (missionProjet.getMissionCohorte() != null) return missionProjet.getMissionCohorte().getTitre();
        if (missionProjet.getMissionEtape() != null) return missionProjet.getMissionEtape().getTitre();
        return "Mission";
    }
}