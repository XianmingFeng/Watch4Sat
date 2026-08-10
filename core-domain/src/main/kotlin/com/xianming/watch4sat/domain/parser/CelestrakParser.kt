/*
 * Look4Sat. Amateur radio satellite tracker and pass predictor.
 * Copyright (C) 2019-2026 Arty Bishop and contributors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 *
 * CelesTrak CSV/TLE field extraction adapted for Watch4Sat from Look4Sat
 * DataParser.kt.
 */
package com.xianming.watch4sat.domain.parser

import com.xianming.watch4sat.domain.model.OrbitalData
import com.xianming.watch4sat.domain.model.SatelliteRecord
import java.time.LocalDateTime

object CelestrakParser {

    fun parseCsv(text: String): List<SatelliteRecord> {
        return parseCsvResult(text).records
    }

    fun parseCsvResult(text: String): FeedParseResult<SatelliteRecord> {
        val lines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.isEmpty()) {
            return FeedParseResult(
                records = emptyList(),
                inputRecordCount = 0,
                syntaxErrors = listOf("Missing CSV header.")
            )
        }

        val parsedHeader = parseCsvLine(lines.first())
        if (parsedHeader.hasUnclosedQuote) {
            return FeedParseResult(
                records = emptyList(),
                inputRecordCount = (lines.size - 1).coerceAtLeast(0),
                syntaxErrors = listOf("CSV header contains an unclosed quote.")
            )
        }
        val headers = parsedHeader.values
        val indexes = headers.withIndex().associate { it.value to it.index }
        val missingHeaders = RequiredCsvHeaders.filterNot(indexes::containsKey)
        if (missingHeaders.isNotEmpty()) {
            return FeedParseResult(
                records = emptyList(),
                inputRecordCount = (lines.size - 1).coerceAtLeast(0),
                syntaxErrors = listOf("Missing CSV headers: ${missingHeaders.joinToString()}.")
            )
        }

        val records = mutableListOf<SatelliteRecord>()
        val rejections = mutableListOf<FeedRecordRejection>()
        lines.drop(1).forEachIndexed { index, line ->
            val parsedLine = parseCsvLine(line)
            if (parsedLine.hasUnclosedQuote) {
                rejections += FeedRecordRejection(
                    recordNumber = index + 1,
                    reasons = listOf(FeedValidationIssue("record", "Contains an unclosed quote."))
                )
                return@forEachIndexed
            }
            val parsed = parseCsvRecord(parsedLine.values, indexes)
            if (parsed.record != null) {
                records += parsed.record
            } else {
                rejections += FeedRecordRejection(
                    recordNumber = index + 1,
                    reasons = parsed.issues.ifEmpty {
                        listOf(FeedValidationIssue("record", "Record is invalid."))
                    }
                )
            }
        }
        return FeedParseResult(
            records = records,
            inputRecordCount = lines.size - 1,
            rejections = rejections,
            duplicateStableIds = records
                .groupingBy { it.catalogNumber.toString() }
                .eachCount()
                .filterValues { count -> count > 1 }
                .keys
        )
    }

    fun parseTle(text: String): List<SatelliteRecord> {
        val lines = text.lineSequence()
            .map { it.trimEnd() }
            .filter { it.isNotBlank() }
            .toList()

        return lines.chunked(3)
            .filter { it.size == 3 && it[1].startsWith("1") && it[2].startsWith("2") }
            .mapNotNull { block ->
                runCatching {
                    val name = block[0].removePrefix("0 ").trim()
                    val line1 = block[1]
                    val line2 = block[2]
                    val catalogNumber = line1.substring(2, 7).trim().toInt()
                    val orbitalData = OrbitalData(
                        name = name,
                        catalogNumber = catalogNumber,
                        epoch = line1.substring(18, 32).toDouble(),
                        meanMotion = line2.substring(52, 63).toDouble(),
                        eccentricity = line2.substring(26, 33).toDouble() / 1e7,
                        inclinationDegrees = line2.substring(8, 16).toDouble(),
                        rightAscensionAscendingNodeDegrees = line2.substring(17, 25).toDouble(),
                        argumentOfPerigeeDegrees = line2.substring(34, 42).toDouble(),
                        meanAnomalyDegrees = line2.substring(43, 51).toDouble(),
                        bstar = parseTleExponent(line1.substring(53, 61)),
                        meanMotionDot = line1.substring(33, 43).trim().toDouble()
                    )
                    SatelliteRecord(
                        catalogNumber = catalogNumber,
                        displayName = name,
                        orbitalData = orbitalData
                    )
                }.getOrNull()
            }
    }

    private fun epochFromCelestrakTimestamp(timestamp: String): Double {
        LocalDateTime.parse(timestamp)
        val year = timestamp.substring(0, 4)
        val month = timestamp.substring(5, 7).toInt()
        val dayOfMonth = timestamp.substring(8, 10).toInt()
        val day = dayOfYear(year.toInt(), month, dayOfMonth).toString().padStart(3, '0')
        val hour = timestamp.substring(11, 13).toInt() * 3_600_000
        val minute = timestamp.substring(14, 16).toInt() * 60_000
        val second = timestamp.substring(17, 19).toInt() * 1_000
        val fraction = timestamp.substringAfter('.', "0").padEnd(6, '0').take(6).toInt() / 1000.0
        val dayFraction = ((hour + minute + second + fraction) / 86_400_000.0).toString().substring(1)
        return "${year.substring(2)}$day$dayFraction".toDouble()
    }

    private fun dayOfYear(year: Int, month: Int, dayOfMonth: Int): Int {
        val daysInMonth = intArrayOf(31, if (isLeapYear(year)) 29 else 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31)
        return daysInMonth.take(month - 1).sum() + dayOfMonth
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    }

    private fun parseTleExponent(field: String): Double {
        val trimmed = field.trim()
        if (trimmed.isEmpty()) return 0.0
        val exponentSignIndex = trimmed.indexOfLast { it == '+' || it == '-' }
        if (exponentSignIndex <= 0) return trimmed.toDouble()
        val mantissa = trimmed.substring(0, exponentSignIndex).toDouble() / 100_000.0
        val exponent = trimmed.substring(exponentSignIndex).toInt()
        return mantissa * Math.pow(10.0, exponent.toDouble())
    }

    private fun parseCsvLine(line: String): ParsedCsvLine {
        val values = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var index = 0
        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && inQuotes && line.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> inQuotes = !inQuotes
                char == ',' && !inQuotes -> {
                    values += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }
        values += current.toString()
        return ParsedCsvLine(values = values, hasUnclosedQuote = inQuotes)
    }

    private fun parseCsvRecord(
        values: List<String>,
        indexes: Map<String, Int>
    ): ParsedSatelliteRecord {
        val issues = mutableListOf<FeedValidationIssue>()

        fun value(field: String): String? {
            val index = indexes.getValue(field)
            val value = values.getOrNull(index)
            if (value == null) {
                issues += FeedValidationIssue(field, "Missing value.")
            }
            return value
        }

        fun requiredText(field: String): String? {
            val value = value(field)?.trim() ?: return null
            if (value.isBlank()) {
                issues += FeedValidationIssue(field, "Must not be blank.")
                return null
            }
            return value
        }

        fun integer(field: String): Int? {
            val raw = value(field)?.trim() ?: return null
            return raw.toIntOrNull() ?: run {
                issues += FeedValidationIssue(field, "Must be an integer.")
                null
            }
        }

        fun number(field: String): Double? {
            val raw = value(field)?.trim() ?: return null
            val number = raw.toDoubleOrNull()
            if (number == null || !number.isFinite()) {
                issues += FeedValidationIssue(field, "Must be a finite number.")
                return null
            }
            return number
        }

        val name = requiredText("OBJECT_NAME")
        val catalogNumber = integer("NORAD_CAT_ID")?.also { number ->
            if (number <= 0) issues += FeedValidationIssue("NORAD_CAT_ID", "Must be positive.")
        }
        val epoch = requiredText("EPOCH")?.let { timestamp ->
            runCatching { epochFromCelestrakTimestamp(timestamp) }.getOrElse {
                issues += FeedValidationIssue("EPOCH", "Must be a valid CelesTrak timestamp.")
                null
            }
        }?.also { value ->
            if (!value.isFinite() || value < 0.0) {
                issues += FeedValidationIssue("EPOCH", "Must be a finite non-negative epoch.")
            }
        }
        val meanMotion = number("MEAN_MOTION")?.also { value ->
            if (value <= 0.0) issues += FeedValidationIssue("MEAN_MOTION", "Must be positive.")
        }
        val eccentricity = number("ECCENTRICITY")?.also { value ->
            if (value < 0.0 || value >= 1.0) {
                issues += FeedValidationIssue("ECCENTRICITY", "Must be in [0, 1).")
            }
        }
        val inclination = number("INCLINATION")?.also { value ->
            if (value < 0.0 || value > 180.0) {
                issues += FeedValidationIssue("INCLINATION", "Must be in [0, 180].")
            }
        }
        val raan = boundedAngle("RA_OF_ASC_NODE", number("RA_OF_ASC_NODE"), issues)
        val argumentOfPerigee = boundedAngle(
            "ARG_OF_PERICENTER",
            number("ARG_OF_PERICENTER"),
            issues
        )
        val meanAnomaly = boundedAngle("MEAN_ANOMALY", number("MEAN_ANOMALY"), issues)
        val bstar = number("BSTAR")
        val meanMotionDot = number("MEAN_MOTION_DOT")
        val objectId = value("OBJECT_ID")?.trim()?.takeIf(String::isNotBlank)

        if (issues.isNotEmpty()) return ParsedSatelliteRecord(null, issues)
        return ParsedSatelliteRecord(
            record = SatelliteRecord(
                catalogNumber = requireNotNull(catalogNumber),
                displayName = requireNotNull(name),
                objectId = objectId,
                orbitalData = OrbitalData(
                    name = name,
                    catalogNumber = catalogNumber,
                    epoch = requireNotNull(epoch),
                    meanMotion = requireNotNull(meanMotion),
                    eccentricity = requireNotNull(eccentricity),
                    inclinationDegrees = requireNotNull(inclination),
                    rightAscensionAscendingNodeDegrees = requireNotNull(raan),
                    argumentOfPerigeeDegrees = requireNotNull(argumentOfPerigee),
                    meanAnomalyDegrees = requireNotNull(meanAnomaly),
                    bstar = requireNotNull(bstar),
                    meanMotionDot = requireNotNull(meanMotionDot)
                )
            ),
            issues = emptyList()
        )
    }

    private fun boundedAngle(
        field: String,
        value: Double?,
        issues: MutableList<FeedValidationIssue>
    ): Double? {
        if (value != null && (value < 0.0 || value > 360.0)) {
            issues += FeedValidationIssue(field, "Must be in [0, 360].")
        }
        return value
    }

    private data class ParsedSatelliteRecord(
        val record: SatelliteRecord?,
        val issues: List<FeedValidationIssue>
    )

    private data class ParsedCsvLine(
        val values: List<String>,
        val hasUnclosedQuote: Boolean
    )

    private val RequiredCsvHeaders = listOf(
        "OBJECT_NAME",
        "OBJECT_ID",
        "EPOCH",
        "MEAN_MOTION",
        "ECCENTRICITY",
        "INCLINATION",
        "RA_OF_ASC_NODE",
        "ARG_OF_PERICENTER",
        "MEAN_ANOMALY",
        "NORAD_CAT_ID",
        "BSTAR",
        "MEAN_MOTION_DOT"
    )
}
