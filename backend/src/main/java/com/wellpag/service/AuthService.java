package com.wellpag.service;

import com.wellpag.dto.AuthResponse;
import com.wellpag.dto.LoginRequest;
import com.wellpag.dto.RegisterRequest;
import com.wellpag.model.Aluno;
import com.wellpag.model.AuthProvider;
import com.wellpag.model.Role;
import com.wellpag.model.Usuario;
import com.wellpag.repository.AlunoRepository;
import com.wellpag.repository.UsuarioRepository;
import com.wellpag.security.JwtService;
import lombok.RequiredArgsConstructor;
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
            alunosVinculados.forEach(a -> a.setUsuarioId(usuario.getId()));
            alunoRepository.saveAll(alunosVinculados);
        }

        String token = jwtService.generate(usuario);
        return new AuthResponse(token, usuario.getNome(), usuario.getEmail(), usuario.getRole().name());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.login(), request.senha())
        );

        Usuario usuario = usuarioRepository.findByUsername(request.login())
            .or(() -> usuarioRepository.findByEmail(request.login()))
            .orElseThrow(() -> new IllegalArgumentException("Usuário não encontrado"));

        String token = jwtService.generate(usuario);
        return new AuthResponse(token, usuario.getNome(), usuario.getEmail(), usuario.getRole().name());
    }
}
