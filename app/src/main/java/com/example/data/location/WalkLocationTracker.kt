package com.example.data.location

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import com.example.data.model.GpsAccuracyLevel
import com.example.data.model.WalkPoint
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

class WalkLocationTracker(private val context: Context) : SensorEventListener {

    private val tag = "WalkLocationTracker"
    private val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)
    private val locationManager =
        context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager
    private val sensorManager =
        context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager

    private var locationCallback: LocationCallback? = null
    private var isTracking = false
    private var isPaused = false

    // Step counting
    private var initialStepCount: Int? = null
    private var stepCountSensor: Sensor? = null
    private var stepDetectorSensor: Sensor? = null
    private var sensorStepsRecorded = 0

    // Flows
    private val _locationFlow = MutableSharedFlow<WalkPoint>(extraBufferCapacity = 64)
    val locationFlow: SharedFlow<WalkPoint> = _locationFlow.asSharedFlow()

    private val _stepFlow = MutableSharedFlow<Int>(extraBufferCapacity = 64)
    val stepFlow: SharedFlow<Int> = _stepFlow.asSharedFlow()

    private val _accuracyLevel = MutableStateFlow(GpsAccuracyLevel.NONE)
    val accuracyLevel: StateFlow<GpsAccuracyLevel> = _accuracyLevel.asStateFlow()

    private val _currentAccuracyMeters = MutableStateFlow(0f)
    val currentAccuracyMeters: StateFlow<Float> = _currentAccuracyMeters.asStateFlow()

    // Simulation Support
    private var simulationJob: Job? = null
    private val coroutineScope = CoroutineScope(Dispatchers.Default)

    init {
        stepCountSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_COUNTER)
        stepDetectorSensor = sensorManager?.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
    }

    val isStepCounterAvailable: Boolean
        get() = stepCountSensor != null || stepDetectorSensor != null

    fun isGpsEnabled(): Boolean {
        return try {
            locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
                    locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        } catch (e: Exception) {
            false
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (isTracking) return
        isTracking = true
        isPaused = false
        initialStepCount = null
        sensorStepsRecorded = 0

        _accuracyLevel.value = GpsAccuracyLevel.SEARCHING

        // Register step sensors
        stepCountSensor?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
        }
        if (stepCountSensor == null) {
            stepDetectorSensor?.let { sensor ->
                sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_UI)
            }
        }

        // Configure high accuracy location request with fast step sensitivity
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L)
            .setMinUpdateIntervalMillis(600L)
            .setMinUpdateDistanceMeters(0.5f)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (isPaused) return
                for (location in result.locations) {
                    processNewLocation(location)
                }
            }
        }

        try {
            locationCallback?.let { cb ->
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    cb,
                    Looper.getMainLooper()
                )
            }
            // Fetch last known location for quick immediate fix
            fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
                if (loc != null && !isPaused && isTracking) {
                    processNewLocation(loc)
                }
            }
        } catch (e: SecurityException) {
            Log.e(tag, "Location permission not granted", e)
            _accuracyLevel.value = GpsAccuracyLevel.NONE
        }
    }

    fun pauseTracking() {
        isPaused = true
    }

    fun resumeTracking() {
        isPaused = false
    }

    fun stopTracking() {
        isTracking = false
        isPaused = false
        stopSimulation()

        locationCallback?.let {
            try {
                fusedLocationClient.removeLocationUpdates(it)
            } catch (e: Exception) {
                Log.e(tag, "Error removing location updates", e)
            }
        }
        locationCallback = null

        try {
            sensorManager?.unregisterListener(this)
        } catch (e: Exception) {
            Log.e(tag, "Error unregistering sensor", e)
        }

        _accuracyLevel.value = GpsAccuracyLevel.NONE
        _currentAccuracyMeters.value = 0f
    }

    private fun processNewLocation(location: Location) {
        val accuracy = location.accuracy
        _currentAccuracyMeters.value = accuracy

        val level = when {
            accuracy <= 0f -> GpsAccuracyLevel.SEARCHING
            accuracy <= 6.0f -> GpsAccuracyLevel.EXCELLENT
            accuracy <= 12.0f -> GpsAccuracyLevel.GOOD
            accuracy <= 25.0f -> GpsAccuracyLevel.FAIR
            else -> GpsAccuracyLevel.POOR
        }
        _accuracyLevel.value = level

        val point = WalkPoint(
            latitude = location.latitude,
            longitude = location.longitude,
            accuracy = accuracy,
            speed = if (location.hasSpeed()) location.speed else 0f,
            altitude = if (location.hasAltitude()) location.altitude else 0.0,
            timestamp = location.time
        )
        _locationFlow.tryEmit(point)
    }

    // Step Sensor listener
    override fun onSensorChanged(event: SensorEvent?) {
        if (!isTracking || isPaused || event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_STEP_COUNTER -> {
                val totalSteps = event.values[0].toInt()
                if (initialStepCount == null) {
                    initialStepCount = totalSteps
                }
                val delta = totalSteps - (initialStepCount ?: totalSteps)
                if (delta >= 0) {
                    sensorStepsRecorded = delta
                    _stepFlow.tryEmit(sensorStepsRecorded)
                }
            }
            Sensor.TYPE_STEP_DETECTOR -> {
                if (event.values[0] == 1.0f) {
                    sensorStepsRecorded++
                    _stepFlow.tryEmit(sensorStepsRecorded)
                }
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // ----------------------------------------------------
    // Realistic Walk Simulation (For Emulators & Indoors)
    // ----------------------------------------------------
    fun startSimulation(startLat: Double = 37.7749, startLon: Double = -122.4194) {
        stopSimulation()
        isTracking = true
        isPaused = false
        _accuracyLevel.value = GpsAccuracyLevel.EXCELLENT
        _currentAccuracyMeters.value = 3.5f

        simulationJob = coroutineScope.launch {
            var currentLat = startLat
            var currentLon = startLon
            var headingDegrees = 45.0
            var simSteps = 0

            while (isActive && isTracking) {
                if (!isPaused) {
                    // Realistic walk speed: 1.25 m/s (~4.5 km/h) with subtle variation
                    val stepDistanceMeters = 1.25 + (Math.random() - 0.5) * 0.3
                    val speedMps = (stepDistanceMeters).toFloat()

                    // Slight natural curve in direction
                    headingDegrees += (Math.random() - 0.5) * 8.0
                    val headingRad = Math.toRadians(headingDegrees)

                    // 1 meter in latitude is approx 1 / 111,111 degrees
                    val dLat = (stepDistanceMeters * cos(headingRad)) / 111111.0
                    val dLon = (stepDistanceMeters * sin(headingRad)) / (111111.0 * cos(Math.toRadians(currentLat)))

                    currentLat += dLat
                    currentLon += dLon
                    simSteps += 2 // ~2 steps per meter/second

                    val point = WalkPoint(
                        latitude = currentLat,
                        longitude = currentLon,
                        accuracy = 3.2f + (Math.random() * 1.5).toFloat(),
                        speed = speedMps,
                        altitude = 42.0 + Math.sin(simSteps * 0.05) * 2.0,
                        timestamp = System.currentTimeMillis()
                    )
                    _locationFlow.tryEmit(point)
                    _stepFlow.tryEmit(simSteps)
                }
                delay(1000L)
            }
        }
    }

    fun stopSimulation() {
        simulationJob?.cancel()
        simulationJob = null
    }
}
