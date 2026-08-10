package com.xianming.watch4sat.wear.state

data class AboutInfoRow(
    val label: AboutInfoLabel,
    val value: AboutInfoValue
)

enum class AboutInfoLabel {
    Version,
    Package,
    App,
    Data,
    Maps,
    Location,
    License
}

sealed interface AboutInfoValue {
    data class Version(val versionName: String, val versionCode: Int) : AboutInfoValue
    data object Package : AboutInfoValue
    data object App : AboutInfoValue
    data object Data : AboutInfoValue
    data object Maps : AboutInfoValue
    data object Location : AboutInfoValue
    data object License : AboutInfoValue
}

object AboutPagePolicy {
    fun rowsFor(versionName: String, versionCode: Int): List<AboutInfoRow> {
        return listOf(
            AboutInfoRow(
                AboutInfoLabel.Version,
                AboutInfoValue.Version(versionName, versionCode)
            ),
            AboutInfoRow(AboutInfoLabel.Package, AboutInfoValue.Package),
            AboutInfoRow(AboutInfoLabel.App, AboutInfoValue.App),
            AboutInfoRow(AboutInfoLabel.Data, AboutInfoValue.Data),
            AboutInfoRow(AboutInfoLabel.Maps, AboutInfoValue.Maps),
            AboutInfoRow(AboutInfoLabel.Location, AboutInfoValue.Location),
            AboutInfoRow(AboutInfoLabel.License, AboutInfoValue.License)
        )
    }
}
