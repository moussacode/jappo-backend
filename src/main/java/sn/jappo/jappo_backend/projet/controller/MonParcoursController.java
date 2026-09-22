package sn.jappo.jappo_backend.projet.controller;

import java.util.List;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import sn.jappo.jappo_backend.cohorte.service.ParticipationService;
import sn.jappo.jappo_backend.mission.dto.MissionResponse;
import sn.jappo.jappo_backend.mission.service.MissionService;
import sn.jappo.jappo_backend.projet.dto.ParticipationCohorteResponse;
import sn.jappo.jappo_backend.projet.dto.ProjetResponse;
import sn.jappo.jappo_backend.projet.service.ProjetService;
import sn.jappo.jappo_backend.user.entity.User;

/**
 * Endpoints dédiés à la vue "Mon Parcours" de l'entrepreneur.
 * L'entrepreneur accède uniquement à ses propres données.
 */
@RestController
@RequestMapping("/api/mon-parcours")
@RequiredArgsConstructor
public class MonParcoursController {

    private final ProjetService projetService;
    private final ParticipationService participationService;
    private final MissionService missionService;

    /**
     * Résumé du projet principal de l'entrepreneur connecté.
     */
    @GetMapping("/projet")
    public ResponseEntity<ProjetResponse> getMonProjet(@AuthenticationPrincipal User currentUser) {
        return ResponseEntity.ok(projetService.getProjetPrincipalByEntrepreneur(currentUser.getId()));
    }

    /**
     * Historique complet de participation aux cohortes (frise chronologique).
     * Du plus récent au plus ancien.
     */
    @GetMapping("/historique")
    public ResponseEntity<List<ParticipationCohorteResponse>> getHistorique(
            @AuthenticationPrincipal User currentUser) {
        ProjetResponse projet = projetService.getProjetPrincipalByEntrepreneur(currentUser.getId());
        return ResponseEntity.ok(participationService.getHistorique(projet.id()));
    }

    /**
     * Missions actives dans la cohorte courante.
     */
    @GetMapping("/missions")
    public ResponseEntity<List<MissionResponse>> getMesMissions(
            @AuthenticationPrincipal User currentUser) {
        ProjetResponse projet = projetService.getProjetPrincipalByEntrepreneur(currentUser.getId());
        return ResponseEntity.ok(missionService.getMissionsByProjet(projet.id()));
    }
}
