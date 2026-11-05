package com.example.sn_distributors

import com.example.sn_distributors.data.repository.PasswordCredential
import com.example.sn_distributors.data.repository.PasswordHasher
import org.junit.Assert.*
import org.junit.Test

class PasswordHasherTest {
    private val hasher = PasswordHasher()

    @Test fun correctPasswordVerifiesAndWrongPasswordDoesNot() {
        val credential = hasher.hash("Admin@123")
        assertTrue(hasher.verifies("Admin@123", credential))
        assertFalse(hasher.verifies("admin@123", credential))
        assertFalse(hasher.verifies("Admin@123 ", credential))
        assertFalse(hasher.verifies("", credential))
    }

    @Test fun identicalPasswordsHaveIndependentSaltsAndHashes() {
        val first = hasher.hash("Supervisor@123")
        val second = hasher.hash("Supervisor@123")
        assertFalse(first.salt.contentEquals(second.salt))
        assertFalse(first.hash.contentEquals(second.hash))
        assertEquals(16, first.salt.size)
        assertEquals(32, first.hash.size)
        assertTrue(hasher.verifies("Supervisor@123", second))
    }

    @Test fun storedCredentialCanBeReconstructedAndTamperingIsRejected() {
        val original = hasher.hash("পাসওয়ার্ড@123")
        val restored = PasswordCredential(original.salt.copyOf(), original.hash.copyOf(), original.iterations)
        assertTrue(hasher.verifies("পাসওয়ার্ড@123", restored))
        restored.hash[0] = (restored.hash[0].toInt() xor 1).toByte()
        assertFalse(hasher.verifies("পাসওয়ার্ড@123", restored))
    }

    @Test fun malformedCredentialsAreRejected() {
        assertFalse(hasher.verifies("password", PasswordCredential(ByteArray(0), ByteArray(0), 0)))
        assertFalse(hasher.verifies("password", PasswordCredential(ByteArray(16), ByteArray(32), Int.MAX_VALUE)))
    }
}
