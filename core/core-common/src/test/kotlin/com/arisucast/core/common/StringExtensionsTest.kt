package com.arisucast.core.common

import com.arisucast.core.common.extensions.sha256
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class StringExtensionsTest {

    @Test
    fun `sha256 - known value matches expected hash`() {
        val result = "hello".sha256()
        assertEquals("2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824", result)
    }

    @Test
    fun `sha256 - same input produces same hash`() {
        val url = "https://example.com/feed.rss"
        assertEquals(url.sha256(), url.sha256())
    }

    @Test
    fun `sha256 - different inputs produce different hashes`() {
        assertNotEquals(
            "https://example.com/feed1.rss".sha256(),
            "https://example.com/feed2.rss".sha256()
        )
    }

    @Test
    fun `sha256 - output is 64 hex characters`() {
        val hash = "any string".sha256()
        assertEquals(64, hash.length)
        assertTrue(hash.all { it in '0'..'9' || it in 'a'..'f' })
    }

    @Test
    fun `sha256 - whitespace matters`() {
        assertNotEquals("hello".sha256(), " hello ".sha256())
    }

    private fun assertTrue(value: Boolean) = org.junit.Assert.assertTrue(value)
}
