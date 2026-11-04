
package com.example.sn_distributors

import android.os.Bundle
import android.util.Patterns
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.ui.screens.*
import com.example.sn_distributors.ui.theme.SN_DistributorsTheme
import java.util.UUID

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            SN_DistributorsTheme {

                var currentScreen by remember {
                    mutableStateOf("login")
                }

                var loggedInEmail by remember {
                    mutableStateOf<String?>(null)
                }

                val accounts = remember {
                    mutableStateMapOf<String, UserAccount>().apply {
                        put(
                            "admin@sn.com",
                            UserAccount(
                                id = "ADM-0001",
                                name = "System Admin",
                                email = "admin@sn.com",
                                role = UserRole.ADMIN,
                                status = AccountStatus.ACTIVE
                            )
                        )
                    }
                }

                // Development-only login credentials.
                // Do not use plaintext passwords in production.
                val passwords = remember {
                    mutableStateMapOf(
                        "admin@sn.com" to "Admin@123"
                    )
                }

                fun createAccount(
                    name: String,
                    email: String,
                    password: String,
                    role: UserRole,
                    status: AccountStatus,
                    department: StaffDepartment? = null
                ): Boolean {
                    val cleanEmail = email.trim().lowercase()

                    val validEmail = Patterns.EMAIL_ADDRESS
                        .matcher(cleanEmail)
                        .matches()

                    if (
                        name.isBlank() ||
                        !validEmail ||
                        password.length < 6 ||
                        accounts.containsKey(cleanEmail)
                    ) {
                        return false
                    }

                    val prefix = when (role) {
                        UserRole.ADMIN -> "ADM"
                        UserRole.STAFF -> "STF"
                        UserRole.DISTRIBUTOR -> "DST"
                    }

                    val accountId =
                        "$prefix-${UUID.randomUUID().toString().take(8).uppercase()}"

                    accounts[cleanEmail] = UserAccount(
                        id = accountId,
                        name = name.trim(),
                        email = cleanEmail,
                        role = role,
                        department = department,
                        status = status
                    )

                    passwords[cleanEmail] = password
                    return true
                }

                fun logout() {
                    loggedInEmail = null
                    currentScreen = "login"
                }

                BackHandler(enabled = currentScreen != "login") {
                    if (currentScreen == "register") {
                        currentScreen = "login"
                    } else {
                        logout()
                    }
                }

                when (currentScreen) {

                    "login" -> {
                        LoginScreen(
                            onLogin = { email, password, role ->
                                val cleanEmail =
                                    email.trim().lowercase()

                                val account = accounts[cleanEmail]

                                val valid =
                                    account != null &&
                                            passwords[cleanEmail] == password &&
                                            account.role == role &&
                                            account.status == AccountStatus.ACTIVE

                                if (valid) {
                                    loggedInEmail = cleanEmail

                                    currentScreen = when (role) {
                                        UserRole.ADMIN -> "admin"
                                        UserRole.STAFF -> "staff"
                                        UserRole.DISTRIBUTOR -> "portal"
                                    }
                                }

                                valid
                            },
                            onNavigateToRegister = {
                                currentScreen = "register"
                            }
                        )
                    }

                    "register" -> {
                        RegistrationScreen(
                            onRegister = { name, email, password ->
                                val created = createAccount(
                                    name = name,
                                    email = email,
                                    password = password,
                                    role = UserRole.DISTRIBUTOR,
                                    status = AccountStatus.PENDING
                                )

                                if (created) {
                                    currentScreen = "login"
                                }

                                created
                            },
                            onNavigateToLogin = {
                                currentScreen = "login"
                            }
                        )
                    }

                    "admin" -> {
                        val admin = loggedInEmail?.let {
                            accounts[it]
                        }

                        if (
                            admin == null ||
                            admin.role != UserRole.ADMIN ||
                            admin.status != AccountStatus.ACTIVE
                        ) {
                            logout()
                        } else {
                            AdminDashboardScreen(
                                accounts = accounts.values.toList(),

                                onApproveDistributor = { id ->
                                    val entry = accounts.entries.firstOrNull {
                                        it.value.id == id &&
                                                it.value.role == UserRole.DISTRIBUTOR &&
                                                it.value.status == AccountStatus.PENDING
                                    }

                                    if (entry != null) {
                                        accounts[entry.key] = entry.value.copy(
                                            status = AccountStatus.ACTIVE
                                        )
                                    }
                                },

                                onRejectDistributor = { id ->
                                    val entry = accounts.entries.firstOrNull {
                                        it.value.id == id &&
                                                it.value.role == UserRole.DISTRIBUTOR &&
                                                it.value.status == AccountStatus.PENDING
                                    }

                                    if (entry != null) {
                                        accounts[entry.key] = entry.value.copy(
                                            status = AccountStatus.REJECTED
                                        )
                                    }
                                },

                                onCreateStaff = {
                                        name, email, password, department ->
                                    createAccount(
                                        name = name,
                                        email = email,
                                        password = password,
                                        role = UserRole.STAFF,
                                        status = AccountStatus.ACTIVE,
                                        department = department
                                    )
                                },

                                onCreateDistributor = {
                                        name, email, password ->
                                    createAccount(
                                        name = name,
                                        email = email,
                                        password = password,
                                        role = UserRole.DISTRIBUTOR,
                                        status = AccountStatus.ACTIVE
                                    )
                                },

                                onLogout = {
                                    logout()
                                }
                            )
                        }
                    }

                    "staff" -> {
                        val staffAccount = loggedInEmail?.let {
                            accounts[it]
                        }

                        if (
                            staffAccount != null &&
                            staffAccount.role == UserRole.STAFF &&
                            staffAccount.status == AccountStatus.ACTIVE
                        ) {
                            StaffDashboardScreen(
                                staff = staffAccount,
                                onLogout = { logout() }
                            )
                        } else {
                            logout()
                        }
                    }

                    "portal" -> {
                        val distributorAccount = loggedInEmail?.let {
                            accounts[it]
                        }

                        if (
                            distributorAccount != null &&
                            distributorAccount.role == UserRole.DISTRIBUTOR &&
                            distributorAccount.status == AccountStatus.ACTIVE
                        ) {
                            DistributorPortalScreen(
                                modifier = Modifier.fillMaxSize(),
                                distributorToken = distributorAccount.id
                            )
                        } else {
                            logout()
                        }
                    }

                    else -> {
                        logout()
                    }
                }
            }
        }
    }
}
