package sn.jappo.jappo_backend.assistance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import sn.jappo.jappo_backend.assistance.dto.AssistanceRequest;

import org.springframework.beans.factory.annotation.Value;
import java.util.Map;

@Service
public class AssistanceService {

    private static final Logger log = LoggerFactory.getLogger(AssistanceService.class);

    private final String n8nBaseUrl;
    private static final String N8N_WEBHOOK_PATH = "/webhook/jappo-support";

    private final RestClient restClient;

    public AssistanceService(@Value("${n8n.base-url:http://127.0.0.1:5678}") String n8nBaseUrl) {
        this.n8nBaseUrl = n8nBaseUrl;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000); // 3s
        requestFactory.setReadTimeout(8000);    // 8s

        this.restClient = RestClient.builder()
                .baseUrl(n8nBaseUrl)
                .requestFactory(requestFactory)
                .build();
    }

    public Map<String, Object> envoyerDemande(AssistanceRequest request) {
        try {
            Map<String, Object> response = restClient.post()
                    .uri(N8N_WEBHOOK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            return response;

        } catch (ResourceAccessException e) {
            log.warn("Assistance : n8n injoignable ({})", e.getMessage());
            throw new IllegalStateException(
                    "Le service d'assistance est momentanément indisponible (n8n injoignable ou trop lent).", e
            );

        } catch (RestClientResponseException e) {
            log.warn("Assistance : n8n a répondu {}", e.getStatusCode());
            throw new IllegalStateException(
                    "Le service d'assistance a renvoyé une erreur : " + e.getResponseBodyAsString(), e
            );
        }
    }
}