package com.umc.pediupatrao.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

/** Read model returned by the administrative dashboard API. */
public record DashboardResumo(
        Periodo periodo,
        String statusSelecionado,
        String granularidade,
        Indicadores indicadores,
        List<VendaSerie> vendas,
        List<StatusResumo> pedidosPorStatus,
        List<ProdutoResumo> produtosMaisVendidos,
        List<PedidoResumo> pedidosRecentes,
        Instant atualizadoEm) {

    public record Periodo(LocalDate inicio, LocalDate fim) {}

    public record Indicadores(
            BigDecimal faturamento,
            long totalPedidos,
            BigDecimal ticketMedio,
            long pedidosEmAndamento,
            long pedidosCancelados) {}

    public record VendaSerie(String periodo, String rotulo, BigDecimal faturamento, long pedidos) {}

    public record StatusResumo(String codigo, String nome, long quantidade) {}

    public record ProdutoResumo(String produtoId, String nome, long quantidade, BigDecimal faturamento) {}

    public record PedidoResumo(
            String id,
            String cliente,
            String status,
            String statusNome,
            Instant dataHoraEntrada,
            BigDecimal valorTotal) {}
}
