package com.astrologyapi.sdk.testing

import com.astrologyapi.sdk.AstrologyAPI
import com.astrologyapi.sdk.AstrologyAPIConfig
import com.astrologyapi.sdk.errors.AstrologyAPIException
import com.astrologyapi.sdk.http.CapturedHttpExchange
import com.astrologyapi.sdk.http.HttpClient
import com.astrologyapi.sdk.http.HttpExchangeObserver
import com.astrologyapi.sdk.http.RequestEncoding
import com.astrologyapi.sdk.models.*
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.runBlocking
import kotlin.reflect.KFunction
import kotlin.reflect.KParameter
import kotlin.reflect.KVisibility
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.instanceParameter
import kotlin.reflect.full.memberFunctions
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.io.path.exists
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.math.min
import kotlin.random.Random

private val gson = GsonBuilder().setPrettyPrinting().create()
private val repoRoot: Path = Paths.get(System.getProperty("user.dir")).toAbsolutePath()
private val testingDir: Path = repoRoot.resolve("testing")
private val catalogDir: Path = testingDir.resolve("catalog")
private val resultsDir: Path = testingDir.resolve("results")
private val postmanInventoryPath: Path = testingDir.resolve("postman/postman-endpoints.json")

private val birthFileType = object : TypeToken<BirthData>() {}.type
private val numerologyFileType = object : TypeToken<NumerologyData>() {}.type
private val matchFileType = object : TypeToken<MatchBirthData>() {}.type
private val coupleFileType = object : TypeToken<CoupleBirthData>() {}.type
private val brandingFileType = object : TypeToken<PDFBranding>() {}.type
private val zodiacFileType = object : TypeToken<ZodiacScenario>() {}.type
private val postmanType = object : TypeToken<List<PostmanEndpoint>>() {}.type

internal enum class QaResponseKind {
    JSON,
    PDF,
}

internal enum class QaStatus {
    SUCCESS,
    FAILURE,
    NOT_TESTABLE,
}

internal data class ZodiacPair(
    val zodiac: String,
    val partnerZodiac: String,
)

internal data class ZodiacScenario(
    val primary: ZodiacPair,
    val alternates: List<ZodiacPair> = emptyList(),
)

internal data class QaScenarios(
    val births: Map<String, BirthData>,
    val numerology: Map<String, NumerologyData>,
    val match: MatchBirthData,
    val couple: CoupleBirthData,
    val pdfBranding: PDFBranding,
    val zodiac: ZodiacScenario,
) {
    val standardBirth: BirthData get() = births.getValue("standard")
}

internal data class PostmanBodyParameter(
    val key: String,
    val value: String? = null,
    val disabled: Boolean = false,
)

internal data class PostmanEndpoint(
    val normalizedEndpoint: String,
    val endpoint: String,
    val domain: String,
    val method: String,
    val displayNames: List<String> = emptyList(),
    val sourceFiles: List<String> = emptyList(),
    val bodyMode: String? = null,
    val bodyParameters: List<PostmanBodyParameter> = emptyList(),
    val pathParameters: List<String> = emptyList(),
    val authStyles: List<String> = emptyList(),
    val headerKeys: List<String> = emptyList(),
) {
    val enabledBodyKeys: List<String>
        get() = bodyParameters.filterNot { it.disabled }.map { it.key }

    val canonicalEndpoint: String
        get() = canonicalizeEndpoint(normalizedEndpoint)
}

internal data class RequestSnapshot(
    val endpoint: String,
    val normalizedEndpoint: String,
    val canonicalEndpoint: String,
    val domain: String,
    val encoding: String,
    val authStyle: String,
    val headers: Map<String, String>,
    val body: String?,
    val bodyFields: Map<String, String>,
    val pathValues: List<String>,
)

internal data class SdkInventoryEntry(
    val module: String,
    val functionName: String,
    val targetId: String,
    val responseKind: String,
    val domain: String,
    val requestEncoding: String,
    val normalizedEndpoint: String,
    val canonicalEndpoint: String,
    val bodyFields: List<String>,
    val pathValues: List<String>,
    val authStyle: String,
    val headers: Map<String, String>,
    val requestBody: String?,
    val arguments: Map<String, String>,
    val coverageMatch: String? = null,
)

internal data class ExecutionRecord(
    val targetId: String,
    val module: String,
    val functionName: String,
    val responseKind: QaResponseKind,
    val status: QaStatus,
    val httpStatus: Int?,
    val message: String,
    val errorName: String? = null,
    val request: RequestSnapshot? = null,
    val responseSummary: String? = null,
    val rawResponse: Any? = null,
)

internal data class ComparisonRecord(
    val targetId: String,
    val endpoint: String,
    val status: QaStatus,
    val message: String,
    val fallbackReason: String? = null,
    val sdkRequest: RequestSnapshot? = null,
    val directRequest: RequestSnapshot? = null,
    val sdkResponse: String? = null,
    val directResponse: String? = null,
    val diff: List<String> = emptyList(),
)

internal class ExchangeCollector : HttpExchangeObserver {
    private val exchanges = mutableListOf<CapturedHttpExchange>()

    override fun onExchange(exchange: CapturedHttpExchange) {
        exchanges += exchange
    }

    fun clear() {
        exchanges.clear()
    }

    fun last(): CapturedHttpExchange? = exchanges.lastOrNull()
}

internal class InvocationTarget(
    val module: String,
    val function: KFunction<*>,
    val responseKind: QaResponseKind,
    private val instanceProvider: (AstrologyAPI) -> Any,
    private val parameterArguments: Map<KParameter, Any?>,
    val argumentSummary: Map<String, String>,
    val plannerNote: String? = null,
) {
    val functionName: String = function.name
    val targetId: String = "$module.$functionName"

    suspend fun execute(api: AstrologyAPI, collector: ExchangeCollector): Pair<Any?, CapturedHttpExchange?> {
        collector.clear()
        val bound = linkedMapOf<KParameter, Any?>()
        bound[function.instanceParameter!!] = instanceProvider(api)
        bound.putAll(parameterArguments)
        val result = function.callSuspendBy(bound)
        return result to collector.last()
    }

    fun defaultBodyFields(): Map<String, String> {
        val defaults = linkedMapOf<String, String>()
        parameterArguments.values.forEach { value ->
            when (value) {
                is BirthData -> {
                    value.toMap(includeProfileFields = true).forEach { (key, fieldValue) ->
                        defaults[key] = fieldValue.toString()
                    }
                    value.name?.takeIf { it.isNotBlank() }?.let { defaults["full name"] = it }
                    value.name?.takeIf { it.isNotBlank() }?.let { defaults["full_name"] = it }
                    defaults["date"] = value.day.toString()
                }
                is NumerologyData -> {
                    value.toMap().forEach { (key, fieldValue) -> defaults[key] = fieldValue.toString() }
                    defaults["full name"] = value.name
                    defaults["full_name"] = value.name
                    defaults["date"] = value.day.toString()
                }
                is MatchBirthData -> {
                    value.flatten().forEach { (key, fieldValue) -> defaults[key] = fieldValue.toString() }
                    value.male.name?.let { defaults["m_first_name"] = splitName(it).first ?: "" }
                    value.male.name?.let { splitName(it).second?.let { last -> defaults["m_last_name"] = last } }
                    value.female.name?.let { defaults["f_first_name"] = splitName(it).first ?: "" }
                    value.female.name?.let { splitName(it).second?.let { last -> defaults["f_last_name"] = last } }
                    value.male.place?.let { defaults["m_place"] = it }
                    value.female.place?.let { defaults["f_place"] = it }
                    defaults["ayanamsha"] = "LAHIRI"
                }
                is CoupleBirthData -> {
                    value.flatten().forEach { (key, fieldValue) -> defaults[key] = fieldValue.toString() }
                    value.person1.name?.let { defaults["p_first_name"] = splitName(it).first ?: "" }
                    value.person1.name?.let { splitName(it).second?.let { last -> defaults["p_last_name"] = last } }
                    value.person2.name?.let { defaults["s_first_name"] = splitName(it).first ?: "" }
                    value.person2.name?.let { splitName(it).second?.let { last -> defaults["s_last_name"] = last } }
                    value.person1.place?.let { defaults["p_place"] = it }
                    value.person2.place?.let { defaults["s_place"] = it }
                }
                is PDFBranding -> value.toMap().forEach { (key, fieldValue) -> defaults[key] = fieldValue.toString() }
            }
        }

        parameterArguments.forEach { (parameter, value) ->
            val name = parameter.name ?: return@forEach
            when (name) {
                "yearCount" -> {
                    val birth = parameterArguments.values.filterIsInstance<BirthData>().firstOrNull()
                    val yearCount = value as? Int
                    if (birth != null && yearCount != null) {
                        defaults["varshaphal_year"] = (birth.year + yearCount).toString()
                    }
                }
                "year" -> if (functionName.startsWith("getSolarReturn") && value is Int) {
                    defaults["solar_year"] = value.toString()
                }
                "solarYear" -> if (value is Int) {
                    defaults["solar_year"] = value.toString()
                }
                "tarotId" -> if (value != null) defaults["tarot_id"] = value.toString()
                "love", "career", "finance", "maxRows", "timezone" -> if (value != null) defaults[name] = value.toString()
                "name" -> {
                    val split = splitName(value as? String)
                    split.first?.let { defaults["name"] = value.toString() }
                    split.first?.let { defaults["m_first_name"] = defaults["m_first_name"] ?: it }
                    split.second?.let { defaults["m_last_name"] = defaults["m_last_name"] ?: it }
                    split.first?.let { defaults["p_first_name"] = defaults["p_first_name"] ?: it }
                    split.second?.let { defaults["p_last_name"] = defaults["p_last_name"] ?: it }
                }
                "partnerName" -> {
                    val split = splitName(value as? String)
                    split.first?.let { defaults["partner_name"] = value.toString() }
                    split.first?.let { defaults["f_first_name"] = defaults["f_first_name"] ?: it }
                    split.second?.let { defaults["f_last_name"] = defaults["f_last_name"] ?: it }
                    split.first?.let { defaults["s_first_name"] = defaults["s_first_name"] ?: it }
                    split.second?.let { defaults["s_last_name"] = defaults["s_last_name"] ?: it }
                }
                "place" -> if (value != null) defaults["place"] = value.toString()
                "language" -> if (value != null) defaults["language"] = value.toString()
            }
        }

        return defaults
    }
}

internal object QaHarness {

    fun ensureOutputDirectories() {
        Files.createDirectories(catalogDir)
        Files.createDirectories(resultsDir)
    }

    fun loadScenarios(): QaScenarios = QaScenarios(
        births = linkedMapOf(
            "standard" to readJson(testingDir.resolve("test-data/birth/standard.json"), birthFileType),
            "dst-sensitive" to readJson(testingDir.resolve("test-data/birth/dst-sensitive.json"), birthFileType),
            "fractional-timezone" to readJson(testingDir.resolve("test-data/birth/fractional-timezone.json"), birthFileType),
            "negative-timezone" to readJson(testingDir.resolve("test-data/birth/negative-timezone.json"), birthFileType),
            "edge-date" to readJson(testingDir.resolve("test-data/birth/edge-date.json"), birthFileType),
        ),
        numerology = linkedMapOf(
            "basic" to readJson(testingDir.resolve("test-data/numerology/basic.json"), numerologyFileType),
            "alternate" to readJson(testingDir.resolve("test-data/numerology/alternate-name.json"), numerologyFileType),
        ),
        match = readJson(testingDir.resolve("test-data/match/basic-match.json"), matchFileType),
        couple = readJson(testingDir.resolve("test-data/couple/basic-couple.json"), coupleFileType),
        pdfBranding = readJson(testingDir.resolve("test-data/pdf/basic-branding.json"), brandingFileType),
        zodiac = readJson(testingDir.resolve("test-data/zodiac/basic-sign-pairs.json"), zodiacFileType),
    )

    fun loadPostmanInventory(): List<PostmanEndpoint> =
        readJson(postmanInventoryPath, postmanType)

    fun loadTargets(scenarios: QaScenarios = loadScenarios()): List<InvocationTarget> {
        val prototype = AstrologyAPI(
            AstrologyAPIConfig(
                userId = "inventory-user",
                apiKey = "ak-inventory-key",
                baseJsonUrl = "https://example.invalid",
                basePdfUrl = "https://example.invalid",
            )
        )

        val modules = listOf(
            "vedic" to { api: AstrologyAPI -> api.vedic as Any },
            "kp" to { api: AstrologyAPI -> api.kp as Any },
            "lalKitab" to { api: AstrologyAPI -> api.lalKitab as Any },
            "horoscopes" to { api: AstrologyAPI -> api.horoscopes as Any },
            "numerology" to { api: AstrologyAPI -> api.numerology as Any },
            "western" to { api: AstrologyAPI -> api.western as Any },
            "westernTransit" to { api: AstrologyAPI -> api.westernTransit as Any },
            "tarot" to { api: AstrologyAPI -> api.tarot as Any },
            "chinese" to { api: AstrologyAPI -> api.chinese as Any },
            "location" to { api: AstrologyAPI -> api.location as Any },
            "pdf.vedic" to { api: AstrologyAPI -> api.pdf.vedic as Any },
            "pdf.western" to { api: AstrologyAPI -> api.pdf.western as Any },
        )

        return modules.flatMap { (moduleName, instanceProvider) ->
            val prototypeModule = instanceProvider(prototype)
            prototypeModule::class.memberFunctions
                .filter { it.visibility == KVisibility.PUBLIC && it.name.startsWith("get") }
                .groupBy { it.name }
                .mapNotNull { (_, overloads) ->
                    val function = overloads.maxByOrNull { candidate ->
                        candidate.parameters.count { it.kind == KParameter.Kind.VALUE }
                    } ?: return@mapNotNull null
                    buildTarget(moduleName, instanceProvider, function, scenarios)
                }
        }.sortedBy { it.targetId }
    }

    suspend fun buildSdkInventory(
        targets: List<InvocationTarget> = loadTargets(),
    ): List<SdkInventoryEntry> {
        ensureOutputDirectories()

        val server = MockWebServer()
        server.start()
        val collector = ExchangeCollector()
        val config = AstrologyAPIConfig(
            userId = "inventory-user",
            apiKey = "ak-inventory-key",
            baseJsonUrl = server.url("/").toString(),
            basePdfUrl = server.url("/").toString(),
        )
        val api = AstrologyAPI(config, collector)

        return try {
            targets.mapNotNull { target ->
                enqueueInventoryResponse(server, target.responseKind)
                runCatching {
                    val (_, exchange) = target.execute(api, collector)
                    val snapshot = exchange?.let { buildRequestSnapshot(target, it) } ?: return@runCatching null
                    SdkInventoryEntry(
                        module = target.module,
                        functionName = target.functionName,
                        targetId = target.targetId,
                        responseKind = target.responseKind.name.lowercase(),
                        domain = snapshot.domain,
                        requestEncoding = snapshot.encoding,
                        normalizedEndpoint = snapshot.normalizedEndpoint,
                        canonicalEndpoint = snapshot.canonicalEndpoint,
                        bodyFields = snapshot.bodyFields.keys.toList(),
                        pathValues = snapshot.pathValues,
                        authStyle = snapshot.authStyle,
                        headers = snapshot.headers,
                        requestBody = snapshot.body,
                        arguments = target.argumentSummary,
                    )
                }.getOrElse { error ->
                    SdkInventoryEntry(
                        module = target.module,
                        functionName = target.functionName,
                        targetId = target.targetId,
                        responseKind = target.responseKind.name.lowercase(),
                        domain = "unknown",
                        requestEncoding = "unknown",
                        normalizedEndpoint = "unresolved",
                        canonicalEndpoint = "unresolved",
                        bodyFields = emptyList(),
                        pathValues = emptyList(),
                        authStyle = "unknown",
                        headers = emptyMap(),
                        requestBody = null,
                        arguments = target.argumentSummary + mapOf("planner_error" to (error.message ?: error::class.simpleName.orEmpty())),
                    )
                }
            }
        } finally {
            server.shutdown()
        }
    }

    fun writeCatalogArtifacts(
        sdkInventory: List<SdkInventoryEntry>,
        postmanInventory: List<PostmanEndpoint> = loadPostmanInventory(),
        executionRecords: List<ExecutionRecord> = emptyList(),
    ) {
        ensureOutputDirectories()
        val matchedByTarget = matchSdkToPostman(sdkInventory, postmanInventory)
        val enrichedInventory = sdkInventory.map { entry ->
            entry.copy(coverageMatch = matchedByTarget[entry.targetId]?.normalizedEndpoint)
        }
        catalogDir.resolve("sdk-endpoints.json").writeText(gson.toJson(enrichedInventory))
        catalogDir.resolve("sdk-modules.md").writeText(buildModuleCatalog(enrichedInventory, executionRecords))
        resultsDir.resolve("missing-apis.md").writeText(buildCoverageReport(enrichedInventory, postmanInventory))
        resultsDir.resolve("parameter-mismatches.md").writeText(buildParameterMismatchReport(enrichedInventory, postmanInventory))
    }

    suspend fun runDeterministicSweep(
        targets: List<InvocationTarget> = loadTargets(),
    ): List<ExecutionRecord> {
        ensureOutputDirectories()
        val credentials = loadCredentials()
        if (credentials == null) {
            val records = listOf(
                ExecutionRecord(
                    targetId = "qa.credentials",
                    module = "qa",
                    functionName = "credentials",
                    responseKind = QaResponseKind.JSON,
                    status = QaStatus.NOT_TESTABLE,
                    httpStatus = null,
                    message = "Live credentials were not found in environment variables or .env/.env.local.",
                )
            )
            resultsDir.resolve("failing-apis.md").writeText(buildFailingReport(records))
            resultsDir.resolve("not-testable-with-current-plan.md").writeText(buildNotTestableReport(records))
            return records
        }

        val collector = ExchangeCollector()
        val api = AstrologyAPI(
            AstrologyAPIConfig(
                userId = credentials.userId,
                apiKey = credentials.apiKey,
            ),
            collector,
        )

        val records = targets.map { target -> executeTarget(target, api, collector) }
        resultsDir.resolve("failing-apis.md").writeText(buildFailingReport(records))
        resultsDir.resolve("not-testable-with-current-plan.md").writeText(buildNotTestableReport(records))
        return records
    }

    suspend fun runRandomizedCompare(
        sampleSize: Int = System.getenv("ASTROLOGYAPI_QA_COMPARE_SAMPLE_SIZE")?.toIntOrNull() ?: 20,
    ): List<ComparisonRecord> {
        ensureOutputDirectories()
        val credentials = loadCredentials()
        if (credentials == null) {
            val records = listOf(
                ComparisonRecord(
                    targetId = "qa.credentials",
                    endpoint = "unavailable",
                    status = QaStatus.NOT_TESTABLE,
                    message = "Live credentials were not found in environment variables or .env/.env.local.",
                )
            )
            resultsDir.resolve("inconsistent-responses.md").writeText(buildComparisonReport(records))
            return records
        }

        val targets = loadTargets()
        val postmanInventory = loadPostmanInventory()
        val random = Random(42)
        val selected = targets.shuffled(random).take(min(sampleSize, targets.size))
        val sdkCollector = ExchangeCollector()
        val directCollector = ExchangeCollector()
        val sdkApi = AstrologyAPI(AstrologyAPIConfig(userId = credentials.userId, apiKey = credentials.apiKey), sdkCollector)
        val directClient = HttpClient(AstrologyAPIConfig(userId = credentials.userId, apiKey = credentials.apiKey), directCollector)

        val records = selected.mapNotNull { target ->
            val sdkRecord = executeTarget(target, sdkApi, sdkCollector)
            if (sdkRecord.status != QaStatus.SUCCESS) {
                return@mapNotNull ComparisonRecord(
                    targetId = target.targetId,
                    endpoint = sdkRecord.request?.endpoint ?: "unresolved",
                    status = sdkRecord.status,
                    message = sdkRecord.message,
                    sdkRequest = sdkRecord.request,
                    sdkResponse = sdkRecord.responseSummary,
                )
            }

            val snapshot = sdkRecord.request ?: return@mapNotNull null
            compareAgainstDirect(
                target = target,
                sdkRecord = sdkRecord,
                snapshot = snapshot,
                postmanInventory = postmanInventory,
                directClient = directClient,
                directCollector = directCollector,
            )
        }

        resultsDir.resolve("inconsistent-responses.md").writeText(buildComparisonReport(records))
        return records
    }

    suspend fun runSingleTarget(
        sdkTargetId: String = System.getenv("ASTROLOGYAPI_QA_SDK_TARGET") ?: "vedic.getBirthDetails",
        directTargetLookup: String? = System.getenv("ASTROLOGYAPI_QA_DIRECT_TARGET_LOOKUP")?.takeIf { it.isNotBlank() },
    ): String {
        val credentials = loadCredentials()
            ?: return "Live credentials were not found in environment variables or .env/.env.local."

        val target = loadTargets().firstOrNull { it.targetId == sdkTargetId }
            ?: return "No Kotlin SDK target matched `$sdkTargetId`."

        val postmanInventory = loadPostmanInventory()
        val sdkCollector = ExchangeCollector()
        val directCollector = ExchangeCollector()
        val sdkApi = AstrologyAPI(AstrologyAPIConfig(userId = credentials.userId, apiKey = credentials.apiKey), sdkCollector)
        val directClient = HttpClient(AstrologyAPIConfig(userId = credentials.userId, apiKey = credentials.apiKey), directCollector)

        val sdkRecord = executeTarget(target, sdkApi, sdkCollector)
        val builder = StringBuilder()
        builder.appendLine("# Single Endpoint Debug")
        builder.appendLine()
        builder.appendLine("- SDK target: `${target.targetId}`")
        builder.appendLine("- Arguments: `${target.argumentSummary}`")
        builder.appendLine("- SDK status: `${sdkRecord.status.name.lowercase()}`")
        builder.appendLine("- SDK message: ${sdkRecord.message}")
        builder.appendLine()

        val snapshot = sdkRecord.request
        if (snapshot == null) {
            return builder.toString()
        }

        val forcedMatch = directTargetLookup?.let { lookup ->
            postmanInventory.firstOrNull { endpoint ->
                endpoint.normalizedEndpoint.contains(lookup, ignoreCase = true) ||
                    endpoint.displayNames.any { it.contains(lookup, ignoreCase = true) }
            }
        }

        val comparison = compareAgainstDirect(
            target = target,
            sdkRecord = sdkRecord,
            snapshot = snapshot,
            postmanInventory = postmanInventory,
            directClient = directClient,
            directCollector = directCollector,
            forcedPostmanMatch = forcedMatch,
        )

        builder.appendLine("- Direct status: `${comparison.status.name.lowercase()}`")
        comparison.fallbackReason?.let { builder.appendLine("- Direct fallback: $it") }
        builder.appendLine()
        builder.appendLine("## SDK Request")
        builder.appendLine()
        builder.appendLine("```json")
        builder.appendLine(gson.toJson(snapshot))
        builder.appendLine("```")
        builder.appendLine()
        builder.appendLine("## Direct Request")
        builder.appendLine()
        builder.appendLine("```json")
        builder.appendLine(gson.toJson(comparison.directRequest))
        builder.appendLine("```")
        builder.appendLine()
        builder.appendLine("## Comparison")
        builder.appendLine()
        builder.appendLine(comparison.message)
        if (comparison.diff.isNotEmpty()) {
            builder.appendLine()
            builder.appendLine("```text")
            comparison.diff.forEach { builder.appendLine(it) }
            builder.appendLine("```")
        }
        return builder.toString()
    }

    private fun buildTarget(
        module: String,
        instanceProvider: (AstrologyAPI) -> Any,
        function: KFunction<*>,
        scenarios: QaScenarios,
    ): InvocationTarget? {
        val arguments = linkedMapOf<KParameter, Any?>()
        val summary = linkedMapOf<String, String>()

        function.parameters
            .filter { it.kind == KParameter.Kind.VALUE }
            .forEach { parameter ->
                val planned = planValue(module, function.name, parameter, responseKindFor(function), scenarios)
                    ?: if (parameter.isOptional) {
                        null
                    } else {
                        return null
                    }

                if (planned == null && parameter.isOptional) {
                    return@forEach
                }

                if (planned != null) {
                    arguments[parameter] = planned.first
                    summary[parameter.name.orEmpty()] = planned.second
                }
            }

        return InvocationTarget(
            module = module,
            function = function,
            responseKind = responseKindFor(function),
            instanceProvider = instanceProvider,
            parameterArguments = arguments,
            argumentSummary = summary,
        )
    }

    private fun planValue(
        module: String,
        functionName: String,
        parameter: KParameter,
        responseKind: QaResponseKind,
        scenarios: QaScenarios,
    ): Pair<Any, String>? {
        val classifier = parameter.type.classifier
        val name = parameter.name.orEmpty()

        return when (classifier) {
            BirthData::class -> scenarios.standardBirth to "birth.standard"
            MatchBirthData::class -> scenarios.match to "match.basic-match"
            CoupleBirthData::class -> scenarios.couple to "couple.basic-couple"
            NumerologyData::class -> scenarios.numerology.getValue("basic") to "numerology.basic"
            PDFBranding::class -> scenarios.pdfBranding to "pdf.basic-branding"
            ChartId::class -> ChartId.D1 to ChartId.D1.toString()
            PlanetName::class -> PlanetName.JUPITER to PlanetName.JUPITER.toString()
            ZodiacSign::class -> when (name) {
                "partnerZodiac" -> ZodiacSign.valueOf(scenarios.zodiac.primary.partnerZodiac.uppercase()) to scenarios.zodiac.primary.partnerZodiac
                else -> ZodiacSign.valueOf(scenarios.zodiac.primary.zodiac.uppercase()) to scenarios.zodiac.primary.zodiac
            }
            String::class -> planStringValue(module, functionName, name, responseKind, scenarios)
            Int::class -> planIntValue(functionName, name, scenarios)
            Double::class -> planDoubleValue(name, scenarios)
            else -> null
        }
    }

    private fun planStringValue(
        module: String,
        functionName: String,
        name: String,
        responseKind: QaResponseKind,
        scenarios: QaScenarios,
    ): Pair<Any, String>? = when (name) {
        "language" -> "en" to "en"
        "name" -> when {
            module == "pdf.vedic" && functionName == "getMatchMaking" -> scenarios.match.male.name.orEmpty() to scenarios.match.male.name.orEmpty()
            module == "pdf.western" && functionName == "getSynastry" -> scenarios.couple.person1.name.orEmpty() to scenarios.couple.person1.name.orEmpty()
            else -> scenarios.standardBirth.name.orEmpty() to scenarios.standardBirth.name.orEmpty()
        }
        "partnerName" -> when {
            module == "pdf.vedic" && functionName == "getMatchMaking" -> scenarios.match.female.name.orEmpty() to scenarios.match.female.name.orEmpty()
            else -> scenarios.couple.person2.name.orEmpty() to scenarios.couple.person2.name.orEmpty()
        }
        "place" -> when {
            module == "pdf.vedic" && functionName == "getMatchMaking" -> scenarios.match.male.place.orEmpty() to scenarios.match.male.place.orEmpty()
            else -> scenarios.standardBirth.place.orEmpty() to scenarios.standardBirth.place.orEmpty()
        }
        "mahaDasha" -> "aries" to "aries"
        "md" -> "saturn" to "saturn"
        "ad" -> "mercury" to "mercury"
        "pd" -> "venus" to "venus"
        "sd" -> "moon" to "moon"
        else -> if (responseKind == QaResponseKind.JSON && name.isEmpty()) null else null
    }

    private fun planIntValue(
        functionName: String,
        name: String,
        scenarios: QaScenarios,
    ): Pair<Any, String>? = when (name) {
        "day" -> scenarios.standardBirth.day to scenarios.standardBirth.day.toString()
        "month" -> scenarios.standardBirth.month to scenarios.standardBirth.month.toString()
        "year" -> if (functionName.startsWith("getSolarReturn")) 2025 to "2025" else scenarios.standardBirth.year to scenarios.standardBirth.year.toString()
        "hour" -> scenarios.standardBirth.hour to scenarios.standardBirth.hour.toString()
        "min" -> scenarios.standardBirth.min to scenarios.standardBirth.min.toString()
        "maxRows" -> 6 to "6"
        "yearCount" -> 34 to "34"
        "solarYear" -> 2025 to "2025"
        "tarotId" -> 5 to "5"
        "love" -> 12 to "12"
        "career" -> 23 to "23"
        "finance" -> 45 to "45"
        else -> null
    }

    private fun planDoubleValue(name: String, scenarios: QaScenarios): Pair<Any, String>? = when (name) {
        "lat" -> scenarios.standardBirth.lat to scenarios.standardBirth.lat.toString()
        "lon" -> scenarios.standardBirth.lon to scenarios.standardBirth.lon.toString()
        "tzone", "timezone" -> scenarios.standardBirth.tzone to scenarios.standardBirth.tzone.toString()
        else -> null
    }
}

private fun responseKindFor(function: KFunction<*>): QaResponseKind =
    if (function.returnType.classifier == ByteArray::class) QaResponseKind.PDF else QaResponseKind.JSON

private fun enqueueInventoryResponse(server: MockWebServer, responseKind: QaResponseKind) {
    val response = when (responseKind) {
        QaResponseKind.JSON -> MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/json")
            .setBody("""{"status":true,"ok":true}""")
        QaResponseKind.PDF -> MockResponse()
            .setResponseCode(200)
            .addHeader("Content-Type", "application/pdf")
            .setBody(okio.Buffer().write(byteArrayOf(0x25, 0x50, 0x44, 0x46)))
    }
    server.enqueue(response)
}

private suspend fun executeTarget(
    target: InvocationTarget,
    api: AstrologyAPI,
    collector: ExchangeCollector,
): ExecutionRecord {
    return try {
        val (response, exchange) = target.execute(api, collector)
        val request = exchange?.let { buildRequestSnapshot(target, it) }
        when (target.responseKind) {
            QaResponseKind.JSON -> ExecutionRecord(
                targetId = target.targetId,
                module = target.module,
                functionName = target.functionName,
                responseKind = target.responseKind,
                status = QaStatus.SUCCESS,
                httpStatus = 200,
                message = "SDK call succeeded.",
                request = request,
                responseSummary = summarizeJson(response as? JsonElement ?: JsonParser.parseString(gson.toJson(response))),
                rawResponse = response,
            )
            QaResponseKind.PDF -> {
                val inspection = inspectPdfResponse(response as ByteArray)
                val status = if (inspection.success) QaStatus.SUCCESS else QaStatus.FAILURE
                ExecutionRecord(
                    targetId = target.targetId,
                    module = target.module,
                    functionName = target.functionName,
                    responseKind = target.responseKind,
                    status = status,
                    httpStatus = 200,
                    message = if (inspection.success) "PDF response validated." else "PDF response was not valid.",
                    request = request,
                    responseSummary = inspection.summary,
                    rawResponse = response,
                )
            }
        }
    } catch (error: AstrologyAPIException) {
        val exchange = collector.last()
        val request = exchange?.let { buildRequestSnapshot(target, it) }
        val status = if (error.status == 402 || error.status == 403) QaStatus.NOT_TESTABLE else QaStatus.FAILURE
        ExecutionRecord(
            targetId = target.targetId,
            module = target.module,
            functionName = target.functionName,
            responseKind = target.responseKind,
            status = status,
            httpStatus = error.status,
            message = error.message ?: error::class.simpleName.orEmpty(),
            errorName = error::class.simpleName,
            request = request,
            responseSummary = error.body?.toString(),
            rawResponse = null,
        )
    } catch (error: Throwable) {
        val exchange = collector.last()
        val request = exchange?.let { buildRequestSnapshot(target, it) }
        ExecutionRecord(
            targetId = target.targetId,
            module = target.module,
            functionName = target.functionName,
            responseKind = target.responseKind,
            status = QaStatus.FAILURE,
            httpStatus = null,
            message = error.message ?: error::class.simpleName.orEmpty(),
            errorName = error::class.simpleName,
            request = request,
            responseSummary = null,
            rawResponse = null,
        )
    }
}

private suspend fun compareAgainstDirect(
    target: InvocationTarget,
    sdkRecord: ExecutionRecord,
    snapshot: RequestSnapshot,
    postmanInventory: List<PostmanEndpoint>,
    directClient: HttpClient,
    directCollector: ExchangeCollector,
    forcedPostmanMatch: PostmanEndpoint? = null,
): ComparisonRecord {
    val postmanMatch = forcedPostmanMatch ?: findPostmanMatch(snapshot, postmanInventory)
    val defaults = target.defaultBodyFields()
    val initialPlan = buildDirectPlan(snapshot, postmanMatch, defaults)
    val initialResult = executeDirectPlan(initialPlan, sdkRecord.responseKind, directClient, directCollector)
    val bodyDrift = postmanMatch != null && snapshot.bodyFields.keys != postmanMatch.enabledBodyKeys.toSet()

    val finalResult = if (
        (initialResult.status != QaStatus.SUCCESS || comparisonMismatchExists(sdkRecord, initialResult, sdkRecord.responseKind)) &&
        (postmanMatch == null || bodyDrift)
    ) {
        executeDirectPlan(
            DirectRequestPlan(
                endpoint = snapshot.endpoint,
                domain = snapshot.domain,
                encoding = snapshot.encoding,
                body = typedBodyFromSnapshot(snapshot),
                fallbackReason = when {
                    postmanMatch == null -> "No Postman match was found, so the direct call reused the SDK-captured endpoint and body."
                    else -> "The Postman body diverged from the SDK-captured request shape, so the direct call retried with the SDK-captured endpoint and body."
                },
            ),
            sdkRecord.responseKind,
            directClient,
            directCollector,
        )
    } else {
        initialResult
    }

    val diff = when (sdkRecord.responseKind) {
        QaResponseKind.JSON -> buildJsonDiff(
            sdkRecord.rawResponse?.let { JsonParser.parseString(gson.toJson(it)) } ?: JsonObject(),
            finalResult.jsonResponse ?: JsonObject(),
        )
        QaResponseKind.PDF -> emptyList()
    }

    val comparisonMessage = when (sdkRecord.responseKind) {
        QaResponseKind.JSON -> if (diff.isEmpty() && finalResult.status == QaStatus.SUCCESS) {
            "Normalized SDK and direct responses matched."
        } else if (finalResult.status == QaStatus.SUCCESS) {
            "SDK and direct responses differed after normalization."
        } else {
            finalResult.message
        }
        QaResponseKind.PDF -> if (finalResult.status == QaStatus.SUCCESS) {
            "Both SDK and direct PDF responses passed validation."
        } else {
            finalResult.message
        }
    }

    return ComparisonRecord(
        targetId = target.targetId,
        endpoint = snapshot.endpoint,
        status = if (diff.isEmpty() && finalResult.status == QaStatus.SUCCESS) QaStatus.SUCCESS else finalResult.status,
        message = comparisonMessage,
        fallbackReason = finalResult.plan.fallbackReason,
        sdkRequest = snapshot,
        directRequest = finalResult.snapshot,
        sdkResponse = sdkRecord.responseSummary,
        directResponse = finalResult.summary,
        diff = diff,
    )
}

private data class DirectRequestPlan(
    val endpoint: String,
    val domain: String,
    val encoding: String,
    val body: Map<String, Any?>,
    val fallbackReason: String? = null,
)

private data class DirectExecutionResult(
    val plan: DirectRequestPlan,
    val status: QaStatus,
    val message: String,
    val snapshot: RequestSnapshot? = null,
    val summary: String? = null,
    val jsonResponse: JsonElement? = null,
)

private fun buildDirectPlan(
    sdkSnapshot: RequestSnapshot,
    postmanMatch: PostmanEndpoint?,
    defaults: Map<String, String>,
): DirectRequestPlan {
    if (postmanMatch == null) {
        return DirectRequestPlan(
            endpoint = sdkSnapshot.endpoint,
            domain = sdkSnapshot.domain,
            encoding = sdkSnapshot.encoding,
            body = typedBodyFromSnapshot(sdkSnapshot),
            fallbackReason = "No Postman match was found, so the direct call reused the SDK-captured endpoint and body.",
        )
    }

    val body = linkedMapOf<String, Any?>()
    postmanMatch.bodyParameters
        .filterNot { it.disabled }
        .forEach { parameter ->
            val value = sdkSnapshot.bodyFields[parameter.key] ?: defaults[parameter.key] ?: parameter.value
            if (value != null) {
                body[parameter.key] = value
            }
        }

    return DirectRequestPlan(
        endpoint = materializePostmanEndpoint(postmanMatch.endpoint, sdkSnapshot.pathValues),
        domain = postmanMatch.domain,
        encoding = postmanMatch.bodyMode ?: sdkSnapshot.encoding,
        body = body,
    )
}

private suspend fun executeDirectPlan(
    plan: DirectRequestPlan,
    responseKind: QaResponseKind,
    directClient: HttpClient,
    directCollector: ExchangeCollector,
): DirectExecutionResult {
    return try {
        directCollector.clear()
        when (responseKind) {
            QaResponseKind.JSON -> {
                val response = directClient.postElement(
                    endpoint = plan.endpoint,
                    body = plan.body,
                    language = null,
                    encoding = if (plan.encoding == "json") RequestEncoding.JSON else RequestEncoding.FORM_URLENCODED,
                    domain = if (plan.domain == "pdf") ApiDomain.PDF else ApiDomain.JSON,
                )
                val snapshot = directCollector.last()?.let {
                    buildRequestSnapshot(
                        target = null,
                        exchange = it,
                        endpointOverride = plan.endpoint,
                    )
                }
                DirectExecutionResult(
                    plan = plan,
                    status = QaStatus.SUCCESS,
                    message = "Direct call succeeded.",
                    snapshot = snapshot,
                    summary = summarizeJson(response),
                    jsonResponse = response,
                )
            }
            QaResponseKind.PDF -> {
                val response = directClient.postPdf(plan.endpoint, plan.body)
                val snapshot = directCollector.last()?.let {
                    buildRequestSnapshot(
                        target = null,
                        exchange = it,
                        endpointOverride = plan.endpoint,
                    )
                }
                val inspection = inspectPdfResponse(response)
                DirectExecutionResult(
                    plan = plan,
                    status = if (inspection.success) QaStatus.SUCCESS else QaStatus.FAILURE,
                    message = if (inspection.success) "Direct PDF response validated." else "Direct PDF response was not valid.",
                    snapshot = snapshot,
                    summary = inspection.summary,
                    jsonResponse = inspection.jsonResponse,
                )
            }
        }
    } catch (error: AstrologyAPIException) {
        val snapshot = directCollector.last()?.let {
            buildRequestSnapshot(
                target = null,
                exchange = it,
                endpointOverride = plan.endpoint,
            )
        }
        DirectExecutionResult(
            plan = plan,
            status = if (error.status == 402 || error.status == 403) QaStatus.NOT_TESTABLE else QaStatus.FAILURE,
            message = error.message ?: error::class.simpleName.orEmpty(),
            snapshot = snapshot,
            summary = error.body?.toString(),
        )
    }
}

private data class PdfInspection(
    val success: Boolean,
    val summary: String,
    val jsonResponse: JsonElement? = null,
)

private fun inspectPdfResponse(bytes: ByteArray): PdfInspection {
    if (bytes.isNotEmpty() && bytes.size >= 4 && bytes[0] == 0x25.toByte() && bytes[1] == 0x50.toByte()) {
        return PdfInspection(success = true, summary = "Valid binary PDF response (${bytes.size} bytes).")
    }

    val text = runCatching { bytes.toString(Charsets.UTF_8) }.getOrNull()?.trim().orEmpty()
    if (text.startsWith("{") || text.startsWith("[")) {
        val json = JsonParser.parseString(text)
        val pdfUrl = findStringAtKey(json, "pdf_url")
        if (!pdfUrl.isNullOrBlank()) {
            return PdfInspection(success = true, summary = "Successful JSON PDF response with pdf_url.", jsonResponse = json)
        }
        return PdfInspection(success = false, summary = "JSON response did not contain pdf_url.", jsonResponse = json)
    }

    return PdfInspection(success = false, summary = "Response was neither a binary PDF nor a JSON payload with pdf_url.")
}

private fun findStringAtKey(element: JsonElement, key: String): String? = when {
    element.isJsonObject -> {
        val obj = element.asJsonObject
        obj[key]?.takeIf { it.isJsonPrimitive }?.asString
            ?: obj.entrySet().firstNotNullOfOrNull { (_, value) -> findStringAtKey(value, key) }
    }
    element.isJsonArray -> element.asJsonArray.firstNotNullOfOrNull { findStringAtKey(it, key) }
    else -> null
}

private fun buildRequestSnapshot(
    target: InvocationTarget?,
    exchange: CapturedHttpExchange,
    endpointOverride: String? = null,
): RequestSnapshot {
    val endpoint = endpointOverride ?: exchange.request.endpoint
    val bodyFields = when (exchange.request.encoding) {
        RequestEncoding.FORM_URLENCODED -> parseFormBody(exchange.request.body)
        RequestEncoding.JSON -> parseJsonBody(exchange.request.body)
    }
    val pathValues = mutableListOf<String>()
    val segments = endpoint.trim('/').split('/').toMutableList()
    target?.argumentSummary
        ?.filterKeys { it != "language" }
        ?.forEach { (name, value) ->
            val index = segments.indexOf(value)
            if (index >= 0) {
                pathValues += value
                segments[index] = "<$name>"
            }
        }

    val normalizedEndpoint = segments.joinToString("/")
    return RequestSnapshot(
        endpoint = endpoint,
        normalizedEndpoint = normalizedEndpoint,
        canonicalEndpoint = canonicalizeEndpoint(normalizedEndpoint),
        domain = exchange.request.domain.name.lowercase(),
        encoding = when (exchange.request.encoding) {
            RequestEncoding.JSON -> "json"
            RequestEncoding.FORM_URLENCODED -> "urlencoded"
        },
        authStyle = when {
            exchange.request.headers.containsKey("x-astrologyapi-key") -> "header"
            exchange.request.headers["Authorization"]?.startsWith("Basic ") == true -> "basic"
            else -> "unknown"
        },
        headers = exchange.request.headers,
        body = exchange.request.body,
        bodyFields = bodyFields,
        pathValues = pathValues,
    )
}

private fun parseFormBody(body: String?): Map<String, String> {
    if (body.isNullOrBlank()) return emptyMap()
    return body.split("&")
        .filter { it.isNotBlank() }
        .associate { pair ->
            val pieces = pair.split("=", limit = 2)
            val key = URLDecoder.decode(pieces[0], StandardCharsets.UTF_8.name())
            val value = URLDecoder.decode(pieces.getOrElse(1) { "" }, StandardCharsets.UTF_8.name())
            key to value
        }
}

private fun parseJsonBody(body: String?): Map<String, String> {
    if (body.isNullOrBlank()) return emptyMap()
    val element = JsonParser.parseString(body)
    if (!element.isJsonObject) return emptyMap()
    return element.asJsonObject.entrySet().associate { (key, value) -> key to jsonScalarString(value) }
}

private fun typedBodyFromSnapshot(snapshot: RequestSnapshot): Map<String, Any?> = when (snapshot.encoding) {
    "json" -> snapshot.body?.let { jsonObjectToTypedMap(JsonParser.parseString(it)) } ?: emptyMap()
    else -> snapshot.bodyFields
}

private fun jsonObjectToTypedMap(element: JsonElement): Map<String, Any?> {
    if (!element.isJsonObject) return emptyMap()
    return element.asJsonObject.entrySet().associate { (key, value) ->
        key to jsonElementToTypedValue(value)
    }
}

private fun jsonElementToTypedValue(element: JsonElement): Any? = when {
    element.isJsonNull -> null
    element.isJsonPrimitive -> {
        val primitive = element.asJsonPrimitive
        when {
            primitive.isBoolean -> primitive.asBoolean
            primitive.isNumber -> primitive.asNumber
            else -> primitive.asString
        }
    }
    element.isJsonArray -> element.asJsonArray.map { jsonElementToTypedValue(it) }
    element.isJsonObject -> jsonObjectToTypedMap(element)
    else -> element.toString()
}

private fun jsonScalarString(value: JsonElement): String = when {
    value.isJsonNull -> ""
    value.isJsonPrimitive -> value.asJsonPrimitive.run {
        when {
            isBoolean -> asBoolean.toString()
            isNumber -> asNumber.toString()
            else -> asString
        }
    }
    else -> gson.toJson(value)
}

private fun canonicalizeEndpoint(endpoint: String): String =
    endpoint.split("/")
        .joinToString("/") { segment ->
            if ((segment.startsWith("<") && segment.endsWith(">")) || segment.startsWith(":")) "*" else segment
        }

private fun materializePostmanEndpoint(endpoint: String, pathValues: List<String>): String {
    val iterator = pathValues.iterator()
    return endpoint.split("/").joinToString("/") { segment ->
        if (segment.startsWith(":") && iterator.hasNext()) iterator.next() else segment
    }
}

private fun summarizeJson(element: JsonElement): String {
    val normalized = normalizeJson(element)
    return gson.toJson(normalized)
}

private fun normalizeJson(element: JsonElement, path: List<String> = emptyList()): JsonElement = when {
    element.isJsonObject -> {
        val normalized = JsonObject()
        element.asJsonObject.entrySet()
            .sortedBy { it.key }
            .forEach { (key, value) ->
                normalized.add(key, normalizeJsonValue(key, value, path + key))
            }
        normalized
    }
    element.isJsonArray -> {
        val normalized = JsonArray()
        element.asJsonArray.forEachIndexed { index, value ->
            normalized.add(normalizeJson(value, path + index.toString()))
        }
        normalized
    }
    else -> element
}

private fun normalizeJsonValue(key: String, value: JsonElement, path: List<String>): JsonElement {
    if (key == "chart_url" && value.isJsonPrimitive && value.asJsonPrimitive.isString) {
        val stripped = value.asString.substringBefore("?")
        return JsonParser.parseString(gson.toJson(stripped))
    }
    return normalizeJson(value, path)
}

private fun buildJsonDiff(left: JsonElement, right: JsonElement): List<String> {
    val differences = mutableListOf<String>()
    compareJson(path = "$", left = normalizeJson(left), right = normalizeJson(right), differences = differences)
    return differences
}

private fun compareJson(path: String, left: JsonElement, right: JsonElement, differences: MutableList<String>) {
    when {
        left.isJsonObject && right.isJsonObject -> {
            val keys = (left.asJsonObject.keySet() + right.asJsonObject.keySet()).sorted()
            keys.forEach { key ->
                val leftValue = left.asJsonObject.get(key)
                val rightValue = right.asJsonObject.get(key)
                when {
                    leftValue == null -> differences += "$path.$key missing from SDK response"
                    rightValue == null -> differences += "$path.$key missing from direct response"
                    else -> compareJson("$path.$key", leftValue, rightValue, differences)
                }
            }
        }
        left.isJsonArray && right.isJsonArray -> {
            val max = maxOf(left.asJsonArray.size(), right.asJsonArray.size())
            for (index in 0 until max) {
                val leftValue = left.asJsonArray.getOrNull(index)
                val rightValue = right.asJsonArray.getOrNull(index)
                when {
                    leftValue == null -> differences += "$path[$index] missing from SDK response"
                    rightValue == null -> differences += "$path[$index] missing from direct response"
                    else -> compareJson("$path[$index]", leftValue, rightValue, differences)
                }
            }
        }
        left != right -> differences += "$path differs: SDK=${left} DIRECT=${right}"
    }
}

private fun comparisonMismatchExists(
    sdkRecord: ExecutionRecord,
    directResult: DirectExecutionResult,
    responseKind: QaResponseKind,
): Boolean = when (responseKind) {
    QaResponseKind.JSON -> {
        val sdkJson = sdkRecord.rawResponse?.let { JsonParser.parseString(gson.toJson(it)) } ?: JsonObject()
        val directJson = directResult.jsonResponse ?: JsonObject()
        buildJsonDiff(sdkJson, directJson).isNotEmpty()
    }
    QaResponseKind.PDF -> directResult.status != QaStatus.SUCCESS
}

private fun findPostmanMatch(snapshot: RequestSnapshot, postmanInventory: List<PostmanEndpoint>): PostmanEndpoint? =
    postmanInventory.firstOrNull {
        it.normalizedEndpoint == snapshot.normalizedEndpoint && it.domain == snapshot.domain
    } ?: postmanInventory.firstOrNull {
        it.canonicalEndpoint == snapshot.canonicalEndpoint && it.domain == snapshot.domain
    }

private fun matchSdkToPostman(
    sdkInventory: List<SdkInventoryEntry>,
    postmanInventory: List<PostmanEndpoint>,
): Map<String, PostmanEndpoint?> = sdkInventory.associate { entry ->
    val match = postmanInventory.firstOrNull {
        it.normalizedEndpoint == entry.normalizedEndpoint && it.domain == entry.domain
    } ?: postmanInventory.firstOrNull {
        it.canonicalEndpoint == entry.canonicalEndpoint && it.domain == entry.domain
    }
    entry.targetId to match
}

private fun JsonArray.getOrNull(index: Int): JsonElement? =
    if (index in 0 until size()) get(index) else null

private fun buildCoverageReport(
    sdkInventory: List<SdkInventoryEntry>,
    postmanInventory: List<PostmanEndpoint>,
): String {
    val matched = matchSdkToPostman(sdkInventory, postmanInventory)
    val matchedPostman = matched.values.filterNotNull().toSet()
    val sdkOnly = sdkInventory.filter { matched[it.targetId] == null }
    val postmanOnly = postmanInventory.filter { endpoint -> matchedPostman.none { it.normalizedEndpoint == endpoint.normalizedEndpoint } }

    return buildString {
        appendLine("# Missing APIs")
        appendLine()
        appendLine("## Postman endpoints missing from the Kotlin SDK")
        appendLine()
        if (postmanOnly.isEmpty()) {
            appendLine("None.")
        } else {
            postmanOnly.forEach { appendLine("- `${it.normalizedEndpoint}` (`${it.domain}`)") }
        }
        appendLine()
        appendLine("## Kotlin SDK endpoints absent from Postman")
        appendLine()
        if (sdkOnly.isEmpty()) {
            appendLine("None.")
        } else {
            sdkOnly.forEach { appendLine("- `${it.targetId}` -> `${it.normalizedEndpoint}` (`${it.domain}`)") }
        }
    }
}

private fun buildParameterMismatchReport(
    sdkInventory: List<SdkInventoryEntry>,
    postmanInventory: List<PostmanEndpoint>,
): String {
    val postmanByCanonical = postmanInventory.groupBy { "${it.domain}:${it.canonicalEndpoint}" }
    val mismatches = sdkInventory.mapNotNull { entry ->
        val postman = postmanByCanonical["${entry.domain}:${entry.canonicalEndpoint}"]?.firstOrNull() ?: return@mapNotNull null
        val missingSdkParams = postman.enabledBodyKeys.toSet() - entry.bodyFields.toSet()
        val extraSdkParams = entry.bodyFields.toSet() - postman.enabledBodyKeys.toSet()
        val notes = mutableListOf<String>()
        if (entry.requestEncoding != (postman.bodyMode ?: "unknown")) {
            notes += "encoding `${entry.requestEncoding}` vs Postman `${postman.bodyMode}`"
        }
        if (entry.normalizedEndpoint != postman.normalizedEndpoint) {
            notes += "endpoint `${entry.normalizedEndpoint}` vs Postman `${postman.normalizedEndpoint}`"
        }
        if (missingSdkParams.isEmpty() && extraSdkParams.isEmpty() && notes.isEmpty()) {
            return@mapNotNull null
        }
        Triple(entry, missingSdkParams, extraSdkParams) to notes
    }

    return buildString {
        appendLine("# Parameter Mismatches")
        appendLine()
        if (mismatches.isEmpty()) {
            appendLine("No parameter mismatches were found between the current Kotlin SDK inventory and the Postman inventory.")
            return@buildString
        }
        mismatches.forEach { (payload, notes) ->
            val (entry, missingSdkParams, extraSdkParams) = payload
            appendLine("## `${entry.targetId}`")
            appendLine()
            appendLine("- SDK endpoint: `${entry.normalizedEndpoint}`")
            val postman = postmanByCanonical["${entry.domain}:${entry.canonicalEndpoint}"]!!.first()
            appendLine("- Postman endpoint: `${postman.normalizedEndpoint}`")
            if (missingSdkParams.isNotEmpty()) {
                appendLine("- Missing SDK params: `${missingSdkParams.sorted().joinToString(", ")}`")
            }
            if (extraSdkParams.isNotEmpty()) {
                appendLine("- Extra SDK params: `${extraSdkParams.sorted().joinToString(", ")}`")
            }
            notes.forEach { appendLine("- Note: $it") }
            appendLine()
        }
    }
}

private fun buildModuleCatalog(
    sdkInventory: List<SdkInventoryEntry>,
    executionRecords: List<ExecutionRecord>,
): String {
    val executionsByTarget = executionRecords.associateBy { it.targetId }
    return buildString {
        appendLine("# Kotlin SDK Modules")
        appendLine()
        sdkInventory.groupBy { it.module }
            .toSortedMap()
            .forEach { (module, entries) ->
                appendLine("## `$module`")
                appendLine()
                entries.sortedBy { it.functionName }.forEach { entry ->
                    appendLine("### `${entry.functionName}`")
                    appendLine()
                    appendLine("- Target: `${entry.targetId}`")
                    appendLine("- Endpoint: `${entry.normalizedEndpoint}`")
                    appendLine("- Domain: `${entry.domain}`")
                    appendLine("- Encoding: `${entry.requestEncoding}`")
                    appendLine("- Body fields: `${entry.bodyFields.joinToString(", ")}`")
                    appendLine("- Auth style: `${entry.authStyle}`")
                    appendLine("- Arguments: `${entry.arguments}`")
                    executionsByTarget[entry.targetId]?.responseSummary?.let { response ->
                        appendLine("- Latest response summary: `${response.take(200)}`")
                    }
                    appendLine()
                }
            }
    }
}

private fun buildFailingReport(records: List<ExecutionRecord>): String {
    val failures = records.filter { it.status == QaStatus.FAILURE }
    if (failures.isEmpty()) {
        return "# Failing APIs\n\nNo SDK failures were recorded in the latest deterministic sweep.\n"
    }

    val groups = failures.groupBy {
        "${it.httpStatus ?: "network"} | ${it.errorName ?: "UnknownError"} | ${it.message.lineSequence().firstOrNull().orEmpty()}"
    }

    return buildString {
        appendLine("# Failing APIs")
        appendLine()
        appendLine("## Error Summary")
        appendLine()
        groups.forEach { (signature, entries) ->
            appendLine("- `${signature}` -> ${entries.size} API(s)")
        }
        appendLine()
        groups.forEach { (signature, entries) ->
            appendLine("## `${signature}`")
            appendLine()
            entries.forEach { entry ->
                appendLine("### `${entry.targetId}`")
                appendLine()
                appendLine("- Message: ${entry.message}")
                entry.request?.let {
                    appendLine("- Endpoint: `${it.endpoint}`")
                    appendLine("- Body: `${it.body}`")
                }
                entry.responseSummary?.let { appendLine("- Response: `${it.take(300)}`") }
                appendLine()
            }
        }
    }
}

private fun buildNotTestableReport(records: List<ExecutionRecord>): String {
    val entries = records.filter { it.status == QaStatus.NOT_TESTABLE }
    return buildString {
        appendLine("# Not Testable With Current Plan")
        appendLine()
        if (entries.isEmpty()) {
            appendLine("No `402` or `403` plan restrictions were recorded in the latest deterministic sweep.")
        } else {
            entries.forEach { entry ->
                appendLine("## `${entry.targetId}`")
                appendLine()
                appendLine("- Status: `${entry.httpStatus}`")
                appendLine("- Message: ${entry.message}")
                entry.request?.let {
                    appendLine("- Endpoint: `${it.endpoint}`")
                    appendLine("- Body: `${it.body}`")
                }
                appendLine()
            }
        }
    }
}

private fun buildComparisonReport(records: List<ComparisonRecord>): String {
    val mismatches = records.filter { it.status != QaStatus.SUCCESS }
    return buildString {
        appendLine("# Inconsistent Responses")
        appendLine()
        if (mismatches.isEmpty()) {
            appendLine("No SDK vs direct API inconsistencies were recorded in the latest comparison run.")
        } else {
            mismatches.forEach { record ->
                appendLine("## `${record.targetId}`")
                appendLine()
                appendLine("- Endpoint: `${record.endpoint}`")
                appendLine("- Status: `${record.status.name.lowercase()}`")
                appendLine("- Message: ${record.message}")
                record.fallbackReason?.let { appendLine("- Fallback: $it") }
                record.diff.forEach { appendLine("- Diff: $it") }
                appendLine()
            }
        }
    }
}

private fun loadCredentials(): QaCredentials? {
    val envUserId = System.getenv("ASTROLOGYAPI_USER_ID")
    val envApiKey = System.getenv("ASTROLOGYAPI_API_KEY")
    if (!envApiKey.isNullOrBlank()) {
        return QaCredentials(envUserId, envApiKey)
    }

    listOf(repoRoot.resolve(".env.local"), repoRoot.resolve(".env"))
        .firstOrNull { it.exists() }
        ?.let { envPath ->
            val values = parseEnvFile(envPath.readText())
            val apiKey = values["ASTROLOGYAPI_API_KEY"] ?: return@let null
            return QaCredentials(values["ASTROLOGYAPI_USER_ID"], apiKey)
        }

    return null
}

private fun parseEnvFile(contents: String): Map<String, String> = contents.lineSequence()
    .map { it.trim() }
    .filter { it.isNotBlank() && !it.startsWith("#") && it.contains("=") }
    .associate { line ->
        val (key, value) = line.split("=", limit = 2)
        key.trim() to value.trim().removeSurrounding("\"")
    }

private data class QaCredentials(
    val userId: String?,
    val apiKey: String,
)

private fun <T> readJson(path: Path, type: java.lang.reflect.Type): T =
    gson.fromJson(path.readText(), type)

private fun Path.writeText(content: String) {
    Files.createDirectories(parent)
    this.writeText(content, StandardCharsets.UTF_8)
}

private fun splitName(value: String?): Pair<String?, String?> {
    val name = value?.trim().orEmpty()
    if (name.isBlank()) return null to null
    val parts = name.split(Regex("\\s+"), limit = 2)
    return parts[0] to parts.getOrNull(1)
}

fun mainCatalog() = runBlocking {
    val inventory = QaHarness.buildSdkInventory()
    QaHarness.writeCatalogArtifacts(inventory)
    println("Generated Kotlin QA catalog artifacts in ${catalogDir.toAbsolutePath()} and ${resultsDir.toAbsolutePath()}.")
}

fun mainSdk() = runBlocking {
    val inventory = QaHarness.buildSdkInventory()
    val records = QaHarness.runDeterministicSweep()
    QaHarness.writeCatalogArtifacts(inventory, executionRecords = records)
    println("Deterministic Kotlin QA sweep complete. Results written under ${resultsDir.toAbsolutePath()}.")
}

fun mainCompare() = runBlocking {
    val records = QaHarness.runRandomizedCompare()
    println("Randomized Kotlin SDK-vs-direct comparison complete. ${records.size} comparison records evaluated.")
}

fun mainSingle() = runBlocking {
    println(QaHarness.runSingleTarget())
}
