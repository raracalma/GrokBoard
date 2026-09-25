# GrokBoard — MVP

Teclado Android com Grok: persona + corrigir, reescrever, encurtar, compor, responder no X.

## Gerar o APK só com o celular (GitHub Actions)

Não precisa de Android Studio nem de PC. O GitHub compila e você baixa o APK.

1. No Chrome do celular, entra em [github.com](https://github.com) e cria uma conta (ou entra).
2. **New repository**. Nome: `GrokBoard`. Público. **Não** marque README, .gitignore nem license.
3. No celular, descompacta este zip (app Arquivos da Samsung).
4. No repositório vazio, **Add file → Upload files**. Sobe a pasta inteira, incluindo a pasta oculta `.github` e o arquivo `gradlew`. Se o Chrome não mostrar pasta oculta, no Arquivos liga "mostrar arquivos ocultos" antes de selecionar.
5. Commit: `primeiro commit`.
6. Abre a aba **Actions**. Se pedir, clica em **I understand my workflows, go ahead and enable them**.
7. O workflow **Build APK** dispara sozinho no push. Espera ficar verde (uns 5–10 min na primeira vez).
8. Abre o run verde → embaixo, **Artifacts** → `grokboard-debug`. Baixa o zip, abre, instala o `.apk`.
9. Se o Android bloquear: Ajustes → Segurança → instalar apps desconhecidos → Chrome (ou Arquivos) → permitir.

Pra gerar de novo depois de mudar o código: sobe os arquivos de novo (ou edita no site) e o Actions roda outra vez. Também dá pra ir em Actions → Build APK → **Run workflow**.

## No S22, depois de instalar

1. Abre o app GrokBoard.
2. **Ativar teclado** e liga o GrokBoard.
3. Troca o teclado do sistema para GrokBoard.
4. **Configurar Grok** e cola a key `xai-...` de [console.x.ai](https://console.x.ai).
5. No teclado, toca o chip da persona para ciclar.
6. Opcional, pra ler o post no X: Ajustes → Acessibilidade → GrokBoard.

Modelo padrão: `grok-4-fast`. Troca nas configs se a console mostrar outro id.

A key fica só no aparelho. Não publica esse app na Play Store assim.
