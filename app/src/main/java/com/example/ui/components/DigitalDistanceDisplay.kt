package com.example.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.HaversineCalculator
import com.example.data.model.DistanceDisplayMode
import com.example.data.model.TrackingState
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.NeonGreenGlow
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun DigitalDistanceDisplay(
    distanceMeters: Double,
    trackingState: TrackingState,
    displayMode: DistanceDisplayMode,
    onToggleDisplayMode: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTracking = trackingState == TrackingState.TRACKING

    // Pulsing glow when actively tracking
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = if (isTracking) 0.85f else 0.25f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    val borderBrush = if (isTracking) {
        Brush.linearGradient(
            colors = listOf(
                NeonGreen.copy(alpha = pulseAlpha),
                Color(0xFF00B0FF).copy(alpha = pulseAlpha * 0.7f),
                NeonGreen.copy(alpha = pulseAlpha)
            )
        )
    } else {
        Brush.linearGradient(
            colors = listOf(SurfaceBorder, SurfaceBorder)
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SurfaceElevated,
                        SurfaceDark
                    )
                )
            )
            .border(1.5.dp, borderBrush, RoundedCornerShape(24.dp))
            .clickable(onClick = onToggleDisplayMode)
            .padding(vertical = 20.dp, horizontal = 16.dp)
            .testTag("digital_distance_display"),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Header Pill: Status & Unit Switcher
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
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(
                                when (trackingState) {
                                    TrackingState.TRACKING -> NeonGreen
                                    TrackingState.PAUSED -> Color(0xFFFBBF24)
                                    TrackingState.FINISHED -> Color(0xFF38BDF8)
                                    TrackingState.IDLE -> TextMuted
                                }
                            )
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = when (trackingState) {
                            TrackingState.TRACKING -> "LIVE TRACKING"
                            TrackingState.PAUSED -> "PAUSED"
                            TrackingState.FINISHED -> "WALK FINISHED"
                            TrackingState.IDLE -> "READY"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = when (trackingState) {
                            TrackingState.TRACKING -> NeonGreen
                            TrackingState.PAUSED -> Color(0xFFFBBF24)
                            TrackingState.FINISHED -> Color(0xFF38BDF8)
                            TrackingState.IDLE -> TextMuted
                        },
                        letterSpacing = 1.sp
                    )
                }

                // Switcher hint
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF1E293B))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
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
                            DistanceDisplayMode.METERS -> "Meters"
                            DistanceDisplayMode.KILOMETERS -> "Kilometers"
                            DistanceDisplayMode.BOTH -> "Dual (m / km)"
                        },
                        fontSize = 11.sp,
                        color = TextSecondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Primary Huge Digital Numbers
            AnimatedContent(
                targetState = Pair(distanceMeters, displayMode),
                transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(150)) },
                label = "primaryDistance"
            ) { (_, mode) ->
                when (mode) {
                    DistanceDisplayMode.METERS, DistanceDisplayMode.BOTH -> {
                        // Main readout in Meters
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = HaversineCalculator.formatMeters(distanceMeters),
                                fontSize = if (distanceMeters >= 10000) 46.sp else 54.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen,
                                letterSpacing = (-1).sp,
                                modifier = Modifier.testTag("distance_meters_text")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "m",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                    DistanceDisplayMode.KILOMETERS -> {
                        // Main readout in Kilometers
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = HaversineCalculator.formatKilometers(distanceMeters),
                                fontSize = 54.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = NeonGreen,
                                letterSpacing = (-1).sp,
                                modifier = Modifier.testTag("distance_km_text")
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "km",
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = TextSecondary,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                        }
                    }
                }
            }

            // Secondary Equivalent (Dual view or Alternate)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                when (displayMode) {
                    DistanceDisplayMode.METERS -> {
                        Text(
                            text = "= ${HaversineCalculator.formatKilometers(distanceMeters)} km",
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    DistanceDisplayMode.KILOMETERS -> {
                        Text(
                            text = "= ${HaversineCalculator.formatMeters(distanceMeters)} m",
                            fontSize = 15.sp,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    DistanceDisplayMode.BOTH -> {
                        Text(
                            text = "${HaversineCalculator.formatKilometers(distanceMeters)} km",
                            fontSize = 18.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}
