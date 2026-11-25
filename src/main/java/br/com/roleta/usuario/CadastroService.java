package br.com.roleta.usuario;

import java.math.BigDecimal;
import java.time.Clock;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import br.com.roleta.web.RegraDoJogoException;

@Service
public class CadastroService {

    public static final BigDecimal SALDO_INICIAL = new BigDecimal("1000.00");

    private final UsuarioRepository usuarios;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;

    public CadastroService(UsuarioRepository usuarios, PasswordEncoder passwordEncoder, Clock clock) {
        this.usuarios = usuarios;
        this.passwordEncoder = passwordEncoder;
        this.clock = clock;
    }

    @Transactional
    public Usuario cadastrar(CadastroForm form) {
        if (!form.getSenha().equals(form.getConfirmacaoSenha())) {
            throw new RegraDoJogoException("As senhas não conferem.");
        }
        String username = UsuarioDetailsService.normalizar(form.getUsername());
        if (usuarios.existsByUsername(username)) {
            throw new RegraDoJogoException("Este nome de usuário já está em uso.");
        }
        try {
            return usuarios.saveAndFlush(new Usuario(username, passwordEncoder.encode(form.getSenha()),
                    SALDO_INICIAL, clock.instant()));
        } catch (DataIntegrityViolationException e) {
            // Corrida entre dois cadastros simultâneos com o mesmo nome: a constraint UNIQUE decide.
            throw new RegraDoJogoException("Este nome de usuário já está em uso.");
        }
    }
}
