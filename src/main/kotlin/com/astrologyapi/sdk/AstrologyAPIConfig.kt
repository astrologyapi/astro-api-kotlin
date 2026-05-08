package com.astrologyapi.sdk

/**
 * Configuration for [AstrologyAPI].
 *
 * Credentials can also be supplied via environment variables:
 * - `ASTROLOGYAPI_USER_ID`
 * - `ASTROLOGYAPI_API_KEY`
 */
data class AstrologyAPIConfig(
    /**
     * Your AstrologyAPI user ID from astrologyapi.com/dashboard.
     * Falls back to the `ASTROLOGYAPI_USER_ID` environment variable.
     */
    val userId: String? = null,

    /**
     * Your AstrologyAPI key from astrologyapi.com/dashboard.
     * Falls back to the `ASTROLOGYAPI_API_KEY` environment variable.
     */
    val apiKey: String? = null,

    /** API version path component. Defaults to `"v1"`. */
    val version: String = "v1",

    /** Connection timeout in milliseconds. Defaults to 30 000. */
    val connectTimeoutMs: Long = 30_000L,

    /** Read timeout in milliseconds. Defaults to 30 000. */
    val readTimeoutMs: Long = 30_000L,

    /**
     * Override the JSON API base URL. Used in unit tests with MockWebServer.
     * Leave null to use the production URL.
     */
    val baseJsonUrl: String? = null,

    /**
     * Override the PDF API base URL. Used in unit tests with MockWebServer.
     * Leave null to use the production URL.
     */
    val basePdfUrl: String? = null,
)
