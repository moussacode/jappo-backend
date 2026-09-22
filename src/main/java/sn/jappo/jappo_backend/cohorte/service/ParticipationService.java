
package sn.jappo.jappo_backend.cohorte.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.cohorte.dto.CohorteResponse;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.entity.ParticipationCohorte;
import sn.jappo.jappo_backend.cohorte.entity.ParticipationCohorte.MotifSortie;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.cohorte.repository.ParticipationCohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.mission.entity.MissionCohorte;
import sn.jappo.jappo_backend.mission.entity.MissionProjet;
import sn.jappo.jappo_backend.mission.entity.StatutMission;
import sn.jappo.jappo_backend.mission.repository.MissionCohorteRepository;
import sn.jappo.jappo_backend.mission.repository.MissionProjetRepository;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.projet.dto.MissionNonValidee;
import sn.jappo.jappo_backend.projet.dto.ParticipationCohorteResponse;
import sn.jappo.jappo_backend.projet.dto.PromotionGroupeeResultat;
import sn.jappo.jappo_backend.projet.dto.PromouvoirProjetRequest;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.exception.PromotionBloqueeException;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

/**
 * Service central gérant les participations aux cohortes et les promotions.
 *
 * Invariant : au plus une participation active (dateSortie nulle) par projet.
 * Le champ Projet.cohorte est la copie dénormalisée, écrite UNIQUEMENT ici.
 */
@Service
@RequiredArgsConstructor
public class ParticipationService {

    private final ParticipationCohorteRepository participationRepository;
    private final ProjetRepository projetRepository;
    private final CohorteRepository cohorteRepository;
    private final StructureRepository structureRepository;
    private final MissionCohorteRepository missionCohorteRepository;
    private final MissionProjetRepository missionProjetRepository;
    private final CohorteService cohorteService;
    private final ParcoursPhaseRepository parcoursPhaseRepository;

    // -----------------------------------------------------------------------
    // Ouvrir / Fermer une participation
    // -----------------------------------------------------------------------

    /**
     * Ouvre une participation active pour un projet dans une cohorte.
     * Met à jour Projet.cohorte (copie dénormalisée).
     * Ajoute les missions de la cohorte au projet.
     */
    @Transactional
    public ParticipationCohorte ouvrirParticipation(
            Projet projet,
            Cohorte cohorte,
            User effectuePar
    ) {
        // Vérifier qu'il n'y a pas déjà une participation active
        participationRepository
                .findByProjetIdAndDateSortieIsNull(projet.getId())
                .ifPresent(p -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Le projet a déjà une participation active dans la cohorte : "
                                    + p.getCohorte().getNom()
                    );
                });

        ParticipationCohorte participation = new ParticipationCohorte();
        participation.setProjet(projet);
        participation.setCohorte(cohorte);
        participation.setDateEntree(LocalDateTime.now());
        participation.setEffectuePar(effectuePar);

        participation = participationRepository.save(participation);

        // Mettre à jour la copie dénormalisée
        projet.setCohorte(cohorte);
        projetRepository.save(projet);

        // Créer les MissionProjet pour toutes les missions de la cohorte
        assignerMissionsCohorteAuProjet(projet, cohorte);

        return participation;
    }

    /**
     * Ferme la participation active d'un projet
     * (motif RETIRE, PROMU, TERMINE).
     */
    @Transactional
    public void fermerParticipation(
            ParticipationCohorte participation,
            MotifSortie motif,
            String raison,
            User effectuePar
    ) {
        participation.setDateSortie(LocalDateTime.now());
        participation.setMotifSortie(motif);
        participation.setRaison(raison);
        participation.setEffectuePar(effectuePar);
        participationRepository.save(participation);
    }

    // -----------------------------------------------------------------------
    // Promotion individuelle
    // -----------------------------------------------------------------------

    /**
     * Promeut un projet vers une cohorte cible.
     *
     * Règle 4 : ADMIN_STRUCTURE et COACH uniquement
     * (vérifié en amont par @PreAuthorize ou contrôleur).
     *
     * Règle 5 : missions non validées → 409 sauf si forcer=true.
     *
     * Règle 6 : fermer participation en cours, ouvrir dans cohorte cible,
     * créer missions.
     */
    @Transactional
    public Projet promouvoir(
            UUID projetId,
            PromouvoirProjetRequest request,
            User currentUser
    ) {
        UUID structureId = getRequiredTenantId();

        Projet projet = projetRepository
                .findByIdAndStructureId(projetId, structureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Projet introuvable"
                ));

        if (projet.isArchive()) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Un projet archivé ne peut pas être promu"
            );
        }

        Cohorte cohorteCible = cohorteRepository
                .findByIdAndStructureId(
                        request.cohorteCibleId(),
                        structureId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Cohorte cible introuvable"
                ));

        validerCohorteCible(projet, cohorteCible);

        // Vérification des missions non validées
        List<MissionNonValidee> missionsNonValidees =
                getMissionsNonValidees(projet, structureId);

        if (!missionsNonValidees.isEmpty() && !request.forcer()) {
            throw new PromotionBloqueeException(missionsNonValidees);
        }

        // Fermer la participation en cours
        participationRepository
                .findByProjetIdAndDateSortieIsNull(projetId)
                .ifPresent(p ->
                        fermerParticipation(
                                p,
                                MotifSortie.PROMU,
                                request.raison(),
                                currentUser
                        )
                );

        // Ouvrir dans la cohorte cible
        // met à jour Projet.cohorte + crée missions
        ouvrirParticipation(
                projet,
                cohorteCible,
                currentUser
        );

        return projet;
    }

    /**
     * Promotion groupée de plusieurs projets vers une cohorte cible.
     * Renvoie un résultat par projet.
     */
    @Transactional
    public List<PromotionGroupeeResultat> promotionGroupee(
            List<UUID> projetIds,
            UUID cohorteCibleId,
            String raison,
            boolean forcer,
            User currentUser
    ) {
        List<PromotionGroupeeResultat> resultats = new ArrayList<>();

        PromouvoirProjetRequest baseRequest =
                new PromouvoirProjetRequest(
                        cohorteCibleId,
                        raison,
                        forcer
                );

        for (UUID projetId : projetIds) {
            try {
                Projet projet = promouvoir(
                        projetId,
                        baseRequest,
                        currentUser
                );

                resultats.add(
                        new PromotionGroupeeResultat(
                                projetId,
                                projet.getNom(),
                                true,
                                "Promu avec succès",
                                List.of()
                        )
                );

            } catch (PromotionBloqueeException e) {
                String nomProjet = projetRepository
                        .findById(projetId)
                        .map(Projet::getNom)
                        .orElse("Inconnu");

                List<String> missions = e
                        .getMissionsNonValidees()
                        .stream()
                        .map(MissionNonValidee::titre)
                        .toList();

                resultats.add(
                        new PromotionGroupeeResultat(
                                projetId,
                                nomProjet,
                                false,
                                e.getMessage(),
                                missions
                        )
                );

            } catch (ResponseStatusException e) {
                String nomProjet = projetRepository
                        .findById(projetId)
                        .map(Projet::getNom)
                        .orElse("Inconnu");

                List<MissionNonValidee> missionsNonValides =
                        getMissionsNonValidesSafe(projetId);

                List<String> missions = missionsNonValides
                        .stream()
                        .map(MissionNonValidee::titre)
                        .toList();

                resultats.add(
                        new PromotionGroupeeResultat(
                                projetId,
                                nomProjet,
                                false,
                                e.getReason(),
                                missions
                        )
                );
            }
        }

        return resultats;
    }

    // -----------------------------------------------------------------------
    // Historique
    // -----------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<ParticipationCohorteResponse> getHistorique(UUID projetId) {
        UUID structureId = getRequiredTenantId();

        projetRepository
                .findByIdAndStructureId(projetId, structureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Projet introuvable"
                ));

        return participationRepository
                .findByProjetIdOrderByDateEntreeDesc(projetId)
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    /**
     * Retourne les cohortes éligibles comme cibles pour la promotion d'un projet.
     *
     * Critères :
     * - même structure
     * - même parcours
     * - phase dans un ordre supérieur à la phase actuelle
     * - cohorte non archivée
     *
     * L'ordre est porté par ParcoursPhase et non par Phase.
     */
    @Transactional(readOnly = true)
    public List<CohorteResponse> getCohortesEligibles(UUID projetId) {
        UUID structureId = getRequiredTenantId();

        Projet projet = projetRepository
                .findByIdAndStructureId(projetId, structureId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Projet introuvable"
                ));

        if (projet.getCohorte() == null
                || projet.getCohorte().getParcours() == null
                || projet.getCohorte().getPhase() == null) {
            return List.of();
        }

        UUID parcoursId = projet.getCohorte().getParcours().getId();
        UUID phaseActuelleId = projet.getCohorte().getPhase().getId();

        int ordreActuel = getOrdrePhase(
                parcoursId,
                phaseActuelleId
        );

        return cohorteRepository
                .findAllByStructureIdAndParcours_Id(
                        structureId,
                        parcoursId
                )
                .stream()
                .filter(c -> c.getStatut() != StatutCohorte.ARCHIVEE)
                .filter(c -> c.getPhase() != null)
                .filter(c -> {
                    Integer ordreCible = getOrdrePhase(
                            parcoursId,
                            c.getPhase().getId()
                    );

                    return ordreCible > ordreActuel;
                })
                .map(cohorteService::mapToResponse)
                .toList();
    }

    // -----------------------------------------------------------------------
    // Helpers de validation
    // -----------------------------------------------------------------------

    private void validerCohorteCible(
            Projet projet,
            Cohorte cohorteCible
    ) {
        if (cohorteCible.getStatut() == StatutCohorte.ARCHIVEE) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "La cohorte cible est archivée"
            );
        }

        if (cohorteCible.getPhase() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cohorte cible n'a pas de phase définie"
            );
        }

        if (cohorteCible.getParcours() == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "La cohorte cible n'a pas de parcours défini"
            );
        }

        // Même parcours
        if (projet.getCohorte() != null) {
            if (projet.getCohorte().getParcours() == null
                    || projet.getCohorte().getPhase() == null) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La cohorte actuelle du projet est invalide"
                );
            }

            UUID parcoursProjet =
                    projet.getCohorte().getParcours().getId();

            UUID parcoursCible =
                    cohorteCible.getParcours().getId();

            if (!parcoursProjet.equals(parcoursCible)) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "Le projet et la cohorte cible ne sont pas dans le même parcours"
                );
            }

            // Phase strictement supérieure
            int ordreActuel = getOrdrePhase(
                    parcoursProjet,
                    projet.getCohorte().getPhase().getId()
            );

            int ordreCible = getOrdrePhase(
                    parcoursCible,
                    cohorteCible.getPhase().getId()
            );

            if (ordreCible <= ordreActuel) {
                throw new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La cohorte cible doit être dans une phase d'ordre strictement supérieur"
                );
            }
        }
    }

    private List<MissionNonValidee> getMissionsNonValidees(
            Projet projet,
            UUID structureId
    ) {
        if (projet.getCohorte() == null) {
            return List.of();
        }

        return missionProjetRepository
                .findAllByProjetIdAndStructureId(
                        projet.getId(),
                        structureId
                )
                .stream()
                .filter(mp ->
                        mp.getMissionCohorte().getCohorte() != null
                                && mp.getMissionCohorte()
                                        .getCohorte()
                                        .getId()
                                        .equals(projet.getCohorte().getId())
                )
                .filter(mp -> !mp.isArchive())
                .filter(mp -> mp.getStatut() != StatutMission.VALIDE)
                .map(mp ->
                        new MissionNonValidee(
                                mp.getMissionCohorte().getTitre(),
                                mp.getStatut() != null
                                        ? mp.getStatut().name()
                                        : null
                        )
                )
                .toList();
    }

    private List<MissionNonValidee> getMissionsNonValidesSafe(
            UUID projetId
    ) {
        try {
            UUID structureId = getRequiredTenantId();

            return projetRepository
                    .findById(projetId)
                    .map(p ->
                            getMissionsNonValidees(
                                    p,
                                    structureId
                            )
                    )
                    .orElse(List.of());

        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Assigne toutes les missions actives de la cohorte au projet.
     *
     * Une seule méthode utilisée par création, acceptation invitation,
     * et promotion.
     *
     * Évite les doublons grâce au existsBy check.
     */
    public void assignerMissionsCohorteAuProjet(
            Projet projet,
            Cohorte cohorte
    ) {
        Structure structure = projet.getStructure();

        List<MissionCohorte> missions = missionCohorteRepository
                .findAllByCohorteIdAndStructureId(
                        cohorte.getId(),
                        structure.getId()
                )
                .stream()
                .filter(m -> !m.isArchive())
                .toList();

        for (MissionCohorte mc : missions) {
            boolean existe =
                    missionProjetRepository
                            .existsByMissionCohorteIdAndProjetIdAndStructureId(
                                    mc.getId(),
                                    projet.getId(),
                                    structure.getId()
                            );

            if (!existe) {
                MissionProjet mp = new MissionProjet();
                mp.setMissionCohorte(mc);
                mp.setProjet(projet);
                mp.setStructure(structure);
                mp.setStatut(StatutMission.A_FAIRE);

                missionProjetRepository.save(mp);
            }
        }
    }

    // -----------------------------------------------------------------------
    // Mapping
    // -----------------------------------------------------------------------

    private ParticipationCohorteResponse mapToResponse(
            ParticipationCohorte p
    ) {
        Cohorte c = p.getCohorte();

        Integer ordre = null;

        if (c.getParcours() != null && c.getPhase() != null) {
            ordre = parcoursPhaseRepository
                    .findByParcours_IdAndPhase_Id(
                            c.getParcours().getId(),
                            c.getPhase().getId()
                    )
                    .map(ParcoursPhase::getOrdre)
                    .orElse(null);
        }

        return new ParticipationCohorteResponse(
                p.getId(),
                c.getId(),
                c.getNom(),
                c.getParcours().getId(),
                c.getParcours().getNom(),
                c.getPhase().getId(),
                c.getPhase().getNom(),
                ordre,
                p.getDateEntree(),
                p.getDateSortie(),
                p.getMotifSortie() != null
                        ? p.getMotifSortie().name()
                        : null,
                p.getRaison(),
                p.isActive()
        );
    }

    // -----------------------------------------------------------------------
    // Parcours / phases
    // -----------------------------------------------------------------------

    /**
     * Récupère l'ordre d'une phase dans un parcours.
     *
     * Une même Phase peut être utilisée dans plusieurs Parcours,
     * donc l'ordre doit toujours être recherché avec les deux UUID.
     */
    private int getOrdrePhase(
            UUID parcoursId,
            UUID phaseId
    ) {
        return parcoursPhaseRepository
                .findByParcours_IdAndPhase_Id(
                        parcoursId,
                        phaseId
                )
                .map(ParcoursPhase::getOrdre)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.CONFLICT,
                        "La phase sélectionnée n'appartient pas à ce parcours"
                ));
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();

        if (tenantId == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "En-tête X-Structure-Id manquant"
            );
        }

        return tenantId;
    }
}

