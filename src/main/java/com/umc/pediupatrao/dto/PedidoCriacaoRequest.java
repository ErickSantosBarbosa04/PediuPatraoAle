package com.umc.pediupatrao.dto;

import java.util.ArrayList;
import java.util.List;

public class PedidoCriacaoRequest {
    private String clienteId;
    private List<ItemPedidoRequest> itens = new ArrayList<>();
    // Campos legados aceitos apenas para resolver o produto; o preço continua vindo do cadastro.
    private String tipoPizza;
    private String sabor;
    private int quantidade;

    public String getClienteId() { return clienteId; }
    public void setClienteId(String clienteId) { this.clienteId = clienteId; }
    public List<ItemPedidoRequest> getItens() { return itens; }
    public void setItens(List<ItemPedidoRequest> itens) { this.itens = itens == null ? new ArrayList<>() : itens; }
    public String getTipoPizza() { return tipoPizza; }
    public void setTipoPizza(String tipoPizza) { this.tipoPizza = tipoPizza; }
    public String getSabor() { return sabor; }
    public void setSabor(String sabor) { this.sabor = sabor; }
    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }
}
