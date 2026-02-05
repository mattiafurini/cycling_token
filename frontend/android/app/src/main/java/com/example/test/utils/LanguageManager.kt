package com.example.test.utils

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Language Manager for app localization
 * Handles saving/loading language preferences and applying them
 */
object LanguageManager {
    
    private const val PREF_NAME = "LanguagePreferences"
    private const val KEY_LANGUAGE = "selected_language"
    
    const val LANGUAGE_ENGLISH = "en"
    const val LANGUAGE_ITALIAN = "it"
    
    /**
     * Get available languages
     */
    fun getAvailableLanguages(): List<Pair<String, String>> {
        return listOf(
            LANGUAGE_ENGLISH to "English",
            LANGUAGE_ITALIAN to "Italiano"
        )
    }
    
    /**
     * Save selected language
     */
    fun saveLanguage(context: Context, languageCode: String) {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply()
    }
    
    /**
     * Get saved language (default: English)
     */
    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, LANGUAGE_ENGLISH) ?: LANGUAGE_ENGLISH
    }
    
    /**
     * Apply language to context
     * Call this in Activity.onCreate() or Application.onCreate()
     */
    fun applyLanguage(context: Context): Context {
        val languageCode = getSavedLanguage(context)
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        return context.createConfigurationContext(config)
    }
    
    /**
     * Set language and recreate activity
     */
    fun setLanguage(context: Context, languageCode: String) {
        saveLanguage(context, languageCode)
        
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }
}
