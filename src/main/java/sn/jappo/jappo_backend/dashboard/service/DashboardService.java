package sn.jappo.jappo_backend.dashboard.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.dashboard.dto.AlerteProjetResponse;
import sn.jappo.jappo_backend.dashboard.dto.DashboardStatsResponse;
import sn.jappo.jappo_backend.dashboard.dto.LivrableRecentResponse;
import sn.jappo.jappo_backend.dashboard.dto.ProjetsParPhaseResponse;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import sn.jappo.jappo_backend.livrable.repository.LivrableRepository;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@Service
public class DashboardService {

    private final UserRepository userRepository;
    private final CohorteRepository cohorteRepository;
    private final ProjetRepository projetRepository;
    private final LivrableRepository livrableRepository;
    private final ParcoursPhaseRepository parcoursPhaseRepository;

    public DashboardService(
            UserRepository userRepository,
            CohorteRepository cohorteRepository,
            ProjetRepository projetRepository,
            LivrableRepository livrableRepository,
            ParcoursPhaseRepository parcoursPhaseRepository
    ) {
        this.userRepository = userRepository;
        this.cohorteRepository = cohorteRepository;
        this.projetRepository = projetRepository;
        this.livrableRepository = livrableRepository;
        this.parcoursPhaseRepository = parcoursPhaseRepository;
    }

    @Transactional(readOnly = true)
    public DashboardStatsResponse getStatsForActiveStructure() {
        UUID structureId = getRequiredTenantId();

        long totalEntrepreneurs =
                userRepository.countEntrepreneursByStructureId(structureId);

        long actifs =
                userRepository.countEntrepreneursActifsByStructureId(structureId);

        long enAttente = totalEntrepreneurs - actifs;

        long cohortes =
                cohorteRepository.countByStructureId(structureId);

        Integer maturiteMoyenne =
                projetRepository.findAverageScoreMaturiteByStructureId(structureId);

        int scoreMoyen =
                maturiteMoyenne != null ? maturiteMoyenne : 0;

        long projetsAttention =
                projetRepository.countProjetsAttentionByStructureId(
                        structureId,
                        40
                );

        long livrablesEnAttente =
                livrableRepository.countByStructureIdAndStatut(
                        structureId,
                        StatutLivrable.EN_ATTENTE
                );

        List<ProjetsParPhaseResponse> projetsParPhase =
                compterProjetsParPhase(structureId);

        long projetsEnRetard =
                projetRepository.findProjetsEnRetard(structureId).size();

        return new DashboardStatsResponse(
                totalEntrepreneurs,
                actifs,
                enAttente,
                cohortes,
                scoreMoyen,
                projetsAttention,
                livrablesEnAttente,
                projetsParPhase,
                projetsEnRetard
        );
    }

    private List<ProjetsParPhaseResponse> compterProjetsParPhase(
            UUID structureId) {

        Map<UUID, ProjetsParPhaseResponse> parPhase =
                new LinkedHashMap<>();

        for (Projet projet :
                projetRepository.findAllByStructureIdAndArchive(
                        structureId,
                        false
                )) {

            if (projet.getCohorte() == null
                    || projet.getCohorte().getPhase() == null
                    || projet.getCohorte().getParcours() == null) {
                continue;
            }

            Phase phase =
                    projet.getCohorte().getPhase();

            UUID parcoursId =
                    projet.getCohorte()
                            .getParcours()
                            .getId();

            Integer ordre =
                    parcoursPhaseRepository
                            .findByParcours_IdAndPhase_Id(
                                    parcoursId,
                                    phase.getId()
                            )
                            .map(ParcoursPhase::getOrdre)
                            .orElse(null);

            parPhase.merge(
                    phase.getId(),
                    new ProjetsParPhaseResponse(
                            phase.getId(),
                            phase.getNom(),
                            ordre,
                            1
                    ),
                    (existant, ignore) ->
                            new ProjetsParPhaseResponse(
                                    existant.phaseId(),
                                    existant.nomPhase(),
                                    existant.ordre(),
                                    existant.nombre() + 1
                            )
            );
        }

        return parPhase.values()
                .stream()
                .sorted(
                        Comparator.comparing(
                                ProjetsParPhaseResponse::ordre,
                                Comparator.nullsLast(
                                        Integer::compareTo
                                )
                        )
                )
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AlerteProjetResponse>
    getProjetsAlertesForActiveStructure() {

        UUID structureId = getRequiredTenantId();

        return projetRepository
                .findAllByStructureId(structureId)
                .stream()
                .filter(p ->
                        p.getScoreMaturite() != null
                                && p.getScoreMaturite() < 40
                )
                .map(p -> {

                    String nomEntrepreneur =
                            p.getEntrepreneur() != null
                                    ? (
                                        p.getEntrepreneur().getPrenom() != null
                                                ? p.getEntrepreneur().getPrenom() + " "
                                                : ""
                                    )
                                    + (
                                        p.getEntrepreneur().getNom() != null
                                                ? p.getEntrepreneur().getNom()
                                                : ""
                                    )
                                    : "Non assigné";

                    String email =
                            p.getEntrepreneur() != null
                                    ? p.getEntrepreneur().getEmail()
                                    : null;

                    UUID entrepreneurId =
                            p.getEntrepreneur() != null
                                    ? p.getEntrepreneur().getId()
                                    : null;

                    String nomCohorte =
                            p.getCohorte() != null
                                    ? p.getCohorte().getNom()
                                    : "—";

                    return new AlerteProjetResponse(
                            p.getId(),
                            p.getNom(),
                            entrepreneurId,
                            nomEntrepreneur.trim(),
                            email,
                            nomCohorte,
                            p.getScoreMaturite(),
                            "Maturité faible (< 40%)"
                    );
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LivrableRecentResponse>
    getLivrablesRecentsForActiveStructure(int limit) {

        UUID structureId = getRequiredTenantId();

        return livrableRepository
                .findAllByStructureIdOrderByDateDepotDesc(
                        structureId,
                        PageRequest.of(0, limit)
                )
                .stream()
                .map(l -> {

                    String nomProjet =
                            l.getProjet() != null
                                    ? l.getProjet().getNom()
                                    : "—";

                    String nomEntrepreneur =
                            (
                                l.getProjet() != null
                                && l.getProjet().getEntrepreneur() != null
                            )
                                    ? (
                                        l.getProjet()
                                                .getEntrepreneur()
                                                .getPrenom() != null
                                                ? l.getProjet()
                                                        .getEntrepreneur()
                                                        .getPrenom() + " "
                                                : ""
                                    )
                                    + (
                                        l.getProjet()
                                                .getEntrepreneur()
                                                .getNom() != null
                                                ? l.getProjet()
                                                        .getEntrepreneur()
                                                        .getNom()
                                                : ""
                                    )
                                    : "—";

                    UUID missionId =
                            l.getMissionProjet() != null
                                    ? l.getMissionProjet().getId()
                                    : null;

                    return new LivrableRecentResponse(
                            l.getId(),
                            l.getNom(),
                            l.getProjet() != null
                                    ? l.getProjet().getId()
                                    : null,
                            nomProjet,
                            nomEntrepreneur.trim(),
                            l.getStatut(),
                            l.getDateDepot(),
                            missionId
                    );
                })
                .toList();
    }

    private UUID getRequiredTenantId() {

        UUID tenantId =
                TenantContext.getCurrentTenant();

        if (tenantId == null) {
            throw new IllegalStateException(
                    "Aucune structure active sélectionnée "
                            + "(en-tête X-Structure-Id manquant)"
            );
        }

        return tenantId;
    }
}