package sn.jappo.jappo_backend.abonnement.service;

import org.springframework.http.HttpStatus;

/**
 * Refus lié au plan d'abonnement. Le {@code code} permet au frontend de reconnaître
 * le cas sans analyser le message.
 */
public class QuotaDepasseException extends RuntimeException {

    public static final String IA_ROLE_NON_AUTORISE = "IA_ROLE_NON_AUTORISE";
    public static final String IA_QUOTA_EPUISE = "IA_QUOTA_EPUISE";
    public static final String PLAN_LIMITE_ATTEINTE = "PLAN_LIMITE_ATTEINTE";

    private final HttpStatus status;
    private final String code;
    private final Long consomme;
    private final Long limite;

    public QuotaDepasseException(HttpStatus status, String code, String message, Long consomme, Long limite) {
        super(message);
        this.status = status;
        this.code = code;
        this.consomme = consomme;
        this.limite = limite;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public Long getConsomme() {
        return consomme;
    }

    public Long getLimite() {
        return limite;
    }
}
