package com.example.sn_distributors.data.repository

import com.example.sn_distributors.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID
import java.math.BigDecimal

interface CrmStorage {
    fun load(): List<CrmTask>
    fun save(tasks: List<CrmTask>)
    fun loadState() = ManufacturingState(tasks = load())
    fun saveState(state: ManufacturingState) {
        check(state.materials.isEmpty() && state.recipes.isEmpty() && state.receipts.isEmpty()) { "This storage does not support inventory." }
        save(state.tasks)
    }
}

class CrmRepository(private val storage: CrmStorage, private val catalog: () -> List<ProductItem> = { ProductCatalog.products },
                    private val clock: () -> Long = System::currentTimeMillis) {
    @Volatile private var snapshot = storage.loadState()
    private val state = MutableStateFlow(snapshot.tasks)
    val tasks = state.asStateFlow()
    private val manufacturingState = MutableStateFlow(snapshot)
    val manufacturing = manufacturingState.asStateFlow()

    fun canAccess(user: UserAccount, task: CrmTask): Boolean = user.isActive && when (user.role) {
        UserRole.ADMIN -> true
        UserRole.STAFF -> task.distributorId != null || task.assignedUserId == user.id
        UserRole.DISTRIBUTOR -> task.distributorId == user.id
    }
    fun visibleTasks(user: UserAccount) = state.value.filter { canAccess(user, it) }
    private fun requestDepartment(task: CrmTask) = when (task.stage) {
        OrderStage.SUPERVISOR -> StaffDepartment.SUPERVISOR
        OrderStage.ACCOUNTS -> StaffDepartment.ACCOUNTS
        OrderStage.FACTORY, OrderStage.PRODUCTION, OrderStage.READY -> StaffDepartment.FACTORY
        else -> null
    }
    private fun isDemoReviewer(id: String) = DemoAccounts.logins.any { it.account.id == id && it.account.role == UserRole.STAFF }

    fun approvalRoute(accounts: List<UserAccount>): List<UserAccount> = StaffDepartment.entries.map { department ->
        accounts.filter { it.isActive && it.role == UserRole.STAFF && it.department == department }
            .sortedWith(compareBy<UserAccount> { isDemoReviewer(it.id) }
                .thenBy { candidate -> state.value.count { it.assignedUserId == candidate.id && it.status != CrmTaskStatus.COMPLETED } }
                .thenBy { it.email })
            .firstOrNull() ?: error("No active ${department.name.lowercase()} staff is available. Contact Admin.")
    }

    fun canClaim(user: UserAccount, task: CrmTask, accounts: List<UserAccount>): Boolean = canAccess(user, task) &&
        user.role == UserRole.STAFF && requestDepartment(task) != null && user.department == requestDepartment(task) && user.id != task.assignedUserId &&
        (isDemoReviewer(task.assignedUserId) || accounts.none { it.id == task.assignedUserId && it.isActive })

    @Synchronized
    fun claimRequest(actor: UserAccount, taskId: String, accounts: List<UserAccount>) {
        val task = state.value.firstOrNull { it.id == taskId } ?: error("Order no longer exists.")
        require(canClaim(actor, task, accounts)) { "Only staff in the required department can claim an unstaffed or demo request." }
        val now = clock()
        val next = task.copy(assignedUserId = actor.id, assignedName = actor.name,
            supervisorId = if (task.stage == OrderStage.SUPERVISOR) actor.id else task.supervisorId,
            accountsId = if (task.stage == OrderStage.ACCOUNTS) actor.id else task.accountsId,
            factoryId = if (requestDepartment(task) == StaffDepartment.FACTORY) actor.id else task.factoryId,
            updatedAt = now, history = task.history + CrmHistory(now, actor.id, actor.name, "Approval request claimed", "Assigned to ${actor.name} (${actor.department})."))
        commit(state.value.map { if (it.id == taskId) next else it })
    }
    fun canAct(user: UserAccount, task: CrmTask): Boolean = canAccess(user, task) &&
        user.role == UserRole.STAFF && task.assignedUserId == user.id && when (task.stage) {
            OrderStage.SUPERVISOR -> user.department == StaffDepartment.SUPERVISOR
            OrderStage.ACCOUNTS -> user.department == StaffDepartment.ACCOUNTS
            OrderStage.FACTORY, OrderStage.PRODUCTION, OrderStage.READY -> user.department == StaffDepartment.FACTORY
            else -> false
        }
    fun workStatus(user: UserAccount, task: CrmTask): CrmTaskStatus? {
        if (!canAccess(user, task)) return null
        if (user.role != UserRole.STAFF) return when (task.stage) {
            OrderStage.ACCOUNTS, OrderStage.FACTORY, OrderStage.PRODUCTION, OrderStage.READY -> CrmTaskStatus.IN_PROGRESS
            else -> task.status
        }
        if (canAct(user, task)) return task.status
        if (user.department == requestDepartment(task) && isDemoReviewer(task.assignedUserId)) return task.status
        return if (task.history.any { it.actorId == user.id }) CrmTaskStatus.COMPLETED else null
    }

    @Synchronized
    fun placeOrder(distributor: UserAccount, items: List<CartItem>, accounts: List<UserAccount>,
                   deliveryAddress: String, contact: String, note: String = "", requestId: String? = null): CrmTask {
        require(distributor.isActive && distributor.role == UserRole.DISTRIBUTOR) { "Only distributors can place orders." }
        require(deliveryAddress.isNotBlank() && contact.isNotBlank()) { "Enter a delivery address and contact phone or email." }
        if (requestId != null) {
            require(runCatching { UUID.fromString(requestId) }.isSuccess) { "Invalid checkout reference." }
            state.value.firstOrNull { it.id == requestId }?.let { existing ->
                require(existing.distributorId == distributor.id && existing.items == items && existing.deliveryAddress == deliveryAddress.trim() && existing.customer.contact == contact.trim() && existing.orderNote == note.trim()) { "This checkout reference was already used. Open the submitted order." }
                return existing
            }
        }
        require(items.isNotEmpty() && items.map { it.product.id }.distinct().size == items.size) { "Select products and combine duplicate items." }
        val canonicalItems = items.map { item ->
            require(item.quantity in 1..100_000) { "Quantity must be between 1 and 100000." }
            val product = catalog().firstOrNull { it.id == item.product.id } ?: error("A product was removed. Please review your cart.")
            require(product == item.product) { "A product changed. Please review the current price and place your order again." }
            CartItem(product, item.quantity)
        }
        require(canonicalItems.sumOf { it.quantity * it.product.discountedPrice }.isFinite()) { "Invalid order total." }
        val route = approvalRoute(accounts)
        val supervisor = route.first { it.department == StaffDepartment.SUPERVISOR }
        val finance = route.first { it.department == StaffDepartment.ACCOUNTS }
        val factory = route.first { it.department == StaffDepartment.FACTORY }
        val now = clock()
        val id = requestId ?: UUID.randomUUID().toString()
        // CustomerNeed is retained only for compatibility with already-saved orders.
        val customer = CustomerNeed(distributor.name, contact.trim(), CustomerIntent.FIRST_PURCHASE,
            "Purchase order", "", "", "", "", "")
        val task = CrmTask(id, customer, "Order #${id.take(8).uppercase()}",
            supervisor.id, supervisor.name, UserRole.STAFF, StaffDepartment.SUPERVISOR, now + 7 * 86_400_000L,
            CrmTaskStatus.PENDING, now, listOf(CrmHistory(now, distributor.id, distributor.name,
                "Order submitted", "${canonicalItems.size} product(s). Assigned to ${supervisor.name}; next: ${finance.name}, then ${factory.name}.")),
            distributor.id, distributor.name, distributor.email, canonicalItems, OrderStage.SUPERVISOR,
            supervisor.id, finance.id, factory.id, deliveryAddress = deliveryAddress.trim(), orderNote = note.trim())
        commit(state.value + task)
        return task
    }

    @Synchronized
    fun act(actor: UserAccount, taskId: String, action: OrderAction, note: String, expectedDeliveryAt: Long? = null,
            accounts: List<UserAccount>? = null) {
        val task = state.value.firstOrNull { it.id == taskId } ?: error("Order no longer exists.")
        require(canAct(actor, task)) { "Only the assigned staff member at the current approval stage can act." }
        require(note.isNotBlank()) { "Enter a review note, rejection reason or delivery outcome." }
        var next: CrmTask
        val label: String
        fun nextReviewer(department: StaffDepartment, savedId: String?): UserAccount? {
            if (accounts == null) return null
            val saved = accounts.firstOrNull { it.id == savedId && it.isActive && it.role == UserRole.STAFF && it.department == department }
            return saved?.takeUnless { isDemoReviewer(it.id) } ?: approvalRoute(accounts).first { it.department == department }
        }
        when (action) {
            OrderAction.PAUSE -> {
                require(task.stage in listOf(OrderStage.SUPERVISOR, OrderStage.ACCOUNTS, OrderStage.FACTORY)) { "Only an approval request can be paused." }
                require(!task.isPaused) { "This request is already paused. Approve it when ready to continue." }
                next = task.copy(isPaused = true, pauseReason = note.trim(), status = CrmTaskStatus.PENDING)
                label = "${actor.department} paused approval"
            }
            OrderAction.START_REVIEW -> {
                require(!task.isPaused) { "This request is paused. Approve it when ready to continue." }
                require(task.stage in listOf(OrderStage.SUPERVISOR, OrderStage.ACCOUNTS, OrderStage.FACTORY) && task.status == CrmTaskStatus.PENDING) { "This order is already being reviewed." }
                next = task.copy(status = CrmTaskStatus.IN_PROGRESS)
                label = "${actor.department} review started"
            }
            OrderAction.REJECT -> {
                require(task.stage in listOf(OrderStage.SUPERVISOR, OrderStage.ACCOUNTS, OrderStage.FACTORY)) { "This stage cannot reject the order." }
                next = task.copy(stage = OrderStage.REJECTED, status = CrmTaskStatus.COMPLETED)
                label = "${actor.department} rejected order"
            }
            OrderAction.APPROVE -> {
                next = when (task.stage) {
                    OrderStage.SUPERVISOR -> {
                        val reviewer = nextReviewer(StaffDepartment.ACCOUNTS, task.accountsId)
                        task.copy(stage = OrderStage.ACCOUNTS, assignedUserId = reviewer?.id ?: requireNotNull(task.accountsId),
                            accountsId = reviewer?.id ?: task.accountsId, assignedName = reviewer?.name ?: "Accounts",
                            assignedDepartment = StaffDepartment.ACCOUNTS, status = CrmTaskStatus.PENDING)
                    }
                    OrderStage.ACCOUNTS -> {
                        val reviewer = nextReviewer(StaffDepartment.FACTORY, task.factoryId)
                        task.copy(stage = OrderStage.FACTORY, assignedUserId = reviewer?.id ?: requireNotNull(task.factoryId),
                            factoryId = reviewer?.id ?: task.factoryId, assignedName = reviewer?.name ?: "Factory",
                            assignedDepartment = StaffDepartment.FACTORY, status = CrmTaskStatus.PENDING)
                    }
                    OrderStage.FACTORY -> {
                        require(expectedDeliveryAt != null && expectedDeliveryAt > clock()) { "Enter a future expected delivery date for factory approval." }
                        task.copy(stage = OrderStage.PRODUCTION, status = CrmTaskStatus.IN_PROGRESS, expectedDeliveryAt = expectedDeliveryAt, dueAt = expectedDeliveryAt)
                    }
                    else -> error("This stage cannot approve the order.")
                }
                label = "${actor.department} approved order"
            }
            OrderAction.READY -> {
                require(task.stage == OrderStage.PRODUCTION) { "Factory approval is required before marking ready." }
                require(task.prints.any { it.kind == PrintKind.PRODUCTION }) { "Issue the production print and deduct materials before marking ready." }
                next = task.copy(stage = OrderStage.READY, status = CrmTaskStatus.IN_PROGRESS)
                label = "Production completed; ready for delivery"
            }
            OrderAction.DELIVER -> {
                require(task.stage == OrderStage.READY) { "Order must be ready before delivery." }
                next = task.copy(stage = OrderStage.DELIVERED, status = CrmTaskStatus.COMPLETED)
                label = "Delivery completed"
            }
        }
        if (action == OrderAction.APPROVE) next = next.copy(isPaused = false, pauseReason = "")
        val now = clock()
        val detail = note.trim() + if (action == OrderAction.APPROVE && task.stage == OrderStage.FACTORY)
            "\nExpected delivery: " + java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.US).format(java.util.Date(requireNotNull(expectedDeliveryAt))) else ""
        next = next.copy(updatedAt = now, history = task.history + CrmHistory(now, actor.id, actor.name, label, detail))
        commit(state.value.map { if (it.id == taskId) next else it })
    }

    fun canManageMaterials(user: UserAccount) = user.isActive &&
        (user.role == UserRole.ADMIN || user.role == UserRole.STAFF && user.department == StaffDepartment.FACTORY)

    @Synchronized
    fun receiveMaterial(actor: UserAccount, name: String, unit: String, quantity: Double, note: String, materialId: String? = null) {
        require(canManageMaterials(actor)) { "Only Admin or Factory can manage raw materials." }
        require(name.isNotBlank() && unit.isNotBlank() && note.isNotBlank()) { "Enter material name, unit and receipt reference." }
        require(quantity.isFinite() && quantity > 0) { "Enter a positive stock quantity." }
        val existing = materialId?.let { id -> snapshot.materials.firstOrNull { it.id == id } ?: error("Material no longer exists.") }
        require(existing == null || existing.name == name.trim() && existing.unit == unit.trim()) { "Use the existing material name and unit." }
        require(existing != null || snapshot.materials.none { it.name.equals(name.trim(), true) }) { "Material exists. Use Receive stock." }
        val material = existing?.copy(stock = BigDecimal.valueOf(existing.stock).add(BigDecimal.valueOf(quantity)).toDouble())
            ?: RawMaterial(UUID.randomUUID().toString(), name.trim(), unit.trim(), quantity)
        require(material.stock.isFinite()) { "Stock quantity is too large." }
        val materials = if (existing == null) snapshot.materials + material else snapshot.materials.map { if (it.id == material.id) material else it }
        commitSnapshot(snapshot.copy(materials = materials, receipts = snapshot.receipts +
            StockReceipt(UUID.randomUUID().toString(), material.id, quantity, clock(), actor.id, note.trim())))
    }

    @Synchronized
    fun saveRecipe(actor: UserAccount, productId: String, lines: List<RecipeLine>) {
        require(canManageMaterials(actor)) { "Only Admin or Factory can configure recipes." }
        val product = catalog().firstOrNull { it.id == productId } ?: error("Product no longer exists.")
        require(lines.isNotEmpty() && lines.map { it.materialId }.distinct().size == lines.size) { "Add materials once each." }
        require(lines.all { line -> line.quantity.isFinite() && line.quantity > 0 && snapshot.materials.any { it.id == line.materialId } }) { "Enter valid materials and positive quantities per product unit." }
        commitSnapshot(snapshot.copy(recipes = snapshot.recipes.filterNot { it.productId == productId } + ProductRecipe(productId, lines, product.unit)))
    }

    @Synchronized
    fun requiredMaterials(task: CrmTask): List<MaterialUse> {
        require(task.items.isNotEmpty()) { "Order has no products." }
        val totals = linkedMapOf<String, BigDecimal>()
        task.items.forEach { item ->
            val recipe = snapshot.recipes.firstOrNull { it.productId == item.product.id }
                ?: error("Configure a raw-material recipe for ${item.product.name} first.")
            require(recipe.lines.isNotEmpty()) { "Recipe is empty for ${item.product.name}." }
            require(recipe.productUnit == item.product.unit) { "Recipe unit does not match ${item.product.name}. Configure a recipe for ${item.product.unit}." }
            recipe.lines.forEach { line -> totals[line.materialId] = (totals[line.materialId] ?: BigDecimal.ZERO).add(BigDecimal.valueOf(line.quantity).multiply(BigDecimal.valueOf(item.quantity.toLong()))) }
        }
        return totals.map { (id, quantity) ->
            val material = snapshot.materials.firstOrNull { it.id == id } ?: error("A recipe material is unavailable.")
            val amount = quantity.toDouble()
            require(amount.isFinite() && amount > 0) { "Invalid material requirement." }
            MaterialUse(id, material.name, material.unit, amount)
        }
    }

    @Synchronized
    fun issuePrint(actor: UserAccount, taskId: String, kind: PrintKind): OrderPrint {
        val task = snapshot.tasks.firstOrNull { it.id == taskId } ?: error("Order no longer exists.")
        require(canAct(actor, task) && actor.department == StaffDepartment.FACTORY && task.stage == OrderStage.PRODUCTION) { "Only the assigned Factory member can issue prints after factory acceptance." }
        require(kind != PrintKind.PRODUCTION || task.prints.none { it.kind == PrintKind.PRODUCTION }) { "Production print already issued. Reprint the saved document." }
        val uses = if (kind == PrintKind.PRODUCTION) requiredMaterials(task) else emptyList()
        uses.forEach { use ->
            val stock = snapshot.materials.first { it.id == use.materialId }.stock
            require(stock >= use.quantity) { "Insufficient ${use.name}: need ${use.quantity} ${use.unit}, available $stock ${use.unit}." }
        }
        val record = OrderPrint(UUID.randomUUID().toString(), kind, clock(), actor.id, actor.name, uses)
        val nextTask = task.copy(updatedAt = record.at, prints = task.prints + record,
            history = task.history + CrmHistory(record.at, actor.id, actor.name,
                if (kind == PrintKind.TEST) "Test print issued" else "Production print issued; raw materials deducted",
                if (uses.isEmpty()) "Reference only. No stock deducted." else uses.joinToString("\n") { "${it.name}: ${it.quantity} ${it.unit}" }))
        val materials = snapshot.materials.map { material ->
            material.copy(stock = BigDecimal.valueOf(material.stock).subtract(BigDecimal.valueOf(uses.firstOrNull { it.materialId == material.id }?.quantity ?: 0.0)).toDouble())
        }
        commitSnapshot(snapshot.copy(tasks = snapshot.tasks.map { if (it.id == taskId) nextTask else it }, materials = materials))
        return record
    }

    private fun commit(tasks: List<CrmTask>) = commitSnapshot(snapshot.copy(tasks = tasks))

    private fun commitSnapshot(next: ManufacturingState) {
        storage.saveState(next)
        snapshot = next
        manufacturingState.value = next
        state.value = next.tasks
    }
}
