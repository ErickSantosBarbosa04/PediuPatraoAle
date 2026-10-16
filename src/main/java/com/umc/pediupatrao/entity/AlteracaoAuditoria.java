package com.umc.pediupatrao.entity;

public class AlteracaoAuditoria {
    private Object valorAnterior;
    private Object valorNovo;

    public AlteracaoAuditoria() { }

    public AlteracaoAuditoria(Object valorAnterior, Object valorNovo) {
        this.valorAnterior = valorAnterior;
        this.valorNovo = valorNovo;
    }

    public Object getValorAnterior() { return valorAnterior; }
    public void setValorAnterior(Object valorAnterior) { this.valorAnterior = valorAnterior; }
    public Object getValorNovo() { return valorNovo; }
    public void setValorNovo(Object valorNovo) { this.valorNovo = valorNovo; }
}
