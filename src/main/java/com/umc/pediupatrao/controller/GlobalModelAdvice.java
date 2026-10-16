package com.umc.pediupatrao.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class GlobalModelAdvice {

    @ModelAttribute
    public void informarPerfil(Model model, Authentication authentication) {
        String perfil = "";
        if (authentication != null && authentication.isAuthenticated()) {
            perfil = authentication.getAuthorities().stream()
                    .map(authority -> authority.getAuthority())
                    .filter(authority -> authority.startsWith("ROLE_"))
                    .map(authority -> authority.substring("ROLE_".length()))
                    .findFirst()
                    .orElse("");
        }
        model.addAttribute("perfilAtual", perfil);
        model.addAttribute("usuarioAtual", authentication == null ? "" : authentication.getName());
        model.addAttribute("isAdmin", "ADMIN".equals(perfil));
        model.addAttribute("isGerente", "GERENTE".equals(perfil));
        model.addAttribute("isAtendente", "ATENDENTE".equals(perfil));
    }
}
