package sn.jappo.jappo_backend.mission.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import sn.jappo.jappo_backend.config.tenant.TenantContext;
import sn.jappo.jappo_backend.livrable.entity.TypeLivrable;
import sn.jappo.jappo_backend.mission.dto.CreateLivrableAttenduRequest;
import sn.jappo.jappo_backend.mission.dto.LivrableAttenduResponse;
import sn.jappo.jappo_backend.mission.dto.TemplateField;
import sn.jappo.jappo_backend.mission.dto.TemplateFieldCodec;
import sn.jappo.jappo_backend.mission.dto.UpdateLivrableAttenduRequest;
import sn.jappo.jappo_backend.mission.entity.LivrableAttendu;
import sn.jappo.jappo_backend.mission.entity.MissionEtape;
import sn.jappo.jappo_backend.mission.repository.LivrableAttenduRepository;
import sn.jappo.jappo_backend.mission.repository.MissionEtapeRepository;
import sn.jappo.jappo_backend.parcours.entity.Etape;
import sn.jappo.jappo_backend.parcours.entity.Phase;
import sn.jappo.jappo_backend.parcours.repository.EtapeRepository;
import sn.jappo.jappo_backend.parcours.repository.PhaseRepository;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.RoleGlobal;
import sn.jappo.jappo_backend.user.entity.User;
import sn.jappo.jappo_backend.user.repository.UserRepository;

@SpringBootTest
@ActiveProfiles("test")
class LivrableAttenduServiceTest {

    @Autowired
    private LivrableAttenduService livrableAttenduService;

    @Autowired
    private LivrableAttenduRepository livrableAttenduRepository;

    @Autowired
    private MissionEtapeRepository missionEtapeRepository;

    @Autowired
    private EtapeRepository etapeRepository;

    @Autowired
    private PhaseRepository phaseRepository;

    @Autowired
    private StructureRepository structureRepository;

    @Autowired
    private UserRepository userRepository;

    private Structure structure;
    private Structure otherStructure;
    private MissionEtape missionEtape;

    @BeforeEach
    void setUp() {
        TenantContext.clear();

        User owner = new User();
        owner.setPrenom("Test");
        owner.setNom("Owner");
        owner.setEmail("owner-test-" + UUID.randomUUID() + "@jappo.sn");
        owner.setPassword("secret");
        owner.setRoleGlobal(RoleGlobal.USER);
        userRepository.save(owner);

        User otherOwner = new User();
        otherOwner.setPrenom("Other");
        otherOwner.setNom("Owner");
        otherOwner.setEmail("owner-other-" + UUID.randomUUID() + "@jappo.sn");
        otherOwner.setPassword("secret");
        otherOwner.setRoleGlobal(RoleGlobal.USER);
        userRepository.save(otherOwner);

        structure = new Structure();
        structure.setNom("Structure A");
        structure.setSlug("structure-a-" + UUID.randomUUID());
        structure.setProprietaire(owner);
        structureRepository.save(structure);

        otherStructure = new Structure();
        otherStructure.setNom("Structure B");
        otherStructure.setSlug("structure-b-" + UUID.randomUUID());
        otherStructure.setProprietaire(otherOwner);
        structureRepository.save(otherStructure);

        Phase phase = new Phase();
        phase.setNom("Phase de test");
        phase.setStructure(structure);
        phaseRepository.save(phase);

        Etape etape = new Etape();
        etape.setNom("Étape de test");
        etape.setOrdre(1);
        etape.setObligatoire(true);
        etape.setStructure(structure);
        etape.setPhase(phase);
        etapeRepository.save(etape);

        missionEtape = new MissionEtape();
        missionEtape.setTitre("Construire le BMC");
        missionEtape.setDescription("Mission de test");
        missionEtape.setOrdre(1);
        missionEtape.setObligatoire(true);
        missionEtape.setStructure(structure);
        missionEtape.setEtape(etape);
        missionEtapeRepository.save(missionEtape);

        TenantContext.setCurrentTenant(structure.getId());
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void createLivrableAttendu_success() {
        CreateLivrableAttenduRequest request = new CreateLivrableAttenduRequest(
                "Business Model Canvas",
                "Plan stratégique",
                1,
                true,
                TypeLivrable.TEMPLATE,
                "BMC"
        );

        LivrableAttenduResponse response = livrableAttenduService.createLivrableAttendu(
                missionEtape.getEtape().getId(),
                missionEtape.getId(),
                request
        );

        assertThat(response.titre()).isEqualTo("Business Model Canvas");
        assertThat(response.type()).isEqualTo(TypeLivrable.TEMPLATE);
        assertThat(response.templateType()).isEqualTo("BMC");
        assertThat(response.obligatoire()).isTrue();
    }

    @Test
    void createBmcLivrableAttendu_persistsEditableFields() {
        List<TemplateField> fields = List.of(
                new TemplateField("valueProposition", "Proposition de valeur"),
                new TemplateField("customerSegments", "Segments clients")
        );
        CreateLivrableAttenduRequest request = new CreateLivrableAttenduRequest(
                "Business Model Canvas",
                "Complétez les rubriques.",
                1,
                true,
                TypeLivrable.TEMPLATE,
                "BMC",
                fields
        );

        LivrableAttenduResponse response = livrableAttenduService.createLivrableAttendu(
                missionEtape.getEtape().getId(),
                missionEtape.getId(),
                request
        );

        assertThat(response.templateFields()).containsExactlyElementsOf(fields);
        LivrableAttendu persisted = livrableAttenduRepository.findById(response.id()).orElseThrow();
        assertThat(TemplateFieldCodec.decode(persisted.getTemplateConfiguration())).containsExactlyElementsOf(fields);
    }

    @Test
    void updateBmcTemplateType_clearsOldFieldsAndPartialUpdatePreservesConfiguration() {
        TemplateField field = new TemplateField("valueProposition", "Proposition de valeur");
        LivrableAttendu attendu = new LivrableAttendu();
        attendu.setTitre("BMC");
        attendu.setType(TypeLivrable.TEMPLATE);
        attendu.setTemplateType("BMC");
        attendu.setTemplateConfiguration(TemplateFieldCodec.encode(List.of(field)));
        attendu.setOrdre(1);
        attendu.setMissionEtape(missionEtape);
        livrableAttenduRepository.save(attendu);

        LivrableAttenduResponse unchanged = livrableAttenduService.updateLivrableAttendu(
                missionEtape.getEtape().getId(),
                missionEtape.getId(),
                attendu.getId(),
                new UpdateLivrableAttenduRequest(null, "Description modifiée", null, null, null, null)
        );
        assertThat(unchanged.templateFields()).containsExactly(field);

        LivrableAttenduResponse changed = livrableAttenduService.updateLivrableAttendu(
                missionEtape.getEtape().getId(),
                missionEtape.getId(),
                attendu.getId(),
                new UpdateLivrableAttenduRequest(null, null, null, null, TypeLivrable.TEMPLATE, "BUSINESS_PLAN")
        );

        assertThat(changed.templateType()).isEqualTo("BUSINESS_PLAN");
        assertThat(changed.templateFields()).isEmpty();
    }

    @Test
    void getLivrablesByMissionEtape_returnsOrderedLivrables() {
        LivrableAttendu first = new LivrableAttendu();
        first.setTitre("Présentation");
        first.setDescription("Présentation du projet");
        first.setOrdre(2);
        first.setObligatoire(true);
        first.setType(TypeLivrable.FICHIER);
        first.setMissionEtape(missionEtape);

        LivrableAttendu second = new LivrableAttendu();
        second.setTitre("BMC");
        second.setDescription("Business Model Canvas");
        second.setOrdre(1);
        second.setObligatoire(true);
        second.setType(TypeLivrable.TEMPLATE);
        second.setTemplateType("BMC");
        second.setMissionEtape(missionEtape);

        livrableAttenduRepository.saveAll(List.of(first, second));

        List<LivrableAttenduResponse> result = livrableAttenduService.getLivrablesByMissionEtape(
                missionEtape.getEtape().getId(),
                missionEtape.getId()
        );

        assertThat(result).hasSize(2);
        assertThat(result.get(0).titre()).isEqualTo("BMC");
        assertThat(result.get(1).titre()).isEqualTo("Présentation");
    }

    @Test
    void updateLivrableAttendu_success() {
        LivrableAttendu livrable = new LivrableAttendu();
        livrable.setTitre("Ancien titre");
        livrable.setDescription("Ancienne desc");
        livrable.setOrdre(1);
        livrable.setObligatoire(true);
        livrable.setType(TypeLivrable.FICHIER);
        livrable.setMissionEtape(missionEtape);
        livrableAttenduRepository.save(livrable);

        UpdateLivrableAttenduRequest request = new UpdateLivrableAttenduRequest(
                "Nouveau titre",
                "Nouvelle desc",
                3,
                false,
                TypeLivrable.TEXTE,
                null
        );

        LivrableAttenduResponse response = livrableAttenduService.updateLivrableAttendu(
                missionEtape.getEtape().getId(),
                missionEtape.getId(),
                livrable.getId(),
                request
        );

        assertThat(response.titre()).isEqualTo("Nouveau titre");
        assertThat(response.type()).isEqualTo(TypeLivrable.TEXTE);
        assertThat(response.obligatoire()).isFalse();
        assertThat(response.ordre()).isEqualTo(3);
    }

    @Test
    void deleteLivrableAttendu_success() {
        LivrableAttendu livrable = new LivrableAttendu();
        livrable.setTitre("À supprimer");
        livrable.setOrdre(1);
        livrable.setObligatoire(false);
        livrable.setType(TypeLivrable.LIEN);
        livrable.setMissionEtape(missionEtape);
        livrableAttenduRepository.save(livrable);

        livrableAttenduService.deleteLivrableAttendu(
                missionEtape.getEtape().getId(),
                missionEtape.getId(),
                livrable.getId()
        );

        assertThat(livrableAttenduRepository.findById(livrable.getId())).isEmpty();
    }

    @Test
    void createLivrableAttendu_refuse_whenMissionEtapeFromOtherStructure() {
        MissionEtape otherMission = new MissionEtape();
        otherMission.setTitre("Autre structure");
        otherMission.setOrdre(1);
        otherMission.setObligatoire(true);
        otherMission.setStructure(otherStructure);
        otherMission.setEtape(missionEtape.getEtape());
        missionEtapeRepository.save(otherMission);

        CreateLivrableAttenduRequest request = new CreateLivrableAttenduRequest(
                "Livrable interdit",
                "desc",
                1,
                true,
                TypeLivrable.FICHIER,
                null
        );

        assertThatThrownBy(() -> livrableAttenduService.createLivrableAttendu(
                otherMission.getEtape().getId(),
                otherMission.getId(),
                request
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void updateLivrableAttendu_refuse_whenMissionEtapeFromOtherStructure() {
        LivrableAttendu livrable = new LivrableAttendu();
        livrable.setTitre("Titre");
        livrable.setOrdre(1);
        livrable.setObligatoire(true);
        livrable.setType(TypeLivrable.FICHIER);
        livrable.setMissionEtape(missionEtape);
        livrableAttenduRepository.save(livrable);

        MissionEtape otherMission = new MissionEtape();
        otherMission.setTitre("Autre mission");
        otherMission.setOrdre(2);
        otherMission.setObligatoire(true);
        otherMission.setStructure(otherStructure);
        otherMission.setEtape(missionEtape.getEtape());
        missionEtapeRepository.save(otherMission);

        UpdateLivrableAttenduRequest request = new UpdateLivrableAttenduRequest(
                "Tentative",
                "desc",
                1,
                true,
                TypeLivrable.LIEN,
                null
        );

        assertThatThrownBy(() -> livrableAttenduService.updateLivrableAttendu(
                missionEtape.getEtape().getId(),
                otherMission.getId(),
                livrable.getId(),
                request
        )).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void createLivrableAttendu_keepsObligatoireOrderAndType() {
        CreateLivrableAttenduRequest request = new CreateLivrableAttenduRequest(
                "Document guide",
                "Guide de préparation",
                7,
                false,
                TypeLivrable.TEXTE,
                null
        );

        LivrableAttenduResponse response = livrableAttenduService.createLivrableAttendu(
                missionEtape.getEtape().getId(),
                missionEtape.getId(),
                request
        );

        assertThat(response.ordre()).isEqualTo(7);
        assertThat(response.obligatoire()).isFalse();
        assertThat(response.type()).isEqualTo(TypeLivrable.TEXTE);
    }

    @Test
    void deleteMissionEtape_removesAssociatedLivrables() {
        LivrableAttendu livrable = new LivrableAttendu();
        livrable.setTitre("À garder avant suppression");
        livrable.setOrdre(1);
        livrable.setObligatoire(true);
        livrable.setType(TypeLivrable.FICHIER);
        livrable.setMissionEtape(missionEtape);
        livrableAttenduRepository.save(livrable);

        missionEtapeRepository.delete(missionEtape);

        assertThat(livrableAttenduRepository.findById(livrable.getId())).isEmpty();
    }
}
