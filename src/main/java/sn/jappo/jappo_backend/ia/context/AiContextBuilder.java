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
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public AiContextBuilder(
            StructureRepository structureRepository,
            CohorteRepository cohorteRepository,
            ProjetRepository projetRepository,
            MissionCohorteRepository missionCohorteRepository,
            MissionProjetRepository missionProjetRepository,
            LivrableRepository livrableRepository,
            UserRepository userRepository
    ) {
        this.structureRepository = structureRepository;
        this.cohorteRepository = cohorteRepository;
        this.projetRepository = projetRepository;
        this.missionCohorteRepository = missionCohorteRepository;
        this.missionProjetRepository = missionProjetRepository;
        this.livrableRepository = livrableRepository;
        this.userRepository = userRepository;
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
            ctx.setUserRole("USER"); // À adapter selon la logique métier
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
            buildContexteEntrepreneur(ctx, structureId, contexte.getEntrepreneurId());
        }

        return ctx;
    }

    // ── Cas A : contexte global ──────────────────────────────────────────────

    private void buildContexteGlobal(AiContext ctx, UUID structureId) {
        ctx.setContextGlobal(true);

        // Statistiques agrégées légères — ne pas charger toutes les entités
        long entrepreneurs = userRepository.countEntrepreneursByStructureId(structureId);
        long cohortes = cohorteRepository.countByStructureId(structureId);
        long projets = projetRepository.countByStructureId(structureId);
        long livrablesEnAttente = livrableRepository.countByStructureIdAndStatut(structureId, StatutLivrable.EN_ATTENTE);

        ctx.setNombreTotalEntrepreneurs(entrepreneurs);
        ctx.setNombreTotalCohortes(cohortes);
        ctx.setNombreTotalProjets(projets);
        ctx.setLivrablesEnAttente(livrablesEnAttente);

        // Charger les cohortes avec leur nombre de projets (résumé, pas tout le détail)
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

    // ── Cas D partiel : entrepreneur sélectionné seul ────────────────────────

    private void buildContexteEntrepreneur(AiContext ctx, UUID structureId, UUID entrepreneurId) {
        ctx.setContextGlobal(false);

        // Trouver le projet de cet entrepreneur dans cette structure
        projetRepository.findByEntrepreneurIdAndStructureId(entrepreneurId, structureId)
                .ifPresent(projet -> {
                    ctx.setProjets(buildProjetsContext(List.of(projet), structureId));
                    ctx.setNombreTotalProjets(1);
                    ctx.setNombreTotalEntrepreneurs(1);
                    
                    // Missions du projet (détails complets)
                    ctx.setMissions(buildMissionsContextForProjet(projet.getId(), structureId));
                    
                    // Livrables du projet (détails complets)
                    ctx.setLivrables(buildLivrablesContextForProjet(projet.getId(), structureId));
                });
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
            return new AiContext.MissionContext(
                    mp.getId(),
                    mc != null ? mc.getTitre() : "Mission sans titre",
                    mc != null ? mc.getDescription() : null,
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
