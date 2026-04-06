package com.wellpag.config;

import com.wellpag.model.AuthProvider;
import com.wellpag.model.Role;
import com.wellpag.model.Usuario;
import com.wellpag.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.findByUsername("calama").isEmpty()) {
            Usuario admin = new Usuario();
            admin.setUsername("calama");
            admin.setNome("Carlos Montanini");
            admin.setSenha(passwordEncoder.encode("131714"));
            admin.setRole(Role.PROFESSOR);
            admin.setProvider(AuthProvider.LOCAL);
            admin.setWebhookToken(java.util.UUID.randomUUID().toString().replace("-", ""));
            usuarioRepository.save(admin);
        }
    }
}
