package br.com.roleta.usuario;

import java.util.List;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;

/** Principal da sessão; carrega o id para os endpoints não precisarem buscar o usuário pelo nome. */
public class UsuarioAutenticado extends User {

    private final Long id;

    public UsuarioAutenticado(Usuario usuario, boolean bloqueado) {
        super(usuario.getUsername(), usuario.getSenhaHash(), true, true, true, !bloqueado,
                List.of(new SimpleGrantedAuthority("ROLE_JOGADOR")));
        this.id = usuario.getId();
    }

    public Long getId() {
        return id;
    }

    @Override
    public boolean equals(Object outro) {
        return super.equals(outro);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
