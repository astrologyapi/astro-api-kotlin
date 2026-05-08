package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.models.BirthData
import com.astrologyapi.sdk.models.ZodiacSign
import com.google.gson.JsonObject

/** Daily, weekly, and monthly sun-sign & nakshatra horoscope predictions. */
class HoroscopesNamespace internal constructor(private val http: HttpClient) {

    private suspend fun post(endpoint: String, body: Map<String, Any?>, language: String?) =
        http.postForm(endpoint, body, language)

    /** Today's sun-sign horoscope prediction. */
    suspend fun getDaily(zodiac: ZodiacSign, language: String? = null): JsonObject =
        getDaily(zodiac, timezone = null, language = language)

    /** Today's sun-sign horoscope prediction. */
    suspend fun getDaily(zodiac: ZodiacSign, timezone: Double?, language: String? = null): JsonObject =
        post("sun_sign_prediction/daily/$zodiac", mapOfNotNull("timezone" to timezone), language)

    /** Tomorrow's sun-sign horoscope prediction. */
    suspend fun getNext(zodiac: ZodiacSign, language: String? = null): JsonObject =
        getNext(zodiac, timezone = null, language = language)

    /** Tomorrow's sun-sign horoscope prediction. */
    suspend fun getNext(zodiac: ZodiacSign, timezone: Double?, language: String? = null): JsonObject =
        post("sun_sign_prediction/daily/next/$zodiac", mapOfNotNull("timezone" to timezone), language)

    /** Yesterday's sun-sign horoscope prediction. */
    suspend fun getPrevious(zodiac: ZodiacSign, language: String? = null): JsonObject =
        getPrevious(zodiac, timezone = null, language = language)

    /** Yesterday's sun-sign horoscope prediction. */
    suspend fun getPrevious(zodiac: ZodiacSign, timezone: Double?, language: String? = null): JsonObject =
        post("sun_sign_prediction/daily/previous/$zodiac", mapOfNotNull("timezone" to timezone), language)

    /** Consolidated daily horoscope covering all life areas. */
    suspend fun getDailyConsolidated(zodiac: ZodiacSign, language: String? = null): JsonObject =
        getDailyConsolidated(zodiac, timezone = null, language = language)

    /** Consolidated daily horoscope covering all life areas. */
    suspend fun getDailyConsolidated(
        zodiac: ZodiacSign,
        timezone: Double?,
        language: String? = null,
    ): JsonObject = post(
        "sun_sign_consolidated/daily/$zodiac",
        mapOfNotNull("timezone" to timezone),
        language,
    )

    /** Monthly sun-sign horoscope prediction. */
    suspend fun getMonthly(zodiac: ZodiacSign, language: String? = null): JsonObject =
        getMonthly(zodiac, timezone = null, language = language)

    /** Monthly sun-sign horoscope prediction. */
    suspend fun getMonthly(zodiac: ZodiacSign, timezone: Double?, language: String? = null): JsonObject =
        post("horoscope_prediction/monthly/$zodiac", mapOfNotNull("timezone" to timezone), language)

    /** Daily prediction based on the birth Nakshatra (lunar mansion). */
    suspend fun getDailyNakshatra(data: BirthData, language: String? = null): JsonObject =
        post("daily_nakshatra_prediction", data.toMap(), language)
}
