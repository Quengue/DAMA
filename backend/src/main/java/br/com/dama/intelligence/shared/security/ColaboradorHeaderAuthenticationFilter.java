package br.com.dama.intelligence.shared.security;

import br.com.dama.intelligence.perfil.api.PerfilFacade;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 * Autenticação simplificada por cabeçalho ("X-Colaborador-Id"): nenhum RF do
 * documento descreve login/senha, mas RNF02/RN01/RN07 exigem que o acesso
 * respeite o perfil do usuário. Este filtro resolve o colaborador informado,
 * carrega suas permissões (via PerfilFacade) e as expõe como authorities do
 * Spring Security, permitindo usar @PreAuthorize nos controllers.
 *
 * Antes de produção isto deve ser substituído por um mecanismo real de
 * autenticação (JWT/OAuth2/SSO da empresa-cliente); o cabeçalho aqui não
 * prova identidade, só permite testar e enforçar as regras de perfil.
 */
public class ColaboradorHeaderAuthenticationFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Colaborador-Id";

    private final PerfilFacade perfilFacade;

    public ColaboradorHeaderAuthenticationFilter(PerfilFacade perfilFacade) {
        this.perfilFacade = perfilFacade;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String header = request.getHeader(HEADER);
        if (header != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            try {
                Long colaboradorId = Long.valueOf(header);
                Set<String> permissoes = perfilFacade.permissoesDoColaborador(colaboradorId);
                List<GrantedAuthority> authorities = permissoes.stream()
                        .map(SimpleGrantedAuthority::new)
                        .map(GrantedAuthority.class::cast)
                        .toList();

                var authentication = new UsernamePasswordAuthenticationToken(colaboradorId, null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (NumberFormatException ignored) {
                // cabeçalho inválido: segue sem autenticar, a cadeia de segurança nega o acesso
            }
        }
        chain.doFilter(request, response);
    }
}
