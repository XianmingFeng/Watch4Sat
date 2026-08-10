package com.xianming.watch4sat.wear.legal

import android.content.Context
import androidx.annotation.StringRes
import com.xianming.watch4sat.R
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

enum class LegalDocument(
    val id: String,
    val assetPath: String,
    @param:StringRes val titleRes: Int
) {
    PrivacyPolicy(
        id = "privacy",
        assetPath = "legal/PRIVACY_POLICY.txt",
        titleRes = R.string.legal_privacy_policy
    ),
    Notices(
        id = "notices",
        assetPath = "legal/NOTICES.txt",
        titleRes = R.string.legal_notices
    ),
    ProjectLicense(
        id = "gpl-3.0",
        assetPath = "legal/GPL-3.0.txt",
        titleRes = R.string.legal_project_license
    ),
    FontLicense(
        id = "ofl-1.1",
        assetPath = "legal/OFL-1.1-GOOGLE-SANS-FLEX.txt",
        titleRes = R.string.legal_font_license
    ),
    OpenStreetMapLicense(
        id = "odbl-1.0",
        assetPath = "legal/ODBL-1.0.txt",
        titleRes = R.string.legal_openstreetmap_license
    ),
    SatNogsLicense(
        id = "cc-by-sa-4.0",
        assetPath = "legal/CC-BY-SA-4.0.txt",
        titleRes = R.string.legal_satnogs_license
    ),
    DependencyInventory(
        id = "dependency-inventory",
        assetPath = "legal/DEPENDENCY-LICENSES.md",
        titleRes = R.string.legal_dependency_inventory
    ),
    ApacheLicense(
        id = "apache-2.0",
        assetPath = "legal/APACHE-2.0.txt",
        titleRes = R.string.legal_apache_license
    ),
    BsdLicense(
        id = "bsd-3-clause",
        assetPath = "legal/BSD-3-CLAUSE.txt",
        titleRes = R.string.legal_bsd_license
    );

    companion object {
        fun fromId(id: String?): LegalDocument? = entries.firstOrNull { it.id == id }
    }
}

object LegalDocumentCatalog {
    val noticesDocuments: List<LegalDocument> = listOf(
        LegalDocument.Notices,
        LegalDocument.ProjectLicense,
        LegalDocument.FontLicense,
        LegalDocument.OpenStreetMapLicense,
        LegalDocument.SatNogsLicense,
        LegalDocument.DependencyInventory,
        LegalDocument.ApacheLicense,
        LegalDocument.BsdLicense
    )
}

fun legalDocumentChunks(text: String, maxCharacters: Int = 1_200): List<String> {
    require(maxCharacters > 0)
    val paragraphs = text
        .trim()
        .split(Regex("""\n\s*\n"""))
        .map(String::trim)
        .filter(String::isNotEmpty)
    if (paragraphs.isEmpty()) return emptyList()

    val chunks = mutableListOf<String>()
    val current = StringBuilder()
    paragraphs.forEach { paragraph ->
        if (current.isNotEmpty() && current.length + paragraph.length + 2 > maxCharacters) {
            chunks += current.toString()
            current.clear()
        }
        if (paragraph.length <= maxCharacters) {
            if (current.isNotEmpty()) current.append("\n\n")
            current.append(paragraph)
        } else {
            if (current.isNotEmpty()) {
                chunks += current.toString()
                current.clear()
            }
            paragraph.chunked(maxCharacters).forEach(chunks::add)
        }
    }
    if (current.isNotEmpty()) chunks += current.toString()
    return chunks
}

class OfflineLegalDocumentLoader(
    private val readAsset: (String) -> String
) {
    fun read(document: LegalDocument): String {
        return readAsset(document.assetPath)
            .trim()
            .takeIf(String::isNotEmpty)
            ?: throw IOException("Empty packaged legal document: ${document.assetPath}")
    }

    companion object {
        fun forContext(context: Context): OfflineLegalDocumentLoader {
            val appContext = context.applicationContext
            return OfflineLegalDocumentLoader { assetPath ->
                appContext.assets.open(assetPath).bufferedReader(Charsets.UTF_8).use { it.readText() }
            }
        }
    }
}

suspend fun OfflineLegalDocumentLoader.readOffMainThread(
    document: LegalDocument
): Result<String> = withContext(Dispatchers.IO) {
    runCatching { read(document) }
}
