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
import kotlinx.serialization.json.*

/**
 * Thread-safe client for interacting with the official TypeSafe System One AI API (`https://api.typesafe.ai`).
 *
 * Provides methods for probabilistic evaluation primitives: `noul` (boolean probability),
 * `choice` (option distribution), and `score` (semantic scale rating).
 *
 * @property apiKey The secure [ApiKey] for authentication.
 * @property baseUrl The base URL of the TypeSafe API (defaults to `https://api.typesafe.ai`).
 * @property httpClient The underlying Ktor [HttpClient] instance.
 */
public class JevClient(
    private val apiKey: ApiKey,
    private val baseUrl: String = "https://api.typesafe.ai",
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
                        encodeDefaults = true
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
     * Evaluates a statement against a given state and returns a probability between 0.0 and 1.0 using the `noul` primitive.
     *
     * @param state The context or current state description.
     * @param statement The statement to evaluate probabilistically.
     * @param model The model to use (defaults to `"jev-latest"`).
     * @param trueDescription Optional description of what true/yes means.
     * @param falseDescription Optional description of what false/no means.
     * @return A [Double] probability value.
     * @throws JevException on API or network errors.
     */
    public suspend fun evaluateNoul(
        state: String,
        statement: String,
        model: String = "jev-latest",
        trueDescription: String? = null,
        falseDescription: String? = null
    ): Double {
        val criteriaMap = if (trueDescription != null || falseDescription != null) {
            JsonObject(
                mapOf(
                    "true" to (trueDescription?.let { JsonPrimitive(it) } ?: JsonNull),
                    "false" to (falseDescription?.let { JsonPrimitive(it) } ?: JsonNull)
                ).filterValues { it !is JsonNull }
            )
        } else null

        val request = SystemOneRequest(
            state = state,
            model = model,
            questions = mapOf(
                "q" to QuestionPayload(
                    type = "noul",
                    instructions = statement,
                    criteria = criteriaMap
                )
            )
        )

        val response = httpClient.post("$baseUrl/v1/systemone") {
            setBody(request)
        }
        validateResponse(response)
        val apiResponse = response.body<SystemOneResponse>()
        val answer = apiResponse.answers["q"] ?: throw JevException("Missing answer for question 'q'")
        return answer.noul ?: throw JevException("Expected noul response but got null")
    }

    /**
     * Evaluates a statement against a given state and returns a [Boolean] based on the specified threshold.
     *
     * @param state The context or current state description.
     * @param statement The statement to evaluate.
     * @param threshold The probability threshold (inclusive) to consider true (defaults to 0.5).
     * @param model The model to use (defaults to `"jev-latest"`).
     * @return `true` if probability >= threshold, `false` otherwise.
     */
    public suspend fun evaluateNoulAsBoolean(
        state: String,
        statement: String,
        threshold: Double = 0.5,
        model: String = "jev-latest"
    ): Boolean {
        val probability = evaluateNoul(state, statement, model)
        return probability >= threshold
    }

    /**
     * Evaluates a state against a strict list of options and returns the winning option with distribution and confidence.
     *
     * @param state The context or current state description.
     * @param options The strict list of candidate options.
     * @param model The model to use (defaults to `"jev-latest"`).
     * @return A [JevChoiceResponse] containing the chosen option and distribution results.
     */
    public suspend fun evaluateChoice(
        state: String,
        options: List<String>,
        model: String = "jev-latest"
    ): JevChoiceResponse {
        require(options.isNotEmpty()) { "Options list cannot be empty" }
        val criteriaMap = JsonObject(
            options.associateWith { JsonNull }
        )

        val request = SystemOneRequest(
            state = state,
            model = model,
            questions = mapOf(
                "q" to QuestionPayload(
                    type = "choice",
                    instructions = "Choose the appropriate option based on the state.",
                    criteria = criteriaMap
                )
            )
        )

        val response = httpClient.post("$baseUrl/v1/systemone") {
            setBody(request)
        }
        validateResponse(response)
        val apiResponse = response.body<SystemOneResponse>()
        val answer = apiResponse.answers["q"] ?: throw JevException("Missing answer for question 'q'")
        
        val chosen = answer.choice ?: throw JevException("Expected choice response but got null")
        val probs = answer.probabilities ?: emptyMap()
        val conf = answer.confidence ?: 0.0

        val results = probs.map { (opt, prob) ->
            ChoiceOptionResult(
                option = opt,
                probability = prob,
                confidence = conf
            )
        }

        return JevChoiceResponse(
            chosenOption = chosen,
            results = results,
            confidence = conf,
            probabilities = probs
        )
    }

    /**
     * Evaluates a state against options with detailed rubric descriptions.
     *
     * @param state The context or current state description.
     * @param optionsWithDescriptions Map of option names to optional descriptions.
     * @param model The model to use (defaults to `"jev-latest"`).
     * @return A [JevChoiceResponse].
     */
    public suspend fun evaluateChoice(
        state: String,
        optionsWithDescriptions: Map<String, String?>,
        model: String = "jev-latest"
    ): JevChoiceResponse {
        require(optionsWithDescriptions.isNotEmpty()) { "Options map cannot be empty" }
        val criteriaMap = JsonObject(
            optionsWithDescriptions.entries.associate { (k, v) ->
                k to (v?.let { JsonPrimitive(it) } ?: JsonNull)
            }
        )

        val request = SystemOneRequest(
            state = state,
            model = model,
            questions = mapOf(
                "q" to QuestionPayload(
                    type = "choice",
                    instructions = "Choose the appropriate option based on the state.",
                    criteria = criteriaMap
                )
            )
        )

        val response = httpClient.post("$baseUrl/v1/systemone") {
            setBody(request)
        }
        validateResponse(response)
        val apiResponse = response.body<SystemOneResponse>()
        val answer = apiResponse.answers["q"] ?: throw JevException("Missing answer for question 'q'")
        
        val chosen = answer.choice ?: throw JevException("Expected choice response but got null")
        val probs = answer.probabilities ?: emptyMap()
        val conf = answer.confidence ?: 0.0

        val results = probs.map { (opt, prob) ->
            ChoiceOptionResult(
                option = opt,
                probability = prob,
                confidence = conf
            )
        }

        return JevChoiceResponse(
            chosenOption = chosen,
            results = results,
            confidence = conf,
            probabilities = probs
        )
    }

    /**
     * Evaluates a state against an ordered list of scale level descriptions and returns a score.
     *
     * @param state The context or current state description.
     * @param criteriaLevels Ordered list of level descriptions (at least 2 levels).
     * @param model The model to use (defaults to `"jev-latest"`).
     * @return A [Double] score evaluation.
     */
    public suspend fun evaluateScore(
        state: String,
        criteriaLevels: List<String>,
        model: String = "jev-latest"
    ): Double {
        require(criteriaLevels.size >= 2) { "Score criteria must have at least 2 levels" }
        val criteriaJson = JsonArray(
            criteriaLevels.map { JsonPrimitive(it) }
        )

        val request = SystemOneRequest(
            state = state,
            model = model,
            questions = mapOf(
                "q" to QuestionPayload(
                    type = "score",
                    instructions = "Rate the state according to the criteria levels.",
                    criteria = criteriaJson
                )
            )
        )

        val response = httpClient.post("$baseUrl/v1/systemone") {
            setBody(request)
        }
        validateResponse(response)
        val apiResponse = response.body<SystemOneResponse>()
        val answer = apiResponse.answers["q"] ?: throw JevException("Missing answer for question 'q'")
        return answer.score ?: throw JevException("Expected score response but got null")
    }

    /**
     * Evaluates a state against a criteria description string (using default rating levels for backward compatibility).
     *
     * @param state The context or current state description.
     * @param criteriaDescription Description or instructions of what to rate.
     * @param model The model to use (defaults to `"jev-latest"`).
     * @return A [Double] score evaluation.
     */
    public suspend fun evaluateScore(
        state: String,
        criteriaDescription: String,
        model: String = "jev-latest"
    ): Double {
        val request = SystemOneRequest(
            state = state,
            model = model,
            questions = mapOf(
                "q" to QuestionPayload(
                    type = "score",
                    instructions = criteriaDescription,
                    criteria = JsonArray(listOf("Low", "Medium", "High").map { JsonPrimitive(it) })
                )
            )
        )

        val response = httpClient.post("$baseUrl/v1/systemone") {
            setBody(request)
        }
        validateResponse(response)
        val apiResponse = response.body<SystemOneResponse>()
        val answer = apiResponse.answers["q"] ?: throw JevException("Missing answer for question 'q'")
        return answer.score ?: throw JevException("Expected score response but got null")
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
            401 -> throw JevUnauthorizedException("Unauthorized request (401): $errorBody")
            422 -> throw JevClientException(422, "Unprocessable Entity (422): $errorBody")
            429 -> throw JevRateLimitException("Rate limit exceeded (429): $errorBody")
            529 -> throw JevServerException(529, "Service overloaded (529): $errorBody")
            in 400..499 -> throw JevClientException(status.value, errorBody)
            in 500..599 -> throw JevServerException(status.value, errorBody)
            else -> throw JevException("Unexpected HTTP error (${status.value}): $errorBody")
        }
    }
}
