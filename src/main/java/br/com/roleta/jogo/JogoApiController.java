package br.com.roleta.jogo;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import br.com.roleta.jogo.JogoDto.EstadoJogo;
import br.com.roleta.jogo.JogoDto.NovaRodadaRequest;
import br.com.roleta.jogo.JogoDto.PaginaHistorico;
import br.com.roleta.jogo.JogoDto.RespostaRequest;
import br.com.roleta.jogo.JogoDto.ResultadoGiro;
import br.com.roleta.jogo.JogoDto.ResultadoResposta;
import br.com.roleta.jogo.JogoDto.RodadaView;
import br.com.roleta.usuario.UsuarioAutenticado;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

@Validated
@RestController
@RequestMapping("/api")
public class JogoApiController {

    private final JogoService jogoService;

    public JogoApiController(JogoService jogoService) {
        this.jogoService = jogoService;
    }

    @GetMapping("/estado")
    public EstadoJogo estado(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return jogoService.estado(usuario.getId());
    }

    @PostMapping("/rodadas")
    public RodadaView comprarTicket(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @Valid @RequestBody NovaRodadaRequest pedido) {
        return jogoService.comprarTicket(usuario.getId(), pedido.premioIndice(), pedido.categoria());
    }

    @PostMapping("/rodadas/{id}/resposta")
    public ResultadoResposta responder(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @PathVariable Long id, @Valid @RequestBody RespostaRequest pedido) {
        return jogoService.responder(usuario.getId(), id, pedido.alternativa());
    }

    @PostMapping("/rodadas/{id}/giro")
    public ResultadoGiro girar(@AuthenticationPrincipal UsuarioAutenticado usuario, @PathVariable Long id) {
        return jogoService.girar(usuario.getId(), id);
    }

    @GetMapping("/historico")
    public PaginaHistorico historico(@AuthenticationPrincipal UsuarioAutenticado usuario,
            @RequestParam(defaultValue = "0") @Min(0) @Max(10_000) int pagina) {
        return jogoService.historico(usuario.getId(), pagina);
    }
}
