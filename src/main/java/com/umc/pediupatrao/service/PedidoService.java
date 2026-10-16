package com.umc.pediupatrao.service;

import com.umc.pediupatrao.dto.ItemPedidoRequest;
import com.umc.pediupatrao.dto.PedidoCriacaoRequest;
import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.entity.ItemPedido;
import com.umc.pediupatrao.entity.Pedido;
import com.umc.pediupatrao.entity.StatusPedido;
import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ClienteRepository;
import com.umc.pediupatrao.repository.PedidoRepository;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PedidoService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private static final BigDecimal LIMITE_DESCONTO = new BigDecimal("20.00");

    private final PedidoRepository pedidoRepository;
    private final ClienteRepository clienteRepository;
    private final ProdutoRepository produtoRepository;
    private final AuditoriaService auditoriaService;

    public PedidoService(
            PedidoRepository pedidoRepository,
            ClienteRepository clienteRepository,
            ProdutoRepository produtoRepository,
            AuditoriaService auditoriaService) {
        this.pedidoRepository = pedidoRepository;
        this.clienteRepository = clienteRepository;
        this.produtoRepository = produtoRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('ATENDENTE', 'GERENTE')")
    public Pedido criarPedido(PedidoCriacaoRequest requisicao, Authentication autenticacao) {
        if (requisicao == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados do pedido.");
        }
        if (requisicao.getClienteId() == null || requisicao.getClienteId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um cliente.");
        }
        clienteRepository.findById(requisicao.getClienteId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
        List<ItemPedidoRequest> solicitados = new ArrayList<>(requisicao.getItens());
        if (solicitados.isEmpty() && requisicao.getQuantidade() > 0) {
            String nomeLegado = requisicao.getSabor() == null || requisicao.getSabor().isBlank()
                    ? requisicao.getTipoPizza() : requisicao.getSabor();
            Produto produtoLegado = produtoRepository.findAll().stream()
                    .filter(produto -> Boolean.TRUE.equals(produto.getAtivo()))
                    .filter(produto -> produto.getNome() != null && produto.getNome().equalsIgnoreCase(nomeLegado == null ? "" : nomeLegado.trim()))
                    .findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                            "O pedido legado precisa corresponder ao nome de um produto ativo."));
            ItemPedidoRequest itemLegado = new ItemPedidoRequest();
            itemLegado.setProdutoId(produtoLegado.getId());
            itemLegado.setQuantidade(requisicao.getQuantidade());
            solicitados.add(itemLegado);
        }
        if (solicitados.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Adicione pelo menos um item ao pedido.");
        }
        if (solicitados.size() > 50) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um pedido pode conter até 50 itens diferentes.");
        }

        List<ItemPedido> itens = new ArrayList<>();
        BigDecimal subtotal = ZERO;
        int quantidadeTotal = 0;
        for (ItemPedidoRequest solicitado : solicitados) {
            if (solicitado == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Remova itens incompletos do pedido.");
            }
            if (solicitado.getProdutoId() == null || solicitado.getProdutoId().isBlank()
                    || solicitado.getQuantidade() < 1 || solicitado.getQuantidade() > 99) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cada item precisa de um produto e uma quantidade entre 1 e 99.");
            }
            Produto produto = produtoRepository.findById(solicitado.getProdutoId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado."));
            if (!Boolean.TRUE.equals(produto.getAtivo()) || produto.getPreco() == null
                    || !Double.isFinite(produto.getPreco()) || produto.getPreco() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O produto selecionado está inativo ou sem preço válido.");
            }

            BigDecimal preco = BigDecimal.valueOf(produto.getPreco()).setScale(2, RoundingMode.HALF_UP);
            BigDecimal totalItem = preco.multiply(BigDecimal.valueOf(solicitado.getQuantidade())).setScale(2, RoundingMode.HALF_UP);
            ItemPedido item = new ItemPedido();
            item.setProdutoId(produto.getId());
            item.setNomeProduto(produto.getNome());
            item.setQuantidade(solicitado.getQuantidade());
            item.setPrecoUnitario(preco);
            item.setValorTotal(totalItem);
            itens.add(item);
            subtotal = subtotal.add(totalItem);
            quantidadeTotal += solicitado.getQuantidade();
        }

        Pedido pedido = new Pedido();
        pedido.setClienteId(requisicao.getClienteId());
        pedido.setItens(itens);
        pedido.setSubtotal(subtotal.setScale(2, RoundingMode.HALF_UP));
        pedido.setPercentualDesconto(ZERO);
        pedido.setValorDesconto(ZERO);
        pedido.setValorTotal(pedido.getSubtotal());
        pedido.setStatus(StatusPedido.RECEBIDO.name());
        pedido.setResponsavelRegistro(autenticacao.getName());
        pedido.setDataHoraEntrada(Instant.now());
        pedido.setQuantidade(quantidadeTotal);
        pedido.setTipoPizza(itens.get(0).getNomeProduto());
        pedido.setSabor(itens.get(0).getNomeProduto());

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(autenticacao, "INCLUSAO", "PEDIDO", salvo.getId(),
                AuditoriaService.diferencas(null, snapshot(salvo)), null);
        return salvo;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Pedido> listarPedidos() {
        return pedidoRepository.findAll();
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido atualizarStatus(String id, String proximoStatus, Authentication autenticacao) {
        Pedido pedido = buscarPedido(id);
        StatusPedido atual = interpretarStatus(pedido.getStatus());
        StatusPedido proximo = interpretarStatus(proximoStatus);
        if (atual == StatusPedido.CANCELADO || atual == StatusPedido.FINALIZADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Pedidos cancelados ou finalizados não podem mudar de status.");
        }
        if (proximo == StatusPedido.CANCELADO || !transicaoPermitida(atual, proximo)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Essa transição de status não é permitida.");
        }

        Map<String, Object> anterior = estadoStatus(pedido);
        Instant agora = Instant.now();
        pedido.setStatus(proximo.name());
        if (proximo == StatusPedido.SAIU_PARA_ENTREGA || proximo == StatusPedido.RETIRADO) {
            pedido.setDataHoraSaida(agora);
            pedido.setModalidadeSaida(proximo.name());
            pedido.setResponsavelSaida(autenticacao.getName());
        } else if (proximo == StatusPedido.FINALIZADO) {
            pedido.setDataHoraFinalizacao(agora);
        }

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(autenticacao, "MUDANCA_DE_STATUS", "PEDIDO", id,
                AuditoriaService.diferencas(anterior, estadoStatus(salvo)), null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido aplicarDesconto(String id, BigDecimal percentual, Authentication autenticacao) {
        if (percentual == null || percentual.compareTo(BigDecimal.ZERO) < 0 || percentual.compareTo(LIMITE_DESCONTO) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O desconto deve ser percentual entre 0% e 20%.");
        }
        Pedido pedido = buscarPedido(id);
        StatusPedido status = interpretarStatus(pedido.getStatus());
        if (status == StatusPedido.SAIU_PARA_ENTREGA || status == StatusPedido.RETIRADO
                || status == StatusPedido.FINALIZADO || status == StatusPedido.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O desconto só pode ser aplicado antes da saída do pedido.");
        }

        BigDecimal subtotal = valor(pedido.getSubtotal());
        BigDecimal percentualNormalizado = percentual.setScale(2, RoundingMode.HALF_UP);
        BigDecimal valorDesconto = subtotal.multiply(percentualNormalizado)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        Map<String, Object> anterior = valoresFinanceiros(pedido);
        pedido.setPercentualDesconto(percentualNormalizado);
        pedido.setValorDesconto(valorDesconto);
        pedido.setValorTotal(subtotal.subtract(valorDesconto).setScale(2, RoundingMode.HALF_UP));

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(autenticacao, "APLICACAO_DE_DESCONTO", "PEDIDO", id,
                AuditoriaService.diferencas(anterior, valoresFinanceiros(salvo)), null);
        return salvo;
    }

    @PreAuthorize("hasRole('GERENTE')")
    public Pedido cancelar(String id, String justificativa, Authentication autenticacao) {
        if (justificativa == null || justificativa.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe a justificativa do cancelamento.");
        }
        String motivo = justificativa.trim();
        if (motivo.length() > 500) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A justificativa deve ter até 500 caracteres.");
        }

        Pedido pedido = buscarPedido(id);
        StatusPedido status = interpretarStatus(pedido.getStatus());
        if (status == StatusPedido.SAIU_PARA_ENTREGA || status == StatusPedido.RETIRADO
                || status == StatusPedido.FINALIZADO || status == StatusPedido.CANCELADO) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O pedido não pode ser cancelado após a saída, conclusão ou cancelamento.");
        }

        Map<String, Object> anterior = estadoStatus(pedido);
        pedido.setStatus(StatusPedido.CANCELADO.name());
        pedido.setDataHoraCancelamento(Instant.now());
        pedido.setResponsavelCancelamento(autenticacao.getName());
        pedido.setJustificativaCancelamento(motivo);

        Pedido salvo = pedidoRepository.save(pedido);
        auditoriaService.registrar(autenticacao, "CANCELAMENTO", "PEDIDO", id,
                AuditoriaService.diferencas(anterior, estadoStatus(salvo)), motivo);
        return salvo;
    }

    private Pedido buscarPedido(String id) {
        return pedidoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Pedido não encontrado."));
    }

    private StatusPedido interpretarStatus(String status) {
        try {
            return StatusPedido.de(status);
        } catch (IllegalArgumentException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erro.getMessage());
        }
    }

    private boolean transicaoPermitida(StatusPedido atual, StatusPedido proximo) {
        return switch (atual) {
            case RECEBIDO -> proximo == StatusPedido.EM_PREPARACAO;
            case EM_PREPARACAO -> proximo == StatusPedido.PRONTO;
            case PRONTO -> proximo == StatusPedido.SAIU_PARA_ENTREGA || proximo == StatusPedido.RETIRADO;
            case SAIU_PARA_ENTREGA, RETIRADO -> proximo == StatusPedido.FINALIZADO;
            default -> false;
        };
    }

    private Map<String, Object> estadoStatus(Pedido pedido) {
        return AuditoriaService.valores(
                "status", pedido.getStatus(),
                "dataHoraSaida", pedido.getDataHoraSaida(),
                "modalidadeSaida", pedido.getModalidadeSaida(),
                "responsavelSaida", pedido.getResponsavelSaida(),
                "dataHoraFinalizacao", pedido.getDataHoraFinalizacao(),
                "dataHoraCancelamento", pedido.getDataHoraCancelamento(),
                "responsavelCancelamento", pedido.getResponsavelCancelamento(),
                "justificativaCancelamento", pedido.getJustificativaCancelamento());
    }

    private Map<String, Object> valoresFinanceiros(Pedido pedido) {
        return AuditoriaService.valores(
                "percentualDesconto", valor(pedido.getPercentualDesconto()),
                "valorDesconto", valor(pedido.getValorDesconto()),
                "valorTotal", valor(pedido.getValorTotal()));
    }

    private Map<String, Object> snapshot(Pedido pedido) {
        List<Map<String, Object>> itens = pedido.getItens().stream().map(item -> {
            Map<String, Object> valorItem = new LinkedHashMap<>();
            valorItem.put("produtoId", item.getProdutoId());
            valorItem.put("nomeProduto", item.getNomeProduto());
            valorItem.put("quantidade", item.getQuantidade());
            valorItem.put("precoUnitario", item.getPrecoUnitario());
            valorItem.put("valorTotal", item.getValorTotal());
            return valorItem;
        }).toList();
        return AuditoriaService.valores(
                "clienteId", pedido.getClienteId(),
                "itens", itens,
                "subtotal", pedido.getSubtotal(),
                "percentualDesconto", pedido.getPercentualDesconto(),
                "valorDesconto", pedido.getValorDesconto(),
                "valorTotal", pedido.getValorTotal(),
                "status", pedido.getStatus(),
                "responsavelRegistro", pedido.getResponsavelRegistro(),
                "dataHoraEntrada", pedido.getDataHoraEntrada());
    }

    private BigDecimal valor(BigDecimal valor) {
        return valor == null ? ZERO : valor.setScale(2, RoundingMode.HALF_UP);
    }
}
