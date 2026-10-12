package sn.jappo.jappo_backend.config.tenant;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class TenantFileIsolationTest {

    @AfterEach
    void clearTenant() {
        TenantContext.clear();
    }

    @Test
    void deniesFilesBelongingToAnotherTenant() throws Exception {
        UUID activeTenantId = UUID.randomUUID();
        UUID fileTenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/fichiers/ressources/" + fileTenantId + "/guide.pdf");
        request.addHeader("X-Structure-Id", activeTenantId.toString());
        MockHttpServletResponse response = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = mock(jakarta.servlet.FilterChain.class);
        TenantFilter filter = new TenantFilter(mock(MembreStructureRepository.class));

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }

    @Test
    void requiresAnActiveTenantForStoredFiles() throws Exception {
        UUID fileTenantId = UUID.randomUUID();
        MockHttpServletRequest request = new MockHttpServletRequest(
                "GET", "/fichiers/ressources/" + fileTenantId + "/guide.pdf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        jakarta.servlet.FilterChain chain = mock(jakarta.servlet.FilterChain.class);
        TenantFilter filter = new TenantFilter(mock(MembreStructureRepository.class));

        filter.doFilter(request, response, chain);

        assertEquals(401, response.getStatus());
        verify(chain, never()).doFilter(request, response);
    }
}
