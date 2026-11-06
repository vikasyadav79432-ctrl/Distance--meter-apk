package com.example.ui.viewmodel

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.location.HaversineCalculator
import com.example.data.location.WalkLocationTracker
import com.example.data.model.DistanceDisplayMode
import com.example.data.model.GpsAccuracyLevel
import com.example.data.model.TrackingState
import com.example.data.model.WalkPoint
import com.example.data.model.WalkSession
import com.example.data.storage.WalkHistoryStore
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max

data class WalkUiState(
    val trackingState: TrackingState = TrackingState.IDLE,
    val distanceMeters: Double = 0.0,
    val elapsedSeconds: Long = 0L,
    val currentSpeedKmh: Float = 0f,
    val avgSpeedKmh: Float = 0f,
    val maxSpeedKmh: Float = 0f,
    val steps: Int = 0,
    val isHardwareStepSensor: Boolean = false,
    val calories: Int = 0,
    val paceString: String = "--'--\"",
    val gpsAccuracyLevel: GpsAccuracyLevel = GpsAccuracyLevel.NONE,
    val currentAccuracyMeters: Float = 0f,
    val routePoints: List<WalkPoint> = emptyList(),
    val displayMode: DistanceDisplayMode = DistanceDisplayMode.BOTH,
    val keepScreenOn: Boolean = true,
    val filterStrictness: Int = 25,
    val isSimulating: Boolean = false,
    val showSummaryDialog: Boolean = false,
    val lastFinishedSession: WalkSession? = null,
    val historyList: List<WalkSession> = emptyList(),
    val isGpsHardwareEnabled: Boolean = true,
    val totalPointsFilteredOut: Int = 0
)

class WalkMeterViewModel(application: Application) : AndroidViewModel(application) {

    private val tracker = WalkLocationTracker(application)
    private val historyStore = WalkHistoryStore(application)

    private val _uiState = MutableStateFlow(
        WalkUiState(
            keepScreenOn = historyStore.isKeepScreenOnEnabled,
            filterStrictness = historyStore.filterAccuracyStrictness,
            displayMode = DistanceDisplayMode.entries.getOrElse(historyStore.unitDisplayModeIndex) { DistanceDisplayMode.BOTH },
            historyList = historyStore.getAllSessions(),
            isHardwareStepSensor = tracker.isStepCounterAvailable,
            isGpsHardwareEnabled = tracker.isGpsEnabled()
        )
    )
    val uiState: StateFlow<WalkUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null
    private var lastRecordedPoint: WalkPoint? = null
    private var sessionStartTimeMs: Long = 0L
    private var hardwareStepsOffset = 0
    private var lastHardwareSteps = 0

    init {
        // Collect location updates from tracker
        viewModelScope.launch {
            tracker.locationFlow.collect { point ->
                handleNewGpsPoint(point)
            }
        }

        // Collect step counter updates from hardware sensor
        viewModelScope.launch {
            tracker.stepFlow.collect { stepsDelta ->
                lastHardwareSteps = stepsDelta
                if (_uiState.value.trackingState == TrackingState.TRACKING) {
                    _uiState.update { it.copy(steps = stepsDelta, isHardwareStepSensor = true) }
                }
            }
        }

        // Collect GPS accuracy states
        viewModelScope.launch {
            tracker.accuracyLevel.collect { level ->
                _uiState.update { it.copy(gpsAccuracyLevel = level) }
            }
        }

        viewModelScope.launch {
            tracker.currentAccuracyMeters.collect { acc ->
                _uiState.update { it.copy(currentAccuracyMeters = acc) }
            }
        }
    }

    fun checkGpsStatus() {
        val enabled = tracker.isGpsEnabled()
        _uiState.update { it.copy(isGpsHardwareEnabled = enabled) }
    }

    fun startTracking(isSimulation: Boolean = false) {
        if (_uiState.value.trackingState == TrackingState.TRACKING) return

        val activeSimulation = isSimulation || com.example.data.location.EmulatorDetector.isRunningOnEmulator()
        sessionStartTimeMs = System.currentTimeMillis()
        lastRecordedPoint = null
        hardwareStepsOffset = 0

        _uiState.update {
            it.copy(
                trackingState = TrackingState.TRACKING,
                isSimulating = activeSimulation,
                distanceMeters = 0.0,
                elapsedSeconds = 0L,
                currentSpeedKmh = 0f,
                avgSpeedKmh = 0f,
                maxSpeedKmh = 0f,
                steps = 0,
                calories = 0,
                paceString = "--'--\"",
                routePoints = emptyList(),
                showSummaryDialog = false,
                lastFinishedSession = null,
                totalPointsFilteredOut = 0
            )
        }

        if (activeSimulation) {
            tracker.startSimulation()
        } else {
            tracker.startTracking()
        }

        startTimer()
    }

    fun pauseTracking() {
        if (_uiState.value.trackingState != TrackingState.TRACKING) return
        _uiState.update { it.copy(trackingState = TrackingState.PAUSED, currentSpeedKmh = 0f) }
        tracker.pauseTracking()
        pauseTimer()
    }

    fun resumeTracking() {
        if (_uiState.value.trackingState != TrackingState.PAUSED) return
        _uiState.update { it.copy(trackingState = TrackingState.TRACKING) }
        tracker.resumeTracking()
        startTimer()
    }

    fun stopTracking() {
        if (_uiState.value.trackingState == TrackingState.IDLE) return

        pauseTimer()
        tracker.stopTracking()

        val current = _uiState.value
        val session = WalkSession(
            id = System.currentTimeMillis(),
            dateFormatted = WalkHistoryStore.formatCurrentDate(),
            distanceMeters = current.distanceMeters,
            durationSeconds = current.elapsedSeconds,
            steps = current.steps,
            avgSpeedKmh = current.avgSpeedKmh,
            maxSpeedKmh = current.maxSpeedKmh,
            calories = current.calories,
            pointsCount = current.routePoints.size
        )

        _uiState.update {
            it.copy(
                trackingState = TrackingState.FINISHED,
                currentSpeedKmh = 0f,
                lastFinishedSession = session,
                showSummaryDialog = current.distanceMeters > 5.0 || current.elapsedSeconds > 10
            )
        }
    }

    fun resetMeter() {
        pauseTimer()
        tracker.stopTracking()
        lastRecordedPoint = null

        _uiState.update {
            it.copy(
                trackingState = TrackingState.IDLE,
                distanceMeters = 0.0,
                elapsedSeconds = 0L,
                currentSpeedKmh = 0f,
                avgSpeedKmh = 0f,
                maxSpeedKmh = 0f,
                steps = 0,
                calories = 0,
                paceString = "--'--\"",
                routePoints = emptyList(),
                showSummaryDialog = false,
                lastFinishedSession = null,
                isSimulating = false,
                totalPointsFilteredOut = 0
            )
        }
    }

    fun saveCurrentWalk() {
        val session = _uiState.value.lastFinishedSession ?: return
        historyStore.saveSession(session)
        _uiState.update {
            it.copy(
                historyList = historyStore.getAllSessions(),
                showSummaryDialog = false
            )
        }
    }

    fun dismissSummaryDialog() {
        _uiState.update { it.copy(showSummaryDialog = false) }
    }

    private fun handleNewGpsPoint(point: WalkPoint) {
        if (_uiState.value.trackingState != TrackingState.TRACKING) return

        val currentState = _uiState.value
        val isValid = HaversineCalculator.isValidGpsReading(
            newLat = point.latitude,
            newLon = point.longitude,
            newAccuracy = point.accuracy,
            newTime = point.timestamp,
            lastLat = lastRecordedPoint?.latitude,
            lastLon = lastRecordedPoint?.longitude,
            lastTime = lastRecordedPoint?.timestamp,
            maxAccuracyThreshold = currentState.filterStrictness.toFloat()
        )

        if (!isValid) {
            _uiState.update { it.copy(totalPointsFilteredOut = it.totalPointsFilteredOut + 1) }
            return
        }

        var addedDistance = 0.0
        val last = lastRecordedPoint
        if (last != null) {
            addedDistance = HaversineCalculator.calculateHaversineDistanceMeters(
                last.latitude,
                last.longitude,
                point.latitude,
                point.longitude
            )
        }

        lastRecordedPoint = point

        val newTotalDistance = currentState.distanceMeters + addedDistance
        val totalSeconds = currentState.elapsedSeconds

        // Calculate speed in km/h
        val instantaneousSpeedKmh = if (point.speed > 0f) {
            point.speed * 3.6f
        } else if (last != null && addedDistance > 0) {
            val deltaSec = max(1.0, (point.timestamp - last.timestamp) / 1000.0)
            ((addedDistance / deltaSec) * 3.6).toFloat()
        } else {
            0f
        }

        val avgSpeedKmh = if (totalSeconds > 0) {
            ((newTotalDistance / totalSeconds) * 3.6).toFloat()
        } else {
            0f
        }

        val maxSpeed = max(currentState.maxSpeedKmh, instantaneousSpeedKmh)
        val calories = HaversineCalculator.calculateCalories(newTotalDistance)
        val pace = HaversineCalculator.formatPace(newTotalDistance, totalSeconds)

        // Estimated steps if device lacks step sensor
        val currentSteps = if (currentState.isHardwareStepSensor) {
            currentState.steps
        } else {
            // Average step stride length is approx 0.762 meters (2.5 feet)
            (newTotalDistance / 0.762).toInt()
        }

        val updatedPoints = if (currentState.routePoints.size > 800) {
            currentState.routePoints.takeLast(700) + point
        } else {
            currentState.routePoints + point
        }

        _uiState.update {
            it.copy(
                distanceMeters = newTotalDistance,
                currentSpeedKmh = instantaneousSpeedKmh,
                avgSpeedKmh = avgSpeedKmh,
                maxSpeedKmh = maxSpeed,
                calories = calories,
                paceString = pace,
                steps = currentSteps,
                routePoints = updatedPoints
            )
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isActive) {
                delay(1000L)
                if (_uiState.value.trackingState == TrackingState.TRACKING) {
                    _uiState.update { state ->
                        val newSeconds = state.elapsedSeconds + 1
                        val avgSpd = if (newSeconds > 0) {
                            ((state.distanceMeters / newSeconds) * 3.6).toFloat()
                        } else 0f
                        val pace = HaversineCalculator.formatPace(state.distanceMeters, newSeconds)
                        state.copy(
                            elapsedSeconds = newSeconds,
                            avgSpeedKmh = avgSpd,
                            paceString = pace
                        )
                    }
                }
            }
        }
    }

    private fun pauseTimer() {
        timerJob?.cancel()
        timerJob = null
    }

    fun toggleDisplayMode() {
        val nextMode = when (_uiState.value.displayMode) {
            DistanceDisplayMode.BOTH -> DistanceDisplayMode.METERS
            DistanceDisplayMode.METERS -> DistanceDisplayMode.KILOMETERS
            DistanceDisplayMode.KILOMETERS -> DistanceDisplayMode.BOTH
        }
        historyStore.unitDisplayModeIndex = nextMode.ordinal
        _uiState.update { it.copy(displayMode = nextMode) }
    }

    fun setFilterStrictness(meters: Int) {
        historyStore.filterAccuracyStrictness = meters
        _uiState.update { it.copy(filterStrictness = meters) }
    }

    fun toggleKeepScreenOn(enabled: Boolean) {
        historyStore.isKeepScreenOnEnabled = enabled
        _uiState.update { it.copy(keepScreenOn = enabled) }
    }

    fun deleteHistoryItem(id: Long) {
        historyStore.deleteSession(id)
        _uiState.update { it.copy(historyList = historyStore.getAllSessions()) }
    }

    fun clearHistory() {
        historyStore.clearAll()
        _uiState.update { it.copy(historyList = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        pauseTimer()
        tracker.stopTracking()
    }
}
