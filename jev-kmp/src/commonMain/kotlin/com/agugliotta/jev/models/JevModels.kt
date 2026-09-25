package com.agugliotta.jev.models

import kotlinx.serialization.Serializable
import kotlinx.serialization.SerialName

/**
 * Request payload for evaluating a probabilistic boolean statement (`noul`).
 *
 * @property state The context or current state description.
 * @property statement The statement to evaluate probabilistically against the state.
 */
@Serializable
public data class NoulRequest(
    val state: String,
    val statement: String
)

/**
 * Response payload for a probabilistic boolean evaluation (`noul`).
 *
 * @property probability The evaluated probability value between 0.0 and 1.0.
 */
@Serializable
public data class NoulResponse(
    val probability: Double
)

/**
 * Request payload for evaluating a choice against a strict list of options.
 *
 * @property state The context or current state description.
 * @property options The strict list of available options to choose from.
 */
@Serializable
public data class ChoiceRequest(
    val state: String,
    val options: List<String>
)

/**
 * Distribution result for an individual option in a choice evaluation.
 *
 * @property option The specific option string.
 * @property probability The calculated probability for this option.
 * @property confidence The confidence score associated with this option's evaluation.
 */
@Serializable
public data class ChoiceOptionResult(
    val option: String,
    val probability: Double,
    val confidence: Double
)

/**
 * Response payload for a choice evaluation (`choice`).
 *
 * @property chosenOption The winning option chosen by the evaluation.
 * @property results The full list of option distribution results.
 * @property confidence The overall confidence score of the evaluation.
 */
@Serializable
public data class JevChoiceResponse(
    @SerialName("chosen_option") val chosenOption: String,
    val results: List<ChoiceOptionResult>,
    val confidence: Double
)

/**
 * Request payload for evaluating a score against ordered semantic criteria (`score`).
 *
 * @property state The context or current state description.
 * @property criteria The ordered scale or criteria definition.
 */
@Serializable
public data class ScoreRequest(
    val state: String,
    val criteria: String
)

/**
 * Response payload for a score evaluation (`score`).
 *
 * @property score The evaluated numeric score as a Double.
 */
@Serializable
public data class ScoreResponse(
    val score: Double
)
