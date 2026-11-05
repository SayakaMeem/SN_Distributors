package com.example.sn_distributors

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.CrmRepository
import com.example.sn_distributors.data.repository.CrmStorage
import com.example.sn_distributors.data.repository.ProductRepository
import com.example.sn_distributors.data.repository.ProductStorage
import com.example.sn_distributors.ui.components.ScrollableDialogBody
import com.example.sn_distributors.ui.screens.*
import org.junit.Rule
import org.junit.Test

class ScreenScrollingTest {
    @get:Rule val compose = createComposeRule()
    private val users = DemoAccounts.logins.map { it.account }
    private fun repository() = CrmRepository(object : CrmStorage {
        override fun load() = emptyList<CrmTask>()
        override fun save(tasks: List<CrmTask>) = Unit
    }) { 1000L }

    @Test fun loginBottomIsReachableInShortViewport() {
        compose.setContent {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 240.dp)) { LoginScreen({ _, _, _ -> false }, {}) } }
        }
        compose.onNodeWithText("Create a new account").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Email address").performScrollTo().assertIsDisplayed()
    }

    @Test fun registrationBottomIsReachableInShortViewport() {
        compose.setContent {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 240.dp)) { RegistrationScreen({ _, _, _ -> false }, {}) } }
        }
        compose.onNodeWithText("Log in").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Full name").performScrollTo().assertIsDisplayed()
    }

    @Test fun loginBottomIsReachableBySwiping() {
        compose.setContent {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 240.dp)) { LoginScreen({ _, _, _ -> false }, {}) } }
        }
        repeat(15) {
            compose.onNode(hasScrollAction()).performTouchInput { swipeUp() }
        }
        compose.onNodeWithText("Create a new account").assertIsDisplayed()
        repeat(15) {
            compose.onNode(hasScrollAction()).performTouchInput { swipeDown() }
        }
        compose.onNodeWithText("Welcome Back").assertIsDisplayed()
    }

    @Test fun dashboardBottomIsReachableBySwiping() {
        val user = users.first { it.role == UserRole.DISTRIBUTOR }
        val repository = repository()
        compose.setContent {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 240.dp)) {
                RoleDashboardScreen(user, users, emptyList(), ProductCatalog.products, repository, 0, {})
            } }
        }
        repeat(15) {
            compose.onNode(hasScrollToIndexAction()).performTouchInput { swipeUp() }
        }
        compose.onNodeWithText("No orders yet.").assertIsDisplayed()
    }

    @Test fun staffWorkflowBottomIsReachableInShortViewport() {
        val staff = UserAccount("staff", "Staff", "staff@sn.com", UserRole.STAFF,
            StaffDepartment.SUPERVISOR, AccountStatus.ACTIVE)
        compose.setContent {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 240.dp)) { StaffDashboardScreen(staff, users, repository(), {}) } }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("No approvals are waiting for your role."))
        compose.onNodeWithText("No approvals are waiting for your role.").assertIsDisplayed()
        compose.onNodeWithText("+ Customer & purchase order").assertDoesNotExist()
    }

    @Test fun adminCanReachLastAccountInLongList() {
        val accounts = List(25) { UserAccount("dst-$it", "Distributor $it", "dst$it@sn.com",
            UserRole.DISTRIBUTOR, status = AccountStatus.ACTIVE) }
        compose.setContent {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 240.dp)) {
                AdminDashboardScreen(accounts, { _, _, _, _ -> false }, { _, _, _ -> false }, {})
            } }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Distributor 24"))
        compose.onNodeWithText("Distributor 24").assertIsDisplayed()
    }

    @Test fun distributorHeaderAndCatalogScrollTogether() {
        compose.setContent {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 240.dp)) {
                CommerceWorkspace(users.first { it.role == UserRole.DISTRIBUTOR }, users, repository(),
                    ProductRepository(object : ProductStorage {
                        override fun load() = ProductCatalog.products
                        override fun save(products: List<ProductItem>) = Unit
                    }), {}, {})
            } }
        }
        compose.onNodeWithText("Shop").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("SN-CRETE MF - BLUE"))
        compose.onNodeWithText("SN-CRETE MF - BLUE").assertIsDisplayed()
    }

    @Test fun longDialogBodyScrollsToLastField() {
        compose.setContent {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 160.dp)) {
                ScrollableDialogBody { repeat(30) { Text("Form field $it") } }
            } }
        }
        compose.onNodeWithText("Form field 29").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Form field 0").performScrollTo().assertIsDisplayed()
    }

    @Test fun crmTaskListAndHistoryAreReachableInShortViewport() {
        val admin = users.first { it.role == UserRole.ADMIN }
        val distributor = users.first { it.role == UserRole.DISTRIBUTOR }
        val repository = repository()
        repeat(25) { repository.placeOrder(distributor, listOf(CartItem(ProductCatalog.products.first(), 1)), users, "Dhaka", "01700000000") }
        val lastTitle = repository.tasks.value.last().title
        compose.setContent {
            MaterialTheme { Box(Modifier.requiredSize(320.dp, 240.dp)) {
                OrdersScreen(admin, users, repository, {})
            } }
        }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(lastTitle))
        compose.onNodeWithText(lastTitle).assertIsDisplayed().performClick()
        compose.onNodeWithText("Activity history · 1 entries").performScrollTo().assertIsDisplayed()
    }
}
