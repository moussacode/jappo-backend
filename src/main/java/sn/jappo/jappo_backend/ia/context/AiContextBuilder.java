package sn.jappo.jappo_backend.ia.context;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;


/**
 * Construit le AiContext à partir du ConversationContexte sélectionné par le coach
 * et des données réelles de la BDD.
 *
 * Responsabilité unique : transformer
 *   (structureId + ConversationContexte) → AiContext
 *
 * Règles :
 * - Ne jamais envoyer toute la BDD au modèle.
 * - Toujours vérifier le tenant actif (structureId) sur chaque requête.
 * - Contexte vide → structure globale (données agrégées limitées).
 * - Contexte avec cohorteId → données de la cohorte uniquement.
 * - Contexte avec projetId  → données du projet uniquement.
 * - Toujours vérifier que les IDs fournis appartiennent bien à la structure active.
 *
 * Réutilise les repositories métier existants (pas de nouvelle couche parallèle).
 */
@Service
@Transactional(readOnly = true)
public class AiContextBuilder {

    private final StructureRepository structureRepository;
    private final CohorteRepository cohorteRepository;
    private final ProjetRepository projetRepository;
    private final MissionCohorteRepository missionCohorteRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final LivrableRepository livrableRepository;
    private final UserRepository userRepository;
    private final MembreStructureRepository membreStructureRepository; 
    private final ParcoursRepository parcoursRepository;
    private final PhaseRepository phaseRepository;
    private final AiResourceContextService aiResourceContextService;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public AiContextBuilder(
            StructureRepository structureRepository,
            CohorteRepository cohorteRepository,
            ProjetRepository projetRepository,
            MissionCohorteRepository missionCohorteRepository,
            MissionProjetRepository missionProjetRepository,
            LivrableRepository livrableRepository,
            UserRepository userRepository,
            MembreStructureRepository membreStructureRepository,
            ParcoursRepository parcoursRepository,
            PhaseRepository phaseRepository,
            AiResourceContextService aiResourceContextService
            
    ) {
        this.structureRepository = structureRepository;
        this.cohorteRepository = cohorteRepository;
        this.projetRepository = projetRepository;
        this.missionCohorteRepository = missionCohorteRepository;
        this.missionProjetRepository = missionProjetRepository;
        this.livrableRepository = livrableRepository;
        this.userRepository = userRepository;
        this.membreStructureRepository = membreStructureRepository;
        this.parcoursRepository = parcoursRepository;
        this.phaseRepository = phaseRepository;
        this.aiResourceContextService = aiResourceContextService;
    }

    /**
     * Point d'entrée principal.
     *
     * @param structureId  tenant actif
     * @param contexte     périmètre sélectionné par le coach (peut être vide)
     * @param currentUser   utilisateur connecté (pour informations de contexte)
     */
    public AiContext build(UUID structureId, ConversationContexte contexte, User currentUser) {
        Structure structure = structureRepository.findById(structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Structure introuvable"));

        AiContext ctx = new AiContext();
        ctx.setStructureId(structureId);
        ctx.setStructureNom(structure.getNom());

        // Informations utilisateur
        if (currentUser != null) {
            ctx.setUserId(currentUser.getId());
            ctx.setUserPrenom(currentUser.getPrenom());
            ctx.setUserNom(currentUser.getNom());
            // Le rôle pourrait être déduit depuis MembreStructure, mais pour simplifier on peut le passer directement
            ctx.setUserRole("USER");
        }

        MembreStructure membre = currentUser == null ? null
                : membreStructureRepository.findByUserIdAndStructureId(currentUser.getId(), structureId).orElse(null);
        if (membre != null && membre.getRole() == RoleMembreStructure.ENTREPRENEUR) {
            if (membre.getStatut() != StatutMembre.ACCEPTE) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Membre entrepreneur inactif");
            }
            ctx.setUserRole(RoleMembreStructure.ENTREPRENEUR.name());
            buildContexteEntrepreneur(ctx, structureId, contexte, currentUser);
            return ctx;
        }
        if (membre != null) {
            ctx.setUserRole(membre.getRole().name());
        }

        if (contexte == null || contexte.isEmpty()) {
            // ── Cas A : contexte vide → structure globale ──────────────────
            buildContexteGlobal(ctx, structureId);
        } else if (contexte.getProjetId() != null) {
            // ── Cas C / D : projet (± cohorte) sélectionné ────────────────
            buildContexteProjet(ctx, structureId, contexte);
        } else if (contexte.getCohorteId() != null) {
            // ── Cas B : cohorte sélectionnée ──────────────────────────────
            buildContexteCohorte(ctx, structureId, contexte.getCohorteId());
        } else {
            // entrepreneurId seul (cas D partiel)
            buildContexteEntrepreneurSelectionne(ctx, structureId, contexte.getEntrepreneurId());
        }

        return ctx;
    }

    public AiResourceContextService.ResourceFile loadEntrepreneurSourceFile(
            UUID structureId, ConversationContexte contexte, User currentUser, UUID sourceId) {
        AiContext context = build(structureId, contexte, currentUser);
        boolean sourceIsAllowed = "ENTREPRENEUR".equals(context.getUserRole())
                && context.getSources().stream().anyMatch(source -> source.id.equals(sourceId));
        if (!sourceIsAllowed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Source introuvable");
        }
        return aiResourceContextService.loadStoredResourceFile(structureId, sourceId);
    }

    // ── Cas A : contexte global ──────────────────────────────────────────────

 // ── Cas A : contexte global ──────────────────────────────────────────────

private void buildContexteGlobal(AiContext ctx, UUID structureId) {
    ctx.setContextGlobal(true);

    // Statistiques agrégées (conservées, utiles pour un résumé rapide)
    long entrepreneurs = membreStructureRepository.countEntrepreneursByStructureId(structureId);
    long cohortesCount = cohorteRepository.countByStructureId(structureId);
    long projetsCount = projetRepository.countByStructureId(structureId);
    long livrablesEnAttente = livrableRepository.countByStructureIdAndStatut(structureId, StatutLivrable.EN_ATTENTE);

    ctx.setNombreTotalEntrepreneurs(entrepreneurs);
    ctx.setNombreTotalCohortes(cohortesCount);
    ctx.setNombreTotalProjets(projetsCount);
    ctx.setLivrablesEnAttente(livrablesEnAttente);

    // Charger TOUTES les cohortes de la structure
    List<Cohorte> allCohortes = cohorteRepository.findAllByStructureId(structureId);

    List<AiContext.CohorteContext> cohortesCtx = allCohortes.stream()
            .map(c -> {
                int nbProjets = (int) projetRepository.countByCohorteIdAndStructureId(c.getId(), structureId);
                return new AiContext.CohorteContext(
                        c.getId(), c.getNom(), c.getStatut().name(), nbProjets,
                        c.getDateDebut() != null ? c.getDateDebut().format(DATE_FORMATTER) : null,
                        c.getDateFin() != null ? c.getDateFin().format(DATE_FORMATTER) : null,
                        c.getPhase() != null ? c.getPhase().getNom() : null
                );
            })
            .toList();
    ctx.setCohortes(cohortesCtx);

  

    // NOUVEAU — Charger TOUS les projets (de toutes les cohortes)
    List<Projet> allProjets = new java.util.ArrayList<>();
    for (Cohorte c : allCohortes) {
        allProjets.addAll(projetRepository.findAllByCohorteIdAndStructureId(c.getId(), structureId));
    }
    ctx.setProjets(buildProjetsContext(allProjets, structureId));

    // NOUVEAU — Charger TOUTES les missions (de toutes les cohortes)
    List<AiContext.MissionContext> allMissions = new java.util.ArrayList<>();
    for (Cohorte c : allCohortes) {
        allMissions.addAll(buildMissionsContextForCohorte(c.getId(), structureId));
    }
    ctx.setMissions(allMissions);

    // NOUVEAU — Charger TOUS les livrables (de toutes les cohortes)
    List<AiContext.LivrableContext> allLivrables = new java.util.ArrayList<>();
    for (Cohorte c : allCohortes) {
        allLivrables.addAll(buildLivrablesContextForCohorte(c.getId(), structureId));
    }
    ctx.setLivrables(allLivrables);



      // NOUVEAU — Charger les parcours actifs (non archivés) de la structure
List<AiContext.ParcoursContext> parcoursCtx = parcoursRepository
        .findByStructureIdAndArchiveFalse(structureId)
        .stream()
        .map(p -> new AiContext.ParcoursContext(p.getId(), p.getNom()))
        .toList();
ctx.setParcours(parcoursCtx);
// NOUVEAU — Charger les phases actives (non archivées) de la structure
List<AiContext.PhaseContext> phasesCtx = phaseRepository
        .findAllByStructureIdAndArchiveFalseOrderByNom(structureId)
        .stream()
        .map(p -> new AiContext.PhaseContext(p.getId(), p.getNom()))
        .toList();
ctx.setPhases(phasesCtx);
}
    // ── Cas B : cohorte sélectionnée ─────────────────────────────────────────

    private void buildContexteCohorte(AiContext ctx, UUID structureId, UUID cohorteId) {
        ctx.setContextGlobal(false);

        // Sécurité : vérifier que la cohorte appartient bien à la structure active
        Cohorte cohorte = cohorteRepository.findByIdAndStructureId(cohorteId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Cohorte introuvable ou accès non autorisé"));

        int nbProjets = (int) projetRepository.countByCohorteIdAndStructureId(cohorteId, structureId);
        AiContext.CohorteContext cohorteCtx = new AiContext.CohorteContext(
                cohorte.getId(), cohorte.getNom(), cohorte.getStatut().name(), nbProjets,
                cohorte.getDateDebut() != null ? cohorte.getDateDebut().format(DATE_FORMATTER) : null,
                cohorte.getDateFin() != null ? cohorte.getDateFin().format(DATE_FORMATTER) : null,
                cohorte.getPhase() != null ? cohorte.getPhase().getNom() : null
        );
        ctx.setCohortes(List.of(cohorteCtx));

        // Projets de cette cohorte avec leur progression
        List<Projet> projets = projetRepository.findAllByCohorteIdAndStructureId(cohorteId, structureId);
        ctx.setProjets(buildProjetsContext(projets, structureId));

        // Missions de cette cohorte (détails complets)
        ctx.setMissions(buildMissionsContextForCohorte(cohorteId, structureId));

        // Livrables de cette cohorte (détails complets)
        ctx.setLivrables(buildLivrablesContextForCohorte(cohorteId, structureId));

        ctx.setNombreTotalProjets(nbProjets);
        ctx.setNombreTotalEntrepreneurs(projets.stream()
                .filter(p -> p.getEntrepreneur() != null).count());
    }

    // ── Cas C : projet sélectionné ───────────────────────────────────────────

    private void buildContexteProjet(AiContext ctx, UUID structureId, ConversationContexte contexte) {
        ctx.setContextGlobal(false);

        UUID projetId = contexte.getProjetId();

        // Sécurité : vérifier que le projet appartient à la structure active
        Projet projet = projetRepository.findByIdAndStructureId(projetId, structureId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN,
                        "Projet introuvable ou accès non autorisé"));

        // Si une cohorteId est aussi fournie, vérifier la cohérence
        if (contexte.getCohorteId() != null) {
            if (projet.getCohorte() == null
                    || !contexte.getCohorteId().equals(projet.getCohorte().getId())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Le projet sélectionné n'appartient pas à la cohorte sélectionnée");
            }
            // Ajouter le contexte cohorte également
            Cohorte cohorte = projet.getCohorte();
            ctx.setCohortes(List.of(new AiContext.CohorteContext(
                    cohorte.getId(), cohorte.getNom(), cohorte.getStatut().name(), 1,
                    cohorte.getDateDebut() != null ? cohorte.getDateDebut().format(DATE_FORMATTER) : null,
                    cohorte.getDateFin() != null ? cohorte.getDateFin().format(DATE_FORMATTER) : null,
                    cohorte.getPhase() != null ? cohorte.getPhase().getNom() : null
            )));
        }

        ctx.setProjets(buildProjetsContext(List.of(projet), structureId));
        ctx.setNombreTotalProjets(1);
        ctx.setNombreTotalEntrepreneurs(projet.getEntrepreneur() != null ? 1 : 0);

        // Missions du projet (détails complets)
        ctx.setMissions(buildMissionsContextForProjet(projetId, structureId));

        // Livrables du projet (détails complets)
        ctx.setLivrables(buildLivrablesContextForProjet(projetId, structureId));
    }

    // ── Cas D partiel : entrepreneur sélectionné par un coach ────────────────

    private void buildContexteEntrepreneurSelectionne(
            AiContext ctx, UUID structureId, UUID entrepreneurId) {
        ctx.setContextGlobal(false);
        projetRepository.findByEntrepreneurIdAndStructureId(entrepreneurId, structureId)
                .ifPresent(projet -> {
                    ctx.setProjets(buildProjetsContext(List.of(projet), structureId));
                    ctx.setNombreTotalProjets(1);
                    ctx.setNombreTotalEntrepreneurs(1);
                    ctx.setMissions(buildMissionsContextForProjet(projet.getId(), structureId));
                    ctx.setLivrables(buildLivrablesContextForProjet(projet.getId(), structureId));
                });
    }

    // ── Contexte entrepreneur authentifié ───────────────────────────────────
    private void buildContexteEntrepreneur(
            AiContext ctx, UUID structureId, ConversationContexte contexte,
            User currentUser) {
        ctx.setContextGlobal(false);
        UUID entrepreneurId = currentUser.getId();
        if (contexte != null && contexte.getEntrepreneurId() != null
                && !entrepreneurId.equals(contexte.getEntrepreneurId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Entrepreneur hors périmètre");
        }

        List<Projet> projetsPropres = projetRepository.findAllByEntrepreneurIdAndStructureId(
                entrepreneurId, structureId);
        if (projetsPropres.isEmpty()) {
            boolean contexteDemandeDesDonneesProjet = contexte != null
                    && (contexte.getCohorteId() != null
                    || contexte.getProjetId() != null
                    || contexte.getMissionId() != null);
            if (!contexteDemandeDesDonneesProjet) {
                // Un entrepreneur sans projet peut discuter de sujets généraux,
                // mais ne reçoit aucune donnée de la structure ou d'un autre projet.
                ctx.setSources(aiResourceContextService.loadForEntrepreneur(
                        structureId, null, null, null, List.of()));
                return;
            }
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Projet introuvable");
        }
        List<UUID> ownProjectIds = projetsPropres.stream().map(Projet::getId).toList();
        Projet projet;
        MissionProjet missionSelectionnee = null;
        if (contexte != null && contexte.getProjetId() != null) {
            projet = projetsPropres.stream()
                    .filter(ownProject -> ownProject.getId().equals(contexte.getProjetId()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Projet hors périmètre"));
        } else if (contexte != null && contexte.getMissionId() != null) {
            missionSelectionnee = missionProjetRepository
                    .findByIdAndProjetIdInAndStructureId(contexte.getMissionId(), ownProjectIds, structureId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Mission hors périmètre"));
            projet = missionSelectionnee.getProjet();
        } else if (projetsPropres.size() == 1) {
            projet = projetsPropres.getFirst();
        } else {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Sélectionnez un projet pour discuter");
        }
        Cohorte cohorte = projet.getCohorte();
        if (cohorte != null && !structureId.equals(cohorte.getStructure().getId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Projet hors structure active");
        }
        if (contexte != null && contexte.getCohorteId() != null
                && (cohorte == null || !cohorte.getId().equals(contexte.getCohorteId()))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cohorte hors périmètre");
        }

        List<MissionProjet> missionsProjet = missionProjetRepository
                .findAllByProjetIdAndStructureId(projet.getId(), structureId);
        if (missionSelectionnee == null && contexte != null && contexte.getMissionId() != null) {
            missionSelectionnee = missionProjetRepository
                    .findByIdAndProjetIdAndStructureId(contexte.getMissionId(), projet.getId(), structureId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Mission hors périmètre"));
        }

        ctx.setProjets(buildProjetsContext(List.of(projet), structureId));
        ctx.setNombreTotalProjets(1);
        ctx.setNombreTotalEntrepreneurs(1);
        List<AiContext.MissionContext> projectMissions = buildMissionsContextForProjet(projet.getId(), structureId);
        if (missionSelectionnee != null) {
            UUID missionSelectionneeId = missionSelectionnee.getId();
            ctx.setMissions(projectMissions.stream()
                    .filter(mission -> mission.id.equals(missionSelectionneeId))
                    .toList());
        } else {
            ctx.setMissions(projectMissions);
        }
        ctx.setLivrables(missionSelectionnee == null
                ? buildLivrablesContextForProjet(projet.getId(), structureId)
                : buildLivrablesContextForMission(missionSelectionnee, structureId));

        UUID cohorteId = null;
        UUID parcoursId = null;
        UUID phaseId = null;
        if (cohorte != null && cohorte.getStructure().getId().equals(structureId)) {
            cohorteId = cohorte.getId();
            if (cohorte.getParcours() != null
                    && !cohorte.getParcours().isArchive()
                    && cohorte.getParcours().getStructure().getId().equals(structureId)) {
                parcoursId = cohorte.getParcours().getId();
            }
            if (cohorte.getPhase() != null
                    && !cohorte.getPhase().isArchive()
                    && cohorte.getPhase().getStructure().getId().equals(structureId)) {
                phaseId = cohorte.getPhase().getId();
            }
        }
        List<MissionProjet> resourceMissions = missionSelectionnee == null
                ? missionsProjet : List.of(missionSelectionnee);
        List<UUID> missionResourceIds = resourceMissions.stream()
                .filter(mission -> !mission.isArchive())
                .filter(mp -> mp.getMissionCohorte() != null)
                .filter(mp -> !mp.getMissionCohorte().isArchive())
                .flatMap(mp -> mp.getMissionCohorte().getRessources().stream())
                .map(resource -> resource.getId())
                .distinct()
                .toList();
        ctx.setSources(aiResourceContextService.loadForEntrepreneur(
                structureId, cohorteId, parcoursId, phaseId, missionResourceIds));
    }

    private List<AiContext.LivrableContext> buildLivrablesContextForMission(
            MissionProjet mission, UUID structureId) {
        return mission.getLivrables().stream()
                .filter(l -> l.getStructure().getId().equals(structureId))
                .map(l -> {
                    MissionCohorte missionCohorte = mission.getMissionCohorte();
                    sn.jappo.jappo_backend.mission.entity.MissionEtape missionEtape = mission.getMissionEtape();
                    return new AiContext.LivrableContext(
                            l.getId(),
                            l.getNom(),
                            l.getStatut() != null ? l.getStatut().name() : null,
                            l.getDateDepot() != null ? l.getDateDepot().format(DATE_FORMATTER) : null,
                            l.getDateEvaluation() != null ? l.getDateEvaluation().format(DATE_FORMATTER) : null,
                            mission.getId(),
                            missionCohorte != null ? missionCohorte.getTitre()
                                    : missionEtape != null ? missionEtape.getTitre() : null,
                            mission.getProjet().getId(),
                            mission.getProjet().getNom());
                })
                .toList();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private List<AiContext.ProjetContext> buildProjetsContext(List<Projet> projets, UUID structureId) {
        return projets.stream().map(p -> {
            int nbTotal = (int) missionProjetRepository.countByProjetIdAndStructureId(p.getId(), structureId);
            int nbValidees = (int) missionProjetRepository.countByProjetIdAndStructureIdAndStatut(
                    p.getId(), structureId, StatutMission.VALIDE
            );
            long livrablesEnAttente = livrableRepository.countByProjetIdAndStructureIdAndStatut(
                    p.getId(), structureId, StatutLivrable.EN_ATTENTE
            );

            String entrepreneurNom = null;
            UUID entrepreneurId = null;
            if (p.getEntrepreneur() != null) {
                entrepreneurId = p.getEntrepreneur().getId();
                String prenom = p.getEntrepreneur().getPrenom() != null ? p.getEntrepreneur().getPrenom() : "";
                String nom = p.getEntrepreneur().getNom() != null ? p.getEntrepreneur().getNom() : "";
                entrepreneurNom = (prenom + " " + nom).trim();
            }

            UUID cohorteId = p.getCohorte() != null ? p.getCohorte().getId() : null;
            String cohorteNom = p.getCohorte() != null ? p.getCohorte().getNom() : null;

            return new AiContext.ProjetContext(
                    p.getId(), p.getNom(), p.getStatut().name(), p.getScoreMaturite(),
                    cohorteId, cohorteNom, entrepreneurId, entrepreneurNom,
                    nbTotal, nbValidees, (int) livrablesEnAttente
            );
        }).toList();
    }

    private List<AiContext.MissionContext> buildMissionsContextForCohorte(UUID cohorteId, UUID structureId) {
        // Récupérer les missions de cohorte de cette cohorte
        List<sn.jappo.jappo_backend.mission.entity.MissionCohorte> missionsCohorte = 
                missionCohorteRepository.findAllByCohorteIdAndStructureId(cohorteId, structureId);
        
        return missionsCohorte.stream().map(mc -> {
            return new AiContext.MissionContext(
                    mc.getId(),
                    mc.getTitre(),
                    mc.getDescription(),
                    mc.getPriorite() != null ? mc.getPriorite().name() : null,
                    mc.getDateEcheance() != null ? mc.getDateEcheance().format(DATE_FORMATTER) : null,
                    "ACTIVE", // Statut de la mission de cohorte (simplifié)
                    mc.getId(),
                    mc.getTitre(), // Utiliser le titre au lieu du nom (getNom n'existe pas)
                    null, // projetId non applicable pour MissionCohorte
                    null  // projetNom non applicable
            );
        }).toList();
    }

    private List<AiContext.MissionContext> buildMissionsContextForProjet(UUID projetId, UUID structureId) {
        // Récupérer les missions projet de ce projet
        List<sn.jappo.jappo_backend.mission.entity.MissionProjet> missionsProjet = 
                missionProjetRepository.findAllByProjetIdAndStructureId(projetId, structureId);
        
        return missionsProjet.stream().map(mp -> {
            MissionCohorte mc = mp.getMissionCohorte();
            sn.jappo.jappo_backend.mission.entity.MissionEtape missionEtape = mp.getMissionEtape();
            String titre = mc != null ? mc.getTitre()
                    : missionEtape != null ? missionEtape.getTitre() : "Mission sans titre";
            String description = mc != null ? mc.getDescription()
                    : missionEtape != null ? missionEtape.getDescription() : null;
            return new AiContext.MissionContext(
                    mp.getId(),
                    titre,
                    description,
                    mc != null && mc.getPriorite() != null ? mc.getPriorite().name() : null,
                    mc != null && mc.getDateEcheance() != null ? mc.getDateEcheance().format(DATE_FORMATTER) : null,
                    mp.getStatut() != null ? mp.getStatut().name() : null,
                    mc != null ? mc.getId() : null,
                    mc != null ? mc.getTitre() : null, // Utiliser getTitre au lieu de getNom
                    projetId,
                    null // projetNom pourrait être ajouté si nécessaire
            );
        }).toList();
    }

    private List<AiContext.LivrableContext> buildLivrablesContextForCohorte(UUID cohorteId, UUID structureId) {
        // Récupérer les projets de cette cohorte
        List<Projet> projets = projetRepository.findAllByCohorteIdAndStructureId(cohorteId, structureId);
        
        List<AiContext.LivrableContext> livrablesCtx = new java.util.ArrayList<>();
        for (Projet projet : projets) {
            // Récupérer tous les livrables de la structure et filtrer par projet
            List<sn.jappo.jappo_backend.livrable.entity.Livrable> livrablesStructure = 
                    livrableRepository.findAllByStructureIdOrderByDateDepotDesc(structureId, 
                            org.springframework.data.domain.Pageable.unpaged());
            
            List<sn.jappo.jappo_backend.livrable.entity.Livrable> livrables = livrablesStructure.stream()
                    .filter(l -> l.getProjet() != null && l.getProjet().getId().equals(projet.getId()))
                    .toList();
            
            livrablesCtx.addAll(livrables.stream().map(l -> {
                MissionCohorte mc = l.getMissionProjet() != null ? l.getMissionProjet().getMissionCohorte() : null;
                return new AiContext.LivrableContext(
                        l.getId(),
                        l.getNom(),
                        l.getStatut() != null ? l.getStatut().name() : null,
                        l.getDateDepot() != null ? l.getDateDepot().format(DATE_FORMATTER) : null,
                        l.getDateEvaluation() != null ? l.getDateEvaluation().format(DATE_FORMATTER) : null,
                        l.getMissionProjet() != null ? l.getMissionProjet().getId() : null,
                        mc != null ? mc.getTitre() : null,
                        projet.getId(),
                        projet.getNom()
                );
            }).toList());
        }
        
        return livrablesCtx;
    }

    private List<AiContext.LivrableContext> buildLivrablesContextForProjet(UUID projetId, UUID structureId) {
        // Récupérer tous les livrables de la structure et filtrer par projet
        List<sn.jappo.jappo_backend.livrable.entity.Livrable> livrablesStructure = 
                livrableRepository.findAllByStructureIdOrderByDateDepotDesc(structureId, 
                        org.springframework.data.domain.Pageable.unpaged());
        
        List<sn.jappo.jappo_backend.livrable.entity.Livrable> livrables = livrablesStructure.stream()
                .filter(l -> l.getProjet() != null && l.getProjet().getId().equals(projetId))
                .toList();
        
        return livrables.stream().map(l -> {
            MissionCohorte mc = l.getMissionProjet() != null ? l.getMissionProjet().getMissionCohorte() : null;
            return new AiContext.LivrableContext(
                    l.getId(),
                    l.getNom(),
                    l.getStatut() != null ? l.getStatut().name() : null,
                    l.getDateDepot() != null ? l.getDateDepot().format(DATE_FORMATTER) : null,
                    l.getDateEvaluation() != null ? l.getDateEvaluation().format(DATE_FORMATTER) : null,
                    l.getMissionProjet() != null ? l.getMissionProjet().getId() : null,
                    mc != null ? mc.getTitre() : null,
                    projetId,
                    null // projetNom pourrait être ajouté si nécessaire
            );
        }).toList();
    }
}
