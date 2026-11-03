package com.example.sn_distributors.data.model

enum class PortalTab {
    DASHBOARD,
    PRODUCTS,
}

enum class ApprovalStatus(
    val label: String,
    val stepIndex: Int,
) {
    PENDING_SUPERVISOR("Pending Supervisor Approval", 1),
    APPROVED_SUPERVISOR("Approved by Supervisor", 1),
    PENDING_ACCOUNTS("Pending Accounts Clearance", 2),
    APPROVED_ACCOUNTS("Approved by Accounts", 2),
    FACTORY_ACCEPTED("Factory Accepted - Preparing Batches", 3),
    READY_FOR_DELIVERY("Ready for Delivery", 4),
}

data class Distributor(
    val id: String,
    val name: String,
    val code: String,
    val token: String,
    val creditLimit: Double,
    val currentBalance: Double,
)

data class ProductItem(
    val id: String,
    val name: String,
    val code: String,
    val basePrice: Double,
    val discountedPrice: Double,
    val unit: String,
    val recipeBreakdown: String,
)

data class CartItem(
    val product: ProductItem,
    val quantity: Int,
)

data class Requisition(
    val id: String,
    val distributorId: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val status: ApprovalStatus,
    val createdAt: String,
    val updatedAt: String,
)
