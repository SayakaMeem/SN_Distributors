package com.example.sn_distributors.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.CrmRepository
import com.example.sn_distributors.ui.components.OrderAmountBadge
import com.example.sn_distributors.ui.components.CopyOrderId
import java.util.Locale

@Composable
fun RoleDashboardScreen(
    user: UserAccount,
    accounts: List<UserAccount>,
    orders: List<CrmTask>,
    products: List<ProductItem>,
    repository: CrmRepository,
    cartQuantity: Int,
    navigate: (String) -> Unit
) {
    val visible = orders.filter { repository.canAccess(user, it) && it.stage != null }
    val assigned = visible.filter { repository.canAct(user, it) || repository.canClaim(user, it, accounts) }
    val now = System.currentTimeMillis()
    val roleTitle = when (user.role) {
        UserRole.ADMIN -> "Admin"
        UserRole.DISTRIBUTOR -> "Distributor"
        UserRole.STAFF -> when (user.department) {
            StaffDepartment.SUPERVISOR -> "Supervisor"
            StaffDepartment.ACCOUNTS -> "Accounts"
            StaffDepartment.FACTORY -> "Factory"
            null -> "Staff"
        }
    }
    val orderDestination = when (user.role) {
        UserRole.ADMIN -> "Orders"
        UserRole.STAFF -> "Approvals"
        UserRole.DISTRIBUTOR -> "My orders"
    }
    val workStatuses = visible.mapNotNull { repository.workStatus(user, it) ?: if (repository.canClaim(user, it, accounts)) it.status else null }
    val workMetrics = listOf("Pending" to workStatuses.count { it == CrmTaskStatus.PENDING }.toString(),
        "In progress" to workStatuses.count { it == CrmTaskStatus.IN_PROGRESS }.toString(),
        "Done" to workStatuses.count { it == CrmTaskStatus.COMPLETED }.toString())
    val metrics = workMetrics + when (user.role) {
        UserRole.ADMIN -> listOf(
            "Total orders" to visible.size.toString(),
            "Open orders" to visible.count { it.status != CrmTaskStatus.COMPLETED }.toString(),
            "Delivered order value" to dashboardMoney(visible.filter { it.stage == OrderStage.DELIVERED }.sumOf { it.totalAmount }),
            "Active staff" to accounts.count { it.role == UserRole.STAFF && it.isActive }.toString(),
            "Active distributors" to accounts.count { it.role == UserRole.DISTRIBUTOR && it.isActive }.toString(),
            "Catalog products" to products.size.toString()
        )
        UserRole.DISTRIBUTOR -> listOf(
            "My total orders" to visible.size.toString(),
            "Awaiting approval" to visible.count { it.stage in listOf(OrderStage.SUPERVISOR, OrderStage.ACCOUNTS, OrderStage.FACTORY) }.toString(),
            "In production / ready" to visible.count { it.stage in listOf(OrderStage.PRODUCTION, OrderStage.READY) }.toString(),
            "Delivered orders" to visible.count { it.stage == OrderStage.DELIVERED }.toString(),
            "Rejected orders" to visible.count { it.stage == OrderStage.REJECTED }.toString(),
            "Cart quantity" to cartQuantity.toString()
        )
        UserRole.STAFF -> buildList {
            add("Awaiting my review" to assigned.count { it.stage in listOf(OrderStage.SUPERVISOR, OrderStage.ACCOUNTS, OrderStage.FACTORY) }.toString())
            add("My overdue tasks" to assigned.count { it.dueAt < now }.toString())
            add("Orders I worked on" to visible.count { task -> task.history.any { it.actorId == user.id } }.toString())
            when (user.department) {
                StaffDepartment.ACCOUNTS -> add("Value awaiting my clearance" to dashboardMoney(assigned.sumOf { it.totalAmount }))
                StaffDepartment.FACTORY -> {
                    add("My production orders" to assigned.count { it.stage == OrderStage.PRODUCTION }.toString())
                    add("My ready deliveries" to assigned.count { it.stage == OrderStage.READY }.toString())
                }
                else -> Unit
            }
        }
    }
    val shortcuts = when (user.role) {
        UserRole.ADMIN -> listOf("Review all orders" to "Orders", "Manage products" to "Products", "Manage accounts" to "Accounts")
        UserRole.DISTRIBUTOR -> listOf("Browse products" to "Shop", "Open cart" to "Cart", "Track my orders" to "My orders")
        UserRole.STAFF -> listOf("Open my work queue" to "Approvals", "Manage catalog" to "Products", "View distributors" to "Distributors")
    } + if (repository.canManageMaterials(user)) listOf("Raw materials and recipes" to "Materials") else emptyList()
    val recent = if (user.role == UserRole.STAFF) assigned.sortedBy { it.dueAt }.take(5)
        else visible.sortedByDescending { it.updatedAt }.take(5)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("$roleTitle dashboard", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Welcome, ${user.name}")
            Text(when (user.role) {
                UserRole.ADMIN -> "Monitor orders, delivery performance, products and accounts."
                UserRole.DISTRIBUTOR -> "Place orders and follow your approvals, production and delivery."
                UserRole.STAFF -> when (user.department) {
                    StaffDepartment.SUPERVISOR -> "Review assigned distributor orders and send approved orders to Accounts."
                    StaffDepartment.ACCOUNTS -> "Review assigned orders for financial clearance before Factory approval."
                    StaffDepartment.FACTORY -> "Approve assigned production, set delivery dates and complete deliveries."
                    null -> "Contact Admin to assign your department."
                }
            })
        }
        items(metrics) { (label, value) ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text(label)
                }
            }
        }
        item { Text("Quick actions", style = MaterialTheme.typography.titleMedium) }
        items(shortcuts) { (label, destination) ->
            Button(onClick = { navigate(destination) }, modifier = Modifier.fillMaxWidth()) { Text(label) }
        }
        item { Text(if (user.role == UserRole.STAFF) "Assigned work" else "Recent orders", style = MaterialTheme.typography.titleMedium) }
        if (recent.isEmpty()) item { Text(if (user.role == UserRole.STAFF) "No orders need your action." else "No orders yet.") }
        items(recent, key = { it.id }) { order ->
            OutlinedCard(onClick = { navigate(orderDestination) }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(order.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                        if (user.role == UserRole.DISTRIBUTOR) OrderAmountBadge(order.totalAmount, order.items.sumOf { it.quantity })
                    }
                    CopyOrderId(order.id, showId = false)
                    if (order.isPaused) Text("Approval paused: ${order.pauseReason}", color = MaterialTheme.colorScheme.error)
                    if (user.role != UserRole.DISTRIBUTOR) Text(order.distributorName)
                    Text(order.stage?.label.orEmpty())
                    Text(dashboardMoney(order.totalAmount))
                }
            }
        }
    }
}

private fun dashboardMoney(amount: Double) = "BDT " + String.format(Locale.US, "%,.2f", amount)
