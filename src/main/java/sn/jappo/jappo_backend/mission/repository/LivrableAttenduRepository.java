package sn.jappo.jappo_backend.mission.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.mission.entity.LivrableAttendu;

public interface LivrableAttenduRepository extends JpaRepository<LivrableAttendu, UUID> {
    
    List<LivrableAttendu> findByMissionEtapeId(UUID missionEtapeId);

    long countByMissionEtapeId(UUID missionEtapeId);
    
    List<LivrableAttendu> findByMissionEtapeIdOrderByOrdreAsc(UUID missionEtapeId);
    
    List<LivrableAttendu> findByMissionCohorteId(UUID missionCohorteId);

    long countByMissionCohorteId(UUID missionCohorteId);
    
    List<LivrableAttendu> findByMissionCohorteIdOrderByOrdreAsc(UUID missionCohorteId);
}
