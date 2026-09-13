package sn.jappo.jappo_backend.config.tenant;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantFilterTest {

    @Mock private MembreStructureRepository membreStructureRepository;
    @Mock private HttpServletRequest request;
    @Mock private HttpServletResponse response;
    @Mock private FilterChain filterChain;

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
        TenantContext.clear();
    }

    private void authentifier(UUID userId) {
        User user = new User();
        user.setId(userId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of())
        );
    }

    @Test
    void refuseAccesSiUtilisateurNestPasMembreDeLaStructure() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID structureCibleeId = UUID.randomUUID();
        authentifier(userId);

        
        when(request.getHeader("X-Structure-Id")).thenReturn(structureCibleeId.toString());
        when(membreStructureRepository.findByUserIdAndStructureId(userId, structureCibleeId))
                .thenReturn(Optional.empty());
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        new TenantFilter(membreStructureRepository).doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
        assertThat(TenantContext.getCurrentTenant()).isNull();
    }

    @Test
    void refuseAccesSiMembreEncoreEnAttente() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID structureId = UUID.randomUUID();
        authentifier(userId);

        MembreStructure membreEnAttente = new MembreStructure();
        membreEnAttente.setStatut(StatutMembre.EN_ATTENTE);

        
        when(request.getHeader("X-Structure-Id")).thenReturn(structureId.toString());
        when(membreStructureRepository.findByUserIdAndStructureId(userId, structureId))
                .thenReturn(Optional.of(membreEnAttente));
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        new TenantFilter(membreStructureRepository).doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void autoriseAccesSiMembreActifDeLaStructure() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID structureId = UUID.randomUUID();
        authentifier(userId);

        MembreStructure membreActif = new MembreStructure();
        membreActif.setStatut(StatutMembre.ACCEPTE);

       
        when(request.getHeader("X-Structure-Id")).thenReturn(structureId.toString());
        when(membreStructureRepository.findByUserIdAndStructureId(userId, structureId))
                .thenReturn(Optional.of(membreActif));

        new TenantFilter(membreStructureRepository).doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(HttpServletResponse.SC_FORBIDDEN);
    }

    @Test
    void laisseFairePasserSansHeaderTenant() throws Exception {
        
        when(request.getHeader("X-Structure-Id")).thenReturn(null);

        new TenantFilter(membreStructureRepository).doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(membreStructureRepository);
    }
}