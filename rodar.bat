@echo off
setlocal
title Roleta do Saber
cd /d "%~dp0"

rem Porta opcional: rodar.bat 8085  (padrao 8080)
set "PORTA=%~1"
if "%PORTA%"=="" set "PORTA=8080"

rem Le a chave salva nas variaveis do usuario (funciona mesmo se o terminal foi aberto antes de salvar).
if not defined OPENROUTER_API_KEY (
    for /f "tokens=2,*" %%a in ('reg query HKCU\Environment /v OPENROUTER_API_KEY 2^>nul') do set "OPENROUTER_API_KEY=%%b"
)

if not defined OPENROUTER_API_KEY (
    echo.
    echo [!] Chave do OpenRouter nao encontrada.
    echo     Crie uma chave gratis em https://openrouter.ai/settings/keys
    echo.
    choice /c SN /m "Deseja configurar a chave agora"
    if errorlevel 2 goto :semchave
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0scripts\configurar-chave.ps1"
    for /f "tokens=2,*" %%a in ('reg query HKCU\Environment /v OPENROUTER_API_KEY 2^>nul') do set "OPENROUTER_API_KEY=%%b"
)
if not defined OPENROUTER_API_KEY goto :semchave

echo.
echo ===============================================
echo   Roleta do Saber
echo   Iniciando em http://localhost:%PORTA%
echo   O navegador abre sozinho quando estiver pronto.
echo   Para parar: feche esta janela ou aperte Ctrl+C
echo ===============================================
echo.

rem Abre o navegador assim que a porta responder (espera no maximo 3 minutos).
start "" /b powershell -NoProfile -WindowStyle Hidden -Command "$fim=(Get-Date).AddMinutes(3); while((Get-Date) -lt $fim){ try { $c=New-Object Net.Sockets.TcpClient('localhost',%PORTA%); $c.Close(); Start-Process 'http://localhost:%PORTA%'; break } catch { Start-Sleep -Seconds 1 } }"

call "%~dp0mvnw.cmd" -q spring-boot:run "-Dspring-boot.run.arguments=--server.port=%PORTA%"

echo.
echo O sistema foi encerrado.
pause
exit /b

:semchave
echo.
echo Sem a chave a IA nao funciona. Rode este arquivo de novo depois de configurar.
pause
exit /b 1
