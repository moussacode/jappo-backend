package sn.jappo.jappo_backend.config.tenant;

import java.util.UUID;

public class TenantContext {

    // Variable ThreadLocal : chaque requête/thread Java a sa propre case mémoire
    private static final ThreadLocal<UUID> CURRENT_TENANT = new ThreadLocal<>();

    // 1. Stocker l'ID de la structure active pour la requête en cours
    public static void setCurrentTenant(UUID tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    // 2. Récupérer l'ID de la structure active à n'importe quel endroit du code
    public static UUID getCurrentTenant() {
        return CURRENT_TENANT.get();
    }

    // 3. Effacer impérativement l'ID à la fin de la requête (Sécurité)
    public static void clear() {
        CURRENT_TENANT.remove();
    }
}