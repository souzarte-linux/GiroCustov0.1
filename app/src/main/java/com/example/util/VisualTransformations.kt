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
