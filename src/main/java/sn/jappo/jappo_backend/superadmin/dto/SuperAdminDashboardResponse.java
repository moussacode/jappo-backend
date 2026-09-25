package sn.jappo.jappo_backend.superadmin.dto;
import java.util.List;
import java.util.Map;
public record SuperAdminDashboardResponse(
        long totalStructures,
        long structuresPremium,
        long structuresFreemium,

        long revenuTotal,
        long revenuMois,

        long transactionsEchecRecentes,
        long abonnementsExpiration7Jours,

        long nouvellesStructuresCetteSemaine,
        long nouvellesStructuresCeMois,

        List<RevenuMensuelResponse> revenusMensuels,
        List<NouvellesStructuresMensuellesResponse> nouvellesStructuresMensuelles,
        Map<String, Long> transactionsParStatut
) {}