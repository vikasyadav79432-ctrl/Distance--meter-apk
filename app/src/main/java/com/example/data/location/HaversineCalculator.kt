package com.example.data.location

import android.location.Location
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object HaversineCalculator {

    private const val EARTH_RADIUS_METERS = 6371000.0

    /**
     * Calculates great-circle distance between two GPS coordinates using the Haversine formula.
     * @param lat1 Latitude of point 1 in degrees
     * @param lon1 Longitude of point 1 in degrees
     * @param lat2 Latitude of point 2 in degrees
     * @param lon2 Longitude of point 2 in degrees
     * @return Distance in meters
     */
    fun calculateHaversineDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Double {
        val latDistance = Math.toRadians(lat2 - lat1)
        val lonDistance = Math.toRadians(lon2 - lon1)

        val a = sin(latDistance / 2) * sin(latDistance / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(lonDistance / 2) * sin(lonDistance / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return EARTH_RADIUS_METERS * c
    }

    /**
     * Uses Android Location's native distanceBetween for verification/fallback.
     */
    fun calculateAndroidLocationDistanceMeters(
        lat1: Double,
        lon1: Double,
        lat2: Double,
        lon2: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lon1, lat2, lon2, results)
        return results[0]
    }

    /**
     * Validates whether a new GPS fix is accurate enough to be accepted into distance calculation.
     * Prevents GPS jitter/drift when stationary or in poor signal conditions.
     */
    fun isValidGpsReading(
        newLat: Double,
        newLon: Double,
        newAccuracy: Float,
        newTime: Long,
        lastLat: Double?,
        lastLon: Double?,
        lastTime: Long?,
        maxAccuracyThreshold: Float = 25.0f
    ): Boolean {
        // Discard if accuracy exceeds threshold (too imprecise)
        if (newAccuracy > maxAccuracyThreshold || newAccuracy <= 0f) {
            return false
        }

        // First fix is always accepted as baseline if within accuracy
        if (lastLat == null || lastLon == null || lastTime == null) {
            return true
        }

        val distance = calculateHaversineDistanceMeters(lastLat, lastLon, newLat, newLon)
        val timeDeltaSeconds = (newTime - lastTime) / 1000.0

        // Discard tiny stationary jitter (< 0.4 meters)
        if (distance < 0.4) {
            return false
        }

        // Discard unrealistic movement spikes (walking speed rarely exceeds 4 m/s ~ 14.4 km/h; running 7 m/s)
        if (timeDeltaSeconds > 0.5) {
            val speedMps = distance / timeDeltaSeconds
            if (speedMps > 8.0) { // > 28.8 km/h is not walking
                return false
            }
        }

        return true
    }

    /**
     * Estimates calories burned based on walking distance and typical human metabolic rate.
     * Average walking burns ~0.05 - 0.055 kcal per meter for an average 70kg person.
     */
    fun calculateCalories(distanceMeters: Double, weightKg: Double = 70.0): Int {
        val kcalPerMeter = 0.00075 * weightKg
        return (distanceMeters * kcalPerMeter).toInt()
    }

    /**
     * Formats seconds into HH:MM:SS or MM:SS
     */
    fun formatDuration(totalSeconds: Long): String {
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60
        return if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Formats pace (e.g. 11'45" /km)
     */
    fun formatPace(distanceMeters: Double, durationSeconds: Long): String {
        if (distanceMeters < 20 || durationSeconds < 5) return "--'--\""
        val distanceKm = distanceMeters / 1000.0
        val secondsPerKm = (durationSeconds / distanceKm).toLong()
        if (secondsPerKm > 3600 || secondsPerKm <= 0) return "--'--\""
        val minutes = secondsPerKm / 60
        val seconds = secondsPerKm % 60
        return String.format(Locale.US, "%d'%02d\"", minutes, seconds)
    }

    /**
     * Formats distance in meters with thousands separator
     */
    fun formatMeters(distanceMeters: Double): String {
        return String.format(Locale.US, "%,.1f", distanceMeters)
    }

    /**
     * Formats distance in kilometers with 2 decimal places
     */
    fun formatKilometers(distanceMeters: Double): String {
        return String.format(Locale.US, "%.3f", distanceMeters / 1000.0)
    }
}
