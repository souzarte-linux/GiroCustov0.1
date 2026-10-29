package com.example.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.util.BrazilianPhoneVisualTransformation

@Composable
fun AuthPhoneFormSection(
    phone: String,
    onPhoneChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = phone,
            onValueChange = { input ->
                val digits = input.filter { it.isDigit() }
                if (digits.length <= 11) {
                    onPhoneChange(digits)
                }
            },
            label = { Text("Número do Celular") },
            placeholder = { Text("(11) 98765-4321", color = ColorTextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = ColorTextMuted
                )
            },
            visualTransformation = BrazilianPhoneVisualTransformation(),
            singleLine = true,
            enabled = false,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions.Default,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorSurfaceInput,
                unfocusedContainerColor = ColorSurfaceInput,
                disabledContainerColor = ColorSurfaceInput,
                focusedBorderColor = ColorBrandEmerald,
                unfocusedBorderColor = ColorBorder,
                disabledBorderColor = ColorBorder,
                focusedTextColor = ColorTextPrimary,
                unfocusedTextColor = ColorTextPrimary,
                disabledTextColor = ColorTextMuted,
                focusedLabelColor = ColorBrandEmerald,
                unfocusedLabelColor = ColorTextMuted,
                disabledLabelColor = ColorTextMuted,
                disabledLeadingIconColor = ColorTextMuted
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_phone_input")
        )

        Text(
            text = stringResource(R.string.auth_sms_coming_soon_notice),
            fontSize = 12.sp,
            color = ColorTextMuted,
            lineHeight = 16.sp
        )

        Button(
            onClick = {},
            enabled = false,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("auth_submit_phone_btn"),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorBrandEmerald,
                contentColor = ColorOnPrimary,
                disabledContainerColor = ColorSurfaceInput,
                disabledContentColor = ColorTextMuted
            )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Send,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = stringResource(R.string.auth_btn_sms_coming_soon),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
