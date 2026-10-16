package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.AlteracaoAuditoria;
import com.umc.pediupatrao.entity.EventoAuditoria;
import com.umc.pediupatrao.repository.AuditoriaRepository;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
public class AuditoriaService {

    private final AuditoriaRepository repository;

    public AuditoriaService(AuditoriaRepository repository) {
        this.repository = repository;
    }

    public void registrar(
            Authentication autenticacao,
            String operacao,
            String entidade,
            String entidadeId,
            Map<String, AlteracaoAuditoria> campos,
            String justificativa) {
        if (autenticacao == null || !autenticacao.isAuthenticated()) {
            throw new IllegalStateException("A auditoria exige um usuário autenticado.");
        }

        EventoAuditoria evento = new EventoAuditoria();
        evento.setUsuario(autenticacao.getName());
        evento.setPerfil(autenticacao.getAuthorities().stream()
                .map(authority -> authority.getAuthority())
                .filter(authority -> authority.startsWith("ROLE_"))
                .map(authority -> authority.substring("ROLE_".length()))
                .findFirst()
                .orElse("DESCONHECIDO"));
        evento.setDataHora(Instant.now());
        evento.setOperacao(operacao);
        evento.setEntidade(entidade);
        evento.setEntidadeId(entidadeId);
        evento.setCampos(campos == null ? Map.of() : campos);
        evento.setJustificativa(justificativa);
        repository.save(evento);
    }

    public static Map<String, AlteracaoAuditoria> diferencas(
            Map<String, Object> anterior,
            Map<String, Object> novo) {
        Map<String, AlteracaoAuditoria> resultado = new LinkedHashMap<>();
        Set<String> nomes = new LinkedHashSet<>();
        if (anterior != null) nomes.addAll(anterior.keySet());
        if (novo != null) nomes.addAll(novo.keySet());

        for (String nome : nomes) {
            Object valorAnterior = anterior == null ? null : anterior.get(nome);
            Object valorNovo = novo == null ? null : novo.get(nome);
            if (!Objects.equals(valorAnterior, valorNovo)) {
                resultado.put(nome, new AlteracaoAuditoria(valorAnterior, valorNovo));
            }
        }
        return resultado;
    }

    public static Map<String, Object> valores(Object... pares) {
        Map<String, Object> resultado = new LinkedHashMap<>();
        for (int i = 0; i + 1 < pares.length; i += 2) {
            resultado.put(String.valueOf(pares[i]), pares[i + 1]);
        }
        return resultado;
    }
}
