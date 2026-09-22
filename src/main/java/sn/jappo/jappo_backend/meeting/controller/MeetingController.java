package sn.jappo.jappo_backend.meeting.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.meeting.dto.CreateMeetingRequest;
import sn.jappo.jappo_backend.meeting.dto.JoinMeetingResponse;
import sn.jappo.jappo_backend.meeting.dto.MeetingResponse;
import sn.jappo.jappo_backend.meeting.exception.MeetingException;
import sn.jappo.jappo_backend.meeting.service.MeetingService;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.user.entity.User;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/meetings")
public class MeetingController {

    private final MeetingService meetingService;
    private final MembreStructureRepository membreStructureRepository;

    public MeetingController(
            MeetingService meetingService,
            MembreStructureRepository membreStructureRepository
    ) {
        this.meetingService = meetingService;
        this.membreStructureRepository = membreStructureRepository;
    }

    @PostMapping
    public ResponseEntity<MeetingResponse> createMeeting(
            @RequestBody CreateMeetingRequest request,
            Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            UUID structureId = TenantContext.getCurrentTenant();

            if (structureId == null) {
                throw new MeetingException("Aucune structure active sélectionnée (en-tête X-Structure-Id manquant).");
            }

            MembreStructure coach = membreStructureRepository
                    .findByUserIdAndStructureId(currentUser.getId(), structureId)
                    .orElseThrow(() -> new MeetingException("Vous n'êtes pas membre de cette structure."));

            MeetingResponse response = meetingService.createMeeting(request, coach.getId());
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (MeetingException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<MeetingResponse>> getMeetings(
            @RequestParam(required = false) UUID cohorteId
    ) {
        return ResponseEntity.ok(meetingService.getMeetings(cohorteId));
    }

    @GetMapping("/my")
    public ResponseEntity<List<MeetingResponse>> getMyMeetings(Authentication authentication) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            UUID structureId = TenantContext.getCurrentTenant();

            MembreStructure membreStructure = membreStructureRepository
                    .findByUserIdAndStructureId(currentUser.getId(), structureId)
                    .orElseThrow(() -> new MeetingException("Vous n'êtes pas membre de cette structure."));

            return ResponseEntity.ok(meetingService.getMeetingsByParticipant(membreStructure.getId()));
        } catch (MeetingException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<MeetingResponse> getMeetingById(@PathVariable UUID id) {
        try {
            return ResponseEntity.ok(meetingService.getMeetingById(id));
        } catch (MeetingException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @PostMapping("/{id}/join")
    public ResponseEntity<JoinMeetingResponse> joinMeeting(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            UUID structureId = TenantContext.getCurrentTenant();

            MembreStructure membreStructure = membreStructureRepository
                    .findByUserIdAndStructureId(currentUser.getId(), structureId)
                    .orElseThrow(() -> new MeetingException("Vous n'êtes pas membre de cette structure."));

            return ResponseEntity.ok(meetingService.joinMeeting(id, membreStructure.getId()));
        } catch (MeetingException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, e.getMessage());
        }
    }

    @PostMapping("/{id}/leave")
    public ResponseEntity<Void> leaveMeeting(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            UUID structureId = TenantContext.getCurrentTenant();

            MembreStructure membreStructure = membreStructureRepository
                    .findByUserIdAndStructureId(currentUser.getId(), structureId)
                    .orElseThrow(() -> new MeetingException("Vous n'êtes pas membre de cette structure."));

            meetingService.leaveMeeting(id, membreStructure.getId());
            return ResponseEntity.noContent().build();
        } catch (MeetingException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    @PostMapping("/{id}/end")
    public ResponseEntity<Void> endMeeting(
            @PathVariable UUID id,
            Authentication authentication
    ) {
        try {
            User currentUser = (User) authentication.getPrincipal();
            UUID structureId = TenantContext.getCurrentTenant();

            MembreStructure membreStructure = membreStructureRepository
                    .findByUserIdAndStructureId(currentUser.getId(), structureId)
                    .orElseThrow(() -> new MeetingException("Vous n'êtes pas membre de cette structure."));

            meetingService.endMeeting(id, membreStructure.getId());
            return ResponseEntity.noContent().build();
        } catch (MeetingException e) {
            throw new org.springframework.web.server.ResponseStatusException(
                    HttpStatus.FORBIDDEN, e.getMessage());
        }
    }
}
