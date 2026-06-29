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

/** Result of an extraction: the contact plus the field keys Gemini was unsure about. */
data class Extraction(val contact: ParsedContact, val lowConfidence: Set<String>)

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

    fun extract(images: List<String>, apiKey: String): Extraction {
        if (apiKey.isBlank()) throw GeminiException("No API key set. Add it in Settings.")
        if (images.isEmpty()) throw GeminiException("No image to read.")

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$model:generateContent"
        val body = buildRequestBody(images).toString().toRequestBody(json)

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
            throw GeminiException("Network error: ${e.message ?: "couldn't reach the server."}")
        }

        if (code !in 200..299) {
            throw GeminiException(parseApiError(responseText, code))
        }
        return parseExtraction(responseText)
    }

    private fun buildRequestBody(images: List<String>): JSONObject {
        val multi = if (images.size > 1)
            "The images are different sides/photos of the SAME card — combine them into one contact.\n" else ""
        val prompt = """
            You are extracting contact details from a photo of a business card or contact information.
            ${multi}Read every part of the image. Return ONLY the fields that actually appear.
            Use the person's full name for "name".
            For phone numbers, output digits only with a leading + if present — no spaces, hyphens, or parentheses.
            Combine multi-line street addresses into a single "address" string.
            Leave a field empty (or its array empty) if it is not present. Do not invent anything.
            In "uncertainFields", list the keys of any fields you are NOT confident about
            (allowed keys: name, jobTitle, company, phones, emails, websites, address).
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
                    .put("uncertainFields", arrType())
            )

        val parts = JSONArray().put(JSONObject().put("text", prompt))
        images.forEach { b64 ->
            parts.put(
                JSONObject().put(
                    "inline_data",
                    JSONObject().put("mime_type", "image/jpeg").put("data", b64)
                )
            )
        }

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

    private val knownFields = setOf("name", "jobTitle", "company", "phones", "emails", "websites", "address")

    private fun parseExtraction(responseText: String): Extraction {
        val root = JSONObject(responseText)
        val candidates = root.optJSONArray("candidates")
            ?: throw GeminiException("No result. Try again.")
        if (candidates.length() == 0) throw GeminiException("No result. Try again.")

        val content = candidates.getJSONObject(0).optJSONObject("content")
        val firstPart = content?.optJSONArray("parts")?.optJSONObject(0)
        val text = firstPart?.optString("text").orEmpty()
        if (text.isBlank()) throw GeminiException("Couldn't read any details from the image.")

        val data = JSONObject(text)
        val contact = ParsedContact(
            name = data.optString("name").trim(),
            jobTitle = data.optString("jobTitle").trim(),
            company = data.optString("company").trim(),
            phones = data.optJSONArray("phones").toStringList().map { Phones.normalize(it) }.filter { it.isNotEmpty() },
            emails = data.optJSONArray("emails").toStringList(),
            websites = data.optJSONArray("websites").toStringList(),
            address = data.optString("address").trim(),
            rawText = text
        )
        val lowConfidence = data.optJSONArray("uncertainFields").toStringList()
            .filter { it in knownFields }.toSet()
        return Extraction(contact, lowConfidence)
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
            else -> "Scan failed ($code). Please try again."
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) return emptyList()
        return (0 until length())
            .map { optString(it).trim() }
            .filter { it.isNotEmpty() }
    }
}
