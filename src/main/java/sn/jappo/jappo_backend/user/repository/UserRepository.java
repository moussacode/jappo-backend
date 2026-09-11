package sn.jappo.jappo_backend.user.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import sn.jappo.jappo_backend.user.entity.User;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByEmail(String email);

    Optional<User> findByEmail(String email);

    @Query("SELECT COUNT(m) FROM MembreStructure m WHERE m.structure.id = :structureId AND m.role = 'ENTREPRENEUR'")
long countEntrepreneursByStructureId(@Param("structureId") UUID structureId);

@Query("SELECT COUNT(m) FROM MembreStructure m WHERE m.structure.id = :structureId AND m.role = 'ENTREPRENEUR' AND m.statut = 'ACTIF'")
long countEntrepreneursActifsByStructureId(@Param("structureId") UUID structureId);
}