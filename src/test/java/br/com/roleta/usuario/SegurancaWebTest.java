package br.com.roleta.usuario;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import br.com.roleta.ia.GeradorPerguntaIA;

@SpringBootTest
@AutoConfigureMockMvc
class SegurancaWebTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private UsuarioRepository usuarios;
    @Autowired
    private PasswordEncoder passwordEncoder;

    @MockitoBean
    private GeradorPerguntaIA geradorPergunta;

    @Test
    void paginaDoJogoExigeLogin() throws Exception {
        mvc.perform(get("/")).andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void apiSemSessaoRetorna401() throws Exception {
        mvc.perform(get("/api/estado")).andExpect(status().isUnauthorized());
    }

    @Test
    void apiRecusaPostSemTokenCsrf() throws Exception {
        mvc.perform(post("/api/rodadas").with(user("alguem"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"premioIndice\":1,\"categoria\":\"CIENCIAS\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void paginasTemCabecalhosDeSeguranca() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(header().exists("Content-Security-Policy"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @Test
    void cadastroCriaUsuarioComSenhaEmHashESaldoInicial() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("username", "Maria_Teste")
                        .param("senha", "senha-super-secreta")
                        .param("confirmacaoSenha", "senha-super-secreta"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?cadastrado"));

        Usuario criado = usuarios.findByUsername("maria_teste").orElseThrow();
        assertThat(criado.getSaldo()).isEqualByComparingTo("1000");
        assertThat(criado.getSenhaHash()).isNotEqualTo("senha-super-secreta");
        assertThat(passwordEncoder.matches("senha-super-secreta", criado.getSenhaHash())).isTrue();
    }

    @Test
    void cadastroComSenhasDiferentesNaoCriaUsuario() throws Exception {
        mvc.perform(post("/cadastro").with(csrf())
                        .param("username", "joao_teste")
                        .param("senha", "senha-super-secreta")
                        .param("confirmacaoSenha", "outra-senha-qualquer"))
                .andExpect(status().isOk());

        assertThat(usuarios.existsByUsername("joao_teste")).isFalse();
    }
}
