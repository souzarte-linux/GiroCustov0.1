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
    ),
    SOLAR_NAUTICO(
        id = "SOLAR_NAUTICO",
        displayName = "Solar & Náutico",
        description = "Azul náutico profundo e realces solares de alta visibilidade sob claridade externa intensa.",
        primaryColor = Color(0xFF2563EB),
        secondaryColor = Color(0xFFEAB308),
        tertiaryColor = Color(0xFF10B981),
        surfaceColorDark = Color(0xFF131B2E)
    );

    companion object {
        fun fromId(id: String?): AppTheme {
            return entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: EMERALD_GIROCUSTO
        }
    }
}
