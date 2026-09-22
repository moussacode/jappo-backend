package sn.jappo.jappo_backend.parcours.dto;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class CreatePhaseRequest {

    private UUID id;

    private String nom;
    private String description;
    private Integer ordre;

}