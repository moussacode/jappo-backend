package sn.jappo.jappo_backend.livrable.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import sn.jappo.jappo_backend.livrable.entity.LivrableVersion;

public interface LivrableVersionRepository extends JpaRepository<LivrableVersion, UUID> {

    List<LivrableVersion> findAllByLivrableIdOrderByNumeroVersionDesc(UUID livrableId);

    Optional<LivrableVersion> findByLivrableIdAndNumeroVersion(UUID livrableId, Integer numeroVersion);
}
