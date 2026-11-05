package com.example.sn_distributors.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.ManageAccounts
import androidx.compose.material.icons.filled.Warehouse
import androidx.compose.material.icons.automirrored.filled.FactCheck
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.mapSaver
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import com.example.sn_distributors.ui.components.ScrollableDialogBody
import com.example.sn_distributors.ui.components.OrderAmountBadge
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.UUID

private fun price(value: Double) = "BDT " + String.format(Locale.US, "%,.2f", value)

@Composable
fun CommerceWorkspace(user: UserAccount, accounts: List<UserAccount>, orders: CrmRepository,
                      catalog: ProductRepository, onLogout: () -> Unit, adminContent: @Composable () -> Unit) {
    val products by catalog.products.collectAsState()
    val activity = LocalActivity.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val orderList by orders.tasks.collectAsState()
    val tabs = when (user.role) {
        UserRole.DISTRIBUTOR -> listOf("Dashboard", "Shop", "Cart", "My orders")
        UserRole.STAFF -> listOf("Dashboard", "Approvals", "Products", "Distributors")
        UserRole.ADMIN -> listOf("Dashboard", "Orders", "Products", "Accounts")
    } + if (orders.canManageMaterials(user)) listOf("Materials") else emptyList()
    var routeHistory by rememberSaveable(user.id) { mutableStateOf("Dashboard") }
    val tab = routeHistory.substringAfterLast('|')
    var cart by rememberSaveable(user.id, stateSaver = mapSaver<Map<String, Int>>(
        save = { it }, restore = { values -> values.mapValues { it.value as Int } }
    )) { mutableStateOf(emptyMap()) }
    var placedOrderId by rememberSaveable(user.id) { mutableStateOf<String?>(null) }
    var distributorFilter by remember { mutableStateOf<String?>(null) }
    var checkout by remember { mutableStateOf(false) }
    var notice by remember { mutableStateOf("") }
    fun navigate(destination: String) {
        if (destination !in tabs) return
        focusManager.clearFocus()
        keyboard?.hide()
        routeHistory = when {
            destination == "Dashboard" -> "Dashboard"
            destination == tab -> routeHistory
            else -> "$routeHistory|$destination"
        }
        notice = ""
    }
    fun goBack() {
        focusManager.clearFocus()
        keyboard?.hide()
        distributorFilter = null
        routeHistory = routeHistory.substringBeforeLast('|', "Dashboard")
    }
    BackHandler(enabled = tab != "Dashboard") { goBack() }
    fun add(product: ProductItem) {
        val quantity = (cart[product.id] ?: 0) + 1
        if (quantity <= 100_000) cart = cart + (product.id to quantity)
        notice = "${product.name} added to cart."
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            Surface(tonalElevation = 2.dp) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                    IconButton(onClick = { if (tab == "Dashboard") activity?.finish() else goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                    Column(Modifier.weight(1f)) {
                        Text("SN Chemical", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("${user.name} · ${user.department?.name ?: user.role.name}", style = MaterialTheme.typography.labelMedium)
                    }
                    TextButton(onClick = onLogout) { Text("Logout") }
                }
            }
        },
        bottomBar = {
            NavigationBar(windowInsets = WindowInsets(0, 0, 0, 0)) {
                tabs.forEach { item ->
                    val badgeCount = when (item) {
                        "Cart" -> cart.values.sum()
                        "Approvals" -> orderList.count { orders.canAct(user, it) || orders.canClaim(user, it, accounts) }
                        else -> 0
                    }
                    NavigationBarItem(selected = tab == item, onClick = {
                        navigate(item)
                        if (item in listOf("Approvals", "Orders", "My orders")) distributorFilter = null
                    },
                        icon = {
                            BadgedBox(badge = { if (badgeCount > 0) Badge { Text(badgeCount.toString()) } }) {
                                Icon(when (item) {
                                    "Dashboard" -> Icons.Default.Dashboard
                                    "Shop" -> Icons.Default.Storefront
                                    "Cart" -> Icons.Default.ShoppingCart
                                    "Products" -> Icons.Default.Inventory2
                                    "Distributors" -> Icons.Default.Groups
                                    "Accounts" -> Icons.Default.ManageAccounts
                                    "Materials" -> Icons.Default.Warehouse
                                    "Approvals" -> Icons.AutoMirrored.Filled.FactCheck
                                    else -> Icons.AutoMirrored.Filled.ReceiptLong
                                }, contentDescription = null)
                            }
                        }, label = { Text(item) })
                }
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) {
            when (tab) {
                "Dashboard" -> RoleDashboardScreen(user, accounts, orderList, products, orders, cart.values.sum(), ::navigate)
                "Shop" -> ShopScreen(products, notice, ::add) { navigate("Cart") }
                "Cart" -> CartScreen(products, cart, { id, count ->
                    cart = if (count <= 0) cart - id else cart + (id to count.coerceAtMost(100_000))
                }, { checkout = true }, { navigate("Shop") })
                "Products" -> ProductManagementScreen(user, catalog)
                "Materials" -> MaterialsScreen(user, orders, products)
                "Accounts" -> adminContent()
                "Distributors" -> LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    item { Text("Distributors", style = MaterialTheme.typography.headlineSmall) }
                    val distributors = accounts.filter { it.role == UserRole.DISTRIBUTOR }
                    if (distributors.isEmpty()) item { Text("No distributor accounts.") }
                    items(distributors, key = { it.id }) { distributor ->
                        OutlinedCard(Modifier.fillMaxWidth()) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(distributor.name, fontWeight = FontWeight.Bold)
                                Text("${distributor.email}\nAccount: ${distributor.id}\nStatus: ${distributor.status}")
                                val rows = orderList.filter { it.distributorId == distributor.id }
                                Text("${rows.size} orders · ${price(rows.sumOf { it.totalAmount })} ordered")
                                TextButton(onClick = { distributorFilter = distributor.id; navigate("Approvals") }) { Text("View orders") }
                            }
                        }
                    }
                }
                else -> key(tab, distributorFilter) {
                    OrdersScreen(user, accounts, orders, onLogout, distributorFilter, placedOrderId)
                }
            }
        }
    }
    if (checkout && user.role == UserRole.DISTRIBUTOR) {
        val rows = cart.mapNotNull { (id, quantity) -> products.firstOrNull { it.id == id }?.let { CartItem(it, quantity) } }
        CheckoutDialog(user, rows, cart.size != rows.size, accounts, orders, { checkout = false }) { submitted ->
            placedOrderId = submitted.id
            checkout = false; cart = emptyMap(); distributorFilter = null; navigate("My orders")
        }
    }
}

@Composable
private fun ShopScreen(products: List<ProductItem>, notice: String, add: (ProductItem) -> Unit, openCart: () -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    val rows = products.filter { it.name.contains(search, true) || it.code.contains(search, true) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            Text("Construction chemicals & coatings", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text("Browse products, add quantities to your cart and place an order.")
            OutlinedTextField(search, { search = it }, label = { Text("Search products") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            if (notice.isNotBlank()) {
                Text(notice, color = MaterialTheme.colorScheme.primary)
                OutlinedButton(onClick = openCart) { Text("View cart & checkout") }
            }
        }
        if (rows.isEmpty()) item { Text("No products found.") }
        items(rows, key = { it.id }) { product ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(Icons.Default.Inventory2, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                        Column(Modifier.weight(1f)) {
                            Text(product.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(product.code, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    Text(product.recipeBreakdown.ifBlank { "Contact us for product specifications." })
                    Text("${price(product.discountedPrice)} / ${product.unit}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (product.basePrice > product.discountedPrice) Text("List price: ${price(product.basePrice)}", style = MaterialTheme.typography.bodySmall)
                    Button(onClick = { add(product) }, modifier = Modifier.fillMaxWidth()) { Text("Add to cart") }
                }
            }
        }
    }
}

@Composable
private fun CartScreen(products: List<ProductItem>, cart: Map<String, Int>, change: (String, Int) -> Unit,
                       checkout: () -> Unit, shop: () -> Unit) {
    val missing = cart.keys.any { id -> products.none { it.id == id } }
    val total = cart.entries.sumOf { (id, count) -> (products.firstOrNull { it.id == id }?.discountedPrice ?: 0.0) * count }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { Text("Your cart", style = MaterialTheme.typography.headlineSmall) }
        if (cart.isEmpty()) item {
            Text("Your cart is empty.")
            Button(onClick = shop) { Text("Browse products") }
        }
        items(cart.entries.toList(), key = { it.key }) { (id, count) ->
            val p = products.firstOrNull { it.id == id }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(p?.name ?: "Product no longer available", fontWeight = FontWeight.Bold)
                    if (p != null) {
                        Text("${price(p.discountedPrice)} / ${p.unit} · ${price(p.discountedPrice * count)}")
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedButton(onClick = { change(id, count - 1) }) { Text("−") }
                            Text("$count", modifier = Modifier.padding(12.dp))
                            OutlinedButton(onClick = { change(id, count + 1) }, enabled = count < 100_000) { Text("+") }
                        }
                    }
                    TextButton(onClick = { change(id, 0) }) { Text("Remove") }
                }
            }
        }
        if (cart.isNotEmpty()) item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Order summary", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                OrderAmountBadge(total, cart.values.sum())
            }
            if (missing) Text("Remove unavailable products before checkout.", color = MaterialTheme.colorScheme.error)
            Button(enabled = !missing, onClick = checkout, modifier = Modifier.fillMaxWidth()) { Text("Proceed to checkout") }
            Text("Your order will go through Supervisor, Accounts and Factory approval.")
        }
    }
}

@Composable
private fun CheckoutDialog(user: UserAccount, items: List<CartItem>, hasMissingItems: Boolean, accounts: List<UserAccount>,
                           orders: CrmRepository, dismiss: () -> Unit, placed: (CrmTask) -> Unit) {
    val requestId = rememberSaveable { UUID.randomUUID().toString() }
    var contact by remember { mutableStateOf(user.email) }
    var address by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val approvalRoute = remember(accounts) { runCatching { orders.approvalRoute(accounts) } }
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text("Checkout") }, text = {
        ScrollableDialogBody {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(user.name, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                OrderAmountBadge(items.sumOf { it.quantity * it.product.discountedPrice }, items.sumOf { it.quantity })
            }
            items.forEach { Text("${it.product.name} × ${it.quantity} · ${price(it.product.discountedPrice * it.quantity)}") }
            Text("Total: ${price(items.sumOf { it.quantity * it.product.discountedPrice })}", fontWeight = FontWeight.Bold)
            FormField(contact, "Contact phone / email *", enabled = !busy) { contact = it }
            FormField(address, "Delivery address *", enabled = !busy) { address = it }
            FormField(note, "Order note (optional)", enabled = !busy) { note = it }
            Text("Approval request route", fontWeight = FontWeight.Bold)
            approvalRoute.getOrNull()?.forEach { reviewer -> Text("${reviewer.department}: ${reviewer.name}") }
            approvalRoute.exceptionOrNull()?.let { Text(it.message.orEmpty(), color = MaterialTheme.colorScheme.error) }
            if (hasMissingItems) Text("A product was removed. Return to your cart.", color = MaterialTheme.colorScheme.error)
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        Button(enabled = !busy && !hasMissingItems && items.isNotEmpty() && approvalRoute.isSuccess, onClick = {
            if (address.isBlank() || contact.isBlank()) {
                error = "Enter your delivery address and contact details."
                return@Button
            }
            busy = true
            scope.launch {
                try {
                    val submitted = withContext(Dispatchers.IO) { orders.placeOrder(user, items, accounts, address, contact, note, requestId) }
                    placed(submitted)
                } catch (e: CancellationException) { throw e }
                catch (e: Exception) { error = e.message ?: "Could not place order." }
                finally { busy = false }
            }
        }) { Text(if (busy) "Placing order…" else "Place order") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Back to cart") } })
}

@Composable
fun ProductManagementScreen(user: UserAccount, repository: ProductRepository) {
    val products by repository.products.collectAsState()
    var editing by remember { mutableStateOf<ProductItem?>(null) }
    var adding by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf<ProductItem?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Manage products", style = MaterialTheme.typography.headlineSmall)
            Text("Catalog changes appear in the distributor shop. Existing orders keep their purchased details.")
            Button(onClick = { adding = true }) { Text("+ Add product") }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
        if (products.isEmpty()) item { Text("No products. Add your first product.") }
        items(products, key = { it.id }) { p ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(p.name, fontWeight = FontWeight.Bold)
                    Text("${p.code} · ${price(p.discountedPrice)} / ${p.unit}")
                    Text(p.recipeBreakdown)
                    Row {
                        TextButton(onClick = { editing = p }) { Text("Edit") }
                        TextButton(onClick = { deleting = p }) { Text("Delete") }
                    }
                }
            }
        }
    }
    if (adding || editing != null) ProductEditor(user, repository, editing) { adding = false; editing = null }
    val target = deleting
    if (target != null) AlertDialog(onDismissRequest = { if (!busy) deleting = null },
        title = { Text("Delete product?") }, text = { Text("Remove ${target.name} from the shop? Existing orders retain their saved details.") },
        confirmButton = {
            Button(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try { withContext(Dispatchers.IO) { repository.delete(user, target.id) }; deleting = null; error = "" }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = e.message ?: "Could not delete product."; deleting = null }
                    finally { busy = false }
                }
            }) { Text("Delete product") }
        }, dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null }) { Text("Cancel") } })
}

@Composable
private fun ProductEditor(user: UserAccount, repository: ProductRepository, original: ProductItem?, dismiss: () -> Unit) {
    var name by remember { mutableStateOf(original?.name.orEmpty()) }
    var code by remember { mutableStateOf(original?.code.orEmpty()) }
    var listPrice by remember { mutableStateOf(original?.basePrice?.toString().orEmpty()) }
    var salePrice by remember { mutableStateOf(original?.discountedPrice?.toString().orEmpty()) }
    var unit by remember { mutableStateOf(original?.unit.orEmpty()) }
    var description by remember { mutableStateOf(original?.recipeBreakdown.orEmpty()) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text(if (original == null) "Add product" else "Edit product") }, text = {
        ScrollableDialogBody {
            FormField(name, "Product name *") { name = it }
            FormField(code, "Product code / SKU *") { code = it }
            OutlinedTextField(listPrice, { listPrice = it }, label = { Text("List price (BDT) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            OutlinedTextField(salePrice, { salePrice = it }, label = { Text("Selling price (BDT) *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            FormField(unit, "Unit, e.g. bag / bucket / set *") { unit = it }
            FormField(description, "Description / specifications") { description = it }
            if (error.isNotBlank()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        Button(enabled = !busy, onClick = {
            val base = listPrice.toDoubleOrNull()
            val sale = salePrice.toDoubleOrNull()
            if (base == null || sale == null) error = "Enter numeric list and selling prices."
            else {
                busy = true
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) { repository.save(user, ProductItem(original?.id.orEmpty(), name, code, base, sale, unit, description), isNew = original == null) }
                        dismiss()
                    } catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = e.message ?: "Could not save product." }
                    finally { busy = false }
                }
            }
        }) { Text(if (busy) "Saving…" else "Save product") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Cancel") } })
}

@Composable
private fun FormField(value: String, label: String, enabled: Boolean = true, change: (String) -> Unit) {
    OutlinedTextField(value, change, enabled = enabled, label = { Text(label) }, modifier = Modifier.fillMaxWidth())
}
