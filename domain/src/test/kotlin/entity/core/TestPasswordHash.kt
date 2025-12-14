package entity.core

import org.example.entity.core.PasswordHash
import org.example.entity.core.isPasswordValid
import org.example.entity.core.toPasswordFromHash
import org.example.entity.core.toPasswordFromRaw
import org.example.entity.core.toPasswordOrNullFromRaw
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TestPasswordHash {
    private fun calculateExpectedHash(password: String): String =
        PasswordHash.sha256.digest(password.toByteArray()).joinToString("") { "%02x".format(it) }

    @Test
    fun `fromRaw should generate correct SHA-256 hash for TesteBanana1`() {
        val password = "TesteBanana1"
        val passwordHash = PasswordHash.fromRaw(password)

        // This is the expected hash that matches the SQL test data
        val expectedHash = calculateExpectedHash(password)

        assertEquals(expectedHash, passwordHash.value)
    }

    @Test
    fun `fromRaw should generate correct SHA-256 hash for Password123`() {
        val password = "Password123"
        val passwordHash = PasswordHash.fromRaw(password)

        // Expected SHA-256 hash for "Password123"
        val expectedHash = calculateExpectedHash(password)

        // Verify it's a 64-character hex string
        assertEquals(64, passwordHash.value.length)
        assertTrue(passwordHash.value.all { it.isDigit() || it in 'a'..'f' })
        assertEquals(expectedHash, passwordHash.value)
    }

    @Test
    fun `fromRaw should generate correct SHA-256 hash for MySecure1Pass`() {
        val password = "MySecure1Pass"
        val passwordHash = PasswordHash.fromRaw(password)

        val expectedHash = calculateExpectedHash(password)
        assertEquals(expectedHash, passwordHash.value)
        assertEquals(64, passwordHash.value.length)
        assertTrue(passwordHash.value.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `fromRaw should generate correct SHA-256 hash for Admin2024`() {
        val password = "Admin2024"
        val passwordHash = PasswordHash.fromRaw(password)

        val expectedHash = calculateExpectedHash(password)
        assertEquals(expectedHash, passwordHash.value)
        assertEquals(64, passwordHash.value.length)
        assertTrue(passwordHash.value.all { it.isDigit() || it in 'a'..'f' })
    }

    @Test
    fun `fromRaw should throw exception for invalid password without uppercase`() {
        val password = "testbanana1"

        val exception =
            assertThrows<IllegalArgumentException> {
                PasswordHash.fromRaw(password)
            }

        assertEquals("Invalid password format", exception.message)
    }

    @Test
    fun `fromRaw should throw exception for invalid password without digit`() {
        val password = "TesteBanana"

        val exception =
            assertThrows<IllegalArgumentException> {
                PasswordHash.fromRaw(password)
            }

        assertEquals("Invalid password format", exception.message)
    }

    @Test
    fun `fromRaw should throw exception for blank password`() {
        val password = "   "

        val exception =
            assertThrows<IllegalArgumentException> {
                PasswordHash.fromRaw(password)
            }

        assertEquals("Invalid password format", exception.message)
    }

    @Test
    fun `fromRawOrNull should return PasswordHash for valid password`() {
        val password = "ValidPass123"
        val passwordHash = PasswordHash.fromRawOrNull(password)

        val expectedHash = calculateExpectedHash(password)
        assertEquals(expectedHash, passwordHash?.value)
        assertNotNull(passwordHash)
        assertEquals(64, passwordHash.value.length)
    }

    @Test
    fun `fromRawOrNull should return null for invalid password`() {
        val password = "invalidpass"
        val passwordHash = PasswordHash.fromRawOrNull(password)

        assertNull(passwordHash)
    }

    @Test
    fun `fromRawOrNull should return null for null password`() {
        val password: String? = null
        val passwordHash = PasswordHash.fromRawOrNull(password)

        assertNull(passwordHash)
    }

    @Test
    fun `fromHash should accept valid 64-character hex string`() {
        val hash = "183783f2f9a3306158aa309951440e947cfa8b6720c88fd07c6f52c4436a1901"
        val passwordHash = PasswordHash.fromHash(hash)

        assertEquals(hash, passwordHash.value)
    }

    @Test
    fun `fromHash should throw exception for invalid hash length`() {
        val hash = "183783f2f9a3306158aa309951440e947cfa8b67"

        val exception =
            assertThrows<IllegalArgumentException> {
                PasswordHash.fromHash(hash)
            }

        assertEquals("Invalid hash format", exception.message)
    }

    @Test
    fun `fromHash should throw exception for invalid hex characters`() {
        val hash = "183783f2f9a3306158aa309951440e947cfa8b6720c88fd07c6f52c4436a1g01"

        val exception =
            assertThrows<IllegalArgumentException> {
                PasswordHash.fromHash(hash)
            }

        assertEquals("Invalid hash format", exception.message)
    }

    @Test
    fun `isPasswordValid should return true for valid password`() {
        assertTrue("TesteBanana1".isPasswordValid())
        assertTrue("Password123".isPasswordValid())
        assertTrue("Admin2024".isPasswordValid())
        assertTrue("MySecure1Pass".isPasswordValid())
    }

    @Test
    fun `isPasswordValid should return false for password without uppercase`() {
        assertFalse("testbanana1".isPasswordValid())
        assertFalse("password123".isPasswordValid())
    }

    @Test
    fun `isPasswordValid should return false for password without digit`() {
        assertFalse("TesteBanana".isPasswordValid())
        assertFalse("Password".isPasswordValid())
    }

    @Test
    fun `isPasswordValid should return false for blank password`() {
        assertFalse("   ".isPasswordValid())
        assertFalse("".isPasswordValid())
    }

    @Test
    fun `toPassword extension should create PasswordHash from valid string`() {
        val password = "TesteBanana1"
        val passwordHash = password.toPasswordFromRaw()

        assertEquals("183783f2f9a3306158aa309951440e947cfa8b6720c88fd07c6f52c4436a1901", passwordHash.value)
    }

    @Test
    fun `toPasswordOrNull extension should return PasswordHash for valid string`() {
        val password = "TesteBanana1"
        val passwordHash = password.toPasswordOrNullFromRaw()

        assertNotNull(passwordHash)
        assertEquals("183783f2f9a3306158aa309951440e947cfa8b6720c88fd07c6f52c4436a1901", passwordHash.value)
    }

    @Test
    fun `toPasswordOrNull extension should return null for invalid string`() {
        val password = "invalid"
        val passwordHash = password.toPasswordOrNullFromRaw()

        assertNull(passwordHash)
    }

    @Test
    fun `toPasswordHash extension should create PasswordHash from hash string`() {
        val hash = "183783f2f9a3306158aa309951440e947cfa8b6720c88fd07c6f52c4436a1901"
        val passwordHash = hash.toPasswordFromHash()

        assertEquals(hash, passwordHash.value)
    }

    @Test
    fun `same password should produce same hash`() {
        val password = "TesteBanana1"
        val hash1 = PasswordHash.fromRaw(password)
        val hash2 = PasswordHash.fromRaw(password)

        assertEquals(hash1.value, hash2.value)
    }

    @Test
    fun `different passwords should produce different hashes`() {
        val password1 = "TesteBanana1"
        val password2 = "TesteBanana2"
        val hash1 = PasswordHash.fromRaw(password1)
        val hash2 = PasswordHash.fromRaw(password2)

        assertTrue(hash1.value != hash2.value)
    }

    @Test
    fun `verify SQL test data password hash`() {
        // All users in insert-test-data.sql use this password
        val password = "TesteBanana1"
        val passwordHash = PasswordHash.fromRaw(password)

        // This is the hash stored in the SQL for all test users
        val sqlHash = "183783f2f9a3306158aa309951440e947cfa8b6720c88fd07c6f52c4436a1901"

        assertEquals(sqlHash, passwordHash.value)

        // Verify we can also recreate from hash
        val recreatedHash = PasswordHash.fromHash(sqlHash)
        assertEquals(passwordHash.value, recreatedHash.value)
    }
}
