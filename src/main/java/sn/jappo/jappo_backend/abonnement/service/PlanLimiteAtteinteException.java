package sn.jappo.jappo_backend.abonnement.service;

public class PlanLimiteAtteinteException extends RuntimeException {
    public PlanLimiteAtteinteException(String message) {
        super(message);
    }
}