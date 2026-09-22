package sn.jappo.jappo_backend.meeting.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.meeting.dto.CreateMeetingRequest;
import sn.jappo.jappo_backend.meeting.dto.JoinMeetingResponse;
import sn.jappo.jappo_backend.meeting.dto.MeetingParticipantResponse;
import sn.jappo.jappo_backend.meeting.dto.MeetingResponse;
import sn.jappo.jappo_backend.meeting.entity.Meeting;
import sn.jappo.jappo_backend.meeting.entity.MeetingParticipant;
import sn.jappo.jappo_backend.meeting.enums.MeetingMode;
import sn.jappo.jappo_backend.meeting.enums.MeetingStatus;
import sn.jappo.jappo_backend.meeting.enums.MeetingType;
import sn.jappo.jappo_backend.meeting.enums.ParticipantRole;
import sn.jappo.jappo_backend.meeting.exception.MeetingException;
import sn.jappo.jappo_backend.meeting.livekit.LiveKitRoomClient;
import sn.jappo.jappo_backend.meeting.repository.MeetingParticipantRepository;
import sn.jappo.jappo_backend.meeting.repository.MeetingRepository;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MeetingService {

    private static final Logger log = LoggerFactory.getLogger(MeetingService.class);

    private final MeetingRepository meetingRepository;
    private final MeetingParticipantRepository participantRepository;
    private final MembreStructureRepository membreStructureRepository;
    private final StructureRepository structureRepository;
    private final UserRepository userRepository;
    private final CohorteRepository cohorteRepository;
    private final ProjetRepository projetRepository;
    private final LiveKitRoomClient liveKitRoomClient;
    private final ApplicationEventPublisher eventPublisher;

    private static final SecureRandom random = new SecureRandom();

    public MeetingService(
            MeetingRepository meetingRepository,
            MeetingParticipantRepository participantRepository,
            MembreStructureRepository membreStructureRepository,
            StructureRepository structureRepository,
            UserRepository userRepository,
            CohorteRepository cohorteRepository,
            ProjetRepository projetRepository,
            LiveKitRoomClient liveKitRoomClient,
            ApplicationEventPublisher eventPublisher
    ) {
        this.meetingRepository = meetingRepository;
        this.participantRepository = participantRepository;
        this.membreStructureRepository = membreStructureRepository;
        this.structureRepository = structureRepository;
        this.userRepository = userRepository;
        this.cohorteRepository = cohorteRepository;
        this.projetRepository = projetRepository;
        this.liveKitRoomClient = liveKitRoomClient;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public MeetingResponse createMeeting(CreateMeetingRequest request, UUID coachMembreStructureId) {
        log.info("Creating meeting - type={}, participantId={}, cohortId={}",
            request.type(), request.participantId(), request.cohortId());

        UUID structureId = getRequiredTenantId();

        // 1. Valider que le coach appartient à la structure et a le rôle COACH
        MembreStructure coach = membreStructureRepository.findById(coachMembreStructureId)
                .orElseThrow(() -> new MeetingException("Coach non trouvé"));

        if (!coach.getStructure().getId().equals(structureId)) {
            throw new MeetingException("Le coach n'appartient pas à la structure active");
        }

        if (coach.getRole() != RoleMembreStructure.COACH && coach.getRole() != RoleMembreStructure.ADMIN_STRUCTURE) {
            throw new MeetingException("Seuls les coaches et admins peuvent créer des réunions");
        }

        // 2. Valider le type de réunion et les participants
        List<MembreStructure> participantMembers = new ArrayList<>();
        Cohorte cohort = null;

        if (request.type() == MeetingType.INDIVIDUAL) {
            if (request.participantId() == null) {
                throw new MeetingException("participantId est obligatoire pour une réunion INDIVIDUAL");
            }
            if (request.cohortId() != null) {
                throw new MeetingException("cohortId ne doit pas être présent pour une réunion INDIVIDUAL");
            }

            // Valider que l'entrepreneur appartient à la structure
            MembreStructure entrepreneurMember = membreStructureRepository.findByUserIdAndStructureId(request.participantId(), structureId)
                    .orElseThrow(() -> new MeetingException("Entrepreneur non trouvé dans cette structure"));

            if (entrepreneurMember.getRole() != RoleMembreStructure.ENTREPRENEUR) {
                throw new MeetingException("Le participant doit être un entrepreneur");
            }

            participantMembers.add(entrepreneurMember);

        } else if (request.type() == MeetingType.GROUP) {
            if (request.cohortId() == null) {
                throw new MeetingException("cohortId est obligatoire pour une réunion GROUP");
            }
            if (request.participantId() != null) {
                throw new MeetingException("participantId ne doit pas être présent pour une réunion GROUP");
            }

            // Valider que la cohorte appartient à la structure
            cohort = cohorteRepository.findByIdAndStructureId(request.cohortId(), structureId)
                    .orElseThrow(() -> new MeetingException("Cohorte non trouvée dans cette structure"));

            // Entrepreneurs des projets actifs de la cohorte (pas le champ dénormalisé MembreStructure.cohorte)
            List<Projet> projetsActifs = projetRepository
                    .findAllByCohorteIdAndStructureIdAndArchive(request.cohortId(), structureId, false);
            List<MembreStructure> cohortEntrepreneurs = new ArrayList<>();
            for (Projet projet : projetsActifs) {
                if (projet.getEntrepreneur() == null) {
                    continue;
                }
                membreStructureRepository
                        .findByUserIdAndStructureId(projet.getEntrepreneur().getId(), structureId)
                        .filter(m -> m.getRole() == RoleMembreStructure.ENTREPRENEUR)
                        .ifPresent(m -> {
                            if (cohortEntrepreneurs.stream().noneMatch(existant -> existant.getId().equals(m.getId()))) {
                                cohortEntrepreneurs.add(m);
                            }
                        });
            }

            if (cohortEntrepreneurs.isEmpty()) {
                throw new MeetingException("Aucun entrepreneur trouvé dans cette cohorte");
            }

            participantMembers = cohortEntrepreneurs;
        }

        // 3. Générer un identifiant de room unique (seulement pour ONLINE)
        // 3. Déterminer l'identifiant de salle
String roomIdentifier;

if (request.mode() == MeetingMode.ONLINE) {
    // Pour une réunion en ligne, générer une room LiveKit unique
    roomIdentifier = generateRoomIdentifier();

    try {
        liveKitRoomClient.createRoom(roomIdentifier);
    } catch (Exception e) {
        throw new MeetingException("Impossible de créer la room LiveKit", e);
    }

} else {
    // Pour une réunion présentielle, roomIdentifier correspond au nom de la salle
    if (request.roomIdentifier() == null || request.roomIdentifier().isBlank()) {
        throw new MeetingException("La salle est obligatoire pour une réunion en présentiel");
    }

    roomIdentifier = request.roomIdentifier().trim();
}
        // 5. Créer la réunion
        Meeting meeting = new Meeting();
        meeting.setStructure(coach.getStructure());
        meeting.setCoach(coach);
        meeting.setTitle(request.title());
        meeting.setDescription(request.description());
        meeting.setType(request.type());
        meeting.setMode(request.mode());
        meeting.setCohort(cohort);
        meeting.setLocation(request.location());
        meeting.setAddress(request.address());
        meeting.setScheduledAt(request.scheduledAt());
        meeting.setDurationMinutes(request.durationMinutes());
        meeting.setStatus(MeetingStatus.PLANNED);
        meeting.setRoomIdentifier(roomIdentifier);

        Meeting savedMeeting;
        try {
            savedMeeting = meetingRepository.save(meeting);
        } catch (Exception e) {
            // En cas d'erreur DB, tenter de nettoyer la room LiveKit (seulement si ONLINE)
            if (request.mode() == MeetingMode.ONLINE && roomIdentifier != null) {
                liveKitRoomClient.deleteRoom(roomIdentifier);
            }
            throw new MeetingException("Erreur lors de la création de la réunion", e);
        }

        // 6. Créer les participants
        try {
            // Coach en tant que HOST
            MeetingParticipant coachParticipant = new MeetingParticipant();
            coachParticipant.setMeeting(savedMeeting);
            coachParticipant.setMembreStructure(coach);
            coachParticipant.setRole(ParticipantRole.HOST);
            participantRepository.save(coachParticipant);

            // Participants ATTENDEE
            for (MembreStructure participantMember : participantMembers) {
                MeetingParticipant participant = new MeetingParticipant();
                participant.setMeeting(savedMeeting);
                participant.setMembreStructure(participantMember);
                participant.setRole(ParticipantRole.ATTENDEE);
                participantRepository.save(participant);
            }
        } catch (Exception e) {
            // En cas d'erreur lors de la création des participants, tenter de nettoyer
            if (request.mode() == MeetingMode.ONLINE && roomIdentifier != null) {
                liveKitRoomClient.deleteRoom(roomIdentifier);
            }
            meetingRepository.delete(savedMeeting);
            throw new MeetingException("Erreur lors de la création des participants", e);
        }

        return mapToResponse(savedMeeting);
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetings(UUID cohorteId) {
        UUID structureId = getRequiredTenantId();
        List<Meeting> meetings = meetingRepository.findAllByStructureId(structureId);
        if (cohorteId != null) {
            meetings = meetings.stream()
                    .filter(m -> m.getCohort() != null && cohorteId.equals(m.getCohort().getId()))
                    .toList();
        }
        return meetings.stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MeetingResponse> getMeetingsByParticipant(UUID membreStructureId) {
        UUID structureId = getRequiredTenantId();
        return participantRepository.findAllByMembreStructureId(membreStructureId).stream()
                .map(p -> p.getMeeting())
                .filter(m -> m.getStructure().getId().equals(structureId))
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MeetingResponse getMeetingById(UUID id) {
        UUID structureId = getRequiredTenantId();
        Meeting meeting = meetingRepository.findByIdAndStructureId(id, structureId)
                .orElseThrow(() -> new MeetingException("Réunion non trouvée"));
        return mapToResponse(meeting);
    }

    @Transactional
    public JoinMeetingResponse joinMeeting(UUID meetingId, UUID membreStructureId) {
        UUID structureId = getRequiredTenantId();

        Meeting meeting = meetingRepository.findByIdAndStructureId(meetingId, structureId)
                .orElseThrow(() -> new MeetingException("Réunion non trouvée"));

        if (meeting.getStatus() == MeetingStatus.ENDED || meeting.getStatus() == MeetingStatus.CANCELLED) {
            throw new MeetingException("Cette réunion est terminée ou annulée");
        }

        MeetingParticipant participant = participantRepository.findByMeetingIdAndMembreStructureId(meetingId, membreStructureId)
                .orElseThrow(() -> new MeetingException("Vous n'êtes pas participant à cette réunion"));

        // Mettre à jour joinedAt
        participant.setJoinedAt(LocalDateTime.now());
        participantRepository.save(participant);

        // Passer la réunion à ONGOING si c'est le premier participant
        if (meeting.getStatus() == MeetingStatus.PLANNED) {
            meeting.setStatus(MeetingStatus.ONGOING);
            meetingRepository.save(meeting);
        }

        // Générer le token LiveKit
        if (meeting.getMode() == MeetingMode.PRESENTIEL) {
    throw new MeetingException("Cette réunion est en présentiel et ne nécessite pas de connexion LiveKit");
}

boolean isHost = participant.getRole() == ParticipantRole.HOST;

String token = liveKitRoomClient.generateToken(
        meeting.getRoomIdentifier(),
        participant.getMembreStructure().getUser().getId(),
        isHost
);
        return new JoinMeetingResponse(
                meeting.getRoomIdentifier(),
                liveKitRoomClient.getHost(),
                token,
                isHost
        );
    }

    @Transactional
    public void leaveMeeting(UUID meetingId, UUID membreStructureId) {
        UUID structureId = getRequiredTenantId();

        Meeting meeting = meetingRepository.findByIdAndStructureId(meetingId, structureId)
                .orElseThrow(() -> new MeetingException("Réunion non trouvée"));

        MeetingParticipant participant = participantRepository.findByMeetingIdAndMembreStructureId(meetingId, membreStructureId)
                .orElseThrow(() -> new MeetingException("Vous n'êtes pas participant à cette réunion"));

        participant.setLeftAt(LocalDateTime.now());
        participantRepository.save(participant);
    }

    @Transactional
    public void endMeeting(UUID meetingId, UUID membreStructureId) {
        UUID structureId = getRequiredTenantId();

        Meeting meeting = meetingRepository.findByIdAndStructureId(meetingId, structureId)
                .orElseThrow(() -> new MeetingException("Réunion non trouvée"));

        // Vérifier que l'utilisateur est le host
        MeetingParticipant participant = participantRepository.findByMeetingIdAndMembreStructureId(meetingId, membreStructureId)
                .orElseThrow(() -> new MeetingException("Vous n'êtes pas participant à cette réunion"));

        if (participant.getRole() != ParticipantRole.HOST) {
            throw new MeetingException("Seul le host peut terminer la réunion");
        }

        // Supprimer la room LiveKit
        // Supprimer la room LiveKit uniquement pour les réunions en ligne
if (meeting.getMode() == MeetingMode.ONLINE) {
    liveKitRoomClient.deleteRoom(meeting.getRoomIdentifier());
}
        // Mettre à jour le statut de la réunion
        meeting.setStatus(MeetingStatus.ENDED);
        meetingRepository.save(meeting);

        // Enregistrer les départs des participants qui n'ont pas quitté
        List<MeetingParticipant> activeParticipants = participantRepository.findByMeetingIdAndRole(meetingId, ParticipantRole.ATTENDEE);
        for (MeetingParticipant p : activeParticipants) {
            if (p.getLeftAt() == null) {
                p.setLeftAt(LocalDateTime.now());
                participantRepository.save(p);
            }
        }
    }

    private UUID getRequiredTenantId() {
        UUID tenantId = TenantContext.getCurrentTenant();
        if (tenantId == null) {
            throw new IllegalStateException("Aucune structure active sélectionnée");
        }
        return tenantId;
    }

    private String generateRoomIdentifier() {
        return "meeting_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
    }

    private MeetingResponse mapToResponse(Meeting meeting) {
        List<MeetingParticipantResponse> participants = participantRepository.findAllByMeeting(meeting).stream()
                .map(p -> new MeetingParticipantResponse(
                        p.getId(),
                        p.getMembreStructure().getUser().getId(),
                        p.getMembreStructure().getUser().getPrenom() + " " + p.getMembreStructure().getUser().getNom(),
                        p.getRole(),
                        p.getJoinedAt(),
                        p.getLeftAt()
                ))
                .toList();

        return new MeetingResponse(
                meeting.getId(),
                meeting.getStructure().getId(),
                meeting.getCoach().getId(),
                meeting.getCoach().getUser().getPrenom() + " " + meeting.getCoach().getUser().getNom(),
                meeting.getTitle(),
                meeting.getDescription(),
                meeting.getType(),
                meeting.getMode(),
                meeting.getCohort() != null ? meeting.getCohort().getId() : null,
                meeting.getCohort() != null ? meeting.getCohort().getNom() : null,
                meeting.getLocation(),
                meeting.getAddress(),
                meeting.getScheduledAt(),
                meeting.getDurationMinutes(),
                meeting.getStatus(),
                meeting.getCreatedAt(),
                meeting.getUpdatedAt(),
                participants
        );
    }
}
