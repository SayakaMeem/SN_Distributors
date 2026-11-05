package com.example.sn_distributors

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import com.example.sn_distributors.ui.screens.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class RoleApprovalUiTest {
    @get:Rule val compose = createComposeRule()
    private val users = DemoAccounts.logins.map { it.account }
    private fun repository() = CrmRepository(object : CrmStorage {
        override fun load() = emptyList<CrmTask>()
        override fun save(tasks: List<CrmTask>) = Unit
    }) { 1000L }
    private fun products() = ProductRepository(object : ProductStorage {
        var saved = ProductCatalog.products
        override fun load() = saved
        override fun save(products: List<ProductItem>) { saved = products }
    })

    @Test fun supervisorCanPauseThenApproveWithoutRemindersOrCustomerEntryForm() {
        val repo = repository()
        val supervisor = users.first { it.department == StaffDepartment.SUPERVISOR }
        val task = repo.placeOrder(users.first { it.role == UserRole.DISTRIBUTOR },
            listOf(CartItem(ProductCatalog.products.first(), 1)), users, "Dhaka", "buyer@example.com")
        compose.setContent { MaterialTheme { OrdersScreen(supervisor, users, repo, {}) } }
        compose.onNodeWithText("Remind me in 15 min").assertDoesNotExist()
        compose.onNodeWithText("+ Customer & purchase order").assertDoesNotExist()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(task.title))
        compose.onNodeWithText(task.title).performClick()
        compose.onNodeWithText("Customer name *").assertDoesNotExist()
        compose.onNodeWithText("Review note / pause reason / outcome *").performScrollTo().performTextInput("Waiting for confirmation")
        compose.onNodeWithText("Start review").assertDoesNotExist()
        compose.onNodeWithText("Reject").assertDoesNotExist()
        compose.onNodeWithText("Pause").performScrollTo().performClick()
        compose.waitUntil(15_000) { repo.tasks.value.single().isPaused }
        assertEquals(OrderStage.SUPERVISOR, repo.tasks.value.single().stage)
        compose.onNodeWithText("Pause").performScrollTo().assertIsNotEnabled()
        compose.onNodeWithText("Approve").performScrollTo().performClick()
        compose.waitUntil(15_000) { repo.tasks.value.single().stage == OrderStage.ACCOUNTS }
        assertEquals(CrmTaskStatus.COMPLETED, repo.workStatus(supervisor, repo.tasks.value.single()))
    }

    @Test fun distributorCanCopyFullOrderIdAndReadCircularOrderTotal() {
        val repo = repository()
        val buyer = users.first { it.role == UserRole.DISTRIBUTOR }
        val task = repo.placeOrder(buyer, listOf(CartItem(ProductCatalog.products.first(), 1)), users, "Dhaka", "01700000000")
        compose.setContent { MaterialTheme { OrdersScreen(buyer, users, repo, {}) } }
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasContentDescription("Order total BDT 42,004.20"))
        compose.onNodeWithContentDescription("Order total BDT 42,004.20").assertIsDisplayed()
        compose.onNodeWithText("1 unit").assertIsDisplayed()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Copy order ID"))
        compose.onNodeWithText("Copy order ID").performClick()
        compose.onNodeWithText("Order ID copied").assertIsDisplayed()
        compose.runOnIdle {
            val context = androidx.test.platform.app.InstrumentationRegistry.getInstrumentation().targetContext
            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
            assertEquals(task.id, clipboard.primaryClip?.getItemAt(0)?.text?.toString())
        }
    }

    @Test fun distributorChecksOutFromShoppingCartWithoutCustomerFollowupForm() {
        val repo = repository()
        val distributor = users.first { it.role == UserRole.DISTRIBUTOR }
        val products = products()
        compose.setContent { MaterialTheme { CommerceWorkspace(distributor, users, repo, products, {}, {}) } }
        compose.onNodeWithText("Shop").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Add to cart"))
        compose.onAllNodesWithText("Add to cart")[0].performClick()
        compose.onNodeWithText("Cart").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Proceed to checkout"))
        compose.onNodeWithText("Proceed to checkout").performClick()
        compose.onNodeWithText("Customer name *").assertDoesNotExist()
        compose.onNodeWithText("Delivery address *").performScrollTo().performTextInput("Dhaka warehouse")
        compose.onNodeWithText("Place order").performClick()
        compose.waitUntil(15_000) { repo.tasks.value.isNotEmpty() }
        assertEquals(distributor.id, repo.tasks.value.single().distributorId)
        assertEquals(OrderStage.SUPERVISOR, repo.tasks.value.single().stage)
        assertEquals("Dhaka warehouse", repo.tasks.value.single().deliveryAddress)
    }

    @Test fun staffCanAddCatalogProductWithoutCustomerFields() {
        val products = products()
        val staff = users.first { it.role == UserRole.STAFF }
        compose.setContent { MaterialTheme { CommerceWorkspace(staff, users, repository(), products, {}, {}) } }
        compose.onNodeWithText("Products").performClick()
        compose.onNodeWithText("+ Add product").performClick()
        compose.onNodeWithText("Product name *").performScrollTo().performTextInput("Test coating")
        compose.onNodeWithText("Product code / SKU *").performScrollTo().performTextInput("TEST-UI")
        compose.onNodeWithText("List price (BDT) *").performScrollTo().performTextInput("600")
        compose.onNodeWithText("Selling price (BDT) *").performScrollTo().performTextInput("500")
        compose.onNodeWithText("Unit, e.g. bag / bucket / set *").performScrollTo().performTextInput("bag")
        compose.onNodeWithText("Save product").performClick()
        compose.waitUntil(15_000) { products.products.value.any { it.code == "TEST-UI" } }
        compose.onNodeWithText("Customer name *").assertDoesNotExist()
    }
}
