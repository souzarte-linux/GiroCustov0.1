package com.example.ui.screens.auth

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AlternateEmail
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthEmailFormSection(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    isRegisterMode: Boolean,
    onToggleMode: () -> Unit,
    isLoading: Boolean,
    onSubmit: () -> Unit
) {
    var passwordVisible by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            label = { Text("E-mail profissional") },
            placeholder = { Text("exemplo@email.com", color = ColorTextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.AlternateEmail,
                    contentDescription = null,
                    tint = ColorTextMuted
                )
            },
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorSurfaceInput,
                unfocusedContainerColor = ColorSurfaceInput,
                focusedBorderColor = ColorBrandEmerald,
                unfocusedBorderColor = ColorBorder,
                focusedTextColor = ColorTextPrimary,
                unfocusedTextColor = ColorTextPrimary,
                focusedLabelColor = ColorBrandEmerald,
                unfocusedLabelColor = ColorTextMuted
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_email_input")
                .semantics { testTag = "auth_gate_email_input" }
        )

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("Senha") },
            placeholder = { Text("••••••••", color = ColorTextMuted) },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    contentDescription = null,
                    tint = ColorTextMuted
                )
            },
            trailingIcon = {
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.testTag("auth_password_toggle_visibility")
                ) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                        contentDescription = if (passwordVisible) "Ocultar senha" else "Ver senha",
                        tint = ColorTextMuted
                    )
                }
            },
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            singleLine = true,
            enabled = !isLoading,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = { onSubmit() }
            ),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = ColorSurfaceInput,
                unfocusedContainerColor = ColorSurfaceInput,
                focusedBorderColor = ColorBrandEmerald,
                unfocusedBorderColor = ColorBorder,
                focusedTextColor = ColorTextPrimary,
                unfocusedTextColor = ColorTextPrimary,
                focusedLabelColor = ColorBrandEmerald,
                unfocusedLabelColor = ColorTextMuted
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_password_input")
                .semantics { testTag = "auth_gate_password_input" }
        )

        Button(
            onClick = onSubmit,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("auth_submit_email_btn")
                .semantics { testTag = "auth_gate_submit_btn" },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ColorBrandEmerald,
                contentColor = ColorOnPrimary
            )
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(24.dp)
                        .testTag("auth_loading_indicator"),
                    color = ColorOnPrimary,
                    strokeWidth = 2.5.dp
                )
            } else {
                Text(
                    text = if (isRegisterMode) "Cadastrar Gratuitamente" else "Entrar no GiroCusto",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        TextButton(
            onClick = onToggleMode,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("auth_toggle_mode_btn")
                .semantics { testTag = "auth_gate_tab_register" }
        ) {
            Text(
                text = if (isRegisterMode) "Já tem conta? Entrar agora" else "Não tem conta? Cadastre-se grátis",
                color = ColorBrandEmerald,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}
