
package com.example.sn_distributors.data.repository

import com.example.sn_distributors.data.model.ApprovalStatus
import com.example.sn_distributors.data.model.CartItem
import com.example.sn_distributors.data.model.NotificationType
import com.example.sn_distributors.data.model.Requisition
import com.example.sn_distributors.data.model.RequisitionHistoryEntry
import com.example.sn_distributors.data.model.RequisitionNotification
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class ErpRepository {

    // In-memory storage
    private val requisitions = mutableListOf<Requisition>()
    private val notifications = mutableListOf<RequisitionNotification>()
    private val history = mutableListOf<RequisitionHistoryEntry>()
    private val rawMaterialInventory = mutableMapOf<String, Double>()

    // Common helpers
    private fun generateId(): String = UUID.randomUUID().toString()

    private fun currentTime(): String =
        SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss",
            Locale.getDefault()
        ).format(Date())

    private fun addHistory(
        requisitionId: String,
        performedByUserId: String,
        action: String,
        details: String
    ) {
        history.add(
            RequisitionHistoryEntry(
                id = generateId(),
                requisitionId = requisitionId,
                performedByUserId = performedByUserId,
                action = action,
                details = details,
                createdAt = currentTime()
            )
        )
    }

    private fun sendNotification(
        recipientUserId: String,
        requisitionId: String,
        title: String,
        message: String,
        type: NotificationType
    ) {
        if (recipientUserId.isBlank()) return

        notifications.add(
            RequisitionNotification(
                id = generateId(),
                recipientUserId = recipientUserId,
                requisitionId = requisitionId,
                title = title,
                message = message,
                type = type,
                createdAt = currentTime(),
                isRead = false
            )
        )
    }

    private fun findRequisition(id: String): Requisition? =
        requisitions.find { it.id == id }

    private fun updateRequisition(updated: Requisition) {
        val index = requisitions.indexOfFirst {
            it.id == updated.id
        }
        if (index >= 0) {
            requisitions[index] = updated
        }
    }

    // Requisition submission
    @Synchronized
    fun submitRequisition(
        distributorId: String,
        items: List<CartItem>,
        assignedSupervisorId: String,
        assignedAccountsId: String,
        assignedFactoryId: String
    ): Requisition? {
        if (distributorId.isBlank() || items.isEmpty()) return null
        if (assignedSupervisorId.isBlank()) return null
        if (assignedAccountsId.isBlank()) return null
        if (assignedFactoryId.isBlank()) return null

        if (items.any {
                it.quantity <= 0 ||
                        !it.product.discountedPrice.isFinite() ||
                        it.product.discountedPrice < 0.0
            }) return null

        val totalAmount = items.sumOf {
            it.product.discountedPrice * it.quantity
        }
        if (!totalAmount.isFinite()) return null

        val now = currentTime()
        val requisition = Requisition(
            id = generateId(),
            distributorId = distributorId,
            items = items.toList(),
            totalAmount = totalAmount,
            status = ApprovalStatus.PENDING_SUPERVISOR,
            createdAt = now,
            updatedAt = now,
            assignedSupervisorId = assignedSupervisorId,
            assignedAccountsId = assignedAccountsId,
            assignedFactoryId = assignedFactoryId
        )

        requisitions.add(requisition)
        addHistory(
            requisition.id,
            distributorId,
            "REQUISITION_SUBMITTED",
            "Distributor submitted a requisition."
        )
        sendNotification(
            assignedSupervisorId,
            requisition.id,
            "New Requisition",
            "A new requisition requires your review.",
            NotificationType.NEW_REQUISITION
        )
        return requisition
    }

    // Requisition queries
    @Synchronized
    fun getAllRequisitions(): List<Requisition> =
        requisitions.toList()

    @Synchronized
    fun getRequisitionById(requisitionId: String): Requisition? =
        findRequisition(requisitionId)

    @Synchronized
    fun getRequisitionsForDistributor(distributorId: String): List<Requisition> =
        requisitions.filter { it.distributorId == distributorId }

    @Synchronized
    fun getRequisitionsForSupervisor(supervisorUserId: String): List<Requisition> =
        requisitions.filter {
            it.assignedSupervisorId == supervisorUserId &&
                    it.status == ApprovalStatus.PENDING_SUPERVISOR
        }

    @Synchronized
    fun getRequisitionsForAccounts(accountsUserId: String): List<Requisition> =
        requisitions.filter {
            it.assignedAccountsId == accountsUserId &&
                    it.status == ApprovalStatus.PENDING_ACCOUNTS
        }

    @Synchronized
    fun getRequisitionsForFactory(factoryUserId: String): List<Requisition> =
        requisitions.filter {
            it.assignedFactoryId == factoryUserId &&
                    (
                            it.status == ApprovalStatus.APPROVED_ACCOUNTS ||
                                    it.status == ApprovalStatus.FACTORY_ACCEPTED ||
                                    it.status == ApprovalStatus.READY_FOR_DELIVERY
                            )
        }

    // Supervisor approval
    @Synchronized
    fun approveBySupervisor(
        requisitionId: String,
        supervisorUserId: String
    ): Boolean {
        val req = findRequisition(requisitionId) ?: return false
        if (req.status != ApprovalStatus.PENDING_SUPERVISOR) return false
        if (req.assignedSupervisorId != supervisorUserId) return false

        // Validate before changing state
        val accountsId = req.assignedAccountsId
            ?.takeIf { it.isNotBlank() } ?: return false

        updateRequisition(
            req.copy(
                status = ApprovalStatus.PENDING_ACCOUNTS,
                updatedAt = currentTime()
            )
        )
        addHistory(
            requisitionId,
            supervisorUserId,
            "SUPERVISOR_APPROVED",
            "Supervisor approved the requisition."
        )
        sendNotification(
            accountsId,
            requisitionId,
            "Requisition Awaiting Accounts Review",
            "The Supervisor approved this requisition. Please review it.",
            NotificationType.SUPERVISOR_APPROVED
        )
        return true
    }

    // Supervisor rejection
    @Synchronized
    fun rejectBySupervisor(
        requisitionId: String,
        supervisorUserId: String,
        reason: String
    ): Boolean {
        val req = findRequisition(requisitionId) ?: return false
        if (req.status != ApprovalStatus.PENDING_SUPERVISOR) return false
        if (req.assignedSupervisorId != supervisorUserId) return false
        if (reason.isBlank()) return false

        val cleanReason = reason.trim()
        updateRequisition(
            req.copy(
                status = ApprovalStatus.REJECTED_SUPERVISOR,
                rejectionReason = cleanReason,
                rejectedByUserId = supervisorUserId,
                updatedAt = currentTime()
            )
        )
        addHistory(
            requisitionId,
            supervisorUserId,
            "SUPERVISOR_REJECTED",
            cleanReason
        )
        sendNotification(
            req.distributorId,
            requisitionId,
            "Requisition Rejected",
            "Your requisition was rejected by the Supervisor. Reason: $cleanReason",
            NotificationType.SUPERVISOR_REJECTED
        )
        return true
    }

    // Accounts approval
    @Synchronized
    fun approveByAccounts(
        requisitionId: String,
        accountsUserId: String
    ): Boolean {
        val req = findRequisition(requisitionId) ?: return false
        if (req.status != ApprovalStatus.PENDING_ACCOUNTS) return false
        if (req.assignedAccountsId != accountsUserId) return false

        val factoryId = req.assignedFactoryId
            ?.takeIf { it.isNotBlank() } ?: return false

        updateRequisition(
            req.copy(
                status = ApprovalStatus.APPROVED_ACCOUNTS,
                updatedAt = currentTime()
            )
        )
        addHistory(
            requisitionId,
            accountsUserId,
            "ACCOUNTS_APPROVED",
            "Accounts department approved the requisition."
        )
        sendNotification(
            factoryId,
            requisitionId,
            "Requisition Awaiting Factory Review",
            "Accounts approved this requisition. Please check raw materials and production.",
            NotificationType.ACCOUNTS_APPROVED
        )
        return true
    }

    // Accounts rejection
    @Synchronized
    fun rejectByAccounts(
        requisitionId: String,
        accountsUserId: String,
        reason: String
    ): Boolean {
        val req = findRequisition(requisitionId) ?: return false
        if (req.status != ApprovalStatus.PENDING_ACCOUNTS) return false
        if (req.assignedAccountsId != accountsUserId) return false
        if (reason.isBlank()) return false

        val cleanReason = reason.trim()
        updateRequisition(
            req.copy(
                status = ApprovalStatus.REJECTED_ACCOUNTS,
                rejectionReason = cleanReason,
                rejectedByUserId = accountsUserId,
                updatedAt = currentTime()
            )
        )
        addHistory(
            requisitionId,
            accountsUserId,
            "ACCOUNTS_REJECTED",
            cleanReason
        )
        sendNotification(
            req.distributorId,
            requisitionId,
            "Requisition Rejected",
            "Your requisition was rejected by Accounts. Reason: $cleanReason",
            NotificationType.ACCOUNTS_REJECTED
        )
        return true
    }

    // Factory acceptance
    @Synchronized
    fun acceptByFactory(
        requisitionId: String,
        factoryUserId: String,
        expectedDeliveryDate: String,
        factoryNotes: String = ""
    ): Boolean {
        val req = findRequisition(requisitionId) ?: return false
        if (req.status != ApprovalStatus.APPROVED_ACCOUNTS) return false
        if (req.assignedFactoryId != factoryUserId) return false
        if (expectedDeliveryDate.isBlank()) return false

        val cleanDate = expectedDeliveryDate.trim()
        updateRequisition(
            req.copy(
                status = ApprovalStatus.FACTORY_ACCEPTED,
                expectedDeliveryDate = cleanDate,
                factoryNotes = factoryNotes,
                updatedAt = currentTime()
            )
        )
        addHistory(
            requisitionId,
            factoryUserId,
            "FACTORY_ACCEPTED",
            "Factory accepted the requisition. Expected delivery: $cleanDate"
        )
        sendNotification(
            req.distributorId,
            requisitionId,
            "Factory Accepted Requisition",
            "The Factory accepted your requisition. Expected delivery: $cleanDate",
            NotificationType.FACTORY_ACCEPTED
        )
        return true
    }

    // Factory rejection
    @Synchronized
    fun rejectByFactory(
        requisitionId: String,
        factoryUserId: String,
        reason: String
    ): Boolean {
        val req = findRequisition(requisitionId) ?: return false
        if (
            req.status != ApprovalStatus.APPROVED_ACCOUNTS &&
            req.status != ApprovalStatus.FACTORY_ACCEPTED
        ) return false
        if (req.assignedFactoryId != factoryUserId) return false
        if (reason.isBlank()) return false

        val cleanReason = reason.trim()
        updateRequisition(
            req.copy(
                status = ApprovalStatus.REJECTED_FACTORY,
                rejectionReason = cleanReason,
                rejectedByUserId = factoryUserId,
                updatedAt = currentTime()
            )
        )
        addHistory(
            requisitionId,
            factoryUserId,
            "FACTORY_REJECTED",
            cleanReason
        )
        sendNotification(
            req.distributorId,
            requisitionId,
            "Factory Rejected Requisition",
            "The Factory rejected your requisition. Reason: $cleanReason",
            NotificationType.FACTORY_REJECTED
        )
        return true
    }

    // Raw material inventory
    @Synchronized
    fun setRawMaterialStock(
        materialName: String,
        quantity: Double
    ): Boolean {
        if (materialName.isBlank()) return false
        if (!quantity.isFinite() || quantity < 0.0) return false

        rawMaterialInventory[materialName.trim()] = quantity
        return true
    }

    @Synchronized
    fun getRawMaterialStock(): Map<String, Double> =
        rawMaterialInventory.toMap()

    @Synchronized
    fun getRawMaterialQuantity(materialName: String): Double =
        rawMaterialInventory[materialName.trim()] ?: 0.0

    // Factory production and stock deduction
    @Synchronized
    fun confirmFactoryProduction(
        requisitionId: String,
        factoryUserId: String,
        rawMaterialDeductions: Map<String, Double>
    ): Boolean {
        val req = findRequisition(requisitionId) ?: return false

        if (req.status != ApprovalStatus.FACTORY_ACCEPTED) return false
        if (req.assignedFactoryId != factoryUserId) return false
        if (req.rawMaterialsDeducted) return false
        if (rawMaterialDeductions.isEmpty()) return false

        // Normalize names and reject duplicate normalized keys
        val normalized = mutableMapOf<String, Double>()

        for ((name, quantity) in rawMaterialDeductions) {
            val key = name.trim()
            if (key.isEmpty()) return false
            if (!quantity.isFinite() || quantity <= 0.0) return false
            if (normalized.containsKey(key)) return false
            normalized[key] = quantity
        }

        // Check all stock before modifying anything
        for ((name, required) in normalized) {
            val available = rawMaterialInventory[name] ?: return false
            if (!available.isFinite() || available < required) return false
        }

        // Apply all deductions after successful validation
        for ((name, required) in normalized) {
            val available = rawMaterialInventory[name] ?: return false
            rawMaterialInventory[name] = available - required
        }

        updateRequisition(
            req.copy(
                status = ApprovalStatus.READY_FOR_DELIVERY,
                rawMaterialsDeducted = true,
                updatedAt = currentTime()
            )
        )
        addHistory(
            requisitionId,
            factoryUserId,
            "PRODUCTION_CONFIRMED",
            "Factory confirmed production and deducted raw materials."
        )
        sendNotification(
            req.distributorId,
            requisitionId,
            "Production Completed",
            "Your requisition is ready for delivery.",
            NotificationType.DELIVERY_READY
        )
        return true
    }

    // Delivery completion
    @Synchronized
    fun markAsDelivered(
        requisitionId: String,
        factoryUserId: String
    ): Boolean {
        val req = findRequisition(requisitionId) ?: return false
        if (req.status != ApprovalStatus.READY_FOR_DELIVERY) return false
        if (req.assignedFactoryId != factoryUserId) return false

        updateRequisition(
            req.copy(
                status = ApprovalStatus.DELIVERED,
                updatedAt = currentTime()
            )
        )
        addHistory(
            requisitionId,
            factoryUserId,
            "DELIVERED",
            "Requisition delivery completed."
        )
        sendNotification(
            req.distributorId,
            requisitionId,
            "Delivery Completed",
            "Your requisition has been delivered.",
            NotificationType.DELIVERY_COMPLETED
        )
        return true
    }

    // Private notifications
    @Synchronized
    fun getNotificationsForUser(userId: String): List<RequisitionNotification> {
        if (userId.isBlank()) return emptyList()
        return notifications.filter {
            it.recipientUserId == userId
        }.sortedByDescending { it.createdAt }
    }

    @Synchronized
    fun getUnreadNotificationsForUser(userId: String): List<RequisitionNotification> {
        if (userId.isBlank()) return emptyList()
        return notifications.filter {
            it.recipientUserId == userId && !it.isRead
        }.sortedByDescending { it.createdAt }
    }

    @Synchronized
    fun getUnreadNotificationCount(userId: String): Int {
        if (userId.isBlank()) return 0
        return notifications.count {
            it.recipientUserId == userId && !it.isRead
        }
    }

    @Synchronized
    fun markNotificationAsRead(
        notificationId: String,
        userId: String
    ): Boolean {
        if (userId.isBlank()) return false

        val index = notifications.indexOfFirst {
            it.id == notificationId &&
                    it.recipientUserId == userId
        }
        if (index == -1) return false

        notifications[index] = notifications[index].copy(isRead = true)
        return true
    }

    @Synchronized
    fun markAllNotificationsAsRead(userId: String): Boolean {
        if (userId.isBlank()) return false

        for (index in notifications.indices) {
            val notification = notifications[index]
            if (notification.recipientUserId == userId && !notification.isRead) {
                notifications[index] = notification.copy(isRead = true)
            }
        }
        return true
    }

    // Requisition history
    @Synchronized
    fun getHistoryForRequisition(
        requisitionId: String
    ): List<RequisitionHistoryEntry> =
        history.filter {
            it.requisitionId == requisitionId
        }.sortedBy { it.createdAt }

    @Synchronized
    fun getHistoryForUser(
        requisitionId: String,
        userId: String
    ): List<RequisitionHistoryEntry> {
        if (userId.isBlank()) return emptyList()

        val req = findRequisition(requisitionId) ?: return emptyList()
        val authorized =
            req.distributorId == userId ||
                    req.assignedSupervisorId == userId ||
                    req.assignedAccountsId == userId ||
                    req.assignedFactoryId == userId

        if (!authorized) return emptyList()
        return getHistoryForRequisition(requisitionId)
    }
}
