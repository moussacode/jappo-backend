package sn.jappo.jappo_backend.ia.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import sn.jappo.jappo_backend.ia.context.AiContext;
import sn.jappo.jappo_backend.ia.entity.Message;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implémentation HTTP du service IA qui communique avec FastAPI.
 *
 * Remplace FakeAiService par un appel réel à l'orchestrateur FastAPI.
 * @Primary : c'est ce bean que Spring injecte dans ConversationIaService
 * pour le fonctionnement réel de l'assistant.
 */
@Service
@org.springframework.context.annotation.Primary
public class HttpAiService implements AiService {

    private static final Logger log = LoggerFactory.getLogger(HttpAiService.class);

    private final RestTemplate restTemplate;
    private final String orchestratorUrl;
    private final String orchestratorApiKey;
    private final int timeout;

    public HttpAiService(
            RestTemplate restTemplate,
            @Value("${ai.orchestrator.url:http://localhost:8000}") String orchestratorUrl,
            @Value("${ai.orchestrator.api-key:default-secret-key}") String orchestratorApiKey,
            @Value("${ai.orchestrator.timeout:30000}") int timeout
    ) {
        this.restTemplate = restTemplate;
        this.orchestratorUrl = orchestratorUrl;
        this.orchestratorApiKey = orchestratorApiKey;
        this.timeout = timeout;
    }

    /**
     * Message générique affiché au coach quand l'IA est indisponible, quelle que soit la cause
     * technique réelle (timeout, FastAPI down, OpenRouter en panne, clé invalide, modèle retiré...).
     * La cause précise est toujours loguée côté serveur (voir chaque catch ci-dessous),
     * jamais renvoyée telle quelle au client — cf. section "Fiabiliser le flux IA".
     */
    private static final String MESSAGE_INDISPONIBLE =
            "L'assistant est temporairement indisponible. Veuillez réessayer dans quelques instants.";

    private static final String MESSAGE_SURCHARGE =
            "L'assistant reçoit trop de demandes en ce moment. Merci de réessayer dans un instant.";

    private static final String MESSAGE_REPONSE_INVALIDE =
            "L'assistant n'a pas pu générer de réponse exploitable. Veuillez reformuler votre question.";

    @Override
    public AiService.AiResponse generateResponse(String question, AiContext context, List<Message> history) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("message", question);
        requestBody.put("context", convertAiContextToMap(context));
        requestBody.put("history", convertHistoryToMap(history));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Internal-Api-Key", orchestratorApiKey);

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);
        String url = orchestratorUrl + "/api/v1/chat";

        try {
            log.info("[HttpAiService] Calling FastAPI orchestrator at {}", orchestratorUrl);

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.postForObject(url, request, Map.class);

            if (response == null) {
                log.error("[HttpAiService] Réponse nulle reçue de FastAPI ({})", url);
                return AiService.AiResponse.failure(MESSAGE_REPONSE_INVALIDE);
            }

            String content = (String) response.get("content");
            String model = (String) response.get("model");

            if (content == null || content.isBlank()) {
                log.error("[HttpAiService] Contenu vide reçu de FastAPI (modèle={})", model);
                return AiService.AiResponse.failure(MESSAGE_REPONSE_INVALIDE);
            }

            log.info("[HttpAiService] Réponse reçue du modèle: {}", model);
            return new AiService.AiResponse(
                    content,
                    model != null ? model : "unknown",
                    true,
                    extractMapList(response, "sources"),
                    extractMapList(response, "actions")
            );

        } catch (HttpClientErrorException.Unauthorized e) {
            // 401 : clé interne Spring/FastAPI désynchronisée — erreur de configuration, jamais exposée au coach.
            log.error("[HttpAiService] Authentification interne refusée par FastAPI (401) — "
                    + "vérifier que ai.orchestrator.api-key correspond à AI_ORCHESTRATOR_API_KEY côté FastAPI");
            return AiService.AiResponse.failure(MESSAGE_INDISPONIBLE);

        } catch (HttpClientErrorException.TooManyRequests e) {
            // 429 : quota OpenRouter ou FastAPI atteint.
            log.warn("[HttpAiService] 429 Too Many Requests reçu de FastAPI");
            return AiService.AiResponse.failure(MESSAGE_SURCHARGE);

        } catch (HttpClientErrorException e) {
            // 400, 404, etc. côté FastAPI (ex : modèle retiré chez le fournisseur, payload invalide).
            HttpStatusCode statusCode = e.getStatusCode();
            log.error("[HttpAiService] Erreur cliente {} reçue de FastAPI : {}", statusCode, e.getMessage());
            return AiService.AiResponse.failure(MESSAGE_INDISPONIBLE);

        } catch (HttpServerErrorException e) {
            // 500/502/503 côté FastAPI (ex : OpenRouter indisponible).
            log.error("[HttpAiService] Erreur serveur {} reçue de FastAPI : {}", e.getStatusCode(), e.getMessage());
            return AiService.AiResponse.failure(MESSAGE_INDISPONIBLE);

        } catch (ResourceAccessException e) {
            // Timeout Spring, ou FastAPI totalement injoignable (connexion refusée, DNS, etc.).
            log.error("[HttpAiService] FastAPI injoignable ou timeout dépassé ({}) : {}", url, e.getMessage());
            return AiService.AiResponse.failure(MESSAGE_INDISPONIBLE);

        } catch (RestClientException e) {
            // Filet de sécurité pour toute autre erreur du client HTTP (ex : désérialisation).
            log.error("[HttpAiService] Erreur inattendue lors de l'appel à FastAPI : {}", e.getMessage(), e);
            return AiService.AiResponse.failure(MESSAGE_INDISPONIBLE);

        } catch (Exception e) {
            // Filet de sécurité absolu : quelle que soit l'exception (même un type qu'on
            // n'a pas anticipé ci-dessus), l'application ne doit JAMAIS planter parce que
            // l'IA est indisponible — exigence explicite du cahier des charges.
            log.error("[HttpAiService] Erreur totalement inattendue lors de l'appel à FastAPI : {}", e.getMessage(), e);
            return AiService.AiResponse.failure(MESSAGE_INDISPONIBLE);
        }
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractMapList(Map<String, Object> response, String key) {
        Object value = response.get(key);
        if (value instanceof List<?> list) {
            return list.stream()
                    .filter(Map.class::isInstance)
                    .map(item -> (Map<String, Object>) item)
                    .toList();
        }
        return List.of();
    }

    private Map<String, Object> convertAiContextToMap(AiContext context) {
        Map<String, Object> map = new HashMap<>();
        map.put("structureId", context.getStructureId());
        map.put("structureNom", context.getStructureNom());
        map.put("contextGlobal", context.isContextGlobal());
        map.put("nombreTotalEntrepreneurs", context.getNombreTotalEntrepreneurs());
        map.put("nombreTotalProjets", context.getNombreTotalProjets());
        map.put("nombreTotalCohortes", context.getNombreTotalCohortes());
        map.put("livrablesEnAttente", context.getLivrablesEnAttente());

        // Convert cohortes
        List<Map<String, Object>> cohortes = context.getCohortes().stream()
                .map(c -> {
                    Map<String, Object> cohorteMap = new HashMap<>();
                    cohorteMap.put("id", c.id);
                    cohorteMap.put("nom", c.nom);
                    cohorteMap.put("statut", c.statut);
                    cohorteMap.put("nombreProjets", c.nombreProjets);
                    return cohorteMap;
                })
                .collect(Collectors.toList());
        map.put("cohortes", cohortes);

        // Convert projets
        List<Map<String, Object>> projets = context.getProjets().stream()
                .map(p -> {
                    Map<String, Object> projetMap = new HashMap<>();
                    projetMap.put("id", p.id);
                    projetMap.put("nom", p.nom);
                    projetMap.put("statut", p.statut);
                    projetMap.put("scoreMaturite", p.scoreMaturite);
                    projetMap.put("cohorteId", p.cohorteId);
                    projetMap.put("cohorteNom", p.cohorteNom);
                    projetMap.put("entrepreneurId", p.entrepreneurId);
                    projetMap.put("entrepreneurNom", p.entrepreneurNom);
                    projetMap.put("nombreMissionsTotal", p.nombreMissionsTotal);
                    projetMap.put("nombreMissionsValidees", p.nombreMissionsValidees);
                    projetMap.put("nombreLivrablesEnAttente", p.nombreLivrablesEnAttente);
                    return projetMap;
                })
                .collect(Collectors.toList());
        map.put("projets", projets);

        return map;
    }

    private List<Map<String, Object>> convertHistoryToMap(List<Message> history) {
        return history.stream()
                .map(m -> {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("auteur", m.getAuteur().name());
                    msgMap.put("contenu", m.getContenu());
                    return msgMap;
                })
                .collect(Collectors.toList());
    }
}