package com.example.ui.components

import android.content.Intent
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.GpsNotFixed
import androidx.compose.material.icons.filled.GpsOff
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.GpsAccuracyLevel
import com.example.ui.theme.BrightLime
import com.example.ui.theme.CrimsonStop
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun GpsStatusBadge(
    accuracyLevel: GpsAccuracyLevel,
    accuracyMeters: Float,
    isGpsHardwareEnabled: Boolean,
    isSimulating: Boolean,
    filteredPoints: Int,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Warning if GPS disabled
        AnimatedVisibility(
            visible = !isGpsHardwareEnabled && !isSimulating,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF331515))
                    .border(1.dp, CrimsonStop.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WarningAmber,
                        contentDescription = "GPS Disabled",
                        tint = CrimsonStop,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Device GPS is turned OFF. Enable location for meter tracking.",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                }
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
                        } catch (e: Exception) {
                            // Ignored
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonStop),
                    shape = RoundedCornerShape(8.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    modifier = Modifier.testTag("enable_gps_button")
                ) {
                    Text("Turn On", fontSize = 11.sp, color = Color.White)
                }
            }
        }

        // Live GPS Pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(SurfaceElevated)
                .border(1.dp, SurfaceBorder, RoundedCornerShape(14.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Signal Icon & Indicator
                val (tintColor, icon) = when {
                    isSimulating -> Pair(ElectricBlue, Icons.Default.GpsFixed)
                    accuracyLevel == GpsAccuracyLevel.EXCELLENT -> Pair(NeonGreen, Icons.Default.GpsFixed)
                    accuracyLevel == GpsAccuracyLevel.GOOD -> Pair(BrightLime, Icons.Default.GpsFixed)
                    accuracyLevel == GpsAccuracyLevel.FAIR -> Pair(Color(0xFFFBBF24), Icons.Default.GpsNotFixed)
                    accuracyLevel == GpsAccuracyLevel.POOR -> Pair(CrimsonStop, Icons.Default.GpsNotFixed)
                    else -> Pair(TextMuted, Icons.Default.GpsOff)
                }

                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(tintColor)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Icon(
                    imageVector = icon,
                    contentDescription = "GPS Status",
                    tint = tintColor,
                    modifier = Modifier.size(16.dp)
                )

                Spacer(modifier = Modifier.width(6.dp))

                Text(
                    text = if (isSimulating) "Demo Walk Simulator" else accuracyLevel.label,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    style = MaterialTheme.typography.labelMedium
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (accuracyMeters > 0f) {
                    Text(
                        text = "±${String.format("%.1f", accuracyMeters)}m",
                        color = TextSecondary,
                        fontSize = 11.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                if (filteredPoints > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Filtered: $filteredPoints",
                        color = TextMuted,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}
