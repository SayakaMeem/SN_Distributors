package com.example.sn_distributors.data.repository

import android.util.AtomicFile
import com.example.sn_distributors.data.model.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Versioned, atomic app-private storage; never silently discard unreadable customer records. */
class JsonCrmStorage(file: File) : CrmStorage {
    private val file = AtomicFile(file)

    override fun load(): List<CrmTask> {
        val bytes = try { file.openRead().use { it.readBytes() } }
        catch (e: java.io.FileNotFoundException) { return emptyList() }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        require(root.getInt("version") in 1..3) { "Unsupported CRM data version." }
        val tasks = root.getJSONArray("tasks")
        return List(tasks.length()) { index ->
            val t = tasks.getJSONObject(index)
            val c = t.getJSONObject("customer")
            val h = t.getJSONArray("history")
            CrmTask(t.getString("id"), CustomerNeed(c.getString("name"), c.getString("contact"),
                CustomerIntent.valueOf(c.getString("intent")), c.getString("productNeeded"),
                c.getString("currentProduct"), c.getString("desiredImprovement"), c.getString("budget"),
                c.getString("recommendedProduct"), c.getString("fitReason")),
                t.getString("title"), t.getString("assignedUserId"), t.getString("assignedName"),
                UserRole.valueOf(t.getString("assignedRole")),
                t.optString("assignedDepartment").takeIf { it.isNotBlank() }?.let { StaffDepartment.valueOf(it) },
                t.getLong("dueAt"), CrmTaskStatus.valueOf(t.getString("status")), t.getLong("updatedAt"),
                List(h.length()) { hi ->
                    val entry = h.getJSONObject(hi)
                    CrmHistory(entry.getLong("at"), entry.getString("actorId"), entry.getString("actorName"),
                        entry.getString("action"), entry.getString("details"))
                },
                distributorId = t.optString("distributorId").takeIf { it.isNotBlank() },
                distributorName = t.optString("distributorName"), distributorEmail = t.optString("distributorEmail"),
                items = (t.optJSONArray("items") ?: JSONArray()).let { rows -> List(rows.length()) { i ->
                    val row = rows.getJSONObject(i)
                    CartItem(ProductItem(row.getString("id"), row.getString("name"), row.getString("code"),
                        row.getDouble("basePrice"), row.getDouble("price"), row.getString("unit"), row.getString("recipe")), row.getInt("quantity"))
                } },
                stage = t.optString("stage").takeIf { it.isNotBlank() }?.let { OrderStage.valueOf(it) },
                supervisorId = t.optString("supervisorId").takeIf { it.isNotBlank() },
                accountsId = t.optString("accountsId").takeIf { it.isNotBlank() },
                factoryId = t.optString("factoryId").takeIf { it.isNotBlank() },
                expectedDeliveryAt = if (t.has("expectedDeliveryAt")) t.getLong("expectedDeliveryAt") else null,
                deliveryAddress = t.optString("deliveryAddress"), orderNote = t.optString("orderNote"),
                isPaused = t.optBoolean("isPaused", false), pauseReason = t.optString("pauseReason"),
                prints = (t.optJSONArray("prints") ?: JSONArray()).objects().map { p ->
                    OrderPrint(p.getString("id"), PrintKind.valueOf(p.getString("kind")), p.getLong("at"),
                        p.getString("actorId"), p.getString("actorName"), p.getJSONArray("materials").objects().map { m ->
                            MaterialUse(m.getString("materialId"), m.getString("name"), m.getString("unit"), m.getDouble("quantity"))
                        })
                })
        }
    }

    override fun loadState(): ManufacturingState {
        val tasks = load()
        val bytes = try { file.openRead().use { it.readBytes() } }
        catch (_: java.io.FileNotFoundException) { return ManufacturingState(tasks) }
        val root = JSONObject(bytes.toString(Charsets.UTF_8))
        return ManufacturingState(tasks,
            (root.optJSONArray("materials") ?: JSONArray()).objects().map { RawMaterial(it.getString("id"), it.getString("name"), it.getString("unit"), it.getDouble("stock")) },
            (root.optJSONArray("recipes") ?: JSONArray()).objects().map { r -> ProductRecipe(r.getString("productId"), r.getJSONArray("lines").objects().map { RecipeLine(it.getString("materialId"), it.getDouble("quantity")) }, r.optString("productUnit")) },
            (root.optJSONArray("receipts") ?: JSONArray()).objects().map { StockReceipt(it.getString("id"), it.getString("materialId"), it.getDouble("quantity"), it.getLong("at"), it.getString("actorId"), it.getString("note")) })
    }

    override fun save(tasks: List<CrmTask>) = saveState(loadState().copy(tasks = tasks))

    override fun saveState(state: ManufacturingState) {
        val tasks = state.tasks
        val array = JSONArray()
        tasks.forEach { t ->
            val c = t.customer
            val customer = JSONObject().put("name", c.name).put("contact", c.contact)
                .put("intent", c.intent.name).put("productNeeded", c.productNeeded)
                .put("currentProduct", c.currentProduct).put("desiredImprovement", c.desiredImprovement)
                .put("budget", c.budget).put("recommendedProduct", c.recommendedProduct).put("fitReason", c.fitReason)
            val history = JSONArray()
            val items = JSONArray()
            val prints = JSONArray()
            t.prints.forEach { p ->
                val materials = JSONArray()
                p.materials.forEach { m -> materials.put(JSONObject().put("materialId", m.materialId).put("name", m.name).put("unit", m.unit).put("quantity", m.quantity)) }
                prints.put(JSONObject().put("id", p.id).put("kind", p.kind.name).put("at", p.at).put("actorId", p.actorId).put("actorName", p.actorName).put("materials", materials))
            }
            t.items.forEach { item ->
                val p = item.product
                items.put(JSONObject().put("id", p.id).put("name", p.name).put("code", p.code)
                    .put("basePrice", p.basePrice).put("price", p.discountedPrice).put("unit", p.unit)
                    .put("recipe", p.recipeBreakdown).put("quantity", item.quantity))
            }
            t.history.forEach { h -> history.put(JSONObject().put("at", h.at).put("actorId", h.actorId)
                .put("actorName", h.actorName).put("action", h.action).put("details", h.details)) }
            array.put(JSONObject().put("id", t.id).put("customer", customer).put("title", t.title)
                .put("assignedUserId", t.assignedUserId).put("assignedName", t.assignedName)
                .put("assignedRole", t.assignedRole.name).put("assignedDepartment", t.assignedDepartment?.name ?: "")
                .put("dueAt", t.dueAt).put("status", t.status.name).put("updatedAt", t.updatedAt).put("history", history)
                .put("distributorId", t.distributorId).put("distributorName", t.distributorName).put("distributorEmail", t.distributorEmail)
                .put("items", items).put("stage", t.stage?.name).put("supervisorId", t.supervisorId)
                .put("accountsId", t.accountsId).put("factoryId", t.factoryId).put("expectedDeliveryAt", t.expectedDeliveryAt)
                .put("deliveryAddress", t.deliveryAddress).put("orderNote", t.orderNote).put("prints", prints)
                .put("isPaused", t.isPaused).put("pauseReason", t.pauseReason))
        }
        val materials = JSONArray()
        state.materials.forEach { materials.put(JSONObject().put("id", it.id).put("name", it.name).put("unit", it.unit).put("stock", it.stock)) }
        val recipes = JSONArray()
        state.recipes.forEach { recipe ->
            val lines = JSONArray()
            recipe.lines.forEach { lines.put(JSONObject().put("materialId", it.materialId).put("quantity", it.quantity)) }
            recipes.put(JSONObject().put("productId", recipe.productId).put("lines", lines).put("productUnit", recipe.productUnit))
        }
        val receipts = JSONArray()
        state.receipts.forEach { receipts.put(JSONObject().put("id", it.id).put("materialId", it.materialId).put("quantity", it.quantity).put("at", it.at).put("actorId", it.actorId).put("note", it.note)) }
        val stream = file.startWrite()
        try {
            stream.write(JSONObject().put("version", 3).put("tasks", array).put("materials", materials).put("recipes", recipes).put("receipts", receipts).toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(stream)
        } catch (e: Exception) {
            file.failWrite(stream)
            throw e
        }
    }
}

private fun JSONArray.objects() = List(length()) { getJSONObject(it) }
