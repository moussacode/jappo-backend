package sn.jappo.jappo_backend.ia.context;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Représente l'état métier réel fourni au service IA pour construire une réponse.
 *
 * AiContext est construit par AiContextBuilder à partir de :
 *   - la structure active (TenantContext)
 *   - le ConversationContexte (périmètre sélectionné par le coach)
 *   - les données réelles de la BDD (via les repositories/services métier existants)
 *
 * Il ne doit JAMAIS contenir d'entités JPA complètes — uniquement des
 * identifiants et valeurs scalaires (UUID, String, int, enum, ...) pour éviter
 * tout lazy-loading, problème de sérialisation ou de dépendances JPA.
 *
 * Distinction fondamentale :
 *   ConversationContexte = ce que le coach a SÉLECTIONNÉ   ({"cohorteId":"..."})
 *   AiContext            = données réelles récupérées depuis la BDD
 */
public class AiContext {

    private UUID structureId;
    private String structureNom;

    // Utilisateur actuel
    private UUID userId;
    private String userPrenom;
    private String userNom;
    private String userRole;

    // Périmètre calculé
    private boolean contextGlobal;        // true si contexte vide = structure globale

    // Données cohorte (si cohorteId présent ou contexte global)
    private List<CohorteContext> cohortes = new ArrayList<>();

    // Données projet (si projetId présent ou contexte global limité)
    private List<ProjetContext> projets = new ArrayList<>();

    // Données missions (détails complets)
    private List<MissionContext> missions = new ArrayList<>();

    // Données livrables (détails complets)
    private List<LivrableContext> livrables = new ArrayList<>();

    // Statistiques agrégées utiles au LLM (évite d'envoyer toutes les entités)
    private long nombreTotalEntrepreneurs;
    private long nombreTotalProjets;
    private long nombreTotalCohortes;
    private long livrablesEnAttente;

    // ── Sous-objets ─────────────────────────────────────────────────────────

    public static class CohorteContext {
        public UUID id;
        public String nom;
        public String statut;
        public int nombreProjets;
        public String dateDebut;
        public String dateFin;
        public String phase;

        public CohorteContext(UUID id, String nom, String statut, int nombreProjets, 
                             String dateDebut, String dateFin, String phase) {
            this.id = id;
            this.nom = nom;
            this.statut = statut;
            this.nombreProjets = nombreProjets;
            this.dateDebut = dateDebut;
            this.dateFin = dateFin;
            this.phase = phase;
        }
    }

    public static class ProjetContext {
        public UUID id;
        public String nom;
        public String statut;
        public int scoreMaturite;
        public UUID cohorteId;
        public String cohorteNom;
        public UUID entrepreneurId;
        public String entrepreneurNom;
        public int nombreMissionsTotal;
        public int nombreMissionsValidees;
        public int nombreLivrablesEnAttente;

        public ProjetContext(UUID id, String nom, String statut, int scoreMaturite,
                             UUID cohorteId, String cohorteNom,
                             UUID entrepreneurId, String entrepreneurNom,
                             int nombreMissionsTotal, int nombreMissionsValidees,
                             int nombreLivrablesEnAttente) {
            this.id = id;
            this.nom = nom;
            this.statut = statut;
            this.scoreMaturite = scoreMaturite;
            this.cohorteId = cohorteId;
            this.cohorteNom = cohorteNom;
            this.entrepreneurId = entrepreneurId;
            this.entrepreneurNom = entrepreneurNom;
            this.nombreMissionsTotal = nombreMissionsTotal;
            this.nombreMissionsValidees = nombreMissionsValidees;
            this.nombreLivrablesEnAttente = nombreLivrablesEnAttente;
        }
    }

    public static class MissionContext {
        public UUID id;
        public String titre;
        public String description;
        public String priorite;
        public String dateEcheance;
        public String statut;
        public UUID cohorteId;
        public String cohorteNom;
        public UUID projetId;
        public String projetNom;

        public MissionContext(UUID id, String titre, String description, String priorite,
                              String dateEcheance, String statut, UUID cohorteId, String cohorteNom,
                              UUID projetId, String projetNom) {
            this.id = id;
            this.titre = titre;
            this.description = description;
            this.priorite = priorite;
            this.dateEcheance = dateEcheance;
            this.statut = statut;
            this.cohorteId = cohorteId;
            this.cohorteNom = cohorteNom;
            this.projetId = projetId;
            this.projetNom = projetNom;
        }
    }

    public static class LivrableContext {
        public UUID id;
        public String nom;
        public String statut;
        public String dateSoumission;
        public String dateEvaluation;
        public UUID missionId;
        public String missionTitre;
        public UUID projetId;
        public String projetNom;

        public LivrableContext(UUID id, String nom, String statut, String dateSoumission,
                               String dateEvaluation, UUID missionId, String missionTitre,
                               UUID projetId, String projetNom) {
            this.id = id;
            this.nom = nom;
            this.statut = statut;
            this.dateSoumission = dateSoumission;
            this.dateEvaluation = dateEvaluation;
            this.missionId = missionId;
            this.missionTitre = missionTitre;
            this.projetId = projetId;
            this.projetNom = projetNom;
        }
    }

    // ── Getters / Setters ────────────────────────────────────────────────────

    public UUID getStructureId() { return structureId; }
    public void setStructureId(UUID structureId) { this.structureId = structureId; }

    public String getStructureNom() { return structureNom; }
    public void setStructureNom(String structureNom) { this.structureNom = structureNom; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getUserPrenom() { return userPrenom; }
    public void setUserPrenom(String userPrenom) { this.userPrenom = userPrenom; }

    public String getUserNom() { return userNom; }
    public void setUserNom(String userNom) { this.userNom = userNom; }

    public String getUserRole() { return userRole; }
    public void setUserRole(String userRole) { this.userRole = userRole; }

    public boolean isContextGlobal() { return contextGlobal; }
    public void setContextGlobal(boolean contextGlobal) { this.contextGlobal = contextGlobal; }

    public List<CohorteContext> getCohortes() { return cohortes; }
    public void setCohortes(List<CohorteContext> cohortes) { this.cohortes = cohortes; }

    public List<ProjetContext> getProjets() { return projets; }
    public void setProjets(List<ProjetContext> projets) { this.projets = projets; }

    public List<MissionContext> getMissions() { return missions; }
    public void setMissions(List<MissionContext> missions) { this.missions = missions; }

    public List<LivrableContext> getLivrables() { return livrables; }
    public void setLivrables(List<LivrableContext> livrables) { this.livrables = livrables; }

    public long getNombreTotalEntrepreneurs() { return nombreTotalEntrepreneurs; }
    public void setNombreTotalEntrepreneurs(long n) { this.nombreTotalEntrepreneurs = n; }

    public long getNombreTotalProjets() { return nombreTotalProjets; }
    public void setNombreTotalProjets(long n) { this.nombreTotalProjets = n; }

    public long getNombreTotalCohortes() { return nombreTotalCohortes; }
    public void setNombreTotalCohortes(long n) { this.nombreTotalCohortes = n; }

    public long getLivrablesEnAttente() { return livrablesEnAttente; }
    public void setLivrablesEnAttente(long n) { this.livrablesEnAttente = n; }
}
