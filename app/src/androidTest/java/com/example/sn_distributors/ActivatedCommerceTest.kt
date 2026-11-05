package com.example.sn_distributors

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import com.example.sn_distributors.ui.screens.*
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

class ActivatedCommerceTest {
    @get:Rule val compose = createComposeRule()

    @Test fun distributorCheckoutRoutesThroughRealStaffThenFactoryPrintsAndDelivery() {
        val users = DemoAccounts.logins.map { login ->
            if (login.account.role == UserRole.STAFF) login.account.copy(id = "REAL-${login.account.department}", name = "Company ${login.account.department}") else login.account
        }
        val buyer = users.first { it.role == UserRole.DISTRIBUTOR }
        val supervisor = users.first { it.department == StaffDepartment.SUPERVISOR }
        val accounts = users.first { it.department == StaffDepartment.ACCOUNTS }
        val factory = users.first { it.department == StaffDepartment.FACTORY }
        val repo = CrmRepository(object : CrmStorage {
            var saved = ManufacturingState()
            override fun load() = saved.tasks
            override fun save(tasks: List<CrmTask>) { saved = saved.copy(tasks = tasks) }
            override fun loadState() = saved
            override fun saveState(state: ManufacturingState) { saved = state }
        })
        val products = ProductRepository(object : ProductStorage {
            override fun load() = ProductCatalog.products
            override fun save(products: List<ProductItem>) = Unit
        })
        repo.receiveMaterial(factory, "Test resin", "kg", 100.0, "Test opening stock")
        val material = repo.manufacturing.value.materials.single()
        ProductCatalog.products.forEach { repo.saveRecipe(factory, it.id, listOf(RecipeLine(material.id, 2.0))) }
        val actor = mutableStateOf(buyer)
        val printed = mutableListOf<OrderPrint>()
        compose.setContent {
            MaterialTheme { key(actor.value.id) {
                if (actor.value.role == UserRole.DISTRIBUTOR) CommerceWorkspace(actor.value, users, repo, products, {}, {})
                else OrdersScreen(actor.value, users, repo, {}, printDocument = { _, _, print -> printed.add(print) })
            } }
        }
        compose.onNodeWithText("Shop").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Add to cart"))
        compose.onAllNodesWithText("Add to cart")[0].performClick()
        compose.onNodeWithText("Cart").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText("Proceed to checkout"))
        compose.onNodeWithText("Proceed to checkout").performClick()
        compose.onNodeWithText("Delivery address *").performScrollTo().performTextInput("Dhaka test warehouse")
        compose.onNodeWithText("Place order").performClick()
        compose.waitUntil(15_000) { repo.tasks.value.isNotEmpty() }
        compose.onNodeWithText("Order placed successfully").assertIsDisplayed()
        val id = repo.tasks.value.single().id
        val title = repo.tasks.value.single().title
        assertEquals(supervisor.id, repo.tasks.value.single().assignedUserId)

        fun openAs(user: UserAccount) {
            compose.runOnIdle { actor.value = user }
            compose.waitForIdle()
            compose.onNodeWithText("Remind me in 15 min").assertDoesNotExist()
            compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(title))
            compose.onNodeWithText(title).performClick()
        }
        fun note(text: String) {
            compose.onNodeWithText("Review note / pause reason / outcome *").performScrollTo().performTextClearance()
            compose.onNodeWithText("Review note / pause reason / outcome *").performTextInput(text)
        }
        fun waitStage(stage: OrderStage) { compose.waitUntil(15_000) { repo.tasks.value.single().stage == stage } }

        openAs(supervisor); note("Order requirements checked")
        compose.onNodeWithText("Approve").performScrollTo().performClick(); waitStage(OrderStage.ACCOUNTS)
        assertEquals(accounts.id, repo.tasks.value.single().assignedUserId)
        openAs(accounts); note("Payment cleared")
        compose.onNodeWithText("Approve").performScrollTo().performClick(); waitStage(OrderStage.FACTORY)
        assertEquals(factory.id, repo.tasks.value.single().assignedUserId)
        openAs(factory); note("Factory capacity confirmed")
        compose.onNodeWithText("Approve").performScrollTo().performClick(); waitStage(OrderStage.PRODUCTION)
        assertNotNull(repo.tasks.value.single().expectedDeliveryAt)
        compose.onNodeWithText("Issue test print").performScrollTo().performClick()
        compose.waitUntil(15_000) { printed.any { it.kind == PrintKind.TEST } }
        assertEquals(100.0, repo.manufacturing.value.materials.single().stock, 0.0)
        compose.onNodeWithText("Issue production print").performScrollTo().performClick()
        compose.onNodeWithText("Deduct stock & issue print").performClick()
        compose.waitUntil(15_000) { printed.any { it.kind == PrintKind.PRODUCTION } }
        assertEquals(98.0, repo.manufacturing.value.materials.single().stock, 0.0)
        compose.onNodeWithText("Issue production print").assertDoesNotExist()
        note("Packed for delivery")
        compose.onNodeWithText("Mark ready for delivery").performScrollTo().performClick(); waitStage(OrderStage.READY)
        note("Received by distributor")
        compose.onNodeWithText("Confirm delivery").performScrollTo().performClick(); waitStage(OrderStage.DELIVERED)
        assertEquals(CrmTaskStatus.COMPLETED, repo.workStatus(factory, repo.tasks.value.single()))
        compose.runOnIdle { actor.value = buyer }
        compose.onNodeWithText("My orders").performClick()
        compose.onNode(hasScrollToIndexAction()).performScrollToNode(hasText(title))
        compose.onNodeWithText(title).performClick()
        compose.onNodeWithText("Delivery completed: Done").performScrollTo().assertIsDisplayed()
        assertEquals(id, repo.tasks.value.single().id)
    }
}
