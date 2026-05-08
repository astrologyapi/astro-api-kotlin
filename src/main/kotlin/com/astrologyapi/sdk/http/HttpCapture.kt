package com.astrologyapi.sdk.http

import com.astrologyapi.sdk.models.ApiDomain

internal enum class RequestEncoding {
    JSON,
    FORM_URLENCODED,
}

internal data class CapturedHttpRequest(
    val endpoint: String,
    val url: String,
    val domain: ApiDomain,
    val encoding: RequestEncoding,
    val method: String,
    val headers: Map<String, String>,
    val body: String?,
)

internal data class CapturedHttpResponse(
    val status: Int,
    val headers: Map<String, String>,
    val body: String?,
    val binaryLength: Int? = null,
)

internal data class CapturedHttpExchange(
    val request: CapturedHttpRequest,
    val response: CapturedHttpResponse? = null,
    val failure: String? = null,
)

internal fun interface HttpExchangeObserver {
    fun onExchange(exchange: CapturedHttpExchange)
}
