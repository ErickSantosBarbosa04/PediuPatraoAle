package com.umc.pediupatrao.entity;

import java.util.Arrays;

public enum PerfilAcesso {
    ADMIN,
    GERENTE,
    ATENDENTE;

    public static PerfilAcesso validar(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Selecione um perfil válido.");
        }
        try {
            return valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Perfil inválido. Use ADMIN, GERENTE ou ATENDENTE.");
        }
    }

    public static String migrarPerfilLegado(String perfil, String username) {
        if (perfil == null || perfil.isBlank()) {
            return "admin".equalsIgnoreCase(username) ? ADMIN.name() : ATENDENTE.name();
        }
        if ("USER".equalsIgnoreCase(perfil)) {
            return ATENDENTE.name();
        }
        return Arrays.stream(values())
                .filter(role -> role.name().equalsIgnoreCase(perfil))
                .findFirst()
                .map(Enum::name)
                .orElse(ATENDENTE.name());
    }
}
