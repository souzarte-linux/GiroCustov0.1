package com.example.ui.screens.platforms

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
internal fun PlatformSectionAccordionHeader(
    title: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) 180f else 0f,
        animationSpec = tween(durationMillis = 200),
        label = "chevron_rotation"
    )
    val expandedDesc = stringResource(R.string.cd_section_expanded, title)
    val collapsedDesc = stringResource(R.string.cd_section_collapsed, title)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clickable(
                onClick = onToggle,
                role = Role.Button
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .semantics {
                contentDescription = if (isExpanded) expandedDesc else collapsedDesc
                role = Role.Button
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Icon(
            imageVector = Icons.Filled.KeyboardArrowDown,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(24.dp)
                .rotate(rotation)
        )
    }
}

@Composable
internal fun PlatformGeneralViewContent(
    name: String,
    segment: String,
    paymentModel: String,
    cycle: String,
    paymentDay: String,
    fixedPayDelay: String,
    cycleEntriesCount: Int
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text("Nome da plataforma", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            text = name.ifBlank { "Sem nome" },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        val isLogistica = segment == "logistica"
        Badge(
            containerColor = if (isLogistica) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Text(
                text = if (isLogistica) stringResource(R.string.platform_segment_logistics) else stringResource(R.string.platform_segment_delivery),
                color = if (isLogistica) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
        val isProducao = paymentModel == "producao"
        Badge(
            containerColor = if (isProducao) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(
                text = if (isProducao) stringResource(R.string.platform_model_production_short) else stringResource(R.string.platform_model_daily_short),
                color = if (isProducao) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                softWrap = false
            )
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("Ciclo de Pagamento", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val cycleSummary = when (cycle) {
            "semanal" -> "Ciclo Semanal • Fechamento: $paymentDay • Prazo: $fixedPayDelay dias"
            "quinzenal" -> "Ciclo Quinzenal • Prazo: $fixedPayDelay dias após fechamento"
            "mensal" -> "Ciclo Mensal • Prazo: $fixedPayDelay dias após fechamento"
            else -> "Ciclo Variável • $cycleEntriesCount ciclo(s) configurado(s)"
        }
        Text(
            text = cycleSummary,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun PlatformBankingViewContent(
    bankName: String,
    bankAgency: String,
    bankAccount: String
) {
    if (bankName.isNotBlank() || bankAgency.isNotBlank() || bankAccount.isNotBlank()) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (bankName.isNotBlank()) {
                Text(bankName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            }
            val details = listOfNotNull(
                if (bankAgency.isNotBlank()) "Agência: $bankAgency" else null,
                if (bankAccount.isNotBlank()) "Conta: $bankAccount" else null
            ).joinToString(" • ")
            if (details.isNotBlank()) {
                Text(details, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    } else {
        Text(
            text = "Nenhum dado bancário cadastrado",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
internal fun PlatformPixViewContent(
    pixKeyType: String,
    pixKey: String
) {
    if (pixKey.isNotBlank()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Badge(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = pixKeyType,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = pixKey,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    } else {
        Text(
            text = "Nenhuma chave PIX cadastrada",
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
