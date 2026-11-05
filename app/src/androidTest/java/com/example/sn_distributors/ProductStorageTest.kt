package com.example.sn_distributors

import androidx.test.platform.app.InstrumentationRegistry
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class ProductStorageTest {
    @Test fun catalogChangesSurviveClosingAndReopeningDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "test-products-${UUID.randomUUID()}.db"
        val staff = DemoAccounts.logins.first { it.account.role == UserRole.STAFF }.account
        var id = ""
        try {
            SqliteProductStorage(context, name).use { storage ->
                val repo = ProductRepository(storage)
                repo.save(staff, ProductItem("", "Demo product", "DEMO-NEW", 600.0, 500.0, "bag", "Description"), isNew = true)
                id = repo.products.value.first { it.code == "DEMO-NEW" }.id
            }
            SqliteProductStorage(context, name).use { storage ->
                val repo = ProductRepository(storage)
                val product = repo.products.value.first { it.id == id }
                repo.save(staff, product.copy(discountedPrice = 550.0))
                repo.delete(staff, ProductCatalog.products.first().id)
            }
            SqliteProductStorage(context, name).use { storage ->
                assertEquals(550.0, storage.load().first { it.id == id }.discountedPrice, 0.0)
                assertFalse(storage.load().any { it.id == ProductCatalog.products.first().id })
            }
        } finally { context.deleteDatabase(name) }
    }
}
