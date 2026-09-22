package sn.jappo.jappo_backend.livrable.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.jappo.jappo_backend.livrable.entity.StatutLivrable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.livrable.entity.Livrable;

public interface LivrableRepository extends JpaRepository<Livrable, UUID> {

    List<Livrable> findAllByStructureId(UUID structureId);

    List<Livrable> findAllByMissionProjetIdAndStructureId(UUID missionProjetId, UUID structureId);

    List<Livrable> findAllByProjetIdAndStructureId(UUID projetId, UUID structureId);

    List<Livrable> findAllByStructureIdOrderByDateDepotDesc(UUID structureId, Pageable pageable);

    long countByStructureIdAndStatut(UUID structureId, StatutLivrable statut);

    Optional<Livrable> findByIdAndStructureId(UUID id, UUID structureId);

    long countByProjetIdAndStructureIdAndStatut(UUID projetId, UUID structureId, StatutLivrable statut);

    @Query("SELECT COUNT(l) > 0 FROM Livrable l WHERE l.missionProjet.id = :missionProjetId AND l.structure.id = :structureId")
    boolean hasLivrableForMission(@Param("missionProjetId") UUID missionProjetId, @Param("structureId") UUID structureId);

    /**
     * Vérifie si au moins un livrable a été soumis pour n'importe quelle
     * MissionProjet rattachée à la MissionCohorte donnée.
     * Un livrable existant dans le système = déjà soumis (statuts : EN_ATTENTE, VALIDE, A_CORRIGER, REJETE).
     * Utilisé pour le verrouillage structural de la mission de cohorte.
     */
    @Query("""
            SELECT COUNT(l) > 0 FROM Livrable l
            WHERE l.missionProjet.missionCohorte.id = :missionCohorteId
            """)
    boolean existsSoumissionForMissionCohorte(@Param("missionCohorteId") UUID missionCohorteId);
}