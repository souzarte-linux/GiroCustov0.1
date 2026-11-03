package com.example.ui.screens

import java.util.Locale

fun formatHistoryCurrency(amount: Double, isPrivacyEnabled: Boolean): String {
    return if (isPrivacyEnabled) {
        "R$ ••••••"
    } else {
        String.format(Locale.GERMAN, "R$ %,.2f", amount)
    }
}

