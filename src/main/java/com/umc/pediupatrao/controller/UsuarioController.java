package com.umc.pediupatrao.controller;

import com.umc.pediupatrao.dto.UsuarioRequest;
import com.umc.pediupatrao.dto.UsuarioResumo;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.service.UsuarioService;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public List<UsuarioResumo> listarTodos() {
        return usuarioService.listarTodos().stream().map(UsuarioController::resumo).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<UsuarioResumo> buscarPorId(@PathVariable String id) {
        return usuarioService.buscarPorId(id)
                .map(usuario -> ResponseEntity.ok(resumo(usuario)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<UsuarioResumo> criarUsuario(@RequestBody UsuarioRequest requisicao) {
        Usuario usuario = preencher(requisicao);
        return ResponseEntity.status(HttpStatus.CREATED).body(resumo(usuarioService.criarUsuario(usuario)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<UsuarioResumo> atualizarUsuario(
            @PathVariable String id,
            @RequestBody UsuarioRequest requisicao,
            Authentication autenticacao) {
        return ResponseEntity.ok(resumo(usuarioService.atualizarUsuario(id, preencher(requisicao), autenticacao)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletarUsuario(@PathVariable String id, Authentication autenticacao) {
        usuarioService.deletarUsuario(id, autenticacao);
        return ResponseEntity.noContent().build();
    }

    private static UsuarioResumo resumo(Usuario usuario) {
        return new UsuarioResumo(usuario.getId(), usuario.getUsername(), usuario.getRole());
    }

    private static Usuario preencher(UsuarioRequest requisicao) {
        if (requisicao == null) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Informe os dados do usuário.");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(requisicao.getUsername());
        usuario.setPassword(requisicao.getPassword());
        usuario.setRole(requisicao.getRole());
        return usuario;
    }
}
