package com.agugliotta.jev.example

import com.agugliotta.jev.client.JevClient
import com.agugliotta.jev.models.ApiKey

/**
 * Example usage of TypeSafe Jev KMP SDK.
 */
public suspend fun main() {
    // 1. Initialize client with ApiKey value class
    val apiKey = ApiKey("jev_live_sample_key_12345")
    val client = JevClient(apiKey = apiKey)

    val currentState = "The user is attempting to checkout with an empty cart."

    try {
        // 2. Evaluate Noul (Probability)
        val noulProbability = client.evaluateNoul(
            state = currentState,
            statement = "The user will successfully complete the purchase."
        )
        println("Noul Probability: $noulProbability")

        // 3. Evaluate Noul as Boolean with custom threshold
        val canCheckout = client.evaluateNoulAsBoolean(
            state = currentState,
            statement = "The user has items in their cart.",
            threshold = 0.7
        )
        println("Can Checkout: $canCheckout")

        // 4. Evaluate Choice against strict options
        val choiceResponse = client.evaluateChoice(
            state = currentState,
            options = listOf("Show error message", "Redirect to shop", "Do nothing")
        )
        println("Chosen Option: ${choiceResponse.chosenOption} (Confidence: ${choiceResponse.confidence})")

        // 5. Evaluate Score against semantic criteria
        val score = client.evaluateScore(
            state = currentState,
            criteria = "Urgency level of user frustration from 0.0 to 10.0"
        )
        println("Frustration Score: $score")

    } catch (e: Exception) {
        println("Error during Jev API evaluation: ${e.message}")
    }
}
