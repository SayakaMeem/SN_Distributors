package com.example.sn_distributors

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.unit.dp
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import com.example.sn_distributors.ui.components.ScreenViewport
import com.example.sn_distributors.ui.screens.*
import com.example.sn_distributors.ui.theme.SN_DistributorsTheme
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {
    private val accountRepository by lazy { AccountRepository(applicationContext) }
    private val productStorage by lazy { SqliteProductStorage(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SN_DistributorsTheme {
                var page by rememberSaveable { mutableStateOf("login") }
                var loggedInId by rememberSaveable { mutableStateOf<String?>(null) }
                val focusManager = LocalFocusManager.current
                val keyboard = LocalSoftwareKeyboardController.current
                LaunchedEffect(page, loggedInId) {
                    focusManager.clearFocus()
                    keyboard?.hide()
                }
                val accounts by accountRepository.accounts.collectAsState()
                var catalog by remember { mutableStateOf<ProductRepository?>(null) }
                var orders by remember { mutableStateOf<CrmRepository?>(null) }
                var ready by remember { mutableStateOf(false) }
                var loadError by remember { mutableStateOf<String?>(null) }
                var attempt by remember { mutableIntStateOf(0) }

                LaunchedEffect(attempt) {
                    ready = false
                    loadError = null
                    try {
                        val loaded = withContext(Dispatchers.IO) {
                            accountRepository.initialize()
                            val products = ProductRepository(productStorage)
                            // Keep the existing file path so older purchase records remain available.
                            val purchaseOrders = CrmRepository(JsonCrmStorage(File(filesDir, "crm-tasks.json")),
                                catalog = { products.products.value })
                            products to purchaseOrders
                        }
                        catalog = loaded.first
                        orders = loaded.second
                        ready = true
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { loadError = "Could not open local store data. ${e.message.orEmpty()}" }
                }

                fun logout() { loggedInId = null; page = "login" }
                BackHandler(enabled = page == "register" && loggedInId == null) { page = "login" }
                val user = accounts.firstOrNull { it.id == loggedInId && it.isActive }
                val products = catalog
                val purchaseOrders = orders

                ScreenViewport {
                    when {
                        !ready -> Column(
                            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
                            verticalArrangement = Arrangement.Center
                        ) {
                            if (loadError == null) {
                                CircularProgressIndicator()
                                Text("Opening SN Chemical…")
                            } else {
                                Text(loadError.orEmpty())
                                Button(onClick = { attempt++ }) { Text("Retry") }
                            }
                        }
                        user != null && products != null && purchaseOrders != null -> {
                            key(user.id) {
                                CommerceWorkspace(user, accounts, purchaseOrders, products, { logout() }) {
                                    AdminDashboardScreen(
                                        accounts = accounts,
                                        onCreateStaff = { name, email, password, department ->
                                            accountRepository.createByAdmin(user.id, name, email, password, UserRole.STAFF, department)
                                        },
                                        onCreateDistributor = { name, email, password ->
                                            accountRepository.createByAdmin(user.id, name, email, password, UserRole.DISTRIBUTOR)
                                        },
                                        onLogout = { logout() }
                                    )
                                }
                            }
                        }
                        page == "register" -> RegistrationScreen(
                            onRegister = { name, email, password ->
                                val success = accountRepository.registerDistributor(name, email, password)
                                if (success) page = "login"
                                success
                            },
                            onNavigateToLogin = { page = "login" }
                        )
                        else -> LoginScreen(
                            onLogin = { email, password, role ->
                                val account = accountRepository.authenticate(email, password, role)
                                if (account != null) { loggedInId = account.id; page = "login" }
                                account != null
                            },
                            onNavigateToRegister = { page = "register" },
                            onBack = { finish() }
                        )
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        accountRepository.close()
        productStorage.close()
    }
}
