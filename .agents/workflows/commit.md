---
description: commit+sync+git
---

Commit com frase descritiva em Português Brasileiro e Sync com o repositório remoto. Sempre gerar relatório do commit e do sync com a identificação do commit (ex: Commit 83a74f7).

### Regra Obrigatória para a Pasta `docs/`
1. **Dados Mascarados:** A pasta `docs/` só pode conter dados mascarados. Qualquer menção a e-mails (apenas as 3 primeiras letras visíveis, ex.: `sou***@g***.com`, `cyb***@h***.com`), telefones (apenas os 3 últimos números visíveis, ex.: `[OCULTO]433`), senhas, tokens ou endereços reais deve ser estritamente mascarada com `[OCULTO]`.
2. **Proibição de Exportações e Dumps:** É TERMINANTEMENTE PROIBIDO commitar exportações de banco de dados (CSV, JSON, dumps) ou qualquer arquivo contendo dados pessoais de usuários.
3. **Varredura Obrigatória de `docs/` Pré-Commit:**
   Antes de todo commit, o DevOps deve obrigatoriamente executar as seguintes varreduras na pasta `docs/` e explicar cada ocorrência no relatório:
   ```bash
   # 1. E-mails não mascarados em docs/
   grep -rnE "[A-Za-z0-9._%+-]{2,}@[A-Za-z0-9.-]+\.[A-Za-z]{2,}" docs | grep -v "\*\*\*"

   # 2. Telefones em docs/ (sequências de 8 a 11 dígitos)
   grep -rnE "\b[0-9]{8,11}\b" docs

   # 3. Menções a senha ou password em docs/
   grep -rn -iE "(senha|password)" docs

   # 4. Tokens JWT em docs/
   grep -rn "eyJ" docs

   # 5. URLs do Supabase em docs/
   grep -rn "supabase\.co" docs
   ```

### Varreduras de Integridade Obrigatórias (Código e Testes)
As três varreduras de integridade devem usar EXATAMENTE os comandos definidos em `.agents/rules/integridade-de-testes.md` (se algum for alterado, justifique por escrito e mostre a saída da versão original também):
```bash
# 1. Stubs, mocks e pendências em código de produção (cada ocorrência deve ser explicada)
grep -rniE "mock|stub|fake|homologa|TODO|FIXME|em breve" app/src/main/java

# 2. Testes desativados ou vazios
grep -rnE "@Ignore|assertTrue\(true\)|assertEquals\(([0-9]+), *\1\)" app/src/test app/src/androidTest

# 3. Segredos (qualquer resultado bloqueia o commit)
grep -rniE "(api[_-]?key|secret|password|service_role|BEGIN (RSA|PRIVATE))" --include=*.kt --include=*.kts --include=*.xml --include=*.json --include=*.properties . | grep -v ".env.example"

# 4. Suíte completa e lint, sem filtros
./gradlew testDebugUnitTest lintDebug
```
