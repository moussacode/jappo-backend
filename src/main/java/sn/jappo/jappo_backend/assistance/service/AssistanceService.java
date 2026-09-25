package sn.jappo.jappo_backend.assistance.service;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import sn.jappo.jappo_backend.assistance.dto.AssistanceRequest;

import java.util.Map;

@Service
public class AssistanceService {

    private static final String N8N_BASE_URL = "http://127.0.0.1:5678";
    private static final String N8N_WEBHOOK_PATH = "/webhook/jappo-support";

    private final RestClient restClient;

    public AssistanceService() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(3000); // 3s
        requestFactory.setReadTimeout(8000);    // 8s

        this.restClient = RestClient.builder()
                .baseUrl(N8N_BASE_URL)
                .requestFactory(requestFactory)
                .build();
    }

    public Map<String, Object> envoyerDemande(AssistanceRequest request) {
        System.out.println(">>> APPEL N8N");
        System.out.println(">>> URL : " + N8N_BASE_URL + N8N_WEBHOOK_PATH);
        System.out.println(">>> BODY ENVOYÉ : email=" + request.email() + " | message=" + request.message());

        try {
            Map<String, Object> response = restClient.post()
                    .uri(N8N_WEBHOOK_PATH)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(Map.class);

            System.out.println(">>> RÉPONSE N8N : " + response);
            return response;

        } catch (ResourceAccessException e) {
            System.err.println(">>> ERREUR RÉSEAU vers n8n : " + e.getMessage());
            throw new IllegalStateException(
                    "Le service d'assistance est momentanément indisponible (n8n injoignable ou trop lent).", e
            );

        } catch (RestClientResponseException e) {
            System.err.println(">>> ERREUR HTTP n8n (" + e.getStatusCode() + ") : " + e.getResponseBodyAsString());
            throw new IllegalStateException(
                    "Le service d'assistance a renvoyé une erreur : " + e.getResponseBodyAsString(), e
            );
        }
    }
}