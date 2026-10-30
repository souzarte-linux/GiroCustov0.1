package com.example.ui.screens.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

@Composable
internal fun OnboardingHeaderSection(
    errorMessage: String?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(modifier = Modifier.height(4.dp))

        // HEADER DE PROGRESSO & BOAS-VINDAS
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(ColorEmeraldBadgeBg)
                .padding(horizontal = 10.dp, vertical = 5.dp)
        ) {
            Text(
                text = stringResource(R.string.onboarding_step_badge),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = ColorEmeraldBadgeText,
                letterSpacing = 1.sp
            )
        }

        Text(
            text = stringResource(R.string.onboarding_title),
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = ColorTextPrimary
        )

        Text(
            text = stringResource(R.string.onboarding_subtitle),
            fontSize = 13.sp,
            color = ColorTextMuted,
            lineHeight = 18.sp
        )

        // Mensagem de erro de validação geral
        AnimatedVisibility(visible = errorMessage != null) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = ColorErrorBg,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, ColorError)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Error,
                        contentDescription = null,
                        tint = ColorError,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = errorMessage.orEmpty(),
                        color = ColorError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}
