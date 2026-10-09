package sn.jappo.jappo_backend.parcours.dto;

public record UpdateEtapeRequest(
    String nom,
    String description,
    Integer ordre,
    Boolean obligatoire,
    Boolean archive
) {}
