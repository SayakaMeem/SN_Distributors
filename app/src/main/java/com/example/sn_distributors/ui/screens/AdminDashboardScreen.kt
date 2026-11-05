
package com.example.sn_distributors.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.ui.components.ScrollableDialogBody
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

private val AdminGreen = Color(0xFF0A3420)
private val AdminBackground = Color(0xFFF4F6F5)

@Composable
fun AdminDashboardScreen(
    accounts: List<UserAccount>,
    onCreateStaff: suspend (String, String, String, StaffDepartment) -> Boolean,
    onCreateDistributor: suspend (String, String, String) -> Boolean,
    onLogout: () -> Unit
) {
    var showStaffDialog by remember { mutableStateOf(false) }
    var showDistributorDialog by remember { mutableStateOf(false) }

    val distributors = accounts.filter {
        it.role == UserRole.DISTRIBUTOR &&
                it.status == AccountStatus.ACTIVE
    }

    val staff = accounts.filter {
        it.role == UserRole.STAFF
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = AdminBackground
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "SN Chemical",
                            color = AdminGreen,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Admin Dashboard",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Text(
                            text = "Account and staff management",
                            color = Color.Gray
                        )
                    }

                    TextButton(onClick = onLogout) {
                        Text("Logout")
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminSummaryCard(
                        title = "Accounts",
                        count = accounts.size,
                        modifier = Modifier.weight(1f)
                    )

                    AdminSummaryCard(
                        title = "Distributors",
                        count = distributors.size,
                        modifier = Modifier.weight(1f)
                    )

                    AdminSummaryCard(
                        title = "Staff",
                        count = staff.size,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            item {
                Text("New distributor accounts are active immediately. No account approval is required.")
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminSectionTitle("Staff Members")

                    Button(
                        onClick = { showStaffDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AdminGreen
                        )
                    ) {
                        Text("+ Create Staff")
                    }
                }
            }

            if (staff.isEmpty()) {
                item {
                    AdminEmptyMessage(
                        "No staff accounts have been created."
                    )
                }
            } else {
                items(staff, key = { it.id }) { account ->
                    AdminAccountCard(account)
                }
            }

            item {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AdminSectionTitle("Active Distributors")

                    Button(
                        onClick = {
                            showDistributorDialog = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AdminGreen
                        )
                    ) {
                        Text("+ Create Distributor")
                    }
                }
            }

            if (distributors.isEmpty()) {
                item {
                    AdminEmptyMessage(
                        "No active distributors yet."
                    )
                }
            } else {
                items(distributors, key = { it.id }) { account ->
                    AdminAccountCard(account)
                }
            }
        }
    }

    if (showDistributorDialog) {
        AdminCreateAccountDialog(
            title = "Create Distributor",
            nameLabel = "Distributor Name",
            onDismiss = {
                showDistributorDialog = false
            },
            onCreate = { name, email, password ->
                onCreateDistributor(name, email, password)
            }
        )
    }

    if (showStaffDialog) {
        AdminCreateStaffDialog(
            onDismiss = {
                showStaffDialog = false
            },
            onCreateStaff = onCreateStaff
        )
    }
}

@Composable
private fun AdminSectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 19.sp,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun AdminSummaryCard(
    title: String,
    count: Int,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = count.toString(),
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AdminGreen
            )

            Text(
                text = title,
                fontSize = 11.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
private fun AdminAccountCard(account: UserAccount) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = account.name,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = account.email,
                color = Color.Gray
            )

            Text(
                text = "ID: ${account.id}",
                fontSize = 12.sp
            )

            account.department?.let { department ->
                Text(
                    text = "Department: ${department.name}",
                    color = AdminGreen
                )
            }

            Text(
                text = "Status: ${account.status.name}",
                color = AdminGreen
            )
        }
    }
}

@Composable
private fun AdminEmptyMessage(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Text(
            text = message,
            modifier = Modifier.padding(16.dp),
            color = Color.Gray
        )
    }
}

@Composable
private fun AdminCreateAccountDialog(
    title: String,
    nameLabel: String,
    onDismiss: () -> Unit,
    onCreate: suspend (String, String, String) -> Boolean
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = {
            Text(title)
        },
        text = {
            ScrollableDialogBody(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = ""
                    },
                    label = {
                        Text(nameLabel)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = ""
                    },
                    label = {
                        Text("Email Address")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = ""
                    },
                    label = {
                        Text("Password")
                    },
                    visualTransformation =
                        PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (
                        name.isBlank() ||
                        email.isBlank() ||
                        password.length < 6
                    ) {
                        error =
                            "Enter all fields. Password needs 6+ characters."
                    } else {
                        submitting = true
                        scope.launch {
                            try {
                                if (onCreate(name.trim(), email.trim(), password)) onDismiss()
                                else error = "Invalid email or account already exists."
                            } catch (e: CancellationException) { throw e }
                            catch (_: Exception) { error = "Could not save the account. Please try again." }
                            finally { submitting = false }
                        }
                    }
                },
                enabled = !submitting
            ) {
                Text(if (submitting) "Saving…" else "Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun AdminCreateStaffDialog(
    onDismiss: () -> Unit,
    onCreateStaff: suspend (
        String,
        String,
        String,
        StaffDepartment
    ) -> Boolean
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    var department by remember {
        mutableStateOf(StaffDepartment.SUPERVISOR)
    }

    var expanded by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    var submitting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = {
            Text("Create Staff Account")
        },
        text = {
            ScrollableDialogBody(
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        error = ""
                    },
                    label = {
                        Text("Full Name")
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        error = ""
                    },
                    label = {
                        Text("Email Address")
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Email
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        error = ""
                    },
                    label = {
                        Text("Password")
                    },
                    visualTransformation =
                        PasswordVisualTransformation(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Select Department")

                Box {
                    OutlinedButton(
                        onClick = {
                            expanded = true
                        }
                    ) {
                        Text(department.name)
                    }

                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {
                            expanded = false
                        }
                    ) {
                        StaffDepartment.values().forEach { value ->
                            DropdownMenuItem(
                                text = {
                                    Text(value.name)
                                },
                                onClick = {
                                    department = value
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                if (error.isNotEmpty()) {
                    Text(
                        text = error,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (
                        name.isBlank() ||
                        email.isBlank() ||
                        password.length < 6
                    ) {
                        error = "Enter valid account details."
                    } else {
                        submitting = true
                        scope.launch {
                            try {
                                if (onCreateStaff(name.trim(), email.trim(), password, department)) onDismiss()
                                else error = "Email invalid or already registered."
                            } catch (e: CancellationException) { throw e }
                            catch (_: Exception) { error = "Could not save the account. Please try again." }
                            finally { submitting = false }
                        }
                    }
                },
                enabled = !submitting
            ) {
                Text(if (submitting) "Saving…" else "Create Staff")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) {
                Text("Cancel")
            }
        }
    )
}
