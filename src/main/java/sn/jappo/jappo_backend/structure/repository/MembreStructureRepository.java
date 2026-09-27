package sn.jappo.jappo_backend.structure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.user.entity.User;

public interface MembreStructureRepository extends JpaRepository<MembreStructure, UUID> {

    List<MembreStructure> findAllByUser(User user);

    List<MembreStructure> findAllByStructure(Structure structure);

    List<MembreStructure> findAllByUserAndRole(
            User user,
            RoleMembreStructure role
    );

 
    boolean existsByUserAndStructure(User user, Structure structure);

    // Recherche de l'invitation par token
    Optional<MembreStructure> findByInvitationToken(String token);

    List<MembreStructure> findByStructureIdAndRole(UUID structureId, RoleMembreStructure role);

    Optional<MembreStructure> findByUserIdAndStructureId(UUID userId, UUID structureId);


    long countByStructureId(UUID structureId);



     /**
     * Retourne uniquement les entrepreneurs appartenant
     * aux cohortes affectées au coach.
     */
    @Query("""
        SELECT ms
        FROM MembreStructure ms
        JOIN ms.cohorte c
        JOIN c.coachs coach
        WHERE ms.structure.id = :structureId
          AND ms.role = :role
          AND coach.id = :coachId
    """)
    List<MembreStructure> findEntrepreneursByCoachAndStructure(
            @Param("structureId") UUID structureId,
            @Param("coachId") UUID coachId,
            @Param("role") RoleMembreStructure role
    );

    /**
     * Retourne un entrepreneur uniquement si
     * le coach est affecté à sa cohorte.
     */
    @Query("""
        SELECT ms
        FROM MembreStructure ms
        JOIN ms.cohorte c
        JOIN c.coachs coach
        WHERE ms.user.id = :userId
          AND ms.structure.id = :structureId
          AND ms.role = :role
          AND coach.id = :coachId
    """)
    Optional<MembreStructure> findEntrepreneurByUserIdAndCoach(
            @Param("userId") UUID userId,
            @Param("structureId") UUID structureId,
            @Param("coachId") UUID coachId,
            @Param("role") RoleMembreStructure role
    );

    // Dans sn.jappo.jappo_backend.structure.repository.MembreStructureRepository

@Query("SELECT COUNT(ms) FROM MembreStructure ms " +
       "WHERE ms.structure.id = :structureId " +
       "AND ms.role = 'ENTREPRENEUR' " +
       "AND ms.statut = 'ACCEPTE'")
long countEntrepreneursByStructureId(@Param("structureId") UUID structureId);
}