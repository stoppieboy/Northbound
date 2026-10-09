package com.truenorth.compass

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.SouthWest
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

private val Ink = Color(0xFF102D29)
private val Forest = Color(0xFF183D36)
private val Sage = Color(0xFF9EBAA6)
private val Paper = Color(0xFFF5F3EC)
private val Muted = Color(0xFF71827A)
private val Gold = Color(0xFFE3B979)

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private val _heading = mutableFloatStateOf(0f)
    private val _available = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        _available.value = sensor != null
        setContent { CompassScreen(_heading.floatValue, _available.value) }
    }

    override fun onResume() {
        super.onResume()
        sensor?.let { sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_UI) }
    }

    override fun onPause() {
        super.onPause()
        sensorManager.unregisterListener(this)
    }

    override fun onSensorChanged(event: SensorEvent) {
        val values = FloatArray(9)
        val orientation = FloatArray(3)
        if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(values, event.values)
            SensorManager.getOrientation(values, orientation)
            _heading.floatValue = (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
        } else {
            _heading.floatValue = (event.values[0] + 360f) % 360f
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
private fun CompassScreen(heading: Float, available: Boolean) {
    val animatedHeading by animateFloatAsState(
        targetValue = heading,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "compass-heading"
    )
    val cardinal = cardinal(heading)
    val degrees = heading.roundToInt().mod(360)

    Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(Ink), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Explore, null, tint = Gold, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("FIELD NOTES", fontSize = 10.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Northbound", fontSize = 20.sp, color = Ink, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(CircleShape).background(Color(0xFFE6EAE1)).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (available) Color(0xFF53836A) else Color(0xFFC37A62)))
                        Spacer(Modifier.width(7.dp))
                        Text(if (available) "SENSOR READY" else "NO SENSOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Ink)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Text("FIND YOUR WAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = Muted)
            Spacer(Modifier.height(6.dp))
            Text("Keep your bearings.", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = Ink, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(25.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = size.minDimension * .47f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(Brush.radialGradient(listOf(Color.White, Color(0xFFE8E8DF)), center, radius * 1.1f), radius * 1.1f)
                    drawCircle(Ink.copy(alpha = .08f), radius * 1.04f, center)
                    drawCircle(Brush.linearGradient(listOf(Forest, Ink)), radius, center)
                    drawCircle(Color.White.copy(alpha = .08f), radius - 5.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                    rotate(-animatedHeading, center) {
                        for (i in 0 until 72) {
                            val angle = Math.toRadians((i * 5 - 90).toDouble())
                            val major = i % 6 == 0
                            val r1 = radius * .88f
                            val r2 = radius * if (major) .78f else .83f
                            val start = Offset(center.x + kotlin.math.cos(angle).toFloat() * r1, center.y + kotlin.math.sin(angle).toFloat() * r1)
                            val end = Offset(center.x + kotlin.math.cos(angle).toFloat() * r2, center.y + kotlin.math.sin(angle).toFloat() * r2)
                            drawLine(if (major) Gold.copy(alpha = .9f) else Sage.copy(alpha = .55f), start, end, if (major) 2.dp.toPx() else 1.dp.toPx())
                        }
                        val north = Path().apply {
                            moveTo(center.x, center.y - radius * .67f)
                            lineTo(center.x - radius * .105f, center.y + radius * .035f)
                            quadraticTo(center.x, center.y - radius * .045f, center.x, center.y)
                            close()
                        }
                        drawPath(north, Gold)
                        val south = Path().apply {
                            moveTo(center.x, center.y + radius * .67f)
                            lineTo(center.x + radius * .105f, center.y - radius * .035f)
                            quadraticTo(center.x, center.y + radius * .045f, center.x, center.y)
                            close()
                        }
                        drawPath(south, Sage.copy(alpha = .8f))
                    }
                    drawCircle(Ink, radius * .055f, center)
                    drawCircle(Gold, radius * .023f, center)
                    // A fixed index at the top lets the rotating dial read against the phone's heading.
                    val tip = Path().apply {
                        moveTo(center.x, center.y - radius * .92f)
                        lineTo(center.x - radius * .055f, center.y - radius * .80f)
                        lineTo(center.x + radius * .055f, center.y - radius * .80f)
                        close()
                    }
                    drawPath(tip, Gold)
                }
                Column(Modifier.fillMaxWidth(.48f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$degrees°", fontSize = 55.sp, lineHeight = 58.sp, fontWeight = FontWeight.Light, color = Color.White, letterSpacing = (-2).sp)
                    Spacer(Modifier.height(1.dp))
                    Text(cardinal.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = Gold)
                }
                Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = Gold)
                Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sage)
                Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sage)
                Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Sage)
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Color.White).padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Readout("HEADING", "$degrees°", Icons.Default.SouthWest)
                Box(Modifier.width(1.dp).height(36.dp).background(Color(0xFFE9EBE5)))
                Readout("DIRECTION", cardinal, Icons.Default.Explore)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(Color(0xFFE8ECE5)).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.GpsFixed, null, tint = Forest, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (available) "Magnetic north" else "Compass sensor unavailable", fontSize = 12.sp, color = Ink, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(if (available) "LIVE" else "—", fontSize = 9.sp, color = Muted, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text("Hold your phone flat for the steadiest reading", fontSize = 11.sp, color = Muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Readout(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(Color(0xFFF0F2EB)), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Forest, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 9.sp, color = Muted, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(value, fontSize = 17.sp, color = Ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun cardinal(degrees: Float): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return directions[((degrees + 22.5f) / 45f).toInt().mod(8)]
}
