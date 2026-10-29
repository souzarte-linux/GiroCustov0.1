# Formalização de Schemas Room e Histórico de Migrações do GiroCusto

**Documento:** `docs/database-schemas.md`  
**Projeto:** GiroCusto (Android Nativo - Kotlin, Jetpack Compose, Room, Supabase)  
**Pacote:** `com.example.data`  
**Classe de Banco:** `GiroCustoDatabase` (`giro_custo_database`)  
**Versão Atual:** **15**  
**Fase do Projeto:** **Pré-lançamento**  
**Tarefa Relacionada:** T-19 [DAT-02]  

---

## 1. Visão Geral e Governança de Banco de Dados

O aplicativo **GiroCusto** utiliza o **Android Jetpack Room** como camada de persistência local SQLite, operando sob o paradigma *offline-first*. Todas as entidades do aplicativo são salvas localmente no aparelho e sincronizadas com a nuvem (Supabase) através de rotinas de sincronização bidirecional com resolução de conflitos *Last-Write-Wins* (LWW) e isolamento estrito de dados por conta (*DataOwnerStorage*).

Como o aplicativo é voltado para entregadores e motoristas de aplicativo (iFood, Uber, 99, etc.), a integridade dos dados armazenados é crítica: turnos trabalhados, abastecimentos, manutenções, peças e metas financeiras representam o ganho e o patrimônio do profissional. Portanto, qualquer alteração estrutural no banco de dados deve seguir regras rigorosas de governança de esquema e migração.

---

## 2. Regras Inegociáveis de Arquitetura de Dados

### 2.1 `exportSchema = true` é Obrigatório e Permanente
- **Configuração:** O parâmetro `exportSchema = true` está definido na anotação `@Database` em [GiroCustoDatabase.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/GiroCustoDatabase.kt#L14):
  ```kotlin
  @Database(
      entities = [
          Vehicle::class,
          VehiclePart::class,
          DailyRecord::class,
          UserProfile::class,
          Platform::class,
          FuelRefill::class,
          MaintenanceRecord::class,
          UserGoals::class
      ],
      version = 15,
      exportSchema = true
  )
  abstract class GiroCustoDatabase : RoomDatabase()
  ```
- **Diretório de Exportação:** Configurado no `app/build.gradle.kts` via KSP:
  ```kotlin
  ksp {
      arg("room.schemaLocation", "${projectDir}/schemas")
  }
  ```
  E exposto aos conjuntos de teste e debug:
  ```kotlin
  sourceSets {
      getByName("debug").assets.srcDirs(files("$projectDir/schemas"))
      getByName("test").assets.srcDirs(files("$projectDir/schemas"))
  }
  ```
- **Justificativa Técnica:** A exportação do esquema JSON em tempo de compilação permite o rastreamento cirúrgico de alterações no Git, auditoria de integridade do modelo de dados e execução do `MigrationTestHelper` em testes unitários para validar se o SQLite real reflete com exatidão a declaração do Room.
- **Regra:** É **terminantemente proibido** alterar `exportSchema` para `false` ou remover os argumentos do KSP.

### 2.2 `fallbackToDestructiveMigration()` é ESTRITAMENTE PROIBIDO
- **Justificativa Técnica:** Métodos de migração destrutiva (`fallbackToDestructiveMigration()` ou `fallbackToDestructiveMigrationOnDowngrade()`) apagam silenciosamente todas as tabelas do banco de dados quando uma versão é incrementada sem migração correspondente. No GiroCusto, isso causaria a perda definitiva do histórico de faturamento, despesas e manutenções do trabalhador autônomo, além de quebrar o estado de sincronização local (*dirty records* e *tombstones*).
- **Regra:** Sob nenhuma circunstância deve-se invocar `fallbackToDestructiveMigration` na instanciação do `Room.databaseBuilder`. Toda e qualquer alteração de esquema DEVE possuir uma classe `Migration` correspondente registrada em `addMigrations(...)`.

### 2.3 Regra de Versionamento Sequencial Estrito
- A versão atual do banco é **15**.
- Qualquer alteração futura de esquema DEVE incrementar a versão para exatamente **16** (`Migration(15, 16)`).
- É **proibido**:
  1. Pular números de versão (ex: migrar diretamente de 15 para 17);
  2. Duplicar ou reutilizar números de versão já existentes;
  3. Modificar retroativamente migrations já homologadas em versões anteriores.

### 2.4 Fase Atual: Pré-lançamento
O projeto encontra-se atualmente em fase de **pré-lançamento**, sem usuários externos em ambiente público de produção (Google Play Store).
- A migração `MIGRATION_14_15` (conversão monetária de `Double` para inteiros em centavos) foi implementada de forma simplificada em ambiente pré-lançamento.
- **Atenção:** Padrões simplificados de migração adotados no pré-lançamento **NÃO devem ser replicados após o lançamento público**. A partir do lançamento, qualquer migração deverá prever dados legados em produção com testes de volume e resiliência a dados corrompidos.

---

## 3. Inventário e Status dos Schemas JSON no Repositório

Os schemas exportados pelo Room residem no diretório:  
`app/schemas/com.example.data.GiroCustoDatabase/`

### 3.1 Tabela Comparativa de Schemas (v1 a v15)

| Versão | Arquivo JSON no Git | Status | Identificador / Hash Room | Resumo da Alteração | Tarefa / Referência |
| :---: | :---: | :---: | :---: | :--- | :--- |
| **v1** | *(sem arquivo)* | **Legado** | N/A | Criação inicial do banco: `vehicle`, `vehicle_parts`, `daily_records`. | Legado inicial |
| **v2** | `2.json` | ✅ Presente | `27635c1306c698b18210c9e0f5993d0f` | Adicionada coluna `platform` em `daily_records`. | `MIGRATION_1_2` |
| **v3** | `3.json` | ✅ Presente | `693eb4a682da0a7751939103c80e1ec8` | Criação da tabela `user_profile`. | `MIGRATION_2_3` |
| **v4** | *(sem arquivo)* | **Legado** | N/A | Criação da tabela `platforms`. | `MIGRATION_3_4` |
| **v5** | *(sem arquivo)* | **Legado** | N/A | Limpeza em `user_profile` removendo a coluna `platforms`. | `MIGRATION_4_5` |
| **v6** | *(sem arquivo)* | **Legado** | N/A | Adicionada coluna `active` em `vehicle`. | `MIGRATION_5_6` |
| **v7** | *(sem arquivo)* | **Legado** | N/A | Criação da tabela `fuel_refills`. | `MIGRATION_6_7` |
| **v8** | `8.json` | ✅ Presente | `b21404e9a8f4c1ebc80650d3060fc184` | Criação da tabela `maintenance_records`. | `MIGRATION_7_8` |
| **v9** | `9.json` | ✅ Presente | `45239a04f2d7a965709971bc391f63cb` | Adicionados campos de horário (`startTime`, `endTime`, `pauseDuration`) em `daily_records`. | `MIGRATION_8_9` |
| **v10** | `10.json` | ✅ Presente | `db5f93630f9a2d8cb30f89fa6e9fb509` | Adicionados campos de sincronização em nuvem (`remoteId`, `syncStatus`, `updatedAt`, `isDeleted`) nas 7 tabelas. | `MIGRATION_9_10` |
| **v11** | `11.json` | ✅ Presente | `e18ffbc72e0a2933f86bdf5359a0f4ae` | Expansão cadastral: endereço em `user_profile` e características em `vehicle`. | `MIGRATION_10_11` |
| **v12** | `12.json` | ✅ Presente | `bc7f7f90fbbd0979bfbfa716ea08aa66` | Adicionada coluna `vehicleId` em `vehicle_parts`. | `MIGRATION_11_12` |
| **v13** | `13.json` | ✅ Presente | `b0c793ff0285a8dae7e0e7a8e7a030cb` | Criação de 20 índices para performance nas consultas dos DAOs e rotinas de sync. | T-08 [PERF-01] |
| **v14** | `14.json` | ✅ Presente | `5326fc549302e3b2e5572ea03d656dc4` | Criação da 8ª entidade `user_goals` (metas sincronizadas na nuvem). | T-28B [DAT-03] |
| **v15** | `15.json` | ✅ Presente | `4045f284c7eb68b394142f9b8849646a` | Migração monetária para centavos (`Long` / `INTEGER`) em 19 campos monetários. | T-18B [DAT-01B] |

### 3.2 Esclarecimento sobre Schemas Legados Faltantes
Os arquivos `1.json`, `4.json`, `5.json`, `6.json` e `7.json` não foram exportados para o controle de versão Git durante a fase de prototipagem rápida inicial do projeto.
- **Decisão Arquitetural Homologada:** Conforme especificado no Plano da Onda 2 (Tarefa T-19), **é estritamente proibida a criação de schemas sintéticos retroativos**. A integridade de migração para instalações históricas está integralmente garantida pelo código executável das migrations `MIGRATION_1_2` até `MIGRATION_7_8` em [GiroCustoDatabase.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/GiroCustoDatabase.kt).

---

## 4. Evolução Histórica Detalhada das Versões (v1 → v15)

### Versão 1 (Legado Inicial)
- **Objetivo:** Primeira estrutura operacional do aplicativo.
- **Tabelas:**
  - `vehicle`: dados do veículo (modelo, consumo médio, tipo de combustível, custos fixos, dias de trabalho, hodômetro atual).
  - `vehicle_parts`: peças e desgaste (nome, preço, vida útil em km, km rodados desde a troca).
  - `daily_records`: lançamentos da jornada (data, faturamento bruto, entregas, hodômetro inicial/final, preço do combustível, alimentação, custos calculados e lucro líquido).

---

### Versão 2 (`MIGRATION_1_2`)
- **Schema:** `2.json`
- **Objetivo:** Permitir segmentação dos lançamentos por plataforma de trabalho (iFood, Uber, etc.).
- **DDL:**
  ```sql
  ALTER TABLE daily_records ADD COLUMN platform TEXT NOT NULL DEFAULT 'Geral';
  ```

---

### Versão 3 (`MIGRATION_2_3`)
- **Schema:** `3.json`
- **Objetivo:** Adicionar perfil básico do usuário motorista/entregador.
- **DDL:**
  ```sql
  CREATE TABLE IF NOT EXISTS `user_profile` (
      `id` INTEGER NOT NULL,
      `name` TEXT NOT NULL,
      `phone` TEXT NOT NULL,
      `city` TEXT NOT NULL,
      `platforms` TEXT NOT NULL,
      PRIMARY KEY(`id`)
  );
  ```

---

### Versão 4 (`MIGRATION_3_4`)
- **Schema:** Legado (sem arquivo)
- **Objetivo:** Estruturar plataformas de trabalho como entidade dedicada com modelos de pagamento, ciclo e dados bancários/PIX para recebimento.
- **DDL:**
  ```sql
  CREATE TABLE IF NOT EXISTS `platforms` (
      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      `name` TEXT NOT NULL,
      `segment` TEXT NOT NULL,
      `paymentModel` TEXT NOT NULL,
      `cycle` TEXT NOT NULL,
      `paymentDay` TEXT NOT NULL,
      `fixedPayDelay` INTEGER NOT NULL,
      `cycleEntriesJson` TEXT NOT NULL,
      `bankName` TEXT NOT NULL,
      `bankAgency` TEXT NOT NULL,
      `bankAccount` TEXT NOT NULL,
      `pixKeyType` TEXT NOT NULL,
      `pixKey` TEXT NOT NULL,
      `active` INTEGER NOT NULL
  );
  ```

---

### Versão 5 (`MIGRATION_4_5`)
- **Schema:** Legado (sem arquivo)
- **Objetivo:** Normalização da tabela `user_profile`, removendo a coluna desnormalizada `platforms` que passou a residir na tabela `platforms`.
- **Estratégia SQLite (Table Rebuild):**
  ```sql
  CREATE TABLE IF NOT EXISTS `user_profile_new` (
      `id` INTEGER NOT NULL,
      `name` TEXT NOT NULL,
      `phone` TEXT NOT NULL,
      `city` TEXT NOT NULL,
      PRIMARY KEY(`id`)
  );
  INSERT INTO `user_profile_new` (`id`, `name`, `phone`, `city`)
      SELECT `id`, `name`, `phone`, `city` FROM `user_profile`;
  DROP TABLE `user_profile`;
  ALTER TABLE `user_profile_new` RENAME TO `user_profile`;
  ```

---

### Versão 6 (`MIGRATION_5_6`)
- **Schema:** Legado (sem arquivo)
- **Objetivo:** Permitir ativação/desativação de veículos para suporte a múltiplos veículos mantendo apenas um ativo por vez.
- **DDL:**
  ```sql
  ALTER TABLE vehicle ADD COLUMN active INTEGER NOT NULL DEFAULT 1;
  ```

---

### Versão 7 (`MIGRATION_6_7`)
- **Schema:** Legado (sem arquivo)
- **Objetivo:** Adicionar módulo de abastecimentos de combustível com cálculo de consumo real e histórico de preços.
- **DDL:**
  ```sql
  CREATE TABLE IF NOT EXISTS `fuel_refills` (
      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      `vehicleId` INTEGER NOT NULL,
      `dateTimestamp` INTEGER NOT NULL,
      `dateString` TEXT NOT NULL,
      `gasStation` TEXT NOT NULL,
      `fuelType` TEXT NOT NULL,
      `pricePerLiter` REAL NOT NULL,
      `liters` REAL NOT NULL,
      `discount` REAL NOT NULL,
      `totalPaid` REAL NOT NULL,
      `odometer` REAL NOT NULL,
      `isFullTank` INTEGER NOT NULL,
      `paymentMethod` TEXT NOT NULL,
      `isInstallment` INTEGER NOT NULL,
      `installmentsCount` INTEGER NOT NULL
  );
  ```

---

### Versão 8 (`MIGRATION_7_8`)
- **Schema:** `8.json`
- **Objetivo:** Adicionar módulo de registros de manutenção e revisões de veículos.
- **DDL:**
  ```sql
  CREATE TABLE IF NOT EXISTS `maintenance_records` (
      `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
      `vehicleId` INTEGER NOT NULL,
      `dateTimestamp` INTEGER NOT NULL,
      `dateString` TEXT NOT NULL,
      `description` TEXT NOT NULL,
      `location` TEXT NOT NULL,
      `value` REAL NOT NULL,
      `odometer` REAL NOT NULL
  );
  ```

---

### Versão 9 (`MIGRATION_8_9`)
- **Schema:** `9.json`
- **Objetivo:** Adicionar horários de turno de trabalho e pausas na jornada diária para apuração de faturamento por hora.
- **DDL:**
  ```sql
  ALTER TABLE daily_records ADD COLUMN startTime TEXT NOT NULL DEFAULT '';
  ALTER TABLE daily_records ADD COLUMN endTime TEXT NOT NULL DEFAULT '';
  ALTER TABLE daily_records ADD COLUMN pauseDuration TEXT NOT NULL DEFAULT '';
  ```

---

### Versão 10 (`MIGRATION_9_10`)
- **Schema:** `10.json`
- **Objetivo:** Preparação estrutural para arquitetura *offline-first* com sincronização remota (Supabase). Adição uniforme de 4 colunas em todas as 7 tabelas do aplicativo:
  - `remoteId TEXT NOT NULL DEFAULT ''`
  - `syncStatus TEXT NOT NULL DEFAULT 'SYNCED'`
  - `updatedAt INTEGER NOT NULL DEFAULT 0`
  - `isDeleted INTEGER NOT NULL DEFAULT 0`
- **Tabelas Atualizadas:** `vehicle`, `vehicle_parts`, `daily_records`, `user_profile`, `platforms`, `fuel_refills`, `maintenance_records`.

---

### Versão 11 (`MIGRATION_10_11`)
- **Schema:** `11.json`
- **Objetivo:** Expansão de dados de perfil do motorista (endereço completo e onboarding) e especificações técnicas de veículos para frotas de motos e carros.
- **DDL:**
  ```sql
  -- user_profile
  ALTER TABLE `user_profile` ADD COLUMN `email` TEXT NOT NULL DEFAULT '';
  ALTER TABLE `user_profile` ADD COLUMN `isWhatsapp` INTEGER NOT NULL DEFAULT 1;
  ALTER TABLE `user_profile` ADD COLUMN `cep` TEXT NOT NULL DEFAULT '';
  ALTER TABLE `user_profile` ADD COLUMN `street` TEXT NOT NULL DEFAULT '';
  ALTER TABLE `user_profile` ADD COLUMN `addressNumber` TEXT NOT NULL DEFAULT '';
  ALTER TABLE `user_profile` ADD COLUMN `neighborhood` TEXT NOT NULL DEFAULT '';
  ALTER TABLE `user_profile` ADD COLUMN `stateUf` TEXT NOT NULL DEFAULT '';
  ALTER TABLE `user_profile` ADD COLUMN `onboardingCompleted` INTEGER NOT NULL DEFAULT 0;

  -- vehicle
  ALTER TABLE `vehicle` ADD COLUMN `vehicleType` TEXT NOT NULL DEFAULT 'MOTO';
  ALTER TABLE `vehicle` ADD COLUMN `brand` TEXT NOT NULL DEFAULT '';
  ALTER TABLE `vehicle` ADD COLUMN `manufacturingYear` TEXT NOT NULL DEFAULT '';
  ALTER TABLE `vehicle` ADD COLUMN `tankCapacity` REAL NOT NULL DEFAULT 16.0;
  ALTER TABLE `vehicle` ADD COLUMN `hasTopBox` INTEGER NOT NULL DEFAULT 0;
  ALTER TABLE `vehicle` ADD COLUMN `hasDeliveryBag` INTEGER NOT NULL DEFAULT 0;
  ALTER TABLE `vehicle` ADD COLUMN `carBodyType` TEXT NOT NULL DEFAULT '';
  ```

---

### Versão 12 (`MIGRATION_11_12`)
- **Schema:** `12.json`
- **Objetivo:** Associar peças de reposição diretamente a um veículo específico via chave estrangeira lógica (`vehicleId`).
- **DDL:**
  ```sql
  ALTER TABLE `vehicle_parts` ADD COLUMN `vehicleId` INTEGER NOT NULL DEFAULT 0;
  UPDATE `vehicle_parts`
     SET `vehicleId` = (SELECT id FROM vehicle WHERE active = 1 LIMIT 1)
   WHERE `vehicleId` = 0
     AND (SELECT id FROM vehicle WHERE active = 1 LIMIT 1) IS NOT NULL;
  ```

---

### Versão 13 (`MIGRATION_12_13` - Tarefa T-08 [PERF-01])
- **Schema:** `13.json`
- **Objetivo:** Eliminar varreduras completas de tabela (*full table scans*) em consultas frequentes dos DAOs e nas rotinas de sincronização offline, criando **20 índices** nas 7 entidades.
- **Índices Criados:**
  1. `vehicle`:
     - `index_vehicle_active_isDeleted` em `(active, isDeleted)`
     - `index_vehicle_remoteId` em `(remoteId)`
     - `index_vehicle_syncStatus` em `(syncStatus)`
  2. `vehicle_parts`:
     - `index_vehicle_parts_vehicleId` em `(vehicleId)`
     - `index_vehicle_parts_remoteId` em `(remoteId)`
     - `index_vehicle_parts_syncStatus` em `(syncStatus)`
  3. `daily_records`:
     - `index_daily_records_isDeleted_dateTimestamp` em `(isDeleted, dateTimestamp)`
     - `index_daily_records_remoteId` em `(remoteId)`
     - `index_daily_records_syncStatus` em `(syncStatus)`
  4. `user_profile`:
     - `index_user_profile_remoteId` em `(remoteId)`
     - `index_user_profile_syncStatus` em `(syncStatus)`
  5. `platforms`:
     - `index_platforms_remoteId` em `(remoteId)`
     - `index_platforms_syncStatus` em `(syncStatus)`
  6. `fuel_refills`:
     - `index_fuel_refills_vehicleId_dateTimestamp` em `(vehicleId, dateTimestamp)`
     - `index_fuel_refills_vehicleId_odometer` em `(vehicleId, odometer)`
     - `index_fuel_refills_remoteId` em `(remoteId)`
     - `index_fuel_refills_syncStatus` em `(syncStatus)`
  7. `maintenance_records`:
     - `index_maintenance_records_vehicleId_dateTimestamp` em `(vehicleId, dateTimestamp)`
     - `index_maintenance_records_remoteId` em `(remoteId)`
     - `index_maintenance_records_syncStatus` em `(syncStatus)`

---

### Versão 14 (`MIGRATION_13_14` - Tarefa T-28B [DAT-03])
- **Schema:** `14.json`
- **Objetivo:** Adicionar persistência local e sincronização na nuvem para as metas de trabalho do motorista (faturamento bruto, líquido, km e entregas em horizontes diário, semanal e mensal), além de desacoplar o tema visual para SharedPreferences gerais.
- **Nova Tabela e Índices:**
  ```sql
  CREATE TABLE IF NOT EXISTS `user_goals` (
      `id` INTEGER NOT NULL,
      `dailyGoalGross` REAL NOT NULL,
      `dailyGoalNet` REAL NOT NULL,
      `dailyGoalKm` REAL NOT NULL,
      `dailyGoalDeliveries` INTEGER NOT NULL,
      `weeklyGoalGross` REAL NOT NULL,
      `weeklyGoalNet` REAL NOT NULL,
      `weeklyGoalKm` REAL NOT NULL,
      `weeklyGoalDeliveries` INTEGER NOT NULL,
      `monthlyGoalGross` REAL NOT NULL,
      `monthlyGoalNet` REAL NOT NULL,
      `monthlyGoalKm` REAL NOT NULL,
      `monthlyGoalDeliveries` INTEGER NOT NULL,
      `remoteId` TEXT NOT NULL DEFAULT '',
      `syncStatus` TEXT NOT NULL DEFAULT 'SYNCED',
      `updatedAt` INTEGER NOT NULL DEFAULT 0,
      `isDeleted` INTEGER NOT NULL DEFAULT 0,
      PRIMARY KEY(`id`)
  );
  CREATE INDEX IF NOT EXISTS `index_user_goals_remoteId` ON `user_goals` (`remoteId`);
  CREATE INDEX IF NOT EXISTS `index_user_goals_syncStatus` ON `user_goals` (`syncStatus`);
  ```

---

### Versão 15 (`MIGRATION_14_15` - Tarefa T-18B [DAT-01B])
- **Schema:** `15.json`
- **Objetivo:** Eliminar erros de ponto flutuante binário (IEEE 754) em operações financeiras e consolidações de faturamento/lucro, convertendo **19 campos monetários** em 6 entidades de `Double` (`REAL`) para inteiros em centavos `Long` (`INTEGER`) e milésimos para combustíveis.
- **Campos Convertidos:**
  - `vehicle`: `monthlyFixedCosts` (* 100)
  - `vehicle_parts`: `price` (* 100)
  - `daily_records`: `grossEarnings` (* 100), `fuelPrice` (* 1000), `foodExpense` (* 100), `fuelCost` (* 100), `wearCost` (* 100), `proportionalFixedCost` (* 100), `netProfit` (* 100)
  - `fuel_refills`: `pricePerLiter` (* 1000), `discount` (* 100), `totalPaid` (* 100)
  - `maintenance_records`: `value` (* 100)
  - `user_goals`: `dailyGoalGross` (* 100), `dailyGoalNet` (* 100), `weeklyGoalGross` (* 100), `weeklyGoalNet` (* 100), `monthlyGoalGross` (* 100), `monthlyGoalNet` (* 100)
- **Estratégia SQLite:** Recriação segura de tabelas temporárias (`<table>_new`) com cópia convertida via `CAST(ROUND(campo * escala) AS INTEGER)`, `DROP` da tabela original, `RENAME` da temporária e recriação integral de todos os índices.

---

## 5. Entidades e Estrutura Atual do Banco de Dados (v15)

O banco de dados do GiroCusto possui atualmente **8 entidades Room**, totalizando **22 índices**:

| # | Entidade Kotlin | Nome da Tabela | Chave Primária | Índices Criados | Descrição |
| :-: | :--- | :--- | :--- | :-: | :--- |
| 1 | `Vehicle` | `vehicle` | `id` (Auto-inc) | 3 | Veículos cadastrados e configurações de custos fixos. |
| 2 | `VehiclePart` | `vehicle_parts` | `id` (Auto-inc) | 3 | Peças de reposição e monitoramento de desgaste. |
| 3 | `DailyRecord` | `daily_records` | `id` (Auto-inc) | 3 | Registros de jornada, faturamento, despesas e horários. |
| 4 | `UserProfile` | `user_profile` | `id` (Singleton `1`) | 2 | Dados do motorista, contato e endereço. |
| 5 | `Platform` | `platforms` | `id` (Auto-inc) | 2 | Plataformas de trabalho, ciclos e dados bancários. |
| 6 | `FuelRefill` | `fuel_refills` | `id` (Auto-inc) | 4 | Registros de abastecimentos e cálculo de consumo. |
| 7 | `MaintenanceRecord` | `maintenance_records` | `id` (Auto-inc) | 3 | Registros de revisões e manutenções mecânicas. |
| 8 | `UserGoals` | `user_goals` | `id` (Singleton `1`) | 2 | Metas de faturamento, km e entregas sincronizadas. |

### Padrão Comum de Sincronização em Nuvem (Offline-First)
Todas as 8 tabelas contêm compulsoriamente os campos de sincronização:
- `remoteId TEXT NOT NULL DEFAULT ''`: Identificador UUID do registro no PostgreSQL/Supabase.
- `syncStatus TEXT NOT NULL DEFAULT 'SYNCED'`: Estado da sincronização local (`SYNCED`, `PENDING_INSERT`, `PENDING_UPDATE`, `PENDING_DELETE`).
- `updatedAt INTEGER NOT NULL DEFAULT 0`: Timestamp UTC em milissegundos da última alteração para reconciliação LWW.
- `isDeleted INTEGER NOT NULL DEFAULT 0`: Flag de exclusão lógica (*soft delete* / tombstone).

---

## 6. Procedimento Padronizado para Futuras Migrações (v16+)

Ao introduzir qualquer alteração no esquema do banco de dados Room (adicionar/remover coluna, criar tabela ou alterar índice), siga rigorosamente o checklist abaixo:

### Passo 1: Atualizar as Entidades Kotlin
Modifique a classe correspondente em `app/src/main/java/com/example/data/Entities.kt`.

### Passo 2: Incrementar a Versão do Banco
Em [GiroCustoDatabase.kt](file:///home/nando/Documentos/GiroCustov0.1/app/src/main/java/com/example/data/GiroCustoDatabase.kt#L13):
- Incremente `version` (de `15` para `16`).

### Passo 3: Implementar o Objeto de Migração
- Em `GiroCustoDatabase.Companion`, crie a constante `MIGRATION_15_16`:
  ```kotlin
  val MIGRATION_15_16 = object : Migration(15, 16) {
      override fun migrate(db: SupportSQLiteDatabase) {
          // Comandos SQL explícitos e seguros
      }
  }
  ```
- Registre o objeto no builder em `getDatabase(context)`:
  ```kotlin
  .addMigrations(..., MIGRATION_14_15, MIGRATION_15_16)
  ```

### Passo 4: Exportar o Novo Schema JSON
Execute a compilação KSP para gerar o arquivo `16.json`:
```bash
./gradlew kspDebugKotlin
```
Verifique a criação do arquivo em `app/schemas/com.example.data.GiroCustoDatabase/16.json` e comite-o no Git.

### Passo 5: Criar Teste Automatizado com `MigrationTestHelper`
Implemente teste unitário cobrindo a migração v15 -> v16 utilizando `MigrationTestHelper`:
```kotlin
val dbV15 = helper.createDatabase(dbName, 15)
dbV15.close()

val dbV16 = helper.runMigrationsAndValidate(
    dbName,
    16,
    true,
    GiroCustoDatabase.MIGRATION_15_16
)
```

### Passo 6: Atualizar a Documentação
Atualize esta documentação (`docs/database-schemas.md`) com a descrição da versão 16, hashes gerados e detalhamento do DDL.

### Passo 7: Validação Completa
Execute compilação e verificação de testes:
```bash
./gradlew testDebugUnitTest lintDebug assembleReleaseCheck
```
