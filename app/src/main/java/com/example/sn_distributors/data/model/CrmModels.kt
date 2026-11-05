package com.example.sn_distributors.data.model

enum class CustomerIntent(val label: String, val guidance: String) {
    FIRST_PURCHASE("First purchase", "Discover the application, budget and essential features before recommending a suitable product."),
    UPGRADE("Upgrade", "Compare the current product with the requested improvement and explain the benefit of switching.")
}

enum class CrmTaskStatus(val label: String) {
    PENDING("Pending"), IN_PROGRESS("In progress"), COMPLETED("Completed")
}

data class CustomerNeed(
    val name: String,
    val contact: String,
    val intent: CustomerIntent,
    val productNeeded: String,
    val currentProduct: String,
    val desiredImprovement: String,
    val budget: String,
    val recommendedProduct: String,
    val fitReason: String
)

data class CrmHistory(
    val at: Long,
    val actorId: String,
    val actorName: String,
    val action: String,
    val details: String
)

data class CrmTask(
    val id: String,
    val customer: CustomerNeed,
    val title: String,
    val assignedUserId: String,
    val assignedName: String,
    val assignedRole: UserRole,
    val assignedDepartment: StaffDepartment?,
    val dueAt: Long,
    val status: CrmTaskStatus,
    val updatedAt: Long,
    val history: List<CrmHistory>,
    val distributorId: String? = null,
    val distributorName: String = "",
    val distributorEmail: String = "",
    val items: List<CartItem> = emptyList(),
    val stage: OrderStage? = null,
    val supervisorId: String? = null,
    val accountsId: String? = null,
    val factoryId: String? = null,
    val expectedDeliveryAt: Long? = null,
    val deliveryAddress: String = "",
    val orderNote: String = "",
    val prints: List<OrderPrint> = emptyList(),
    val isPaused: Boolean = false,
    val pauseReason: String = ""
) {
    val totalAmount: Double get() = items.sumOf { it.product.discountedPrice * it.quantity }
}

enum class PrintKind { TEST, PRODUCTION }
data class MaterialUse(val materialId: String, val name: String, val unit: String, val quantity: Double)
data class OrderPrint(val id: String, val kind: PrintKind, val at: Long, val actorId: String,
                      val actorName: String, val materials: List<MaterialUse> = emptyList())
data class RawMaterial(val id: String, val name: String, val unit: String, val stock: Double)
data class RecipeLine(val materialId: String, val quantity: Double)
data class ProductRecipe(val productId: String, val lines: List<RecipeLine>, val productUnit: String = "")
data class StockReceipt(val id: String, val materialId: String, val quantity: Double, val at: Long,
                        val actorId: String, val note: String)
data class ManufacturingState(val tasks: List<CrmTask> = emptyList(), val materials: List<RawMaterial> = emptyList(),
                              val recipes: List<ProductRecipe> = emptyList(), val receipts: List<StockReceipt> = emptyList())

enum class OrderStage(val label: String) {
    SUPERVISOR("Awaiting supervisor approval"), ACCOUNTS("Awaiting accounts clearance"),
    FACTORY("Awaiting factory approval"), PRODUCTION("In production"),
    READY("Ready for delivery"), DELIVERED("Delivered"), REJECTED("Rejected")
}

enum class OrderAction(val label: String) {
    START_REVIEW("Start review"), APPROVE("Approve"), PAUSE("Pause"), REJECT("Reject"),
    READY("Mark ready for delivery"), DELIVER("Confirm delivery")
}
