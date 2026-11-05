package com.example.sn_distributors

import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import org.junit.Assert.*
import org.junit.Test

class ProductionWorkflowTest {
    private class Store : CrmStorage {
        var state = ManufacturingState()
        var fail = false
        override fun load() = state.tasks
        override fun save(tasks: List<CrmTask>) { saveState(state.copy(tasks = tasks)) }
        override fun loadState() = state
        override fun saveState(state: ManufacturingState) {
            if (fail) throw java.io.IOException("Disk full")
            this.state = state
        }
    }
    private val users = DemoAccounts.logins.map { it.account }
    private val buyer = users.first { it.role == UserRole.DISTRIBUTOR }
    private val supervisor = users.first { it.department == StaffDepartment.SUPERVISOR }
    private val accounts = users.first { it.department == StaffDepartment.ACCOUNTS }
    private val factory = users.first { it.department == StaffDepartment.FACTORY }
    private val admin = users.first { it.role == UserRole.ADMIN }
    private fun repo(store: Store = Store()) = CrmRepository(store) { 1000L }
    private fun order(repo: CrmRepository) = repo.placeOrder(buyer, listOf(CartItem(ProductCatalog.products[0], 2),
        CartItem(ProductCatalog.products[1], 3)), users, "Dhaka", "01700000000")
    private fun accept(repo: CrmRepository, task: CrmTask) {
        repo.act(supervisor, task.id, OrderAction.APPROVE, "Verified")
        repo.act(accounts, task.id, OrderAction.APPROVE, "Cleared")
        repo.act(factory, task.id, OrderAction.APPROVE, "Capacity confirmed", 50_000L)
    }
    private fun stock(repo: CrmRepository, quantity: Double = 100.0) {
        repo.receiveMaterial(admin, "Resin", "kg", quantity, "Opening stock")
        val material = repo.manufacturing.value.materials.single()
        repo.saveRecipe(factory, ProductCatalog.products[0].id, listOf(RecipeLine(material.id, 2.0)))
        repo.saveRecipe(factory, ProductCatalog.products[1].id, listOf(RecipeLine(material.id, 3.0)))
    }

    @Test fun testPrintsDoNotConsumeAndProductionConsumesAggregatedMaterialsExactlyOnce() {
        val store = Store(); val repo = repo(store); stock(repo)
        val task = order(repo); accept(repo, task)
        repeat(2) { repo.issuePrint(factory, task.id, PrintKind.TEST) }
        assertEquals(100.0, repo.manufacturing.value.materials.single().stock, 0.0)
        assertThrows(IllegalArgumentException::class.java) { repo.act(factory, task.id, OrderAction.READY, "Too early") }
        val print = repo.issuePrint(factory, task.id, PrintKind.PRODUCTION)
        assertEquals(13.0, print.materials.single().quantity, 0.0)
        assertEquals(87.0, repo.manufacturing.value.materials.single().stock, 0.0)
        assertEquals(CrmTaskStatus.IN_PROGRESS, repo.workStatus(factory, repo.tasks.value.single()))
        assertThrows(IllegalArgumentException::class.java) { repo.issuePrint(factory, task.id, PrintKind.PRODUCTION) }
        val restored = repo(store)
        assertEquals(87.0, restored.manufacturing.value.materials.single().stock, 0.0)
        assertEquals(3, restored.tasks.value.single().prints.size)
        assertThrows(IllegalArgumentException::class.java) { restored.issuePrint(factory, task.id, PrintKind.PRODUCTION) }
        restored.act(factory, task.id, OrderAction.READY, "Packed")
        restored.act(factory, task.id, OrderAction.DELIVER, "Received")
        assertEquals(CrmTaskStatus.COMPLETED, restored.workStatus(factory, restored.tasks.value.single()))
        assertEquals(CrmTaskStatus.COMPLETED, restored.workStatus(buyer, restored.tasks.value.single()))
    }

    @Test fun missingRecipeAndInsufficientStockLeaveOrderAndInventoryUntouched() {
        val repo = repo(); val task = order(repo); accept(repo, task)
        assertThrows(IllegalStateException::class.java) { repo.issuePrint(factory, task.id, PrintKind.PRODUCTION) }
        stock(repo, 12.0)
        val before = repo.manufacturing.value
        assertThrows(IllegalArgumentException::class.java) { repo.issuePrint(factory, task.id, PrintKind.PRODUCTION) }
        assertEquals(before, repo.manufacturing.value)
        repo.receiveMaterial(factory, "Resin", "kg", 1.0, "Restock", before.materials.single().id)
        repo.issuePrint(factory, task.id, PrintKind.PRODUCTION)
        assertEquals(0.0, repo.manufacturing.value.materials.single().stock, 0.0)
    }

    @Test fun writeFailureCannotDeductStockOrCreatePrintAndRetrySucceedsOnce() {
        val store = Store(); val repo = repo(store); stock(repo)
        val task = order(repo); accept(repo, task)
        val before = repo.manufacturing.value
        store.fail = true
        assertThrows(java.io.IOException::class.java) { repo.issuePrint(factory, task.id, PrintKind.PRODUCTION) }
        assertEquals(before, repo.manufacturing.value)
        assertEquals(before, store.state)
        store.fail = false
        repo.issuePrint(factory, task.id, PrintKind.PRODUCTION)
        assertEquals(87.0, repo.manufacturing.value.materials.single().stock, 0.0)
    }

    @Test fun printsRequireFactoryAcceptanceAndAssignedFactoryAndStockManagementIsRestricted() {
        val repo = repo(); val task = order(repo)
        assertThrows(IllegalArgumentException::class.java) { repo.issuePrint(factory, task.id, PrintKind.TEST) }
        accept(repo, task)
        listOf(buyer, supervisor, accounts, admin, factory.copy(id = "another-factory")).forEach { actor ->
            assertThrows(IllegalArgumentException::class.java) { repo.issuePrint(actor, task.id, PrintKind.TEST) }
        }
        listOf(buyer, supervisor, accounts).forEach { actor ->
            assertThrows(IllegalArgumentException::class.java) { repo.receiveMaterial(actor, "Resin", "kg", 1.0, "Receipt") }
        }
        listOf(0.0, -1.0, Double.NaN, Double.POSITIVE_INFINITY).forEach { quantity ->
            assertThrows(IllegalArgumentException::class.java) { repo.receiveMaterial(factory, "Resin", "kg", quantity, "Receipt") }
        }
    }

    @Test fun dashboardWorkStatesFollowHandoffsAndOnlyDeliveryCompletesFactoryWork() {
        val repo = repo(); val task = order(repo)
        assertEquals(CrmTaskStatus.PENDING, repo.workStatus(supervisor, task))
        assertNull(repo.workStatus(accounts, task))
        repo.act(supervisor, task.id, OrderAction.APPROVE, "Verified")
        assertEquals(CrmTaskStatus.COMPLETED, repo.workStatus(supervisor, repo.tasks.value.single()))
        assertEquals(CrmTaskStatus.PENDING, repo.workStatus(accounts, repo.tasks.value.single()))
        assertEquals(CrmTaskStatus.IN_PROGRESS, repo.workStatus(buyer, repo.tasks.value.single()))
        repo.act(accounts, task.id, OrderAction.APPROVE, "Cleared")
        assertEquals(CrmTaskStatus.COMPLETED, repo.workStatus(accounts, repo.tasks.value.single()))
        assertEquals(CrmTaskStatus.PENDING, repo.workStatus(factory, repo.tasks.value.single()))
        repo.act(factory, task.id, OrderAction.APPROVE, "Accepted", 50_000L)
        assertEquals(CrmTaskStatus.IN_PROGRESS, repo.workStatus(factory, repo.tasks.value.single()))
    }

    @Test fun decimalRecipesDeductExactAvailableStockWithoutRoundingShortages() {
        val repo = repo()
        repo.receiveMaterial(factory, "Pigment", "kg", 0.3, "Opening stock")
        val product = ProductCatalog.products.first()
        repo.saveRecipe(factory, product.id, listOf(RecipeLine(repo.manufacturing.value.materials.single().id, 0.1)))
        val task = repo.placeOrder(buyer, listOf(CartItem(product, 3)), users, "Dhaka", "01700000000")
        accept(repo, task)
        repo.issuePrint(factory, task.id, PrintKind.PRODUCTION)
        assertEquals(0.0, repo.manufacturing.value.materials.single().stock, 0.0)
    }

    @Test fun concurrentProductionRequestsCanDeductStockOnlyOnce() {
        val repo = repo(); stock(repo); val task = order(repo); accept(repo, task)
        val start = java.util.concurrent.CountDownLatch(1)
        val executor = java.util.concurrent.Executors.newFixedThreadPool(2)
        try {
            val attempts = (1..2).map { executor.submit<Boolean> {
                start.await()
                try { repo.issuePrint(factory, task.id, PrintKind.PRODUCTION); true }
                catch (_: IllegalArgumentException) { false }
            } }
            start.countDown()
            assertEquals(1, attempts.count { it.get(5, java.util.concurrent.TimeUnit.SECONDS) })
            assertEquals(87.0, repo.manufacturing.value.materials.single().stock, 0.0)
            assertEquals(1, repo.tasks.value.single().prints.size)
        } finally { executor.shutdownNow() }
    }

    @Test fun realStaffReceiveNewRequestsAndCanClaimOlderDemoRequests() {
        val repo = repo()
        val realStaff = users.filter { it.role == UserRole.STAFF }.map { it.copy(id = "REAL-${it.department}", name = "Company ${it.department}", email = "z-${it.department}@sn.com") }
        val all = users + realStaff
        val task = repo.placeOrder(buyer, listOf(CartItem(ProductCatalog.products.first(), 1)), all, "Dhaka", "01700000000")
        assertEquals("REAL-SUPERVISOR", task.assignedUserId)
        assertEquals("REAL-ACCOUNTS", task.accountsId)
        assertEquals("REAL-FACTORY", task.factoryId)
        val old = order(repo)
        val realSupervisor = realStaff.first { it.department == StaffDepartment.SUPERVISOR }
        assertTrue(repo.canClaim(realSupervisor, old, all))
        repo.claimRequest(realSupervisor, old.id, all)
        assertTrue(repo.canAct(realSupervisor, repo.tasks.value.first { it.id == old.id }))
        repo.act(realSupervisor, old.id, OrderAction.APPROVE, "Verified", accounts = all)
        assertEquals("REAL-ACCOUNTS", repo.tasks.value.first { it.id == old.id }.assignedUserId)
        assertThrows(IllegalArgumentException::class.java) { repo.claimRequest(supervisor, task.id, all) }
    }

    @Test fun retryingCheckoutWithSameReferenceCreatesOnlyOneApprovalRequest() {
        val repo = repo()
        val reference = java.util.UUID.randomUUID().toString()
        val items = listOf(CartItem(ProductCatalog.products.first(), 1))
        val first = repo.placeOrder(buyer, items, users, "Dhaka", "01700000000", requestId = reference)
        assertEquals(first, repo.placeOrder(buyer, items, users, "Dhaka", "01700000000", requestId = reference))
        assertEquals(1, repo.tasks.value.size)
        assertThrows(IllegalArgumentException::class.java) {
            repo.placeOrder(buyer.copy(id = "another-buyer"), items, users, "Dhaka", "01700000000", requestId = reference)
        }
    }

    @Test fun pausedApprovalPersistsWithoutForwardingAndApprovingContinuesSameOrder() {
        val store = Store(); val repo = repo(store); val task = order(repo)
        repo.act(supervisor, task.id, OrderAction.PAUSE, "Awaiting distributor confirmation")
        val paused = repo.tasks.value.single()
        assertEquals(task.id, paused.id)
        assertEquals(OrderStage.SUPERVISOR, paused.stage)
        assertEquals(supervisor.id, paused.assignedUserId)
        assertTrue(paused.isPaused)
        assertEquals(CrmTaskStatus.PENDING, repo.workStatus(supervisor, paused))
        assertNull(repo.workStatus(accounts, paused))
        val restored = repo(store)
        assertTrue(restored.tasks.value.single().isPaused)
        assertEquals("Awaiting distributor confirmation", restored.tasks.value.single().pauseReason)
        assertThrows(IllegalArgumentException::class.java) { restored.act(accounts, task.id, OrderAction.APPROVE, "Cannot skip") }
        restored.act(supervisor, task.id, OrderAction.APPROVE, "Confirmed")
        val approved = restored.tasks.value.single()
        assertEquals(task.id, approved.id)
        assertFalse(approved.isPaused)
        assertEquals("", approved.pauseReason)
        assertEquals(OrderStage.ACCOUNTS, approved.stage)
        assertTrue(approved.history.any { it.action.contains("paused") })
    }

    @Test fun eachNewOrderHasUniqueIdAndReloadingPreservesCopiedReferences() {
        val store = Store(); val repo = repo(store)
        repeat(100) { order(repo) }
        assertEquals(100, repo.tasks.value.map { it.id }.distinct().size)
        assertEquals(repo.tasks.value.map { it.id }, repo(store).tasks.value.map { it.id })
    }
}
