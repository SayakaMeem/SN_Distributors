
package com.example.sn_distributors.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
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
    modifier: Modifier = Modifier,
    distributorToken: String? = null
) {
    var selectedTab by remember { mutableStateOf("Welcome") }
    var searchQuery by remember { mutableStateOf("") }

    val cart = remember { mutableStateListOf<CartItem>() }

    // Demo requisition data. Every newly submitted request starts as pending.
    val requisitions = remember {
        mutableStateListOf(
            RequisitionItem(
                id = "DR-0007",
                items = listOf(
                    CartItem(
                        PortalProduct(
                            "P-001",
                            "SN-ULTRA-TOP",
                            "SN-ULTRA-TOP 17 KG SET",
                            "EPOXY WALL & FLOOR",
                            "Resin Part-A (14.5 kg) + Hardener Part-B (2.5 kg)",
                            45000.0,
                            42004.20,
                            "set"
                        ),
                        6
                    )
                ),
                totalAmount = 252025.20,
                paymentStatus = "On due",
                approvalStatus = "Pending Approval"
            )
        )
    }

    val products = listOf(
        PortalProduct(
            "P-001",
            "SN-ULTRA-TOP",
            "SN-ULTRA-TOP 17 KG SET",
            "EPOXY WALL & FLOOR",
            "Resin Part-A + Hardener Part-B",
            45000.0,
            42004.20,
            "set"
        ),
        PortalProduct(
            "P-002",
            "APPLE-WHITE",
            "APPLE WHITE",
            "EPOXY COATING",
            "SN-100 SLE White Pigment Base + Hardener",
            4800.0,
            4188.88,
            "unit"
        ),
        PortalProduct(
            "P-003",
            "SN-CRETE-MF",
            "SN-CRETE MF - BLUE",
            "CEMENTITIOUS FLOORING",
            "Heavy Duty Aggregate Blend + Liquid Polymer",
            2700.0,
            2380.00,
            "bag"
        )
    )

    val filteredProducts = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            products
        } else {
            products.filter {
                it.name.contains(searchQuery, ignoreCase = true) ||
                        it.code.contains(searchQuery, ignoreCase = true)
            }
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
                        Text(
                            text = "SN CHEMICAL INDUSTRIES LTD.",
                            color = Color.White,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )

                        Text(
                            text = "CONSTRUCTION CHEMICALS • SYSTEMS & SOLUTIONS",
                            color = Color(0xFFA5D6A7),
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp
                        )
                    }

                    Surface(
                        color = ForestGreenButton,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(
                            1.dp,
                            Color(0xFF2E7D32)
                        )
                    ) {
                        Text(
                            text = distributorToken ?: "DST-0001",
                            color = Color.White,
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 6.dp
                            ),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Main navigation
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(ForestGreenPrimary)
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    listOf("Welcome", "Dashboard", "Products").forEach { tab ->
                        val selected = selectedTab == tab

                        val displayTitle = when (tab) {
                            "Products" -> "Stock Catalog"
                            else -> tab
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    selectedTab = tab
                                }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = displayTitle,
                                        color = if (selected) {
                                            Color.White
                                        } else {
                                            Color(0xFFC8E6C9)
                                        },
                                        fontWeight = if (selected) {
                                            FontWeight.Bold
                                        } else {
                                            FontWeight.Normal
                                        },
                                        fontSize = 13.sp
                                    )

                                    if (tab == "Products" && cart.isNotEmpty()) {
                                        Spacer(Modifier.width(4.dp))

                                        Badge(
                                            containerColor = AlertRedBadge
                                        ) {
                                            Text(
                                                text = "${cart.sumOf { it.quantity }}",
                                                color = Color.White,
                                                fontSize = 10.sp
                                            )
                                        }
                                    }
                                }

                                Spacer(Modifier.height(6.dp))

                                Box(
                                    modifier = Modifier
                                        .width(36.dp)
                                        .height(3.dp)
                                        .background(
                                            if (selected) {
                                                Color.White
                                            } else {
                                                Color.Transparent
                                            },
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

                // WELCOME SCREEN
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
                                Box(
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Business,
                                        contentDescription = null,
                                        tint = ForestGreenPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            }

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Partners & Stock Portal",
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )

                                Spacer(Modifier.height(4.dp))

                                Text(
                                    text = "ISO-Ready Systems & Industrial Solutions",
                                    fontSize = 13.sp,
                                    color = TextMutedSecondary
                                )
                            }

                            Spacer(Modifier.height(8.dp))

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White
                                ),
                                shape = RoundedCornerShape(16.dp),
                                elevation = CardDefaults.cardElevation(
                                    defaultElevation = 2.dp
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Text(
                                        text = "AUTHORIZED DISTRIBUTION",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreenPrimary,
                                        letterSpacing = 1.2.sp
                                    )

                                    Text(
                                        text = "Naha Construction & Registered Partners",
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextDarkPrimary
                                    )

                                    Text(
                                        text = "Access inventory specifications, review your credit balance, and submit product requisitions.",
                                        fontSize = 13.sp,
                                        color = TextMutedSecondary
                                    )

                                    HorizontalDivider(
                                        color = CardBorderLight,
                                        modifier = Modifier.padding(vertical = 4.dp)
                                    )

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = {
                                                selectedTab = "Dashboard"
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ForestGreenButton
                                            ),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text("Open Dashboard")
                                        }

                                        OutlinedButton(
                                            onClick = {
                                                selectedTab = "Products"
                                            },
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = ForestGreenPrimary
                                            )
                                        ) {
                                            Text("Stock Catalog")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // DISTRIBUTOR DASHBOARD
                "Dashboard" -> {

                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = 3.dp
                            )
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    text = "DISTRIBUTOR ACCOUNT SUMMARY",
                                    color = ForestGreenPrimary,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.2.sp
                                )

                                Text(
                                    text = "NAHA CONSTRUCTION",
                                    color = TextDarkPrimary,
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold
                                )

                                Text(
                                    text = "Your account and requisition overview.",
                                    color = TextMutedSecondary,
                                    fontSize = 13.sp
                                )

                                HorizontalDivider(
                                    color = CardBorderLight,
                                    modifier = Modifier.padding(vertical = 4.dp)
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column {
                                        Text(
                                            "Credit Limit",
                                            color = TextMutedSecondary,
                                            fontSize = 11.sp
                                        )

                                        Text(
                                            "৳ 500,000.00",
                                            color = ForestGreenPrimary,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }

                                    Column {
                                        Text(
                                            "Current Balance",
                                            color = TextMutedSecondary,
                                            fontSize = 11.sp
                                        )

                                        Text(
                                            "৳ 142,500.00",
                                            color = IndustrialGoldAccent,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Distributor's shopping cart is for submitting requests only.
                    // It does not approve any requisition.
                    if (cart.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = ForestGreenContainer
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.ShoppingCart,
                                            contentDescription = null,
                                            tint = ForestGreenPrimary
                                        )

                                        Spacer(Modifier.width(10.dp))

                                        Column {
                                            Text(
                                                text = "Cart (${cart.sumOf { it.quantity }} items)",
                                                fontWeight = FontWeight.Bold,
                                                color = ForestGreenPrimary
                                            )

                                            val total = cart.sumOf {
                                                it.product.discountedPrice * it.quantity
                                            }

                                            Text(
                                                text = "Total: ৳ ${String.format(Locale.US, "%,.2f", total)}",
                                                fontSize = 12.sp,
                                                color = TextMutedSecondary
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            if (cart.isNotEmpty()) {
                                                val newReq = RequisitionItem(
                                                    id = "DR-000${requisitions.size + 8}",
                                                    items = cart.map {
                                                        it.copy()
                                                    },
                                                    totalAmount = cart.sumOf {
                                                        it.product.discountedPrice * it.quantity
                                                    },
                                                    paymentStatus = "On due",
                                                    approvalStatus = "Pending Approval"
                                                )

                                                requisitions.add(0, newReq)
                                                cart.clear()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = ForestGreenButton
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text("Submit Request")
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Text(
                            text = "My Requisitions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )
                    }

                    // Status-only requisition cards. No approval controls here.
                    items(
                        items = requisitions,
                        key = { requisition -> requisition.id }
                    ) { req ->

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = req.id,
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreenPrimary,
                                        fontSize = 16.sp
                                    )

                                    Surface(
                                        color = Color(0xFFFFF4D6),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = req.approvalStatus,
                                            modifier = Modifier.padding(
                                                horizontal = 10.dp,
                                                vertical = 6.dp
                                            ),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF95620B)
                                        )
                                    }
                                }

                                req.items.forEach { cartItem ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "${cartItem.product.name} x ${cartItem.quantity}",
                                            fontSize = 13.sp,
                                            color = TextDarkPrimary,
                                            modifier = Modifier.weight(1f)
                                        )

                                        Text(
                                            text = "৳ ${
                                                String.format(
                                                    Locale.US,
                                                    "%,.2f",
                                                    cartItem.product.discountedPrice * cartItem.quantity
                                                )
                                            }",
                                            fontSize = 13.sp,
                                            color = TextMutedSecondary
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    color = CardBorderLight
                                )

                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    color = Color(0xFFFFF8E7),
                                    shape = RoundedCornerShape(10.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = "●",
                                            color = Color(0xFFE09B19),
                                            fontSize = 16.sp
                                        )

                                        Column {
                                            Text(
                                                text = "Request Pending",
                                                color = Color(0xFF95620B),
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            Text(
                                                text = "Your request has been submitted and is waiting for approval.",
                                                color = Color(0xFF95620B),
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // PRODUCT CATALOG
                "Products" -> {
                    item {
                        Text(
                            text = "Stock Catalog & Systems",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDarkPrimary
                        )

                        Text(
                            text = "Select high-performance industrial chemicals & systems",
                            fontSize = 12.sp,
                            color = TextMutedSecondary
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = {
                                searchQuery = it
                            },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = {
                                Text("Search by name or code...")
                            },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = null
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(10.dp)
                        )
                    }

                    items(
                        items = filteredProducts,
                        key = { product -> product.id }
                    ) { product ->

                        var quantity by remember {
                            mutableIntStateOf(1)
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = product.code,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = IndustrialGoldAccent
                                    )

                                    Text(
                                        text = product.unit,
                                        fontSize = 11.sp,
                                        color = TextMutedSecondary
                                    )
                                }

                                Text(
                                    text = product.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDarkPrimary
                                )

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            SurfaceOffWhite,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            1.dp,
                                            CardBorderLight,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .padding(8.dp)
                                ) {
                                    Text(
                                        text = "Specification: ${product.recipeBreakdown}",
                                        fontSize = 11.sp,
                                        color = TextMutedSecondary
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.Bottom
                                ) {
                                    Text(
                                        text = "৳ ${
                                            String.format(
                                                Locale.US,
                                                "%,.2f",
                                                product.discountedPrice
                                            )
                                        }",
                                        fontWeight = FontWeight.Bold,
                                        color = ForestGreenPrimary,
                                        fontSize = 18.sp
                                    )

                                    Spacer(Modifier.width(8.dp))

                                    Text(
                                        text = "৳ ${
                                            String.format(
                                                Locale.US,
                                                "%,.2f",
                                                product.basePrice
                                            )
                                        }",
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            textDecoration = TextDecoration.LineThrough
                                        ),
                                        color = TextMutedSecondary,
                                        fontSize = 12.sp
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                if (quantity > 1) {
                                                    quantity--
                                                }
                                            },
                                            modifier = Modifier.size(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("-")
                                        }

                                        Text(
                                            text = quantity.toString(),
                                            fontWeight = FontWeight.Bold
                                        )

                                        OutlinedButton(
                                            onClick = {
                                                quantity++
                                            },
                                            modifier = Modifier.size(32.dp),
                                            contentPadding = PaddingValues(0.dp)
                                        ) {
                                            Text("+")
                                        }
                                    }

                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = {
                                                val existing = cart.find {
                                                    it.product.id == product.id
                                                }

                                                if (existing != null) {
                                                    existing.quantity += quantity
                                                } else {
                                                    cart.add(
                                                        CartItem(
                                                            product = product,
                                                            quantity = quantity
                                                        )
                                                    )
                                                }
                                            },
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(
                                                horizontal = 10.dp,
                                                vertical = 6.dp
                                            )
                                        ) {
                                            Text(
                                                "Add to Cart",
                                                fontSize = 12.sp
                                            )
                                        }

                                        Button(
                                            onClick = {
                                                val newReq = RequisitionItem(
                                                    id = "DR-000${requisitions.size + 8}",
                                                    items = listOf(
                                                        CartItem(
                                                            product = product,
                                                            quantity = quantity
                                                        )
                                                    ),
                                                    totalAmount = product.discountedPrice * quantity,
                                                    paymentStatus = "On due",
                                                    approvalStatus = "Pending Approval"
                                                )

                                                requisitions.add(0, newReq)
                                                selectedTab = "Dashboard"
                                            },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = ForestGreenButton
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            contentPadding = PaddingValues(
                                                horizontal = 10.dp,
                                                vertical = 6.dp
                                            )
                                        ) {
                                            Text(
                                                "Requisition Only",
                                                fontSize = 12.sp
                                            )
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

