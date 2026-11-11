package com.umc.pediupatrao.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.umc.pediupatrao.dto.DashboardResumo;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private PedidoRepository pedidoRepository;
    @Mock
    private ClienteRepository clienteRepository;

    private DashboardService dashboardService;

    @BeforeEach
    void preparar() {
        dashboardService = new DashboardService(pedidoRepository, clienteRepository);
        lenient().when(clienteRepository.findAllById(anyIterable())).thenReturn(List.of());
    }

    @Test
    void resumeVendasIgnoraCanceladosEAplicaDescontoNoRanking() {
        Pedido venda = pedido("pedido-1", "RECEBIDO", "90.00", "100.00", 2,
                "2026-10-30T12:00:00Z", "Pizza de queijo", "100.00");
        Pedido cancelado = pedido("pedido-2", "CANCELADO", "40.00", "40.00", 1,
                "2026-10-30T14:00:00Z", "Pizza calabresa", "40.00");
        when(pedidoRepository.findByDataHoraEntradaGreaterThanEqualAndDataHoraEntradaLessThan(any(), any()))
                .thenReturn(List.of(venda, cancelado));

        DashboardResumo resumo = dashboardService.gerarResumo("2026-10-30", "2026-10-30", null);

        assertEquals(new BigDecimal("90.00"), resumo.indicadores().faturamento());
        assertEquals(2, resumo.indicadores().totalPedidos());
        assertEquals(new BigDecimal("90.00"), resumo.indicadores().ticketMedio());
        assertEquals(1, resumo.indicadores().pedidosEmAndamento());
        assertEquals(1, resumo.indicadores().pedidosCancelados());
        assertEquals(2, resumo.vendas().getFirst().pedidos());
        assertEquals(new BigDecimal("90.00"), resumo.vendas().getFirst().faturamento());
        assertEquals("Pizza de queijo", resumo.produtosMaisVendidos().getFirst().nome());
        assertEquals(2, resumo.produtosMaisVendidos().getFirst().quantidade());
        assertEquals(new BigDecimal("90.00"), resumo.produtosMaisVendidos().getFirst().faturamento());
    }

    @Test
    void filtroDeStatusRetornaSomentePedidosCorrespondentes() {
        Pedido venda = pedido("pedido-1", "RECEBIDO", "30.00", "30.00", 1,
                "2026-10-30T12:00:00Z", "Esfiha", "30.00");
        when(pedidoRepository.findByDataHoraEntradaGreaterThanEqualAndDataHoraEntradaLessThan(any(), any()))
                .thenReturn(List.of(venda));

        DashboardResumo resumo = dashboardService.gerarResumo("2026-10-30", "2026-10-30", "recebido");

        assertEquals("RECEBIDO", resumo.statusSelecionado());
        assertEquals(1, resumo.indicadores().totalPedidos());
    }

    @Test
    void rejeitaPeriodoInvertido() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> dashboardService.gerarResumo("2026-10-31", "2026-10-30", null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void rejeitaDataEmFormatoInvalido() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> dashboardService.gerarResumo("30/10/2026", "2026-10-30", null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void rejeitaStatusDesconhecido() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> dashboardService.gerarResumo("2026-10-30", "2026-10-30", "EM_ENTREGA"));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    @Test
    void rejeitaConsultaComMaisDe366Dias() {
        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> dashboardService.gerarResumo("2025-01-01", "2026-10-30", null));
        assertEquals(HttpStatus.BAD_REQUEST, erro.getStatusCode());
    }

    private Pedido pedido(String id, String status, String total, String subtotal, int quantidade,
                          String dataHora, String nomeProduto, String totalItem) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setStatus(status);
        pedido.setValorTotal(new BigDecimal(total));
        pedido.setSubtotal(new BigDecimal(subtotal));
        pedido.setQuantidade(quantidade);
        pedido.setDataHoraEntrada(Instant.parse(dataHora));
        ItemPedido item = new ItemPedido();
        item.setProdutoId(id + "-produto");
        item.setNomeProduto(nomeProduto);
        item.setQuantidade(quantidade);
        item.setPrecoUnitario(new BigDecimal(totalItem).divide(BigDecimal.valueOf(quantidade)));
        item.setValorTotal(new BigDecimal(totalItem));
        pedido.setItens(List.of(item));
        return pedido;
    }
}
