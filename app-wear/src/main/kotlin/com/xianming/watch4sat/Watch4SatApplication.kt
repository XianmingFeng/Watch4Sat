package com.xianming.watch4sat

import android.app.Application
import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList

class Watch4SatApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(EnglishLocaleContext.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val englishLocales = LocaleList(EnglishLocaleContext.locale)
            val localeManager = getSystemService(LocaleManager::class.java)
            if (localeManager.applicationLocales != englishLocales) {
                localeManager.applicationLocales = englishLocales
            }
        }
    }
}
