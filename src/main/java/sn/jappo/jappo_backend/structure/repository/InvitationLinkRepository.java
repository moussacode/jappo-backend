package sn.jappo.jappo_backend.structure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import sn.jappo.jappo_backend.structure.entity.InvitationLink;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface InvitationLinkRepository extends JpaRepository<InvitationLink, UUID> {

    // Résout l'erreur de recherche du lien actif non expiré
    Optional<InvitationLink> findByStructureIdAndRoleAndActifTrueAndExpiresAtAfter(
            UUID structureId, RoleMembreStructure role, LocalDateTime now
    );

    // Résout l'erreur de désactivation des anciens liens
    @Modifying
    @Query("UPDATE InvitationLink l SET l.actif = false WHERE l.structure.id = :structureId AND l.role = :role AND l.actif = true")
    void desactiverAnciensLiens(@Param("structureId") UUID structureId, @Param("role") RoleMembreStructure role);
}