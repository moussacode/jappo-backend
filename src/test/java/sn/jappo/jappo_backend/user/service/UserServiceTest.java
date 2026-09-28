package sn.jappo.jappo_backend.user.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.cohorte.entity.Cohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.dto.EntrepreneurResponse;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;
import sn.jappo.jappo_backend.auth.service.EmailService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private StructureRepository structureRepository;
    @Mock private CohorteRepository cohorteRepository;
    @Mock private MembreStructureRepository membreStructureRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks
    private UserService userService;

    private UUID structureId;
    private Structure structure;

    @BeforeEach
    void setUp() {
        structureId = UUID.randomUUID();
        structure = new Structure();
        structure.setId(structureId);
        structure.setNom("Structure A");
        TenantContext.setCurrentTenant(structureId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
        SecurityContextHolder.clearContext();
    }

    private User createUser(String prenom, String nom, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setPrenom(prenom);
        user.setNom(nom);
        user.setEmail(email);
        return user;
    }

    private MembreStructure createMembreEntrepreneur(User user, Cohorte cohorte) {
        MembreStructure ms = new MembreStructure();
        ms.setId(UUID.randomUUID());
        ms.setUser(user);
        ms.setStructure(structure);
        ms.setCohorte(cohorte);
        ms.setRole(RoleMembreStructure.ENTREPRENEUR);
        ms.setStatut(StatutMembre.ACCEPTE);
        ms.setDateInvitation(LocalDateTime.now());
        return ms;
    }

    private Cohorte createCohorte(String nom) {
        Cohorte cohorte = new Cohorte();
        cohorte.setId(UUID.randomUUID());
        cohorte.setNom(nom);
        cohorte.setStructure(structure);
        return cohorte;
    }

    private void authenticateAsAdmin(User adminUser) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                adminUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN_STRUCTURE"), new SimpleGrantedAuthority("ADMIN_STRUCTURE"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void authenticateAsCoach(User coachUser) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                coachUser,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_COACH"), new SimpleGrantedAuthority("COACH"))
        );
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Test
    @DisplayName("Test 1 — Admin voit tous les entrepreneurs de la structure")
    void test1_adminVoitTousLesEntrepreneurs() {
        User admin = createUser("Admin", "User", "admin@structure.com");
        authenticateAsAdmin(admin);

        Cohorte cohorteA = createCohorte("Cohorte A");
        Cohorte cohorteB = createCohorte("Cohorte B");
        Cohorte cohorteC = createCohorte("Cohorte C");

        User e1 = createUser("E1", "Alpha", "e1@test.com");
        User e2 = createUser("E2", "Beta", "e2@test.com");
        User e3 = createUser("E3", "Gamma", "e3@test.com");

        MembreStructure ms1 = createMembreEntrepreneur(e1, cohorteA);
        MembreStructure ms2 = createMembreEntrepreneur(e2, cohorteB);
        MembreStructure ms3 = createMembreEntrepreneur(e3, cohorteC);

        when(membreStructureRepository.findByStructureIdAndRole(structureId, RoleMembreStructure.ENTREPRENEUR))
                .thenReturn(List.of(ms1, ms2, ms3));

        List<EntrepreneurResponse> result = userService.getEntrepreneursByActiveStructure();

        assertThat(result).hasSize(3);
        assertThat(result).extracting(EntrepreneurResponse::id)
                .containsExactlyInAnyOrder(e1.getId(), e2.getId(), e3.getId());
        verify(membreStructureRepository).findByStructureIdAndRole(structureId, RoleMembreStructure.ENTREPRENEUR);
    }

    @Test
    @DisplayName("Test 2 — Coach affecté à la cohorte A voit uniquement les entrepreneurs de A")
    void test2_coachAffecteACohorteAVoitUniquementA() {
        User coach = createUser("Coach", "Un", "coach1@structure.com");
        authenticateAsCoach(coach);

        Cohorte cohorteA = createCohorte("Cohorte A");
        User e1 = createUser("E1", "Alpha", "e1@test.com");
        MembreStructure ms1 = createMembreEntrepreneur(e1, cohorteA);

        when(membreStructureRepository.findEntrepreneursByCoachAndStructure(
                structureId, coach.getId(), RoleMembreStructure.ENTREPRENEUR))
                .thenReturn(List.of(ms1));

        List<EntrepreneurResponse> result = userService.getEntrepreneursByActiveStructure();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(e1.getId());
        assertThat(result.get(0).nomCohorte()).isEqualTo("Cohorte A");
        verify(membreStructureRepository).findEntrepreneursByCoachAndStructure(
                structureId, coach.getId(), RoleMembreStructure.ENTREPRENEUR);
    }

    @Test
    @DisplayName("Test 3 — Coach affecté aux cohortes A + B voit les entrepreneurs de A et B")
    void test3_coachAffecteAetBVoitEntrepreneursDeAetB() {
        User coach = createUser("Coach", "Multi", "coach@structure.com");
        authenticateAsCoach(coach);

        Cohorte cohorteA = createCohorte("Cohorte A");
        Cohorte cohorteB = createCohorte("Cohorte B");

        User e1 = createUser("E1", "Alpha", "e1@test.com");
        User e2 = createUser("E2", "Beta", "e2@test.com");

        MembreStructure ms1 = createMembreEntrepreneur(e1, cohorteA);
        MembreStructure ms2 = createMembreEntrepreneur(e2, cohorteB);

        when(membreStructureRepository.findEntrepreneursByCoachAndStructure(
                structureId, coach.getId(), RoleMembreStructure.ENTREPRENEUR))
                .thenReturn(List.of(ms1, ms2));

        List<EntrepreneurResponse> result = userService.getEntrepreneursByActiveStructure();

        assertThat(result).hasSize(2);
        assertThat(result).extracting(EntrepreneurResponse::id)
                .containsExactlyInAnyOrder(e1.getId(), e2.getId());
        verify(membreStructureRepository).findEntrepreneursByCoachAndStructure(
                structureId, coach.getId(), RoleMembreStructure.ENTREPRENEUR);
    }

    @Test
    @DisplayName("Test 4 — Coach accepté mais sans aucune cohorte voit 0 entrepreneur")
    void test4_coachSansCohorteVoitZeroEntrepreneur() {
        User coach = createUser("Coach", "SansCohorte", "coach.sans@structure.com");
        authenticateAsCoach(coach);

        when(membreStructureRepository.findEntrepreneursByCoachAndStructure(
                structureId, coach.getId(), RoleMembreStructure.ENTREPRENEUR))
                .thenReturn(List.of());

        List<EntrepreneurResponse> result = userService.getEntrepreneursByActiveStructure();

        assertThat(result).isEmpty();
        verify(membreStructureRepository).findEntrepreneursByCoachAndStructure(
                structureId, coach.getId(), RoleMembreStructure.ENTREPRENEUR);
    }

    @Test
    @DisplayName("Test 5 — Coach tente d'accéder directement par UUID à un entrepreneur hors de ses cohortes -> 404")
    void test5_coachAccesDirectInterditVersEntrepreneurAutreCohorte() {
        User coach = createUser("Coach", "A", "coach.a@structure.com");
        authenticateAsCoach(coach);

        UUID e2UserId = UUID.randomUUID();

        when(membreStructureRepository.findEntrepreneurByUserIdAndCoach(
                e2UserId, structureId, coach.getId(), RoleMembreStructure.ENTREPRENEUR))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getEntrepreneurById(e2UserId))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(rse.getReason()).contains("Entrepreneur introuvable ou accès non autorisé");
                });

        verify(membreStructureRepository).findEntrepreneurByUserIdAndCoach(
                e2UserId, structureId, coach.getId(), RoleMembreStructure.ENTREPRENEUR);
    }

    @Test
    @DisplayName("Coach accède avec succès à un entrepreneur de sa cohorte -> 200 OK")
    void testCoachAccesSuccesEntrepreneurDeSaCohorte() {
        User coach = createUser("Coach", "A", "coach.a@structure.com");
        authenticateAsCoach(coach);

        Cohorte cohorteA = createCohorte("Cohorte A");
        User e1 = createUser("E1", "Alpha", "e1@test.com");
        MembreStructure ms1 = createMembreEntrepreneur(e1, cohorteA);

        when(membreStructureRepository.findEntrepreneurByUserIdAndCoach(
                e1.getId(), structureId, coach.getId(), RoleMembreStructure.ENTREPRENEUR))
                .thenReturn(Optional.of(ms1));

        EntrepreneurResponse response = userService.getEntrepreneurById(e1.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(e1.getId());
        assertThat(response.prenom()).isEqualTo("E1");
        assertThat(response.nomCohorte()).isEqualTo("Cohorte A");
    }

    @Test
    @DisplayName("Admin accède à n'importe quel entrepreneur de la structure -> 200 OK")
    void testAdminAccesEntrepreneurStructure() {
        User admin = createUser("Admin", "User", "admin@structure.com");
        authenticateAsAdmin(admin);

        Cohorte cohorteB = createCohorte("Cohorte B");
        User e2 = createUser("E2", "Beta", "e2@test.com");
        MembreStructure ms2 = createMembreEntrepreneur(e2, cohorteB);

        when(membreStructureRepository.findByUserIdAndStructureId(e2.getId(), structureId))
                .thenReturn(Optional.of(ms2));

        EntrepreneurResponse response = userService.getEntrepreneurById(e2.getId());

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(e2.getId());
        assertThat(response.nomCohorte()).isEqualTo("Cohorte B");
        verify(membreStructureRepository).findByUserIdAndStructureId(e2.getId(), structureId);
    }
}
