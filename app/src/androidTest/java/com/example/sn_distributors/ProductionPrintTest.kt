package com.example.sn_distributors

import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.graphics.pdf.PdfRenderer
import androidx.test.platform.app.InstrumentationRegistry
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.*
import com.example.sn_distributors.ui.components.OrderPdf
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.UUID

class ProductionPrintTest {
    @Test fun savedProductionPrintProducesReprintablePdfWithoutFurtherStockDeduction() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.cacheDir, "production-${UUID.randomUUID()}.json")
        val pdf = File(context.cacheDir, "production-${UUID.randomUUID()}.pdf")
        try {
            val users = DemoAccounts.logins.map { it.account }
            val factory = users.first { it.department == StaffDepartment.FACTORY }
            val repo = CrmRepository(JsonCrmStorage(file)) { 1000L }
            val product = ProductCatalog.products.first()
            val task = repo.placeOrder(users.first { it.role == UserRole.DISTRIBUTOR }, listOf(CartItem(product, 2)), users, "Dhaka", "01700000000")
            users.filter { it.department in listOf(StaffDepartment.SUPERVISOR, StaffDepartment.ACCOUNTS) }
                .sortedBy { if (it.department == StaffDepartment.SUPERVISOR) 0 else 1 }
                .forEach { repo.act(it, task.id, OrderAction.APPROVE, "Approved") }
            repo.act(factory, task.id, OrderAction.APPROVE, "Accepted", 50_000L)
            repo.receiveMaterial(factory, "Resin", "kg", 100.0, "Opening stock")
            repo.saveRecipe(factory, product.id, listOf(RecipeLine(repo.manufacturing.value.materials.single().id, 2.0)))
            val record = repo.issuePrint(factory, task.id, PrintKind.PRODUCTION)
            val snapshot = repo.manufacturing.value
            repeat(2) {
                val document = OrderPdf(context, repo.tasks.value.single(), record)
                val attrs = PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4)
                    .setResolution(PrintAttributes.Resolution("test", "Test", 300, 300))
                    .setMinMargins(PrintAttributes.Margins.NO_MARGINS).build()
                assertTrue(document.layout(attrs).pageCount > 0)
                ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_CREATE or ParcelFileDescriptor.MODE_TRUNCATE or ParcelFileDescriptor.MODE_READ_WRITE).use { destination ->
                    assertTrue(document.write(arrayOf(PageRange.ALL_PAGES), destination, CancellationSignal()).isNotEmpty())
                }
                PdfRenderer(ParcelFileDescriptor.open(pdf, ParcelFileDescriptor.MODE_READ_ONLY)).use { assertTrue(it.pageCount > 0) }
                assertEquals(snapshot, repo.manufacturing.value)
            }
        } finally { file.delete(); pdf.delete() }
    }
}
