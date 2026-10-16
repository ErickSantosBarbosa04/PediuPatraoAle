package com.umc.pediupatrao.dto;

public class UsuarioResumo {
    private final String id;
    private final String username;
    private final String role;

    public UsuarioResumo(String id, String username, String role) {
        this.id = id;
        this.username = username;
        this.role = role;
    }

    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
}
