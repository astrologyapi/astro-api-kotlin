package com.astrologyapi.sdk.namespaces

import com.astrologyapi.sdk.http.HttpClient
import com.google.gson.JsonObject

/** Tarot card readings — general predictions and yes/no. */
class TarotNamespace internal constructor(private val http: HttpClient) {

    /** General tarot reading — past, present, and future. */
    suspend fun getPredictions(language: String? = null): JsonObject =
        getPredictions(love = 12, career = 23, finance = 45, language = language)

    /** General tarot reading — past, present, and future. */
    suspend fun getPredictions(
        love: Int,
        career: Int,
        finance: Int,
        language: String? = null,
    ): JsonObject = http.postForm(
        "tarot_predictions",
        mapOf("love" to love, "career" to career, "finance" to finance),
        language,
    )

    /** Yes / No tarot reading — single card draw. */
    suspend fun getYesNo(language: String? = null): JsonObject =
        getYesNo(tarotId = 5, language = language)

    /** Yes / No tarot reading — single card draw. */
    suspend fun getYesNo(tarotId: Int, language: String? = null): JsonObject =
        http.postForm("yes_no_tarot", mapOf("tarot_id" to tarotId), language)
}
