package com.xianming.watch4sat

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object EnglishLocaleContext {
    val locale: Locale = Locale.US

    fun wrap(context: Context): Context {
        val englishLocale = locale
        val currentConfiguration = context.resources.configuration
        if (
            currentConfiguration.locales.size() == 1 &&
            currentConfiguration.locales[0] == englishLocale
        ) {
            return context
        }
        return context.createConfigurationContext(
            Configuration(currentConfiguration).apply {
                setLocale(englishLocale)
                setLayoutDirection(englishLocale)
            }
        )
    }
}
