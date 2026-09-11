package sn.jappo.jappo_backend.structure.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

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
}