package com.wellpag.auth.service;

import com.wellpag.auth.dto.AuthResponse;
import com.wellpag.auth.dto.LoginRequest;
import com.wellpag.auth.dto.RegisterRequest;
import com.wellpag.auth.model.Aluno;
import com.wellpag.auth.model.AuthProvider;
import com.wellpag.auth.model.Role;
import com.wellpag.auth.model.Usuario;
import com.wellpag.auth.repository.AlunoRepository;
import com.wellpag.auth.repository.UsuarioRepository;
import com.wellpag.auth.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final AlunoRepository alunoRepository;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;

    // Escrita cruzada de dominio (vincular Aluno.usuarioId): AlunoRepository e
    // read-only de proposito (ver repository/AlunoRepository.java), entao esse
    // save pontual usa MongoTemplate diretamente. Sera substituido por uma
    // chamada REST ao futuro aluno-service quando ele tiver sua propria base.
    private final MongoTemplate mongoTemplate;

    public AuthResponse registrar(RegisterRequest request) {
        if (usuarioRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("E-mail já cadastrado");
        }

        // Se existe algum Aluno com esse e-mail, o usuário é ALUNO; caso contrário PROFESSOR.
        List<Aluno> alunosVinculados = alunoRepository.findByEmail(request.email());
        Role role = alunosVinculados.isEmpty() ? Role.PROFESSOR : Role.ALUNO;

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome());
        usuario.setEmail(request.email());
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setRole(role);
        usuario.setProvider(AuthProvider.LOCAL);
        usuario.setWebhookToken(java.util.UUID.randomUUID().toString().replace("-", ""));
        usuarioRepository.save(usuario);

        // Vincula os registros de Aluno à conta recém-criada
        if (!alunosVinculados.isEmpty()) {
            alunosVinculados.forEach(a -> {
                a.setUsuarioId(usuario.getId());
                mongoTemplate.save(a);
            });
        }

        String token = jwtService.generate(usuario);
        return new AuthResponse(token, usuario.getNome(), usuario.getEmail(), usuario.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.email(), request.senha())
        );

        Usuario usuario = usuarioRepository.findByEmail(request.email())
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        String token = jwtService.generate(usuario);
        return new AuthResponse(token, usuario.getNome(), usuario.getEmail(), usuario.getRole().name());
    }
}
