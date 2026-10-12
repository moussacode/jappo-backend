package sn.jappo.jappo_backend.ia.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import sn.jappo.jappo_backend.abonnement.service.PlanLimiteService;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.ia.action.AiActionService;
import sn.jappo.jappo_backend.ia.context.AiContext;
import sn.jappo.jappo_backend.ia.context.AiContextBuilder;
import sn.jappo.jappo_backend.ia.context.ConversationContexte;
import sn.jappo.jappo_backend.ia.dto.SendMessageRequest;
import sn.jappo.jappo_backend.ia.entity.Conversation;
import sn.jappo.jappo_backend.ia.entity.Message;
import sn.jappo.jappo_backend.ia.repository.ConversationRepository;
import sn.jappo.jappo_backend.ia.repository.MessageRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConversationOwnershipTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private MessageRepository messageRepository;
    @Mock private StructureRepository structureRepository;
    @Mock private UserRepository userRepository;
    @Mock private AiContextBuilder aiContextBuilder;
    @Mock private AiService aiService;
    @Mock private AiActionService aiActionService;
    @Mock private PlanLimiteService planLimiteService;

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void idOperationsRequireTheActiveTenantAndConversationOwner() {
        UUID activeTenant = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User otherUser = new User();
        otherUser.setId(UUID.randomUUID());
        TenantContext.setCurrentTenant(activeTenant);
        when(conversationRepository.findByIdAndStructureIdAndCoachId(
                conversationId, activeTenant, otherUser.getId())).thenReturn(Optional.empty());

        ConversationIaService service = new ConversationIaService(
                conversationRepository, messageRepository, structureRepository, userRepository,
                aiContextBuilder, aiService, aiActionService, planLimiteService);
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> service.archiveConversation(conversationId, otherUser));

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verify(conversationRepository).findByIdAndStructureIdAndCoachId(
                conversationId, activeTenant, otherUser.getId());
        verifyNoInteractions(messageRepository);
    }

    @Test
    void entrepreneurCanReadConversationWhenTheyAreItsOwner() {
        UUID activeTenant = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User entrepreneur = new User();
        entrepreneur.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(activeTenant);
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setStructure(structure);
        conversation.setCoach(entrepreneur);
        conversation.setContexteJson("{}");

        TenantContext.setCurrentTenant(activeTenant);
        when(conversationRepository.findByIdAndStructureIdAndCoachId(
                conversationId, activeTenant, entrepreneur.getId())).thenReturn(Optional.of(conversation));
        when(messageRepository.findAllByConversationIdOrderByDateEnvoiAsc(conversationId))
                .thenReturn(List.of());

        ConversationIaService service = new ConversationIaService(
                conversationRepository, messageRepository, structureRepository, userRepository,
                aiContextBuilder, aiService, aiActionService, planLimiteService);
        var response = service.getConversation(conversationId, entrepreneur);

        assertNotNull(response);
        assertEquals(conversationId, response.id());
        verify(conversationRepository).findByIdAndStructureIdAndCoachId(
                conversationId, activeTenant, entrepreneur.getId());
    }

    @Test
    void entrepreneurCanPostToTheirOwnedConversation() {
        UUID activeTenant = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        User entrepreneur = new User();
        entrepreneur.setId(UUID.randomUUID());
        Structure structure = new Structure();
        structure.setId(activeTenant);
        Conversation conversation = new Conversation();
        conversation.setId(conversationId);
        conversation.setStructure(structure);
        conversation.setCoach(entrepreneur);
        conversation.setContexteJson("{}");
        AiContext context = new AiContext();
        context.setUserRole("ENTREPRENEUR");

        TenantContext.setCurrentTenant(activeTenant);
        when(conversationRepository.findByIdAndStructureIdAndCoachId(
                conversationId, activeTenant, entrepreneur.getId())).thenReturn(Optional.of(conversation));
        when(conversationRepository.save(conversation)).thenReturn(conversation);
        when(messageRepository.save(any(Message.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(messageRepository.findAllByConversationIdOrderByDateEnvoiAsc(conversationId)).thenReturn(List.of());
        when(aiContextBuilder.build(eq(activeTenant), any(ConversationContexte.class), eq(entrepreneur)))
                .thenReturn(context);
        when(aiService.generateResponse("Question", context, List.of()))
                .thenReturn(AiService.AiResponse.ok("Réponse", "test"));

        ConversationIaService service = new ConversationIaService(
                conversationRepository, messageRepository, structureRepository, userRepository,
                aiContextBuilder, aiService, aiActionService, planLimiteService);
        var messages = service.sendMessage(conversationId, entrepreneur, new SendMessageRequest("Question"));

        assertEquals(2, messages.size());
        assertEquals("Question", messages.get(0).contenu());
        assertEquals("Réponse", messages.get(1).contenu());
        verify(conversationRepository).findByIdAndStructureIdAndCoachId(
                conversationId, activeTenant, entrepreneur.getId());
    }
}
