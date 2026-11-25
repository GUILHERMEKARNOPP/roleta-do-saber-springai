package br.com.roleta.usuario;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import br.com.roleta.web.RegraDoJogoException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
public class AuthController {

    private final CadastroService cadastroService;
    private final LimitadorTentativas limitadorCadastro;

    public AuthController(CadastroService cadastroService,
            @Qualifier("limitadorCadastro") LimitadorTentativas limitadorCadastro) {
        this.cadastroService = cadastroService;
        this.limitadorCadastro = limitadorCadastro;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/cadastro")
    public String formularioCadastro(Model model) {
        model.addAttribute("form", new CadastroForm());
        return "cadastro";
    }

    @PostMapping("/cadastro")
    public String cadastrar(@Valid @ModelAttribute("form") CadastroForm form, BindingResult erros,
            HttpServletRequest request, Model model) {
        if (erros.hasErrors()) {
            return "cadastro";
        }
        // Conta toda tentativa (inclusive nome repetido) de forma atômica: evita corrida
        // com cadastros paralelos e limita a descoberta de nomes de usuário existentes.
        if (!limitadorCadastro.tentarConsumir(request.getRemoteAddr())) {
            model.addAttribute("erroGeral", "Muitas tentativas de cadastro a partir desta rede. Tente mais tarde.");
            return "cadastro";
        }
        try {
            cadastroService.cadastrar(form);
        } catch (RegraDoJogoException e) {
            model.addAttribute("erroGeral", e.getMessage());
            return "cadastro";
        }
        return "redirect:/login?cadastrado";
    }
}
