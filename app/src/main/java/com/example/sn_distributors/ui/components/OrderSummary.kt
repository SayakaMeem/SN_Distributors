package com.example.sn_distributors.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun OrderAmountBadge(amount: Double, quantity: Int, modifier: Modifier = Modifier) {
    val formatted = String.format(Locale.US, "%,.2f", amount)
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer,
            modifier = Modifier.size(112.dp).clearAndSetSemantics { contentDescription = "Order total BDT $formatted" }) {
            Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text("BDT", style = MaterialTheme.typography.labelSmall)
                Text(formatted, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center,
                    fontSize = when { formatted.length <= 10 -> 15.sp; formatted.length <= 14 -> 11.sp; else -> 8.sp })
                Text("Order total", style = MaterialTheme.typography.labelSmall)
            }
        }
        Text("$quantity ${if (quantity == 1) "unit" else "units"}", style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
fun CopyOrderId(orderId: String, modifier: Modifier = Modifier, showId: Boolean = true) {
    val context = LocalContext.current
    var copied by remember(orderId) { mutableStateOf(false) }
    Column(modifier) {
        if (showId) SelectionContainer { Text("Order ID: $orderId", style = MaterialTheme.typography.bodySmall) }
        TextButton(onClick = {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText("Order ID", orderId))
            copied = true
        }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 4.dp)) { Text("Copy order ID") }
        if (copied) Text("Order ID copied", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}
