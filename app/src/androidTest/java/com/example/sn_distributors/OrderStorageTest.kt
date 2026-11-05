package com.example.sn_distributors

import androidx.test.platform.app.InstrumentationRegistry
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class OrderStorageTest {
    @Test fun persistedOrdersRetainProductsOwnerApprovalsAndDeliveryHistory() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "order-test-${UUID.randomUUID()}.json")
        try {
            val users = DemoAccounts.logins.map { it.account }
            val distributor = users.first { it.role == UserRole.DISTRIBUTOR }
            val supervisor = users.first { it.department == StaffDepartment.SUPERVISOR }
            val accounts = users.first { it.department == StaffDepartment.ACCOUNTS }
            val factory = users.first { it.department == StaffDepartment.FACTORY }
            val repo = CrmRepository(JsonCrmStorage(file)) { 1000L }
            val task = repo.placeOrder(distributor, listOf(CartItem(ProductCatalog.products.first(), 2)),
                users, "Dhaka warehouse", "01700000000", "Deliver during business hours")
            repo.act(supervisor, task.id, OrderAction.APPROVE, "Approved")
            repo.act(accounts, task.id, OrderAction.APPROVE, "Cleared")
            repo.act(factory, task.id, OrderAction.APPROVE, "Scheduled", 6000)
            repo.receiveMaterial(factory, "Test resin", "kg", 100.0, "Opening stock")
            repo.saveRecipe(factory, task.items.single().product.id, listOf(RecipeLine(repo.manufacturing.value.materials.single().id, 2.5)))
            repo.issuePrint(factory, task.id, PrintKind.TEST)
            repo.issuePrint(factory, task.id, PrintKind.PRODUCTION)
            repo.act(factory, task.id, OrderAction.READY, "Produced")
            repo.act(factory, task.id, OrderAction.DELIVER, "Received")
            val reopened = CrmRepository(JsonCrmStorage(file)) { 1000L }
            assertEquals(repo.tasks.value, reopened.tasks.value)
            assertEquals(repo.manufacturing.value, reopened.manufacturing.value)
            assertEquals(95.0, reopened.manufacturing.value.materials.single().stock, 0.0)
            assertEquals(2, reopened.tasks.value.single().prints.size)
            assertEquals(OrderStage.DELIVERED, reopened.visibleTasks(distributor).single().stage)
            assertEquals(84008.4, reopened.tasks.value.single().totalAmount, 0.01)
            assertEquals("Dhaka warehouse", reopened.tasks.value.single().deliveryAddress)
            assertEquals("Deliver during business hours", reopened.tasks.value.single().orderNote)
        } finally { file.delete() }
    }
}
