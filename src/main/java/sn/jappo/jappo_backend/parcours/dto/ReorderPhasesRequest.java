package sn.jappo.jappo_backend.parcours.dto;

import java.util.List;
import java.util.UUID;

import lombok.Data;

@Data
public class ReorderPhasesRequest {

    private List<UUID> phaseIds;
}