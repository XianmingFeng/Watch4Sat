package com.xianming.watch4sat.domain.parser

import com.xianming.watch4sat.domain.model.TransmitterRecord
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.decodeFromJsonElement

object SatnogsTransmitterParser {

    private val json = Json {
        ignoreUnknownKeys = true
    }

    fun parseActiveTransmitters(text: String): List<TransmitterRecord> {
        return parseActiveTransmittersResult(text).records
    }

    fun parseActiveTransmittersResult(text: String): FeedParseResult<TransmitterRecord> {
        val root = runCatching { json.parseToJsonElement(text) }.getOrElse {
            return FeedParseResult(
                records = emptyList(),
                inputRecordCount = 0,
                syntaxErrors = listOf("Invalid SatNOGS JSON.")
            )
        } as? JsonArray ?: return FeedParseResult(
            records = emptyList(),
            inputRecordCount = 0,
            syntaxErrors = listOf("SatNOGS response must be a JSON array.")
        )

        val records = mutableListOf<TransmitterRecord>()
        val rejections = mutableListOf<FeedRecordRejection>()
        val stableIds = mutableListOf<String>()
        var ignored = 0
        root.forEachIndexed { index, element ->
            val dto = runCatching {
                json.decodeFromJsonElement<SatnogsTransmitterDto>(element)
            }.getOrElse {
                rejections += FeedRecordRejection(
                    recordNumber = index + 1,
                    reasons = listOf(FeedValidationIssue("record", "Invalid transmitter JSON object."))
                )
                return@forEachIndexed
            }
            dto.uuid?.trim()?.takeIf(String::isNotBlank)?.let(stableIds::add)
            val parsed = dto.toValidatedRecord()
            when {
                parsed.issues.isNotEmpty() -> rejections += FeedRecordRejection(
                    recordNumber = index + 1,
                    reasons = parsed.issues
                )
                !parsed.active -> ignored++
                parsed.record != null -> records += parsed.record
            }
        }
        return FeedParseResult(
            records = records,
            inputRecordCount = root.size,
            ignoredRecordCount = ignored,
            rejections = rejections,
            duplicateStableIds = stableIds
                .groupingBy { it }
                .eachCount()
                .filterValues { count -> count > 1 }
                .keys
        )
    }

    @Serializable
    private data class SatnogsTransmitterDto(
        @SerialName("uuid") val uuid: String? = null,
        @SerialName("description") val description: String? = null,
        @SerialName("alive") val alive: Boolean = false,
        @SerialName("uplink_low") val uplinkLow: Long? = null,
        @SerialName("uplink_high") val uplinkHigh: Long? = null,
        @SerialName("downlink_low") val downlinkLow: Long? = null,
        @SerialName("downlink_high") val downlinkHigh: Long? = null,
        @SerialName("mode") val mode: String? = null,
        @SerialName("uplink_mode") val uplinkMode: String? = null,
        @SerialName("invert") val invert: Boolean = false,
        @SerialName("norad_cat_id") val noradCatId: Int? = null,
        @SerialName("status") val status: String = ""
    ) {
        fun toValidatedRecord(): ParsedTransmitterRecord {
            val issues = mutableListOf<FeedValidationIssue>()
            val validUuid = uuid?.trim()?.takeIf(String::isNotBlank) ?: run {
                issues += FeedValidationIssue("uuid", "Must not be blank.")
                null
            }
            val validDescription = description?.trim()?.takeIf(String::isNotBlank) ?: run {
                issues += FeedValidationIssue("description", "Must not be blank.")
                null
            }
            if (noradCatId != null && noradCatId <= 0) {
                issues += FeedValidationIssue("norad_cat_id", "Must be positive when present.")
            }
            validateFrequency("uplink_low", uplinkLow, issues)
            validateFrequency("uplink_high", uplinkHigh, issues)
            validateFrequency("downlink_low", downlinkLow, issues)
            validateFrequency("downlink_high", downlinkHigh, issues)
            if (uplinkLow != null && uplinkHigh != null && uplinkLow > uplinkHigh) {
                issues += FeedValidationIssue("uplink_high", "Must be greater than or equal to uplink_low.")
            }
            if (downlinkLow != null && downlinkHigh != null && downlinkLow > downlinkHigh) {
                issues += FeedValidationIssue(
                    "downlink_high",
                    "Must be greater than or equal to downlink_low."
                )
            }
            val active = status.trim().equals("active", ignoreCase = true)
            if (issues.isNotEmpty()) return ParsedTransmitterRecord(null, active, issues)
            return ParsedTransmitterRecord(
                record = TransmitterRecord(
                    uuid = requireNotNull(validUuid),
                    catalogNumber = noradCatId,
                    description = requireNotNull(validDescription),
                    isAlive = alive,
                    status = status,
                    downlinkLowHz = downlinkLow,
                    downlinkHighHz = downlinkHigh,
                    downlinkMode = mode,
                    uplinkLowHz = uplinkLow,
                    uplinkHighHz = uplinkHigh,
                    uplinkMode = uplinkMode,
                    isInverted = invert
                ),
                active = active,
                issues = emptyList()
            )
        }

        private fun validateFrequency(
            field: String,
            value: Long?,
            issues: MutableList<FeedValidationIssue>
        ) {
            if (value != null && value < 0L) {
                issues += FeedValidationIssue(field, "Must be non-negative when present.")
            }
        }
    }

    private data class ParsedTransmitterRecord(
        val record: TransmitterRecord?,
        val active: Boolean,
        val issues: List<FeedValidationIssue>
    )
}
