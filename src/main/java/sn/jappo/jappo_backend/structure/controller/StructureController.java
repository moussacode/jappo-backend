package sn.jappo.jappo_backend.structure.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.structure.dto.CreateStructureRequest;
import sn.jappo.jappo_backend.structure.dto.StructureMembershipResponse;
import sn.jappo.jappo_backend.structure.dto.StructureResponse;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.service.StructureService;
import sn.jappo.jappo_backend.user.entity.User;

@RestController
@RequestMapping("/api/structures")
public class StructureController {

    private final StructureService structureService;
    private final MembreStructureRepository membreStructureRepository;

    public StructureController(
            StructureService structureService,
            MembreStructureRepository membreStructureRepository
    ) {
        this.structureService = structureService;
        this.membreStructureRepository = membreStructureRepository;
    }

    @PostMapping
    public ResponseEntity<Structure> createStructure(
            @Valid @RequestBody CreateStructureRequest request,
            Authentication authentication
    ) {

        User user = (User) authentication.getPrincipal();

        Structure structure = structureService.createStructure(
                request,
                user
        );

        return ResponseEntity.ok(structure);
    }
   @GetMapping("/me")
public ResponseEntity<List<StructureMembershipResponse>> getMyStructures(
        Authentication authentication
) {
    User user = (User) authentication.getPrincipal();

    List<StructureMembershipResponse> memberships =
            membreStructureRepository.findAllByUser(user)
                    .stream()
                    .map(membre -> {
                        Structure structure = membre.getStructure();

                        StructureResponse structureResponse =
                                new StructureResponse(
                                        structure.getId(),
                                        structure.getNom(),
                                        structure.getType(),
                                        structure.getPays(),
                                        structure.getDescription(),
                                        structure.getEmail(),
                                        structure.getTelephone(),
                                        structure.getAdresse(),
                                        structure.getVille(),
                                        structure.getSiteWeb(),
                                        structure.getLogo(),
                                        structure.getDateCreation()
                                );

                        return new StructureMembershipResponse(
                                structureResponse,
                                membre.getRole()
                        );
                    })
                    .toList();

    return ResponseEntity.ok(memberships);
}

}