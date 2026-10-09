package sn.jappo.jappo_backend.cohorte.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.cohorte.dto.CreateCohorteRequest;
import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.parcours.entity.Parcours;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.entity.ParcoursPhase;
import sn.jappo.jappo_backend.parcours.repository.ParcoursPhaseRepository;
import sn.jappo.jappo_backend.parcours.repository.ParcoursRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.programme.entity.Programme;
import sn.jappo.jappo_backend.programme.repository.ProgrammeRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.abonnement.service.PlanLimiteService;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
public class CohorteServiceTest {

    @Mock
    private CohorteRepository cohorteRepository;

    @Mock
    private StructureRepository structureRepository;

    @Mock
    private ParcoursRepository parcoursRepository;

    @Mock
    private PhaseRepository phaseRepository;

    @Mock
    private ProgrammeRepository programmeRepository;

    @Mock
    private ParcoursPhaseRepository parcoursPhaseRepository;

    @Mock
    private PlanLimiteService planLimiteService;

    @InjectMocks
    private CohorteService cohorteService;

    private UUID structureId = UUID.randomUUID();
    private UUID autreStructureId = UUID.randomUUID();
    private UUID programmeId = UUID.randomUUID();
    private UUID parcoursId = UUID.randomUUID();
    private UUID phaseId = UUID.randomUUID();

    /**
     * Test : Cohérence programme/parcours - parcours n'appartient pas au programme
     */
    @Test
    public void testCreationCohorte_ParcoursNAppartientPasAuProgramme_Echoue() {
        // Arrange
        Structure structure = new Structure();
        structure.setId(structureId);

        Programme programme = new Programme();
        programme.setId(programmeId);
        programme.setStructure(structure);

        Parcours parcours = new Parcours();
        parcours.setId(parcoursId);
        parcours.setStructure(structure);
        parcours.setProgramme(null); // Parcours sans programme

        Phase phase = new Phase();
        phase.setId(phaseId);
        phase.setStructure(structure);

        ParcoursPhase parcoursPhase = new ParcoursPhase();
        parcoursPhase.setParcours(parcours);
        parcoursPhase.setPhase(phase);

        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(programmeRepository.findByIdAndStructureId(programmeId, structureId)).thenReturn(Optional.of(programme));
        when(parcoursRepository.findByIdAndStructureId(parcoursId, structureId)).thenReturn(Optional.of(parcours));
        when(phaseRepository.findById(phaseId)).thenReturn(Optional.of(phase));
        when(parcoursPhaseRepository.findByParcours_IdAndPhase_Id(parcoursId, phaseId)).thenReturn(Optional.of(parcoursPhase));
        doNothing().when(planLimiteService).verifierCreationCohorte(any());

        CreateCohorteRequest request = new CreateCohorteRequest(
                "Cohorte Test",
                "Description",
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                parcoursId,
                phaseId,
                programmeId,
                null
        );

        // Act & Assert
        // Le parcours n'a pas de programme, donc la cohérence programme/parcours échoue
        // Si un programme est fourni, le parcours doit appartenir à ce programme
        TenantContext.setCurrentTenant(structureId);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            cohorteService.createCohorte(request);
        });
        TenantContext.clear();
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
        assertEquals("Le parcours sélectionné n'appartient pas au programme de la cohorte", ex.getReason());
    }

    /**
     * Test : Programme d'une autre structure refusé
     */
    @Test
    public void testCreationCohorte_ProgrammeAutreStructure_Echoue() {
        // Arrange
        Structure structure = new Structure();
        structure.setId(structureId);

        Structure autreStructure = new Structure();
        autreStructure.setId(autreStructureId);

        Programme programme = new Programme();
        programme.setId(programmeId);
        programme.setStructure(autreStructure); // Programme d'une autre structure

        when(structureRepository.findById(structureId)).thenReturn(Optional.of(structure));
        when(programmeRepository.findByIdAndStructureId(programmeId, structureId)).thenReturn(Optional.empty());
        doNothing().when(planLimiteService).verifierCreationCohorte(any());

        CreateCohorteRequest request = new CreateCohorteRequest(
                "Cohorte Test",
                "Description",
                LocalDate.now(),
                LocalDate.now().plusMonths(3),
                parcoursId,
                phaseId,
                programmeId,
                null
        );

        // Act & Assert
        TenantContext.setCurrentTenant(structureId);
        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> {
            cohorteService.createCohorte(request);
        });
        TenantContext.clear();
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Programme introuvable ou n'appartient pas à votre structure", ex.getReason());
    }
}
