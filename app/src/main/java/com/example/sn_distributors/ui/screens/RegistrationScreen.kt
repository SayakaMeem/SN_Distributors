
package com.example.sn_distributors.ui.screens

import android.util.Patterns
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val RegisterGreen = Color(0xFF0A3420)
private val RegisterBackground = Color(0xFFF4F6F5)
private val RegisterText = Color(0xFF202938)
private val RegisterMuted = Color(0xFF667085)

@Composable
fun RegistrationScreen(
    onRegister: (String, String, String) -> Boolean,
    onNavigateToLogin: () -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }

    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = RegisterBackground
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(5.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    Box(
                        modifier = Modifier
                            .size(65.dp)
                            .background(
                                RegisterGreen,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "SN",
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        "Create Your Account",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = RegisterText
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        "Register to access SN ERP System",
                        fontSize = 14.sp,
                        color = RegisterMuted
                    )

                    Spacer(Modifier.height(26.dp))

                    OutlinedTextField(
                        value = fullName,
                        onValueChange = {
                            fullName = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Full name") },
                        placeholder = { Text("Enter your full name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Text
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Email address") },
                        placeholder = { Text("Enter your email") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Password") },
                        placeholder = { Text("Create a password") },
                        singleLine = true,
                        visualTransformation =
                            if (passwordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    passwordVisible = !passwordVisible
                                }
                            ) {
                                Text(
                                    if (passwordVisible) "Hide" else "Show"
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = {
                            confirmPassword = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm password") },
                        placeholder = { Text("Re-enter your password") },
                        singleLine = true,
                        visualTransformation =
                            if (confirmPasswordVisible) {
                                VisualTransformation.None
                            } else {
                                PasswordVisualTransformation()
                            },
                        trailingIcon = {
                            TextButton(
                                onClick = {
                                    confirmPasswordVisible =
                                        !confirmPasswordVisible
                                }
                            ) {
                                Text(
                                    if (confirmPasswordVisible) {
                                        "Hide"
                                    } else {
                                        "Show"
                                    }
                                )
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    if (errorMessage.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(22.dp))

                    Button(
                        onClick = {
                            val cleanName = fullName.trim()
                            val cleanEmail = email.trim()

                            when {
                                cleanName.isBlank() -> {
                                    errorMessage =
                                        "Please enter your full name."
                                }

                                cleanEmail.isBlank() -> {
                                    errorMessage =
                                        "Please enter your email address."
                                }

                                !Patterns.EMAIL_ADDRESS
                                    .matcher(cleanEmail)
                                    .matches() -> {
                                    errorMessage =
                                        "Please enter a valid email address."
                                }

                                password.length < 6 -> {
                                    errorMessage =
                                        "Password must contain at least 6 characters."
                                }

                                password != confirmPassword -> {
                                    errorMessage =
                                        "Passwords do not match."
                                }

                                else -> {
                                    val success = onRegister(
                                        cleanName,
                                        cleanEmail,
                                        password
                                    )

                                    if (!success) {
                                        errorMessage =
                                            "An account with this email already exists."
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = RegisterGreen
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            "CREATE ACCOUNT",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                    }

                    Spacer(Modifier.height(18.dp))

                    Text(
                        "Already have an account?",
                        color = RegisterMuted,
                        fontSize = 13.sp
                    )

                    TextButton(
                        onClick = onNavigateToLogin
                    ) {
                        Text(
                            "Log in",
                            color = RegisterGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        "SN Chemical • ERP Management",
                        color = Color(0xFF98A2B3),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
