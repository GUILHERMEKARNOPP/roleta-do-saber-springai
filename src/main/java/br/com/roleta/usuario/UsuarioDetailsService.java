package br.com.roleta.usuario;

import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Service
public class UsuarioDetailsService implements UserDetailsService {

    private static final Pattern USERNAME_VALIDO = Pattern.compile("^[a-z0-9_.]{3,30}$");

    private final UsuarioRepository usuarios;
    private final LimitadorTentativas limitadorLogin;

    public UsuarioDetailsService(UsuarioRepository usuarios,
            @Qualifier("limitadorLogin") LimitadorTentativas limitadorLogin) {
        this.usuarios = usuarios;
        this.limitadorLogin = limitadorLogin;
    }

    @Override
    public UserDetails loadUserByUsername(String username) {
        String normalizado = normalizar(username);
        if (!usernameValido(normalizado)) {
            throw new UsernameNotFoundException("Usuário não encontrado");
        }
        Usuario usuario = usuarios.findByUsername(normalizado)
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado"));
        return new UsuarioAutenticado(usuario, limitadorLogin.bloqueado(chaveLogin(normalizado, ipAtual())));
    }

    public static String normalizar(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public static boolean usernameValido(String normalizado) {
        return USERNAME_VALIDO.matcher(normalizado).matches();
    }

    /**
     * Chave do limite de login: usuário + IP. Assim um atacante não consegue bloquear
     * a conta de outra pessoa a partir da própria máquina.
     */
    public static String chaveLogin(String usernameNormalizado, String ip) {
        return usernameNormalizado + "|" + ip;
    }

    private static String ipAtual() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes atributos) {
            return atributos.getRequest().getRemoteAddr();
        }
        return "desconhecido";
    }
}
