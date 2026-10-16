package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class AuditoriaController {

    private final AuditoriaRepository auditoriaRepository;

    public AuditoriaController(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @GetMapping("/auditoria")
    public String consultar(Model model) {
        model.addAttribute("eventos", auditoriaRepository.findTop200ByOrderByDataHoraDesc());
        model.addAttribute("content", "auditoria :: content");
        return "layout";
    }

    @GetMapping("/api/auditoria")
    @ResponseBody
    public List<EventoAuditoria> consultarApi() {
        return auditoriaRepository.findTop200ByOrderByDataHoraDesc();
    }
}
