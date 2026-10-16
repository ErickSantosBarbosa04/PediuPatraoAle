package com.umc.pediupatrao.service;

import com.umc.pediupatrao.entity.PerfilAcesso;
import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorId(String id) {
        return usuarioRepository.findById(id);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Optional<Usuario> buscarPorUsername(String username) {
        return usuarioRepository.findByUsername(username);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Usuario criarUsuario(Usuario recebido) {
        String username = validarUsername(recebido.getUsername());
        if (usuarioRepository.findByUsername(username).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse nome de usuário já está em uso.");
        }
        if (recebido.getPassword() == null || recebido.getPassword().length() < 8) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 8 caracteres.");
        }
        Usuario usuario = new Usuario();
        usuario.setUsername(username);
        usuario.setRole(validarPerfil(recebido.getRole()));
        usuario.setPassword(passwordEncoder.encode(recebido.getPassword()));
        return usuarioRepository.save(usuario);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public Usuario atualizarUsuario(String id, Usuario recebido, Authentication autenticacao) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        String username = validarUsername(recebido.getUsername());
        usuarioRepository.findByUsername(username)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> { throw new ResponseStatusException(HttpStatus.CONFLICT, "Esse nome de usuário já está em uso."); });

        String perfil = validarPerfil(recebido.getRole());
        if (existente.getUsername().equals(autenticacao.getName())) {
            if (!perfil.equals(existente.getRole())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não é permitido alterar o próprio perfil.");
            }
            if (!username.equals(existente.getUsername())) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Não é permitido alterar o próprio nome de usuário.");
            }
        }
        existente.setUsername(username);
        existente.setRole(perfil);
        if (recebido.getPassword() != null && !recebido.getPassword().isBlank()) {
            if (recebido.getPassword().length() < 8) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A senha deve ter pelo menos 8 caracteres.");
            }
            existente.setPassword(passwordEncoder.encode(recebido.getPassword()));
        }
        return usuarioRepository.save(existente);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public void deletarUsuario(String id, Authentication autenticacao) {
        Usuario existente = usuarioRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (existente.getUsername().equals(autenticacao.getName())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Não é permitido excluir a própria conta.");
        }
        if ("ADMIN".equals(existente.getRole())) {
            long totalAdmins = usuarioRepository.findAll().stream()
                    .filter(usuario -> "ADMIN".equals(PerfilAcesso.migrarPerfilLegado(usuario.getRole(), usuario.getUsername())))
                    .count();
            if (totalAdmins <= 1) {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "O sistema precisa manter ao menos um administrador.");
            }
        }
        usuarioRepository.delete(existente);
    }

    @PreAuthorize("hasRole('ADMIN')")
    public List<Usuario> listarTodos() {
        return usuarioRepository.findAll();
    }

    private String validarUsername(String valor) {
        if (valor == null || valor.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um nome de usuário.");
        }
        String username = valor.trim();
        if (username.length() < 3 || username.length() > 40) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O nome de usuário deve ter entre 3 e 40 caracteres.");
        }
        return username;
    }

    private String validarPerfil(String valor) {
        try {
            return PerfilAcesso.validar(valor).name();
        } catch (IllegalArgumentException erro) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, erro.getMessage());
        }
    }
}
