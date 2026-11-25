package br.com.roleta.usuario;

import java.math.BigDecimal;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByUsername(String username);

    boolean existsByUsername(String username);

    /**
     * Débito atômico: só desconta se houver saldo, evitando saldo negativo
     * mesmo com requisições simultâneas. Retorna 0 se não havia saldo suficiente.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.saldo = u.saldo - :valor where u.id = :id and u.saldo >= :valor")
    int debitar(@Param("id") Long id, @Param("valor") BigDecimal valor);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Usuario u set u.saldo = u.saldo + :valor where u.id = :id")
    int creditar(@Param("id") Long id, @Param("valor") BigDecimal valor);
}
