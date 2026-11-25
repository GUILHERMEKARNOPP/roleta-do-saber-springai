# Salva a chave do OpenRouter como variavel de ambiente do usuario no Windows.
# A chave NAO fica gravada no projeto (nem vai para o GitHub).
# Uso: powershell -ExecutionPolicy Bypass -File scripts\configurar-chave.ps1

$chaveSegura = Read-Host "Cole sua chave do OpenRouter (comeca com sk-or-)" -AsSecureString
$chave = [System.Net.NetworkCredential]::new('', $chaveSegura).Password.Trim()

if (-not $chave.StartsWith('sk-or-')) {
    Write-Host "Isso nao parece uma chave do OpenRouter (deve comecar com sk-or-). Nada foi salvo." -ForegroundColor Red
    exit 1
}

[Environment]::SetEnvironmentVariable('OPENROUTER_API_KEY', $chave, 'User')
$env:OPENROUTER_API_KEY = $chave
Write-Host "Chave salva em OPENROUTER_API_KEY (variavel do usuario)." -ForegroundColor Green
Write-Host "Feche e abra o terminal/VS Code para os novos terminais enxergarem a variavel."
