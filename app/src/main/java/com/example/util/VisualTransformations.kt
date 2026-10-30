package com.example.util

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

class BrazilianPhoneVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 11) text.text.substring(0, 11) else text.text
        val out = StringBuilder()

        for (i in trimmed.indices) {
            when (i) {
                0 -> out.append("(").append(trimmed[i])
                1 -> out.append(trimmed[i]).append(") ")
                6 -> out.append(trimmed[i]).append("-")
                else -> out.append(trimmed[i])
            }
        }

        val transformed = out.toString()

        val numberOffsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, trimmed.length)
                val target = when {
                    clamped <= 0 -> 0
                    clamped <= 1 -> clamped + 1 // após "("
                    clamped <= 2 -> clamped + 3 // após ") "
                    clamped <= 7 -> clamped + 4 // após "-"
                    else -> clamped + 4
                }
                return target.coerceIn(0, transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformed.length)
                val orig = when {
                    clamped <= 1 -> 0
                    clamped <= 4 -> clamped - 1
                    clamped <= 10 -> clamped - 3
                    else -> clamped - 4
                }
                return orig.coerceIn(0, trimmed.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), numberOffsetTranslator)
    }
}

class BrazilianCepVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = if (text.text.length >= 8) text.text.substring(0, 8) else text.text
        val out = StringBuilder()

        for (i in trimmed.indices) {
            if (i == 4) {
                out.append(trimmed[i]).append("-")
            } else {
                out.append(trimmed[i])
            }
        }

        val transformed = out.toString()

        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, trimmed.length)
                val target = if (clamped <= 4) clamped else clamped + 1
                return target.coerceIn(0, transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformed.length)
                val orig = if (clamped <= 5) clamped else clamped - 1
                return orig.coerceIn(0, trimmed.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetTranslator)
    }
}

class CurrencyVisualTransformation(val prefix: String = "R$ ") : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        if (text.text.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val transformed = prefix + text.text
        val prefixLen = prefix.length

        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, text.text.length)
                return (clamped + prefixLen).coerceIn(0, transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, transformed.length)
                return (clamped - prefixLen).coerceIn(0, text.text.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetTranslator)
    }
}

class PauseDurationVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isBlank()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val hasColon = raw.contains(":")
        val isAllDigits = raw.all { it.isDigit() }

        if (!hasColon && !isAllDigits) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val (h, m) = if (hasColon) {
            val parts = raw.split(":")
            val rawH = parts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
            val rawM = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: 0
            val totalMinutes = (rawH * 60 + rawM).coerceAtLeast(0)
            Pair(totalMinutes / 60, totalMinutes % 60)
        } else {
            val totalMinutes = (raw.trim().toIntOrNull() ?: 0).coerceAtLeast(0)
            Pair(totalMinutes / 60, totalMinutes % 60)
        }

        val formatted = when {
            h > 0 && m > 0 -> "${if (h == 1) "1 hora" else "$h horas"} e ${if (m == 1) "1 minuto" else "$m minutos"}"
            h > 0 && m == 0 -> if (h == 1) "1 hora" else "$h horas"
            h == 0 && m > 0 -> if (m == 1) "1 minuto" else "$m minutos"
            else -> "0 minuto"
        }

        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                val clamped = offset.coerceIn(0, raw.length)
                if (raw.isEmpty()) return 0
                val transformedOffset = (clamped.toLong() * formatted.length / raw.length).toInt()
                return transformedOffset.coerceIn(0, formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val clamped = offset.coerceIn(0, formatted.length)
                if (formatted.isEmpty()) return 0
                val originalOffset = (clamped.toLong() * raw.length / formatted.length).toInt()
                return originalOffset.coerceIn(0, raw.length)
            }
        }

        return TransformedText(AnnotatedString(formatted), offsetTranslator)
    }
}
