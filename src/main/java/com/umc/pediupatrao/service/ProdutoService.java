package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.Produto;
import com.umc.pediupatrao.repository.ProdutoRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProdutoService {

    private final ProdutoRepository produtoRepository;

    public ProdutoService(ProdutoRepository produtoRepository) {
        this.produtoRepository = produtoRepository;
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Produto> listarProdutos() {
        return produtoRepository.findAll();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE', 'ATENDENTE')")
    public List<Produto> listarProdutosAtivos() {
        return produtoRepository.findAll().stream()
                .filter(produto -> Boolean.TRUE.equals(produto.getAtivo())
                        && produto.getPreco() != null && produto.getPreco() > 0)
                .toList();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public void excluir(String id) {
        if (!produtoRepository.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado.");
        }
        produtoRepository.deleteById(id);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'GERENTE')")
    public Produto salvar(Produto produto) {
        if (produto == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe os dados do produto.");
        }
        if (produto.getNome() == null || produto.getNome().isBlank()
                || produto.getTipo() == null || produto.getTipo().isBlank()
                || produto.getPreco() == null || !Double.isFinite(produto.getPreco()) || produto.getPreco() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe nome, tipo e um preço maior que zero.");
        }
        if (produto.getId() == null || produto.getId().isBlank()) {
            produto.setId(null);
            if (produto.getAtivo() == null) produto.setAtivo(true);
            return produtoRepository.save(produto);
        }
        if (!produtoRepository.existsById(produto.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Produto não encontrado.");
        }
        return produtoRepository.save(produto);
    }
}
