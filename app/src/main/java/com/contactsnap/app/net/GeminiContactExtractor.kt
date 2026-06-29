package com.contactsnap.app.net

import com.contactsnap.app.model.ParsedContact
import com.contactsnap.app.util.Phones
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/** Raised with a user-facing message when extraction fails. */
class GeminiException(message: String) : Exception(message)

/**
 * Sends a card image to Google's Gemini vision model and gets back structured
 * contact fields. Uses the free-tier `generateContent` endpoint with a response
 * schema so the model returns strict JSON.
 */
class GeminiContactExtractor(
    private val model: String = "gemini-2.5-flash"
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = "application/json".toMediaType()

    fun extract(base64Jpeg: String, apiKey: String): ParsedContact {
        if (apiKey.isBlank()) throw GeminiException("No API key set. Add your Gemini key in Settings.")

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val body = buildRequestBody(base64Jpeg).toString().toRequestBody(json)

        val request = Request.Builder()
            .url(url)
            .header("x-goog-api-key", apiKey)
            .post(body)
            .build()

        val responseText: String
        val code: Int
        try {
            client.newCall(request).execute().use { resp ->
                code = resp.code
                responseText = resp.body?.string().orEmpty()
            }
        } catch (e: Exception) {
            throw GeminiException("Network error: ${e.message ?: "couldn't reach Gemini."}")
        }

        if (code !in 200..299) {
            throw GeminiException(parseApiError(responseText, code))
        }
        return parseContact(responseText)
    }

    private fun buildRequestBody(base64Jpeg: String): JSONObject {
        val prompt = """
            You are extracting contact details from a photo of a business card or contact information.
            Read every part of the image. Return ONLY the fields that actually appear.
            Use the person's full name for "name".
            For phone numbers, output digits only with a leading + if present — no spaces, hyphens, or parentheses.
            Combine multi-line street addresses into a single "address" string.
            Leave a field empty (or its array empty) if it is not present. Do not invent anything.
        """.trimIndent()

        val schema = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties", JSONObject()
                    .put("name", strType())
                    .put("jobTitle", strType())
                    .put("company", strType())
                    .put("phones", arrType())
                    .put("emails", arrType())
                    .put("websites", arrType())
                    .put("address", strType())
            )

        val parts = JSONArray()
            .put(JSONObject().put("text", prompt))
            .put(
                JSONObject().put(
                    "inline_data",
                    JSONObject().put("mime_type", "image/jpeg").put("data", base64Jpeg)
                )
            )

        return JSONObject()
            .put("contents", JSONArray().put(JSONObject().put("parts", parts)))
            .put(
                "generationConfig", JSONObject()
                    .put("responseMimeType", "application/json")
                    .put("responseSchema", schema)
                    .put("temperature", 0)
            )
    }

    private fun strType() = JSONObject().put("type", "STRING")
    private fun arrType() = JSONObject().put("type", "ARRAY").put("items", strType())

    private fun parseContact(responseText: String): ParsedContact {
        val root = JSONObject(responseText)
        val candidates = root.optJSONArray("candidates")
            ?: throw GeminiException("Gemini returned no result. Try again.")
        if (candidates.length() == 0) throw GeminiException("Gemini returned no result. Try again.")

        val content = candidates.getJSONObject(0).optJSONObject("content")
        val firstPart = content?.optJSONArray("parts")?.optJSONObject(0)
        val text = firstPart?.optString("text").orEmpty()
        if (text.isBlank()) throw GeminiException("Couldn't read any details from the image.")

        val data = JSONObject(text)
        return ParsedContact(
            name = data.optString("name").trim(),
            jobTitle = data.optString("jobTitle").trim(),
            company = data.optString("company").trim(),
            phones = data.optJSONArray("phones").toStringList().map { Phones.normalize(it) }.filter { it.isNotEmpty() },
            emails = data.optJSONArray("emails").toStringList(),
            websites = data.optJSONArray("websites").toStringList(),
            address = data.optString("address").trim(),
            rawText = text
        )
    }

    private fun parseApiError(responseText: String, code: Int): String {
        val message = runCatching {
            JSONObject(responseText).optJSONObject("error")?.optString("message")
        }.getOrNull()
        return when {
            code == 400 && message?.contains("API key", true) == true ->
                "Invalid API key. Check it in Settings."
            code == 429 -> "Free-tier rate limit hit. Wait a moment and try again."
            !message.isNullOrBlank() -> message
            else -> "Gemini error ($code). Please try again."
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length())
            .map { optString(it).trim() }
            .filter { it.isNotEmpty() }
    }
}
