package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Cliente;
import com.umc.pediupatrao.repository.ClienteRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ClienteService {

    private final ClienteRepository clienteRepository;
    private final AuditoriaService auditoriaService;

    public ClienteService(ClienteRepository clienteRepository, AuditoriaService auditoriaService) {
        this.clienteRepository = clienteRepository;
        this.auditoriaService = auditoriaService;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Cliente> listarClientes() {
        return clienteRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public Optional<Cliente> buscarPorId(String id) {
        return clienteRepository.findById(id);
    }

    @PreAuthorize("hasRole('ATENDENTE')")
    public Cliente salvar(Cliente recebido, Authentication autenticacao) {
        if (recebido == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados do cliente.");
        }
        if (recebido.getNome() == null || recebido.getNome().isBlank()
                || recebido.getTelefone() == null || recebido.getTelefone().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Nome e telefone são obrigatórios.");
        }

        if (recebido.getId() == null || recebido.getId().isBlank()) {
            recebido.setId(null);
            Cliente salvo = clienteRepository.save(recebido);
            auditoriaService.registrar(autenticacao, "INCLUSAO", "CLIENTE", salvo.getId(),
                    AuditoriaService.diferencas(null, snapshot(salvo)), null);
            return salvo;
        }

        Cliente anterior = clienteRepository.findById(recebido.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
        Cliente salvo = clienteRepository.save(recebido);
        auditoriaService.registrar(autenticacao, "ALTERACAO", "CLIENTE", salvo.getId(),
                AuditoriaService.diferencas(snapshot(anterior), snapshot(salvo)), null);
        return salvo;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void excluir(String id, Authentication autenticacao) {
        Cliente anterior = clienteRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cliente não encontrado."));
        clienteRepository.delete(anterior);
        auditoriaService.registrar(autenticacao, "EXCLUSAO", "CLIENTE", id,
                AuditoriaService.diferencas(snapshot(anterior), null), null);
    }

    private Map<String, Object> snapshot(Cliente cliente) {
        return AuditoriaService.valores(
                "nome", cliente.getNome(),
                "telefone", cliente.getTelefone(),
                "cep", cliente.getCep(),
                "logradouro", cliente.getLogradouro(),
                "numero", cliente.getNumero(),
                "complemento", cliente.getComplemento(),
                "bairro", cliente.getBairro(),
                "cidade", cliente.getCidade(),
                "estado", cliente.getEstado(),
                "endereco", cliente.getEndereco());
    }
}
