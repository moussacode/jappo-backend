package sn.jappo.jappo_backend.parcours.dto;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ParcoursResponse {
    private UUID id;
    private String nom;
    private String description;
    private boolean archive;
    private LocalDateTime dateCreation;
    private LocalDateTime dateModification;
    private List<PhaseResponse> phases;
}
