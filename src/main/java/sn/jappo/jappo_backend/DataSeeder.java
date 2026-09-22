package sn.jappo.jappo_backend;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.cohorte.service.ParticipationService;

import sn.jappo.jappo_backend.config.tenant.TenantContext;

import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;

import sn.jappo.jappo_backend.parcours.dto.CreateParcoursRequest;
import sn.jappo.jappo_backend.parcours.dto.CreatePhaseRequest;
import sn.jappo.jappo_backend.parcours.dto.PhaseResponse;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.parcours.service.ParcoursPhaseService;
import sn.jappo.jappo_backend.parcours.service.ParcoursService;
import sn.jappo.jappo_backend.parcours.service.PhaseService;

import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;

import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Seeder idempotent pour le scénario de démonstration "Téranga Hub".
 *
 * Actif uniquement en profil "dev".
 *
 * Utilisateurs créés :
 * - awa@teranga.sn      → ADMIN_STRUCTURE
 * - ibrahima@teranga.sn → COACH
 * - fatou@teranga.sn    → ENTREPRENEUR
 * - cheikh@teranga.sn   → ENTREPRENEUR
 * - ndeye@teranga.sn    → ENTREPRENEUR
 *
 * Mot de passe de démonstration :
 * Demo2024!
 */
@Slf4j
@Component
@Profile("dev")
@RequiredArgsConstructor
public class DataSeeder implements ApplicationRunner {

    private static final String DEMO_PASSWORD = "Demo2024!";
    private static final String STRUCTURE_SLUG = "teranga-hub";

    private final UserRepository userRepository;
    private final StructureRepository structureRepository;
    private final MembreStructureRepository membreStructureRepository;

    private final ParcoursRepository parcoursRepository;
    private final PhaseRepository phaseRepository;

    private final CohorteRepository cohorteRepository;

    private final ProjetRepository projetRepository;

    private final MissionCohorteRepository missionCohorteRepository;

    private final ParticipationService participationService;

    private final ParcoursService parcoursService;
    private final PhaseService phaseService;
    private final ParcoursPhaseService parcoursPhaseService;

    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {

        // Idempotent :
        // si la structure existe déjà, on ne recrée pas les données de démonstration.
        if (structureRepository.findBySlug(STRUCTURE_SLUG).isPresent()) {
            log.info(
                    "[Seeder] Scénario Téranga Hub déjà présent — aucune action."
            );
            return;
        }

        log.info(
                "[Seeder] Création du scénario Téranga Hub..."
        );

        // -------------------------------------------------------------------
        // 1. Utilisateurs
        // -------------------------------------------------------------------

        User awa = creerUser(
                "Awa",
                "Diallo",
                "awa@teranga.sn"
        );

        User ibrahima = creerUser(
                "Ibrahima",
                "Sarr",
                "ibrahima@teranga.sn"
        );

        User fatou = creerUser(
                "Fatou",
                "Ndiaye",
                "fatou@teranga.sn"
        );

        User cheikh = creerUser(
                "Cheikh",
                "Fall",
                "cheikh@teranga.sn"
        );

        User ndeye = creerUser(
                "Ndeye",
                "Mbaye",
                "ndeye@teranga.sn"
        );

        // -------------------------------------------------------------------
        // 2. Structure
        // -------------------------------------------------------------------

        Structure structure = new Structure();

        structure.setNom("Téranga Hub");
        structure.setSlug(STRUCTURE_SLUG);
        structure.setType("Incubateur");
        structure.setPays("Sénégal");
        structure.setVille("Dakar");
        structure.setEmail("contact@teranga-hub.sn");
        structure.setProprietaire(awa);

        structure = structureRepository.save(structure);

        // -------------------------------------------------------------------
        // 3. Membres de la structure
        // -------------------------------------------------------------------

        creerMembre(
                awa,
                structure,
                RoleMembreStructure.ADMIN_STRUCTURE
        );

        creerMembre(
                ibrahima,
                structure,
                RoleMembreStructure.COACH
        );

        creerMembre(
                fatou,
                structure,
                RoleMembreStructure.ENTREPRENEUR
        );

        creerMembre(
                cheikh,
                structure,
                RoleMembreStructure.ENTREPRENEUR
        );

        creerMembre(
                ndeye,
                structure,
                RoleMembreStructure.ENTREPRENEUR
        );

        // -------------------------------------------------------------------
        // 4. Parcours + phases
        // -------------------------------------------------------------------

        // Les services utilisent le tenant courant.
        TenantContext.setCurrentTenant(structure.getId());

        try {

            Parcours parcours = creerParcoursStandard(structure);

            /*
             * Les phases sont maintenant globales à la structure.
             *
             * L'ordre appartient à ParcoursPhase et non à Phase.
             *
             * On récupère donc les phases du parcours dans leur ordre
             * directement depuis ParcoursPhaseService.
             */
            List<PhaseResponse> phaseResponses =
                    parcoursPhaseService.getPhasesByParcours(
                            parcours.getId()
                    );

            if (phaseResponses.size() < 3) {
                throw new IllegalStateException(
                        "Le parcours doit contenir au moins 3 phases"
                );
            }

            Phase phase1 = getPhaseById(
                    phaseResponses.get(0).getId()
            );

            Phase phase2 = getPhaseById(
                    phaseResponses.get(1).getId()
            );

            Phase phase3 = getPhaseById(
                    phaseResponses.get(2).getId()
            );

            // ----------------------------------------------------------------
            // 5. Cohortes
            // ----------------------------------------------------------------

            Cohorte cohorteA = creerCohorte(
                    "Cohorte A",
                    structure,
                    parcours,
                    phase1,
                    LocalDate.of(2026, 1, 15),
                    LocalDate.of(2026, 6, 30)
            );

            Cohorte cohorteB = creerCohorte(
                    "Cohorte B",
                    structure,
                    parcours,
                    phase1,
                    LocalDate.of(2026, 2, 1),
                    LocalDate.of(2026, 7, 31)
            );

            Cohorte cohorteC = creerCohorte(
                    "Cohorte C",
                    structure,
                    parcours,
                    phase2,
                    LocalDate.of(2026, 7, 1),
                    LocalDate.of(2026, 12, 31)
            );

            // Évite un warning inutile si phase3 n'est pas utilisée
            log.debug(
                    "[Seeder] Phase disponible : {} ({})",
                    phase3.getNom(),
                    phase3.getId()
            );

            // ----------------------------------------------------------------
            // 6. Missions pour Cohorte A
            // ----------------------------------------------------------------

            creerMissions(
                    cohorteA,
                    structure
            );

            // ----------------------------------------------------------------
            // 7. Projets entrepreneurs
            // ----------------------------------------------------------------

            Projet projetFatou = creerProjet(
                    "AgriFresh",
                    "Plateforme agri-tech Sénégal",
                    fatou,
                    structure
            );

            Projet projetCheikh = creerProjet(
                    "EduKids",
                    "Application éducative pour enfants",
                    cheikh,
                    structure
            );

            Projet projetNdeye = creerProjet(
                    "HealthMobile",
                    "Santé mobile en zones rurales",
                    ndeye,
                    structure
            );

            // ----------------------------------------------------------------
            // 8. Participations aux cohortes
            // ----------------------------------------------------------------

            // L'ouverture de participation assigne également
            // les missions correspondantes.
            participationService.ouvrirParticipation(
                    projetFatou,
                    cohorteA,
                    awa
            );

            participationService.ouvrirParticipation(
                    projetCheikh,
                    cohorteA,
                    awa
            );

            participationService.ouvrirParticipation(
                    projetNdeye,
                    cohorteA,
                    awa
            );

            // ----------------------------------------------------------------
            // 9. Logs de confirmation
            // ----------------------------------------------------------------

            log.info(
                    "[Seeder] Scénario Téranga Hub créé avec succès !"
            );

            log.info(
                    "[Seeder] Connexions de démonstration :"
            );

            log.info(
                    "[Seeder] awa@teranga.sn / {} → ADMIN",
                    DEMO_PASSWORD
            );

            log.info(
                    "[Seeder] ibrahima@teranga.sn / {} → COACH",
                    DEMO_PASSWORD
            );

            log.info(
                    "[Seeder] fatou@teranga.sn / {} → ENTREPRENEUR (projet AgriFresh)",
                    DEMO_PASSWORD
            );

            log.info(
                    "[Seeder] cheikh@teranga.sn / {} → ENTREPRENEUR (projet EduKids)",
                    DEMO_PASSWORD
            );

            log.info(
                    "[Seeder] ndeye@teranga.sn / {} → ENTREPRENEUR (projet HealthMobile)",
                    DEMO_PASSWORD
            );

        } finally {
            TenantContext.clear();
        }
    }

    // =======================================================================
    // Helpers
    // =======================================================================

    private User creerUser(
            String prenom,
            String nom,
            String email
    ) {

        return userRepository.findByEmail(email)
                .orElseGet(() -> {

                    User user = new User();

                    user.setPrenom(prenom);
                    user.setNom(nom);
                    user.setEmail(email);
                    user.setPassword(
                            passwordEncoder.encode(DEMO_PASSWORD)
                    );
                    user.setEmailVerified(true);

                    return userRepository.save(user);
                });
    }

    private void creerMembre(
            User user,
            Structure structure,
            RoleMembreStructure role
    ) {

        if (membreStructureRepository
                .findByUserIdAndStructureId(
                        user.getId(),
                        structure.getId()
                )
                .isEmpty()) {

            MembreStructure membre = new MembreStructure();

            membre.setUser(user);
            membre.setStructure(structure);
            membre.setRole(role);
            membre.setStatut(StatutMembre.ACCEPTE);

            membreStructureRepository.save(membre);
        }
    }

    /**
     * Création du parcours de démonstration.
     *
     * Les phases sont maintenant des entités globales à la structure.
     * Elles sont ensuite associées au parcours via ParcoursPhase.
     *
     * Exemple :
     *
     * Structure
     *   ├── Pré-incubation
     *   ├── Incubation
     *   └── Post-incubation
     *
     * Parcours "Programme Standard"
     *   ├── Pré-incubation  → ordre 1
     *   ├── Incubation      → ordre 2
     *   └── Post-incubation → ordre 3
     */
    private Parcours creerParcoursStandard(
            Structure structure
    ) {

        // ---------------------------------------------------------------
        // 1. Création du parcours
        // ---------------------------------------------------------------

        CreateParcoursRequest parcoursRequest =
                new CreateParcoursRequest();

        parcoursRequest.setNom(
                "Programme Standard"
        );

        parcoursRequest.setDescription(
                "Parcours d'incubation en 3 phases"
        );

        parcoursService.createParcours(
                parcoursRequest
        );

        // ---------------------------------------------------------------
        // 2. Récupération du parcours créé
        // ---------------------------------------------------------------

        Parcours parcours =
                parcoursRepository
                        .findByStructureIdAndArchiveFalse(
                                structure.getId()
                        )
                        .stream()
                        .filter(p ->
                                "Programme Standard"
                                        .equals(p.getNom())
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Parcours non créé"
                                )
                        );

        // ---------------------------------------------------------------
        // 3. Création des phases globales + association au parcours
        // ---------------------------------------------------------------

        creerPhase(
                parcours,
                "Pré-incubation",
                "Phase de validation du concept"
        );

        creerPhase(
                parcours,
                "Incubation",
                "Phase principale de développement"
        );

        creerPhase(
                parcours,
                "Post-incubation",
                "Phase de consolidation et scaling"
        );

        // ---------------------------------------------------------------
        // 4. Retour du parcours
        // ---------------------------------------------------------------

        return parcoursRepository
                .findById(parcours.getId())
                .orElseThrow(() ->
                        new RuntimeException(
                                "Parcours introuvable après création"
                        )
                );
    }

    /**
     * Crée une phase globale dans la structure puis l'ajoute au parcours.
     *
     * Important :
     * - Phase ne possède plus de parcoursId.
     * - Phase ne possède plus d'ordre.
     * - L'association et l'ordre sont gérés par ParcoursPhase.
     */
    private Phase creerPhase(
            Parcours parcours,
            String nom,
            String description
    ) {

        CreatePhaseRequest request =
                new CreatePhaseRequest();

        request.setNom(nom);
        request.setDescription(description);

        // Création globale de la phase dans la structure.
        PhaseResponse phaseResponse =
                phaseService.createPhase(request);

        Phase phase =
                getPhaseById(phaseResponse.getId());

        // Association de la phase au parcours.
        // L'ordre est automatiquement déterminé par
        // ParcoursPhaseService.
        parcoursPhaseService.ajouterPhase(
                parcours.getId(),
                phase.getId()
        );

        return phase;
    }

    /**
     * Récupère une phase appartenant à la structure courante.
     */
    private Phase getPhaseById(UUID phaseId) {

        return phaseRepository
                .findById(phaseId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Phase introuvable : " + phaseId
                        )
                );
    }

    private Cohorte creerCohorte(
            String nom,
            Structure structure,
            Parcours parcours,
            Phase phase,
            LocalDate debut,
            LocalDate fin
    ) {

        Cohorte cohorte = new Cohorte();

        cohorte.setNom(nom);
        cohorte.setStatut(StatutCohorte.EN_COURS);
        cohorte.setStructure(structure);
        cohorte.setParcours(parcours);
        cohorte.setPhase(phase);
        cohorte.setDateDebut(debut);
        cohorte.setDateFin(fin);

        return cohorteRepository.save(cohorte);
    }

    private void creerMissions(
            Cohorte cohorte,
            Structure structure
    ) {

        String[] titres = {
                "Valider le problème utilisateur",
                "Construire un MVP (Minimum Viable Product)"
        };

        for (String titre : titres) {

            MissionCohorte mission =
                    new MissionCohorte();

            mission.setTitre(titre);
            mission.setCohorte(cohorte);
            mission.setStructure(structure);
            mission.setPriorite(PrioriteMission.HAUTE);
            mission.setDateEcheance(
                    LocalDate.of(2026, 3, 31)
            );

            missionCohorteRepository.save(mission);
        }
    }

    private Projet creerProjet(
            String nom,
            String description,
            User entrepreneur,
            Structure structure
    ) {

        Projet projet = new Projet();

        projet.setNom(nom);
        projet.setDescription(description);
        projet.setEntrepreneur(entrepreneur);
        projet.setStructure(structure);
        projet.setStatut(StatutProjet.ACTIF);

        return projetRepository.save(projet);
    }
}