package com.wellpag.security;

import com.wellpag.model.Aluno;
import com.wellpag.model.AuthProvider;
import com.wellpag.model.Role;
import com.wellpag.model.Usuario;
import com.wellpag.repository.AlunoRepository;
import com.wellpag.repository.UsuarioRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final UsuarioRepository usuarioRepository;
    private final AlunoRepository alunoRepository;
    private final JwtService jwtService;

    @Value("${wellpag.frontend-url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2AuthenticationToken oauthToken = (OAuth2AuthenticationToken) authentication;
        OAuth2User oAuth2User = oauthToken.getPrincipal();
        String registrationId = oauthToken.getAuthorizedClientRegistrationId();

        String email = resolveEmail(oAuth2User, registrationId);
        String nome  = oAuth2User.getAttribute("name");
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        Usuario usuario = usuarioRepository.findByEmail(email).orElseGet(() -> {
            List<Aluno> alunosVinculados = alunoRepository.findByEmail(email);
            Role role = alunosVinculados.isEmpty() ? Role.PROFESSOR : Role.ALUNO;

            Usuario novo = new Usuario();
            novo.setEmail(email);
            novo.setNome(nome);
            novo.setRole(role);
            novo.setProvider(provider);
            novo.setWebhookToken(java.util.UUID.randomUUID().toString().replace("-", ""));
            usuarioRepository.save(novo);

            if (!alunosVinculados.isEmpty()) {
                alunosVinculados.forEach(a -> a.setUsuarioId(novo.getId()));
                alunoRepository.saveAll(alunosVinculados);
            }

            return novo;
        });

        String token = jwtService.generate(usuario);
        getRedirectStrategy().sendRedirect(request, response,
            frontendUrl + "/auth/callback?token=" + token);
    }

    private String resolveEmail(OAuth2User user, String registrationId) {
        String email = user.getAttribute("email");
        if (email == null) {
            throw new IllegalStateException("E-mail não retornado pelo provider " + registrationId);
        }
        return email;
    }
}
