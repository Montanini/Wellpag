package com.wellpag.security;

import com.wellpag.model.Usuario;
import com.wellpag.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    @Override
    public UserDetails loadUserByUsername(String login) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByUsername(login)
            .or(() -> usuarioRepository.findByEmail(login))
            .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado: " + login));

        String principal = usuario.getUsername() != null ? usuario.getUsername() : usuario.getEmail();
        return new User(
            principal,
            usuario.getSenha() != null ? usuario.getSenha() : "",
            List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRole().name()))
        );
    }
}
