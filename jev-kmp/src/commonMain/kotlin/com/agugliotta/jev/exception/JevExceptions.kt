package com.agugliotta.jev.exception

/**
 * Base exception class for all TypeSafe Jev KMP SDK errors.
 * 
 * @param message The detail message explaining the error condition.
 * @param cause The underlying cause of the exception, if any.
 */
public open class JevException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)

/**
 * Thrown when the provided API key is missing, expired, or invalid (HTTP 401 Unauthorized).
 *
 * @param message Detail message describing the authentication failure.
 */
public class JevUnauthorizedException(
    message: String = "Unauthorized: Invalid or missing API key"
) : JevException(message)

/**
 * Thrown when API rate limits have been exceeded (HTTP 429 Too Many Requests).
 *
 * @param message Detail message describing the rate limit violation.
 */
public class JevRateLimitException(
    message: String = "Rate limit exceeded. Please try again later."
) : JevException(message)

/**
 * Thrown when the server returns a 4xx client-side error.
 *
 * @property statusCode The HTTP status code returned by the server (e.g., 400, 404).
 * @param message Detail message returned by the server or request pipeline.
 */
public class JevClientException(
    public val statusCode: Int,
    message: String
) : JevException("Client error ($statusCode): $message")

/**
 * Thrown when the server encounters an internal error (HTTP 5xx server-side error).
 *
 * @property statusCode The HTTP status code returned by the server (e.g., 500, 503).
 * @param message Detail message returned by the server.
 */
public class JevServerException(
    public val statusCode: Int,
    message: String
) : JevException("Server error ($statusCode): $message")
