package sn.jappo.jappo_backend.parcours.dto;

import java.util.List;

import lombok.Data;

@Data
public class CreateParcoursRequest {
    private String nom;
    private String description;
    private List<CreatePhaseRequest> phases;
}
