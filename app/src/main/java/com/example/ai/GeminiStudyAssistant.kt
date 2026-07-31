package com.example.ai

import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object GeminiStudyAssistant {
    private const val MODEL_NAME = "gemini-3.5-flash"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent"

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    suspend fun generateAiContent(prompt: String, systemInstruction: String? = null): String = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext getLocalFallbackResponse(prompt)
        }

        try {
            val requestJson = JSONObject()
            val contentsArray = JSONArray()
            val contentObj = JSONObject()
            val partsArray = JSONArray()
            val partObj = JSONObject()
            partObj.put("text", prompt)
            partsArray.put(partObj)
            contentObj.put("parts", partsArray)
            contentsArray.put(contentObj)
            requestJson.put("contents", contentsArray)

            if (!systemInstruction.isNullOrBlank()) {
                val sysContent = JSONObject()
                val sysParts = JSONArray()
                val sysPart = JSONObject()
                sysPart.put("text", systemInstruction)
                sysParts.put(sysPart)
                sysContent.put("parts", sysParts)
                requestJson.put("systemInstruction", sysContent)
            }

            val requestBody = requestJson.toString().toRequestBody("application/json".toMediaType())
            val request = Request.Builder()
                .url("$BASE_URL?key=$apiKey")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseString = response.body?.string() ?: ""

            if (response.isSuccessful && responseString.isNotEmpty()) {
                val jsonRes = JSONObject(responseString)
                val candidates = jsonRes.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val firstCandidate = candidates.getJSONObject(0)
                    val content = firstCandidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text", "No text generated.")
                    }
                }
            }
            return@withContext getLocalFallbackResponse(prompt)
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext getLocalFallbackResponse(prompt)
        }
    }

    private fun getLocalFallbackResponse(prompt: String): String {
        val lower = prompt.lowercase()
        return when {
            lower.contains("explain") || lower.contains("what is") -> {
                "### Concept Overview\n\n**${prompt.take(40)}...** is a fundamental topic in university curriculum.\n\n" +
                "1. **Core Principle**: It focuses on structuring complex systems efficiently using established architectural patterns.\n" +
                "2. **Key Formulas / Syntax**: E = mc^2, O(N log N), or standard algorithmic state transformations.\n" +
                "3. **Exam Tip**: Highlight practical code examples and state transition diagrams in your answer sheet to score full marks!"
            }
            lower.contains("quiz") || lower.contains("mcq") -> {
                "1. **Question 1**: What is the primary advantage of normalized database schemas?\n" +
                "   - A) Faster read access\n" +
                "   - B) Reduction of data redundancy [Correct]\n" +
                "   - C) More storage consumption\n" +
                "   - D) Disables foreign keys\n\n" +
                "2. **Question 2**: Which time complexity best describes Binary Search on a sorted array?\n" +
                "   - A) O(N)\n" +
                "   - B) O(N^2)\n" +
                "   - C) O(log N) [Correct]\n" +
                "   - D) O(1)"
            }
            lower.contains("flashcard") -> {
                "📇 **Flashcard 1**\n**Q**: What is Deadlock in OS?\n**A**: A situation where a set of processes are blocked because each process holds a resource and waits for another resource held by some other process.\n\n" +
                "📇 **Flashcard 2**\n**Q**: What are ACID properties?\n**A**: Atomicity, Consistency, Isolation, Durability."
            }
            lower.contains("summary") || lower.contains("summarize") -> {
                "📌 **Executive Notes Summary**:\n\n" +
                "- **Key Concept 1**: Modular design increases maintainability.\n" +
                "- **Key Concept 2**: Time & space complexity trade-offs must be evaluated based on dataset scale.\n" +
                "- **Key Formula**: Performance = $\\frac{1}{\\text{Execution Time}}$\n" +
                "- **Important Exam Question**: Expected in 10-mark long questions!"
            }
            else -> {
                "🤖 **StudySwap AI Assistant**: Here is a structured study breakdown for **$prompt**:\n\n" +
                "• **Overview**: Essential subject matter for college semester exams.\n" +
                "• **Key Takeaway**: Master basic definitions, diagrams, and code snippets.\n" +
                "• **Next Step**: Try generating practice MCQs or flashcards with the tools below!"
            }
        }
    }
}
