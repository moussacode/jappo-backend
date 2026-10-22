package sn.jappo.jappo_backend.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.auth.service.JwtService;
import sn.jappo.jappo_backend.projet.entity.Projet;
import sn.jappo.jappo_backend.projet.entity.StatutProjet;
import sn.jappo.jappo_backend.projet.repository.ProjetRepository;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.entity.StatutStructure;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.RoleGlobal;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

/**
 * Matrice des rôles vérifiée au niveau HTTP : JWT réel, rôle calculé pour la structure
 * active (X-Structure-Id), TenantFilter et @PreAuthorize.
 */
// Clé de test uniquement (base64, 256 bits) : la valeur du profil "test" n'est pas encodée en base64.
@SpringBootTest(properties = "app.jwt.secret=dGVzdC1vbmx5LWp3dC1rZXktZm9yLXJvbGUtbWF0cml4LXRlc3Q=")
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class RoleMatrixSecurityTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private JwtService jwtService;
    @Autowired private UserRepository userRepository;
    @Autowired private StructureRepository structureRepository;
    @Autowired private MembreStructureRepository membreStructureRepository;
    @Autowired private ProjetRepository projetRepository;

    private Structure structure;
    private Structure autreStructure;
    private User admin;
    private User coach;
    private User entrepreneur;
    private Projet projetEntrepreneur;
    private Projet projetAutreEntrepreneur;

    @BeforeEach
    void setUp() {
        admin = user("admin");
        coach = user("coach");
        entrepreneur = user("entrepreneur");
        User autreEntrepreneur = user("autre");

        structure = structure(admin);
        autreStructure = structure(user("owner2"));

        membre(admin, structure, RoleMembreStructure.ADMIN_STRUCTURE);
        membre(coach, structure, RoleMembreStructure.COACH);
        membre(entrepreneur, structure, RoleMembreStructure.ENTREPRENEUR);
        membre(autreEntrepreneur, structure, RoleMembreStructure.ENTREPRENEUR);

        projetEntrepreneur = projet(entrepreneur);
        projetAutreEntrepreneur = projet(autreEntrepreneur);
    }

    @Test
    void entrepreneur_nAccedePasAuxListesNiAuPilotage() throws Exception {
        mockMvc.perform(as(entrepreneur, get("/api/projets"))).andExpect(status().isForbidden());
        mockMvc.perform(as(entrepreneur, get("/api/progression"))).andExpect(status().isForbidden());
        mockMvc.perform(as(entrepreneur, get("/api/dashboard/stats"))).andExpect(status().isForbidden());
        mockMvc.perform(as(entrepreneur, get("/api/ressources"))).andExpect(status().isForbidden());
        mockMvc.perform(as(entrepreneur, get("/api/cohortes/" + UUID.randomUUID() + "/rapport")))
                .andExpect(status().isForbidden());
    }

    @Test
    void entrepreneur_nePeutPasGererLesRessourcesNiArchiverUnProjet() throws Exception {
        mockMvc.perform(as(entrepreneur, post("/api/ressources")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"titre\":\"Intrusion\",\"url\":\"https://example.test\"}")))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(entrepreneur, delete("/api/projets/" + projetEntrepreneur.getId())))
                .andExpect(status().isForbidden());
        mockMvc.perform(as(entrepreneur, post("/api/projets/" + projetEntrepreneur.getId() + "/cloturer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"issue\":\"DIPLOME\",\"motif\":\"auto-diplôme\",\"forcer\":true}")))
                .andExpect(status().isForbidden());
    }

    @Test
    void entrepreneur_litSonProjetMaisPasCeluiDunAutre() throws Exception {
        mockMvc.perform(as(entrepreneur, get("/api/projets/" + projetEntrepreneur.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(as(entrepreneur, get("/api/projets/" + projetAutreEntrepreneur.getId())))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(entrepreneur, get("/api/suivi/projet/" + projetAutreEntrepreneur.getId())))
                .andExpect(status().isNotFound());
        mockMvc.perform(as(entrepreneur, get("/api/entrepreneur/ressources")))
                .andExpect(status().isOk());
    }

    @Test
    void encadrants_accedentAuPilotageDeLaStructure() throws Exception {
        mockMvc.perform(as(coach, get("/api/projets"))).andExpect(status().isOk());
        mockMvc.perform(as(coach, get("/api/projets/" + projetAutreEntrepreneur.getId())))
                .andExpect(status().isOk());
        mockMvc.perform(as(admin, get("/api/dashboard/stats"))).andExpect(status().isOk());
        mockMvc.perform(as(admin, get("/api/ressources"))).andExpect(status().isOk());
    }

    @Test
    void adminDUneStructure_estRefuseSurUneAutreStructure() throws Exception {
        mockMvc.perform(get("/api/projets")
                        .header("Authorization", "Bearer " + jwtService.generateToken(admin))
                        .header("X-Structure-Id", autreStructure.getId().toString()))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder as(User user, MockHttpServletRequestBuilder request) {
        return request
                .header("Authorization", "Bearer " + jwtService.generateToken(user))
                .header("X-Structure-Id", structure.getId().toString());
    }

    private User user(String prefixe) {
        User user = new User();
        user.setPrenom("Test");
        user.setNom(prefixe);
        user.setEmail(prefixe + "-" + UUID.randomUUID() + "@test.local");
        user.setPassword("secret");
        user.setRoleGlobal(RoleGlobal.USER);
        return userRepository.save(user);
    }

    private Structure structure(User proprietaire) {
        Structure s = new Structure();
        s.setNom("Structure");
        s.setSlug("structure-" + UUID.randomUUID());
        s.setProprietaire(proprietaire);
        s.setStatut(StatutStructure.ACTIVE);
        return structureRepository.save(s);
    }

    private void membre(User user, Structure s, RoleMembreStructure role) {
        MembreStructure membre = new MembreStructure();
        membre.setUser(user);
        membre.setStructure(s);
        membre.setRole(role);
        membre.setStatut(StatutMembre.ACCEPTE);
        membreStructureRepository.save(membre);
    }

    private Projet projet(User proprietaire) {
        Projet projet = new Projet();
        projet.setNom("Projet " + proprietaire.getNom());
        projet.setStructure(structure);
        projet.setStatut(StatutProjet.ACTIF);
        projet.setEntrepreneur(proprietaire);
        return projetRepository.save(projet);
    }
}
