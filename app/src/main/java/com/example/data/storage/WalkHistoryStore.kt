package com.example.data.storage

import android.content.Context
import android.content.SharedPreferences
import com.example.data.model.WalkSession
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class WalkHistoryStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("walk_meter_history", Context.MODE_PRIVATE)

    private val keyHistory = "history_sessions"
    private val keyKeepScreenOn = "pref_keep_screen_on"
    private val keyFilterStrictness = "pref_filter_strictness"
    private val keyUnitMode = "pref_unit_mode"

    fun getAllSessions(): List<WalkSession> {
        val rawJson = prefs.getString(keyHistory, null) ?: return emptyList()
        return try {
            val array = JSONArray(rawJson)
            val list = mutableListOf<WalkSession>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(WalkSession.fromJson(obj))
            }
            list.sortedByDescending { it.id }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun saveSession(session: WalkSession) {
        val current = getAllSessions().toMutableList()
        current.add(0, session)
        persistSessions(current)
    }

    fun deleteSession(sessionId: Long) {
        val current = getAllSessions().filter { it.id != sessionId }
        persistSessions(current)
    }

    fun clearAll() {
        prefs.edit().remove(keyHistory).apply()
    }

    private fun persistSessions(list: List<WalkSession>) {
        val array = JSONArray()
        list.forEach { session ->
            array.put(session.toJson())
        }
        prefs.edit().putString(keyHistory, array.toString()).apply()
    }

    // Preferences
    var isKeepScreenOnEnabled: Boolean
        get() = prefs.getBoolean(keyKeepScreenOn, true)
        set(value) = prefs.edit().putBoolean(keyKeepScreenOn, value).apply()

    var filterAccuracyStrictness: Int
        get() = prefs.getInt(keyFilterStrictness, 20) // 20 meters default
        set(value) = prefs.edit().putInt(keyFilterStrictness, value).apply()

    var unitDisplayModeIndex: Int
        get() = prefs.getInt(keyUnitMode, 0)
        set(value) = prefs.edit().putInt(keyUnitMode, value).apply()

    companion object {
        fun formatCurrentDate(): String {
            val sdf = SimpleDateFormat("MMM d, yyyy • h:mm a", Locale.getDefault())
            return sdf.format(Date())
        }
    }
}
