package sn.jappo.jappo_backend.abonnement.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import sn.jappo.jappo_backend.abonnement.entity.StatutTransaction;
import sn.jappo.jappo_backend.abonnement.entity.TransactionPaiement;

import java.util.Optional;
import java.util.UUID;
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
}