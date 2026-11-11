package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.DashboardResumo;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.time.DayOfWeek;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class DashboardService {

    private static final ZoneId ZONA_NEGOCIO = ZoneId.of("America/Sao_Paulo");
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final DateTimeFormatter ROTULO_DIA = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter ROTULO_MES = DateTimeFormatter.ofPattern("MMM/yy", Locale.forLanguageTag("pt-BR"));
    private static final int LIMITE_DIAS = 366;
    private static final int LIMITE_PRODUTOS = 5;
    private static final int LIMITE_PEDIDOS_RECENTES = 8;

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;

    public DashboardService(PedidoRepository pedidoRepository, ClienteRepository clienteRepository) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
    }

    public DashboardResumo gerarResumo(String inicioInformado, String fimInformado, String statusInformado) {
        LocalDate hoje = LocalDate.now(ZONA_NEGOCIO);
        LocalDate inicio = lerData(inicioInformado, "inicio", hoje.minusDays(6));
        LocalDate fim = lerData(fimInformado, "fim", hoje);
        if (inicio.isAfter(fim)) {
            throw erro("A data inicial deve ser anterior ou igual à data final.");
        }
        if (fim.equals(LocalDate.MAX)) {
            throw erro("A data final informada está fora do intervalo suportado.");
        }
        if (ChronoUnit.DAYS.between(inicio, fim) + 1 > LIMITE_DIAS) {
            throw erro("O período máximo de consulta é de 366 dias.");
        }

        StatusPedido statusFiltro = lerStatus(statusInformado);
        Instant inicioInclusivo = inicio.atStartOfDay(ZONA_NEGOCIO).toInstant();
        Instant fimExclusivo = fim.plusDays(1).atStartOfDay(ZONA_NEGOCIO).toInstant();
        List<Pedido> pedidos = pedidoRepository.findByDataHoraEntradaGreaterThanEqualAndDataHoraEntradaLessThan(
                        inicioInclusivo, fimExclusivo).stream()
                .filter(pedido -> statusFiltro == null || lerStatusDoPedido(pedido) == statusFiltro)
                .toList();

        String granularidade = granularidade(inicio, fim);
        Map<LocalDate, Acumulador> serie = criarSerieVazia(inicio, fim, granularidade);
        Map<StatusPedido, Long> contagemStatus = new EnumMap<>(StatusPedido.class);
        for (StatusPedido status : StatusPedido.values()) contagemStatus.put(status, 0L);
        Map<String, AcumuladorProduto> produtos = new LinkedHashMap<>();

        BigDecimal faturamento = ZERO;
        long pedidosValidosParaTicket = 0;
        long emAndamento = 0;
        long cancelados = 0;

        for (Pedido pedido : pedidos) {
            StatusPedido status = lerStatusDoPedido(pedido);
            if (status != null) contagemStatus.compute(status, (chave, valor) -> valor + 1);
            if (status == StatusPedido.CANCELADO) cancelados++;
            if (status == StatusPedido.RECEBIDO || status == StatusPedido.EM_PREPARACAO || status == StatusPedido.PRONTO) {
                emAndamento++;
            }

            LocalDate data = dataDeEntrada(pedido);
            Acumulador ponto = serie.get(chavePeriodo(data, granularidade));
            if (ponto != null) ponto.pedidos++;

            if (status != StatusPedido.CANCELADO) {
                BigDecimal valorPedido = valorPedido(pedido);
                faturamento = faturamento.add(valorPedido);
                pedidosValidosParaTicket++;
                if (ponto != null) ponto.faturamento = ponto.faturamento.add(valorPedido);
                agregarProdutos(pedido, produtos);
            }
        }

        BigDecimal ticketMedio = pedidosValidosParaTicket == 0
                ? ZERO
                : faturamento.divide(BigDecimal.valueOf(pedidosValidosParaTicket), 2, RoundingMode.HALF_UP);

        List<DashboardResumo.VendaSerie> vendas = serie.entrySet().stream()
                .map(entry -> new DashboardResumo.VendaSerie(
                        entry.getKey().toString(), rotulo(entry.getKey(), granularidade),
                        dinheiro(entry.getValue().faturamento), entry.getValue().pedidos))
                .toList();

        List<DashboardResumo.StatusResumo> statusResumo = List.of(StatusPedido.values()).stream()
                .map(status -> new DashboardResumo.StatusResumo(status.name(), nomeStatus(status), contagemStatus.get(status)))
                .toList();

        List<DashboardResumo.ProdutoResumo> ranking = produtos.values().stream()
                .sorted(Comparator.comparingLong((AcumuladorProduto item) -> item.quantidade).reversed()
                        .thenComparing(item -> item.nome, String.CASE_INSENSITIVE_ORDER))
                .limit(LIMITE_PRODUTOS)
                .map(item -> new DashboardResumo.ProdutoResumo(item.produtoId, item.nome, item.quantidade, dinheiro(item.faturamento)))
                .toList();

        Set<String> clientesSolicitados = pedidos.stream()
                .sorted(Comparator.comparing(Pedido::getDataHoraEntrada, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(LIMITE_PEDIDOS_RECENTES)
                .map(Pedido::getClienteId)
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<String, String> clientes = clienteRepository.findAllById(clientesSolicitados).stream()
                .filter(cliente -> cliente.getId() != null)
                .collect(Collectors.toMap(Cliente::getId, cliente -> texto(cliente.getNome(), "Cliente"), (a, b) -> a));

        List<DashboardResumo.PedidoResumo> recentes = pedidos.stream()
                .sorted(Comparator.comparing(Pedido::getDataHoraEntrada, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(LIMITE_PEDIDOS_RECENTES)
                .map(pedido -> {
                    StatusPedido status = lerStatusDoPedido(pedido);
                    return new DashboardResumo.PedidoResumo(
                            pedido.getId(), clientes.getOrDefault(pedido.getClienteId(), "Cliente"),
                            status == null ? "DESCONHECIDO" : status.name(),
                            status == null ? "Não informado" : nomeStatus(status),
                            pedido.getDataHoraEntrada(), dinheiro(valorPedido(pedido)));
                })
                .toList();

        return new DashboardResumo(
                new DashboardResumo.Periodo(inicio, fim),
                statusFiltro == null ? null : statusFiltro.name(),
                granularidade,
                new DashboardResumo.Indicadores(dinheiro(faturamento), pedidos.size(), ticketMedio, emAndamento, cancelados),
                vendas, statusResumo, ranking, recentes, Instant.now());
    }

    private LocalDate lerData(String valor, String campo, LocalDate padrao) {
        if (valor == null || valor.isBlank()) return padrao;
        try {
            return LocalDate.parse(valor.trim());
        } catch (RuntimeException excecao) {
            throw erro("O parâmetro '" + campo + "' deve usar o formato AAAA-MM-DD.");
        }
    }

    private StatusPedido lerStatus(String valor) {
        if (valor == null || valor.isBlank()) return null;
        try {
            return StatusPedido.de(valor);
        } catch (IllegalArgumentException excecao) {
            throw erro("Status inválido. Consulte os valores disponíveis em pedidosPorStatus.");
        }
    }

    private StatusPedido lerStatusDoPedido(Pedido pedido) {
        try {
            return StatusPedido.de(pedido.getStatus());
        } catch (IllegalArgumentException excecao) {
            return null;
        }
    }

    private LocalDate dataDeEntrada(Pedido pedido) {
        return pedido.getDataHoraEntrada() == null
                ? null
                : pedido.getDataHoraEntrada().atZone(ZONA_NEGOCIO).toLocalDate();
    }

    private String granularidade(LocalDate inicio, LocalDate fim) {
        long dias = ChronoUnit.DAYS.between(inicio, fim) + 1;
        return dias <= 31 ? "DIA" : dias <= 180 ? "SEMANA" : "MES";
    }

    private Map<LocalDate, Acumulador> criarSerieVazia(LocalDate inicio, LocalDate fim, String granularidade) {
        Map<LocalDate, Acumulador> serie = new TreeMap<>();
        LocalDate cursor = chavePeriodo(inicio, granularidade);
        LocalDate ultima = chavePeriodo(fim, granularidade);
        while (!cursor.isAfter(ultima)) {
            serie.put(cursor, new Acumulador());
            cursor = switch (granularidade) {
                case "DIA" -> cursor.plusDays(1);
                case "SEMANA" -> cursor.plusWeeks(1);
                default -> cursor.plusMonths(1);
            };
        }
        return serie;
    }

    private LocalDate chavePeriodo(LocalDate data, String granularidade) {
        return switch (granularidade) {
            case "DIA" -> data;
            case "SEMANA" -> data.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            default -> data.withDayOfMonth(1);
        };
    }

    private String rotulo(LocalDate data, String granularidade) {
        return granularidade.equals("MES") ? ROTULO_MES.format(data) : ROTULO_DIA.format(data);
    }

    private BigDecimal valorPedido(Pedido pedido) {
        BigDecimal valor = pedido.getValorTotal() != null ? pedido.getValorTotal() : pedido.getSubtotal();
        return valor == null ? ZERO : valor;
    }

    private void agregarProdutos(Pedido pedido, Map<String, AcumuladorProduto> produtos) {
        List<ItemPedido> itens = pedido.getItens() == null ? List.of() : pedido.getItens();
        if (!itens.isEmpty()) {
            BigDecimal totalItens = itens.stream()
                    .filter(item -> item != null && item.getQuantidade() > 0)
                    .map(this::valorItem)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal valorLiquidoPedido = valorPedido(pedido);
            for (ItemPedido item : itens) {
                if (item == null || item.getQuantidade() <= 0) continue;
                String nome = texto(item.getNomeProduto(), "Produto sem nome");
                String chave = texto(item.getProdutoId(), nome.toLowerCase(Locale.ROOT));
                BigDecimal valor = valorItem(item);
                if (totalItens.compareTo(BigDecimal.ZERO) > 0) {
                    valor = valor.multiply(valorLiquidoPedido).divide(totalItens, 2, RoundingMode.HALF_UP);
                }
                acumularProduto(produtos, chave, item.getProdutoId(), nome, item.getQuantidade(), valor);
            }
            return;
        }

        // Older records stored only a flavor/name and a total quantity instead of item lines.
        String nome = texto(pedido.getSabor(), texto(pedido.getTipoPizza(), null));
        int quantidade = pedido.getQuantidade();
        if (nome != null && quantidade > 0) {
            acumularProduto(produtos, nome.toLowerCase(Locale.ROOT), null, nome, quantidade, valorPedido(pedido));
        }
    }

    private BigDecimal valorItem(ItemPedido item) {
        if (item.getValorTotal() != null) return item.getValorTotal();
        return item.getPrecoUnitario() == null
                ? BigDecimal.ZERO
                : item.getPrecoUnitario().multiply(BigDecimal.valueOf(item.getQuantidade()));
    }

    private void acumularProduto(Map<String, AcumuladorProduto> produtos, String chave, String produtoId,
                                 String nome, int quantidade, BigDecimal faturamento) {
        AcumuladorProduto item = produtos.computeIfAbsent(chave, ignorado -> new AcumuladorProduto(produtoId, nome));
        item.quantidade += quantidade;
        if (faturamento != null) item.faturamento = item.faturamento.add(faturamento);
    }

    private String nomeStatus(StatusPedido status) {
        return switch (status) {
            case RECEBIDO -> "Recebido";
            case EM_PREPARACAO -> "Em preparo";
            case PRONTO -> "Pronto";
            case SAIU_PARA_ENTREGA -> "Saiu para entrega";
            case RETIRADO -> "Retirado";
            case FINALIZADO -> "Finalizado";
            case CANCELADO -> "Cancelado";
        };
    }

    private String texto(String valor, String padrao) {
        return valor == null || valor.isBlank() ? padrao : valor.trim();
    }

    private BigDecimal dinheiro(BigDecimal valor) {
        return (valor == null ? ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }

    private ResponseStatusException erro(String mensagem) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensagem);
    }

    private static class Acumulador {
        private BigDecimal faturamento = ZERO;
        private long pedidos;
    }

    private static class AcumuladorProduto {
        private final String produtoId;
        private final String nome;
        private long quantidade;
        private BigDecimal faturamento = ZERO;

        private AcumuladorProduto(String produtoId, String nome) {
            this.produtoId = produtoId;
            this.nome = nome;
        }
    }
}
