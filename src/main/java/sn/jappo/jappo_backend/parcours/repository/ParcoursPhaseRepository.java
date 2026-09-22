package sn.jappo.jappo_backend.parcours.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;

@Repository
public interface ParcoursPhaseRepository
        extends JpaRepository<ParcoursPhase, UUID> {

    /**
     * Toutes les phases d'un parcours dans l'ordre.
     */
    List<ParcoursPhase> findByParcours_IdOrderByOrdreAsc(
            UUID parcoursId
    );

    /**
     * Vérifie si une phase est déjà associée au parcours.
     */
    boolean existsByParcours_IdAndPhase_Id(
            UUID parcoursId,
            UUID phaseId
    );

    /**
     * Récupérer une association précise.
     */
    Optional<ParcoursPhase> findByParcours_IdAndPhase_Id(
            UUID parcoursId,
            UUID phaseId
    );

    /**
     * Vérifie si un ordre est déjà utilisé dans le parcours.
     */
    boolean existsByParcours_IdAndOrdre(
            UUID parcoursId,
            Integer ordre
    );

    /**
     * Récupérer une association par son ordre.
     */
    Optional<ParcoursPhase> findByParcours_IdAndOrdre(
            UUID parcoursId,
            Integer ordre
    );

    /**
     * Supprimer une phase d'un parcours.
     *
     * Attention : cela supprime uniquement l'association,
     * pas la Phase elle-même.
     */
    void deleteByParcours_IdAndPhase_Id(
            UUID parcoursId,
            UUID phaseId
    );
}