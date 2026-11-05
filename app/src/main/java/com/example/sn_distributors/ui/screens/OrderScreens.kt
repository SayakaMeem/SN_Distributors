package com.example.sn_distributors.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.CrmRepository
import com.example.sn_distributors.ui.components.ScrollableDialogBody
import com.example.sn_distributors.ui.components.printOrder
import com.example.sn_distributors.ui.components.OrderAmountBadge
import com.example.sn_distributors.ui.components.CopyOrderId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.ParsePosition
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private fun dateText(time: Long) = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(time))
private fun money(amount: Double) = "BDT " + String.format(Locale.US, "%,.2f", amount)
private fun parseDate(text: String): Long {
    val position = ParsePosition(0)
    val date = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).apply { isLenient = false }.parse(text, position)
    require(date != null && position.index == text.length) { "Use a valid date and time: yyyy-MM-dd HH:mm." }
    return date.time
}

@Composable
fun OrdersScreen(user: UserAccount, accounts: List<UserAccount>, repository: CrmRepository,
                       onLogout: () -> Unit, fixedDistributor: String? = null, recentOrderId: String? = null,
                       printDocument: (Context, CrmTask, OrderPrint) -> Unit = ::printOrder) {
    val all by repository.tasks.collectAsState()
    var filter by remember { mutableStateOf<CrmTaskStatus?>(null) }
    var myWork by remember { mutableStateOf(user.role == UserRole.STAFF && fixedDistributor == null) }
    var distributorFilter by remember(fixedDistributor) { mutableStateOf(fixedDistributor.orEmpty()) }
    var selectedId by remember { mutableStateOf<String?>(null) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    val visible = all.filter { repository.canAccess(user, it) }
    val scoped = visible.filter { (!myWork || repository.workStatus(user, it) != null || repository.canClaim(user, it, accounts)) &&
        (distributorFilter.isBlank() || it.distributorId == distributorFilter) }
    fun status(task: CrmTask) = if (myWork || user.role != UserRole.STAFF) repository.workStatus(user, task) ?: task.status else task.status
    LaunchedEffect(Unit) { while (true) { now = System.currentTimeMillis(); delay(30_000) } }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text(when (user.role) {
                UserRole.DISTRIBUTOR -> "Distributor orders"
                UserRole.STAFF -> "${user.department?.name ?: "Staff"} approvals"
                UserRole.ADMIN -> "Distributor order overview"
            }, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("${user.name} · ${user.department?.name ?: user.role.name}")
            if (user.role == UserRole.DISTRIBUTOR) {
                visible.firstOrNull { it.id == recentOrderId }?.let { submitted ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(12.dp)) {
                            Text("Order placed successfully", fontWeight = FontWeight.Bold)
                            Text("${submitted.title} - approval request sent to Supervisor.")
                            TextButton(onClick = { selectedId = submitted.id }) { Text("Track submitted order") }
                        }
                    }
                }
            }
            Row {
                TextButton(onClick = onLogout) { Text("Logout") }
            }
            Text("Distributor submits → Supervisor → Accounts → Factory → Production → Delivery")
        }
        if (user.role != UserRole.DISTRIBUTOR) {
            item {
                Text("Distributor details", style = MaterialTheme.typography.titleMedium)
                val distributors = accounts.filter { it.role == UserRole.DISTRIBUTOR }
                Choice("Distributor", distributorFilter, listOf("") + distributors.map { it.id }, { id ->
                    if (id.isBlank()) "All distributors" else distributors.firstOrNull { it.id == id }?.let { "${it.name} · ${it.email}" } ?: id
                }) { distributorFilter = it }
                val account = distributors.firstOrNull { it.id == distributorFilter }
                if (account != null) Text("${account.name}\n${account.email}\nID: ${account.id}\nStatus: ${account.status}")
                if (user.role == UserRole.STAFF) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(myWork, { myWork = true }, label = { Text("My approval tasks") })
                    FilterChip(!myWork, { myWork = false }, label = { Text("All orders") })
                }
                Text("Open an order to see delivery details, products, quantities, prices, approvals and history.")
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CrmTaskStatus.entries.forEach { s ->
                    OutlinedCard(onClick = { filter = if (filter == s) null else s }, modifier = Modifier.weight(1f)) {
                        Column(Modifier.padding(8.dp)) {
                            Text(scoped.count { status(it) == s }.toString(), style = MaterialTheme.typography.titleLarge)
                            Text(s.label, style = MaterialTheme.typography.labelMedium)
                            if (filter == s) Text("Selected", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
            TextButton(onClick = { filter = null }) { Text("Show all statuses") }
        }
        item { Text(if (myWork) "My approval tasks" else "Purchase orders", style = MaterialTheme.typography.titleLarge) }
        val rows = scoped.filter { filter == null || status(it) == filter }
            .sortedWith(compareBy<CrmTask> { status(it) == CrmTaskStatus.COMPLETED }.thenBy { it.dueAt })
        if (rows.isEmpty()) item { Text(if (myWork) "No approvals are waiting for your role." else "No orders in this view.") }
        items(rows, key = { it.id }) { task ->
            OutlinedCard(onClick = { selectedId = task.id }, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(task.title, fontWeight = FontWeight.Bold)
                            Text("Distributor: ${task.distributorName.ifBlank { "Legacy record" }}")
                            Text("${task.items.size} product(s)")
                        }
                        if (user.role == UserRole.DISTRIBUTOR) OrderAmountBadge(task.totalAmount, task.items.sumOf { it.quantity })
                    }
                    CopyOrderId(task.id)
                    if (task.isPaused) Text("Approval paused: ${task.pauseReason}", color = MaterialTheme.colorScheme.error)
                    Text(task.stage?.label ?: "Archived record · read only")
                    if (task.status != CrmTaskStatus.COMPLETED) Text("With: ${accounts.firstOrNull { it.id == task.assignedUserId }?.name ?: task.assignedName} (${task.assignedDepartment})")
                    task.expectedDeliveryAt?.let { Text("Expected delivery: ${dateText(it)}") }
                    Text("Your task: ${status(task).label} · Total: ${money(task.totalAmount)}")
                    Text("Required by: ${dateText(task.dueAt)}${if (task.dueAt < now && task.status != CrmTaskStatus.COMPLETED) " · OVERDUE" else ""}")
                    Text("Updated: ${dateText(task.updatedAt)}", style = MaterialTheme.typography.bodySmall)
                    Text("View order & history", color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
    val selected = visible.firstOrNull { it.id == selectedId }
    if (selected != null) key(selected.id) { OrderDetails(user, selected, accounts, repository, printDocument) { selectedId = null } }
}

@Composable
private fun OrderProgress(task: CrmTask) {
    if (task.stage == null) return
    val stages = listOf(OrderStage.SUPERVISOR to "Supervisor approval", OrderStage.ACCOUNTS to "Accounts approval",
        OrderStage.FACTORY to "Factory acceptance & delivery date", OrderStage.PRODUCTION to "Production",
        OrderStage.READY to "Ready for delivery", OrderStage.DELIVERED to "Delivery completed")
    val position = stages.indexOfFirst { it.first == task.stage }
    OutlinedCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Order tracking", fontWeight = FontWeight.Bold)
            if (task.stage == OrderStage.REJECTED) Text("Order rejected. See the reason in approval history.", color = MaterialTheme.colorScheme.error)
            else stages.forEachIndexed { index, (stage, label) ->
                val status = when {
                    index == position && task.isPaused -> "Paused"
                    index < position || task.stage == OrderStage.DELIVERED -> "Done"
                    index > position -> "Waiting"
                    stage == OrderStage.PRODUCTION || stage == OrderStage.READY || task.status == CrmTaskStatus.IN_PROGRESS -> "In progress"
                    else -> "Pending"
                }
                Text("$label: $status")
            }
        }
    }
}

@Composable
private fun Field(value: String, label: String, change: (String) -> Unit) {
    OutlinedTextField(value, change, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun <T> Choice(label: String, value: T, options: List<T>, display: (T) -> String, change: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) { Text("$label: ${display(value)}") }
        DropdownMenu(expanded, { expanded = false }) {
            options.forEach { option -> DropdownMenuItem(text = { Text(display(option)) }, onClick = { change(option); expanded = false }) }
        }
    }
}

@Composable
private fun OrderDetails(user: UserAccount, task: CrmTask, accounts: List<UserAccount>, repository: CrmRepository,
                         printDocument: (Context, CrmTask, OrderPrint) -> Unit, dismiss: () -> Unit) {
    val context = LocalContext.current
    val manufacturing by repository.manufacturing.collectAsState()
    var note by remember(task.updatedAt) { mutableStateOf("") }
    var delivery by remember(task.updatedAt) { mutableStateOf(dateText(task.expectedDeliveryAt ?: System.currentTimeMillis() + 7 * 86_400_000L)) }
    var error by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var confirmProduction by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun claim() {
        saving = true
        scope.launch {
            try { withContext(Dispatchers.IO) { repository.claimRequest(user, task.id, accounts) }; error = "" }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not claim request." }
            finally { saving = false }
        }
    }
    fun openPrint(record: OrderPrint) {
        try { printDocument(context, task, record) }
        catch (e: Exception) { error = "Document is saved. Could not open printing: ${e.message}. Use Reprint to try again." }
    }
    fun issue(kind: PrintKind) {
        saving = true
        scope.launch {
            try {
                val record = withContext(Dispatchers.IO) { repository.issuePrint(user, task.id, kind) }
                error = ""
                confirmProduction = false
                openPrint(record)
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) { error = e.message ?: "Could not issue print." }
            finally { saving = false }
        }
    }
    fun act(action: OrderAction) {
        try {
            val date = if (action == OrderAction.APPROVE && task.stage == OrderStage.FACTORY) parseDate(delivery) else null
            saving = true
            scope.launch {
                try {
                    withContext(Dispatchers.IO) { repository.act(user, task.id, action, note, date, accounts) }
                    error = ""
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "Could not save approval." }
                finally { saving = false }
            }
        } catch (e: Exception) { error = e.message ?: "Check the review details." }
    }
    AlertDialog(onDismissRequest = { if (!saving) dismiss() }, title = { Text("Order details") }, text = {
        ScrollableDialogBody(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(task.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                if (user.role == UserRole.DISTRIBUTOR) OrderAmountBadge(task.totalAmount, task.items.sumOf { it.quantity })
            }
            CopyOrderId(task.id)
            if (task.isPaused) Text("Approval paused: ${task.pauseReason}", color = MaterialTheme.colorScheme.error)
            Text("Distributor: ${task.distributorName}\nEmail: ${task.distributorEmail}\nAccount ID: ${task.distributorId ?: "Legacy record"}")
            Text("Contact: ${task.customer.contact}\nDelivery address: ${task.deliveryAddress.ifBlank { "Not recorded" }}\nOrder note: ${task.orderNote.ifBlank { "None" }}")
            HorizontalDivider()
            Text("Products & purchase details", fontWeight = FontWeight.Bold)
            task.items.forEach { item ->
                Text("${item.product.name}\n${item.quantity} ${item.product.unit} × ${money(item.product.discountedPrice)} = ${money(item.quantity * item.product.discountedPrice)}\n${item.product.recipeBreakdown}")
            }
            Text("Total: ${money(task.totalAmount)}", fontWeight = FontWeight.Bold)
            Text("Status: ${task.stage?.label ?: "Archived record · read only"}\nRequired by: ${dateText(task.dueAt)}")
            if (task.expectedDeliveryAt != null) Text("Expected delivery: ${dateText(task.expectedDeliveryAt)}")
            OrderProgress(task)
            if (repository.canClaim(user, task, accounts)) {
                Text("This request is assigned to a demo or unavailable reviewer in your department.")
                Button(enabled = !saving, onClick = ::claim, modifier = Modifier.fillMaxWidth()) { Text("Claim approval request") }
            }
            if (task.stage == OrderStage.PRODUCTION) Text(if (task.prints.any { it.kind == PrintKind.PRODUCTION })
                "Production print issued. Raw materials deducted; production is in progress."
                else "Factory accepted. Production print is pending; raw materials have not been deducted.")
            Text("Supervisor: ${accounts.firstOrNull { it.id == task.supervisorId }?.name ?: task.supervisorId.orEmpty()}\nAccounts: ${accounts.firstOrNull { it.id == task.accountsId }?.name ?: task.accountsId.orEmpty()}\nFactory: ${accounts.firstOrNull { it.id == task.factoryId }?.name ?: task.factoryId.orEmpty()}")
            if (repository.canAct(user, task)) {
                HorizontalDivider()
                Text("Your approval action", fontWeight = FontWeight.Bold)
                if (task.stage == OrderStage.FACTORY) Field(delivery, "Expected delivery: yyyy-MM-dd HH:mm *") { delivery = it }
                Field(note, "Review note / pause reason / outcome *") { note = it }
                val actions = when (task.stage) {
                    OrderStage.SUPERVISOR, OrderStage.ACCOUNTS, OrderStage.FACTORY ->
                        listOf(OrderAction.APPROVE, OrderAction.PAUSE)
                    OrderStage.PRODUCTION -> if (task.prints.any { it.kind == PrintKind.PRODUCTION }) listOf(OrderAction.READY) else emptyList()
                    OrderStage.READY -> listOf(OrderAction.DELIVER)
                    else -> emptyList()
                }
                if (task.stage == OrderStage.FACTORY) Text("Approving accepts the order and confirms the delivery date above.")
                actions.forEach { action -> Button(enabled = !saving && !(action == OrderAction.PAUSE && task.isPaused), onClick = { act(action) }, modifier = Modifier.fillMaxWidth()) {
                    Text(action.label)
                } }
                if (task.stage == OrderStage.PRODUCTION) {
                    OutlinedButton(enabled = !saving, onClick = { issue(PrintKind.TEST) }, modifier = Modifier.fillMaxWidth()) { Text("Issue test print") }
                    if (task.prints.none { it.kind == PrintKind.PRODUCTION }) {
                        Button(enabled = !saving, onClick = { confirmProduction = true }, modifier = Modifier.fillMaxWidth()) { Text("Issue production print") }
                        Text("Test print: casual reference; no stock deduction. Production print: deducts recipe materials once. Reprints never deduct again.")
                    }
                }
            } else Text(if (user.role == UserRole.DISTRIBUTOR) "Your order is read only after submission. Follow its approvals below." else "Read-only details. Only the assigned staff member at the current stage can approve.")
            if (task.prints.isNotEmpty()) {
                Text("Print records", fontWeight = FontWeight.Bold)
                task.prints.asReversed().forEach { record ->
                    Text("${record.kind} - ${dateText(record.at)} - ${record.actorName}")
                    if (user.role == UserRole.ADMIN || user.id == task.factoryId) {
                        record.materials.forEach { Text("${it.name}: ${it.quantity} ${it.unit} deducted") }
                        TextButton(enabled = !saving, onClick = { openPrint(record) }) { Text("Reprint ${record.kind.name.lowercase()} ${record.id.take(8)}") }
                    }
                }
            }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            HorizontalDivider()
            Text("Activity history · ${task.history.size} entries", fontWeight = FontWeight.Bold)
            task.history.asReversed().forEach { h ->
                Text("${dateText(h.at)} · ${h.actorName}", style = MaterialTheme.typography.labelMedium)
                Text(h.action, fontWeight = FontWeight.SemiBold)
                Text(h.details)
                HorizontalDivider()
            }
        }
    }, confirmButton = { TextButton(enabled = !saving, onClick = dismiss) { Text("Close") } })
    if (confirmProduction) {
        val preview = remember(task.id, manufacturing) { runCatching { repository.requiredMaterials(task) } }
        val enough = preview.getOrNull()?.all { use -> (manufacturing.materials.firstOrNull { it.id == use.materialId }?.stock ?: 0.0) >= use.quantity } == true
        AlertDialog(onDismissRequest = { if (!saving) confirmProduction = false }, title = { Text("Confirm production stock issue") }, text = {
            ScrollableDialogBody {
                Text("Confirming deducts the following raw materials immediately and saves the production document. Closing the printer dialog will not reverse the stock issue. You can reprint the saved document.")
                preview.getOrNull()?.forEach { use ->
                    val stock = manufacturing.materials.firstOrNull { it.id == use.materialId }?.stock ?: 0.0
                    Text("${use.name}: ${use.quantity} ${use.unit} required; $stock available")
                }
                preview.exceptionOrNull()?.let { Text(it.message.orEmpty(), color = MaterialTheme.colorScheme.error) }
                if (!enough) Text("Configure recipes and receive sufficient stock in Materials before production.", color = MaterialTheme.colorScheme.error)
                if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
            }
        }, confirmButton = { Button(enabled = !saving && enough, onClick = { issue(PrintKind.PRODUCTION) }) { Text("Deduct stock & issue print") } },
            dismissButton = { TextButton(enabled = !saving, onClick = { confirmProduction = false }) { Text("Back") } })
    }
}
