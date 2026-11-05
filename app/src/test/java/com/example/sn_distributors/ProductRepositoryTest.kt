package com.example.sn_distributors

import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import org.junit.Assert.*
import org.junit.Test

class ProductRepositoryTest {
    private class MemoryProducts : ProductStorage {
        var saved = ProductCatalog.products
        var fail = false
        override fun load() = saved
        override fun save(products: List<ProductItem>) { if (fail) error("Disk full"); saved = products }
    }
    private val users = DemoAccounts.logins.map { it.account }
    private val staff = users.first { it.role == UserRole.STAFF }
    private val distributor = users.first { it.role == UserRole.DISTRIBUTOR }

    @Test fun staffCanAddEditAndDeleteProductsAndChangesPersist() {
        val storage = MemoryProducts()
        val repo = ProductRepository(storage)
        repo.save(staff, ProductItem("", "New coating", "NEW-01", 600.0, 500.0, "bucket", "Durable"), isNew = true)
        val added = repo.products.value.first { it.code == "NEW-01" }
        assertTrue(added.id.isNotBlank())
        repo.save(staff, added.copy(name = "Better coating", discountedPrice = 550.0))
        assertEquals(550.0, ProductRepository(storage).products.value.first { it.id == added.id }.discountedPrice, 0.0)
        repo.delete(staff, added.id)
        assertFalse(ProductRepository(storage).products.value.any { it.id == added.id })
    }

    @Test fun distributorsAndInactiveStaffCannotMutateCatalog() {
        val repo = ProductRepository(MemoryProducts())
        val p = repo.products.value.first()
        listOf(distributor, staff.copy(status = AccountStatus.SUSPENDED)).forEach { actor ->
            assertThrows(IllegalArgumentException::class.java) { repo.save(actor, p) }
            assertThrows(IllegalArgumentException::class.java) { repo.delete(actor, p.id) }
        }
    }

    @Test fun invalidPricesDuplicateCodesAndMissingRecordsAreRejected() {
        val repo = ProductRepository(MemoryProducts())
        val p = repo.products.value.first()
        listOf(p.copy(discountedPrice = -1.0), p.copy(basePrice = Double.NaN), p.copy(discountedPrice = Double.POSITIVE_INFINITY),
            p.copy(basePrice = 1.0), p.copy(name = "")).forEach {
            assertThrows(IllegalArgumentException::class.java) { repo.save(staff, it) }
        }
        assertThrows(IllegalArgumentException::class.java) { repo.save(staff, p.copy(code = repo.products.value[1].code)) }
        assertThrows(IllegalArgumentException::class.java) { repo.save(staff, p.copy(id = "missing")) }
    }

    @Test fun failedWriteDoesNotPublishAndProductChangesDoNotRewritePurchasedOrder() {
        val storage = MemoryProducts()
        val products = ProductRepository(storage)
        val orderStorage = object : CrmStorage {
            var saved = emptyList<CrmTask>()
            override fun load() = saved
            override fun save(tasks: List<CrmTask>) { saved = tasks }
        }
        val orders = CrmRepository(orderStorage, catalog = { products.products.value }) { 1000L }
        val p = products.products.value.first()
        val order = orders.placeOrder(distributor, listOf(CartItem(p, 2)), users, "Dhaka", "01700000000")
        storage.fail = true
        assertThrows(IllegalStateException::class.java) { products.delete(staff, p.id) }
        assertTrue(products.products.value.any { it.id == p.id })
        storage.fail = false
        products.save(staff, p.copy(name = "Updated", discountedPrice = p.discountedPrice + 1))
        products.delete(staff, p.id)
        assertEquals(order, orders.tasks.value.single())
        assertEquals(p, orders.tasks.value.single().items.single().product)
    }
}
