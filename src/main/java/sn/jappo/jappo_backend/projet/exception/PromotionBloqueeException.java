package sn.jappo.jappo_backend.projet.exception;

import java.util.List;

import sn.jappo.jappo_backend.projet.dto.MissionNonValidee;

/**
 * Promotion refusée (409) tant que des missions de la cohorte actuelle ne sont pas validées
 * et que le client n'envoie pas forcer=true.
 */
public class PromotionBloqueeException extends RuntimeException {

    private final List<MissionNonValidee> missionsNonValidees;

    public PromotionBloqueeException(List<MissionNonValidee> missionsNonValidees) {
        super("Missions non validées");
        this.missionsNonValidees = missionsNonValidees;
    }

    public List<MissionNonValidee> getMissionsNonValidees() {
        return missionsNonValidees;
    }
}
