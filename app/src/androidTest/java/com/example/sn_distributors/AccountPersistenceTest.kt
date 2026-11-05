package com.example.sn_distributors

import androidx.test.platform.app.InstrumentationRegistry
import com.example.sn_distributors.data.model.*
import com.example.sn_distributors.data.repository.AccountRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import java.util.UUID

class AccountPersistenceTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val databaseName = "test-accounts-${UUID.randomUUID()}.db"

    @After fun removeOnlyTestDatabase() { context.deleteDatabase(databaseName) }

    @Test fun staffCredentialsAndStableCrmOwnerIdSurviveReopening() = runBlocking {
        var staffId = ""
        AccountRepository(context, databaseName).use { repo ->
            repo.initialize()
            assertNotNull(repo.authenticate("admin@sn.com", "Admin@123", UserRole.ADMIN))
            assertTrue(repo.createByAdmin("ADM-0001", "Supervisor", "Supervisor@sn.com", "Supervisor@123",
                UserRole.STAFF, StaffDepartment.SUPERVISOR))
            staffId = repo.accounts.value.first { it.email == "supervisor@sn.com" }.id
        }
        AccountRepository(context, databaseName).use { reopened ->
            reopened.initialize()
            val staff = requireNotNull(reopened.authenticate(" SUPERVISOR@SN.COM ", "Supervisor@123", UserRole.STAFF))
            assertEquals(staffId, staff.id)
            assertEquals(StaffDepartment.SUPERVISOR, staff.department)
            assertNull(reopened.authenticate("supervisor@sn.com", "wrong-password", UserRole.STAFF))
            assertNull(reopened.authenticate("supervisor@sn.com", "Supervisor@123", UserRole.ADMIN))
            assertFalse(reopened.registerDistributor("Duplicate", "supervisor@sn.com", "Password@123"))
            assertEquals(1, reopened.accounts.value.count { it.role == UserRole.ADMIN })
        }
    }

    @Test fun registrationIsActiveImmediatelyAndPersists() = runBlocking {
        var distributorId = ""
        AccountRepository(context, databaseName).use { repo ->
            repo.initialize()
            assertTrue(repo.registerDistributor("Distributor", "distributor@sn.com", "Distributor@123"))
            distributorId = repo.accounts.value.first { it.email == "distributor@sn.com" }.id
            assertNotNull(repo.authenticate("distributor@sn.com", "Distributor@123", UserRole.DISTRIBUTOR))
        }
        AccountRepository(context, databaseName).use { repo ->
            repo.initialize()
            assertEquals(AccountStatus.ACTIVE, repo.accounts.value.first { it.id == distributorId }.status)
            assertNotNull(repo.authenticate("distributor@sn.com", "Distributor@123", UserRole.DISTRIBUTOR))
        }
        AccountRepository(context, databaseName).use { repo ->
            repo.initialize()
            assertEquals(distributorId, repo.authenticate("distributor@sn.com", "Distributor@123", UserRole.DISTRIBUTOR)?.id)
        }
    }

    @Test fun nonAdminCannotCreateStaff() = runBlocking {
        AccountRepository(context, databaseName).use { repo ->
            repo.initialize()
            assertTrue(repo.registerDistributor("Rejected", "rejected@sn.com", "Rejected@123"))
            val id = repo.accounts.value.first { it.email == "rejected@sn.com" }.id
            try {
                repo.createByAdmin(id, "Staff", "staff@sn.com", "Staff@123", UserRole.STAFF, StaffDepartment.FACTORY)
                fail("Non-admin must not create staff")
            } catch (_: IllegalArgumentException) { /* expected */ }
        }
        AccountRepository(context, databaseName).use { repo ->
            repo.initialize()
            assertFalse(repo.accounts.value.any { it.email == "staff@sn.com" })
        }
    }

    @Test fun allDemoCredentialsWorkAfterReopeningWithoutDuplicatingAccounts() = runBlocking {
        repeat(2) {
            AccountRepository(context, databaseName).use { repo ->
                repo.initialize()
                assertEquals(DemoAccounts.logins.size, repo.accounts.value.size)
                DemoAccounts.logins.forEach { demo ->
                    assertEquals(demo.account, repo.authenticate(demo.account.email, demo.password, demo.account.role))
                }
            }
        }
    }

    @Test fun versionOneUpgradeActivatesPendingAccountsAndPreservesTheirCredentialsAndIds() = runBlocking {
        var originalId = ""
        AccountRepository(context, databaseName).use { repo ->
            repo.initialize()
            assertTrue(repo.registerDistributor("Legacy", "legacy@sn.com", "Legacy@123"))
            originalId = repo.accounts.value.first { it.email == "legacy@sn.com" }.id
        }
        android.database.sqlite.SQLiteDatabase.openDatabase(context.getDatabasePath(databaseName).path, null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("UPDATE accounts SET status = 'PENDING' WHERE email = 'legacy@sn.com'")
            db.delete("accounts", "email LIKE ?", arrayOf("demo.%"))
            db.version = 1
        }
        AccountRepository(context, databaseName).use { repo ->
            repo.initialize()
            assertEquals(originalId, repo.authenticate("legacy@sn.com", "Legacy@123", UserRole.DISTRIBUTOR)?.id)
            DemoAccounts.logins.forEach { demo ->
                assertNotNull(repo.authenticate(demo.account.email, demo.password, demo.account.role))
            }
        }
    }
}
