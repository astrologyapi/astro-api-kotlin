package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.models.BirthData
import com.google.gson.JsonObject

/** Western transit calculations — tropical and natal transits. */
class WesternTransitNamespace internal constructor(private val http: HttpClient) {

    private suspend fun post(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postForm(endpoint, body, language)

    /** Daily tropical transit positions. */
    suspend fun getDaily(data: BirthData, language: String? = null): JsonObject =
        post("tropical_transits/daily", data.toMap(), language)

    /** Weekly tropical transit overview. */
    suspend fun getWeekly(data: BirthData, language: String? = null): JsonObject =
        post("tropical_transits/weekly", data.toMap(), language)

    /** Monthly tropical transit positions and highlights. */
    suspend fun getMonthly(data: BirthData, language: String? = null): JsonObject =
        post("tropical_transits/monthly", data.toMap(), language)

    /** Daily natal transits — aspects current planets make to the natal chart. */
    suspend fun getNatalDaily(data: BirthData, language: String? = null): JsonObject =
        post("natal_transits/daily", data.toMap(), language)

    /** Weekly natal transits — personalised weekly transit aspects. */
    suspend fun getNatalWeekly(data: BirthData, language: String? = null): JsonObject =
        post("natal_transits/weekly", data.toMap(), language)
}
