package sn.jappo.jappo_backend.abonnement.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.abonnement.dto.PlanAbonnementResponse;
import sn.jappo.jappo_backend.abonnement.dto.UsageAbonnementResponse;
import sn.jappo.jappo_backend.abonnement.entity.Abonnement;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.ia.entity.IaUsage;
import sn.jappo.jappo_backend.ia.repository.IaUsageRepository;
import sn.jappo.jappo_backend.ia.service.AiService;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Point unique de vérification des limites du plan effectif d'une structure.
 *
 * IA : (a) le plan autorise-t-il ce rôle ? puis (b) reste-t-il des tokens dans le pot
 * mensuel de la structure ? L'usage réel est enregistré après chaque réponse réussie ;
 * un léger dépassement est accepté, c'est le message suivant qui est bloqué.
 * Cohortes : seules les cohortes PLANIFIEE et EN_COURS comptent.
 */
@Service
public class QuotaService {

    private static final Logger log = LoggerFactory.getLogger(QuotaService.class);

    private static final Set<StatutCohorte> COHORTES_COMPTEES =
            EnumSet.of(StatutCohorte.PLANIFIEE, StatutCohorte.EN_COURS);

    private final PlanAbonnementService planService;
    private final AbonnementRepository abonnementRepository;
    private final CohorteRepository cohorteRepository;
    private final IaUsageRepository iaUsageRepository;
    private final MembreStructureRepository membreStructureRepository;

    public QuotaService(
            PlanAbonnementService planService,
            AbonnementRepository abonnementRepository,
            CohorteRepository cohorteRepository,
            IaUsageRepository iaUsageRepository,
            MembreStructureRepository membreStructureRepository) {
        this.planService = planService;
        this.abonnementRepository = abonnementRepository;
        this.cohorteRepository = cohorteRepository;
        this.iaUsageRepository = iaUsageRepository;
        this.membreStructureRepository = membreStructureRepository;
    }

    // ═══════════════════════════════════════════
    // IA
    // ═══════════════════════════════════════════

    /**
     * Vérifie qu'un utilisateur peut lancer un appel IA dans la structure.
     *
     * @return le rôle de l'utilisateur dans la structure, à enregistrer avec l'usage
     * @throws QuotaDepasseException 403 IA_ROLE_NON_AUTORISE ou 429 IA_QUOTA_EPUISE
     */
    @Transactional(readOnly = true)
    public RoleMembreStructure verifierAccesIA(UUID structureId, User utilisateur) {
        PlanAbonnement plan = planService.planEffectif(structureId);
        RoleMembreStructure role = roleActif(structureId, utilisateur);

        // a) Le plan autorise-t-il ce rôle à utiliser l'IA ?
        if (!roleAutorise(plan, role)) {
            throw new QuotaDepasseException(
                    HttpStatus.FORBIDDEN,
                    QuotaDepasseException.IA_ROLE_NON_AUTORISE,
                    role == RoleMembreStructure.ENTREPRENEUR
                            ? "L'assistant IA n'est pas inclus pour les entrepreneurs dans l'offre « "
                                    + plan.getLibelle() + " » de votre incubateur."
                            : "L'assistant IA n'est pas inclus dans l'offre « " + plan.getLibelle() + " ».",
                    null,
                    null);
        }

        // b) Reste-t-il des tokens dans le pot de la structure (mois calendaire) ?
        Long limite = plan.getIaTokensMensuels();
        if (limite != null) {
            long consomme = tokensConsommesCeMois(structureId);
            if (consomme >= limite) {
                throw new QuotaDepasseException(
                        HttpStatus.TOO_MANY_REQUESTS,
                        QuotaDepasseException.IA_QUOTA_EPUISE,
                        "Le quota IA mensuel de votre structure est épuisé. Il sera renouvelé le 1er du mois prochain.",
                        consomme,
                        limite);
            }
        }
        return role;
    }

    /** Enregistre l'usage réel d'une réponse réussie. Un appel échoué ne consomme rien. */
    @Transactional
    public void enregistrerUsage(
            UUID structureId,
            UUID userId,
            RoleMembreStructure role,
            UUID conversationId,
            AiService.AiResponse reponse) {

        if (reponse == null || !reponse.success()) {
            return;
        }
        if (reponse.usage() == null) {
            log.warn("[Quota] Usage IA inconnu (modèle={}) : appel enregistré sans tokens", reponse.model());
        }

        IaUsage usage = new IaUsage();
        usage.setStructureId(structureId);
        usage.setUserId(userId);
        usage.setRole(role.name());
        usage.setConversationId(conversationId);
        usage.setTokensEntree(reponse.usage() == null ? 0 : reponse.usage().tokensEntree());
        usage.setTokensSortie(reponse.usage() == null ? 0 : reponse.usage().tokensSortie());
        usage.setModele(reponse.model());
        iaUsageRepository.save(usage);
    }

    // ═══════════════════════════════════════════
    // Cohortes
    // ═══════════════════════════════════════════

    /** @throws QuotaDepasseException 429 PLAN_LIMITE_ATTEINTE */
    @Transactional(readOnly = true)
    public void verifierCreationCohorte(UUID structureId) {
        PlanAbonnement plan = planService.planEffectif(structureId);
        Integer maxCohortes = plan.getMaxCohortes();
        if (maxCohortes == null) {
            return;
        }

        long utilisees = cohortesComptees(structureId);
        if (utilisees >= maxCohortes) {
            throw new QuotaDepasseException(
                    HttpStatus.TOO_MANY_REQUESTS,
                    QuotaDepasseException.PLAN_LIMITE_ATTEINTE,
                    "Votre offre « " + plan.getLibelle() + " » autorise au maximum " + maxCohortes
                            + " cohorte(s) planifiée(s) ou en cours. Changez d'offre pour en créer davantage.",
                    utilisees,
                    maxCohortes.longValue());
        }
    }

    // ═══════════════════════════════════════════
    // Consultation
    // ═══════════════════════════════════════════

    /** Limites du plan effectif et consommation courante, pour l'utilisateur connecté. */
    @Transactional(readOnly = true)
    public UsageAbonnementResponse getUsage(UUID structureId, User utilisateur) {
        Abonnement abonnement = abonnementRepository.findByStructureId(structureId).orElse(null);
        PlanAbonnement plan = planService.planEffectif(abonnement);

        RoleMembreStructure role = roleOuNull(structureId, utilisateur);
        YearMonth mois = YearMonth.now();
        long consomme = tokensConsommesCeMois(structureId);
        Long limite = plan.getIaTokensMensuels();
        Integer pourcentage = limite == null || limite <= 0
                ? null
                : (int) Math.min(100, Math.round(consomme * 100.0 / limite));

        return new UsageAbonnementResponse(
                PlanAbonnementResponse.from(plan),
                abonnement == null ? null : abonnement.getStatut().name(),
                abonnement == null ? null : abonnement.getDateFin(),
                new UsageAbonnementResponse.Compteur(cohortesComptees(structureId), plan.getMaxCohortes()),
                new UsageAbonnementResponse.Ia(
                        plan.isIaCoachActive(),
                        plan.isIaEntrepreneurActive(),
                        role != null && roleAutorise(plan, role),
                        role == null ? null : role.name(),
                        consomme,
                        limite,
                        pourcentage,
                        mois.atDay(1).atStartOfDay(),
                        mois.plusMonths(1).atDay(1).atStartOfDay()));
    }

    // ═══════════════════════════════════════════
    // Internes
    // ═══════════════════════════════════════════

    /** Les administrateurs de structure suivent la règle des coachs. */
    private boolean roleAutorise(PlanAbonnement plan, RoleMembreStructure role) {
        if (role == null) {
            return false;
        }
        return role == RoleMembreStructure.ENTREPRENEUR
                ? plan.isIaEntrepreneurActive()
                : plan.isIaCoachActive();
    }

    private RoleMembreStructure roleActif(UUID structureId, User utilisateur) {
        RoleMembreStructure role = roleOuNull(structureId, utilisateur);
        if (role == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Vous n'êtes pas membre actif de cette structure.");
        }
        return role;
    }

    private RoleMembreStructure roleOuNull(UUID structureId, User utilisateur) {
        if (utilisateur == null) {
            return null;
        }
        return membreStructureRepository.findByUserIdAndStructureId(utilisateur.getId(), structureId)
                .filter(m -> m.getStatut() == StatutMembre.ACCEPTE)
                .map(MembreStructure::getRole)
                .orElse(null);
    }

    private long tokensConsommesCeMois(UUID structureId) {
        YearMonth mois = YearMonth.now();
        LocalDateTime debut = mois.atDay(1).atStartOfDay();
        LocalDateTime fin = mois.plusMonths(1).atDay(1).atStartOfDay();
        return iaUsageRepository.sumTokensByStructureBetween(structureId, debut, fin);
    }

    private long cohortesComptees(UUID structureId) {
        return cohorteRepository.countByStructureIdAndStatutIn(structureId, COHORTES_COMPTEES);
    }
}
