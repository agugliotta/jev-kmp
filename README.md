# TypeSafe Jev Kotlin Multiplatform (KMP) SDK

[![Kotlin Multiplatform](https://img.shields.io/badge/Kotlin-Multiplatform-blue.svg?logo=kotlin)](https://kotlinlang.org/docs/multiplatform.html)
[![Ktor Client](https://img.shields.io/badge/Ktor-Client-orange.svg?logo=ktor)](https://ktor.io/)
[![JitPack](https://jitpack.io/v/agugliotta/jev-kmp.svg)](https://jitpack.io/#agugliotta/jev-kmp)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

> [!CAUTION]
> **UNOFFICIAL DISCLAIMER**: This repository and SDK are **unofficial** community implementations. This project is **NOT** endorsed, certified, sponsored, or affiliated with [TypeSafe](https://typesafe.ai) or **System One AI** in any way. All official trademarks, product names, and APIs belong to their respective owners.

---

## Overview

`jev-kmp` is a lightweight, high-performance, strongly-typed **Kotlin Multiplatform (KMP)** SDK wrapper for the TypeSafe Jev API. It supports **Android**, **iOS**, and **JVM** targets out-of-the-box using **Ktor Client** and **kotlinx.serialization**.

The Jev API is synchronous and non-generative (focused on probabilistic decision-making rather than text streaming), exposing 3 core primitives:
1. **`noul`**: Returns a Boolean probability (Yes/No) between `0.0` and `1.0`.
2. **`choice`**: Evaluates a state against a strict list of candidate options and returns the winner with probability distribution and confidence scores.
3. **`score`**: Evaluates a state against an ordered semantic scale and returns a floating-point score (`Double`).

---

## Features

- **Kotlin Multiplatform**: Seamlessly share business logic across Android (`androidTarget`), iOS (`iosArm64`, `iosSimulatorArm64`, `iosX64`), and JVM (`jvm`).
- **Type-Safe Value Classes**: Uses Kotlin `@JvmInline` value class `ApiKey` to eliminate primitive obsession and prevent accidental exposure of raw API keys.
- **Robust Exception Hierarchy**: Mapped HTTP status errors (`JevUnauthorizedException`, `JevRateLimitException`, `JevClientException`, `JevServerException`).
- **Explicit API Mode**: Enforces strict visibility (`public`, `internal`) across the SDK for robust ABI stability.

---

## Installation via JitPack

This library is published via [JitPack](https://jitpack.io/#agugliotta/jev-kmp).

### 1. Add JitPack repository
In your root `build.gradle.kts` (or `settings.gradle.kts` dependency resolution management):

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
}
```

### 2. Add Dependency
In your module's `build.gradle.kts` (e.g., `commonMain` dependencies):

```kotlin
kotlin {
    sourceSets {
        commonMain {
            dependencies {
                implementation("com.github.agugliotta:jev-kmp:v1.0.0")
            }
        }
    }
}
```
*(You can also use a specific commit hash like `implementation("com.github.agugliotta:jev-kmp:main-SNAPSHOT")` or a commit SHA).*

---

## Usage Examples

### 1. Initialization
Instantiate `JevClient` with your secure `ApiKey`:

```kotlin
import com.agugliotta.jev.client.JevClient
import com.agugliotta.jev.models.ApiKey

val apiKey = ApiKey("jev_live_your_api_key_here")
val client = JevClient(apiKey = apiKey)
```

### 2. Evaluating `noul` (Probability & Boolean)
```kotlin
val state = "The user is attempting to checkout with an empty cart."

// Get probability (0.0 to 1.0)
val probability = client.evaluateNoul(
    state = state,
    statement = "The user will successfully complete the purchase."
)
println("Probability: $probability")

// Evaluate directly as Boolean with a custom threshold
val canCheckout = client.evaluateNoulAsBoolean(
    state = state,
    statement = "The user has items in their cart.",
    threshold = 0.7
)
println("Can Checkout: $canCheckout")
```

### 3. Evaluating `choice` (Options Distribution)
```kotlin
val choiceResponse = client.evaluateChoice(
    state = "The user entered an invalid password three times.",
    options = listOf("Lock account", "Show password recovery prompt", "Ignore")
)

println("Winner: ${choiceResponse.chosenOption}")
println("Confidence: ${choiceResponse.confidence}")
choiceResponse.results.forEach { result ->
    println("- ${result.option}: prob=${result.probability}, conf=${result.confidence}")
}
```

### 4. Evaluating `score` (Semantic Scale)
```kotlin
val score = client.evaluateScore(
    state = "The customer service agent resolved the issue within 2 minutes.",
    criteria = "Customer satisfaction rating from 0.0 to 10.0"
)
println("Satisfaction Score: $score")
```

---

## Error Handling

All SDK errors inherit from `JevException`:

```kotlin
try {
    val score = client.evaluateScore(state, criteria)
} catch (e: JevUnauthorizedException) {
    // HTTP 401: Invalid or missing API key
} catch (e: JevRateLimitException) {
    // HTTP 429: Rate limit exceeded
} catch (e: JevClientException) {
    // HTTP 4xx: Client error (e.g. statusCode = ${e.statusCode})
} catch (e: JevServerException) {
    // HTTP 5xx: Server error (e.g. statusCode = ${e.statusCode})
} catch (e: JevException) {
    // General SDK exception
}
```

---

## Tech Stack

- **Language**: Kotlin 2.x (Kotlin Multiplatform)
- **HTTP Client**: Ktor Client 3.x (`commonMain` engine agnostic)
- **Serialization**: kotlinx.serialization (JSON)
- **Build System**: Gradle with Version Catalogs (`libs.versions.toml`)

---

## License

Distributed under the MIT License. See [LICENSE](LICENSE) for more information.

---
#Kotlin #KotlinMultiplatform #KMP #AI #TypeSafe #Ktor #SDK #SystemOneAI
