package sn.jappo.jappo_backend.structure.dto;

import sn.jappo.jappo_backend.structure.entity.RoleMembreStructure;

public record StructureMembershipResponse(
        StructureResponse structure,
        RoleMembreStructure role
) {
}