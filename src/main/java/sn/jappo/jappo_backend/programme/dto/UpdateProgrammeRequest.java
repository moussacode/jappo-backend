package sn.jappo.jappo_backend.programme.dto;

public record UpdateProgrammeRequest(
    String nom,
    String description,
    Boolean archive
) {}
