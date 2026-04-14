package com.funtime.sciai.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * IntelligenceManager — The adaptive brain of SciAI.
 *
 * Tracks per-topic accuracy, time spent, weak areas, and generates
 * intelligent next-action suggestions. All data persisted via SharedPreferences
 * and a lightweight JSON file for topic history.
 */
class IntelligenceManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sciai_intelligence", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_OVERALL_CORRECT = "overall_correct"
        private const val KEY_OVERALL_TOTAL = "overall_total"
        private const val KEY_LAST_STUDY_TIME = "last_study_time"
        private const val KEY_TOTAL_STUDY_SESSIONS = "total_study_sessions"
        private const val KEY_QUIZ_TIME_MS = "total_quiz_time_ms"
        private const val KEY_TEST_TIME_MS = "total_test_time_ms"
        private const val KEY_EXPERT_QUERIES = "total_expert_queries"
        private const val TOPIC_FILE = "topic_intelligence.json"
        private const val WEAK_THRESHOLD = 60f // Topics below 60% are "weak"
    }

    // ══════════════════════════════════════════════════
    // DATA CLASSES
    // ══════════════════════════════════════════════════

    data class TopicStats(
        val topic: String,
        val correct: Int,
        val total: Int,
        val lastAttemptTime: Long,
        val totalTimeMs: Long
    ) {
        val accuracy: Float get() = if (total > 0) (correct.toFloat() / total * 100f) else 0f
    }

    data class WeakTopic(
        val topic: String,
        val accuracy: Float,
        val attempts: Int
    )

    enum class ActionType { QUIZ, TEST, EXPERT, SEARCH, ARTICLE }

    data class SuggestedAction(
        val title: String,
        val description: String,
        val actionType: ActionType,
        val targetTopic: String? = null
    )

    data class ProgressData(
        val overallAccuracy: Float,
        val totalQuestions: Int,
        val totalCorrect: Int,
        val weakTopicCount: Int,
        val expertQueries: Int,
        val studySessions: Int,
        val topTopics: List<TopicStats> // Top 5 most-practiced topics
    )

    // ══════════════════════════════════════════════════
    // RECORDING — Called after user completes activities
    // ══════════════════════════════════════════════════

    /**
     * Record quiz/test result for a topic.
     * @param topic The topic or question category
     * @param correct Number of correct answers
     * @param total Total questions attempted
     * @param timeMs Time taken in milliseconds
     */
    fun recordQuizResult(topic: String, correct: Int, total: Int, timeMs: Long = 0L) {
        val normalizedTopic = normalizeTopic(topic)

        // Update overall stats
        val overallCorrect = prefs.getInt(KEY_OVERALL_CORRECT, 0) + correct
        val overallTotal = prefs.getInt(KEY_OVERALL_TOTAL, 0) + total
        prefs.edit()
            .putInt(KEY_OVERALL_CORRECT, overallCorrect)
            .putInt(KEY_OVERALL_TOTAL, overallTotal)
            .putLong(KEY_LAST_STUDY_TIME, System.currentTimeMillis())
            .putLong(KEY_QUIZ_TIME_MS, prefs.getLong(KEY_QUIZ_TIME_MS, 0L) + timeMs)
            .apply()

        // Update per-topic stats
        updateTopicStats(normalizedTopic, correct, total, timeMs)
    }

    /**
     * Record theory test submission with a score (0-100).
     */
    fun recordTestSubmission(topic: String, score: Float) {
        val normalizedTopic = normalizeTopic(topic)
        val correct = (score / 100f * 10).toInt().coerceIn(0, 10) // Normalize to /10
        val total = 10

        val overallCorrect = prefs.getInt(KEY_OVERALL_CORRECT, 0) + correct
        val overallTotal = prefs.getInt(KEY_OVERALL_TOTAL, 0) + total
        prefs.edit()
            .putInt(KEY_OVERALL_CORRECT, overallCorrect)
            .putInt(KEY_OVERALL_TOTAL, overallTotal)
            .putLong(KEY_LAST_STUDY_TIME, System.currentTimeMillis())
            .apply()

        updateTopicStats(normalizedTopic, correct, total, 0L)
    }

    /**
     * Record an Expert mode query for a topic.
     */
    fun recordExpertQuery(topic: String) {
        val expertCount = prefs.getInt(KEY_EXPERT_QUERIES, 0) + 1
        prefs.edit()
            .putInt(KEY_EXPERT_QUERIES, expertCount)
            .putLong(KEY_LAST_STUDY_TIME, System.currentTimeMillis())
            .apply()
    }

    /**
     * Record a study session start.
     */
    fun recordStudySession() {
        val sessions = prefs.getInt(KEY_TOTAL_STUDY_SESSIONS, 0) + 1
        prefs.edit()
            .putInt(KEY_TOTAL_STUDY_SESSIONS, sessions)
            .putLong(KEY_LAST_STUDY_TIME, System.currentTimeMillis())
            .apply()
    }

    // ══════════════════════════════════════════════════
    // RETRIEVAL — Used by UI to display intelligence
    // ══════════════════════════════════════════════════

    /**
     * Get accuracy for a specific topic.
     */
    fun getAccuracyForTopic(topic: String): Float {
        val stats = getTopicStats(normalizeTopic(topic))
        return stats?.accuracy ?: 0f
    }

    /**
     * Get overall accuracy across all topics.
     */
    fun getOverallAccuracy(): Float {
        val correct = prefs.getInt(KEY_OVERALL_CORRECT, 0)
        val total = prefs.getInt(KEY_OVERALL_TOTAL, 0)
        return if (total > 0) (correct.toFloat() / total * 100f) else 0f
    }

    /**
     * Get list of weak topics (accuracy < 60%).
     */
    fun getWeakTopics(): List<WeakTopic> {
        return getAllTopicStats()
            .filter { it.accuracy < WEAK_THRESHOLD && it.total >= 2 } // Need at least 2 attempts
            .sortedBy { it.accuracy }
            .map { WeakTopic(it.topic, it.accuracy, it.total) }
    }

    /**
     * Get adaptive next-action suggestion based on user performance.
     */
    fun getSuggestedAction(): SuggestedAction {
        val weakTopics = getWeakTopics()
        val overallAccuracy = getOverallAccuracy()
        val totalQuestions = prefs.getInt(KEY_OVERALL_TOTAL, 0)
        val expertQueries = prefs.getInt(KEY_EXPERT_QUERIES, 0)
        val lastStudy = prefs.getLong(KEY_LAST_STUDY_TIME, 0L)
        val hoursSinceStudy = (System.currentTimeMillis() - lastStudy) / (1000 * 60 * 60)

        return when {
            // No data yet
            totalQuestions == 0 -> SuggestedAction(
                title = "Start Learning",
                description = "Begin your journey → Try a Quiz!",
                actionType = ActionType.QUIZ
            )

            // Haven't studied today
            hoursSinceStudy > 24 -> SuggestedAction(
                title = "Welcome Back!",
                description = "It's been ${hoursSinceStudy.toInt()}h — Let's pick up where you left off",
                actionType = ActionType.QUIZ,
                targetTopic = weakTopics.firstOrNull()?.topic
            )

            // Weak topic exists with very low accuracy
            weakTopics.isNotEmpty() && weakTopics.first().accuracy < 40 -> SuggestedAction(
                title = "Focus: ${weakTopics.first().topic}",
                description = "Only ${weakTopics.first().accuracy.toInt()}% accuracy → Practice Quiz?",
                actionType = ActionType.QUIZ,
                targetTopic = weakTopics.first().topic
            )

            // Weak topic exists with moderate accuracy
            weakTopics.isNotEmpty() && weakTopics.first().accuracy < 60 -> SuggestedAction(
                title = "Improve: ${weakTopics.first().topic}",
                description = "${weakTopics.first().accuracy.toInt()}% → Try a Test to go deeper",
                actionType = ActionType.TEST,
                targetTopic = weakTopics.first().topic
            )

            // Good accuracy, push to expert
            overallAccuracy >= 75 -> SuggestedAction(
                title = "Try Expert Mode",
                description = "You're at ${overallAccuracy.toInt()}% accuracy → Go Expert! 🧠",
                actionType = ActionType.EXPERT
            )

            // Moderate accuracy, keep practicing
            overallAccuracy >= 50 -> SuggestedAction(
                title = "Keep Practicing",
                description = "Accuracy: ${overallAccuracy.toInt()}% → More quizzes will help",
                actionType = ActionType.QUIZ
            )

            // Low accuracy, revise concepts
            else -> SuggestedAction(
                title = "Revise Concepts",
                description = "Accuracy: ${overallAccuracy.toInt()}% → Read articles to build foundation",
                actionType = ActionType.ARTICLE
            )
        }
    }

    /**
     * Get all progress data for dashboard display.
     */
    fun getProgressData(): ProgressData {
        val allTopics = getAllTopicStats()
        val correct = prefs.getInt(KEY_OVERALL_CORRECT, 0)
        val total = prefs.getInt(KEY_OVERALL_TOTAL, 0)

        return ProgressData(
            overallAccuracy = if (total > 0) (correct.toFloat() / total * 100f) else 0f,
            totalQuestions = total,
            totalCorrect = correct,
            weakTopicCount = getWeakTopics().size,
            expertQueries = prefs.getInt(KEY_EXPERT_QUERIES, 0),
            studySessions = prefs.getInt(KEY_TOTAL_STUDY_SESSIONS, 0),
            topTopics = allTopics.sortedByDescending { it.total }.take(5)
        )
    }

    /**
     * Get time since last study in hours.
     */
    fun getHoursSinceLastStudy(): Long {
        val lastStudy = prefs.getLong(KEY_LAST_STUDY_TIME, 0L)
        if (lastStudy == 0L) return -1
        return (System.currentTimeMillis() - lastStudy) / (1000 * 60 * 60)
    }

    // ══════════════════════════════════════════════════
    // INTERNAL — Topic stats JSON persistence
    // ══════════════════════════════════════════════════

    private fun normalizeTopic(topic: String): String {
        return topic.trim()
            .lowercase()
            .replace(Regex("[^a-z0-9 ]"), "")
            .split(" ")
            .filter { it.length > 2 } // Remove small words
            .take(4) // Cap at 4 words for topic key
            .joinToString(" ")
            .ifEmpty { "general" }
    }

    private fun getTopicFile(): File = File(context.filesDir, TOPIC_FILE)

    private fun getAllTopicStats(): List<TopicStats> {
        val file = getTopicFile()
        if (!file.exists()) return emptyList()

        return try {
            val json = JSONArray(file.readText())
            val list = mutableListOf<TopicStats>()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                list.add(
                    TopicStats(
                        topic = obj.getString("topic"),
                        correct = obj.getInt("correct"),
                        total = obj.getInt("total"),
                        lastAttemptTime = obj.optLong("lastAttempt", 0L),
                        totalTimeMs = obj.optLong("totalTimeMs", 0L)
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun getTopicStats(topic: String): TopicStats? {
        return getAllTopicStats().find { it.topic == topic }
    }

    private fun updateTopicStats(topic: String, newCorrect: Int, newTotal: Int, timeMs: Long) {
        val allStats = getAllTopicStats().toMutableList()
        val existing = allStats.find { it.topic == topic }

        if (existing != null) {
            allStats.remove(existing)
            allStats.add(
                TopicStats(
                    topic = topic,
                    correct = existing.correct + newCorrect,
                    total = existing.total + newTotal,
                    lastAttemptTime = System.currentTimeMillis(),
                    totalTimeMs = existing.totalTimeMs + timeMs
                )
            )
        } else {
            allStats.add(
                TopicStats(
                    topic = topic,
                    correct = newCorrect,
                    total = newTotal,
                    lastAttemptTime = System.currentTimeMillis(),
                    totalTimeMs = timeMs
                )
            )
        }

        saveTopicStats(allStats)
    }

    private fun saveTopicStats(stats: List<TopicStats>) {
        try {
            val json = JSONArray()
            stats.forEach { s ->
                json.put(JSONObject().apply {
                    put("topic", s.topic)
                    put("correct", s.correct)
                    put("total", s.total)
                    put("lastAttempt", s.lastAttemptTime)
                    put("totalTimeMs", s.totalTimeMs)
                })
            }
            getTopicFile().writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
