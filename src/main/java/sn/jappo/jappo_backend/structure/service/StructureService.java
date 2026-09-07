package sn.jappo.jappo_backend.structure.service;

import java.util.List;

import org.springframework.stereotype.Service;

import sn.jappo.jappo_backend.structure.dto.CreateStructureRequest;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;

import org.springframework.transaction.annotation.Transactional;
import sn.jappo.jappo_backend.user.entity.User;

@Service
public class StructureService {

    private final StructureRepository structureRepository;
    private final MembreStructureRepository membreStructureRepository;

    public StructureService(
            StructureRepository structureRepository,
            MembreStructureRepository membreStructureRepository
    ) {
        this.structureRepository = structureRepository;
        this.membreStructureRepository = membreStructureRepository;
    }


    @Transactional
public Structure createStructure(
        CreateStructureRequest request,
        User user
) {

    if (!user.isEmailVerified()) {
        throw new RuntimeException(
                "L'adresse email doit être vérifiée avant de créer une structure"
        );
    }

    Structure structure = new Structure();

    structure.setNom(request.nom());
    structure.setType(request.type());
    structure.setPays(request.pays());
    structure.setDescription(request.description());
    structure.setEmail(request.email());
    structure.setTelephone(request.telephone());
    structure.setAdresse(request.adresse());
    structure.setVille(request.ville());
    structure.setSiteWeb(request.siteWeb());
    structure.setLogo(request.logo());

    Structure savedStructure = structureRepository.save(structure);

    MembreStructure membre = new MembreStructure();

    membre.setUser(user);
    membre.setStructure(savedStructure);
    membre.setRole(RoleMembreStructure.ADMIN_STRUCTURE);

    membreStructureRepository.save(membre);

    return savedStructure;
}


public List<Structure> getMyStructures(User user) {

    return membreStructureRepository
            .findAllByUser(user)
            .stream()
            .map(MembreStructure::getStructure)
            .toList();
}
}