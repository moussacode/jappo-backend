package sn.jappo.jappo_backend.ia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import sn.jappo.jappo_backend.ia.context.AiContext;
import sn.jappo.jappo_backend.ia.entity.Message;

import java.util.List;

/**
 * Implémentation de simulation de l'IA.
 *
 * NE CONNECTE AUCUN LLM — objectif : valider le pipeline complet :
 *   Angular → Spring → Conversation → Message COACH
 *   → AiContextBuilder → FakeAiService → Message ASSISTANT → Angular
 *
 * La réponse est construite à partir des données réelles de AiContext
 * pour simuler un comportement cohérent et vérifiable.
 *
 * Reste disponible pour les tests (instancié directement, sans passer par Spring).
 * N'est plus le bean injecté par défaut : HttpAiService (le vrai orchestrateur
 * FastAPI + LLM) porte désormais @Primary.
 */
@Service
public class FakeAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(FakeAiService.class);

    public static final String MODEL_ID = "fake-v1";

    @Override
    public AiResponse generateResponse(String question, AiContext context, List<Message> history) {
        log.info("[FakeAiService] question='{}' structureId={} contextGlobal={}",
                question, context.getStructureId(), context.isContextGlobal());

        StringBuilder sb = new StringBuilder();
        sb.append("**Simulation IA** — réponse générée par FakeAiService.\n\n");

        // Description du contexte utilisé
        if (context.isContextGlobal()) {
            sb.append("J'ai analysé le contexte global de votre structure **")
              .append(context.getStructureNom()).append("**.\n\n");
            sb.append("Données disponibles :\n");
            sb.append("- ").append(context.getNombreTotalEntrepreneurs())
              .append(" entrepreneur(s)\n");
            sb.append("- ").append(context.getNombreTotalCohortes()).append(" cohorte(s)\n");
            sb.append("- ").append(context.getNombreTotalProjets()).append(" projet(s)\n");
            sb.append("- ").append(context.getLivrablesEnAttente())
              .append(" livrable(s) en attente d'évaluation\n");
            if (!context.getCohortes().isEmpty()) {
                sb.append("\nCohortes :\n");
                context.getCohortes().forEach(c ->
                    sb.append("- **").append(c.nom).append("** (").append(c.statut)
                      .append(") — ").append(c.nombreProjets).append(" projet(s)\n")
                );
            }
        } else if (!context.getProjets().isEmpty() && context.getCohortes().isEmpty()) {
            // Contexte projet uniquement
            var p = context.getProjets().get(0);
            sb.append("J'ai analysé le projet **").append(p.nom).append("**.\n\n");
            sb.append("Données disponibles :\n");
            sb.append("- Entrepreneur : ").append(p.entrepreneurNom != null ? p.entrepreneurNom : "—").append("\n");
            sb.append("- Score de maturité : ").append(p.scoreMaturite).append("%\n");
            sb.append("- Missions : ").append(p.nombreMissionsValidees).append("/")
              .append(p.nombreMissionsTotal).append(" validée(s)\n");
            sb.append("- Livrables en attente : ").append(p.nombreLivrablesEnAttente).append("\n");
        } else if (!context.getCohortes().isEmpty()) {
            var c = context.getCohortes().get(0);
            sb.append("J'ai analysé la cohorte **").append(c.nom).append("**.\n\n");
            sb.append("Données disponibles :\n");
            sb.append("- ").append(c.nombreProjets).append(" projet(s) dans cette cohorte\n");
            if (!context.getProjets().isEmpty()) {
                sb.append("\nProjets :\n");
                context.getProjets().forEach(p ->
                    sb.append("- **").append(p.nom).append("** — ")
                      .append(p.scoreMaturite).append("% maturité, ")
                      .append(p.nombreMissionsValidees).append("/").append(p.nombreMissionsTotal)
                      .append(" missions validées\n")
                );
            }
        }

        sb.append("\n*Question reçue :* « ").append(question).append(" »\n\n");
        sb.append("_(Réponse de simulation — le vrai LLM sera branché dans la prochaine étape.)_");

        // Historique pris en compte (log uniquement pour l'instant)
        if (!history.isEmpty()) {
            log.debug("[FakeAiService] {} message(s) d'historique disponibles", history.size());
        }

        return AiResponse.ok(sb.toString(), MODEL_ID);
    }
}