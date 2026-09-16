package sn.jappo.jappo_backend.ia.action;

import sn.jappo.jappo_backend.user.entity.User;

import java.util.Map;
import java.util.UUID;

/**
 * Contrat à implémenter pour rendre un {@link AiActionType} réellement exécutable.
 *
 * Exemple d'implémentation future (non fournie ici, cf. section 12 du cahier des charges) :
 *
 * <pre>{@code
 * @Component
 * public class CreateCohorteExecutor implements AiActionExecutor {
 *     public boolean supports(AiActionType type) { return type == AiActionType.CREATE_COHORTE; }
 *     public void executer(UUID structureId, Map<String,Object> payload, User confirmePar) {
 *         // valider le payload, PUIS appeler CohorteService.createCohorte(...) —
 *         // jamais de SQL direct, jamais de bypass du service métier existant.
 *     }
 * }
 * }</pre>
 *
 * Tant qu'aucun exécuteur ne déclare {@code supports(type) == true} pour un type donné,
 * {@link AiActionService#confirmer} refuse explicitement l'exécution (501) plutôt que
 * de laisser une confirmation humaine ne rien faire silencieusement.
 */
public interface AiActionExecutor {

    boolean supports(AiActionType type);

    /**
     * Exécute l'action. Doit valider le payload et vérifier que toute ressource
     * référencée appartient bien à {@code structureId} avant d'appeler le service métier.
     *
     * @throws org.springframework.web.server.ResponseStatusException si le payload est
     *         invalide ou référence une ressource d'une autre structure.
     */
    void executer(UUID structureId, Map<String, Object> payload, User confirmePar);
    
    /**
     * Retourne les informations sur la ressource créée/modifiée pour permettre
     * au frontend de proposer une navigation intelligente.
     * 
     * @return Map contenant: type, id, name, url
     */
    default Map<String, Object> getResourceInfo() {
        return null;
    }
}