package com.astrologyapi.sdk.errors

/**
 * Base exception for all AstrologyAPI errors.
 *
 * Catch [AstrologyAPIException] to handle any SDK error, or catch
 * specific subclasses for fine-grained handling.
 */
open class AstrologyAPIException(
    message: String,
    /** HTTP status code, or null for network-level errors. */
    val status: Int? = null,
    /** Raw response body from the API. */
    val body: Any? = null,
    cause: Throwable? = null,
) : Exception(message, cause)

/**
 * HTTP 401 — Invalid userId or apiKey.
 * Re-check your credentials at astrologyapi.com/dashboard.
 */
class AuthenticationException(
    message: String = "Authentication failed. Check your apiKey.",
    body: Any? = null,
) : AstrologyAPIException(message, 401, body)

/**
 * HTTP 400 / 422 — Bad request or validation failure.
 * @property field Optional field name that caused the validation error.
 */
class ValidationException(
    message: String,
    /** The field that failed validation, if provided by the API. */
    val field: String? = null,
    body: Any? = null,
) : AstrologyAPIException(message, 400, body)

/**
 * HTTP 402 — Monthly quota exceeded.
 * Upgrade your plan at astrologyapi.com/pricing.
 */
class QuotaExceededException(
    message: String = "API quota exceeded. Please upgrade your plan.",
    body: Any? = null,
) : AstrologyAPIException(message, 402, body)

/**
 * HTTP 403 — Endpoint not available on your current plan.
 */
class PlanRestrictedException(
    message: String = "This endpoint is not available on your current plan.",
    body: Any? = null,
) : AstrologyAPIException(message, 403, body)

/**
 * HTTP 429 — Rate limit exceeded.
 * @property retryAfter Seconds to wait before retrying, parsed from the Retry-After header.
 */
class RateLimitException(
    message: String = "Rate limit exceeded. Please wait before retrying.",
    /** Seconds to wait before retrying, if provided in the Retry-After header. */
    val retryAfter: Int? = null,
    body: Any? = null,
) : AstrologyAPIException(message, 429, body)

/**
 * HTTP 5xx — AstrologyAPI server error.
 */
class ServerException(
    message: String,
    status: Int,
    body: Any? = null,
) : AstrologyAPIException(message, status, body)

/**
 * Network-level error — no connectivity, DNS failure, or timeout.
 */
class NetworkException(
    message: String,
    cause: Throwable,
) : AstrologyAPIException(message, null, null, cause)
