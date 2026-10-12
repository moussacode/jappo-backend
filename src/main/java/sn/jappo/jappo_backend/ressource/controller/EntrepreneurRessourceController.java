package sn.jappo.jappo_backend.ressource.controller;

import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.ia.context.AiResourceContextService.ResourceFile;
import sn.jappo.jappo_backend.ressource.dto.RessourceResponse;
import sn.jappo.jappo_backend.ressource.service.EntrepreneurRessourceAccessService;
import sn.jappo.jappo_backend.user.entity.User;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/entrepreneur/ressources")
@PreAuthorize("hasAuthority('ENTREPRENEUR')")
public class EntrepreneurRessourceController {

    private final EntrepreneurRessourceAccessService accessService;

    public EntrepreneurRessourceController(EntrepreneurRessourceAccessService accessService) {
        this.accessService = accessService;
    }

    @GetMapping
    @Operation(summary = "Lister les ressources accessibles à l'entrepreneur")
    public ResponseEntity<List<RessourceResponse>> getAccessibleResources(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(accessService.getAccessibleResources(user.getId(), requireTenant()));
    }

    @GetMapping("/{resourceId}/fichier")
    @Operation(summary = "Ouvrir un fichier de ressource accessible à l'entrepreneur")
    public ResponseEntity<byte[]> getAccessibleFile(
            @PathVariable UUID resourceId,
            @AuthenticationPrincipal User user) {
        ResourceFile file = accessService.getAccessibleFile(user.getId(), requireTenant(), resourceId);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(file.contentType()));
        headers.setContentDisposition(ContentDisposition.inline().filename(file.filename()).build());
        headers.setCacheControl(CacheControl.noStore());
        headers.set("X-Content-Type-Options", "nosniff");
        return new ResponseEntity<>(file.content(), headers, HttpStatus.OK);
    }

    private UUID requireTenant() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.UNAUTHORIZED, "Aucune structure active");
        }
        return tenantId;
    }
}
