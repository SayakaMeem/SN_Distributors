package com.example.sn_distributors.data.repository

import com.example.sn_distributors.data.model.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface ProductStorage {
    fun load(): List<ProductItem>
    fun save(products: List<ProductItem>)
}

class ProductRepository(private val storage: ProductStorage) {
    private val state = MutableStateFlow(storage.load())
    val products = state.asStateFlow()

    @Synchronized
    fun save(actor: UserAccount, product: ProductItem, isNew: Boolean = false) {
        requireManager(actor)
        require(product.name.isNotBlank() && product.code.isNotBlank() && product.unit.isNotBlank()) { "Enter product name, code and unit." }
        require(product.basePrice.isFinite() && product.discountedPrice.isFinite() && product.discountedPrice >= 0 && product.basePrice >= product.discountedPrice) {
            "Enter valid prices. List price must be at least the selling price."
        }
        val id = if (isNew) "P-${UUID.randomUUID()}" else product.id
        require(isNew || state.value.any { it.id == id }) { "This product no longer exists." }
        require(state.value.none { it.id != id && it.code.equals(product.code.trim(), ignoreCase = true) }) { "Product code already exists." }
        val clean = product.copy(id = id, name = product.name.trim(), code = product.code.trim(), unit = product.unit.trim(), recipeBreakdown = product.recipeBreakdown.trim())
        val next = if (isNew) state.value + clean else state.value.map { if (it.id == id) clean else it }
        storage.save(next)
        state.value = next
    }

    @Synchronized
    fun delete(actor: UserAccount, productId: String) {
        requireManager(actor)
        require(state.value.any { it.id == productId }) { "This product no longer exists." }
        val next = state.value.filterNot { it.id == productId }
        storage.save(next)
        state.value = next
    }

    private fun requireManager(actor: UserAccount) {
        require(actor.isActive && actor.role in listOf(UserRole.ADMIN, UserRole.STAFF)) { "Only active staff or Admin can manage products." }
    }
}
