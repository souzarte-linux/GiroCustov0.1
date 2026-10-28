package com.example.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Utilitário central e thread-safe para operações monetárias no GiroCusto.
 *
 * Princípios Arquiteturais:
 * 1. Todos os valores monetários consolidados no app operam em inteiros (Long) de centavos (escala 100).
 * 2. Preços unitários por litro de combustível operam em inteiros (Long) de milésimos de real (escala 1000),
 *    em estrito cumprimento à Resolução ANP nº 948/2023 (3 casas decimais nos postos brasileiros).
 * 3. Thread-safety: DecimalFormat não é thread-safe na JVM. Instâncias são isoladas via ThreadLocal.
 * 4. Parsing resiliente: Suporta formatos pt-BR ("1.234,56", "150,50"), US ("1,234.56", "150.50"),
 *    números sem separadores e trata explicitamente entradas com ponto de milhar sem vírgula ("1.500" -> R$ 1.500,00).
 */
object CurrencyUtils {

    const val CENTS_PER_REAL = 100L
    const val MILLIS_PER_REAL = 1000L
    val ptBrLocale: Locale = Locale.forLanguageTag("pt-BR")

    private val symbols = DecimalFormatSymbols(ptBrLocale).apply {
        currencySymbol = "R$"
        groupingSeparator = '.'
        decimalSeparator = ','
    }

    // ThreadLocal para garantir concorrência segura com Jetpack Compose e Kotlin Coroutines
    private val currencyFormatWithSymbolThreadLocal = ThreadLocal.withInitial {
        DecimalFormat("R$ #,##0.00", symbols)
    }

    private val currencyFormatWithoutSymbolThreadLocal = ThreadLocal.withInitial {
        DecimalFormat("#,##0.00", symbols)
    }

    private val fuelPriceFormatWithSymbolThreadLocal = ThreadLocal.withInitial {
        DecimalFormat("R$ #,##0.000", symbols)
    }

    private val fuelPriceFormatWithoutSymbolThreadLocal = ThreadLocal.withInitial {
        DecimalFormat("#,##0.000", symbols)
    }

    private val compactFormatThreadLocal = ThreadLocal.withInitial {
        DecimalFormat("R$ #,##0", symbols)
    }

    // =========================================================================
    // 1. CONVERSÃO E FORMATAÇÃO PADRÃO (CENTAVOS - ESCALA 100)
    // =========================================================================

    /**
     * Converte um valor monetário em Double (reais) para centavos inteiros (Long).
     * Exemplo: 150.50 -> 15050L; 150.556 -> 15056L (arredondamento matemático).
     */
    fun toCents(reais: Double): Long {
        if (reais.isNaN() || reais.isInfinite()) return 0L
        return Math.round(reais * CENTS_PER_REAL)
    }

    /**
     * Converte centavos (Long) para Double (reais).
     * Exemplo: 15050L -> 150.50
     */
    fun centsToDouble(cents: Long): Double {
        return cents.toDouble() / CENTS_PER_REAL.toDouble()
    }

    /**
     * Formata centavos no padrão monetário brasileiro: "R$ 1.234,56".
     * Valores negativos são explicitamente formatados como "- R$ 45,00".
     */
    fun formatCents(cents: Long, showSymbol: Boolean = true): String {
        val isNegative = cents < 0
        val absCents = Math.abs(cents)
        val reais = absCents.toDouble() / CENTS_PER_REAL.toDouble()
        val formatter: DecimalFormat = (if (showSymbol) {
            currencyFormatWithSymbolThreadLocal.get()
        } else {
            currencyFormatWithoutSymbolThreadLocal.get()
        }) ?: DecimalFormat(if (showSymbol) "R$ #,##0.00" else "#,##0.00", symbols)
        val formatted = formatter.format(reais)
        return if (isNegative) "- $formatted" else formatted
    }

    /**
     * Formata centavos sem o símbolo "R$" (ex.: "1.234,56").
     * Ideal para campos onde o símbolo já é exibido no ícone/prefixo do TextField.
     */
    fun formatCentsWithoutSymbol(cents: Long): String {
        return formatCents(cents, showSymbol = false)
    }

    /**
     * Formata centavos suprimindo casas decimais (ex.: "R$ 1.235").
     * Utilizado em resumos de metas e cabeçalhos compactos.
     */
    fun formatCentsCompact(cents: Long): String {
        val isNegative = cents < 0
        val absCents = Math.abs(cents)
        val reaisRounded = Math.round(absCents.toDouble() / CENTS_PER_REAL.toDouble())
        val formatter: DecimalFormat = compactFormatThreadLocal.get() ?: DecimalFormat("R$ #,##0", symbols)
        val formatted = formatter.format(reaisRounded)
        return if (isNegative) "- $formatted" else formatted
    }

    /**
     * Parsing resiliente de string digitada pelo usuário para centavos inteiros (Long).
     *
     * Regras e Definições de Entrada:
     * 1. Limpa espaços normais, espaços não-quebráveis (\u00A0, \u202F) e prefixo "R$".
     * 2. Com vírgula e ponto (ex.: "1.234,56"): ponto é milhar, vírgula é decimal -> 123456L.
     * 3. Apenas vírgula (ex.: "150,50", "1500,00"): vírgula é decimal -> 15050L, 150000L.
     * 4. PONTO DE MILHAR SEM VÍRGULA (ex.: "1.500", "15.000", "1.500.000"):
     *    Em campos monetários gerais (faturamento, manutenção, custos fixos, metas), uma entrada
     *    com ponto e sem vírgula onde a parte decimal possui exatamente 3 dígitos (ou múltiplos pontos)
     *    representa um separador de milhar.
     *    -> "1.500" é lido como R$ 1.500,00 (150000L) e NUNCA como R$ 1,50.
     * 5. Ponto decimal americano ou teclado numérico (ex.: "1.50", "1.5", "150.75"):
     *    Se houver um único ponto seguido por 1 ou 2 dígitos, é interpretado como separador decimal.
     *    -> "1.50" é lido como R$ 1,50 (150L).
     */
    fun parseToCents(input: String?, fallbackCents: Long = 0L): Long {
        if (input.isNullOrBlank()) return fallbackCents
        val clean = input.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("\u202F", "")

        if (clean.isBlank()) return fallbackCents

        return try {
            val isNegative = clean.startsWith("-")
            val cleanUnsigned = if (isNegative) clean.substring(1).trim() else clean

            val normalized = if (cleanUnsigned.contains('.') && cleanUnsigned.contains(',')) {
                if (cleanUnsigned.lastIndexOf(',') > cleanUnsigned.lastIndexOf('.')) {
                    // Padrão pt-BR: 1.234,56
                    cleanUnsigned.replace(".", "").replace(',', '.')
                } else {
                    // Padrão US: 1,234.56
                    cleanUnsigned.replace(",", "")
                }
            } else if (cleanUnsigned.contains(',')) {
                // Apenas vírgula: 150,50 ou 1500,00
                cleanUnsigned.replace(',', '.')
            } else if (cleanUnsigned.contains('.')) {
                // Apenas ponto (sem vírgula)
                val dotCount = cleanUnsigned.count { it == '.' }
                if (dotCount > 1) {
                    // Múltiplos pontos: 1.500.000 -> claramente separadores de milhar
                    cleanUnsigned.replace(".", "")
                } else {
                    val parts = cleanUnsigned.split('.')
                    val decimals = parts.getOrNull(1) ?: ""
                    if (decimals.length == 3) {
                        // Ponto seguido de exatamente 3 dígitos: "1.500", "15.000" -> separador de milhar pt-BR
                        cleanUnsigned.replace(".", "")
                    } else {
                        // Ponto seguido de 1 ou 2 dígitos (ex: "1.5", "1.50") -> ponto decimal
                        cleanUnsigned
                    }
                }
            } else {
                cleanUnsigned
            }

            val doubleVal = normalized.toDoubleOrNull() ?: return fallbackCents
            val cents = toCents(doubleVal)
            if (isNegative) -cents else cents
        } catch (_: Exception) {
            fallbackCents
        }
    }

    // =========================================================================
    // 2. CONVERSÃO E FORMATAÇÃO DE COMBUSTÍVEL (MILÉSIMOS - ESCALA 1000 - 3 CASAS)
    // =========================================================================

    /**
     * Converte o preço do litro em Double (ex.: 5.899) para milésimos de real (Long: 5899L).
     */
    fun toFuelPriceMillis(reais: Double): Long {
        if (reais.isNaN() || reais.isInfinite()) return 0L
        return Math.round(reais * MILLIS_PER_REAL)
    }

    /**
     * Converte milésimos de real (5899L) de volta para Double (5.899).
     */
    fun fuelPriceMillisToDouble(millis: Long): Double {
        return millis.toDouble() / MILLIS_PER_REAL.toDouble()
    }

    /**
     * Formata o preço por litro com 3 casas decimais (ex.: "R$ 5,899" ou "5,899").
     */
    fun formatFuelPrice(millis: Long, showSymbol: Boolean = true): String {
        val isNegative = millis < 0
        val absMillis = Math.abs(millis)
        val reais = absMillis.toDouble() / MILLIS_PER_REAL.toDouble()
        val formatter: DecimalFormat = (if (showSymbol) {
            fuelPriceFormatWithSymbolThreadLocal.get()
        } else {
            fuelPriceFormatWithoutSymbolThreadLocal.get()
        }) ?: DecimalFormat(if (showSymbol) "R$ #,##0.000" else "#,##0.000", symbols)
        val formatted = formatter.format(reais)
        return if (isNegative) "- $formatted" else formatted
    }

    /**
     * Formata o preço do combustível sem o símbolo "R$" (ex.: "5,899").
     */
    fun formatFuelPriceWithoutSymbol(millis: Long): String {
        return formatFuelPrice(millis, showSymbol = false)
    }

    /**
     * Parsing resiliente para preços unitários de combustível por litro (até 3 casas decimais).
     *
     * Definição de Domínio:
     * - No contexto de combustíveis, o valor representa o preço unitário do litro (normalmente entre R$ 2,000 e R$ 15,000).
     * - "5,899" e "5.899" são interpretados como R$ 5,899/litro -> 5899L.
     * - "5,89" e "5.89" -> 5890L.
     * - "5,9" e "5.9" -> 5900L.
     * - "6" -> 6000L.
     * - Ponto de milhar com múltiplos pontos (ex.: "1.500.000") ou com vírgula decimal (ex.: "1.500,000"):
     *   os pontos são tratados como milhar.
     * - Se uma entrada ambígua como "1.500" for informada no campo de combustível:
     *   ela representa 1 real e 500 milésimos (R$ 1,500/L -> 1500L), coerente com o padrão de precificação unitária de combustíveis.
     */
    fun parseToFuelPriceMillis(input: String?, fallbackMillis: Long = 0L): Long {
        if (input.isNullOrBlank()) return fallbackMillis
        val clean = input.trim()
            .replace("R$", "")
            .replace(" ", "")
            .replace("\u00A0", "")
            .replace("\u202F", "")

        if (clean.isBlank()) return fallbackMillis

        return try {
            val isNegative = clean.startsWith("-")
            val cleanUnsigned = if (isNegative) clean.substring(1).trim() else clean

            val normalized = if (cleanUnsigned.contains('.') && cleanUnsigned.contains(',')) {
                if (cleanUnsigned.lastIndexOf(',') > cleanUnsigned.lastIndexOf('.')) {
                    cleanUnsigned.replace(".", "").replace(',', '.')
                } else {
                    cleanUnsigned.replace(",", "")
                }
            } else if (cleanUnsigned.contains(',')) {
                cleanUnsigned.replace(',', '.')
            } else if (cleanUnsigned.contains('.')) {
                val dotCount = cleanUnsigned.count { it == '.' }
                if (dotCount > 1) {
                    cleanUnsigned.replace(".", "")
                } else {
                    // Ponto único em preço de combustível: funciona como separador decimal das 3 casas da bomba
                    cleanUnsigned
                }
            } else {
                cleanUnsigned
            }

            val doubleVal = normalized.toDoubleOrNull() ?: return fallbackMillis
            val millis = toFuelPriceMillis(doubleVal)
            if (isNegative) -millis else millis
        } catch (_: Exception) {
            fallbackMillis
        }
    }

    /**
     * Calcula o valor total pago do abastecimento em centavos inteiros (Long).
     *
     * Fórmula Financeira:
     * totalCentavos = Math.round((litros * precoPorLitroMilésimos) / 10.0) - descontoCentavos
     *
     * Exemplo:
     * 40.0 litros a R$ 5,899 (5899L) com R$ 2,00 de desconto (200L):
     * raw = Math.round((40.0 * 5899) / 10.0) = 23596L (R$ 235,96)
     * total = 23596L - 200L = 23396L (R$ 233,96).
     */
    fun computeFuelTotalCents(liters: Double, pricePerLiterMillis: Long, discountCents: Long = 0L): Long {
        if (liters <= 0.0 || pricePerLiterMillis <= 0L) return 0L
        val rawCents = Math.round((liters * pricePerLiterMillis) / 10.0)
        return (rawCents - discountCents).coerceAtLeast(0L)
    }

    // =========================================================================
    // 3. OPERAÇÕES ARITMÉTICAS ESCALARES SEGURAS
    // =========================================================================

    /**
     * Rateio proporcional seguro: divide centavos por um divisor inteiro (ex.: dias de trabalho),
     * aplicando arredondamento bancário (HALF_UP).
     * Exemplo: R$ 180,00 em 22 dias = 18000L / 22 = 818 centavos (R$ 8,18/dia).
     */
    fun divideByInt(cents: Long, divisor: Int, roundingMode: RoundingMode = RoundingMode.HALF_UP): Long {
        if (divisor <= 0) return 0L
        return BigDecimal.valueOf(cents)
            .divide(BigDecimal.valueOf(divisor.toLong()), 0, roundingMode)
            .toLong()
    }

    /**
     * Divisão escalar segura por número decimal (ex.: horas trabalhadas).
     * Utilizado para apurar rendimento horário (grossPerHour e netPerHour) com zero ruído.
     * Exemplo: R$ 250,50 (25050L) em 8.5 horas = 25050 / 8.5 = 2947 centavos (R$ 29,47/h).
     */
    fun divideByDouble(cents: Long, divisor: Double, roundingMode: RoundingMode = RoundingMode.HALF_UP): Long {
        if (cents == 0L || divisor <= 0.0 || divisor.isNaN() || divisor.isInfinite()) return 0L
        return BigDecimal.valueOf(cents)
            .divide(BigDecimal.valueOf(divisor), 0, roundingMode)
            .toLong()
    }

    /**
     * Multiplicação escalar segura para cálculo de taxas e desgaste fracionário.
     */
    fun multiplyByDouble(cents: Long, factor: Double, roundingMode: RoundingMode = RoundingMode.HALF_UP): Long {
        if (cents == 0L || factor == 0.0 || factor.isNaN() || factor.isInfinite()) return 0L
        return BigDecimal.valueOf(cents)
            .multiply(BigDecimal.valueOf(factor))
            .setScale(0, roundingMode)
            .toLong()
    }
}
