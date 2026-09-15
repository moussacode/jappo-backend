package sn.jappo.jappo_backend.structure.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.jappo.jappo_backend.common.util.SlugUtils;
import sn.jappo.jappo_backend.structure.dto.CreateStructureRequest;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;
import sn.jappo.jappo_backend.structure.entity.StatutMembre;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.user.entity.User;


import org.springframework.http.HttpStatus;
import  org.springframework.web.server.ResponseStatusException;
import  sn.jappo.jappo_backend.structure.dto.UpdateStructureRequest;
import java.util.UUID;

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
        // 1. Vérification que l'email est bien vérifié
        if (!user.isEmailVerified()) {
            throw new RuntimeException(
                    "L'adresse email doit être vérifiée avant de créer une structure"
            );
        }

        // 2. Génération du slug unique à partir du nom
        String baseSlug = SlugUtils.makeSlug(request.nom());
        String slug = baseSlug;
        int counter = 1;

        while (structureRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        // 3. Hydratation de l'entité Structure
        Structure structure = new Structure();

        structure.setNom(request.nom());
        structure.setSlug(slug);
        structure.setType(request.type());
        structure.setPays(request.pays());
        structure.setDescription(request.description());
        structure.setEmail(request.email());
        structure.setTelephone(request.telephone());
        structure.setAdresse(request.adresse());
        structure.setVille(request.ville());
        structure.setSiteWeb(request.siteWeb());
        structure.setLogo(request.logo());

        // 🟢 LE CORRECTIF EST ICI : Définir explicitement le créateur comme propriétaire
        structure.setProprietaire(user);

        Structure savedStructure = structureRepository.save(structure);

        // 4. Attribution du rôle ADMIN_STRUCTURE à l'utilisateur créateur
        MembreStructure membre = new MembreStructure();

        membre.setUser(user);
        membre.setStructure(savedStructure);
        membre.setRole(RoleMembreStructure.ADMIN_STRUCTURE);
        membre.setStatut(StatutMembre.ACCEPTE); // Marquer comme directement accepté

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


    @Transactional
public Structure updateStructure(UUID structureId, UpdateStructureRequest request, User user) {
    var membre = membreStructureRepository.findByUserIdAndStructureId(user.getId(), structureId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Accès refusé."));

    if (membre.getRole() != RoleMembreStructure.ADMIN_STRUCTURE) {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                "Seul un administrateur de la structure peut modifier ces informations.");
    }

    Structure structure = structureRepository.findById(structureId)
            .orElseThrow(() -> new RuntimeException("Structure introuvable"));

    if (request.nom() != null) structure.setNom(request.nom());
    if (request.type() != null) structure.setType(request.type());
    if (request.pays() != null) structure.setPays(request.pays());
    if (request.description() != null) structure.setDescription(request.description());
    if (request.email() != null) structure.setEmail(request.email());
    if (request.telephone() != null) structure.setTelephone(request.telephone());
    if (request.adresse() != null) structure.setAdresse(request.adresse());
    if (request.ville() != null) structure.setVille(request.ville());
    if (request.siteWeb() != null) structure.setSiteWeb(request.siteWeb());
    if (request.logo() != null) structure.setLogo(request.logo());

    return structureRepository.save(structure);
}
}