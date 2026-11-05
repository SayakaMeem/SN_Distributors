package com.example.sn_distributors.data.repository

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

data class PasswordCredential(val salt: ByteArray, val hash: ByteArray, val iterations: Int)

/** PBKDF2-HMAC-SHA1 is available on every supported Android API (minSdk 24). */
class PasswordHasher {
    fun hash(password: String): PasswordCredential {
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        val iterations = 210_000
        return PasswordCredential(salt, derive(password, salt, iterations), iterations)
    }

    fun verifies(password: String, credential: PasswordCredential): Boolean {
        if (credential.salt.size != 16 || credential.hash.size != 32 || credential.iterations !in 1..1_000_000) return false
        return MessageDigest.isEqual(credential.hash, derive(password, credential.salt, credential.iterations))
    }

    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try { SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1").generateSecret(spec).encoded }
        finally { spec.clearPassword() }
    }
}
