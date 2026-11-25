package br.com.roleta.ia;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import br.com.roleta.jogo.Categoria;
import br.com.roleta.jogo.Premio;

/**
 * Usa o GenAI (via Spring AI) para criar a pergunta e decidir, com base no valor do
 * prêmio escolhido, a dificuldade e o bônus de chance.
 */
@Component
public class GeradorPerguntaIA {

    private static final Logger log = LoggerFactory.getLogger(GeradorPerguntaIA.class);
    private static final int TENTATIVAS = 2;

    static final String PROMPT_SISTEMA = """
            Você é o mestre de quiz da "Roleta do Saber", um jogo em que o jogador escolhe um prêmio \
            da roleta e precisa acertar uma pergunta para ganhar o direito de girar.

            Sua tarefa: criar UMA pergunta de múltipla escolha em português do Brasil, com exatamente \
            4 alternativas (A, B, C e D) e somente UMA correta.

            VOCÊ decide a dificuldade com base no valor do prêmio escolhido, comparando com os demais \
            prêmios da roleta: quanto mais valioso o prêmio, mais difícil a pergunta.
            Níveis possíveis: FACIL, MEDIO, DIFICIL, MUITO_DIFICIL.

            VOCÊ também decide o bônus de chance: um número inteiro entre 5 e 40 (pontos percentuais) \
            que será somado à chance do jogador ganhar o prêmio escolhido se ele acertar. Perguntas \
            mais difíceis merecem bônus maiores.

            Regras:
            - Use apenas fatos verificáveis e sem ambiguidade; nada de opinião ou "pegadinha" injusta.
            - As 4 alternativas devem ser plausíveis, curtas e diferentes entre si.
            - O campo respostaCorreta deve conter APENAS a letra (A, B, C ou D), nunca o texto da alternativa.
            - Não repita nenhuma das perguntas já feitas que forem listadas.
            - A explicação deve ter no máximo 2 frases e dizer por que a resposta está certa.
            - Responda SOMENTE com o objeto JSON pedido, sem texto antes ou depois.
            """;

    private final ChatClient chatClient;
    private final RandomGenerator random;
    private final BeanOutputConverter<RespostaIA> conversor = new BeanOutputConverter<>(RespostaIA.class);

    public GeradorPerguntaIA(ChatClient.Builder chatClientBuilder, RandomGenerator random) {
        this.chatClient = chatClientBuilder.build();
        this.random = random;
    }

    public PerguntaGerada gerar(Premio alvo, int posicaoNoRanking, List<Premio> tabela, Categoria categoria,
            List<String> perguntasRecentes) {
        // Mensagens montadas direto (sem template) porque as instruções de formato contêm chaves JSON.
        Prompt prompt = new Prompt(List.of(
                new SystemMessage(PROMPT_SISTEMA),
                new UserMessage(montarPedido(alvo, posicaoNoRanking, tabela, categoria, perguntasRecentes))));

        RuntimeException ultimoErro = null;
        for (int tentativa = 1; tentativa <= TENTATIVAS; tentativa++) {
            try {
                String texto = chatClient.prompt(prompt).call().content();
                RespostaIA bruta = conversor.convert(extrairJson(texto));
                return PerguntaGerada.validar(bruta).embaralhar(random);
            } catch (RuntimeException e) {
                ultimoErro = e;
                log.warn("Falha ao gerar pergunta com a IA (tentativa {}/{}): {}", tentativa, TENTATIVAS,
                        e.getMessage());
            }
        }
        throw new IaIndisponivelException("Não foi possível gerar a pergunta agora.", ultimoErro);
    }

    String montarPedido(Premio alvo, int posicaoNoRanking, List<Premio> tabela, Categoria categoria,
            List<String> perguntasRecentes) {
        NumberFormat reais = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
        String tabelaTexto = tabela.stream()
                .map(p -> "- " + p.rotulo() + " (valor de referência " + reais.format(p.valorReferencia()) + ")")
                .collect(Collectors.joining("\n"));
        String recentes = perguntasRecentes.isEmpty() ? "(nenhuma)"
                : perguntasRecentes.stream().map(p -> "- " + p).collect(Collectors.joining("\n"));

        return """
                Prêmios da roleta:
                %s

                Prêmio que o jogador escolheu: %s
                Valor de referência: %s
                Posição no ranking de valor: %dº mais valioso de %d.

                Tema da pergunta: %s

                Perguntas já feitas a este jogador (não repita):
                %s

                %s
                """.formatted(tabelaTexto, alvo.rotulo(), reais.format(alvo.valorReferencia()),
                posicaoNoRanking, tabela.size(), categoria.getNome(), recentes, conversor.getFormat());
    }

    /** Modelos gratuitos às vezes cercam o JSON com texto, ```json ou blocos de raciocínio. */
    static String extrairJson(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("IA não retornou conteúdo");
        }
        String semRaciocinio = texto.replaceAll("(?s)<think>.*?</think>", "");
        int inicio = semRaciocinio.indexOf('{');
        int fim = semRaciocinio.lastIndexOf('}');
        if (inicio < 0 || fim <= inicio) {
            throw new IllegalArgumentException("IA não retornou JSON");
        }
        return semRaciocinio.substring(inicio, fim + 1);
    }
}
