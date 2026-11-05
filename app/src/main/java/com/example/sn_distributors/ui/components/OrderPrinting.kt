package com.example.sn_distributors.ui.components

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.print.pdf.PrintedPdfDocument
import com.example.sn_distributors.data.model.*
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Reprinting uses the saved issue record and never changes inventory. */
fun printOrder(context: Context, task: CrmTask, record: OrderPrint) {
    (context.getSystemService(Context.PRINT_SERVICE) as PrintManager).print("${task.title} ${record.kind}", createOrderPrintAdapter(context, task, record),
        PrintAttributes.Builder().setMediaSize(PrintAttributes.MediaSize.ISO_A4).build())
}

internal fun createOrderPrintAdapter(context: Context, task: CrmTask, record: OrderPrint): PrintDocumentAdapter {
    val document = OrderPdf(context, task, record)
    return object : PrintDocumentAdapter() {
        override fun onLayout(oldAttributes: PrintAttributes?, newAttributes: PrintAttributes, cancellationSignal: CancellationSignal, callback: LayoutResultCallback, extras: Bundle?) {
            if (cancellationSignal.isCanceled) { callback.onLayoutCancelled(); return }
            try { callback.onLayoutFinished(document.layout(newAttributes), true) }
            catch (e: Exception) { callback.onLayoutFailed(e.message ?: "Could not prepare document.") }
        }
        override fun onWrite(ranges: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal, callback: WriteResultCallback) {
            try { callback.onWriteFinished(document.write(ranges, destination, cancellationSignal)) }
            catch (_: android.os.OperationCanceledException) { callback.onWriteCancelled() }
            catch (e: Exception) { callback.onWriteFailed(e.message ?: "Could not print document.") }
        }
    }
}

internal class OrderPdf(private val context: Context, private val task: CrmTask, private val record: OrderPrint) {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
    val text = buildList {
        add("SN Chemical Industries Ltd.")
        add(if (record.kind == PrintKind.TEST) "TEST PRINT - REFERENCE ONLY - NO STOCK DEDUCTION" else "PRODUCTION PRINT - MATERIALS ISSUED")
        add(task.title)
        add("Order ID: ${task.id}")
        add("Document ID: ${record.id}")
        add("Issued: ${format.format(Date(record.at))} by ${record.actorName}")
        add("Distributor: ${task.distributorName}")
        add("Contact: ${task.customer.contact}")
        add("Delivery address: ${task.deliveryAddress}")
        add("Delivery date: ${task.expectedDeliveryAt?.let { format.format(Date(it)) } ?: "Not set"}")
        add("")
        task.items.forEach { add("${it.product.code} | ${it.product.name} | ${it.quantity} ${it.product.unit}") }
        add("Order value: BDT ${String.format(Locale.US, "%.2f", task.totalAmount)}")
        if (record.kind == PrintKind.PRODUCTION) {
            add(""); add("Raw materials deducted for this supply:")
            record.materials.forEach { add("${it.name}: ${it.quantity} ${it.unit}") }
            add("Saved issue record. Reprinting does not deduct stock again.")
        } else add("Casual reference copy. Not a production stock issue.")
        add("Order note: ${task.orderNote.ifBlank { "None" }}")
    }
        private var attributes: PrintAttributes? = null
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { textSize = 10f; typeface = Typeface.MONOSPACE }
        private var pages = emptyList<List<String>>()
        fun layout(newAttributes: PrintAttributes): PrintDocumentInfo {
                attributes = newAttributes
                val pdf = PrintedPdfDocument(context, newAttributes)
                try {
                    val width = pdf.pageContentRect.width().toFloat()
                    val wrapped = text.flatMap { source -> source.lines().flatMap { line ->
                        buildList {
                            var rest = line
                            if (rest.isEmpty()) add("")
                            while (rest.isNotEmpty()) {
                                val count = paint.breakText(rest, true, width, null).coerceAtLeast(1)
                                add(rest.take(count)); rest = rest.drop(count)
                            }
                        }
                    } }
                    pages = wrapped.chunked((pdf.pageContentRect.height() / 15).coerceAtLeast(1))
                    return PrintDocumentInfo.Builder("${task.title}-${record.kind}-${record.id.take(8)}.pdf")
                        .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT).setPageCount(pages.size).build()
                } finally { pdf.close() }
        }
        fun write(ranges: Array<out PageRange>, destination: ParcelFileDescriptor, cancellationSignal: CancellationSignal): Array<PageRange> {
            val pdf = PrintedPdfDocument(context, requireNotNull(attributes))
            try {
                val written = mutableListOf<PageRange>()
                pages.forEachIndexed { index, lines ->
                    cancellationSignal.throwIfCanceled()
                    if (ranges.any { index in it.start..it.end }) {
                        val page = pdf.startPage(index)
                        val rect = page.info.contentRect
                        lines.forEachIndexed { row, line -> page.canvas.drawText(line, rect.left.toFloat(), rect.top + 11f + row * 15f, paint) }
                        pdf.finishPage(page)
                        written.add(PageRange(index, index))
                    }
                }
                FileOutputStream(destination.fileDescriptor).use { pdf.writeTo(it) }
                cancellationSignal.throwIfCanceled()
                return written.toTypedArray()
            }
            finally { pdf.close() }
        }
}
