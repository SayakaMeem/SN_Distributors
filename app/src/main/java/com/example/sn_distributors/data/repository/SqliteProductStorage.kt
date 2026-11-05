package com.example.sn_distributors.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.sn_distributors.data.model.ProductCatalog
import com.example.sn_distributors.data.model.ProductItem
import java.io.Closeable

class SqliteProductStorage(context: Context, name: String = "sn-products.db") : ProductStorage, Closeable {
    private val database = object : SQLiteOpenHelper(context.applicationContext, name, null, 1) {
        override fun onCreate(db: SQLiteDatabase) {
            db.execSQL("CREATE TABLE products (id TEXT PRIMARY KEY NOT NULL, name TEXT NOT NULL, code TEXT COLLATE NOCASE UNIQUE NOT NULL, base_price REAL NOT NULL, price REAL NOT NULL, unit TEXT NOT NULL, description TEXT NOT NULL)")
            ProductCatalog.products.forEach { db.insertOrThrow("products", null, values(it)) }
        }
        override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
            error("Product database migration from $oldVersion to $newVersion is not available.")
        }
    }

    override fun load(): List<ProductItem> = database.readableDatabase.query("products", null, null, null, null, null, "name COLLATE NOCASE").use { c ->
        buildList { while (c.moveToNext()) add(ProductItem(c.getString(c.getColumnIndexOrThrow("id")),
            c.getString(c.getColumnIndexOrThrow("name")), c.getString(c.getColumnIndexOrThrow("code")),
            c.getDouble(c.getColumnIndexOrThrow("base_price")), c.getDouble(c.getColumnIndexOrThrow("price")),
            c.getString(c.getColumnIndexOrThrow("unit")), c.getString(c.getColumnIndexOrThrow("description")))) }
    }

    override fun save(products: List<ProductItem>) {
        val db = database.writableDatabase
        db.beginTransaction()
        try {
            db.delete("products", null, null)
            products.forEach { db.insertOrThrow("products", null, values(it)) }
            db.setTransactionSuccessful()
        } finally { db.endTransaction() }
    }

    private fun values(p: ProductItem) = ContentValues().apply {
        put("id", p.id); put("name", p.name); put("code", p.code); put("base_price", p.basePrice)
        put("price", p.discountedPrice); put("unit", p.unit); put("description", p.recipeBreakdown)
    }
    override fun close() = database.close()
}
