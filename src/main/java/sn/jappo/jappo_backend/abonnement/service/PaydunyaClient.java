package sn.jappo.jappo_backend.abonnement.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import sn.jappo.jappo_backend.abonnement.config.PaydunyaProperties;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class PaydunyaClient {

    private final RestTemplate restTemplate;
    private final PaydunyaProperties props;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public record InitiationResult(
            boolean success,
            String token,
            String redirectUrl,
            String message
    ) {}

    public record PaymentStatusResult(
        boolean success,
        String status,
        String refCommand,
        long amount,
        String message
) {}

    public InitiationResult initierPaiement(
            String description,
            long montant,
            String refCommand,
            String customerName,
            String customerEmail,
            String customerPhone
    ) {

        HttpHeaders headers = new HttpHeaders();

        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("PAYDUNYA-MASTER-KEY", props.getMasterKey());
        headers.set("PAYDUNYA-PRIVATE-KEY", props.getPrivateKey());
        headers.set("PAYDUNYA-TOKEN", props.getToken());

        Map<String, Object> invoice = new HashMap<>();

        invoice.put("total_amount", montant);
        invoice.put("description", description);

        Map<String, String> customer = new HashMap<>();
        customer.put("name", customerName);
        customer.put("email", customerEmail);
        customer.put("phone", customerPhone);

        invoice.put("customer", customer);

        Map<String, Object> store = new HashMap<>();
        store.put("name", "JAPPO");

        Map<String, String> actions = new HashMap<>();
        actions.put("cancel_url", props.getCancelUrl());
        actions.put("return_url", props.getReturnUrl());
        actions.put("callback_url", props.getCallbackUrl());

        Map<String, Object> customData = new HashMap<>();
        customData.put("ref_command", refCommand);

        Map<String, Object> body = new HashMap<>();
        body.put("invoice", invoice);
        body.put("store", store);
        body.put("actions", actions);
        body.put("custom_data", customData);

        try {

            String url = props.getBaseUrl() + "/checkout-invoice/create";

            ResponseEntity<String> responseEntity = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    String.class
            );

            String rawResponse = responseEntity.getBody();

            System.out.println("=== PAYDUNYA RESPONSE ===");
            System.out.println("HTTP STATUS : " + responseEntity.getStatusCode());
            System.out.println("BODY : " + rawResponse);

            if (rawResponse == null || rawResponse.isBlank()) {
                return new InitiationResult(
                        false,
                        null,
                        null,
                        "Réponse vide de PayDunya"
                );
            }

            JsonNode response = objectMapper.readTree(rawResponse);

            String responseCode = response.path("response_code").asText();

            if ("00".equals(responseCode)) {

                String token = response.path("token").asText();
                String redirectUrl = response.path("response_text").asText();

                return new InitiationResult(
                        true,
                        token,
                        redirectUrl,
                        null
                );
            }

            return new InitiationResult(
                    false,
                    null,
                    null,
                    response.path("response_text")
                            .asText("Réponse PayDunya invalide")
            );

        } catch (Exception e) {

            e.printStackTrace();

            return new InitiationResult(
                    false,
                    null,
                    null,
                    e.getMessage()
            );
        }
    }


 public PaymentStatusResult verifierPaiement(String token) {

    HttpHeaders headers = new HttpHeaders();

    headers.set("PAYDUNYA-MASTER-KEY", props.getMasterKey());
    headers.set("PAYDUNYA-PRIVATE-KEY", props.getPrivateKey());
    headers.set("PAYDUNYA-TOKEN", props.getToken());
    headers.setAccept(java.util.List.of(MediaType.APPLICATION_JSON));

    try {

        String url =
                props.getBaseUrl()
                        + "/checkout-invoice/confirm/"
                        + token;

        ResponseEntity<String> responseEntity =
                restTemplate.exchange(
                        url,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        String.class
                );

        String rawResponse = responseEntity.getBody();

        System.out.println("=== PAYDUNYA CONFIRMATION ===");
        System.out.println("HTTP STATUS : " + responseEntity.getStatusCode());
        System.out.println("BODY : " + rawResponse);

        if (rawResponse == null || rawResponse.isBlank()) {

            return new PaymentStatusResult(
                    false,
                    null,
                    null,
                    0,
                    "Réponse vide de PayDunya"
            );
        }

        JsonNode response =
                objectMapper.readTree(rawResponse);

        String responseCode =
                response.path("response_code").asText();

        JsonNode invoice =
                response.path("invoice");

        String status =
                response.path("status").asText(null);

        String refCommand =
                response.path("custom_data")
                        .path("ref_command")
                        .asText(null);

        long amount =
                invoice.path("total_amount").asLong(0);

        String message =
                response.path("response_text")
                        .asText(null);

        boolean success =
                "00".equals(responseCode);

        return new PaymentStatusResult(
                success,
                status,
                refCommand,
                amount,
                message
        );

    } catch (Exception e) {

        e.printStackTrace();

        return new PaymentStatusResult(
                false,
                null,
                null,
                0,
                e.getMessage()
        );
    }
}
}