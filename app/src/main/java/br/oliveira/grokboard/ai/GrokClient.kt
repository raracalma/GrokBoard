package br.oliveira.grokboard.ai

import br.oliveira.grokboard.persona.Persona
import br.oliveira.grokboard.util.AppContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GrokClient(
    private val apiKey: String,
    private val model: String,
) {
    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    fun run(
        persona: Persona,
        action: GrokAction,
        fieldText: String,
        packageName: String?,
        extraScreenContext: String? = null,
    ): String {
        val app = AppContext.label(packageName)
        val user = buildString {
            appendLine("Ação: ${action.label}")
            appendLine("Instrução: ${action.instruction}")
            appendLine("App aberto: $app ($packageName)")
            if (AppContext.isX(packageName)) {
                appendLine("Contexto: o usuário está no X. Trate o texto como rascunho de post ou reply.")
            }
            if (!extraScreenContext.isNullOrBlank()) {
                appendLine("Texto visível na tela (post/thread):")
                appendLine(extraScreenContext.take(2500))
            }
            appendLine("Texto atual do campo:")
            appendLine(fieldText.ifBlank { "(vazio)" })
        }

        val body = JSONObject().apply {
            put("model", model)
            put("temperature", 0.7)
            put(
                "messages",
                JSONArray().put(
                    JSONObject().put("role", "system").put("content", persona.systemPrompt)
                ).put(
                    JSONObject().put("role", "user").put("content", user)
                )
            )
        }

        val req = Request.Builder()
            .url("https://api.x.ai/v1/chat/completions")
            .addHeader("Authorization", "Bearer $apiKey")
            .addHeader("Content-Type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        http.newCall(req).execute().use { res ->
            val raw = res.body?.string().orEmpty()
            if (!res.isSuccessful) {
                throw IllegalStateException("API ${res.code}: ${raw.take(300)}")
            }
            val json = JSONObject(raw)
            return json.getJSONArray("choices")
                .getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim()
                .trim('"')
        }
    }
}
