# Relatório de Execução da Tarefa T-13 [INT-02]

**Data:** 27 de Outubro de 2026  
**Tarefa:** T-13 [INT-02] - Sincronização de Timeouts e Resiliência da Overpass API  
**Risco:** Médio  
**Responsáveis:** Orquestrador, Integrações, Backend, Frontend/UX, QA e DevOps  

---

## 1. Resultado

🟡 **IMPLEMENTADO, NÃO VERIFICADO NO APARELHO**  
(Os timeouts do cliente OkHttp foram alinhados para 25 segundos, o cabeçalho obrigatório `Accept: application/json` foi implementado, os espelhos de servidores foram atualizados para instâncias ativas do OpenStreetMap com remoção do endpoint obsoleto, a mensagem de erro foi traduzida para linguagem amigável em português e a suíte completa de testes automatizados com mutação manual foi aprovada com 100% de sucesso. A validação prática no dispositivo físico depende do teste pelo usuário).

---

## 2. O que mudou

### Explicação Leiga
Ao testar a busca de postos por GPS no celular físico após a conclusão da T-11, o aplicativo apresentou na tela a mensagem vermelha:
`"Não foi possível conectar: failed to connect to overpass.openstreetmap.ru/185.110.56.32 (port 443) from /192.168.2.7 (port 40214) after 8000ms"`.

A investigação técnica identificou com exatidão a causa raiz:
1. **Timeout muito curto (8 segundos vs 25 segundos):** A consulta enviada pelo app ao servidor Overpass pede `[timeout:25]` (25 segundos de tolerância de cálculo), porém o cliente de rede do aplicativo (`OkHttpClient`) estava configurado para cortar a conexão após apenas 8 segundos (`8000ms`). Em conexões móveis ou quando o servidor levava 9 ou 10 segundos, o app cancelava tudo prematuramente.
2. **Falta de Cabeçalho de Formato:** O servidor principal (`overpass-api.de`) retornava erro `406 Not Acceptable` caso a requisição não declarasse explicitamente que aceitava JSON (`Accept: application/json`).
3. **Servidor Morto/Bloqueado na Lista de Espelhos:** O aplicativo possuía uma lista com 3 servidores. O terceiro (`overpass.openstreetmap.ru`) está inativo/bloqueado na internet global (porta 443 inacessível), e o segundo (`overpass.kumi.systems`) foi descontinuado pela comunidade OSM e migrado para `overpass.private.coffee`. Como o aplicativo tentava os 3 e o último a falhar era o russo inativo, o erro exibido na tela era a falha de conexão do servidor morto.
4. **Mensagens Cruas de Erro com Termos Técnicos:** Quando ocorria qualquer instabilidade de conexão, o app despejava termos em inglês como "failed to connect", IP e porta direto na interface do motorista.

**O que foi corrigido:**
- Os timeouts de conexão e leitura foram aumentados para 15 segundos e 25 segundos, garantindo tempo adequado para o servidor mapear os postos no raio de 3 km.
- O cabeçalho `Accept: application/json` foi adicionado a todas as requisições.
- A lista de espelhos públicos foi atualizada conforme a documentação oficial atual do OpenStreetMap: o servidor principal (`overpass-api.de`) e o espelho ativo de alta capacidade (`overpass.private.coffee`), eliminando o endpoint morto.
- A mensagem de falha foi traduzida para português amigável e direto: *"Não foi possível buscar postos próximos no momento. Verifique sua conexão com a internet e tente novamente."*, sem vazar detalhes técnicos de sockets, portas ou IPs.

### Lista de Arquivos Afetados
- `app/src/main/java/com/example/network/OverpassApiService.kt`: Timeouts de conexão e leitura configurados para 15s e 25s, e adição do cabeçalho `Accept: application/json` no interceptor do OkHttp.
- `app/src/main/java/com/example/network/GasStationRepository.kt`: Atualização de `OVERPASS_ENDPOINTS` com instâncias globais ativas do OSM, propriedades de injeção limpa para testes com `MockWebServer` e tratamento amigável de erro em português.
- `app/src/test/java/com/example/network/OverpassApiResilienceTest.kt`: Nova suíte de testes automatizados com `MockWebServer` cobrindo timeout, diretiva de 25s na query, validação de espelhos ativos, fallback gracioso entre servidores e mensagens amigáveis sem vazamento de dados técnicos.
- `docs/plano-onda-2.md`: Tabela de acompanhamento da Onda 2 atualizada com o status da Tarefa T-13.

---

## 3. Evidências

### Execução dos Testes Automatizados e Análise de Lint
```bash
./gradlew testDebugUnitTest lintDebug
```
**Resultado da Execução:**
- Total de testes executados: **270**
- Aprovados: **270 (100%)**
- Falhas: **0**
- Ignorados: **0**
- Caminho do relatório de testes: `app/build/reports/tests/testDebugUnitTest/index.html`
- Caminho do relatório do Lint: `app/build/reports/lint-results-debug.html`

**Últimas linhas da saída do Gradle:**
```text
> Task :app:compileDebugUnitTestKotlin UP-TO-DATE
> Task :app:compileDebugUnitTestJavaWithJavac NO-SOURCE
> Task :app:processDebugUnitTestJavaRes UP-TO-DATE
> Task :app:testDebugUnitTest
> Task :app:finalizeTestRoborazziDebug SKIPPED
> Task :app:lintReportDebug UP-TO-DATE
> Task :app:lintDebug

BUILD SUCCESSFUL in 1m 42s
31 actionable tasks: 7 executed, 24 up-to-date
Configuration cache entry reused.
```

### Compilação do APK Release com R8 e Minificação
```bash
./gradlew assembleReleaseCheck
```
**Resultado da Execução:**
- Minificação com R8: executada e concluída sem erros ou avisos não tratados.
- Status: **BUILD SUCCESSFUL in 7m 43s** (50 actionable tasks: 10 executed, 40 up-to-date).

### Verificação por Mutação Manual (QA)
- **Mutação introduzida:** No arquivo `GasStationRepository.kt`, o loop de fallback entre espelhos foi limitado arbitrariamente a `for (endpoint in endpoints.take(1))`, simulando o cenário em que o sistema desiste após a falha do primeiro servidor sem tentar os espelhos alternativos.
- **Comando executado:** `./gradlew testDebugUnitTest --tests "com.example.network.OverpassApiResilienceTest"`
- **Resultado:** O teste `testOverpassApi_firstEndpointFails_fallbackToSecondEndpointSucceeds` **FALHOU** imediatamente acusando `java.lang.AssertionError at OverpassApiResilienceTest.kt:76` (esperado: `GasStationSearchResult.Success` pelo segundo espelho; obtido: `GasStationSearchResult.Error`).
- **Restauração:** A mutação foi desfeita, restaurando o loop completo de resiliência, e a suíte voltou a passar com 100% de sucesso (270 testes).

---

## 4. Testes

### Testes Adicionados
- `com.example.network.OverpassApiResilienceTest`:
  1. `testOverpassQuery_specifies25SecondsTimeoutDirective`: valida que a query do Overpass declara `[timeout:25]` e o filtro correto de postos de combustível.
  2. `testOverpassEndpoints_doesNotContainDeadRussianEndpointAndHasActiveMirrors`: valida que o endpoint inativo `openstreetmap.ru` e o legado `kumi.systems` foram eliminados, e que `overpass-api.de` e `private.coffee` estão configurados.
  3. `testOverpassApi_firstEndpointFails_fallbackToSecondEndpointSucceeds`: simula com `MockWebServer` um cenário em que o servidor 1 responde com erro 504 Gateway Timeout e o servidor 2 responde com sucesso, validando a comutação automática e transparente.
  4. `testOverpassApi_allEndpointsFail_returnsFriendlyPortugueseErrorMessageWithoutLeaks`: valida que, se todos os espelhos falharem, o app retorna mensagem amigável em português e nunca expõe endereços IP, números de portas ou textos crus como "failed to connect".

### Testes Alterados
- Nenhum teste existente foi alterado ou enfraquecido.

### Testes Removidos
- Nenhum teste foi removido.

---

## 5. O que NÃO foi feito ou está simulado

- Nada está simulado. As chamadas aos servidores públicos do OpenStreetMap e o fallback entre espelhos operam com código de produção real.
- Em ambientes de teste automatizado, as conexões são interceptadas com fidelidade por `MockWebServer` para garantir determinismo e isolamento de rede.

---

## 6. Riscos e dívidas encontrados

- **Disponibilidade de Servidores Públicos Overpass:** Os servidores comunitários do OpenStreetMap são gratuitos e mantidos por doações. Em horários de pico, podem apresentar lentidão temporária. A introdução do timeout de 25s, o cabeçalho JSON e a comutação entre espelhos independentes mitiga essa oscilação sem custos para o motorista.

---

## 7. O que o usuário precisa fazer (Roteiro de Teste Manual no Aparelho)

1. **Instalar a versão atualizada no celular:**
   ```bash
   ./gradlew installDebug
   ```
   *(Ou instale o APK `app-releaseCheck.apk` gerado em `app/build/outputs/apk/releaseCheck/`)*.

2. **Abrir a tela de Abastecimento:**
   - Abra o app **GiroCusto**.
   - Toque em **Abastecimento** (ou botão "+" para novo abastecimento).

3. **Buscar postos próximos pelo GPS:**
   - Ao lado do campo de "Posto de Combustível", toque no botão roxo/azul com ícone de localização (**"Buscar posto próximo"**).
   - O app fará a busca utilizando o timeout sincronizado de até 25 segundos no servidor oficial do OpenStreetMap com cabeçalho JSON.

4. **Verificar o resultado:**
   - **O que deve acontecer:**
     - Uma lista de postos de combustível mais próximos à sua localização atual deve se abrir logo abaixo, mostrando os nomes dos postos (ex.: Shell, Ipiranga, Petrobras, etc.), as bandeiras, os endereços e a distância exata em metros ou quilômetros!
     - Ao tocar em qualquer posto da lista, o nome e a bandeira são preenchidos automaticamente no formulário!
     - Se o celular estiver sem internet ou com sinal muito fraco, a mensagem de erro agora será limpa e educada em português (*"Não foi possível buscar postos próximos no momento..."*), sem exibir mais IPs, portas ou textos em inglês.

---

## 8. Commit e sincronização

- **Hash do commit:** `f863ca3` (`fix(network): alinha timeouts para 25s, adiciona Accept json e espelhos ativos no Overpass (T-13)`)
- **Status do push:** Sincronizado com `origin/main`
