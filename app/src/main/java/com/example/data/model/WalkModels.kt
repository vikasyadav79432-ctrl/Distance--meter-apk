package com.example.data.model

import org.json.JSONObject

data class WalkPoint(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val speed: Float,
    val altitude: Double,
    val timestamp: Long
) {
    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("latitude", latitude)
            put("longitude", longitude)
            put("accuracy", accuracy.toDouble())
            put("speed", speed.toDouble())
            put("altitude", altitude)
            put("timestamp", timestamp)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): WalkPoint {
            return WalkPoint(
                latitude = json.optDouble("latitude", 0.0),
                longitude = json.optDouble("longitude", 0.0),
                accuracy = json.optDouble("accuracy", 0.0).toFloat(),
                speed = json.optDouble("speed", 0.0).toFloat(),
                altitude = json.optDouble("altitude", 0.0),
                timestamp = json.optLong("timestamp", 0L)
            )
        }
    }
}

enum class GpsAccuracyLevel(val label: String) {
    NONE("No GPS Fix"),
    SEARCHING("Searching GPS..."),
    POOR("Poor Accuracy"),
    FAIR("Fair Accuracy"),
    GOOD("Good Accuracy"),
    EXCELLENT("High Precision")
}

enum class TrackingState {
    IDLE,
    TRACKING,
    PAUSED,
    FINISHED
}

enum class DistanceDisplayMode {
    METERS,
    KILOMETERS,
    BOTH
}

data class WalkSession(
    val id: Long,
    val dateFormatted: String,
    val distanceMeters: Double,
    val durationSeconds: Long,
    val steps: Int,
    val avgSpeedKmh: Float,
    val maxSpeedKmh: Float,
    val calories: Int,
    val pointsCount: Int
) {
    val distanceKm: Double get() = distanceMeters / 1000.0

    fun toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("dateFormatted", dateFormatted)
            put("distanceMeters", distanceMeters)
            put("durationSeconds", durationSeconds)
            put("steps", steps)
            put("avgSpeedKmh", avgSpeedKmh.toDouble())
            put("maxSpeedKmh", maxSpeedKmh.toDouble())
            put("calories", calories)
            put("pointsCount", pointsCount)
        }
    }

    companion object {
        fun fromJson(json: JSONObject): WalkSession {
            return WalkSession(
                id = json.optLong("id", System.currentTimeMillis()),
                dateFormatted = json.optString("dateFormatted", ""),
                distanceMeters = json.optDouble("distanceMeters", 0.0),
                durationSeconds = json.optLong("durationSeconds", 0L),
                steps = json.optInt("steps", 0),
                avgSpeedKmh = json.optDouble("avgSpeedKmh", 0.0).toFloat(),
                maxSpeedKmh = json.optDouble("maxSpeedKmh", 0.0).toFloat(),
                calories = json.optInt("calories", 0),
                pointsCount = json.optInt("pointsCount", 0)
            )
        }
    }
}
