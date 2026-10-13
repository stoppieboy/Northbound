package com.truenorth.compass

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
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
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LightMode
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

private enum class ThemeMode { SYSTEM, LIGHT, DARK }

private data class CompassPalette(
    val ink: Color,
    val forest: Color,
    val sage: Color,
    val background: Color,
    val muted: Color,
    val gold: Color,
    val headerChip: Color,
    val readout: Color,
    val divider: Color,
    val infoBackground: Color,
    val iconBackground: Color,
    val dialBackground: Color,
    val dialInk: Color
)

private val LightPalette = CompassPalette(
    ink = Color(0xFF102D29),
    forest = Color(0xFF183D36),
    sage = Color(0xFF9EBAA6),
    background = Color(0xFFF5F3EC),
    muted = Color(0xFF71827A),
    gold = Color(0xFFE3B979),
    headerChip = Color(0xFFE6EAE1),
    readout = Color.White,
    divider = Color(0xFFE9EBE5),
    infoBackground = Color(0xFFE8ECE5),
    iconBackground = Color(0xFFF0F2EB),
    dialBackground = Color(0xFFE8E8DF),
    dialInk = Color(0xFF102D29)
)

private val DarkPalette = CompassPalette(
    ink = Color(0xFFE6EEE8),
    forest = Color(0xFF183D36),
    sage = Color(0xFF9EBAA6),
    background = Color(0xFF111A17),
    muted = Color(0xFFA3B1A9),
    gold = Color(0xFFE3B979),
    headerChip = Color(0xFF26342D),
    readout = Color(0xFF1C2823),
    divider = Color(0xFF35423B),
    infoBackground = Color(0xFF24332C),
    iconBackground = Color(0xFF2A3931),
    dialBackground = Color(0xFF26332D),
    dialInk = Color(0xFF102D29)
)

class MainActivity : ComponentActivity(), SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var sensor: Sensor? = null
    private val _heading = mutableFloatStateOf(0f)
    private val _available = mutableStateOf(false)
    private val _themeMode = mutableStateOf(ThemeMode.SYSTEM)
    private var lastSensorHeading: Float? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _themeMode.value = getPreferences(MODE_PRIVATE).getString("theme_mode", null)
            ?.let { savedMode -> runCatching { ThemeMode.valueOf(savedMode) }.getOrNull() }
            ?: ThemeMode.SYSTEM
        applySystemBarStyle(isDarkTheme(_themeMode.value))
        sensorManager = getSystemService(SENSOR_SERVICE) as SensorManager
        sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
            ?: sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        _available.value = sensor != null
        setContent {
            CompassScreen(_heading.floatValue, _available.value, _themeMode.value) { mode ->
                _themeMode.value = mode
                getPreferences(MODE_PRIVATE).edit().putString("theme_mode", mode.name).apply()
                applySystemBarStyle(isDarkTheme(mode))
            }
        }
    }

    private fun isDarkTheme(mode: ThemeMode): Boolean = when (mode) {
        ThemeMode.SYSTEM -> resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK ==
            android.content.res.Configuration.UI_MODE_NIGHT_YES
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    private fun applySystemBarStyle(darkTheme: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (darkTheme) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
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
        val magneticHeading = if (event.sensor.type == Sensor.TYPE_ROTATION_VECTOR) {
            SensorManager.getRotationMatrixFromVector(values, event.values)
            SensorManager.getOrientation(values, orientation)
            (Math.toDegrees(orientation[0].toDouble()).toFloat() + 360f) % 360f
        } else {
            (event.values[0] + 360f) % 360f
        }
        val previous = lastSensorHeading
        if (previous == null) {
            _heading.floatValue = magneticHeading
        } else {
            var delta = magneticHeading - previous
            if (delta > 180f) delta -= 360f
            if (delta < -180f) delta += 360f
            _heading.floatValue += delta
        }
        lastSensorHeading = magneticHeading
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
}

@Composable
private fun CompassScreen(
    heading: Float,
    available: Boolean,
    themeMode: ThemeMode,
    onThemeModeChange: (ThemeMode) -> Unit
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> systemDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val palette = if (darkTheme) DarkPalette else LightPalette
    val animatedHeading by animateFloatAsState(
        targetValue = heading,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "compass-heading"
    )
    val displayHeading = ((heading % 360f) + 360f) % 360f
    val cardinal = cardinal(displayHeading)
    val degrees = displayHeading.roundToInt().mod(360)

    Surface(color = palette.background, modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(42.dp).clip(CircleShape).background(palette.ink), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Explore, null, tint = palette.gold, modifier = Modifier.size(22.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("FIELD NOTES", fontSize = 10.sp, color = palette.muted, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Text("Northbound", fontSize = 20.sp, color = palette.ink, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.weight(1f))
                Box(Modifier.clip(CircleShape).background(palette.headerChip).padding(horizontal = 12.dp, vertical = 8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(6.dp).clip(CircleShape).background(if (available) Color(0xFF53836A) else Color(0xFFC37A62)))
                        Spacer(Modifier.width(7.dp))
                        Text(if (available) "SENSOR READY" else "NO SENSOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = palette.ink)
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("FIND YOUR WAY", fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = palette.muted)
                Spacer(Modifier.weight(1f))
                val nextThemeMode = when (themeMode) {
                    ThemeMode.SYSTEM -> ThemeMode.LIGHT
                    ThemeMode.LIGHT -> ThemeMode.DARK
                    ThemeMode.DARK -> ThemeMode.SYSTEM
                }
                val themeDescription = when (themeMode) {
                    ThemeMode.SYSTEM -> "Theme: system. Switch to light theme"
                    ThemeMode.LIGHT -> "Theme: light. Switch to dark theme"
                    ThemeMode.DARK -> "Theme: dark. Switch to system theme"
                }
                IconButton(onClick = { onThemeModeChange(nextThemeMode) }) {
                    Icon(
                        imageVector = when (themeMode) {
                            ThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
                            ThemeMode.LIGHT -> Icons.Default.LightMode
                            ThemeMode.DARK -> Icons.Default.DarkMode
                        },
                        contentDescription = themeDescription,
                        tint = palette.ink
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Text("Keep your bearings.", fontSize = 26.sp, fontWeight = FontWeight.Medium, color = palette.ink, letterSpacing = (-0.5).sp)
            Spacer(Modifier.height(25.dp))

            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
                Canvas(Modifier.fillMaxSize()) {
                    val radius = size.minDimension * .47f
                    val center = Offset(size.width / 2f, size.height / 2f)
                    drawCircle(Brush.radialGradient(listOf(palette.readout, palette.dialBackground), center, radius * 1.1f), radius * 1.1f)
                    drawCircle(palette.ink.copy(alpha = .08f), radius * 1.04f, center)
                    drawCircle(Brush.linearGradient(listOf(palette.forest, palette.dialInk)), radius, center)
                    drawCircle(palette.gold.copy(alpha = .32f), radius * .965f, center, style = Stroke(1.dp.toPx()))
                    drawCircle(Color.White.copy(alpha = .08f), radius - 5.dp.toPx(), center, style = Stroke(1.dp.toPx()))
                    drawCircle(palette.sage.copy(alpha = .14f), radius * .69f, center, style = Stroke(1.dp.toPx()))
                    rotate(-animatedHeading, center) {
                        for (i in 0 until 72) {
                            val angle = Math.toRadians((i * 5 - 90).toDouble())
                            val major = i % 6 == 0
                            val medium = i % 2 == 0
                            val r1 = radius * .89f
                            val r2 = radius * when {
                                major -> .77f
                                medium -> .82f
                                else -> .85f
                            }
                            val start = Offset(center.x + kotlin.math.cos(angle).toFloat() * r1, center.y + kotlin.math.sin(angle).toFloat() * r1)
                            val end = Offset(center.x + kotlin.math.cos(angle).toFloat() * r2, center.y + kotlin.math.sin(angle).toFloat() * r2)
                            val tickColor = if (major) palette.gold else palette.sage.copy(alpha = if (medium) .72f else .48f)
                            drawLine(tickColor, start, end, if (major) 2.dp.toPx() else 1.dp.toPx())
                        }
                        val north = Path().apply {
                            moveTo(center.x, center.y - radius * .67f)
                            lineTo(center.x - radius * .105f, center.y + radius * .035f)
                            quadraticTo(center.x, center.y - radius * .045f, center.x, center.y)
                            close()
                        }
                        drawPath(north, palette.gold)
                        val south = Path().apply {
                            moveTo(center.x, center.y + radius * .67f)
                            lineTo(center.x + radius * .105f, center.y - radius * .035f)
                            quadraticTo(center.x, center.y + radius * .045f, center.x, center.y)
                            close()
                        }
                        drawPath(south, palette.sage.copy(alpha = .8f))
                    }
                    drawCircle(palette.dialInk, radius * .055f, center)
                    drawCircle(palette.gold, radius * .023f, center)
                    // A fixed index at the top lets the rotating dial read against the phone's heading.
                    val tip = Path().apply {
                        moveTo(center.x, center.y - radius * .92f)
                        lineTo(center.x - radius * .055f, center.y - radius * .80f)
                        lineTo(center.x + radius * .055f, center.y - radius * .80f)
                        close()
                    }
                    drawPath(tip, palette.gold)
                }
                Column(Modifier.fillMaxWidth(.48f), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("$degrees°", fontSize = 55.sp, lineHeight = 58.sp, fontWeight = FontWeight.Light, color = Color.White, letterSpacing = (-2).sp)
                    Spacer(Modifier.height(1.dp))
                    Text(cardinal.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = palette.gold)
                }
                Text("N", modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp, color = palette.gold)
                Text("S", modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.sage)
                Text("W", modifier = Modifier.align(Alignment.CenterStart).padding(start = 20.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.sage)
                Text("E", modifier = Modifier.align(Alignment.CenterEnd).padding(end = 20.dp), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = palette.sage)
            }

            Spacer(Modifier.height(18.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(palette.readout).padding(vertical = 15.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Readout("HEADING", "$degrees°", Icons.Default.SouthWest, palette)
                Box(Modifier.width(1.dp).height(36.dp).background(palette.divider))
                Readout("DIRECTION", cardinal, Icons.Default.Explore, palette)
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(palette.infoBackground).padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.GpsFixed, null, tint = palette.sage, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (available) "Magnetic north" else "Compass sensor unavailable", fontSize = 12.sp, color = palette.ink, fontWeight = FontWeight.Medium)
                Spacer(Modifier.weight(1f))
                Text(if (available) "LIVE" else "—", fontSize = 9.sp, color = palette.muted, letterSpacing = 1.5.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.weight(1f))
            Text("Hold your phone flat for the steadiest reading", fontSize = 11.sp, color = palette.muted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(20.dp))
        }
    }
}

@Composable
private fun Readout(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, palette: CompassPalette) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(palette.iconBackground), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = palette.forest, modifier = Modifier.size(17.dp))
        }
        Spacer(Modifier.width(10.dp))
        Column {
            Text(label, fontSize = 9.sp, color = palette.muted, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp)
            Text(value, fontSize = 17.sp, color = palette.ink, fontWeight = FontWeight.SemiBold)
        }
    }
}

private fun cardinal(degrees: Float): String {
    val directions = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    return directions[((degrees + 22.5f) / 45f).toInt().mod(8)]
}
