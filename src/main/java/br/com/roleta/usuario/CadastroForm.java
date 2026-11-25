package br.com.roleta.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CadastroForm {

    @NotBlank(message = "Informe um nome de usuário.")
    @Pattern(regexp = "^[a-zA-Z0-9_.]{3,30}$",
            message = "Use de 3 a 30 caracteres: letras, números, ponto ou _.")
    private String username;

    // BCrypt só considera os primeiros 72 bytes, por isso o limite superior.
    @NotBlank(message = "Informe uma senha.")
    @Size(min = 8, max = 72, message = "A senha deve ter entre 8 e 72 caracteres.")
    private String senha;

    @NotBlank(message = "Confirme a senha.")
    private String confirmacaoSenha;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getSenha() {
        return senha;
    }

    public void setSenha(String senha) {
        this.senha = senha;
    }

    public String getConfirmacaoSenha() {
        return confirmacaoSenha;
    }

    public void setConfirmacaoSenha(String confirmacaoSenha) {
        this.confirmacaoSenha = confirmacaoSenha;
    }
}
