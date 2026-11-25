package br.com.roleta.jogo;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RodadaRepository extends JpaRepository<Rodada, Long> {

    Optional<Rodada> findFirstByUsuarioIdAndStatusInOrderByCriadaEmDesc(Long usuarioId,
            Collection<StatusRodada> status);

    /** Sempre filtra pelo dono: impede que um jogador acesse a rodada de outro trocando o id (IDOR). */
    Optional<Rodada> findByIdAndUsuarioId(Long id, Long usuarioId);

    Page<Rodada> findByUsuarioIdOrderByCriadaEmDesc(Long usuarioId, Pageable pageable);

    @Query("select r.pergunta from Rodada r where r.usuario.id = :usuarioId order by r.criadaEm desc")
    List<String> perguntasRecentes(@Param("usuarioId") Long usuarioId, Pageable pageable);
}
