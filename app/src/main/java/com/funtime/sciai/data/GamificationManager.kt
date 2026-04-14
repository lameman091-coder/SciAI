package com.funtime.sciai.data

import android.content.Context
import android.content.SharedPreferences
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * GamificationManager — Complete gamification engine for SciAI.
 *
 * Features:
 * 🔥 Streak System — consecutive daily usage tracking
 * 🏆 Achievements — milestone-based unlocks
 * 📊 XP Breakdown — granular XP rewards with history
 * 🎯 Daily Missions — refreshed every 24h
 *
 * All data persisted via SharedPreferences + JSON files.
 */
class GamificationManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("sciai_gamification", Context.MODE_PRIVATE)

    companion object {
        // Streak keys
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_BEST_STREAK = "best_streak"
        private const val KEY_LAST_ACTIVE_DATE = "last_active_date"

        // XP keys
        private const val KEY_TOTAL_XP = "total_xp"
        private const val KEY_TODAY_XP = "today_xp"
        private const val KEY_TODAY_XP_DATE = "today_xp_date"

        // Mission keys
        private const val KEY_MISSIONS_DATE = "missions_date"
        private const val KEY_MISSION_QUIZ_COUNT = "mission_quiz_count"
        private const val KEY_MISSION_ARTICLE_COUNT = "mission_article_count"
        private const val KEY_MISSION_EXPERT_DONE = "mission_expert_done"
        private const val KEY_MISSION_STREAK_CHECKED = "mission_streak_checked"

        // Achievement tracking
        private const val KEY_TOTAL_QUIZZES = "total_quizzes_completed"
        private const val KEY_TOTAL_TESTS = "total_tests_completed"
        private const val KEY_TOTAL_ARTICLES_READ = "total_articles_read"
        private const val KEY_FIRST_QUIZ_DONE = "first_quiz_done"
        private const val KEY_FIRST_EXPERT_DONE = "first_expert_done"

        // Files
        private const val XP_HISTORY_FILE = "xp_history.json"
        private const val ACHIEVEMENTS_FILE = "achievements.json"
        private const val MAX_XP_HISTORY = 50

        // XP Values
        const val XP_QUIZ_CORRECT = 10
        const val XP_QUIZ_WRONG = 2
        const val XP_TEST_SUBMIT = 25
        const val XP_EXPERT_QUERY = 15
        const val XP_ARTICLE_READ = 5
        const val XP_DAILY_LOGIN = 5
        const val XP_STREAK_BONUS = 10
        const val XP_ALL_MISSIONS = 20
    }

    // ══════════════════════════════════════════════════
    // DATA CLASSES
    // ══════════════════════════════════════════════════

    data class XPEntry(
        val amount: Int,
        val source: String, // "Quiz Correct", "Test Submit", "Expert Query", etc.
        val timestamp: Long
    )

    data class Achievement(
        val id: String,
        val title: String,
        val description: String,
        val emoji: String,
        val unlockedAt: Long = 0L,
        val isUnlocked: Boolean = false
    )

    data class Mission(
        val id: String,
        val title: String,
        val description: String,
        val emoji: String,
        val currentProgress: Int,
        val targetProgress: Int,
        val isCompleted: Boolean
    )

    // ══════════════════════════════════════════════════
    // 🔥 STREAK SYSTEM
    // ══════════════════════════════════════════════════

    private fun todayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    /**
     * Call this on app launch to update streak.
     * Returns true if this is a new day (streak extended).
     */
    fun recordDailyActivity(): Boolean {
        val today = todayDateString()
        val lastActive = prefs.getString(KEY_LAST_ACTIVE_DATE, "") ?: ""

        if (lastActive == today) return false // Already recorded today

        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
            Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        )

        val currentStreak = if (lastActive == yesterday) {
            // Consecutive day — extend streak
            prefs.getInt(KEY_CURRENT_STREAK, 0) + 1
        } else if (lastActive.isEmpty()) {
            // First ever use
            1
        } else {
            // Streak broken — reset
            1
        }

        val bestStreak = maxOf(currentStreak, prefs.getInt(KEY_BEST_STREAK, 0))

        prefs.edit()
            .putString(KEY_LAST_ACTIVE_DATE, today)
            .putInt(KEY_CURRENT_STREAK, currentStreak)
            .putInt(KEY_BEST_STREAK, bestStreak)
            .apply()

        // Award daily login XP
        awardXP(XP_DAILY_LOGIN, "Daily Login")

        // Streak bonus every 3 days
        if (currentStreak > 0 && currentStreak % 3 == 0) {
            awardXP(XP_STREAK_BONUS, "Streak Bonus (${currentStreak}d)")
        }

        return true
    }

    fun getCurrentStreak(): Int = prefs.getInt(KEY_CURRENT_STREAK, 0)
    fun getBestStreak(): Int = prefs.getInt(KEY_BEST_STREAK, 0)

    fun isStreakActive(): Boolean {
        val lastActive = prefs.getString(KEY_LAST_ACTIVE_DATE, "") ?: ""
        val today = todayDateString()
        val yesterday = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(
            Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L)
        )
        return lastActive == today || lastActive == yesterday
    }

    fun getStreakEmoji(): String = if (isStreakActive()) "🔥" else "❄️"

    // ══════════════════════════════════════════════════
    // 📊 XP SYSTEM
    // ══════════════════════════════════════════════════

    fun getTotalXP(): Int = prefs.getInt(KEY_TOTAL_XP, 0)

    fun getTodayXP(): Int {
        val today = todayDateString()
        val xpDate = prefs.getString(KEY_TODAY_XP_DATE, "") ?: ""
        return if (xpDate == today) prefs.getInt(KEY_TODAY_XP, 0) else 0
    }

    fun getLevel(): Int {
        val xp = getTotalXP()
        val lvl = (xp / 100) + 1
        return lvl.coerceIn(1, 10)
    }

    fun getTitle(): String = when (getLevel()) {
        1 -> "Novice"
        2 -> "Student"
        3 -> "Smart"
        4 -> "Researcher"
        5 -> "Expert"
        6 -> "Professional"
        7 -> "Elite"
        8 -> "Legend"
        9 -> "Conqueror"
        10 -> "GOAT"
        else -> "Scholar"
    }

    /**
     * Award XP and record in history.
     */
    fun awardXP(amount: Int, source: String) {
        val today = todayDateString()
        val xpDate = prefs.getString(KEY_TODAY_XP_DATE, "") ?: ""

        val todayXp = if (xpDate == today) prefs.getInt(KEY_TODAY_XP, 0) else 0

        prefs.edit()
            .putInt(KEY_TOTAL_XP, getTotalXP() + amount)
            .putInt(KEY_TODAY_XP, todayXp + amount)
            .putString(KEY_TODAY_XP_DATE, today)
            .apply()

        // Save to history
        addXPHistoryEntry(XPEntry(amount, source, System.currentTimeMillis()))
    }

    /**
     * Get recent XP history entries.
     */
    fun getXPHistory(limit: Int = 10): List<XPEntry> {
        val file = File(context.filesDir, XP_HISTORY_FILE)
        if (!file.exists()) return emptyList()

        return try {
            val json = JSONArray(file.readText())
            val list = mutableListOf<XPEntry>()
            for (i in 0 until json.length()) {
                val obj = json.getJSONObject(i)
                list.add(
                    XPEntry(
                        amount = obj.getInt("amount"),
                        source = obj.getString("source"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list.sortedByDescending { it.timestamp }.take(limit)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun addXPHistoryEntry(entry: XPEntry) {
        try {
            val file = File(context.filesDir, XP_HISTORY_FILE)
            val existing = if (file.exists()) {
                try { JSONArray(file.readText()) } catch (_: Exception) { JSONArray() }
            } else JSONArray()

            val obj = JSONObject().apply {
                put("amount", entry.amount)
                put("source", entry.source)
                put("timestamp", entry.timestamp)
            }
            existing.put(obj)

            // Trim to max
            while (existing.length() > MAX_XP_HISTORY) {
                existing.remove(0)
            }

            file.writeText(existing.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ══════════════════════════════════════════════════
    // 📊 XP RECORDING HELPERS — Called by screens
    // ══════════════════════════════════════════════════

    fun onQuizCorrect() {
        awardXP(XP_QUIZ_CORRECT, "Quiz Correct")
        incrementCounter(KEY_TOTAL_QUIZZES)
        if (!prefs.getBoolean(KEY_FIRST_QUIZ_DONE, false)) {
            prefs.edit().putBoolean(KEY_FIRST_QUIZ_DONE, true).apply()
        }
        updateMissionProgress("quiz")
    }

    fun onQuizWrong() {
        awardXP(XP_QUIZ_WRONG, "Quiz Attempt")
        incrementCounter(KEY_TOTAL_QUIZZES)
        if (!prefs.getBoolean(KEY_FIRST_QUIZ_DONE, false)) {
            prefs.edit().putBoolean(KEY_FIRST_QUIZ_DONE, true).apply()
        }
        updateMissionProgress("quiz")
    }

    fun onTestSubmit() {
        awardXP(XP_TEST_SUBMIT, "Test Submission")
        incrementCounter(KEY_TOTAL_TESTS)
        updateMissionProgress("test")
    }

    fun onExpertQuery() {
        awardXP(XP_EXPERT_QUERY, "Expert Query")
        if (!prefs.getBoolean(KEY_FIRST_EXPERT_DONE, false)) {
            prefs.edit().putBoolean(KEY_FIRST_EXPERT_DONE, true).apply()
        }
        updateMissionProgress("expert")
    }

    fun onArticleRead() {
        awardXP(XP_ARTICLE_READ, "Article Read")
        incrementCounter(KEY_TOTAL_ARTICLES_READ)
        updateMissionProgress("article")
    }

    private fun incrementCounter(key: String) {
        prefs.edit().putInt(key, prefs.getInt(key, 0) + 1).apply()
    }

    // ══════════════════════════════════════════════════
    // 🏆 ACHIEVEMENTS SYSTEM
    // ══════════════════════════════════════════════════

    private val allAchievements = listOf(
        Achievement("first_quiz", "First Quiz", "Complete your first quiz", "✅"),
        Achievement("quiz_10", "Quiz Warrior", "Complete 10 quizzes", "⚔️"),
        Achievement("quiz_50", "Quiz Master", "Complete 50 quizzes", "🏅"),
        Achievement("test_5", "Theory Thinker", "Complete 5 tests", "📝"),
        Achievement("test_10", "Test Titan", "Complete 10 tests", "🔥"),
        Achievement("expert_unlock", "Expert Unlocked", "Use Expert mode for the first time", "🧠"),
        Achievement("streak_3", "3-Day Streak", "Maintain a 3-day study streak", "📅"),
        Achievement("streak_7", "Week Warrior", "Maintain a 7-day study streak", "🗓️"),
        Achievement("streak_30", "Monthly Master", "Maintain a 30-day study streak", "🏆"),
        Achievement("xp_100", "Century Club", "Earn 100 total XP", "💯"),
        Achievement("xp_500", "XP Elite", "Earn 500 total XP", "⭐"),
        Achievement("xp_1000", "XP Legend", "Earn 1000 total XP", "🌟"),
        Achievement("articles_10", "Research Reader", "Read 10 articles", "📚"),
        Achievement("accuracy_80", "Sharp Mind", "Achieve 80%+ overall accuracy", "🎯"),
        Achievement("accuracy_95", "Perfection", "Achieve 95%+ overall accuracy", "💎")
    )

    /**
     * Check and return newly unlocked achievements.
     */
    fun checkNewAchievements(): List<Achievement> {
        val unlocked = getUnlockedIds()
        val newlyUnlocked = mutableListOf<Achievement>()
        val now = System.currentTimeMillis()

        val checks = mapOf(
            "first_quiz" to { prefs.getBoolean(KEY_FIRST_QUIZ_DONE, false) },
            "quiz_10" to { prefs.getInt(KEY_TOTAL_QUIZZES, 0) >= 10 },
            "quiz_50" to { prefs.getInt(KEY_TOTAL_QUIZZES, 0) >= 50 },
            "test_5" to { prefs.getInt(KEY_TOTAL_TESTS, 0) >= 5 },
            "test_10" to { prefs.getInt(KEY_TOTAL_TESTS, 0) >= 10 },
            "expert_unlock" to { prefs.getBoolean(KEY_FIRST_EXPERT_DONE, false) },
            "streak_3" to { getBestStreak() >= 3 },
            "streak_7" to { getBestStreak() >= 7 },
            "streak_30" to { getBestStreak() >= 30 },
            "xp_100" to { getTotalXP() >= 100 },
            "xp_500" to { getTotalXP() >= 500 },
            "xp_1000" to { getTotalXP() >= 1000 },
            "articles_10" to { prefs.getInt(KEY_TOTAL_ARTICLES_READ, 0) >= 10 }
            // accuracy achievements checked separately since they need IntelligenceManager
        )

        checks.forEach { (id, condition) ->
            if (!unlocked.contains(id) && condition()) {
                val achievement = allAchievements.find { it.id == id }
                if (achievement != null) {
                    val unlockedAchievement = achievement.copy(unlockedAt = now, isUnlocked = true)
                    newlyUnlocked.add(unlockedAchievement)
                    saveUnlockedAchievement(id, now)
                }
            }
        }

        return newlyUnlocked
    }

    /**
     * Check accuracy-based achievements (requires IntelligenceManager).
     */
    fun checkAccuracyAchievements(overallAccuracy: Float): List<Achievement> {
        val unlocked = getUnlockedIds()
        val newlyUnlocked = mutableListOf<Achievement>()
        val now = System.currentTimeMillis()

        if (!unlocked.contains("accuracy_80") && overallAccuracy >= 80f) {
            allAchievements.find { it.id == "accuracy_80" }?.let {
                newlyUnlocked.add(it.copy(unlockedAt = now, isUnlocked = true))
                saveUnlockedAchievement("accuracy_80", now)
            }
        }
        if (!unlocked.contains("accuracy_95") && overallAccuracy >= 95f) {
            allAchievements.find { it.id == "accuracy_95" }?.let {
                newlyUnlocked.add(it.copy(unlockedAt = now, isUnlocked = true))
                saveUnlockedAchievement("accuracy_95", now)
            }
        }

        return newlyUnlocked
    }

    /**
     * Get all achievements with their unlock status.
     */
    fun getAllAchievements(): List<Achievement> {
        val unlockedMap = getUnlockedMap()
        return allAchievements.map { achievement ->
            val unlockedTime = unlockedMap[achievement.id]
            if (unlockedTime != null) {
                achievement.copy(unlockedAt = unlockedTime, isUnlocked = true)
            } else {
                achievement
            }
        }
    }

    fun getUnlockedAchievements(): List<Achievement> =
        getAllAchievements().filter { it.isUnlocked }

    fun getLatestAchievement(): Achievement? =
        getUnlockedAchievements().maxByOrNull { it.unlockedAt }

    private fun getUnlockedIds(): Set<String> = getUnlockedMap().keys

    private fun getUnlockedMap(): Map<String, Long> {
        val file = File(context.filesDir, ACHIEVEMENTS_FILE)
        if (!file.exists()) return emptyMap()
        return try {
            val json = JSONObject(file.readText())
            val result = mutableMapOf<String, Long>()
            json.keys().forEach { key ->
                result[key] = json.getLong(key)
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun saveUnlockedAchievement(id: String, timestamp: Long) {
        try {
            val file = File(context.filesDir, ACHIEVEMENTS_FILE)
            val json = if (file.exists()) {
                try { JSONObject(file.readText()) } catch (_: Exception) { JSONObject() }
            } else JSONObject()
            json.put(id, timestamp)
            file.writeText(json.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // ══════════════════════════════════════════════════
    // 🎯 DAILY MISSIONS
    // ══════════════════════════════════════════════════

    /**
     * Get today's missions. Resets if it's a new day.
     */
    fun getDailyMissions(): List<Mission> {
        val today = todayDateString()
        val missionDate = prefs.getString(KEY_MISSIONS_DATE, "") ?: ""

        // Reset missions if new day
        if (missionDate != today) {
            prefs.edit()
                .putString(KEY_MISSIONS_DATE, today)
                .putInt(KEY_MISSION_QUIZ_COUNT, 0)
                .putInt(KEY_MISSION_ARTICLE_COUNT, 0)
                .putBoolean(KEY_MISSION_EXPERT_DONE, false)
                .putBoolean(KEY_MISSION_STREAK_CHECKED, isStreakActive())
                .apply()
        }

        val quizCount = prefs.getInt(KEY_MISSION_QUIZ_COUNT, 0)
        val articleCount = prefs.getInt(KEY_MISSION_ARTICLE_COUNT, 0)
        val expertDone = prefs.getBoolean(KEY_MISSION_EXPERT_DONE, false)
        val streakActive = isStreakActive()

        return listOf(
            Mission(
                id = "quiz_2",
                title = "Quiz Champion",
                description = "Complete 2 quizzes",
                emoji = "🧠",
                currentProgress = quizCount.coerceAtMost(2),
                targetProgress = 2,
                isCompleted = quizCount >= 2
            ),
            Mission(
                id = "article_1",
                title = "Research Explorer",
                description = "Read 1 article",
                emoji = "📖",
                currentProgress = articleCount.coerceAtMost(1),
                targetProgress = 1,
                isCompleted = articleCount >= 1
            ),
            Mission(
                id = "expert_1",
                title = "Expert Challenge",
                description = "Use Expert mode once",
                emoji = "🔬",
                currentProgress = if (expertDone) 1 else 0,
                targetProgress = 1,
                isCompleted = expertDone
            ),
            Mission(
                id = "streak_1",
                title = "Keep the Fire",
                description = "Maintain your streak",
                emoji = "🔥",
                currentProgress = if (streakActive) 1 else 0,
                targetProgress = 1,
                isCompleted = streakActive
            )
        )
    }

    /**
     * Update mission progress. Called by activity recording methods.
     */
    private fun updateMissionProgress(type: String) {
        val today = todayDateString()
        val missionDate = prefs.getString(KEY_MISSIONS_DATE, "") ?: ""
        if (missionDate != today) return // Missions not initialized yet

        when (type) {
            "quiz" -> {
                val count = prefs.getInt(KEY_MISSION_QUIZ_COUNT, 0) + 1
                prefs.edit().putInt(KEY_MISSION_QUIZ_COUNT, count).apply()
            }
            "article" -> {
                val count = prefs.getInt(KEY_MISSION_ARTICLE_COUNT, 0) + 1
                prefs.edit().putInt(KEY_MISSION_ARTICLE_COUNT, count).apply()
            }
            "expert" -> {
                prefs.edit().putBoolean(KEY_MISSION_EXPERT_DONE, true).apply()
            }
            "test" -> {
                // Tests count toward quiz mission too
                val count = prefs.getInt(KEY_MISSION_QUIZ_COUNT, 0) + 1
                prefs.edit().putInt(KEY_MISSION_QUIZ_COUNT, count).apply()
            }
        }

        // Check if all missions completed → bonus XP
        checkAllMissionsCompleted()
    }

    private fun checkAllMissionsCompleted() {
        val missions = getDailyMissions()
        if (missions.all { it.isCompleted }) {
            val alreadyAwarded = prefs.getBoolean("missions_bonus_${todayDateString()}", false)
            if (!alreadyAwarded) {
                awardXP(XP_ALL_MISSIONS, "All Missions Complete!")
                prefs.edit().putBoolean("missions_bonus_${todayDateString()}", true).apply()
            }
        }
    }

    fun areAllMissionsCompleted(): Boolean = getDailyMissions().all { it.isCompleted }

    fun getCompletedMissionCount(): Int = getDailyMissions().count { it.isCompleted }
}
