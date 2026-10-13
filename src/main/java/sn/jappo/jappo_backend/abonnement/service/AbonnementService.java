package sn.jappo.jappo_backend.abonnement.service;

import sn.jappo.jappo_backend.abonnement.dto.AbonnementResponse;
import sn.jappo.jappo_backend.abonnement.dto.ConfirmationPaiementResponse;
import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementResponse;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jappo.jappo_backend.abonnement.entity.*;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.TransactionPaiementRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.abonnement.entity.TransactionPaiement;
import java.time.LocalDateTime;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

import java.util.Map;
import java.util.UUID;

@Service
public class AbonnementService {

        private final AbonnementRepository abonnementRepository;
        private final TransactionPaiementRepository transactionRepository;
        private final StructureRepository structureRepository;
        private final PaydunyaClient paydunyaClient;
        private final MembreStructureRepository membreStructureRepository;
        private final PlanAbonnementService planService;

        public AbonnementService(
                        AbonnementRepository abonnementRepository,
                        TransactionPaiementRepository transactionRepository,
                        StructureRepository structureRepository,
                        PaydunyaClient paydunyaClient,
                        MembreStructureRepository membreStructureRepository,
                        PlanAbonnementService planService) {

                this.abonnementRepository = abonnementRepository;
                this.transactionRepository = transactionRepository;
                this.structureRepository = structureRepository;
                this.paydunyaClient = paydunyaClient;
                this.membreStructureRepository = membreStructureRepository;
                this.planService = planService;
        }

        @Transactional
        public Abonnement getOuCreerAbonnement(UUID structureId) {
                return abonnementRepository.findByStructureId(structureId)
                                .orElseGet(() -> {
                                        Structure structure = structureRepository.findById(structureId)
                                                        .orElseThrow(() -> new IllegalArgumentException(
                                                                        "Structure introuvable"));
                                        Abonnement a = new Abonnement();
                                        a.setStructure(structure);
                                        PlanAbonnement planParDefaut = planService.getPlanParDefaut();
                                        a.setPlan(planParDefaut);
                                        a.setStatut(StatutAbonnement.ACTIF);
                                        a.setDateDebut(LocalDateTime.now());
                                        a.setDateFin(planService.calculerDateFin(planParDefaut, a.getDateDebut()));
                                        return abonnementRepository.save(a);
                                });
        }

        @Transactional
        public AbonnementResponse getAbonnement(UUID structureId) {
                Abonnement abonnement = getOuCreerAbonnement(structureId);
                return new AbonnementResponse(
                                abonnement.getId(),
                                PlanAbonnementResponse.from(abonnement.getPlan()),
                                PlanAbonnementResponse.from(planService.planEffectif(abonnement)),
                                abonnement.getStatut().name(),
                                abonnement.getDateDebut(),
                                abonnement.getDateFin(),
                                abonnement.isRenouvellementAuto(),
                                abonnement.getDateCreation());
        }

        @Transactional
        public String initierPaiement(UUID structureId, UUID planId) {

                Structure structure = structureRepository.findById(structureId)
                                .orElseThrow(() -> new IllegalArgumentException("Structure introuvable"));

                // Le prix vient toujours du plan en base, jamais du client.
                PlanAbonnement plan = planService.getPlan(planId);
                if (!plan.isActif()) {
                        throw new IllegalArgumentException("Ce plan n'est plus proposé");
                }
                if (plan.estGratuit()) {
                        throw new IllegalArgumentException("Ce plan est gratuit : aucun paiement n'est nécessaire");
                }

                String refCommand = "JAPPO-" + structureId + "-" + System.currentTimeMillis();

                TransactionPaiement tx = new TransactionPaiement();

                tx.setStructure(structure);
                tx.setRefCommand(refCommand);
                tx.setPlanVise(plan);
                tx.setMontant(plan.getPrix());
                tx.setDevise(plan.getDevise());
                tx.setStatut(StatutTransaction.EN_ATTENTE);

                transactionRepository.save(tx);

                PaydunyaClient.InitiationResult result = paydunyaClient.initierPaiement(
                                "Abonnement " + plan.getLibelle() + " JAPPO",
                                plan.getPrix(),
                                refCommand,
                                "",
                                "",
                                "");

                if (!result.success()) {

                        tx.setStatut(StatutTransaction.ECHEC);
                        transactionRepository.save(tx);

                        throw new IllegalStateException(
                                        "Échec initiation PayDunya : " + result.message());
                }

                tx.setTokenPaiement(result.token());

                transactionRepository.save(tx);

                return result.redirectUrl();
        }

        @Transactional
        public ConfirmationPaiementResponse confirmerPaiement(String token) {

                TransactionPaiement transaction = transactionRepository.findByTokenPaiement(token)
                                .orElseThrow(() -> new IllegalArgumentException(
                                                "Transaction introuvable pour ce token"));

                // Utilisateur actuellement connecté
                Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

                if (authentication == null
                                || !(authentication.getPrincipal() instanceof User user)) {
                        throw new IllegalStateException("Utilisateur non authentifié");
                }

                // La structure vient de la transaction, pas de la structure active du frontend
                UUID structureId = transaction.getStructure().getId();

                // Vérifier que l'utilisateur appartient à cette structure
                MembreStructure membre = membreStructureRepository
                                .findByUserIdAndStructureId(user.getId(), structureId)
                                .orElseThrow(() -> new IllegalStateException(
                                                "Vous n'appartenez pas à la structure concernée par ce paiement"));

                // Seuls les membres acceptés peuvent confirmer
                if (membre.getStatut() != StatutMembre.ACCEPTE) {
                        throw new IllegalStateException(
                                        "Votre appartenance à cette structure n'est pas active");
                }

                // Idempotence : si déjà traité, on ne recommence pas
                if (transaction.getStatut() == StatutTransaction.SUCCES) {
                        return new ConfirmationPaiementResponse(
                                        "Paiement déjà confirmé",
                                        transaction.getStatut(),
                                        PlanAbonnementResponse.from(transaction.getPlanVise()),
                                        transaction.getMontant(),
                                        transaction.getDateConfirmation());
                }

                PaydunyaClient.PaymentStatusResult resultat = paydunyaClient.verifierPaiement(token);

                if (!resultat.success()) {
                        throw new IllegalStateException(
                                        "Impossible de vérifier le paiement PayDunya : "
                                                        + resultat.message());
                }

                if (!"completed".equalsIgnoreCase(resultat.status())) {
                        throw new IllegalStateException(
                                        "Paiement non terminé. Statut PayDunya : "
                                                        + resultat.status());
                }

                if (resultat.amount() != transaction.getMontant()) {
                        throw new IllegalStateException(
                                        "Montant du paiement incorrect");
                }

                if (!transaction.getRefCommand().equals(resultat.refCommand())) {
                        throw new IllegalStateException(
                                        "Référence de transaction incorrecte");
                }

                // 1. Valider la transaction
                transaction.setStatut(StatutTransaction.SUCCES);
                transaction.setDateConfirmation(LocalDateTime.now());

                transactionRepository.save(transaction);

                // 2. Activer le plan payé pour la durée qu'il définit
                LocalDateTime maintenant = LocalDateTime.now();

                Abonnement abonnement = getOuCreerAbonnement(
                                transaction.getStructure().getId());

                PlanAbonnement planVise = transaction.getPlanVise();
                abonnement.setPlan(planVise);
                abonnement.setStatut(StatutAbonnement.ACTIF);
                abonnement.setDateDebut(maintenant);
                abonnement.setDateFin(planService.calculerDateFin(planVise, maintenant));
                abonnement.setRenouvellementAuto(false);

                abonnementRepository.save(abonnement);

                return new ConfirmationPaiementResponse(
                                "Paiement confirmé",
                                transaction.getStatut(),
                                PlanAbonnementResponse.from(transaction.getPlanVise()),
                                transaction.getMontant(),
                                transaction.getDateConfirmation());
        }
}