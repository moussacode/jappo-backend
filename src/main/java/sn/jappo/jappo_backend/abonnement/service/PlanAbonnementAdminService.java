package sn.jappo.jappo_backend.abonnement.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementRequest;
import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementResponse;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.HistoriqueAbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.PlanAbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.TransactionPaiementRepository;

import java.util.List;
import java.util.UUID;

/**
 * Gestion des plans par le SUPER_ADMIN.
 *
 * Règles : un seul plan par défaut, toujours actif ; le code est immuable ;
 * un plan déjà utilisé (abonnement, historique ou transaction) ne peut pas être supprimé,
 * seulement désactivé.
 */
@Service
public class PlanAbonnementAdminService {

    private final PlanAbonnementRepository planRepository;
    private final AbonnementRepository abonnementRepository;
    private final HistoriqueAbonnementRepository historiqueRepository;
    private final TransactionPaiementRepository transactionRepository;

    public PlanAbonnementAdminService(
            PlanAbonnementRepository planRepository,
            AbonnementRepository abonnementRepository,
            HistoriqueAbonnementRepository historiqueRepository,
            TransactionPaiementRepository transactionRepository) {
        this.planRepository = planRepository;
        this.abonnementRepository = abonnementRepository;
        this.historiqueRepository = historiqueRepository;
        this.transactionRepository = transactionRepository;
    }

    @Transactional(readOnly = true)
    public List<PlanAbonnementResponse> lister() {
        return planRepository.findAllByOrderByOrdreAsc().stream()
                .map(PlanAbonnementResponse::from)
                .toList();
    }

    /** Plans proposés à la souscription (page tarifs). */
    @Transactional(readOnly = true)
    public List<PlanAbonnementResponse> listerActifs() {
        return planRepository.findAllByActifTrueOrderByOrdreAsc().stream()
                .map(PlanAbonnementResponse::from)
                .toList();
    }

    @Transactional
    public PlanAbonnementResponse creer(PlanAbonnementRequest request) {
        if (request.code() == null || request.code().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code du plan est obligatoire");
        }
        if (planRepository.existsByCode(request.code())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Un plan avec ce code existe déjà");
        }

        PlanAbonnement plan = new PlanAbonnement();
        plan.setCode(request.code());
        plan.setActif(true);
        appliquer(plan, request);

        if (plan.isEstDefaut()) {
            retirerAutresPlansParDefaut(null);
        }
        return PlanAbonnementResponse.from(planRepository.save(plan));
    }

    @Transactional
    public PlanAbonnementResponse modifier(UUID planId, PlanAbonnementRequest request) {
        PlanAbonnement plan = getPlan(planId);

        if (request.code() != null && !request.code().equals(plan.getCode())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le code d'un plan ne peut pas être modifié");
        }
        if (plan.isEstDefaut() && !request.estDefaut()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Désignez un autre plan par défaut avant de retirer ce statut à celui-ci");
        }
        if (request.estDefaut() && !plan.isActif()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Un plan désactivé ne peut pas être le plan par défaut");
        }

        appliquer(plan, request);
        if (plan.isEstDefaut()) {
            retirerAutresPlansParDefaut(plan.getId());
        }
        return PlanAbonnementResponse.from(planRepository.save(plan));
    }

    @Transactional
    public PlanAbonnementResponse activer(UUID planId) {
        PlanAbonnement plan = getPlan(planId);
        plan.setActif(true);
        return PlanAbonnementResponse.from(planRepository.save(plan));
    }

    /** Un plan désactivé n'est plus proposé ; ses abonnés gardent leurs droits jusqu'à l'échéance. */
    @Transactional
    public PlanAbonnementResponse desactiver(UUID planId) {
        PlanAbonnement plan = getPlan(planId);
        if (plan.isEstDefaut()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Le plan par défaut ne peut pas être désactivé");
        }
        plan.setActif(false);
        return PlanAbonnementResponse.from(planRepository.save(plan));
    }

    /** Suppression réservée aux plans jamais utilisés : sinon, il faut le désactiver. */
    @Transactional
    public void supprimer(UUID planId) {
        PlanAbonnement plan = getPlan(planId);
        if (plan.isEstDefaut()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Le plan par défaut ne peut pas être supprimé");
        }
        if (abonnementRepository.existsByPlanId(planId)
                || historiqueRepository.existsByPlanId(planId)
                || transactionRepository.existsByPlanViseId(planId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Ce plan est déjà utilisé : désactivez-le au lieu de le supprimer");
        }
        planRepository.delete(plan);
    }

    private PlanAbonnement getPlan(UUID planId) {
        return planRepository.findById(planId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan introuvable"));
    }

    private void appliquer(PlanAbonnement plan, PlanAbonnementRequest request) {
        plan.setLibelle(request.libelle().trim());
        plan.setDescription(request.description() == null || request.description().isBlank()
                ? null
                : request.description().trim());
        plan.setPrix(request.prix());
        plan.setDevise(request.devise());
        plan.setDureeJours(request.dureeJours());
        plan.setMaxCohortes(request.maxCohortes());
        plan.setIaCoachActive(request.iaCoachActive());
        plan.setIaEntrepreneurActive(request.iaEntrepreneurActive());
        plan.setIaTokensMensuels(request.iaTokensMensuels());
        plan.setEstDefaut(request.estDefaut());
        plan.setOrdre(request.ordre());
    }

    private void retirerAutresPlansParDefaut(UUID garde) {
        planRepository.findAllByOrderByOrdreAsc().stream()
                .filter(p -> p.isEstDefaut() && !p.getId().equals(garde))
                .forEach(p -> {
                    p.setEstDefaut(false);
                    planRepository.save(p);
                });
    }
}
