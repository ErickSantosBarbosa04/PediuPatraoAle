package com.umc.pediupatrao.entity;

public enum StatusPedido {
    RECEBIDO,
    EM_PREPARACAO,
    PRONTO,
    SAIU_PARA_ENTREGA,
    RETIRADO,
    FINALIZADO,
    CANCELADO;

    public static StatusPedido de(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new IllegalArgumentException("Informe o próximo status do pedido.");
        }
        String normalizado = valor.trim().toUpperCase();
        if ("EM_PREPARO".equals(normalizado)) {
            normalizado = "EM_PREPARACAO";
        } else if ("ENTREGUE".equals(normalizado)) {
            normalizado = "FINALIZADO";
        }
        try {
            return valueOf(normalizado);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Status de pedido inválido.");
        }
    }
}
