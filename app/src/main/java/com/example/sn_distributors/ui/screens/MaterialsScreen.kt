package com.example.sn_distributors.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.CrmRepository
import com.example.sn_distributors.ui.components.ScrollableDialogBody
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MaterialsScreen(user: UserAccount, repository: CrmRepository, products: List<ProductItem>) {
    if (!repository.canManageMaterials(user)) { Text("Only Admin and Factory can manage raw materials."); return }
    val state by repository.manufacturing.collectAsState()
    var receive by remember { mutableStateOf(false) }
    var target by remember { mutableStateOf<RawMaterial?>(null) }
    var recipe by remember { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item {
            Text("Raw materials & recipes", style = MaterialTheme.typography.headlineSmall)
            Text("Receive actual stock and configure material quantities for one selling unit of each product. Test prints leave stock unchanged. Production prints deduct stock once.")
            Button(onClick = { target = null; receive = true }) { Text("Add raw material") }
            OutlinedButton(onClick = { recipe = true }, enabled = products.isNotEmpty() && state.materials.isNotEmpty()) { Text("Configure product recipe") }
        }
        if (state.materials.isEmpty()) item { Text("No raw materials configured. Add your actual opening stock first.") }
        items(state.materials, key = { it.id }) { material ->
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text(material.name, fontWeight = FontWeight.Bold)
                    Text("Available: ${material.stock} ${material.unit}")
                    TextButton(onClick = { target = material; receive = true }) { Text("Receive stock") }
                }
            }
        }
        item { Text("Product recipes", style = MaterialTheme.typography.titleMedium) }
        items(products, key = { "recipe-${it.id}" }) { product ->
            val saved = state.recipes.firstOrNull { it.productId == product.id }
            OutlinedCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("${product.name} / 1 ${product.unit}", fontWeight = FontWeight.Bold)
                    if (saved == null) Text("Recipe required before production print.")
                    saved?.lines?.forEach { line ->
                        val material = state.materials.firstOrNull { it.id == line.materialId }
                        Text("${material?.name ?: "Missing material"}: ${line.quantity} ${material?.unit.orEmpty()}")
                    }
                }
            }
        }
        item { Text("Recent stock movements", style = MaterialTheme.typography.titleMedium) }
        val movements = state.receipts.map { receipt ->
            receipt.at to "Received ${receipt.quantity} ${state.materials.firstOrNull { it.id == receipt.materialId }?.unit.orEmpty()} ${state.materials.firstOrNull { it.id == receipt.materialId }?.name.orEmpty()} - ${receipt.note}"
        } + state.tasks.flatMap { task -> task.prints.filter { it.kind == PrintKind.PRODUCTION }.map { print ->
            print.at to "${task.title}: " + print.materials.joinToString { "-${it.quantity} ${it.unit} ${it.name}" }
        } }
        if (movements.isEmpty()) item { Text("No stock movements yet.") }
        items(movements.sortedByDescending { it.first }.take(30)) { (_, text) -> Text(text) }
    }
    if (receive) MaterialReceiptDialog(user, repository, target) { receive = false }
    if (recipe) RecipeDialog(user, repository, products, state) { recipe = false }
}

@Composable
private fun MaterialReceiptDialog(user: UserAccount, repository: CrmRepository, material: RawMaterial?, dismiss: () -> Unit) {
    var name by remember { mutableStateOf(material?.name.orEmpty()) }
    var unit by remember { mutableStateOf(material?.unit ?: "kg") }
    var quantity by remember { mutableStateOf("") }
    var note by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text(if (material == null) "Add raw material" else "Receive stock") }, text = {
        ScrollableDialogBody {
            OutlinedTextField(name, { name = it }, readOnly = material != null, label = { Text("Material name *") })
            OutlinedTextField(unit, { unit = it }, readOnly = material != null, label = { Text("Stock unit *") })
            OutlinedTextField(quantity, { quantity = it }, label = { Text("Quantity received *") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            OutlinedTextField(note, { note = it }, label = { Text("Receipt / opening stock reference *") })
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        Button(enabled = !busy, onClick = {
            val amount = quantity.toDoubleOrNull()
            if (amount == null) error = "Enter a valid stock quantity." else {
                busy = true
                scope.launch {
                    try { withContext(Dispatchers.IO) { repository.receiveMaterial(user, name, unit, amount, note, material?.id) }; dismiss() }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = e.message ?: "Could not receive stock." }
                    finally { busy = false }
                }
            }
        }) { Text("Save stock receipt") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Back") } })
}

@Composable
private fun RecipeDialog(user: UserAccount, repository: CrmRepository, products: List<ProductItem>, state: ManufacturingState, dismiss: () -> Unit) {
    var productId by remember { mutableStateOf(products.first().id) }
    var expanded by remember { mutableStateOf(false) }
    val product = products.first { it.id == productId }
    var quantities by remember(productId) { mutableStateOf(state.recipes.firstOrNull { it.productId == productId }?.lines?.associate { it.materialId to it.quantity.toString() }.orEmpty()) }
    var error by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(onDismissRequest = { if (!busy) dismiss() }, title = { Text("Product recipe") }, text = {
        ScrollableDialogBody {
            Box {
                OutlinedButton(enabled = !busy, onClick = { expanded = true }) { Text(product.name) }
                DropdownMenu(expanded, { expanded = false }) {
                    products.forEach { item -> DropdownMenuItem(text = { Text(item.name) }, onClick = { productId = item.id; expanded = false; error = "" }) }
                }
            }
            Text("Material quantities required for 1 ${product.unit}. Leave unused materials blank. Use the stock unit shown; no automatic unit conversion.")
            state.materials.forEach { material ->
                OutlinedTextField(quantities[material.id].orEmpty(), { quantities = quantities + (material.id to it) },
                    label = { Text("${material.name} (${material.unit})") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth())
            }
            if (error.isNotEmpty()) Text(error, color = MaterialTheme.colorScheme.error)
        }
    }, confirmButton = {
        Button(enabled = !busy, onClick = {
            try {
                val lines = quantities.filterValues { it.isNotBlank() }.map { (id, text) -> RecipeLine(id, text.toDoubleOrNull() ?: error("Enter numeric material quantities.")) }
                busy = true
                scope.launch {
                    try { withContext(Dispatchers.IO) { repository.saveRecipe(user, productId, lines) }; dismiss() }
                    catch (e: CancellationException) { throw e }
                    catch (e: Exception) { error = e.message ?: "Could not save recipe." }
                    finally { busy = false }
                }
            } catch (e: Exception) { error = e.message.orEmpty() }
        }) { Text("Save recipe") }
    }, dismissButton = { TextButton(enabled = !busy, onClick = dismiss) { Text("Back") } })
}
