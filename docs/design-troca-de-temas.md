# Documento de Arquitetura e Especificação de UI/UX: Sistema de Temas do GiroCusto (T-THEME-01)

| Metadado | Detalhe |
| :--- | :--- |
| **Identificador da Tarefa** | T-THEME-01 |
| **Responsável** | Agente Arquiteto e Designer de UI/UX |
| **Status** | Aprovado para Implementação |
| **Data** | 2026-10-29 |
| **Camadas Afetadas** | `ui/theme`, `ui/delegates`, `ui/navigation`, `ui/screens` |
| **Documentos Base** | `docs/Tema_Eletric_Kinetic.md`, `docs/Tema_Titan Amber.md`, `docs/Tema_Orchid_Glint.md`, `Tema Padrão Esmeralda` |

---

## 1. Visão Geral e Objetivos do Produto

O aplicativo **GiroCusto** é uma ferramenta de trabalho crítica para motoristas e entregadores de aplicativo. Esses profissionais operam sob condições extremas de ambiente:
1. **Luz solar direta e reflexo excessivo** ao pilotar ou dirigir durante o dia.
2. **Ambiente escuro e necessidade de preservação da visão noturna** em corridas de madrugada.
3. **Consumo de bateria crítico** em aparelhos montados em suportes no guidão da moto ou painel do carro (onde displays OLED se beneficiam imensamente de pixels pretos verdadeiros).
4. **Vibração constante e operação com luvas**, exigindo áreas de toque generosas (>= 48dp) e rápida cognição dos dados de lucro e custos ("leitura estilo velocímetro").

A funcionalidade de troca de temas introduz **4 paletas profissionais personalizadas**, permitindo ao entregador alternar entre estéticas com propriedades óticas e contrastes específicos para cada momento de seu turno, preservando ao mesmo tempo a alternância entre modo claro e escuro.

---

## 2. Catálogo dos 4 Temas do Sistema

### 2.1 Tema 1: Esmeralda GiroCusto (`EMERALD_GIROCUSTO` - Padrão)
* **Conceito:** A identidade visual original e comprovada do GiroCusto. Fundo carvão neutro com realces em verde esmeralda. Transmite equilíbrio financeiro, lucratividade e foco em despesas sob controle.
* **Cenário Ideal:** Uso geral diário, turnos mistos e prestação de contas.
* **Amostras Principais:**
  * Primária: `#10B981` (Emerald 500)
  * Secundária: `#059669` (Emerald 600)
  * Terciária: `#34D399` (Mint Accent)
  * Fundo Escuro: `#121214` (Charcoal) | Cartão: `#1E1E22`
  * Fundo Claro: `#F8FAFC` (Slate 50) | Cartão: `#FFFFFF`

### 2.2 Tema 2: Electric Kinetic (`ELECTRIC_KINETIC`)
* **Conceito:** Interface de telemetria automotiva e fintech de alto desempenho. Chassis preto profundo (`#0A0D12` / `#0F141C`) eliminando qualquer halo ótico, com acelerações em azul elétrico (`#0A84FF`), ciano dinâmico (`#00D2FF`) para rotas/telemetria e verde lima (`#10B981`) para liquidez.
* **Cenário Ideal:** Entregadores com suporte no guidão sob sol escaldante ou em alta velocidade na estrada, exigindo fixação rápida de métricas sem distrações.
* **Amostras Principais:**
  * Primária: `#0A84FF` / `#AAC7FF` (Electric Sapphire)
  * Secundária: `#00D2FF` / `#A5E7FF` (Dynamic Cyan)
  * Terciária: `#10B981` / `#4EDEA3` (Profit Lime)
  * Fundo Escuro: `#0A0D12` (Deep Obsidian) | Cartão: `#131924`
  * Fundo Claro: `#F8F9FF` (Ghost Blue) | Cartão: `#FFFFFF`

### 2.3 Tema 3: Titan Amber Telematics (`TITAN_AMBER`)
* **Conceito:** Cockpit industrial e robusto de maquinário pesado e diagnóstico veicular. Base titânio escuro (`#121316`) com destaque em âmbar enérgico (`#F59E0B`) e laranja corrida (`#F97316`). É a paleta de mais alto contraste de luminância contra luz natural refletida.
* **Cenário Ideal:** Meio-dia ensolarado, dias nublados com forte claridade difusa e visores com película protetora fosca.
* **Amostras Principais:**
  * Primária: `#F59E0B` / `#FFC174` (Energetic Amber)
  * Secundária: `#F97316` / `#FFB690` (Racing Orange)
  * Terciária: `#10B981` / `#56E5A9` (Telemetry Mint)
  * Fundo Escuro: `#121316` (Titanium Dark) | Cartão: `#1A1C23`
  * Fundo Claro: `#FFF8F5` (Warm Cream) | Cartão: `#FFFFFF`

### 2.4 Tema 4: Orchid Glint (`ORCHID_GLINT`)
* **Conceito:** Estética contemporânea brutalista em orquídea fúcsia luminescente (`#D946EF`), lavanda suave (`#C084FC`) e hortelã neon (`#34D399`) sobre uma base obsidiana violeta (`#09050D` / `#17111B`). Elimina contornos opacos em favor de linhas de corte afiadas e alto impacto visual.
* **Cenário Ideal:** Turnos noturnos urbanos e operadores que buscam estética moderna sem perda de definição nas métricas de ganho por hora.
* **Amostras Principais:**
  * Primária: `#D946EF` / `#FBABFF` (Electric Orchid)
  * Secundária: `#C084FC` / `#DDB8FF` (Lavender Violet)
  * Terciária: `#34D399` / `#45DFA4` (Neon Mint)
  * Fundo Escuro: `#09050D` (Obsidian Violet) | Cartão: `#1F142D`
  * Fundo Claro: `#FFF7FD` (Soft Lilac) | Cartão: `#FFFFFF`

---

## 3. Especificação Técnica em Kotlin / Jetpack Compose

### 3.1 Modelo de Dados: `enum class AppTheme`
Local de criação: `app/src/main/java/com/example/ui/theme/AppTheme.kt`

```kotlin
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Catálogo de temas visuais do GiroCusto, calibrados para telemetria móvel,
 * alta vibração, luz solar direta e ergonomia noturna em telas OLED.
 */
enum class AppTheme(
    val id: String,
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color,
    val surfaceColorDark: Color
) {
    EMERALD_GIROCUSTO(
        id = "EMERALD_GIROCUSTO",
        displayName = "Esmeralda GiroCusto",
        description = "Identidade clássica em verde esmeralda e carvão profundo. Equilíbrio ótimo para produtividade financeira.",
        primaryColor = Color(0xFF10B981),
        secondaryColor = Color(0xFF059669),
        tertiaryColor = Color(0xFF34D399),
        surfaceColorDark = Color(0xFF1E1E22)
    ),
    ELECTRIC_KINETIC(
        id = "ELECTRIC_KINETIC",
        displayName = "Electric Kinetic",
        description = "Telemetria de alta performance com azul elétrico e ciano sobre chassi preto puro anti-reflexo.",
        primaryColor = Color(0xFF0A84FF),
        secondaryColor = Color(0xFF00D2FF),
        tertiaryColor = Color(0xFF10B981),
        surfaceColorDark = Color(0xFF131924)
    ),
    TITAN_AMBER(
        id = "TITAN_AMBER",
        displayName = "Titan Amber Telematics",
        description = "Cockpit industrial em titânio com âmbar enérgico e laranja corrida. Máxima legibilidade contra sol direto.",
        primaryColor = Color(0xFFF59E0B),
        secondaryColor = Color(0xFFF97316),
        tertiaryColor = Color(0xFF10B981),
        surfaceColorDark = Color(0xFF1A1C23)
    ),
    ORCHID_GLINT(
        id = "ORCHID_GLINT",
        displayName = "Orchid Glint",
        description = "Estética brutalista com orquídea fúcsia luminescente e hortelã neon sobre obsidiana violeta profunda.",
        primaryColor = Color(0xFFD946EF),
        secondaryColor = Color(0xFFC084FC),
        tertiaryColor = Color(0xFF34D399),
        surfaceColorDark = Color(0xFF1F142D)
    );

    companion object {
        fun fromId(id: String?): AppTheme {
            return values().firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EMERALD_GIROCUSTO
        }
    }
}
```

---

### 3.2 Paletas de Cores Exatas (`Color.kt` e `Theme.kt`)

#### A. Estrutura de `Color.kt`
Adição dos tokens explícitos para cada tema em `app/src/main/java/com/example/ui/theme/Color.kt`:

```kotlin
package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// =========================================================================
// 1. TEMA PADRÃO: ESMERALDA GIROCUSTO
// =========================================================================
val EmeraldPrimary = Color(0xFF10B981)
val EmeraldOnPrimary = Color(0xFF064E3B)
val EmeraldPrimaryContainer = Color(0xFF064E3B)
val EmeraldOnPrimaryContainer = Color(0xFF34D399)
val EmeraldSecondary = Color(0xFF059669)
val EmeraldTertiary = Color(0xFF34D399)
val EmeraldDarkBackground = Color(0xFF121214)
val EmeraldDarkSurface = Color(0xFF1E1E22)
val EmeraldDarkOutline = Color(0xFF2D2D34)

// =========================================================================
// 2. TEMA 2: ELECTRIC KINETIC
// =========================================================================
val ElectricKineticPrimaryDark = Color(0xFFAAC7FF)
val ElectricKineticOnPrimaryDark = Color(0xFF003064)
val ElectricKineticPrimaryContainerDark = Color(0xFF3E90FF)
val ElectricKineticOnPrimaryContainerDark = Color(0xFF002957)
val ElectricKineticSecondaryDark = Color(0xFFA5E7FF)
val ElectricKineticSecondaryContainerDark = Color(0xFF00D2FF)
val ElectricKineticTertiaryDark = Color(0xFF4EDEA3)
val ElectricKineticBackgroundDark = Color(0xFF0A0D12)
val ElectricKineticSurfaceDark = Color(0xFF131924)
val ElectricKineticOutlineDark = Color(0xFF414754)

val ElectricKineticPrimaryLight = Color(0xFF005DB8)
val ElectricKineticOnPrimaryLight = Color(0xFFFFFFFF)
val ElectricKineticPrimaryContainerLight = Color(0xFFD6E3FF)
val ElectricKineticOnPrimaryContainerLight = Color(0xFF001B3E)
val ElectricKineticSecondaryLight = Color(0xFF00667D)
val ElectricKineticTertiaryLight = Color(0xFF006C49)
val ElectricKineticBackgroundLight = Color(0xFFF8F9FF)
val ElectricKineticSurfaceLight = Color(0xFFFFFFFF)
val ElectricKineticOutlineLight = Color(0xFFC0C6D6)

// =========================================================================
// 3. TEMA 3: TITAN AMBER TELEMATICS
// =========================================================================
val TitanAmberPrimaryDark = Color(0xFFFFC174)
val TitanAmberOnPrimaryDark = Color(0xFF472A00)
val TitanAmberPrimaryContainerDark = Color(0xFFF59E0B)
val TitanAmberOnPrimaryContainerDark = Color(0xFF613B00)
val TitanAmberSecondaryDark = Color(0xFFFFB690)
val TitanAmberSecondaryContainerDark = Color(0xFFEC6A06)
val TitanAmberTertiaryDark = Color(0xFF56E5A9)
val TitanAmberBackgroundDark = Color(0xFF121316)
val TitanAmberSurfaceDark = Color(0xFF1A1C23)
val TitanAmberOutlineDark = Color(0xFF2D313E)

val TitanAmberPrimaryLight = Color(0xFF855300)
val TitanAmberOnPrimaryLight = Color(0xFFFFFFFF)
val TitanAmberPrimaryContainerLight = Color(0xFFFFDDB8)
val TitanAmberOnPrimaryContainerLight = Color(0xFF2A1700)
val TitanAmberSecondaryLight = Color(0xFF8F4C23)
val TitanAmberTertiaryLight = Color(0xFF006C49)
val TitanAmberBackgroundLight = Color(0xFFFFF8F5)
val TitanAmberSurfaceLight = Color(0xFFFFFFFF)
val TitanAmberOutlineLight = Color(0xFFD8C3AD)

// =========================================================================
// 4. TEMA 4: ORCHID GLINT
// =========================================================================
val OrchidGlintPrimaryDark = Color(0xFFFBABFF)
val OrchidGlintOnPrimaryDark = Color(0xFF580065)
val OrchidGlintPrimaryContainerDark = Color(0xFFE14EF6)
val OrchidGlintOnPrimaryContainerDark = Color(0xFF4D0059)
val OrchidGlintSecondaryDark = Color(0xFFDDB8FF)
val OrchidGlintSecondaryContainerDark = Color(0xFF62259B)
val OrchidGlintTertiaryDark = Color(0xFF45DFA4)
val OrchidGlintBackgroundDark = Color(0xFF09050D)
val OrchidGlintSurfaceDark = Color(0xFF1F142D)
val OrchidGlintOutlineDark = Color(0xFF524251)

val OrchidGlintPrimaryLight = Color(0xFFA200BA)
val OrchidGlintOnPrimaryLight = Color(0xFFFFFFFF)
val OrchidGlintPrimaryContainerLight = Color(0xFFFFD6FD)
val OrchidGlintOnPrimaryContainerLight = Color(0xFF36003E)
val OrchidGlintSecondaryLight = Color(0xFF7838B5)
val OrchidGlintTertiaryLight = Color(0xFF006C4A)
val OrchidGlintBackgroundLight = Color(0xFFFFF7FD)
val OrchidGlintSurfaceLight = Color(0xFFFFFFFF)
val OrchidGlintOutlineLight = Color(0xFFD6C0D3)
```

#### B. `ColorScheme` Completo em `Theme.kt`
Local: `app/src/main/java/com/example/ui/theme/Theme.kt`

```kotlin
package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// --- Esmeralda GiroCusto ---
val EmeraldDarkColorScheme = darkColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color(0xFF064E3B),
    primaryContainer = Color(0xFF064E3B),
    onPrimaryContainer = Color(0xFF34D399),
    secondary = Color(0xFF10B981),
    onSecondary = Color(0xFF064E3B),
    secondaryContainer = Color(0xFF022C22),
    onSecondaryContainer = Color(0xFF6EE7B7),
    tertiary = Color(0xFF34D399),
    onTertiary = Color(0xFF003822),
    tertiaryContainer = Color(0xFF065F46),
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = Color(0xFF121214),
    onBackground = Color(0xFFF1F5F9),
    surface = Color(0xFF1E1E22),
    onSurface = Color(0xFFE2E8F0),
    surfaceVariant = Color(0xFF26262B),
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = Color(0xFF2D2D34),
    error = Color(0xFFF43F5E),
    onError = Color.White
)

val EmeraldLightColorScheme = lightColorScheme(
    primary = Color(0xFF10B981),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD1FAE5),
    onPrimaryContainer = Color(0xFF065F46),
    secondary = Color(0xFF059669),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFECFDF5),
    onSecondaryContainer = Color(0xFF047857),
    tertiary = Color(0xFF047857),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFA7F3D0),
    onTertiaryContainer = Color(0xFF064E3B),
    background = Color(0xFFF8FAFC),
    onBackground = Color(0xFF0F172A),
    surface = Color.White,
    onSurface = Color(0xFF1E293B),
    surfaceVariant = Color(0xFFF1F5F9),
    onSurfaceVariant = Color(0xFF64748B),
    outline = Color(0xFFE2E8F0),
    error = Color(0xFFE11D48),
    onError = Color.White
)

// --- Electric Kinetic ---
val ElectricKineticDarkColorScheme = darkColorScheme(
    primary = Color(0xFFAAC7FF),
    onPrimary = Color(0xFF003064),
    primaryContainer = Color(0xFF3E90FF),
    onPrimaryContainer = Color(0xFF002957),
    secondary = Color(0xFFA5E7FF),
    onSecondary = Color(0xFF003543),
    secondaryContainer = Color(0xFF00D2FF),
    onSecondaryContainer = Color(0xFF00566A),
    tertiary = Color(0xFF4EDEA3),
    onTertiary = Color(0xFF003824),
    tertiaryContainer = Color(0xFF00A572),
    onTertiaryContainer = Color(0xFF00311F),
    background = Color(0xFF0A0D12),
    onBackground = Color(0xFFDEE2EE),
    surface = Color(0xFF131924),
    onSurface = Color(0xFFDEE2EE),
    surfaceVariant = Color(0xFF30353E),
    onSurfaceVariant = Color(0xFFC0C6D6),
    outline = Color(0xFF414754),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val ElectricKineticLightColorScheme = lightColorScheme(
    primary = Color(0xFF005DB8),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF00667D),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFB6EBFF),
    onSecondaryContainer = Color(0xFF001F28),
    tertiary = Color(0xFF006C49),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF6FFBBE),
    onTertiaryContainer = Color(0xFF002113),
    background = Color(0xFFF8F9FF),
    onBackground = Color(0xFF0F141C),
    surface = Color.White,
    onSurface = Color(0xFF0F141C),
    surfaceVariant = Color(0xFFDEE2EE),
    onSurfaceVariant = Color(0xFF414754),
    outline = Color(0xFFC0C6D6),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

// --- Titan Amber Telematics ---
val TitanAmberDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFC174),
    onPrimary = Color(0xFF472A00),
    primaryContainer = Color(0xFFF59E0B),
    onPrimaryContainer = Color(0xFF613B00),
    secondary = Color(0xFFFFB690),
    onSecondary = Color(0xFF552100),
    secondaryContainer = Color(0xFFEC6A06),
    onSecondaryContainer = Color(0xFF4A1C00),
    tertiary = Color(0xFF56E5A9),
    onTertiary = Color(0xFF003824),
    tertiaryContainer = Color(0xFF30C88F),
    onTertiaryContainer = Color(0xFF004E34),
    background = Color(0xFF121316),
    onBackground = Color(0xFFE3E2E6),
    surface = Color(0xFF1A1C23),
    onSurface = Color(0xFFE3E2E6),
    surfaceVariant = Color(0xFF343538),
    onSurfaceVariant = Color(0xFFD8C3AD),
    outline = Color(0xFF2D313E),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val TitanAmberLightColorScheme = lightColorScheme(
    primary = Color(0xFF855300),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDDB8),
    onPrimaryContainer = Color(0xFF2A1700),
    secondary = Color(0xFF8F4C23),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFDBCA),
    onSecondaryContainer = Color(0xFF341100),
    tertiary = Color(0xFF006C49),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF6FFBBE),
    onTertiaryContainer = Color(0xFF002113),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF201A15),
    surface = Color.White,
    onSurface = Color(0xFF201A15),
    surfaceVariant = Color(0xFFF3DFD1),
    onSurfaceVariant = Color(0xFF52443C),
    outline = Color(0xFFD8C3AD),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

// --- Orchid Glint ---
val OrchidGlintDarkColorScheme = darkColorScheme(
    primary = Color(0xFFFBABFF),
    onPrimary = Color(0xFF580065),
    primaryContainer = Color(0xFFE14EF6),
    onPrimaryContainer = Color(0xFF4D0059),
    secondary = Color(0xFFDDB8FF),
    onSecondary = Color(0xFF490081),
    secondaryContainer = Color(0xFF62259B),
    onSecondaryContainer = Color(0xFFD1A1FF),
    tertiary = Color(0xFF45DFA4),
    onTertiary = Color(0xFF003825),
    tertiaryContainer = Color(0xFF00A574),
    onTertiaryContainer = Color(0xFF003120),
    background = Color(0xFF09050D),
    onBackground = Color(0xFFEADFED),
    surface = Color(0xFF1F142D),
    onSurface = Color(0xFFEADFED),
    surfaceVariant = Color(0xFF39323D),
    onSurfaceVariant = Color(0xFFD6C0D3),
    outline = Color(0xFF524251),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

val OrchidGlintLightColorScheme = lightColorScheme(
    primary = Color(0xFFA200BA),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFD6FD),
    onPrimaryContainer = Color(0xFF36003E),
    secondary = Color(0xFF7838B5),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFF0DBFF),
    onSecondaryContainer = Color(0xFF2C0051),
    tertiary = Color(0xFF006C4A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF68FCBF),
    onTertiaryContainer = Color(0xFF002114),
    background = Color(0xFFFFF7FD),
    onBackground = Color(0xFF201821),
    surface = Color.White,
    onSurface = Color(0xFF201821),
    surfaceVariant = Color(0xFFF1DEEE),
    onSurfaceVariant = Color(0xFF50434E),
    outline = Color(0xFFD6C0D3),
    error = Color(0xFFBA1A1A),
    onError = Color.White
)

fun getThemeColorScheme(theme: AppTheme, isDark: Boolean): ColorScheme {
    return when (theme) {
        AppTheme.EMERALD_GIROCUSTO -> if (isDark) EmeraldDarkColorScheme else EmeraldLightColorScheme
        AppTheme.ELECTRIC_KINETIC -> if (isDark) ElectricKineticDarkColorScheme else ElectricKineticLightColorScheme
        AppTheme.TITAN_AMBER -> if (isDark) TitanAmberDarkColorScheme else TitanAmberLightColorScheme
        AppTheme.ORCHID_GLINT -> if (isDark) OrchidGlintDarkColorScheme else OrchidGlintLightColorScheme
    }
}

@Composable
fun GiroCustoTheme(
    appTheme: AppTheme = AppTheme.EMERALD_GIROCUSTO,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = getThemeColorScheme(appTheme, darkTheme)
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Retrocompatibilidade garantida para testes existentes
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    GiroCustoTheme(
        appTheme = AppTheme.EMERALD_GIROCUSTO,
        darkTheme = darkTheme,
        content = content
    )
}
```

---

## 4. Integração Arquitetural e Reatividade

### 4.1 Contrato no `GoalsAndThemeCoordinator.kt`
* **Persistência Segura e Desacoplada:** O tema é armazenado no `giro_custo_preferences` (mesmo local aprovado na Tarefa T-28B para preferências do app sem misturar com metas).
* **Chave de Persistência:** `"selected_app_theme"` (valor padrão `"EMERALD_GIROCUSTO"`).
* **Retrocompatibilidade:** Mantém intacto `isDarkTheme: MutableStateFlow<Boolean>` e `toggleTheme()`, permitindo que o usuário alterne livremente entre Claro e Escuro sem perder qual das 4 paletas cromáticas está ativa.

Código a adicionar no `GoalsAndThemeCoordinator.kt`:
```kotlin
private fun getInitialThemeSelection(): AppTheme {
    val savedId = appPrefs.getString("selected_app_theme", AppTheme.EMERALD_GIROCUSTO.id)
    return AppTheme.fromId(savedId)
}

val currentTheme = MutableStateFlow(getInitialThemeSelection())

fun setAppTheme(theme: AppTheme) {
    currentTheme.value = theme
    appPrefs.edit().putString("selected_app_theme", theme.id).apply()
}

fun setDarkTheme(isDark: Boolean) {
    isDarkTheme.value = isDark
    appPrefs.edit().putBoolean("is_dark_theme", isDark).apply()
}
```

### 4.2 Contrato no `GiroCustoViewModel.kt`
Exposição pública dos fluxos reativos e métodos de comando:
```kotlin
// Em GiroCustoViewModel.kt
val currentTheme: StateFlow<AppTheme> get() = goalsCoordinator.currentTheme

fun setAppTheme(theme: AppTheme) {
    goalsCoordinator.setAppTheme(theme)
}

fun setDarkTheme(isDark: Boolean) {
    goalsCoordinator.setDarkTheme(isDark)
}
```

### 4.3 Integração no Menu Lateral (`AppDrawer.kt`)
* Inclusão do destino no `enum class DrawerDestination`:
  ```kotlin
  enum class DrawerDestination {
      NONE,
      PERFIL,
      PLATAFORMAS,
      ABASTECIMENTOS,
      MANUTENCAO,
      NUVEM,
      TEMAS // NOVO
  }
  ```
* Item no menu drawer com ícone, rótulo e testTag:
  ```kotlin
  NavigationDrawerItem(
      icon = { Icon(Icons.Filled.Palette, contentDescription = "Temas") },
      label = { Text("Temas") },
      selected = selectedDestination == DrawerDestination.TEMAS,
      onClick = { onDestinationSelected(DrawerDestination.TEMAS) },
      modifier = Modifier
          .padding(NavigationDrawerItemDefaults.ItemPadding)
          .testTag("drawer_item_themes"),
      colors = NavigationDrawerItemDefaults.colors(
          selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
          selectedIconColor = MaterialTheme.colorScheme.primary,
          selectedTextColor = MaterialTheme.colorScheme.primary
      )
  )
  ```

### 4.4 Roteamento e TopBar Dinâmica no `GiroCustoMainApp.kt`
* `GiroCustoMainApp.kt` coleta `currentTheme`:
  ```kotlin
  val currentTheme by viewModel.currentTheme.collectAsStateWithLifecycle()
  val isDark by viewModel.isDarkTheme.collectAsStateWithLifecycle()
  val customColorScheme = getThemeColorScheme(currentTheme, isDark)
  ```
* Título da TopBar atualizado para:
  ```kotlin
  DrawerDestination.TEMAS -> "Temas"
  ```
* Renderização no `AnimatedContent`:
  ```kotlin
  DrawerDestination.TEMAS -> ThemeSelectionScreen(
      viewModel = viewModel,
      onNavigateBack = { drawerDestination = DrawerDestination.NONE }
  )
  ```

---

## 5. Especificação de UI/UX da Tela `ThemeSelectionScreen`

### 5.1 Anatomia da Tela e Layout Hierárquico

```
┌────────────────────────────────────────────────────────┐
│ TopAppBar: "Temas"                    [Voltar / Fechar]│
├────────────────────────────────────────────────────────┤
│ (Scroll vertical com uma mão / Padding inferior 80dp)  │
│                                                        │
│ [Card 1: Prévia em Tempo Real da Interface]            │
│ ┌────────────────────────────────────────────────────┐ │
│ │ Telemetria Ativa • Modo Atual                      │ │
│ │ LUCRO LÍQUIDO (HOJE)                               │ │
│ │ R$ 184,50                          [+38% vs Meta]  │ │
│ │ ────────────────────────────────────────────────── │ │
│ │ R$ 2,45 / km • 68 km rodados                       │ │
│ │ [ Botão de Demonstração Primário: "Nova Corrida" ] │ │
│ └────────────────────────────────────────────────────┘ │
│                                                        │
│ [Card 2: Alternador de Modo Claro / Escuro]            │
│ ┌────────────────────────────────────────────────────┐ │
│ │ (Ícone Sol/Lua) Modo Escuro (Noturno)     [SWITCH] │ │
│ │ Fundo OLED com economia de bateria e anti-ofusco   │ │
│ └────────────────────────────────────────────────────┘ │
│                                                        │
│ Subtítulo: "ESCOLHA SUA PALETA DE CORES"               │
│                                                        │
│ [Card de Seleção: 1. Esmeralda GiroCusto]  (O) Selec. │
│ ┌────────────────────────────────────────────────────┐ │
│ │ ● Esmeralda GiroCusto                     (●) Radio│ │
│ │ [Badge: Padrão]                                    │ │
│ │ Identidade clássica com foco em produtividade      │ │
│ │ Amostras: [Primária] [Secundária] [Terciária]      │ │
│ └────────────────────────────────────────────────────┘ │
│                                                        │
│ [Card de Seleção: 2. Electric Kinetic]                 │
│ ┌────────────────────────────────────────────────────┐ │
│ │ ● Electric Kinetic                        ( ) Radio│ │
│ │ Telemetria esportiva com azul elétrico e ciano     │ │
│ │ Amostras: [Primária] [Secundária] [Terciária]      │ │
│ └────────────────────────────────────────────────────┘ │
│                                                        │
│ [Card de Seleção: 3. Titan Amber Telematics]           │
│ ┌────────────────────────────────────────────────────┐ │
│ │ ● Titan Amber Telematics                  ( ) Radio│ │
│ │ Cockpit industrial em titânio e âmbar anti-sol     │ │
│ │ Amostras: [Primária] [Secundária] [Terciária]      │ │
│ └────────────────────────────────────────────────────┘ │
│                                                        │
│ [Card de Seleção: 4. Orchid Glint]                     │
│ ┌────────────────────────────────────────────────────┐ │
│ │ ● Orchid Glint                            ( ) Radio│ │
│ │ Brutalismo em orquídea fúcsia e hortelã neon       │ │
│ │ Amostras: [Primária] [Secundária] [Terciária]      │ │
│ └────────────────────────────────────────────────────┘ │
└────────────────────────────────────────────────────────┘
```

---

### 5.2 Detalhamento dos Componentes

#### 1. Header & Switch de Modo Claro/Escuro
* **Função:** Permitir ao entregador alternar entre modo diurno e noturno diretamente na mesma tela em que avalia as paletas.
* **Componente:** `Card` com `Switch` Material 3.
* **Área de Toque:** Linha completa clicável (mínimo 56dp de altura).
* **TestTag:** `theme_mode_toggle_switch`.
* **Semântica TalkBack:** `"Modo escuro ativado/desativado. Toque duas vezes para alternar."`

#### 2. Cartão de Prévia Dinâmica (`theme_preview_card`)
* **Função:** Fornecer feedback visual imediato ao tocar em qualquer tema, sem necessidade de sair da tela ou reiniciar o app.
* **Estrutura:**
  * Superfície com container tonal do tema selecionado (`MaterialTheme.colorScheme.surfaceVariant`).
  * Métrica em destaque: valor monetário tabular com `MaterialTheme.colorScheme.tertiary` (rendimento/lucro).
  * Chip indicador com borda e preenchimento translúcido.
  * Botão de teste interativo com `MaterialTheme.colorScheme.primary` e texto em `onPrimary`.

#### 3. Cartões de Seleção de Tema (`theme_card_{ID}`)
* **Altura Mínima:** 96dp (supera amplamente o requisito de 48dp).
* **Estados Visuais:**
  * **Selecionado:** Borda sólida de `2.dp` na cor `theme.primaryColor`, leve elevação de `2.dp` e fundo sutilmente realçado.
  * **Não Selecionado:** Borda fina de `1.dp` em `outlineVariant`, elevação `0.dp`.
* **Swatches de Cores (Amostras):**
  * Fileira com 3 círculos de 24dp exibindo:
    1. Primária (Cor principal de ação e botões).
    2. Secundária (Rotas, telemetria e abas).
    3. Terciária (Lucro líquido, rendimento e confirmações).
  * Borda sutil de 1dp em cada amostra para garantir visibilidade mesmo quando sobreposta a tons similares.
* **RadioButton:**
  * Alinhado à direita com `selected = isSelected`.
  * Cores personalizadas: `RadioButtonDefaults.colors(selectedColor = theme.primaryColor)`.
  * `testTag = "theme_radio_${theme.id}"`.

---

## 6. Critérios de Aceite de Acessibilidade e UX (WCAG 2.1 AA/AAA)

| Requisito de Acessibilidade | Padrão GiroCusto | Solução Adotada na Tela de Temas |
| :--- | :--- | :--- |
| **Área de Toque Mínima** | >= 48 x 48 dp | Toda a área do cartão (largura total x 96dp) é o alvo de toque do `selectable`. O Switch possui altura de 56dp. |
| **TalkBack / Leitor de Tela** | Leitura coesa de agrupamento | `Modifier.selectable(selected = isSelected, onClick = { ... }, role = Role.RadioButton)` aplicado no card raiz com `contentDescription` explicativo. |
| **Contraste Solar (Luz Forte)** | >= 4.5:1 (Normal) / 3:1 (Grande) | Titan Amber e Electric Kinetic alcançam ratios > 9:1 em modo escuro e > 6:1 em modo claro, eliminando reflexos. |
| **Informação Não Exclusiva por Cor** | Nunca depender só de cor | Uso conjunto de: RadioButton ativo, borda de 2dp, badge "Padrão" / "Ativo", e rótulos de texto descritivos. |
| **Escala de Fonte em 200%** | Sem corte de texto | Layouts utilizam `Modifier.fillMaxWidth()` com alturas flexíveis (`Modifier.defaultMinSize(minHeight = 96.dp)`), permitindo expansão vertical sem clipping. |
| **Operação com Uma Mão** | Alcance do polegar | Ações e cartões dispostos verticalmente na metade central e inferior da tela. Rolagem fluida com padding seguro de navegação. |

---

## 7. Strings Recomendadas (`strings.xml`)

```xml
<!-- Temas Visuais (T-THEME-01) -->
<string name="theme_screen_title">Temas</string>
<string name="theme_screen_subtitle">Personalize as cores para melhor visualização na rua, sob sol forte ou à noite.</string>
<string name="theme_preview_title">Prévia em Tempo Real</string>
<string name="theme_preview_metric_label">LUCRO LÍQUIDO (HOJE)</string>
<string name="theme_preview_metric_value">R$ 184,50</string>
<string name="theme_preview_chip">Meta Diária +38%</string>
<string name="theme_preview_button">Nova Corrida</string>
<string name="theme_preview_details">R$ 2,45 / km • 68 km rodados</string>

<string name="theme_dark_mode_title">Modo Escuro (Noturno)</string>
<string name="theme_dark_mode_desc">Fundo escuro para economia de bateria e conforto visual à noite.</string>

<string name="theme_section_palette">ESCOLHA SUA PALETA DE CORES</string>
<string name="theme_badge_default">PADRÃO</string>

<string name="theme_emerald_name">Esmeralda GiroCusto</string>
<string name="theme_emerald_desc">Identidade original em verde esmeralda e carvão. Equilíbrio ótimo para produtividade diária.</string>

<string name="theme_electric_name">Electric Kinetic</string>
<string name="theme_electric_desc">Telemetria de alta performance com azul elétrico e ciano sobre chassi preto puro anti-reflexo.</string>

<string name="theme_titan_name">Titan Amber Telematics</string>
<string name="theme_titan_desc">Cockpit industrial em titânio com âmbar enérgico e laranja corrida. Máxima legibilidade contra sol direto.</string>

<string name="theme_orchid_name">Orchid Glint</string>
<string name="theme_orchid_desc">Estética brutalista com orquídea fúcsia luminescente e hortelã neon sobre obsidiana violeta profunda.</string>

<string name="cd_theme_select">Selecionar tema %1$s</string>
<string name="cd_theme_selected">Tema %1$s selecionado atualmente</string>
```

---

## 8. Guia para o Desenvolvedor e QA

### 8.1 Arquivos a Criar e Alterar
1. **Criar:** `app/src/main/java/com/example/ui/theme/AppTheme.kt`
2. **Atualizar:** `app/src/main/java/com/example/ui/theme/Color.kt` (tokens de cores para os 4 temas).
3. **Atualizar:** `app/src/main/java/com/example/ui/theme/Theme.kt` (paletas `darkColorScheme` e `lightColorScheme` e helper `getThemeColorScheme`).
4. **Atualizar:** `app/src/main/java/com/example/ui/delegates/GoalsAndThemeCoordinator.kt` (`currentTheme`, `setAppTheme`, persistência em `giro_custo_preferences`).
5. **Atualizar:** `app/src/main/java/com/example/ui/GiroCustoViewModel.kt` (exposição de `currentTheme` e `setAppTheme`).
6. **Atualizar:** `app/src/main/java/com/example/ui/navigation/AppDrawer.kt` (adição de `DrawerDestination.TEMAS`).
7. **Atualizar:** `app/src/main/java/com/example/ui/navigation/GiroCustoMainApp.kt` (aplicação do tema dinâmico via `MaterialTheme(colorScheme = customColorScheme)` e navegação para `TEMAS`).
8. **Criar:** `app/src/main/java/com/example/ui/screens/ThemeSelectionScreen.kt` (tela de seleção com preview e acessibilidade).

### 8.2 Roteiro de Testes Unitários e de UI para o QA
1. **Persistência de Tema:**
   * Garantir que ao chamar `viewModel.setAppTheme(AppTheme.TITAN_AMBER)`, o valor é salvo no `giro_custo_preferences` com chave `"selected_app_theme"` e lido corretamente após recriação do ViewModel.
   * Garantir que a persistência existente de `is_dark_theme` (true/false) continue funcionando sem quebra de testes legados.
2. **Navegação do Drawer:**
   * Clicar no item com tag `drawer_item_themes` no `AppDrawer`.
   * Verificar se a tela de temas é exibida com o título "Temas" na TopBar e botão de voltar funcional (`topbar_back_btn`).
3. **Seleção e Feedback em Tempo Real:**
   * Tocar em cada um dos 4 cartões de tema (`theme_card_EMERALD_GIROCUSTO`, `theme_card_ELECTRIC_KINETIC`, `theme_card_TITAN_AMBER`, `theme_card_ORCHID_GLINT`).
   * Verificar se o `RadioButton` correspondente fica selecionado (`assertIsSelected()`).
   * Verificar se o `theme_preview_card` reflete as cores do tema selecionado instantaneamente.
4. **Acessibilidade:**
   * Validar área de toque mínima de 48dp nos cartões e botões.
   * Validar nó de acessibilidade semântica unificado com `Role.RadioButton`.
