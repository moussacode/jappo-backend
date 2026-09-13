package sn.jappo.jappo_backend.projet.dto;

public record UpdateProjetRequest(
        String nom,
        String description,
        String secteur
) {}