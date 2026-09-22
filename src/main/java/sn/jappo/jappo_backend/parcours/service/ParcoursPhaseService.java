package sn.jappo.jappo_backend.parcours.service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.parcours.dto.PhaseResponse;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;

@Service
@RequiredArgsConstructor
public class ParcoursPhaseService {

    private final ParcoursPhaseRepository parcoursPhaseRepository;
    private final ParcoursRepository parcoursRepository;
    private final PhaseRepository phaseRepository;

    // -----------------------------------------------------------------------
    // LECTURE
    // -----------------------------------------------------------------------

    /**
     * Récupérer les phases d'un parcours dans leur ordre.
     */
    @Transactional(readOnly = true)
    public List<PhaseResponse> getPhasesByParcours(
            UUID parcoursId
    ) {

        UUID structureId = getRequiredTenantId();

        getParcoursForTenant(
                parcoursId,
                structureId
        );

        return parcoursPhaseRepository
                .findByParcours_IdOrderByOrdreAsc(parcoursId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }



    @Transactional
public List<PhaseResponse> reorganiserPhases(
        UUID parcoursId,
        List<UUID> phaseIds
) {

    UUID structureId = getRequiredTenantId();

    getParcoursForTenant(
            parcoursId,
            structureId
    );

    if (phaseIds == null || phaseIds.isEmpty()) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La liste des phases ne peut pas être vide"
        );
    }

    // ---------------------------------------------------------------
    // Récupérer les associations actuelles
    // ---------------------------------------------------------------

    List<ParcoursPhase> associations =
            parcoursPhaseRepository
                    .findByParcours_IdOrderByOrdreAsc(parcoursId);

    if (associations.size() != phaseIds.size()) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "La liste doit contenir toutes les phases du parcours"
        );
    }

    // ---------------------------------------------------------------
    // Vérifier les doublons dans la nouvelle liste
    // ---------------------------------------------------------------

    if (phaseIds.stream().distinct().count() != phaseIds.size()) {

        throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Une phase ne peut apparaître qu'une seule fois"
        );
    }

    // ---------------------------------------------------------------
    // Vérifier que toutes les phases appartiennent
    // réellement au parcours
    // ---------------------------------------------------------------

    var associationsByPhaseId = associations.stream()
            .collect(Collectors.toMap(
                    association -> association.getPhase().getId(),
                    association -> association
            ));

    for (UUID phaseId : phaseIds) {

        if (!associationsByPhaseId.containsKey(phaseId)) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Une phase fournie n'appartient pas à ce parcours"
            );
        }
    }

    // ---------------------------------------------------------------
    // ÉTAPE 1
    //
    // Donner temporairement des ordres négatifs.
    //
    // Cela évite les collisions avec la contrainte :
    //
    // (parcours_id, ordre)
    // ---------------------------------------------------------------

    for (int i = 0; i < associations.size(); i++) {

        ParcoursPhase association = associations.get(i);

        association.setOrdre(-(i + 1));
    }

    parcoursPhaseRepository.saveAll(associations);

    // ---------------------------------------------------------------
    // ÉTAPE 2
    //
    // Appliquer le nouvel ordre.
    // ---------------------------------------------------------------

    for (int i = 0; i < phaseIds.size(); i++) {

        UUID phaseId = phaseIds.get(i);

        ParcoursPhase association =
                associationsByPhaseId.get(phaseId);

        association.setOrdre(i + 1);
    }

    parcoursPhaseRepository.saveAll(associations);

    // ---------------------------------------------------------------
    // Retourner le nouveau parcours
    // ---------------------------------------------------------------

    return parcoursPhaseRepository
            .findByParcours_IdOrderByOrdreAsc(parcoursId)
            .stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList());
}

    // -----------------------------------------------------------------------
    // AJOUT
    // -----------------------------------------------------------------------

    /**
     * Ajouter une phase existante à un parcours.
     *
     * IMPORTANT :
     *
     * On ne crée PAS de nouvelle Phase.
     * On crée uniquement une ligne ParcoursPhase.
     */
    @Transactional
    public PhaseResponse ajouterPhase(
            UUID parcoursId,
            UUID phaseId
    ) {

        UUID structureId = getRequiredTenantId();

        Parcours parcours = getParcoursForTenant(
                parcoursId,
                structureId
        );

        Phase phase = getPhaseForTenant(
                phaseId,
                structureId
        );

        // ---------------------------------------------------------------
        // Vérifier que la phase n'est pas déjà dans le parcours
        // ---------------------------------------------------------------

        if (parcoursPhaseRepository
                .existsByParcours_IdAndPhase_Id(
                        parcoursId,
                        phaseId
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cette phase est déjà présente dans le parcours"
            );
        }

        // ---------------------------------------------------------------
        // Déterminer automatiquement le prochain ordre
        // ---------------------------------------------------------------

        List<ParcoursPhase> phases =
                parcoursPhaseRepository
                        .findByParcours_IdOrderByOrdreAsc(
                                parcoursId
                        );

        int ordre = phases.stream()
                .map(ParcoursPhase::getOrdre)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;

        // ---------------------------------------------------------------
        // Créer uniquement l'association
        // ---------------------------------------------------------------

        ParcoursPhase parcoursPhase = new ParcoursPhase();

        parcoursPhase.setParcours(parcours);
        parcoursPhase.setPhase(phase);
        parcoursPhase.setOrdre(ordre);

        parcoursPhaseRepository.save(parcoursPhase);

        return mapToResponse(parcoursPhase);
    }

    // -----------------------------------------------------------------------
    // SUPPRESSION DE L'ASSOCIATION
    // -----------------------------------------------------------------------

    /**
     * Retirer une phase d'un parcours.
     *
     * La Phase reste dans la bibliothèque globale.
     */
    @Transactional
    public void retirerPhase(
            UUID parcoursId,
            UUID phaseId
    ) {

        UUID structureId = getRequiredTenantId();

        getParcoursForTenant(
                parcoursId,
                structureId
        );

        getPhaseForTenant(
                phaseId,
                structureId
        );

        ParcoursPhase association =
                parcoursPhaseRepository
                        .findByParcours_IdAndPhase_Id(
                                parcoursId,
                                phaseId
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Cette phase n'est pas présente dans le parcours"
                        ));

        parcoursPhaseRepository.delete(association);
    }

    // -----------------------------------------------------------------------
    // ORDRE
    // -----------------------------------------------------------------------

    /**
     * Modifier l'ordre d'une phase dans un parcours.
     *
     * ATTENTION :
     * Cette méthode sera améliorée ensuite pour gérer proprement
     * les déplacements entre deux phases sans conflit de contrainte
     * unique.
     */
    @Transactional
    public PhaseResponse modifierOrdre(
            UUID parcoursId,
            UUID phaseId,
            Integer nouvelOrdre
    ) {

        UUID structureId = getRequiredTenantId();

        getParcoursForTenant(
                parcoursId,
                structureId
        );

        getPhaseForTenant(
                phaseId,
                structureId
        );

        if (nouvelOrdre == null || nouvelOrdre < 1) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "L'ordre doit être supérieur ou égal à 1"
            );
        }

        ParcoursPhase association =
                parcoursPhaseRepository
                        .findByParcours_IdAndPhase_Id(
                                parcoursId,
                                phaseId
                        )
                        .orElseThrow(() -> new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Cette phase n'est pas présente dans le parcours"
                        ));

        if (association.getOrdre().equals(nouvelOrdre)) {
            return mapToResponse(association);
        }

        /*
         * Pour l'instant on vérifie simplement que l'ordre
         * demandé n'est pas déjà occupé.
         *
         * La gestion complète du déplacement :
         *
         * 1 → A
         * 2 → B
         * 3 → C
         *
         * déplacer C vers 1
         *
         * deviendra :
         *
         * 1 → C
         * 2 → A
         * 3 → B
         *
         * et sera traitée avec une méthode de réorganisation
         * complète.
         */
        if (parcoursPhaseRepository
                .existsByParcours_IdAndOrdre(
                        parcoursId,
                        nouvelOrdre
                )) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cet ordre est déjà utilisé dans ce parcours"
            );
        }

        association.setOrdre(nouvelOrdre);

        return mapToResponse(
                parcoursPhaseRepository.save(association)
        );
    }

    // -----------------------------------------------------------------------
    // MAPPING
    // -----------------------------------------------------------------------

    private PhaseResponse mapToResponse(
            ParcoursPhase parcoursPhase
    ) {

        Phase phase = parcoursPhase.getPhase();

        PhaseResponse response = new PhaseResponse();

        response.setId(phase.getId());
        response.setNom(phase.getNom());
        response.setDescription(phase.getDescription());

        // Ici l'ordre vient de ParcoursPhase.
        response.setOrdre(parcoursPhase.getOrdre());

        response.setArchive(phase.isArchive());
        response.setDateCreation(phase.getDateCreation());
        response.setDateModification(phase.getDateModification());

        return response;
    }

    // -----------------------------------------------------------------------
    // TENANT - PARCOURS
    // -----------------------------------------------------------------------

    private Parcours getParcoursForTenant(
            UUID parcoursId,
            UUID structureId
    ) {

        return parcoursRepository
                .findByIdAndStructureId(
                        parcoursId,
                        structureId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Parcours introuvable"
                ));
    }

    // -----------------------------------------------------------------------
    // TENANT - PHASE
    // -----------------------------------------------------------------------

    private Phase getPhaseForTenant(
            UUID phaseId,
            UUID structureId
    ) {

        return phaseRepository
                .findByIdAndStructureId(
                        phaseId,
                        structureId
                )
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Phase introuvable"
                ));
    }

    // -----------------------------------------------------------------------
    // TENANT
    // -----------------------------------------------------------------------

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