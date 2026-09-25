package sn.jappo.jappo_backend.user.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import sn.jappo.jappo_backend.user.entity.RoleGlobal;
import sn.jappo.jappo_backend.user.entity.User;

public record UserResponse(

        UUID id,
        String prenom,
        String nom,
        String email,
        boolean emailVerified,
        RoleGlobal roleGlobal,
        LocalDateTime dateCreation

) {

    public static UserResponse from(User user) {

        return new UserResponse(
                user.getId(),
                user.getPrenom(),
                user.getNom(),
                user.getEmail(),
                user.isEmailVerified(),
                user.getRoleGlobal(),
                user.getDateCreation()
        );
    }
}