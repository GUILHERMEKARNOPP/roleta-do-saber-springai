package br.com.roleta.usuario;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 30)
    private String username;

    @Column(name = "senha_hash", nullable = false, length = 100)
    private String senhaHash;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal saldo;

    @Column(nullable = false)
    private long xp;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    protected Usuario() {
    }

    public Usuario(String username, String senhaHash, BigDecimal saldoInicial, Instant criadoEm) {
        this.username = username;
        this.senhaHash = senhaHash;
        this.saldo = saldoInicial;
        this.criadoEm = criadoEm;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public BigDecimal getSaldo() {
        return saldo;
    }

    public long getXp() {
        return xp;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }
}
