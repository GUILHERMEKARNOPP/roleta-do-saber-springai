package br.com.roleta.config;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;

import br.com.roleta.usuario.LimitadorTentativas;
import br.com.roleta.usuario.UsuarioDetailsService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final String CSP = "default-src 'self'; script-src 'self'; style-src 'self'; "
            + "img-src 'self' data:; font-src 'self'; connect-src 'self'; object-src 'none'; "
            + "base-uri 'self'; form-action 'self'; frame-ancestors 'none'";

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Console do H2, só quando ligado explicitamente (spring.h2.console.enabled=true).
     * Ele tem login próprio do banco e só aceita conexões de localhost.
     */
    @Bean
    @Order(1)
    @ConditionalOnProperty(name = "spring.h2.console.enabled", havingValue = "true")
    public SecurityFilterChain h2ConsoleFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/h2-console/**")
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                .csrf(csrf -> csrf.disable())
                .headers(headers -> headers.frameOptions(frame -> frame.sameOrigin()));
        return http.build();
    }

    @Bean
    @Order(2)
    public SecurityFilterChain appFilterChain(HttpSecurity http,
            @Qualifier("limitadorLogin") LimitadorTentativas limitadorLogin) throws Exception {
        http.authorizeHttpRequests(auth -> auth
                        .requestMatchers("/login", "/cadastro", "/css/**", "/js/**", "/img/**", "/error",
                                "/favicon.ico")
                        .permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .successHandler(sucessoLogin(limitadorLogin))
                        .failureHandler(falhaLogin(limitadorLogin))
                        .permitAll())
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?saiu")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID"))
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CSP))
                        .referrerPolicy(ref -> ref.policy(ReferrerPolicy.SAME_ORIGIN)))
                .exceptionHandling(ex -> ex.authenticationEntryPoint(pontoDeEntrada()));
        return http.build();
    }

    /** Chamadas da API sem sessão recebem 401; páginas redirecionam para o login. */
    private static AuthenticationEntryPoint pontoDeEntrada() {
        RequestMatcher api = PathPatternRequestMatcher.withDefaults().matcher("/api/**");
        AuthenticationEntryPoint naoAutorizado = new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED);
        AuthenticationEntryPoint paginaLogin = new LoginUrlAuthenticationEntryPoint("/login");
        return (request, response, excecao) -> (api.matches(request) ? naoAutorizado : paginaLogin)
                .commence(request, response, excecao);
    }

    private static SavedRequestAwareAuthenticationSuccessHandler sucessoLogin(LimitadorTentativas limitador) {
        var handler = new SavedRequestAwareAuthenticationSuccessHandler() {
            @Override
            public void onAuthenticationSuccess(HttpServletRequest request,
                    HttpServletResponse response,
                    Authentication authentication)
                    throws ServletException, IOException {
                limitador.limpar(UsuarioDetailsService.chaveLogin(
                        UsuarioDetailsService.normalizar(authentication.getName()), request.getRemoteAddr()));
                super.onAuthenticationSuccess(request, response, authentication);
            }
        };
        handler.setDefaultTargetUrl("/");
        handler.setAlwaysUseDefaultTargetUrl(true);
        return handler;
    }

    private static SimpleUrlAuthenticationFailureHandler falhaLogin(LimitadorTentativas limitador) {
        return new SimpleUrlAuthenticationFailureHandler("/login?erro") {
            @Override
            public void onAuthenticationFailure(HttpServletRequest request,
                    HttpServletResponse response,
                    AuthenticationException exception)
                    throws IOException, ServletException {
                if (exception instanceof LockedException) {
                    getRedirectStrategy().sendRedirect(request, response, "/login?bloqueado");
                    return;
                }
                String username = UsuarioDetailsService.normalizar(request.getParameter("username"));
                // Nomes fora do formato não podem existir; não ocupam espaço no limitador.
                if (UsuarioDetailsService.usernameValido(username)) {
                    limitador.registrar(UsuarioDetailsService.chaveLogin(username, request.getRemoteAddr()));
                }
                super.onAuthenticationFailure(request, response, exception);
            }
        };
    }
}
