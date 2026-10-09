package sn.jappo.jappo_backend.parcours.dto;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class CreateParcoursRequest {
    private String nom;
    private String description;
    private UUID programmeId;
    private List<CreatePhaseRequest> phases;
}
