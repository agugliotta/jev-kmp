package com.agugliotta.jev.models

import kotlin.jvm.JvmInline

/**
 * Value class representing a secure API key for authenticating with the TypeSafe Jev API.
 * 
 * Using a value class eliminates primitive obsession and prevents accidentally passing
 * raw strings where an API key is expected.
 *
 * @property value The raw string representation of the API key.
 */
@JvmInline
public value class ApiKey(public val value: String) {
    init {
        require(value.isNotBlank()) { "API key cannot be blank or empty" }
    }
}
