package com.umc.pediupatrao.config;

import com.umc.pediupatrao.entity.Usuario;
import com.umc.pediupatrao.entity.PerfilAcesso;
import com.umc.pediupatrao.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initDatabase(
            UsuarioRepository repo,
            PasswordEncoder passwordEncoder,
            @Value("${app.admin.username:admin}") String adminUsername,
            @Value("${app.admin.initial-password:}") String initialPassword) {
        return args -> {
            for (Usuario usuario : repo.findAll()) {
                String perfilMigrado = PerfilAcesso.migrarPerfilLegado(usuario.getRole(), usuario.getUsername());
                if (!perfilMigrado.equals(usuario.getRole())) {
                    usuario.setRole(perfilMigrado);
                    repo.save(usuario);
                }
            }

            if (repo.findByUsername(adminUsername).isPresent() || initialPassword.isBlank()) return;
            if (initialPassword.length() < 8) {
                throw new IllegalStateException("ADMIN_INITIAL_PASSWORD precisa ter pelo menos 8 caracteres.");
            }

            Usuario admin = new Usuario();
            admin.setUsername(adminUsername);
            admin.setPassword(passwordEncoder.encode(initialPassword));
            admin.setRole(PerfilAcesso.ADMIN.name());
            repo.save(admin);
        };
    }
}
