package com.agugliotta.jev.client

import com.agugliotta.jev.exception.*
import com.agugliotta.jev.models.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.json.Json

/**
 * Thread-safe client for interacting with the TypeSafe Jev API (System One AI).
 *
 * Provides methods for probabilistic evaluation primitives: `noul` (boolean probability),
 * `choice` (option distribution), and `score` (semantic scale rating).
 *
 * @property apiKey The secure [ApiKey] for authentication.
 * @property baseUrl The base URL of the TypeSafe Jev API (defaults to `https://api.typesafejev.com`).
 * @property httpClient The underlying Ktor [HttpClient] instance.
 */
public class JevClient(
    private val apiKey: ApiKey,
    private val baseUrl: String = "https://api.typesafejev.com",
    private val httpClient: HttpClient = createDefaultHttpClient(apiKey, 30_000L)
) {

    public companion object {
        /**
         * Creates a default pre-configured Ktor [HttpClient] with JSON serialization,
         * logging, authentication header, and request timeout.
         *
         * @param apiKey The [ApiKey] to include in Authorization headers.
         * @param timeoutMillis Request timeout in milliseconds (default 30 seconds).
         */
        public fun createDefaultHttpClient(apiKey: ApiKey, timeoutMillis: Long = 30_000L): HttpClient {
            return HttpClient {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        prettyPrint = true
                    })
                }
                install(Logging) {
                    level = LogLevel.INFO
                }
                install(HttpTimeout) {
                    requestTimeoutMillis = timeoutMillis
                    connectTimeoutMillis = timeoutMillis
                    socketTimeoutMillis = timeoutMillis
                }
                defaultRequest {
                    header(HttpHeaders.Authorization, "Bearer ${apiKey.value}")
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                }
            }
        }
    }

    /**
     * Evaluates a statement against a given state and returns a probability between 0.0 and 1.0.
     *
     * @param state The context or current state description.
     * @param statement The statement to evaluate probabilistically.
     * @return A [Double] probability value.
     * @throws JevException on API or network errors.
     */
    public suspend fun evaluateNoul(state: String, statement: String): Double {
        val request = NoulRequest(state = state, statement = statement)
        val response = httpClient.post("$baseUrl/noul") {
            setBody(request)
        }
        validateResponse(response)
        return response.body<NoulResponse>().probability
    }

    /**
     * Evaluates a statement against a given state and returns a [Boolean] based on the specified threshold.
     *
     * @param state The context or current state description.
     * @param statement The statement to evaluate.
     * @param threshold The probability threshold (inclusive) to consider true (defaults to 0.5).
     * @return `true` if probability >= threshold, `false` otherwise.
     */
    public suspend fun evaluateNoulAsBoolean(
        state: String,
        statement: String,
        threshold: Double = 0.5
    ): Boolean {
        val probability = evaluateNoul(state, statement)
        return probability >= threshold
    }

    /**
     * Evaluates a state against a strict list of options and returns the winning option with distribution and confidence.
     *
     * @param state The context or current state description.
     * @param options The strict list of candidate options.
     * @return A [JevChoiceResponse] containing the chosen option and distribution results.
     */
    public suspend fun evaluateChoice(state: String, options: List<String>): JevChoiceResponse {
        require(options.isNotEmpty()) { "Options list cannot be empty" }
        val request = ChoiceRequest(state = state, options = options)
        val response = httpClient.post("$baseUrl/choice") {
            setBody(request)
        }
        validateResponse(response)
        return response.body<JevChoiceResponse>()
    }

    /**
     * Evaluates a state against an ordered semantic scale/criteria and returns a Double score.
     *
     * @param state The context or current state description.
     * @param criteria The ordered scale or criteria definition.
     * @return A [Double] score evaluation.
     */
    public suspend fun evaluateScore(state: String, criteria: String): Double {
        val request = ScoreRequest(state = state, criteria = criteria)
        val response = httpClient.post("$baseUrl/score") {
            setBody(request)
        }
        validateResponse(response)
        return response.body<ScoreResponse>().score
    }

    private suspend fun validateResponse(response: HttpResponse) {
        val status = response.status
        if (status.isSuccess()) return

        val errorBody = try {
            response.bodyAsText()
        } catch (_: Exception) {
            response.status.description
        }

        when (status.value) {
            401 -> throw JevUnauthorizedException("Unauthorized request: $errorBody")
            429 -> throw JevRateLimitException("Rate limit exceeded: $errorBody")
            in 400..499 -> throw JevClientException(status.value, errorBody)
            in 500..599 -> throw JevServerException(status.value, errorBody)
            else -> throw JevException("Unexpected HTTP error (${status.value}): $errorBody")
        }
    }
}
