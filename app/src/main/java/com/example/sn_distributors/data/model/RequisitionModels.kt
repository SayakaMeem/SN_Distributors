
package com.example.sn_distributors.data.model

enum class PortalTab {
    DASHBOARD,
    PRODUCTS
}

enum class ApprovalStatus(
    val label: String,
    val stepIndex: Int
) {
    PENDING_SUPERVISOR("Pending Supervisor Approval", 1),
    APPROVED_SUPERVISOR("Approved by Supervisor", 1),
    REJECTED_SUPERVISOR("Rejected by Supervisor", 1),

    PENDING_ACCOUNTS("Pending Accounts Clearance", 2),
    APPROVED_ACCOUNTS("Approved by Accounts", 2),
    REJECTED_ACCOUNTS("Rejected by Accounts", 2),

    FACTORY_ACCEPTED("Factory Accepted - Preparing Batches", 3),
    REJECTED_FACTORY("Rejected by Factory", 3),

    READY_FOR_DELIVERY("Ready for Delivery", 4),
    DELIVERED("Delivered", 5)
}

data class Distributor(
    val id: String,
    val name: String,
    val code: String,
    val token: String,
    val creditLimit: Double,
    val currentBalance: Double
)

data class ProductItem(
    val id: String,
    val name: String,
    val code: String,
    val basePrice: Double,
    val discountedPrice: Double,
    val unit: String,
    val recipeBreakdown: String
)

data class CartItem(
    val product: ProductItem,
    val quantity: Int
)

data class Requisition(
    val id: String,
    val distributorId: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val status: ApprovalStatus,
    val createdAt: String,
    val updatedAt: String,

    // Individual staff assignments.
    // These values contain UserAccount.id, not email addresses.
    val assignedSupervisorId: String? = null,
    val assignedAccountsId: String? = null,
    val assignedFactoryId: String? = null,

    // Rejection information.
    val rejectionReason: String? = null,
    val rejectedByUserId: String? = null,

    // Factory and delivery information.
    val expectedDeliveryDate: String? = null,
    val factoryNotes: String? = null,

    // Inventory protection.
    // Raw materials must only be deducted once after final confirmation.
    val rawMaterialsDeducted: Boolean = false
)

// =====================================================
// NOTIFICATION MODELS
// =====================================================

enum class NotificationType {
    NEW_REQUISITION,
    SUPERVISOR_APPROVED,
    SUPERVISOR_REJECTED,
    ACCOUNTS_APPROVED,
    ACCOUNTS_REJECTED,
    FACTORY_ACCEPTED,
    FACTORY_REJECTED,
    DELIVERY_READY,
    DELIVERY_COMPLETED
}

data class RequisitionNotification(
    val id: String,
    val recipientUserId: String,
    val requisitionId: String,
    val title: String,
    val message: String,
    val type: NotificationType,
    val createdAt: String,
    val isRead: Boolean = false
)

// =====================================================
// REQUISITION ACTIVITY HISTORY
// =====================================================

data class RequisitionHistoryEntry(
    val id: String,
    val requisitionId: String,
    val performedByUserId: String,
    val action: String,
    val details: String,
    val createdAt: String
)
