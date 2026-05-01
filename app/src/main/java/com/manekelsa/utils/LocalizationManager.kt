package com.manekelsa.utils

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import java.util.Locale

object LocalizationManager {
    fun setLanguage(languageCode: String) {
        val appLocale: LocaleListCompat = LocaleListCompat.forLanguageTags(languageCode)
        AppCompatDelegate.setApplicationLocales(appLocale)
    }

    fun getLanguage(): String {
        return AppCompatDelegate.getApplicationLocales()[0]?.language ?: Locale.getDefault().language
    }
}
