package com.example.sn_distributors

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import com.example.sn_distributors.data.model.UserRole
import org.junit.Rule
import org.junit.Test

class MainNavigationTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()

    private fun login(email: String, password: String, role: UserRole) {
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Email address").fetchSemanticsNodes().isNotEmpty() }
        if (role != UserRole.ADMIN) {
            compose.onNodeWithText("Admin").performScrollTo().performClick()
            compose.onNodeWithText(if (role == UserRole.STAFF) "Staff" else "Distributor").performClick()
        }
        compose.onNodeWithText("Email address").performScrollTo().performTextInput(email)
        compose.onNodeWithText("Password").performScrollTo().performTextInput(password)
        compose.onNodeWithText("LOG IN").performScrollTo().performClick()
    }

    @Test fun distributorMovesFromLoginThroughCartCheckoutAndOrders() {
        login("demo.distributor@sn.com", "Distributor@123", UserRole.DISTRIBUTOR)
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Shop").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Distributor dashboard").assertIsDisplayed()
        compose.onNodeWithText("Shop").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Dashboard").assertIsSelected()
        compose.onNodeWithText("Shop").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Add to cart"))
        compose.onAllNodesWithText("Add to cart")[0].performClick()
        compose.onNodeWithText("Cart").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Proceed to checkout"))
        compose.onNodeWithText("Proceed to checkout").performClick()
        compose.onNodeWithText("Delivery address *").performScrollTo().performTextInput("Navigation test warehouse, Dhaka")
        compose.onNodeWithText("Place order").performClick()
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Checkout").fetchSemanticsNodes().isEmpty() }
        compose.onNodeWithText("My orders").assertIsSelected()
        compose.onNodeWithText("Distributor orders").assertIsDisplayed()
        compose.onNodeWithText("Shop").performClick()
        compose.onNodeWithText("Search products").assertIsDisplayed()
    }

    @Test fun adminMovesBetweenOrdersProductsAndAccounts() {
        login("admin@sn.com", "Admin@123", UserRole.ADMIN)
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Orders").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Products").performClick()
        compose.onNodeWithText("Manage products").assertIsDisplayed()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.onNodeWithText("Dashboard").assertIsSelected()
        compose.onNodeWithText("Admin dashboard").assertIsDisplayed()
        compose.onNodeWithText("Accounts").performClick()
        compose.onNodeWithText("Admin Dashboard").assertIsDisplayed()
        compose.onNodeWithText("Orders").performClick()
        compose.onNodeWithText("Distributor order overview").assertIsDisplayed()
    }

    @Test fun staffMovesBetweenApprovalsProductsAndDistributors() {
        login("demo.supervisor@sn.com", "Supervisor@123", UserRole.STAFF)
        compose.waitUntil(30_000) { compose.onAllNodesWithText("Approvals").fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithText("Supervisor dashboard").assertIsDisplayed()
        compose.onNodeWithText("Remind me in 15 min").assertDoesNotExist()
        compose.onNodeWithText("Products").performClick()
        compose.onNodeWithText("Manage products").assertIsDisplayed()
        compose.onNodeWithText("Distributors").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("View orders"))
        compose.onAllNodesWithText("View orders")[0].performClick()
        compose.onNodeWithText("Approvals").assertIsSelected()
    }
}
