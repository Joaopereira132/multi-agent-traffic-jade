@echo off
setlocal

echo [1/5] A limpar relatorios antigos...
if exist metrics_report.txt del metrics_report.txt

echo [2/5] A encerrar processos java antigos...
taskkill /F /IM java.exe >nul 2>&1

echo [3/5] A apagar a pasta build antiga...
if exist build rmdir /s /q build
if exist build echo ERRO: Nao foi possivel apagar a pasta build. Fecha tudo e tenta de novo. & pause & exit

echo [4/5] A criar nova pasta build...
mkdir build

echo [5/5] A COMPILAR TUDO DE NOVO...
javac -encoding UTF-8 -cp libs\jade.jar -d build src\traffic\*.java

if %errorlevel% neq 0 (
    echo.
    echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    echo !!! ERRO DE COMPILACAO - LEIA ACIMA !!!
    echo !!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!!
    pause
    exit /b
)

echo.
echo SUCCESSO! A INICIAR...
java -cp build;libs\jade.jar jade.Boot -gui -agents "starter:traffic.StarterAgent"