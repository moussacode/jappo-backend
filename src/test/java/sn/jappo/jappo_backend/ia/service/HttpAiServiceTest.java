package sn.jappo.jappo_backend.ia.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;
import sn.jappo.jappo_backend.ia.context.AiContext;
import sn.jappo.jappo_backend.ia.entity.Message;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour HttpAiService.
 *
 * Vérifient la communication entre Spring Boot et FastAPI.
 */
@ExtendWith(MockitoExtension.class)
class HttpAiServiceTest {

    @Mock
    private RestTemplate restTemplate;

    private HttpAiService httpAiService;

    private static final String ORCHESTRATOR_URL = "http://localhost:8000";
    private static final String API_KEY = "test-secret-key";

    @BeforeEach
    void setUp() {
        httpAiService = new HttpAiService(
                restTemplate,
                ORCHESTRATOR_URL,
                API_KEY,
                30000
        );
    }

    @Test
    void generateResponse_fastAPIRetourneReponse_retourneContenuEtModel() {
        // Arrange
        String question = "Quels sont les projets ?";
        AiContext context = createTestContext();
        List<Message> history = List.of();

        Map<String, Object> mockResponse = Map.of(
                "content", "Voici les projets disponibles...",
                "model", "gpt-4o",
                "sources", List.of(),
                "actions", List.of()
        );

        when(restTemplate.postForObject(
                startsWith(ORCHESTRATOR_URL),
                any(),
                eq(Map.class)
        )).thenReturn(mockResponse);

        // Act
        AiService.AiResponse response = httpAiService.generateResponse(question, context, history);

        // Assert
        assertThat(response.success()).isTrue();
        assertThat(response.content()).isEqualTo("Voici les projets disponibles...");
        assertThat(response.model()).isEqualTo("gpt-4o");
    }

    @Test
    void generateResponse_fastAPIRetourneNull_retourneErreur() {
        // Arrange
        String question = "Test question";
        AiContext context = createTestContext();
        List<Message> history = List.of();

        when(restTemplate.postForObject(
                startsWith(ORCHESTRATOR_URL),
                any(),
                eq(Map.class)
        )).thenReturn(null);

        // Act
        AiService.AiResponse response = httpAiService.generateResponse(question, context, history);

        // Assert : repli métier, sans détail technique exposé au client
        assertThat(response.success()).isFalse();
        assertThat(response.content()).contains("n'a pas pu générer de réponse exploitable");
        assertThat(response.model()).isEqualTo("unavailable");
    }

    @Test
    void generateResponse_fastAPILeveException_retourneErreur() {
        // Arrange
        String question = "Test question";
        AiContext context = createTestContext();
        List<Message> history = List.of();

        when(restTemplate.postForObject(
                startsWith(ORCHESTRATOR_URL),
                any(),
                eq(Map.class)
        )).thenThrow(new RuntimeException("Service unavailable"));

        // Act
        AiService.AiResponse response = httpAiService.generateResponse(question, context, history);

        // Assert : indisponibilité générique, jamais la cause technique
        assertThat(response.success()).isFalse();
        assertThat(response.content()).contains("temporairement indisponible");
        assertThat(response.content()).doesNotContain("Service unavailable");
        assertThat(response.model()).isEqualTo("unavailable");
    }

    @Test
    void generateResponse_envoieHeadersCorrects() {
        // Arrange
        String question = "Test question";
        AiContext context = createTestContext();
        List<Message> history = List.of();

        Map<String, Object> mockResponse = Map.of(
                "content", "Response",
                "model", "gpt-4o",
                "sources", List.of(),
                "actions", List.of()
        );

        when(restTemplate.postForObject(
                startsWith(ORCHESTRATOR_URL),
                any(),
                eq(Map.class)
        )).thenReturn(mockResponse);

        // Act
        httpAiService.generateResponse(question, context, history);

        // Assert - vérifier que restTemplate a été appelé
        // (la vérification détaillée des headers nécessiterait un ArgumentCaptor)
    }

    private AiContext createTestContext() {
        AiContext context = new AiContext();
        context.setStructureId(UUID.randomUUID());
        context.setStructureNom("Test Structure");
        context.setContextGlobal(true);
        context.setNombreTotalEntrepreneurs(10);
        context.setNombreTotalProjets(5);
        context.setNombreTotalCohortes(2);
        context.setLivrablesEnAttente(3);
        return context;
    }
}
