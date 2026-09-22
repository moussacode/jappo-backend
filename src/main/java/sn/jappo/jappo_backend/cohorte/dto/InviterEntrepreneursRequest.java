package sn.jappo.jappo_backend.cohorte.dto;

import java.util.List;

public record InviterEntrepreneursRequest(
    List<String> emails
) {}
