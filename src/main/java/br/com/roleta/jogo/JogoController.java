package br.com.roleta.jogo;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import br.com.roleta.usuario.UsuarioAutenticado;

@Controller
public class JogoController {

    @GetMapping("/")
    public String jogo(@AuthenticationPrincipal UsuarioAutenticado usuario, Model model) {
        model.addAttribute("usuario", usuario.getUsername());
        return "jogo";
    }
}
