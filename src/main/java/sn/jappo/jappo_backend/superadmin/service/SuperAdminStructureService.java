package sn.jappo.jappo_backend.superadmin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;
import sn.jappo.jappo_backend.structure.entity.StatutStructure;
import sn.jappo.jappo_backend.abonnement.entity.PlanAbonnement;
import sn.jappo.jappo_backend.abonnement.entity.StatutAbonnement;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import sn.jappo.jappo_backend.superadmin.dto.SuperAdminHistoriqueAbonnementResponse;

import sn.jappo.jappo_backend.abonnement.entity.HistoriqueAbonnement;
import sn.jappo.jappo_backend.abonnement.entity.SourceAbonnement;

import org.springframework.data.domain.Pageable;

import sn.jappo.jappo_backend.abonnement.entity.TransactionPaiement;
import sn.jappo.jappo_backend.abonnement.repository.TransactionPaiementRepository;
import sn.jappo.jappo_backend.superadmin.dto.SuperAdminTransactionResponse;

import sn.jappo.jappo_backend.abonnement.entity.Abonnement;
import sn.jappo.jappo_backend.superadmin.dto.SuperAdminStructureDetailResponse;
import sn.jappo.jappo_backend.superadmin.dto.SuperAdminAbonnementResponse;

import sn.jappo.jappo_backend.abonnement.entity.Abonnement;
import sn.jappo.jappo_backend.abonnement.repository.AbonnementRepository;
import sn.jappo.jappo_backend.abonnement.repository.HistoriqueAbonnementRepository;
import sn.jappo.jappo_backend.cohorte.entity.StatutCohorte;
import sn.jappo.jappo_backend.cohorte.repository.CohorteRepository;
import sn.jappo.jappo_backend.structure.entity.MembreStructure;
import sn.jappo.jappo_backend.structure.entity.Structure;
import sn.jappo.jappo_backend.structure.repository.MembreStructureRepository;
import sn.jappo.jappo_backend.structure.repository.StructureRepository;
import sn.jappo.jappo_backend.superadmin.dto.SuperAdminStructureListResponse;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class SuperAdminStructureService {

    private final StructureRepository structureRepository;
    private final MembreStructureRepository membreStructureRepository;
    private final CohorteRepository cohorteRepository;
    private final AbonnementRepository abonnementRepository;
    private final TransactionPaiementRepository transactionPaiementRepository;
    private final HistoriqueAbonnementRepository historiqueAbonnementRepository;

    @Transactional(readOnly = true)
    public List<SuperAdminStructureListResponse> getAllStructures() {

        List<Structure> structures = structureRepository.findAll();

        return structures.stream()
                .map(this::toResponse)
                .toList();
    }

    private SuperAdminStructureListResponse toResponse(Structure structure) {

        // Abonnement actuel de la structure
        Abonnement abonnement = abonnementRepository
                .findByStructureId(structure.getId())
                .orElse(null);

        String plan = abonnement != null
                ? abonnement.getPlan().name()
                : "FREEMIUM";

        String statutStructure = structure.getStatut().name();

String statutAbonnement = abonnement != null
        ? abonnement.getStatut().name()
        : "AUCUN_ABONNEMENT";

        // Nombre total de membres
        long nombreMembres =
                membreStructureRepository.countByStructureId(
                        structure.getId()
                );

        // Nombre de cohortes actuellement en cours
        long nombreCohortesActives =
                cohorteRepository.countByStructureIdAndStatut(
                        structure.getId(),
                        StatutCohorte.EN_COURS
                );

        return new SuperAdminStructureListResponse(
                structure.getId(),
                structure.getNom(),
                structure.getSlug(),
                plan,
                statutStructure,
        statutAbonnement,
                structure.getDateCreation(),
                nombreMembres,
                nombreCohortesActives
        );
    }




    @Transactional(readOnly = true)
public SuperAdminStructureDetailResponse getStructureDetail(UUID structureId) {

    Structure structure = structureRepository.findById(structureId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Structure introuvable"
            ));

    Abonnement abonnement = abonnementRepository
            .findByStructureId(structureId)
            .orElse(null);

    long nombreMembres =
            membreStructureRepository.countByStructureId(structureId);

    long nombreCohortesActives =
            cohorteRepository.countByStructureIdAndStatut(
                    structureId,
                    StatutCohorte.EN_COURS
            );
List<MembreStructure> membres =
        membreStructureRepository.findAllByStructure(structure);

        List<SuperAdminStructureDetailResponse.MembreResponse> membresResponse =
        membres.stream()
                .map(membre -> new SuperAdminStructureDetailResponse.MembreResponse(
                        membre.getId(),
                        membre.getUser().getId(),
                        membre.getUser().getPrenom(),
                        membre.getUser().getNom(),
                        membre.getUser().getEmail(),
                        membre.getRole().name(),
                        membre.getStatut().name()
                ))
                .toList();
    SuperAdminStructureDetailResponse.ProprietaireResponse proprietaire =
            new SuperAdminStructureDetailResponse.ProprietaireResponse(
                    structure.getProprietaire().getId(),
                    structure.getProprietaire().getPrenom(),
                    structure.getProprietaire().getNom(),
                    structure.getProprietaire().getEmail(),
                    structure.getProprietaire().isEmailVerified(),
                    structure.getProprietaire().getRoleGlobal().name()
            );

    SuperAdminStructureDetailResponse.AbonnementResponse abonnementResponse =
            abonnement == null
                    ? null
                    : new SuperAdminStructureDetailResponse.AbonnementResponse(
                            abonnement.getId(),
                            abonnement.getPlan().name(),
                            abonnement.getStatut().name(),
                            abonnement.getDateDebut(),
                            abonnement.getDateFin(),
                            abonnement.isRenouvellementAuto()
                    );

    return new SuperAdminStructureDetailResponse(
            structure.getId(),
            structure.getNom(),
            structure.getSlug(),
            structure.getStatut().name(),
            structure.getType(),
            structure.getPays(),
            structure.getVille(),
            structure.getDescription(),
            structure.getEmail(),
            structure.getTelephone(),
            structure.getAdresse(),
            structure.getSiteWeb(),
            structure.getLogo(),
            structure.getDateCreation(),
            proprietaire,
            abonnementResponse,
            nombreMembres,
            nombreCohortesActives,
            membresResponse
    );
}

@Transactional(readOnly = true)
public List<SuperAdminTransactionResponse> getStructureTransactions(
        UUID structureId
) {
    Structure structure = structureRepository.findById(structureId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Structure introuvable"
            ));

    List<TransactionPaiement> transactions =
            transactionPaiementRepository.findByStructureId(
                    structureId,
                    Pageable.unpaged()
            ).getContent();

    return transactions.stream()
            .map(transaction -> new SuperAdminTransactionResponse(
                    transaction.getId(),
                    structure.getNom(),
                    structure.getId(),
                    transaction.getMontant(),
                    transaction.getDevise(),
                    transaction.getStatut().name(),
                    transaction.getMoyenPaiement(),
                    transaction.getTelephoneClient(),
                    transaction.getRefCommand(),
                    transaction.getTokenPaiement(),
                    transaction.getPlanVise().name(),
                    transaction.getDateCreation(),
                    transaction.getDateConfirmation(),
                    transaction.getPayloadIpnBrut()
            ))
            .toList();
}


@Transactional(readOnly = true)
public List<SuperAdminTransactionResponse> getAllTransactions() {

    List<TransactionPaiement> transactions =
            transactionPaiementRepository.findAll(
                    org.springframework.data.domain.Sort.by(
                            org.springframework.data.domain.Sort.Direction.DESC,
                            "dateCreation"
                    )
            );

    return transactions.stream()
            .map(transaction -> new SuperAdminTransactionResponse(
                    transaction.getId(),
                    transaction.getStructure().getNom(),
                    transaction.getStructure().getId(),
                    transaction.getMontant(),
                    transaction.getDevise(),
                    transaction.getStatut().name(),
                    transaction.getMoyenPaiement(),
                    transaction.getTelephoneClient(),
                    transaction.getRefCommand(),
                    transaction.getTokenPaiement(),
                    transaction.getPlanVise().name(),
                    transaction.getDateCreation(),
                    transaction.getDateConfirmation(),
                    transaction.getPayloadIpnBrut()
            ))
            .toList();
}

@Transactional(readOnly = true)
public List<SuperAdminAbonnementResponse> getAllAbonnements() {

    List<Abonnement> abonnements =
            abonnementRepository.findAll();

    return abonnements.stream()
            .map(abonnement -> new SuperAdminAbonnementResponse(
                    abonnement.getId(),
                    abonnement.getStructure().getId(),
                    abonnement.getStructure().getNom(),
                    abonnement.getPlan().name(),
                    abonnement.getStatut().name(),
                    abonnement.getDateDebut(),
                    abonnement.getDateFin(),
                    abonnement.isRenouvellementAuto(),
                    abonnement.getDateCreation()
            ))
            .toList();
}


@Transactional
public void suspendreStructure(UUID structureId) {

    Structure structure = structureRepository.findById(structureId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Structure introuvable"
            ));

    if (structure.getStatut() == StatutStructure.SUSPENDUE) {
        throw new ResponseStatusException(
                HttpStatus.CONFLICT,
                "La structure est déjà suspendue."
        );
    }

    structure.setStatut(StatutStructure.SUSPENDUE);

    structureRepository.save(structure);
}


@Transactional
public void forcerFreemium(UUID structureId) {

    Structure structure = structureRepository.findById(structureId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Structure introuvable"
            ));

    Abonnement abonnement = abonnementRepository
            .findByStructureId(structureId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Abonnement introuvable"
            ));

enregistrerHistorique(
            structure,
            abonnement,
            SourceAbonnement.ADMINISTRATION
    );


    abonnement.setPlan(PlanAbonnement.FREEMIUM);
    abonnement.setStatut(StatutAbonnement.ACTIF);
    abonnement.setDateDebut(null);
    abonnement.setDateFin(null);
    abonnement.setRenouvellementAuto(false);

    abonnementRepository.save(abonnement);
}



@Transactional
public void activerPremium(UUID structureId) {

    Structure structure = structureRepository.findById(structureId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Structure introuvable"
            ));

    Abonnement abonnement = abonnementRepository
            .findByStructureId(structureId)
            .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "Abonnement introuvable"
            ));


             enregistrerHistorique(
            structure,
            abonnement,
            SourceAbonnement.ADMINISTRATION
    );

    LocalDateTime maintenant = LocalDateTime.now();

    

    abonnement.setPlan(PlanAbonnement.PREMIUM);
    abonnement.setStatut(StatutAbonnement.ACTIF);
    abonnement.setDateDebut(maintenant);
    abonnement.setDateFin(maintenant.plusYears(1));
    abonnement.setRenouvellementAuto(false);

    abonnementRepository.save(abonnement);
}


private void enregistrerHistorique(
        Structure structure,
        Abonnement abonnement,
        SourceAbonnement source
) {
    HistoriqueAbonnement historique = new HistoriqueAbonnement();

    historique.setStructure(structure);
    historique.setPlan(abonnement.getPlan());
    historique.setStatut(abonnement.getStatut());
    historique.setDateDebut(abonnement.getDateDebut());
    historique.setDateFin(abonnement.getDateFin());
    historique.setRenouvellementAuto(
            abonnement.isRenouvellementAuto()
    );
    historique.setSource(source);

    historiqueAbonnementRepository.save(historique);
}


@Transactional(readOnly = true)
public List<SuperAdminHistoriqueAbonnementResponse> getHistoriqueAbonnement(
        UUID structureId
) {

    if (!structureRepository.existsById(structureId)) {
        throw new ResponseStatusException(
                HttpStatus.NOT_FOUND,
                "Structure introuvable"
        );
    }

    return historiqueAbonnementRepository
            .findAllByStructureIdOrderByDateCreationDesc(structureId)
            .stream()
            .map(historique ->
                    new SuperAdminHistoriqueAbonnementResponse(
                            historique.getId(),
                            historique.getPlan().name(),
                            historique.getStatut().name(),
                            historique.getDateDebut(),
                            historique.getDateFin(),
                            historique.isRenouvellementAuto(),
                            historique.getSource().name(),
                            historique.getDateCreation()
                    )
            )
            .toList();
}
}