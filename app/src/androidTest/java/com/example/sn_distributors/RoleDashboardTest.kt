package com.example.sn_distributors

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import com.example.sn_distributors.ui.screens.CommerceWorkspace
import org.junit.Rule
import org.junit.Test

class RoleDashboardTest {
    @get:Rule val compose = createComposeRule()
    private val users = DemoAccounts.logins.map { it.account }
    private val repo = CrmRepository(object : CrmStorage {
        override fun load() = emptyList<CrmTask>()
        override fun save(tasks: List<CrmTask>) = Unit
    })
    private val products = ProductRepository(object : ProductStorage {
        override fun load() = ProductCatalog.products
        override fun save(products: List<ProductItem>) = Unit
    })

    private fun open(user: UserAccount) {
        compose.setContent { MaterialTheme { CommerceWorkspace(user, users, repo, products, {}, {}) } }
        compose.onNodeWithText("Dashboard").assertIsSelected()
    }

    @Test fun distributorSummaryExcludesOtherDistributorsOrders() {
        val distributor = users.first { it.role == UserRole.DISTRIBUTOR }
        val own = repo.placeOrder(distributor, listOf(CartItem(ProductCatalog.products.first(), 1)), users, "Dhaka", "01700000000")
        val other = repo.placeOrder(distributor.copy(id = "other", name = "Other distributor"),
            listOf(CartItem(ProductCatalog.products.first(), 1)), users, "Dhaka", "01700000000")
        open(distributor)
        compose.onNodeWithText("My total orders").onParent().assert(hasAnyDescendant(hasText("1")))
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(own.title))
        compose.onNodeWithText(own.title).assertIsDisplayed()
        compose.onNodeWithText(other.title).assertDoesNotExist()
        compose.onNodeWithText("Manage accounts").assertDoesNotExist()
    }

    @Test fun accountsDashboardShowsFinancialQueue() {
        open(users.first { it.department == StaffDepartment.ACCOUNTS })
        compose.onNodeWithText("Accounts dashboard").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Value awaiting my clearance"))
        compose.onNodeWithText("Value awaiting my clearance").assertIsDisplayed()
        compose.onNodeWithText("My production orders").assertDoesNotExist()
    }

    @Test fun factoryDashboardShowsProductionAndDeliveryQueue() {
        open(users.first { it.department == StaffDepartment.FACTORY })
        compose.onNodeWithText("Factory dashboard").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("My production orders"))
        compose.onNodeWithText("My production orders").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("My ready deliveries"))
        compose.onNodeWithText("My ready deliveries").assertIsDisplayed()
    }
}
