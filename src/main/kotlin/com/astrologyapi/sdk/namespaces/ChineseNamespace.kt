package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.models.BirthData
import com.google.gson.JsonObject

/** Chinese astrology — zodiac sign and annual forecast. */
class ChineseNamespace internal constructor(private val http: HttpClient) {

    /** Chinese zodiac sign, traits, and compatibility. */
    suspend fun getZodiac(data: BirthData, language: String? = null): JsonObject =
        http.postForm("chinese_zodiac", data.toMap(), language)

    /** Annual predictions based on the Chinese zodiac sign. */
    suspend fun getYearForecast(data: BirthData, language: String? = null): JsonObject =
        http.postForm("chinese_year_forecast", data.toMap(), language)
}
