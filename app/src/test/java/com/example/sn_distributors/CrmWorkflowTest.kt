package com.example.sn_distributors

import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import org.junit.Assert.*
import org.junit.Test

class CrmWorkflowTest {
    private class MemoryStorage : CrmStorage {
        var saved = emptyList<CrmTask>()
        var failWrites = false
        var snapshot = ManufacturingState()
        override fun load() = saved
        override fun save(tasks: List<CrmTask>) {
            if (failWrites) throw java.io.IOException("Disk full")
            saved = tasks
        }
        override fun loadState() = snapshot.copy(tasks = saved)
        override fun saveState(state: ManufacturingState) {
            save(state.tasks)
            snapshot = state
        }
    }
    private val users = DemoAccounts.logins.map { it.account }
    private val admin = users.first { it.role == UserRole.ADMIN }
    private val supervisor = users.first { it.department == StaffDepartment.SUPERVISOR }
    private val finance = users.first { it.department == StaffDepartment.ACCOUNTS }
    private val factory = users.first { it.department == StaffDepartment.FACTORY }
    private val distributor = users.first { it.role == UserRole.DISTRIBUTOR }
    private val items = listOf(CartItem(ProductCatalog.products[1], 2))
    private fun repository(storage: CrmStorage = MemoryStorage()) = CrmRepository(storage) { 1000L }
    private fun submit(repo: CrmRepository, buyer: UserAccount = distributor) = repo.placeOrder(buyer, items, users, "Dhaka warehouse", "01700000000")

    @Test fun onlyDistributorsCanSubmitAndEachDistributorSeesOwnOrders() {
        val repo = repository()
        listOf(admin, supervisor, finance, factory).forEach { actor ->
            assertThrows(IllegalArgumentException::class.java) { submit(repo, actor) }
        }
        val task = submit(repo)
        val otherDistributor = distributor.copy(id = "other")
        assertEquals(listOf(task), repo.visibleTasks(distributor))
        assertTrue(repo.visibleTasks(otherDistributor).isEmpty())
        listOf(admin, supervisor, finance, factory).forEach { assertEquals(listOf(task), repo.visibleTasks(it)) }
        assertTrue(repo.visibleTasks(supervisor.copy(status = AccountStatus.SUSPENDED)).isEmpty())
    }

    @Test fun completeFlowRoutesInOrderAndRetainsCompletedStaffWork() {
        val storage = MemoryStorage()
        val repo = repository(storage)
        val task = submit(repo)
        assertEquals(OrderStage.SUPERVISOR, task.stage)
        assertEquals(CrmTaskStatus.PENDING, repo.workStatus(supervisor, task))
        assertNull(repo.workStatus(finance, task))
        repo.act(supervisor, task.id, OrderAction.START_REVIEW, "Checking requirements")
        assertEquals(CrmTaskStatus.IN_PROGRESS, repo.tasks.value.single().status)
        repo.act(supervisor, task.id, OrderAction.APPROVE, "Needs and products checked")
        val accountsTask = repo.tasks.value.single()
        assertEquals(OrderStage.ACCOUNTS, accountsTask.stage)
        assertEquals(finance.id, accountsTask.assignedUserId)
        assertEquals(CrmTaskStatus.COMPLETED, repo.workStatus(supervisor, accountsTask))
        assertEquals(CrmTaskStatus.PENDING, repo.workStatus(finance, accountsTask))
        repo.act(finance, task.id, OrderAction.APPROVE, "Payment cleared")
        assertEquals(factory.id, repo.tasks.value.single().assignedUserId)
        repo.act(factory, task.id, OrderAction.APPROVE, "Capacity confirmed", 6000)
        assertEquals(OrderStage.PRODUCTION, repo.tasks.value.single().stage)
        repo.receiveMaterial(factory, "Test resin", "kg", 100.0, "Opening test stock")
        repo.saveRecipe(factory, items.single().product.id, listOf(RecipeLine(repo.manufacturing.value.materials.single().id, 2.0)))
        repo.issuePrint(factory, task.id, PrintKind.PRODUCTION)
        repo.act(factory, task.id, OrderAction.READY, "Production finished")
        repo.act(factory, task.id, OrderAction.DELIVER, "Received by distributor")
        val restored = repository(storage).visibleTasks(distributor).single()
        assertEquals(OrderStage.DELIVERED, restored.stage)
        assertEquals(CrmTaskStatus.COMPLETED, restored.status)
        assertEquals(8, restored.history.size)
        assertEquals(6000L, restored.expectedDeliveryAt)
        assertEquals(distributor.id, restored.distributorId)
        assertEquals(items, restored.items)
        assertEquals(8377.76, restored.totalAmount, 0.001)
    }

    @Test fun noRoleCanSkipStagesOrActForAnotherAssignedStaffMember() {
        val repo = repository()
        val task = submit(repo)
        listOf(admin, distributor, finance, factory, supervisor.copy(id = "other")).forEach { actor ->
            assertThrows(IllegalArgumentException::class.java) { repo.act(actor, task.id, OrderAction.APPROVE, "Bypass") }
        }
        assertThrows(IllegalArgumentException::class.java) { repo.act(supervisor, task.id, OrderAction.DELIVER, "Bypass") }
        assertThrows(IllegalArgumentException::class.java) { repo.act(supervisor, task.id, OrderAction.READY, "Bypass") }
        assertEquals(task, repo.tasks.value.single())
    }

    @Test fun rejectionIsTerminalAndHistoryShowsReason() {
        val storage = MemoryStorage()
        val repo = repository(storage)
        val task = submit(repo)
        repo.act(supervisor, task.id, OrderAction.APPROVE, "Valid")
        repo.act(finance, task.id, OrderAction.REJECT, "Credit limit exceeded")
        val restored = repository(storage).tasks.value.single()
        assertEquals(OrderStage.REJECTED, restored.stage)
        assertEquals("Credit limit exceeded", restored.history.last().details)
        assertFalse(repo.canAct(finance, restored))
        assertFalse(repo.canAct(factory, restored))
        assertThrows(IllegalArgumentException::class.java) { repo.act(finance, task.id, OrderAction.APPROVE, "Retry") }
    }

    @Test fun reviewRequiresNotesAndFactoryApprovalRequiresDeliveryDate() {
        val repo = repository()
        val task = submit(repo)
        assertThrows(IllegalArgumentException::class.java) { repo.act(supervisor, task.id, OrderAction.APPROVE, "") }
        repo.act(supervisor, task.id, OrderAction.APPROVE, "Approved")
        repo.act(finance, task.id, OrderAction.APPROVE, "Cleared")
        assertThrows(IllegalArgumentException::class.java) { repo.act(factory, task.id, OrderAction.APPROVE, "Approved") }
        assertThrows(IllegalArgumentException::class.java) { repo.act(factory, task.id, OrderAction.APPROVE, "Approved", 500) }
        assertEquals(OrderStage.FACTORY, repo.tasks.value.single().stage)
    }

    @Test fun invalidOrMissingProductsAndMissingReviewersAreRejected() {
        val repo = repository()
        assertThrows(IllegalArgumentException::class.java) { repo.placeOrder(distributor, emptyList(), users, "Dhaka", "01700000000") }
        assertThrows(IllegalArgumentException::class.java) { repo.placeOrder(distributor, listOf(items[0].copy(quantity = 0)), users, "Dhaka", "01700000000") }
        assertThrows(IllegalArgumentException::class.java) { repo.placeOrder(distributor, items + items, users, "Dhaka", "01700000000") }
        assertThrows(IllegalStateException::class.java) { repo.placeOrder(distributor, items, users.filter { it.id != factory.id }, "Dhaka", "01700000000") }
        assertThrows(IllegalArgumentException::class.java) { repo.placeOrder(distributor, items, users, "", "01700000000") }
        assertTrue(repo.tasks.value.isEmpty())
    }

    @Test fun changedPricesAndDeletedProductsCannotBePurchasedFromStaleCart() {
        val current = ProductCatalog.products.toMutableList()
        val repo = CrmRepository(MemoryStorage(), catalog = { current }) { 1000L }
        val tampered = items.map { it.copy(product = it.product.copy(discountedPrice = 0.0)) }
        assertThrows(IllegalArgumentException::class.java) {
            repo.placeOrder(distributor, tampered, users, "Dhaka", "01700000000")
        }
        current.removeAll { it.id == items.first().product.id }
        assertThrows(IllegalStateException::class.java) { submit(repo) }
        assertTrue(repo.tasks.value.isEmpty())
    }

    @Test fun failedPersistenceDoesNotAdvanceOrderOrLoseHistory() {
        val storage = MemoryStorage()
        val repo = repository(storage)
        val task = submit(repo)
        storage.failWrites = true
        assertThrows(java.io.IOException::class.java) { repo.act(supervisor, task.id, OrderAction.APPROVE, "Approved") }
        assertEquals(task, repo.tasks.value.single())
    }
}
