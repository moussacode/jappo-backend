package sn.jappo.jappo_backend.livrable.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
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
}