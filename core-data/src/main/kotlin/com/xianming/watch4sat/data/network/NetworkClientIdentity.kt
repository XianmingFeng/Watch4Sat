package com.xianming.watch4sat.data.network

data class NetworkClientIdentity(
    val versionName: String,
    val applicationId: String,
    val contactUrl: String = PublicProjectUrl
) {
    init {
        require(versionName.isNotBlank()) { "Version name must not be blank." }
        require(applicationId.isNotBlank()) { "Application ID must not be blank." }
        require(contactUrl.startsWith("https://")) { "Contact URL must use HTTPS." }
    }

    val userAgent: String =
        "Watch4Sat/$versionName ($applicationId; +$contactUrl)"

    companion object {
        const val PublicProjectUrl: String =
            "https://github.com/XianmingFeng/Watch4Sat"
    }
}
