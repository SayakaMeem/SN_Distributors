package com.example.sn_distributors.data.repository

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import android.util.Patterns
import com.example.sn_distributors.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.util.Locale
import java.util.UUID

/** Accounts and stable CRM owner IDs survive process/device restarts. */
class AccountRepository(context: Context, databaseName: String = "sn-accounts.db") : Closeable {
    private val hasher = PasswordHasher()
    private val database = AccountDatabase(context.applicationContext, databaseName, hasher)
    private val mutex = Mutex()
    private val state = MutableStateFlow<List<UserAccount>>(emptyList())
    val accounts = state.asStateFlow()

    suspend fun initialize() = withContext(Dispatchers.IO) {
        mutex.withLock { refresh() }
    }

    suspend fun authenticate(email: String, password: String, role: UserRole): UserAccount? = withContext(Dispatchers.IO) {
        database.readableDatabase.query("accounts", null, "email = ?", arrayOf(normalize(email)), null, null, null).use { cursor ->
            if (!cursor.moveToFirst()) return@withContext null
            val user = cursor.account()
            if (!user.isActive || user.role != role) return@withContext null
            val credential = PasswordCredential(cursor.getBlob(cursor.getColumnIndexOrThrow("password_salt")),
                cursor.getBlob(cursor.getColumnIndexOrThrow("password_hash")), cursor.getInt(cursor.getColumnIndexOrThrow("password_iterations")))
            user.takeIf { hasher.verifies(password, credential) }
        }
    }

    suspend fun registerDistributor(name: String, email: String, password: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock { insert(name, email, password, UserRole.DISTRIBUTOR, AccountStatus.ACTIVE, null) }
    }

    suspend fun createByAdmin(adminId: String, name: String, email: String, password: String,
                              role: UserRole, department: StaffDepartment? = null): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            requireAdmin(adminId)
            require(role != UserRole.ADMIN) { "Use an existing administrator account." }
            insert(name, email, password, role, AccountStatus.ACTIVE, department)
        }
    }

    private fun insert(name: String, email: String, password: String, role: UserRole,
                       status: AccountStatus, department: StaffDepartment?): Boolean {
        val cleanEmail = normalize(email)
        if (name.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(cleanEmail).matches() || password.length < 6 ||
            (role == UserRole.STAFF && department == null) || (role != UserRole.STAFF && department != null)) return false
        // Avoid the expensive password derivation for duplicate emails.
        database.readableDatabase.query("accounts", arrayOf("id"), "email = ?", arrayOf(cleanEmail), null, null, null).use {
            if (it.moveToFirst()) return false
        }
        val prefix = if (role == UserRole.STAFF) "STF" else "DST"
        val account = UserAccount("$prefix-${UUID.randomUUID()}", name.trim(), cleanEmail, role, department, status)
        try {
            database.writableDatabase.insertOrThrow("accounts", null, account.values(hasher.hash(password)))
        } catch (_: SQLiteConstraintException) { return false }
        refresh()
        return true
    }

    private fun requireAdmin(id: String) {
        database.readableDatabase.query("accounts", null, "id = ?", arrayOf(id), null, null, null).use {
            require(it.moveToFirst() && it.account().let { user -> user.isActive && user.role == UserRole.ADMIN }) {
                "An active administrator account is required."
            }
        }
    }

    private fun refresh() {
        state.value = database.readableDatabase.query("accounts", null, null, null, null, null, "name COLLATE NOCASE").use {
            buildList { while (it.moveToNext()) add(it.account()) }
        }
    }

    override fun close() = database.close()

    private fun normalize(email: String) = email.trim().lowercase(Locale.ROOT)
}

private class AccountDatabase(context: Context, name: String, private val hasher: PasswordHasher) : SQLiteOpenHelper(context, name, null, 2) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("""
            CREATE TABLE accounts (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                email TEXT NOT NULL COLLATE NOCASE UNIQUE,
                role TEXT NOT NULL CHECK(role IN ('ADMIN', 'STAFF', 'DISTRIBUTOR')),
                department TEXT CHECK(department IN ('SUPERVISOR', 'ACCOUNTS', 'FACTORY')),
                status TEXT NOT NULL CHECK(status IN ('PENDING', 'ACTIVE', 'REJECTED', 'SUSPENDED')),
                password_salt BLOB NOT NULL,
                password_hash BLOB NOT NULL,
                password_iterations INTEGER NOT NULL
            )
        """.trimIndent())
        seedDemoAccounts(db)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        check(oldVersion == 1 && newVersion == 2) { "Account database migration from $oldVersion to $newVersion is not available." }
        // Keep existing IDs and credentials so CRM ownership is preserved.
        db.execSQL("UPDATE accounts SET status = 'ACTIVE' WHERE role = 'DISTRIBUTOR' AND status = 'PENDING'")
        seedDemoAccounts(db)
    }

    private fun seedDemoAccounts(db: SQLiteDatabase) {
        DemoAccounts.logins.forEach { demo ->
            val exists = db.query("accounts", arrayOf("id"), "email = ?", arrayOf(demo.account.email), null, null, null).use { it.moveToFirst() }
            if (!exists) db.insertOrThrow("accounts", null, demo.account.values(hasher.hash(demo.password)))
        }
    }
}

private fun UserAccount.values(credential: PasswordCredential) = ContentValues().apply {
    put("id", id); put("name", name); put("email", email); put("role", role.name)
    put("department", department?.name); put("status", status.name)
    put("password_salt", credential.salt); put("password_hash", credential.hash)
    put("password_iterations", credential.iterations)
}

private fun Cursor.account() = UserAccount(
    id = getString(getColumnIndexOrThrow("id")), name = getString(getColumnIndexOrThrow("name")),
    email = getString(getColumnIndexOrThrow("email")), role = UserRole.valueOf(getString(getColumnIndexOrThrow("role"))),
    department = getString(getColumnIndexOrThrow("department"))?.let { StaffDepartment.valueOf(it) },
    status = AccountStatus.valueOf(getString(getColumnIndexOrThrow("status")))
)
