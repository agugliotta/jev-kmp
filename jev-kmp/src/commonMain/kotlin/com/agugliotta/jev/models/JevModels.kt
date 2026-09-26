package com.agugliotta.jev.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName
import kotlinx.serialization.json.JsonElement

/**
 * Request payload for evaluating state against typed questions using the TypeSafe System One API (`/v1/systemone`).
 *
 * @property state The content to evaluate (string, object, or array).
 * @property model The model handling the request (defaults to `"jev-latest"`).
 * @property questions Map of typed question payloads keyed by custom IDs.
 */
@Serializable
public data class SystemOneRequest(
    val state: String,
    val model: String = "jev-latest",
    val questions: Map<String, QuestionPayload>
)

/**
 * Represents a typed question (`noul`, `choice`, or `score`) sent to the TypeSafe API.
 */
@Serializable
public data class QuestionPayload(
    val type: String,
    val instructions: String,
    val criteria: JsonElement? = null
)

/**
 * Response payload from the TypeSafe System One API.
 *
 * @property model The model that performed the evaluation.
 * @property answers Map of answers corresponding to the request question IDs.
 * @property usage Token usage statistics (`input_tokens`, `output_tokens`).
 */
@Serializable
public data class SystemOneResponse(
    val model: String,
    val answers: Map<String, AnswerPayload>,
    val usage: Usage? = null
)

/**
 * Token usage statistics for an API request.
 */
@Serializable
public data class Usage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

/**
 * Raw answer payload returned by the API for a question.
 */
@Serializable
public data class AnswerPayload(
    val type: String,
    val noul: Double? = null,
    val choice: String? = null,
    val probabilities: Map<String, Double>? = null,
    val confidence: Double? = null,
    val score: Double? = null,
    val legend: Map<String, String>? = null
)

/**
 * Distribution result for an individual option in a choice evaluation.
 */
@Serializable
public data class ChoiceOptionResult(
    val option: String,
    val probability: Double,
    val confidence: Double
)

/**
 * Structured response for a choice evaluation (`choice`).
 */
@Serializable
public data class JevChoiceResponse(
    @SerialName("chosen_option") val chosenOption: String,
    val results: List<ChoiceOptionResult>,
    val confidence: Double,
    val probabilities: Map<String, Double>
)
