package com.example.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.WalkPoint
import com.example.ui.theme.ElectricBlue
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.SurfaceBorder
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceElevated
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.max
import kotlin.math.min

@Composable
fun LiveRouteCanvas(
    routePoints: List<WalkPoint>,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseRadius by infiniteTransition.animateFloat(
        initialValue = 6f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400),
            repeatMode = RepeatMode.Restart
        ),
        label = "radarPulse"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(170.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(SurfaceDark)
            .border(1.dp, SurfaceBorder, RoundedCornerShape(18.dp))
            .padding(12.dp)
            .testTag("live_route_canvas")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Map,
                        contentDescription = "Walk Route Trail",
                        tint = ElectricBlue,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "LIVE BREADCRUMB ROUTE",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        letterSpacing = 0.5.sp
                    )
                }

                Text(
                    text = "${routePoints.size} waypoints",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Canvas Drawing
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF070B10)),
                contentAlignment = Alignment.Center
            ) {
                if (routePoints.size < 2) {
                    // Empty radar placeholder
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Canvas(modifier = Modifier.size(36.dp)) {
                            drawCircle(
                                color = NeonGreen.copy(alpha = 0.2f),
                                radius = size.minDimension / 2
                            )
                            drawCircle(
                                color = NeonGreen.copy(alpha = 0.5f),
                                radius = pulseRadius
                            )
                            drawCircle(
                                color = NeonGreen,
                                radius = 4f
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Walk outdoors to trace GPS route trail",
                            fontSize = 11.sp,
                            color = TextMuted
                        )
                    }
                } else {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val canvasWidth = size.width
                        val canvasHeight = size.height
                        val padding = 24f

                        // Draw subtle grid lines
                        val gridCols = 5
                        for (i in 1 until gridCols) {
                            val x = canvasWidth * (i.toFloat() / gridCols)
                            drawLine(
                                color = Color(0xFF131B26),
                                start = Offset(x, 0f),
                                end = Offset(x, canvasHeight),
                                strokeWidth = 1f
                            )
                        }
                        val gridRows = 3
                        for (i in 1 until gridRows) {
                            val y = canvasHeight * (i.toFloat() / gridRows)
                            drawLine(
                                color = Color(0xFF131B26),
                                start = Offset(0f, y),
                                end = Offset(canvasWidth, y),
                                strokeWidth = 1f
                            )
                        }

                        // Determine bounds
                        var minLat = Double.MAX_VALUE
                        var maxLat = -Double.MAX_VALUE
                        var minLon = Double.MAX_VALUE
                        var maxLon = -Double.MAX_VALUE

                        for (p in routePoints) {
                            if (p.latitude < minLat) minLat = p.latitude
                            if (p.latitude > maxLat) maxLat = p.latitude
                            if (p.longitude < minLon) minLon = p.longitude
                            if (p.longitude > maxLon) maxLon = p.longitude
                        }

                        val latRange = max(maxLat - minLat, 0.00008)
                        val lonRange = max(maxLon - minLon, 0.00008)

                        val drawW = canvasWidth - (padding * 2)
                        val drawH = canvasHeight - (padding * 2)

                        fun toOffset(pt: WalkPoint): Offset {
                            val normX = ((pt.longitude - minLon) / lonRange).toFloat()
                            val normY = ((maxLat - pt.latitude) / latRange).toFloat()
                            return Offset(
                                x = padding + (normX * drawW),
                                y = padding + (normY * drawH)
                            )
                        }

                        // Build path
                        val path = Path()
                        val startOffset = toOffset(routePoints.first())
                        path.moveTo(startOffset.x, startOffset.y)

                        for (i in 1 until routePoints.size) {
                            val offset = toOffset(routePoints[i])
                            path.lineTo(offset.x, offset.y)
                        }

                        // Draw path line with subtle glow
                        drawPath(
                            path = path,
                            color = NeonGreen.copy(alpha = 0.35f),
                            style = Stroke(width = 8f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )
                        drawPath(
                            path = path,
                            color = NeonGreen,
                            style = Stroke(width = 3.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
                        )

                        // Draw Start Marker
                        drawCircle(
                            color = Color(0xFF10B981),
                            radius = 6f,
                            center = startOffset
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5f,
                            center = startOffset
                        )

                        // Draw Current Position Pulsing Marker
                        val currentOffset = toOffset(routePoints.last())
                        drawCircle(
                            color = ElectricBlue.copy(alpha = 0.4f),
                            radius = pulseRadius,
                            center = currentOffset
                        )
                        drawCircle(
                            color = ElectricBlue,
                            radius = 6f,
                            center = currentOffset
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.5f,
                            center = currentOffset
                        )
                    }
                }
            }
        }
    }
}
