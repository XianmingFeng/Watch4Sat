package com.xianming.watch4sat.wear.map

import com.xianming.watch4sat.data.network.NetworkClientIdentity

object OsmUserAgent {
    fun forVersion(versionName: String): String {
        return NetworkClientIdentity(
            versionName = versionName,
            applicationId = ApplicationId
        ).userAgent
    }

    private const val ApplicationId = "com.xianming.watch4sat"
}
