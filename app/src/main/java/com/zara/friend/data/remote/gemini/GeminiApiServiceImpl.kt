package com.zara.friend.data.remote.gemini

import com.zara.friend.core.common.DispatchersProvider
import com.zara.friend.core.common.runSuspendCatching
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.Content
import com.google.ai.client.generativeai.type.GenerationConfig
import com.google.ai.client.generativeai.type.TextPart
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Implementasi [GeminiApiService] via Google Generative AI SDK (API key).
 * Retry sederhana untuk kegagalan jaringan/transien.
 */
class GeminiApiServiceImpl @Inject constructor(
    private val apiKey: String,
    private val modelName: String = DEFAULT_MODEL,
    private val dispatchers: DispatchersProvider,
) : GeminiApiService {

    override suspend fun generateText(request: GeminiTextRequest): Result<GeminiTextResponse> =
        withContext(dispatchers.io) {
            runSuspendCatching {
                var lastError: Exception? = null
                repeat(MAX_RETRIES) { attempt ->
                    try {
                        val generationConfig = GenerationConfig.Builder().apply {
                            temperature = 0.7f
                        }.build()

                        val model = GenerativeModel(
                            modelName = modelName,
                            apiKey = apiKey,
                            systemInstruction = Content(
                                role = "user",
                                parts = listOf(TextPart(request.systemInstruction)),
                            ),
                            generationConfig = generationConfig,
                        )
                        val chat = model.startChat(
                            history = request.history.map { msg ->
                                Content(
                                    role = msg.role.name.lowercase(),
                                    parts = listOf(TextPart(msg.text)),
                                )
                            },
                        )
                        val response = chat.sendMessage(request.userText)
                        val text = response.text?.trim().orEmpty()
                        if (text.isBlank()) error("Respons Gemini kosong")
                        return@withContext Result.success(
                            GeminiTextResponse(text = text, finishReason = null),
                        )
                    } catch (e: Exception) {
                        lastError = e
                        if (attempt < MAX_RETRIES - 1) delay(RETRY_DELAY_MS)
                    }
                }
                throw lastError ?: IllegalStateException("Gagal memanggil Gemini")
            }
        }

    companion object {
        const val DEFAULT_MODEL = "gemini-2.0-flash"
        const val MAX_RETRIES = 2
        const val RETRY_DELAY_MS = 500L
    }
}
