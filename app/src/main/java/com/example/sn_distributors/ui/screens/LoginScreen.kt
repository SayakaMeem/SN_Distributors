
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
import com.example.sn_distributors.data.model.UserRole
import com.example.sn_distributors.data.model.DemoAccounts
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

private val LoginGreen = Color(0xFF0A3420)
private val LoginBackground = Color(0xFFF4F6F5)
private val LoginText = Color(0xFF202938)
private val LoginMuted = Color(0xFF667085)

private fun UserRole.displayName(): String {
    return when (this) {
        UserRole.ADMIN -> "Admin"
        UserRole.STAFF -> "Staff"
        UserRole.DISTRIBUTOR -> "Distributor"
    }
}

@Composable
fun LoginScreen(
    onLogin: suspend (String, String, UserRole) -> Boolean,
    onNavigateToRegister: () -> Unit,
    onBack: () -> Unit = {}
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Admin is selected initially for the default demo account.
    var selectedRole by remember {
        mutableStateOf(UserRole.ADMIN)
    }

    var roleMenuExpanded by remember {
        mutableStateOf(false)
    }

    var passwordVisible by remember {
        mutableStateOf(false)
    }

    var errorMessage by remember {
        mutableStateOf("")
    }
    var submitting by remember { mutableStateOf(false) }
    var demoExpanded by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = LoginBackground
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("Back") }
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

                    // Company logo
                    Box(
                        modifier = Modifier
                            .size(65.dp)
                            .background(
                                LoginGreen,
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "SN",
                            color = Color.White,
                            fontSize = 25.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Welcome Back",
                        fontSize = 25.sp,
                        fontWeight = FontWeight.Bold,
                        color = LoginText
                    )

                    Spacer(Modifier.height(6.dp))

                    Text(
                        text = "Log in to SN ERP Management System",
                        fontSize = 14.sp,
                        color = LoginMuted
                    )

                    Spacer(Modifier.height(26.dp))

                    // Role selection
                    Box {
                        OutlinedButton(onClick = { demoExpanded = true }, enabled = !submitting,
                            modifier = Modifier.fillMaxWidth()) { Text("Use a demo account") }
                        DropdownMenu(expanded = demoExpanded, onDismissRequest = { demoExpanded = false }) {
                            DemoAccounts.logins.forEach { demo ->
                                DropdownMenuItem(text = { Text("${demo.account.name}\n${demo.account.email}") }, onClick = {
                                    email = demo.account.email
                                    password = demo.password
                                    selectedRole = demo.account.role
                                    errorMessage = ""
                                    demoExpanded = false
                                })
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Login as",
                        modifier = Modifier.fillMaxWidth(),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = LoginText
                    )

                    Spacer(Modifier.height(8.dp))

                    Box(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = {
                                roleMenuExpanded = true
                                errorMessage = ""
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = selectedRole.displayName(),
                                color = LoginText,
                                modifier = Modifier.weight(1f)
                            )

                            Text("▼", color = LoginGreen)
                        }

                        DropdownMenu(
                            expanded = roleMenuExpanded,
                            onDismissRequest = {
                                roleMenuExpanded = false
                            }
                        ) {
                            UserRole.values().forEach { role ->
                                DropdownMenuItem(
                                    text = {
                                        Text(role.displayName())
                                    },
                                    onClick = {
                                        selectedRole = role
                                        roleMenuExpanded = false
                                        errorMessage = ""
                                    }
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Email
                    OutlinedTextField(
                        value = email,
                        onValueChange = {
                            email = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Email address")
                        },
                        placeholder = {
                            Text("Enter your email")
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Email
                        ),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(Modifier.height(14.dp))

                    // Password
                    OutlinedTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = ""
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = {
                            Text("Password")
                        },
                        placeholder = {
                            Text("Enter your password")
                        },
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

                    // Error message
                    if (errorMessage.isNotBlank()) {
                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(Modifier.height(24.dp))

                    // Login button
                    Button(
                        onClick = {
                            val cleanEmail = email.trim().lowercase()

                            when {
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

                                password.isBlank() -> {
                                    errorMessage =
                                        "Please enter your password."
                                }

                                else -> {
                                    submitting = true
                                    scope.launch {
                                        try {
                                            if (!onLogin(cleanEmail, password, selectedRole)) {
                                                errorMessage = "Login failed. Check your email, password, selected role, or account status."
                                            }
                                        } catch (e: CancellationException) { throw e }
                                        catch (_: Exception) { errorMessage = "Could not read local accounts. Please try again." }
                                        finally { submitting = false }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        enabled = !submitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = LoginGreen
                        ),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = if (submitting) "Logging in…" else "LOG IN",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Text(
                        text = "Don't have a distributor account?",
                        color = LoginMuted,
                        fontSize = 13.sp
                    )

                    TextButton(
                        onClick = onNavigateToRegister
                    ) {
                        Text(
                            text = "Create a new account",
                            color = LoginGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        text = "SN Chemical • ERP Management",
                        color = Color(0xFF98A2B3),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
