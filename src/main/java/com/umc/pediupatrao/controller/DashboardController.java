package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.DashboardResumo;
import com.umc.pediupatrao.service.DashboardService;
import java.time.Instant;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/resumo")
    public DashboardResumo resumo(
            @RequestParam(required = false) String inicio,
            @RequestParam(required = false) String fim,
            @RequestParam(required = false) String status) {
        return dashboardService.gerarResumo(inicio, fim, status);
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroApi> tratarErroDashboard(ResponseStatusException excecao, HttpServletRequest requisicao) {
        HttpStatus status = HttpStatus.valueOf(excecao.getStatusCode().value());
        return ResponseEntity.status(status).body(new ErroApi(
                Instant.now(), status.value(), status.getReasonPhrase(), excecao.getReason(), requisicao.getRequestURI()));
    }

    public record ErroApi(Instant instante, int status, String erro, String mensagem, String caminho) {}
}
