package com.agugliotta.jev

import com.agugliotta.jev.exception.*
import com.agugliotta.jev.models.*
import kotlinx.serialization.json.Json
import kotlin.test.*

class JevClientTest {

    @Test
    fun testApiKeyValidation() {
        val validKey = ApiKey("valid_key_123")
        assertEquals("valid_key_123", validKey.value)

        assertFailsWith<IllegalArgumentException> {
            ApiKey("   ")
        }
    }

    @Test
    fun testNoulRequestSerialization() {
        val request = NoulRequest(state = "User logged in", statement = "User will purchase item")
        val jsonString = Json.encodeToString(NoulRequest.serializer(), request)
        assertTrue(jsonString.contains("User logged in"))
        assertTrue(jsonString.contains("User will purchase item"))
    }

    @Test
    fun testExceptionHierarchy() {
        val unauthorized = JevUnauthorizedException("Unauthorized")
        assertTrue(unauthorized is JevException)

        val rateLimit = JevRateLimitException("Rate limit")
        assertTrue(rateLimit is JevException)

        val clientEx = JevClientException(400, "Bad Request")
        assertEquals(400, clientEx.statusCode)
        assertTrue(clientEx is JevException)

        val serverEx = JevServerException(500, "Internal Server Error")
        assertEquals(500, serverEx.statusCode)
        assertTrue(serverEx is JevException)
    }
}
