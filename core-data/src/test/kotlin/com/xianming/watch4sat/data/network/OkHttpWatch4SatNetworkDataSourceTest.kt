package com.xianming.watch4sat.data.network

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test

class OkHttpWatch4SatNetworkDataSourceTest {

    @Test
    fun `both feeds use injected current application identity`() = runTest {
        val userAgents = mutableListOf<String?>()
        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                userAgents += chain.request().header("User-Agent")
                Response.Builder()
                    .request(chain.request())
                    .protocol(Protocol.HTTP_1_1)
                    .code(200)
                    .message("OK")
                    .body("payload".toResponseBody())
                    .build()
            }
            .build()
        val identity = NetworkClientIdentity(
            versionName = "1.0.0-rc.1",
            applicationId = "com.xianming.watch4sat"
        )
        val source = OkHttpWatch4SatNetworkDataSource(
            client = client,
            userAgent = identity.userAgent,
            celestrakAmateurCsvUrl = "https://example.test/tle",
            satnogsActiveTransmittersUrl = "https://example.test/tx"
        )

        source.fetchCelestrakAmateur()
        source.fetchSatnogsActiveTransmitters()

        assertEquals(listOf(identity.userAgent, identity.userAgent), userAgents)
        assertEquals(
            "Watch4Sat/1.0.0-rc.1 " +
                "(com.xianming.watch4sat; " +
                "+https://github.com/XianmingFeng/Watch4Sat)",
            identity.userAgent
        )
    }
}
