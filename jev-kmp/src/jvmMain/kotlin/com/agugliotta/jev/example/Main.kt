package com.agugliotta.jev.example

import com.agugliotta.jev.client.JevClient
import com.agugliotta.jev.models.ApiKey

/**
 * Example usage of TypeSafe Jev KMP SDK (JVM Target).
 * 
 * Securely loads the API key programmatically from environment variables (`JEV_API_KEY`)
 * or system properties (`jev.api.key`), preventing hardcoding secrets in version control.
 */
public suspend fun main() {
    // 1. Load API key programmatically (never hardcoded)
    val apiKeyString = System.getenv("JEV_API_KEY")
        ?: System.getProperty("jev.api.key")
        ?: throw IllegalStateException(
            "API key not found! Please set the 'JEV_API_KEY' environment variable " +
                    "or pass -Djev.api.key=\"your_api_key\"."
        )

    val apiKey = ApiKey(apiKeyString)
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
            criteriaDescription = "Urgency level of user frustration from 0.0 to 10.0"
        )
        println("Frustration Score: $score")

    } catch (e: Exception) {
        println("Error during Jev API evaluation: ${e.message}")
    }
}
