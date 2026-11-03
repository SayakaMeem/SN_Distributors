package com.example.sn_distributors.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.sn_distributors.ui.theme.*
import java.util.Locale

data class PortalProduct(
    val id: String,
    val code: String,
    val name: String,
    val category: String,
    val recipeBreakdown: String,
    val basePrice: Double,
    val discountedPrice: Double,
    val unit: String
)

data class CartItem(
    val product: PortalProduct,
    var quantity: Int
)

data class RequisitionItem(
    val id: String,
    val items: List<CartItem>,
    val totalAmount: Double,
    val paymentStatus: String,
    var approvalStatus: String
)

@Composable
fun DistributorPortalScreen(
    distributorToken: String? = null,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("Welcome") }
    var searchQuery by remember { mutableStateOf("") }

    val cart = remember { mutableStateListOf<CartItem>() }
    val requisitions = remember {
        mutableStateListOf(
            RequisitionItem(
                id = "DR-0007",
                items = listOf(
                    CartItem(
                        PortalProduct(
                            "P-001", "SN-ULTRA-TOP", "SN-ULTRA-TOP 17 KG SET",
                            "EPOXY WALL & FLOOR", "Resin Part-A (14.5 kg) + Hardener Part-B (2.5 kg)",
                            45000.0, 42004.20, "set"
                        ),
                        6
                    )
                ),
                totalAmount = 252025.20,
                paymentStatus = "On due",
                approvalStatus = "Pending Supervisor Approval"
            )
        )
    }

    val products = listOf(
        PortalProduct("P-001", "SN-ULTRA-TOP", "SN-ULTRA-TOP 17 KG SET", "EPOXY WALL & FLOOR", "Resin Part-A + Hardener Part-B", 45000.0, 42004.20, "set"),
        PortalProduct("P-002", "APPLE-WHITE", "APPLE WHITE", "EPOXY COATING", "SN-100 SLE White Pigment Base + Hardener", 4800.0, 4188.88, "unit"),
        PortalProduct("P-003", "SN-CRETE-MF", "SN-CRETE MF - BLUE", "CEMENTITIOUS FLOORING", "Heavy Duty Aggregate Blend + Liquid Polymer", 2700.0, 2380.00, "bag")
    )

    val filteredProducts = remember(searchQuery, products) {
        if (searchQuery.isBlank()) products else products.filter {
            it.name.contains(searchQuery, true) || it.code.contains(searchQuery, true)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = SurfaceOffWhite,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(ForestGreenPrimary)
                    .statusBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = "SN CHEMICAL INDUSTRIES", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        Text(text = "PARTNERS & STOCK PORTAL", color = ForestGreenContainer, fontSize = 11.sp, letterSpacing = 1.5.sp)
                    }
                    Surface(color = ForestGreenButton, shape = RoundedCornerShape(20.dp)) {
                        Text(
                            text = distributorToken ?: "DST-0001",
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Fully interactive Tab Navigation Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Welcome", "Dashboard", "Products").forEach { tab ->
                        val selected = selectedTab == tab
                        TextButton(
                            onClick = { selectedTab = tab },
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = when(tab) {
                                            "Products" -> "Stock Catalog"
                                            else -> tab
                                        },
                                        color = if (selected) Color.White else ForestGreenContainer,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                        fontSize = 13.sp
                                    )
                                    if (tab == "Products" && cart.isNotEmpty()) {
                                        Spacer(Modifier.width(6.dp))
                                        Badge(containerColor = AlertRedBadge) {
                                            Text("${cart.sumOf { it.quantity }}", color = Color.White)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(48.dp)
                                        .height(3.dp)
                                        .background(
                                            if (selected) Color.White else Color.Transparent,
                                            RoundedCornerShape(2.dp)
                                        )
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            when (selectedTab) {
                "Welcome" -> {
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(72.dp),
                                shape = CircleShape,
                                color = ForestGreenContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Welcome to Partners & Stock",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    text = "Manage distributor networks, live inventory, and supply chain orders.",
                                    fontSize = 13.sp,
                                    color = TextMutedSecondary
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "PARTNER OPERATIONS",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreenPrimary,
                                        letterSpacing = 1.2.sp
                                    )
                                    Text(
                                        text = "Naha Construction & Registered Distributors",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkPrimary
                                    )
                                    Text(
                                        text = "Access live stock inventories, review credit limits, and submit multi-stage requisitions instantly.",
                                        fontSize = 13.sp,
                                        color = TextMutedSecondary
                                    )

                                    HorizontalDivider(color = CardBorderLight, modifier = Modifier.padding(vertical = 4.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { selectedTab = "Dashboard" },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenButton),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Open Dashboard")
                                        }
                                        OutlinedButton(
                                            onClick = { selectedTab = "Products" },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("View Stock Catalog")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                "Dashboard" -> {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                        ) {
                            Column(modifier = Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Text(text = "PARTNERS & STOCK PORTAL", color = ForestGreenPrimary, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
                                Text(text = "NAHA CONSTRUCTION", color = TextDarkPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                                Text(text = "Review active partner accounts, current stock balances, and requisition pipelines.", color = TextMutedSecondary, fontSize = 13.sp)

                                HorizontalDivider(color = CardBorderLight, modifier = Modifier.padding(vertical = 4.dp))

                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Column {
                                        Text("Credit Limit", color = TextMutedSecondary, fontSize = 11.sp)
                                        Text("৳ 500,000.00", color = ForestGreenPrimary, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    Column {
                                        Text("Current Balance", color = TextMutedSecondary, fontSize = 11.sp)
                                        Text("৳ 142,500.00", color = AccentGold, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                }
                            }
                        }
                    }

                    if (cart.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = ForestGreenContainer),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = ForestGreenPrimary)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(text = "Cart (${cart.sumOf { it.quantity }} items)", fontWeight = FontWeight.Bold, color = ForestGreenPrimary)
                                            val total = cart.sumOf { it.product.discountedPrice * it.quantity }
                                            Text(text = "Total: ৳ ${String.format(Locale.US, "%,.2f", total)}", fontSize = 12.sp, color = TextMutedSecondary)
                                        }
                                    }
                                    Button(
                                        onClick = {
                                            if (cart.isNotEmpty()) {
                                                val item = cart.first()
                                                val newReq = RequisitionItem(
                                                    id = "DR-000" + (requisitions.size + 8),
                                                    items = listOf(item),
                                                    totalAmount = item.product.discountedPrice * item.quantity,
                                                    paymentStatus = "On due",
                                                    approvalStatus = "Pending Supervisor Approval"
                                                )
                                                requisitions.add(0, newReq)
                                                cart.clear()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = ForestGreenButton),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Requisition All")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(text = "Active Requisitions & Approval Pipeline", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                    }

                    items(requisitions, key = { it.id }) { req ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = req.id, fontWeight = FontWeight.Bold, color = ForestGreenPrimary, fontSize = 16.sp)
                                    Surface(
                                        color = if (req.approvalStatus == "Ready for Delivery") Color(0xFFDCFCE7) else ForestGreenContainer,
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = req.approvalStatus,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextDarkPrimary
                                        )
                                    }
                                }

                                req.items.forEach { cartItem ->
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text(text = "${cartItem.product.name} x ${cartItem.quantity}", fontSize = 13.sp, color = TextDarkPrimary)
                                        Text(text = "৳ ${String.format(Locale.US, "%,.2f", cartItem.product.discountedPrice * cartItem.quantity)}", fontSize = 13.sp, color = TextMutedSecondary)
                                    }
                                }

                                HorizontalDivider(color = CardBorderLight)

                                Button(
                                    onClick = {
                                        val index = requisitions.indexOf(req)
                                        if (index != -1) {
                                            val nextStatus = when (req.approvalStatus) {
                                                "Pending Supervisor Approval" -> "Approved by Supervisor"
                                                "Approved by Supervisor" -> "Approved by Accounts"
                                                "Approved by Accounts" -> "Factory Accepted — Preparing"
                                                else -> "Ready for Delivery"
                                            }
                                            requisitions[index] = req.copy(approvalStatus = nextStatus)
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = ForestGreenButton),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("Simulate Next Approval Step")
                                }
                            }
                        }
                    }
                }
                "Products" -> {
                    item {
                        Text(text = "Stock Catalog & Direct Order", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                        Text(text = "Select partner items and check available stock", fontSize = 12.sp, color = TextMutedSecondary)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search by name or code...") },
                            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    items(filteredProducts, key = { it.id }) { product ->
                        var quantity by remember { mutableIntStateOf(1) }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(product.code, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                                    Text(product.unit, fontSize = 11.sp, color = TextMutedSecondary)
                                }
                                Text(product.name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = TextDarkPrimary)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(SurfaceOffWhite, RoundedCornerShape(6.dp))
                                        .border(1.dp, CardBorderLight, RoundedCornerShape(6.dp))
                                        .padding(8.dp)
                                ) {
                                    Text(text = "Recipe: ${product.recipeBreakdown}", fontSize = 11.sp, color = TextMutedSecondary)
                                }
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(text = "৳ ${String.format(Locale.US, "%,.2f", product.discountedPrice)}", fontWeight = FontWeight.Bold, color = ForestGreenPrimary, fontSize = 18.sp)
                                    Spacer(Modifier.width(8.dp))
                                    Text(text = "৳ ${String.format(Locale.US, "%,.2f", product.basePrice)}", style = MaterialTheme.typography.bodySmall.copy(textDecoration = TextDecoration.LineThrough), color = TextMutedSecondary, fontSize = 12.sp)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        OutlinedButton(onClick = { if (quantity > 1) quantity-- }, modifier = Modifier.size(32.dp), contentPadding = PaddingValues(0.dp)) { Text("-") }
                                        Text(quantity.toString(), fontWeight = FontWeight.Bold)
                                        OutlinedButton(onClick = { quantity++ }, modifier = Modifier.size(32.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedButton(
                                            onClick = {
                                                val existing = cart.find { it.product.id == product.id }
                                                if (existing != null) {
                                                    existing.quantity += quantity
                                                } else {
                                                    cart.add(CartItem(product, quantity))
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("Add to Cart", fontSize = 12.sp)
                                        }
                                        Button(
                                            onClick = {
                                                val newReq = RequisitionItem(
                                                    id = "DR-000" + (requisitions.size + 8),
                                                    items = listOf(CartItem(product, quantity)),
                                                    totalAmount = product.discountedPrice * quantity,
                                                    paymentStatus = "On due",
                                                    approvalStatus = "Pending Supervisor Approval"
                                                )
                                                requisitions.add(0, newReq)
                                                selectedTab = "Dashboard"
                                            },
                                            colors = ButtonDefaults.buttonColors(containerColor = ForestGreenButton),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("Requisition this only", fontSize = 12.sp)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}