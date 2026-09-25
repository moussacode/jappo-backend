package sn.jappo.jappo_backend.abonnement.repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import sn.jappo.jappo_backend.abonnement.entity.StatutTransaction;
import sn.jappo.jappo_backend.abonnement.entity.TransactionPaiement;

public interface TransactionPaiementRepository
        extends JpaRepository<TransactionPaiement, UUID> {

    Optional<TransactionPaiement> findByRefCommand(String refCommand);

    Optional<TransactionPaiement> findByTokenPaiement(String tokenPaiement);

    Page<TransactionPaiement> findByStructureId(
            UUID structureId,
            Pageable pageable
    );

    Page<TransactionPaiement> findByStatut(
            StatutTransaction statut,
            Pageable pageable
    );

    Page<TransactionPaiement> findAllByOrderByDateCreationDesc(
            Pageable pageable
    );

    Page<TransactionPaiement> findByDateCreationBetween(
            LocalDateTime debut,
            LocalDateTime fin,
            Pageable pageable
    );

    Page<TransactionPaiement> findByStatutAndDateCreationBetween(
            StatutTransaction statut,
            LocalDateTime debut,
            LocalDateTime fin,
            Pageable pageable
    );

    Page<TransactionPaiement> findByStructureIdAndStatut(
            UUID structureId,
            StatutTransaction statut,
            Pageable pageable
    );

    long countByStatut(StatutTransaction statut);

    long countByStatutAndDateCreationBetween(
            StatutTransaction statut,
            LocalDateTime debut,
            LocalDateTime fin
    );

    @Query("""
        SELECT COALESCE(SUM(t.montant), 0)
        FROM TransactionPaiement t
        WHERE t.statut = :statut
    """)
    long sumMontantByStatut(
            @Param("statut") StatutTransaction statut
    );

    @Query("""
        SELECT COALESCE(SUM(t.montant), 0)
        FROM TransactionPaiement t
        WHERE t.statut = :statut
        AND t.dateCreation BETWEEN :debut AND :fin
    """)
    long sumMontantByStatutAndDateCreationBetween(
            @Param("statut") StatutTransaction statut,
            @Param("debut") LocalDateTime debut,
            @Param("fin") LocalDateTime fin
    );
}