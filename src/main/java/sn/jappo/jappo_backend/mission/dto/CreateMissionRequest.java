package sn.jappo.jappo_backend.mission.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;
import java.util.UUID;
import sn.jappo.jappo_backend.mission.entity.PrioriteMission;

public record CreateMissionRequest(
        @NotBlank(message = "Le titre de la mission est obligatoire")
        String titre,
        String description,
        @FutureOrPresent(message = "La date d'échéance ne peut pas être dans le passé")
        LocalDate dateEcheance,
        PrioriteMission priorite,
        UUID cohorteId,          // Si la mission est diffusée à toute une cohorte
        UUID projetId,           // Optionnel : si ciblée sur un seul projet/startup
        UUID assigneAId,         // Optionnel : si assignée à un coach ou membre spécifique
        UUID modeleId,           // Optionnel : si issue d'un modèle du catalogue
        /**
         * missionEtapeId — ID d'une {@link sn.jappo.jappo_backend.mission.entity.MissionEtape}
         * déjà créée dans le parcours. Quand ce champ est fourni, on instancie directement
         * des {@link sn.jappo.jappo_backend.mission.entity.MissionProjet} liées à cette
         * MissionEtape (chemin A), sans créer de MissionCohorte.
         * <p>
         * Ne pas confondre avec {@code etapeParcoursId} ci-dessous.
         */
        UUID missionEtapeId,
        /**
         * etapeParcoursId — ID d'une {@link sn.jappo.jappo_backend.parcours.entity.Etape}
         * du parcours pédagogique. Quand ce champ est fourni (sans missionEtapeId),
         * la MissionCohorte créée est rattachée à cette Etape pour permettre le suivi
         * dans la feuille de route du projet.
         * <p>
         * Remplace l'ancien champ {@code etapeId} ambigu.
         */
        UUID etapeParcoursId,
        Boolean enregistrerCommeModele // Optionnel : sauvegarder comme modèle réutilisable
) {
    public CreateMissionRequest {
        if (priorite == null) {
            priorite = PrioriteMission.MOYENNE;
        }
    }
}