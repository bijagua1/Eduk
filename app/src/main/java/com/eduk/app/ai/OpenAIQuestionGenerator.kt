package com.eduk.app.ai

import com.eduk.app.model.Question
import android.util.Base64
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import org.json.JSONObject
import org.json.JSONArray

/**
 * Real implementation of QuestionGenerator using OpenAI Vision API.
 * This class demonstrates the "Magnifique" high-tech integration.
 */
class OpenAIQuestionGenerator(private val apiKey: String) : QuestionGenerator {

    override suspend fun generateFromMedia(mediaPath: String, context: GenerationContext): List<Question> {
        val prompt = """
            Analyze this image of a textbook page for a ${context.gradeLevel}th grade student.
            Generate 3 multiple-choice questions in ${context.preferredLanguage}.
            Subject: ${context.targetSubject ?: "General Education"}.
            Format: JSON array of objects with fields: question, options (list), correctIndex, explanation.
        """.trimIndent()

        val imageBase64 = Base64.encodeToString(File(mediaPath).readBytes(), Base64.NO_WRAP)
        val jsonRequest = JSONObject().apply {
            put("model", "gpt-4o")
            put("temperature", 0.2)
            put("messages", JSONArray().put(JSONObject().apply {
                put("role", "user")
                put("content", JSONArray()
                    .put(JSONObject().put("type", "text").put("text", prompt))
                    .put(JSONObject().put("type", "image_url").put("image_url", JSONObject().put("url", "data:image/jpeg;base64,$imageBase64")))
                )
            }))
        }

        val connection = URL("https://api.openai.com/v1/chat/completions").openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.doOutput = true
        connection.setRequestProperty("Authorization", "Bearer $apiKey")
        connection.setRequestProperty("Content-Type", "application/json")
        connection.outputStream.use { it.write(jsonRequest.toString().toByteArray()) }
        check(connection.responseCode in 200..299) { "Vision AI request failed (${connection.responseCode})" }
        val response = connection.inputStream.bufferedReader().use { it.readText() }
        val content = JSONObject(response).getJSONArray("choices").getJSONObject(0)
            .getJSONObject("message").getString("content")
            .replace("```json", "").replace("```", "").trim()
        val questions = JSONArray(content)
        return List(questions.length()) { index ->
            val item = questions.getJSONObject(index)
            Question(
                subject = context.targetSubject ?: "General Education",
                gradeLevel = context.gradeLevel,
                difficulty = item.optString("difficulty", "Medium"),
                questionText = item.getString("question"),
                options = item.getJSONArray("options").let { options -> List(options.length()) { options.getString(it) } },
                correctOptionIndex = item.getInt("correctIndex"),
                explanation = item.optString("explanation"),
                sourceMaterial = mediaPath,
            )
        }
    }
}
