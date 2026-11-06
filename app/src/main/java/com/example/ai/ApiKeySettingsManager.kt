package com.example.ai

import android.content.Context
import android.content.SharedPreferences

enum class AiProviderType(val displayName: String, val defaultModel: String) {
    GEMINI_STUDIO("Google Gemini / AI Studio (Free API Key)", "gemini-2.5-flash"),
    OPENAI_COMPATIBLE("OpenAI / Groq / DeepSeek (API Key)", "gpt-4o-mini"),
    FIREBASE_DEFAULT("Built-in Firebase AI Logic", "gemini-3.8-flash")
}

data class CustomAiConfig(
    val provider: AiProviderType = AiProviderType.GEMINI_STUDIO,
    val apiKey: String = "",
    val modelName: String = "gemini-2.5-flash",
    val customEndpoint: String = "https://api.openai.com/v1/chat/completions"
)

class ApiKeySettingsManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("vibecut_ai_key_prefs", Context.MODE_PRIVATE)

    fun loadConfig(): CustomAiConfig = getConfig()

    fun getConfig(): CustomAiConfig {
        val providerName = prefs.getString("provider", AiProviderType.GEMINI_STUDIO.name)
            ?: AiProviderType.GEMINI_STUDIO.name
        val provider = try {
            AiProviderType.valueOf(providerName)
        } catch (_: Exception) {
            AiProviderType.GEMINI_STUDIO
        }
        val key = prefs.getString("api_key", "") ?: ""
        val model = prefs.getString("model_name", provider.defaultModel) ?: provider.defaultModel
        val endpoint = prefs.getString("endpoint", "https://api.openai.com/v1/chat/completions")
            ?: "https://api.openai.com/v1/chat/completions"

        return CustomAiConfig(
            provider = provider,
            apiKey = key.trim(),
            modelName = model.trim().ifBlank { provider.defaultModel },
            customEndpoint = endpoint.trim()
        )
    }

    fun saveConfig(config: CustomAiConfig) {
        prefs.edit()
            .putString("provider", config.provider.name)
            .putString("api_key", config.apiKey.trim())
            .putString("model_name", config.modelName.trim())
            .putString("endpoint", config.customEndpoint.trim())
            .apply()
    }

    fun hasCustomKey(): Boolean {
        return getConfig().apiKey.isNotBlank()
    }
}
