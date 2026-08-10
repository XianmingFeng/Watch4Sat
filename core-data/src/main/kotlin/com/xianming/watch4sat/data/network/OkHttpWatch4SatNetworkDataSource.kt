package com.xianming.watch4sat.data.network

import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

class OkHttpWatch4SatNetworkDataSource(
    private val client: OkHttpClient,
    private val userAgent: String,
    private val celestrakAmateurCsvUrl: String = CELESTRAK_AMATEUR_CSV_URL,
    private val satnogsActiveTransmittersUrl: String = SATNOGS_ACTIVE_TRANSMITTERS_URL
) : Watch4SatNetworkDataSource {

    override suspend fun fetchCelestrakAmateur(): String {
        return fetch(celestrakAmateurCsvUrl)
    }

    override suspend fun fetchSatnogsActiveTransmitters(): String {
        return fetch(satnogsActiveTransmittersUrl)
    }

    private suspend fun fetch(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("GET $url failed with HTTP ${response.code}.")
            }
            response.body.string()
        }
    }
}
