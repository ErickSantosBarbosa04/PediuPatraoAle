package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.CancelamentoPedidoRequest;
import com.umc.pediupatrao.dto.DescontoPedidoRequest;
import com.umc.pediupatrao.dto.PedidoCriacaoRequest;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.service.PedidoService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pedidos")
public class PedidoController {

    private final PedidoService pedidoService;

    public PedidoController(PedidoService pedidoService) {
        this.pedidoService = pedidoService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public ResponseEntity<Pedido> criarPedido(
            @RequestBody PedidoCriacaoRequest requisicao,
            Authentication autenticacao) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(pedidoService.criarPedido(requisicao, autenticacao));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoService.listarPedidos();
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido atualizarStatus(
            @PathVariable String id,
            @RequestParam String status,
            Authentication autenticacao) {
        return pedidoService.atualizarStatus(id, status, autenticacao);
    }

    @PatchMapping("/{id}/desconto")
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(
            @PathVariable String id,
            @RequestBody DescontoPedidoRequest requisicao,
            Authentication autenticacao) {
        return pedidoService.aplicarDesconto(id, requisicao == null ? null : requisicao.getPercentual(), autenticacao);
    }

    @PostMapping("/{id}/cancelamento")
    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(
            @PathVariable String id,
            @RequestBody CancelamentoPedidoRequest requisicao,
            Authentication autenticacao) {
        return pedidoService.cancelar(id, requisicao == null ? null : requisicao.getJustificativa(), autenticacao);
    }
}
