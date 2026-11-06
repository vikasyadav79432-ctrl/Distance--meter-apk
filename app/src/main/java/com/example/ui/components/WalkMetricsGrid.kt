package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsWalk
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.location.HaversineCalculator
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import java.util.Locale

@Composable
fun WalkMetricsGrid(
    elapsedSeconds: Long,
    currentSpeedKmh: Float,
    avgSpeedKmh: Float,
    steps: Int,
    isHardwareStepSensor: Boolean,
    paceString: String,
    calories: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Row 1: Time & Current Speed
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                icon = Icons.Default.Timer,
                iconTint = Color(0xFFFBBF24),
                label = "WALKING TIME",
                primaryValue = HaversineCalculator.formatDuration(elapsedSeconds),
                subValue = if (elapsedSeconds == 0L) "Not started" else "Active time",
                isMonospace = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_walking_time")
            )

            MetricCard(
                icon = Icons.Default.Speed,
                iconTint = NeonGreen,
                label = "CURRENT SPEED",
                primaryValue = String.format(Locale.US, "%.1f", currentSpeedKmh),
                unit = "km/h",
                subValue = String.format(Locale.US, "Avg: %.1f km/h", avgSpeedKmh),
                isMonospace = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_walking_speed")
            )
        }

        // Row 2: Total Steps & Pace/Calories
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            MetricCard(
                icon = Icons.Default.DirectionsWalk,
                iconTint = ElectricBlue,
                label = "TOTAL STEPS",
                primaryValue = String.format(Locale.US, "%,d", steps),
                unit = "steps",
                subValue = if (isHardwareStepSensor) "Hardware Sensor" else "Stride Estimated",
                isMonospace = true,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_total_steps")
            )

            MetricCard(
                icon = Icons.Default.LocalFireDepartment,
                iconTint = Color(0xFFFF6D00),
                label = "PACE & CALORIES",
                primaryValue = paceString,
                subValue = String.format(Locale.US, "%d kcal burned", calories),
                isMonospace = false,
                modifier = Modifier
                    .weight(1f)
                    .testTag("metric_pace_calories")
            )
        }
    }
}

@Composable
private fun MetricCard(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    primaryValue: String,
    unit: String? = null,
    subValue: String,
    isMonospace: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(SurfaceElevated)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column {
            // Label & Icon Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = label,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Primary Readout
            Row(
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = primaryValue,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = if (isMonospace) FontFamily.Monospace else FontFamily.SansSerif,
                    color = TextPrimary
                )
                if (unit != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = unit,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Subtitle
            Text(
                text = subValue,
                fontSize = 11.sp,
                color = TextMuted,
                fontWeight = FontWeight.Normal
            )
        }
    }
}
