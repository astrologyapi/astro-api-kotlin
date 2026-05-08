package com.astrologyapi.sdk.http

import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.errors.*
import com.astrologyapi.sdk.models.ApiDomain
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Credentials
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okio.Buffer
import java.io.IOException
import java.util.concurrent.TimeUnit

internal const val SDK_VERSION = "1.0.0"

internal class HttpClient(
    config: AstrologyAPIConfig,
    private val observer: HttpExchangeObserver? = null,
) {

    private val userId: String
    private val apiKey: String
    private val useHeaderAuth: Boolean
    private val version: String
    private val baseJsonUrl: String
    private val basePdfUrl: String

    private val gson = Gson()
    private val json = "application/json; charset=utf-8".toMediaType()
    private val form = "application/x-www-form-urlencoded".toMediaType()

    private val okHttp: OkHttpClient

    init {
        apiKey = config.apiKey
            ?: System.getenv("ASTROLOGYAPI_API_KEY")
            ?: error("AstrologyAPI: apiKey is required. Pass it via AstrologyAPIConfig or set ASTROLOGYAPI_API_KEY.")
        useHeaderAuth = apiKey.contains("ak-")
        userId = config.userId
            ?: System.getenv("ASTROLOGYAPI_USER_ID")
            ?: if (useHeaderAuth) "" else error(
                "AstrologyAPI: userId is required for Basic auth. " +
                    "Pass it via AstrologyAPIConfig or set ASTROLOGYAPI_USER_ID.",
            )
        version = config.version
        baseJsonUrl = (config.baseJsonUrl ?: "https://json.astrologyapi.com").trimEnd('/')
        basePdfUrl = (config.basePdfUrl ?: "https://pdf.astrologyapi.com").trimEnd('/')

        okHttp = OkHttpClient.Builder()
            .connectTimeout(config.connectTimeoutMs, TimeUnit.MILLISECONDS)
            .readTimeout(config.readTimeoutMs, TimeUnit.MILLISECONDS)
            .build()
    }

    /** Execute a JSON-domain POST and return a parsed [JsonObject]. */
    suspend fun postJson(
        endpoint: String,
        body: Map<String, Any?>,
        language: String? = null,
    ): JsonObject = withContext(Dispatchers.IO) {
        postObject(
            domain = ApiDomain.JSON,
            endpoint = endpoint,
            body = body,
            language = language,
            encoding = RequestEncoding.JSON,
        )
    }

    /** Execute a form-urlencoded JSON-domain POST and return a parsed [JsonObject]. */
    suspend fun postForm(
        endpoint: String,
        body: Map<String, Any?>,
        language: String? = null,
    ): JsonObject = withContext(Dispatchers.IO) {
        postObject(
            domain = ApiDomain.JSON,
            endpoint = endpoint,
            body = body,
            language = language,
            encoding = RequestEncoding.FORM_URLENCODED,
        )
    }

    /** Execute a JSON-domain POST and return any parsed [JsonElement]. */
    suspend fun postElement(
        endpoint: String,
        body: Map<String, Any?>,
        language: String? = null,
        encoding: RequestEncoding = RequestEncoding.FORM_URLENCODED,
        domain: ApiDomain = ApiDomain.JSON,
    ): JsonElement = withContext(Dispatchers.IO) {
        val url = buildUrl(domain, endpoint)
        val request = buildRequest(url, body, language, encoding)
        executeElement(request, endpoint, domain, encoding)
    }

    /** Execute a PDF-domain POST and return the raw PDF bytes. */
    suspend fun postPdf(
        endpoint: String,
        body: Map<String, Any?>,
        language: String? = null,
    ): ByteArray = withContext(Dispatchers.IO) {
        val url = buildUrl(ApiDomain.PDF, endpoint)
        val request = buildRequest(url, body, language, RequestEncoding.FORM_URLENCODED)
        executePdf(request, endpoint, ApiDomain.PDF, RequestEncoding.FORM_URLENCODED)
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private fun buildUrl(domain: ApiDomain, endpoint: String): String {
        val base = if (domain == ApiDomain.PDF) basePdfUrl else baseJsonUrl
        return "$base/$version/$endpoint"
    }

    private fun buildRequest(
        url: String,
        body: Map<String, Any?>,
        language: String?,
        encoding: RequestEncoding,
    ): Request {
        val requestBody = when (encoding) {
            RequestEncoding.JSON -> gson.toJson(body).toRequestBody(json)
            RequestEncoding.FORM_URLENCODED -> buildFormBody(body)
        }
        return Request.Builder()
            .url(url)
            .post(requestBody)
            .header(
                "Content-Type",
                if (encoding == RequestEncoding.JSON) json.toString() else form.toString(),
            )
            .header("User-Agent", "astrologyapi-kotlin/$SDK_VERSION")
            .apply {
                if (useHeaderAuth) {
                    header("x-astrologyapi-key", apiKey)
                } else {
                    header("Authorization", Credentials.basic(userId, apiKey))
                }
            }
            .apply { language?.let { header("Accept-Language", it) } }
            .build()
    }

    private fun buildFormBody(body: Map<String, Any?>): FormBody {
        val builder = FormBody.Builder()
        body.forEach { (key, value) ->
            if (value != null) {
                builder.add(key, stringifyScalar(value))
            }
        }
        return builder.build()
    }

    private fun stringifyScalar(value: Any): String = when (value) {
        is JsonElement -> when {
            value.isJsonNull -> ""
            value.isJsonPrimitive -> value.asJsonPrimitive.run {
                when {
                    isString -> asString
                    isBoolean -> asBoolean.toString()
                    isNumber -> asNumber.toString()
                    else -> toString()
                }
            }
            else -> gson.toJson(value)
        }
        is Number, is Boolean -> value.toString()
        else -> value.toString()
    }

    private fun postObject(
        domain: ApiDomain,
        endpoint: String,
        body: Map<String, Any?>,
        language: String?,
        encoding: RequestEncoding,
    ): JsonObject {
        val url = buildUrl(domain, endpoint)
        val request = buildRequest(url, body, language, encoding)
        val element = executeElement(request, endpoint, domain, encoding)
        if (element.isJsonObject) {
            return element.asJsonObject
        }
        throw AstrologyAPIException(
            message = "Expected a JSON object response but received ${describeElement(element)}.",
            body = element.toString(),
        )
    }

    private fun executeElement(
        request: Request,
        endpoint: String,
        domain: ApiDomain,
        encoding: RequestEncoding,
    ): JsonElement {
        val capturedRequest = captureRequest(request, endpoint, domain, encoding)
        val response = try {
            okHttp.newCall(request).execute()
        } catch (e: IOException) {
            observer?.onExchange(
                CapturedHttpExchange(
                    request = capturedRequest,
                    failure = e.message ?: e::class.simpleName ?: "Network request failed",
                ),
            )
            throw NetworkException("Network request failed. Check your internet connection.", e)
        }

        response.use {
            val bodyStr = it.body?.string() ?: ""
            observer?.onExchange(
                CapturedHttpExchange(
                    request = capturedRequest,
                    response = CapturedHttpResponse(
                        status = it.code,
                        headers = captureHeaders(it.headers.toMultimap().mapValues { entry -> entry.value.joinToString(", ") }),
                        body = bodyStr,
                    ),
                ),
            )
            if (it.isSuccessful) {
                if (bodyStr.isBlank()) {
                    return JsonNull.INSTANCE
                }
                return JsonParser.parseString(bodyStr)
            }
            throwMappedError(it.code, it.header("Retry-After"), bodyStr)
        }
    }

    private fun executePdf(
        request: Request,
        endpoint: String,
        domain: ApiDomain,
        encoding: RequestEncoding,
    ): ByteArray {
        val capturedRequest = captureRequest(request, endpoint, domain, encoding)
        val response = try {
            okHttp.newCall(request).execute()
        } catch (e: IOException) {
            observer?.onExchange(
                CapturedHttpExchange(
                    request = capturedRequest,
                    failure = e.message ?: e::class.simpleName ?: "Network request failed",
                ),
            )
            throw NetworkException("Network request failed. Check your internet connection.", e)
        }

        response.use {
            val contentType = it.body?.contentType()?.toString()
            val bodyBytes = it.body?.bytes() ?: ByteArray(0)
            observer?.onExchange(
                CapturedHttpExchange(
                    request = capturedRequest,
                    response = CapturedHttpResponse(
                        status = it.code,
                        headers = captureHeaders(it.headers.toMultimap().mapValues { entry -> entry.value.joinToString(", ") }),
                        body = decodeBodyPreview(bodyBytes, contentType),
                        binaryLength = if (isTextual(contentType)) null else bodyBytes.size,
                    ),
                ),
            )
            if (it.isSuccessful) {
                return bodyBytes
            }
            val bodyStr = decodeBodyPreview(bodyBytes, contentType) ?: ""
            throwMappedError(it.code, it.header("Retry-After"), bodyStr)
        }
    }

    private fun throwMappedError(status: Int, retryAfterHeader: String?, bodyStr: String): Nothing {
        val apiMessage = extractMessage(bodyStr)
        when (status) {
            400, 422 -> throw ValidationException(
                apiMessage ?: "Validation error. Check your request parameters.",
                body = bodyStr,
            )
            401 -> throw AuthenticationException(
                apiMessage ?: "Authentication failed. Check your apiKey.",
                body = bodyStr,
            )
            402 -> throw QuotaExceededException(
                apiMessage ?: "API quota exceeded. Please upgrade your plan.",
                body = bodyStr,
            )
            403 -> throw PlanRestrictedException(
                apiMessage ?: "This endpoint is not available on your current plan.",
                body = bodyStr,
            )
            429 -> {
                val retryAfter = retryAfterHeader?.toIntOrNull()
                throw RateLimitException(
                    apiMessage ?: "Rate limit exceeded. Please wait before retrying.",
                    retryAfter = retryAfter,
                    body = bodyStr,
                )
            }
            else -> if (status >= 500) {
                throw ServerException(
                    apiMessage ?: "AstrologyAPI server error ($status). Please try again later.",
                    status = status,
                    body = bodyStr,
                )
            } else {
                throw AstrologyAPIException(
                    apiMessage ?: "Unexpected error ($status).",
                    status = status,
                    body = bodyStr,
                )
            }
        }
    }

    private fun captureRequest(
        request: Request,
        endpoint: String,
        domain: ApiDomain,
        encoding: RequestEncoding,
    ): CapturedHttpRequest = CapturedHttpRequest(
        endpoint = endpoint,
        url = request.url.toString(),
        domain = domain,
        encoding = encoding,
        method = request.method,
        headers = captureHeaders(request.headers.toMultimap().mapValues { entry -> entry.value.joinToString(", ") }),
        body = requestBodyString(request),
    )

    private fun captureHeaders(headers: Map<String, String>): Map<String, String> =
        headers.mapValues { (name, value) -> redactHeader(name, value) }

    private fun redactHeader(name: String, value: String): String = when {
        name.equals("x-astrologyapi-key", ignoreCase = true) -> "[REDACTED]"
        name.equals("authorization", ignoreCase = true) && value.startsWith("Basic ", ignoreCase = true) ->
            "Basic [REDACTED]"
        else -> value
    }

    private fun requestBodyString(request: Request): String? {
        val body = request.body ?: return null
        return runCatching {
            val buffer = Buffer()
            body.writeTo(buffer)
            buffer.readUtf8()
        }.getOrNull()
    }

    private fun decodeBodyPreview(bytes: ByteArray, contentType: String?): String? {
        if (bytes.isEmpty()) return ""
        if (!isTextual(contentType) && !looksLikeJson(bytes)) {
            return null
        }
        return runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull()
    }

    private fun isTextual(contentType: String?): Boolean {
        if (contentType == null) return false
        val normalized = contentType.lowercase()
        return normalized.startsWith("text/") || normalized.contains("json") || normalized.contains("xml")
    }

    private fun looksLikeJson(bytes: ByteArray): Boolean {
        val trimmed = bytes.toString(Charsets.UTF_8).trimStart()
        return trimmed.startsWith("{") || trimmed.startsWith("[") || trimmed.startsWith("\"")
    }

    private fun extractMessage(bodyStr: String): String? = runCatching {
        val element = JsonParser.parseString(bodyStr)
        flattenMessages(element).distinct().joinToString("; ").takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun flattenMessages(element: JsonElement?): List<String> = when {
        element == null || element.isJsonNull -> emptyList()
        element.isJsonPrimitive -> listOf(element.asString)
        element.isJsonArray -> element.asJsonArray.flatMap(::flattenMessages)
        element.isJsonObject -> {
            val obj = element.asJsonObject
            val priority = listOf("message", "error", "errors", "msg")
            val collected = priority.flatMap { key -> flattenMessages(obj[key]) }
            if (collected.isNotEmpty()) {
                collected
            } else {
                obj.entrySet().flatMap { (key, value) ->
                    flattenMessages(value).map { message ->
                        if (message.startsWith("$key:")) message else "$key: $message"
                    }
                }
            }
        }
        else -> emptyList()
    }

    private fun describeElement(element: JsonElement): String = when {
        element.isJsonNull -> "null"
        element.isJsonObject -> "an object"
        element.isJsonArray -> "an array"
        else -> "a scalar"
    }
}
