package sn.jappo.jappo_backend.user.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;
import sn.jappo.jappo_backend.user.dto.InvitationResultResponse;
import sn.jappo.jappo_backend.user.dto.InviterEntrepreneurRequest;
import sn.jappo.jappo_backend.user.service.UserService;
import sn.jappo.jappo_backend.user.dto.EntrepreneurResponse;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * Inviter un ou plusieurs entrepreneurs en masse (Bulk Invite)
     * POST /api/users/inviter
     */
    @PostMapping("/inviter")
    public ResponseEntity<InvitationResultResponse> inviterEntrepreneurs(
            @Valid @RequestBody InviterEntrepreneurRequest request
    ) {
        InvitationResultResponse response = userService.inviterEntrepreneurs(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/entrepreneurs")
    public ResponseEntity<List<EntrepreneurResponse>> getEntrepreneursByActiveStructure() {
        return ResponseEntity.ok(userService.getEntrepreneursByActiveStructure());
    }

    // Récupérer les détails d'un entrepreneur par son ID
    @GetMapping("/{id}")
    public ResponseEntity<EntrepreneurResponse> getEntrepreneurById(@PathVariable UUID id) {
        return ResponseEntity.ok(userService.getEntrepreneurById(id));
    }
}