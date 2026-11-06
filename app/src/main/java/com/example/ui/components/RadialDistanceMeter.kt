package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SyncAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.HaversineCalculator
import com.example.data.model.DistanceDisplayMode
import com.example.data.model.TrackingState
import com.example.ui.theme.BrightLime
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RadialDistanceMeter(
    distanceMeters: Double,
    currentSpeedKmh: Float,
    trackingState: TrackingState,
    displayMode: DistanceDisplayMode,
    onToggleDisplayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTracking = trackingState == TrackingState.TRACKING

    // Pulsing transition for active tracking
    val infiniteTransition = rememberInfiniteTransition(label = "meterGlow")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = if (isTracking) 0.95f else 0.35f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseGlow"
    )

    // Gauge fraction: Speed maps from 0 to 8.0 km/h walking speed (or distance progress)
    val targetSpeedFraction = min(1f, currentSpeedKmh / 8.0f)
    val animatedSpeedFraction by animateFloatAsState(
        targetValue = if (isTracking) targetSpeedFraction else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "speedGauge"
    )

    // Distance progress towards 1km (1000m) cycle milestone
    val distanceCycleFraction = ((distanceMeters % 1000.0) / 1000.0).toFloat()
    val animatedDistanceFraction by animateFloatAsState(
        targetValue = distanceCycleFraction,
        animationSpec = tween(durationMillis = 500),
        label = "distanceProgress"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(SurfaceElevated, SurfaceDark)
                )
            )
            .border(1.5.dp, SurfaceBorder, RoundedCornerShape(28.dp))
            .padding(16.dp)
            .testTag("radial_distance_meter"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Pill: Status & Unit Toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Tracking Status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF0F1722))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (trackingState) {
                                    TrackingState.TRACKING -> NeonGreen
                                    TrackingState.PAUSED -> Color(0xFFFBBF24)
                                    TrackingState.FINISHED -> ElectricBlue
                                    TrackingState.IDLE -> TextMuted
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (trackingState) {
                            TrackingState.TRACKING -> "GPS METER ACTIVE"
                            TrackingState.PAUSED -> "METER PAUSED"
                            TrackingState.FINISHED -> "WALK COMPLETE"
                            TrackingState.IDLE -> "METER READY"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (trackingState) {
                            TrackingState.TRACKING -> NeonGreen
                            TrackingState.PAUSED -> Color(0xFFFBBF24)
                            TrackingState.FINISHED -> ElectricBlue
                            TrackingState.IDLE -> TextMuted
                        },
                        letterSpacing = 1.sp
                    )
                }

                // Interactive Unit Switcher Button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .clickable(onClick = onToggleDisplayMode)
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                        .testTag("unit_toggle_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.SyncAlt,
                        contentDescription = "Switch Unit",
                        tint = TextSecondary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = when (displayMode) {
                            DistanceDisplayMode.METERS -> "Unit: Meters"
                            DistanceDisplayMode.KILOMETERS -> "Unit: Km"
                            DistanceDisplayMode.BOTH -> "Unit: Dual"
                        },
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Circular Radial Dial Gauge
            Box(
                modifier = Modifier
                    .size(230.dp)
                    .clickable(onClick = onToggleDisplayMode),
                contentAlignment = Alignment.Center
            ) {
                // Canvas Drawing the Dial Arc & Ticks
                Canvas(modifier = Modifier.size(230.dp)) {
                    val strokeWidth = 14f
                    val arcSize = size.width - strokeWidth * 2
                    val arcOffset = Offset(strokeWidth, strokeWidth)
                    val startAngle = 140f
                    val sweepAngle = 260f

                    // 1. Background Arc Track
                    drawArc(
                        color = Color(0xFF16202E),
                        startAngle = startAngle,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        topLeft = arcOffset,
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // 2. Active Distance Progress Arc
                    val activeSweep = sweepAngle * animatedDistanceFraction
                    if (activeSweep > 0f) {
                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = listOf(
                                    Color(0xFF00B0FF),
                                    BrightLime,
                                    NeonGreen
                                ),
                                center = Offset(size.width / 2, size.height / 2)
                            ),
                            startAngle = startAngle,
                            sweepAngle = activeSweep,
                            useCenter = false,
                            topLeft = arcOffset,
                            size = Size(arcSize, arcSize),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    // 3. Dial Tick marks
                    val center = Offset(size.width / 2, size.height / 2)
                    val tickRadius = (size.width / 2) - strokeWidth - 10f
                    val tickCount = 13
                    for (i in 0 until tickCount) {
                        val angleDeg = startAngle + (i.toFloat() / (tickCount - 1)) * sweepAngle
                        val angleRad = Math.toRadians(angleDeg.toDouble())
                        val isMajor = i % 3 == 0
                        val tickLength = if (isMajor) 10f else 5f
                        val startX = (center.x + tickRadius * cos(angleRad)).toFloat()
                        val startY = (center.y + tickRadius * sin(angleRad)).toFloat()
                        val endX = (center.x + (tickRadius - tickLength) * cos(angleRad)).toFloat()
                        val endY = (center.y + (tickRadius - tickLength) * sin(angleRad)).toFloat()

                        drawLine(
                            color = if (isMajor) TextSecondary.copy(alpha = 0.6f) else TextMuted.copy(alpha = 0.3f),
                            start = Offset(startX, startY),
                            end = Offset(endX, endY),
                            strokeWidth = if (isMajor) 2.5f else 1.5f
                        )
                    }

                    // 4. Indicator Needle/Dot on the arc
                    val needleAngleDeg = startAngle + (sweepAngle * animatedSpeedFraction)
                    val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())
                    val needleRadius = (size.width / 2) - strokeWidth
                    val needleCenter = Offset(
                        (center.x + needleRadius * cos(needleAngleRad)).toFloat(),
                        (center.y + needleRadius * sin(needleAngleRad)).toFloat()
                    )

                    if (isTracking) {
                        drawCircle(
                            color = NeonGreen.copy(alpha = pulseGlow),
                            radius = 12f,
                            center = needleCenter
                        )
                    }
                    drawCircle(
                        color = Color.White,
                        radius = 5f,
                        center = needleCenter
                    )
                }

                // Inner Digital Readout Cluster
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(horizontal = 24.dp)
                ) {
                    Text(
                        text = "DISTANCE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 1.sp
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Big Distance Readout
                    AnimatedContent(
                        targetState = Pair(distanceMeters, displayMode),
                        transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                        label = "distanceCounter"
                    ) { (dist, mode) ->
                        when (mode) {
                            DistanceDisplayMode.METERS, DistanceDisplayMode.BOTH -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = HaversineCalculator.formatMeters(dist),
                                            fontSize = if (dist >= 10000) 32.sp else 38.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = NeonGreen,
                                            letterSpacing = (-1).sp
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "m",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(bottom = 5.dp)
                                        )
                                    }
                                    Text(
                                        text = "${HaversineCalculator.formatKilometers(dist)} km",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ElectricBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                            DistanceDisplayMode.KILOMETERS -> {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = HaversineCalculator.formatKilometers(dist),
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace,
                                            color = NeonGreen,
                                            letterSpacing = (-1).sp
                                        )
                                        Spacer(modifier = Modifier.width(3.dp))
                                        Text(
                                            text = "km",
                                            fontSize = 18.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = TextSecondary,
                                            modifier = Modifier.padding(bottom = 5.dp)
                                        )
                                    }
                                    Text(
                                        text = "${HaversineCalculator.formatMeters(dist)} m",
                                        fontSize = 14.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = ElectricBlue,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Speed readout inside dial
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF0F1823))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = String.format(Locale.US, "%.1f km/h", currentSpeedKmh),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = if (currentSpeedKmh > 0f) NeonGreen else TextMuted
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle helper
            Text(
                text = "Tap dial to toggle Meters / Kilometers",
                fontSize = 11.sp,
                color = TextMuted
            )
        }
    }
}
