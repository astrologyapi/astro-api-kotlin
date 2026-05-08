package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.google.gson.JsonObject

/** Geographic lookup and timezone resolution. */
class LocationNamespace internal constructor(private val http: HttpClient) {

    /**
     * Resolve geographic coordinates from a place name.
     * @param place Place name to search (e.g. "Mumbai", "New York")
     * @param maxRows Maximum number of results to return (default: 6)
     */
    suspend fun getGeoDetails(place: String, maxRows: Int = 6, language: String? = null): JsonObject =
        http.postForm("geo_details", mapOf("place" to place, "maxRows" to maxRows), language)

    /**
     * Get the UTC timezone offset accounting for DST.
     * Returns the `tzone` value (e.g. 5.5 for IST) to use in birth chart requests.
     */
    suspend fun getTimezone(
        day: Int, month: Int, year: Int,
        hour: Int, min: Int,
        lat: Double, lon: Double,
        language: String? = null,
    ): JsonObject = http.postForm(
        "timezone_with_dst",
        mapOf("day" to day, "month" to month, "year" to year,
              "hour" to hour, "min" to min, "lat" to lat, "lon" to lon),
        language,
    )
}
