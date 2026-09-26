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
    fun testSystemOneRequestSerialization() {
        val request = SystemOneRequest(
            state = "User logged in",
            questions = mapOf(
                "q1" to QuestionPayload(type = "noul", instructions = "User will purchase item")
            )
        )
        val jsonString = Json.encodeToString(SystemOneRequest.serializer(), request)
        assertTrue(jsonString.contains("User logged in"))
        assertTrue(jsonString.contains("noul"))
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
