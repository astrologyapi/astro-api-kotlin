package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.PlanetName
import com.google.gson.JsonElement
import com.google.gson.JsonObject

/** Lal Kitab system — unconventional Vedic astrology with remedies. */
class LalKitabNamespace internal constructor(private val http: HttpClient) {

    private suspend fun post(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postForm(endpoint, body, language)

    private suspend fun postElement(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postElement(endpoint, body, language)

    /** Full Lal Kitab horoscope. */
    suspend fun getHoroscope(data: BirthData, language: String? = null): JsonElement =
        postElement("lalkitab_horoscope", data.ayanamshaBody(), language)

    /** Lal Kitab debts (Rin) — identifies karmic debts. */
    suspend fun getDebts(data: BirthData, language: String? = null): JsonElement =
        postElement("lalkitab_debts", data.ayanamshaBody(), language)

    /** Lal Kitab remedies for a specific planet. */
    suspend fun getRemedies(planet: PlanetName, data: BirthData, language: String? = null): JsonObject =
        post("lalkitab_remedies/$planet", data.ayanamshaBody(), language)

    /** Lal Kitab house analysis. */
    suspend fun getHouses(data: BirthData, language: String? = null): JsonElement =
        postElement("lalkitab_houses", data.ayanamshaBody(), language)

    /** Lal Kitab planetary positions with house-based interpretations. */
    suspend fun getPlanets(data: BirthData, language: String? = null): JsonElement =
        postElement("lalkitab_planets", data.ayanamshaBody(), language)
}
