package br.com.roleta.jogo;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Contratos JSON da API do jogo. Nunca expõem a resposta correta antes do jogador responder. */
public final class JogoDto {

    private JogoDto() {
    }

    public record NovaRodadaRequest(
            @NotNull @Min(0) @Max(19) Integer premioIndice,
            @NotNull Categoria categoria) {
    }

    public record RespostaRequest(
            @NotBlank @Pattern(regexp = "^[ABCD]$") String alternativa) {
    }

    public record PremioView(int indice, TipoPremio tipo, String rotulo) {
    }

    public record CategoriaView(Categoria codigo, String nome) {
    }

    public record RodadaView(
            Long id,
            StatusRodada status,
            int premioAlvo,
            String categoria,
            String dificuldade,
            String pergunta,
            List<String> alternativas,
            int bonusChance,
            int chanceBase,
            Integer chanceFinal,
            long segundosRestantes) {
    }

    public record EstadoJogo(
            String usuario,
            BigDecimal saldo,
            long xp,
            BigDecimal custoTicket,
            int chanceBase,
            long prazoRespostaSegundos,
            List<PremioView> premios,
            List<CategoriaView> categorias,
            RodadaView rodadaAtual) {
    }

    public record ResultadoResposta(
            boolean acertou,
            boolean tempoEsgotado,
            String respostaCorreta,
            String explicacao,
            int chanceBase,
            Integer chanceFinal,
            BigDecimal saldo) {
    }

    public record ResultadoGiro(
            int premioSorteado,
            String descricaoGanho,
            boolean ganhouPremioEscolhido,
            BigDecimal saldo,
            long xp) {
    }

    public record HistoricoItem(
            Long id,
            Instant data,
            String premioEscolhido,
            String categoria,
            String dificuldade,
            StatusRodada status,
            String resultado) {
    }

    public record PaginaHistorico(List<HistoricoItem> itens, int pagina, int totalPaginas) {
    }
}
