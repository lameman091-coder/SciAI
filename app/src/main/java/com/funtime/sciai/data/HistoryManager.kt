package com.funtime.sciai.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class HistoryItem(
    val query: String,
    val mode: String,
    val domain: String,
    val timestamp: Long
)

object HistoryManager {
    private const val MAX_HISTORY = 20
    private const val FILE_NAME = "search_history.json"

    fun saveHistory(context: Context, item: HistoryItem) {
        val historyList = getHistory(context).toMutableList()

        // Avoid exact immediate back-to-back duplicates
        if (historyList.isNotEmpty() && historyList.first().query == item.query && historyList.first().mode == item.mode) {
            return
        }

        historyList.add(0, item) // Add to top

        if (historyList.size > MAX_HISTORY) {
            historyList.removeAt(historyList.lastIndex)
        }

        saveToFile(context, historyList)
    }

    fun getHistory(context: Context): List<HistoryItem> {
        val file = File(context.filesDir, FILE_NAME)
        if (!file.exists()) return emptyList()

        return try {
            val jsonString = file.readText()
            val jsonArray = JSONArray(jsonString)
            val list = mutableListOf<HistoryItem>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                list.add(
                    HistoryItem(
                        query = obj.getString("query"),
                        mode = obj.getString("mode"),
                        domain = obj.getString("domain"),
                        timestamp = obj.getLong("timestamp")
                    )
                )
            }
            list
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveToFile(context: Context, list: List<HistoryItem>) {
        try {
            val jsonArray = JSONArray()
            list.forEach { item ->
                val obj = JSONObject().apply {
                    put("query", item.query)
                    put("mode", item.mode)
                    put("domain", item.domain)
                    put("timestamp", item.timestamp)
                }
                jsonArray.put(obj)
            }
            File(context.filesDir, FILE_NAME).writeText(jsonArray.toString())
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
