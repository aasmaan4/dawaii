package com.dawaii.app.ui.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawaii.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.random.Random

data class SakuraPetal(
    val initialX: Float,
    val speed: Float,
    val size: Float,
    val rotationSpeed: Float,
    val swayAmplitude: Float,
    val swayPhase: Float
)

@Composable
fun DeveloperGreetingScreen(
    onProceed: () -> Unit
) {
    var isAppeared by remember { mutableStateOf(false) }
    var isSwooshing by remember { mutableStateOf(false) }

    // Popup entry animation, hold for a while, then swoosh away into the main screen
    LaunchedEffect(Unit) {
        isAppeared = true
        delay(1900)
        isSwooshing = true
        delay(680)
        onProceed()
    }

    val entryScale by animateFloatAsState(
        targetValue = if (isAppeared) 1f else 0.72f,
        animationSpec = tween(durationMillis = 480, easing = FastOutSlowInEasing),
        label = "entryScale"
    )

    val entryAlpha by animateFloatAsState(
        targetValue = if (isAppeared) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = LinearEasing),
        label = "entryAlpha"
    )

    val swooshOffset by animateFloatAsState(
        targetValue = if (isSwooshing) -1500f else 0f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "swooshOffset"
    )

    val swooshAlpha by animateFloatAsState(
        targetValue = if (isSwooshing) 0f else 1f,
        animationSpec = tween(durationMillis = 480, easing = LinearEasing),
        label = "swooshAlpha"
    )

    val swooshScale by animateFloatAsState(
        targetValue = if (isSwooshing) 0.88f else 1f,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "swooshScale"
    )

    // Infinite transition for falling Sakura petals
    val transition = rememberInfiniteTransition(label = "sakuraRain")
    val time by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "time"
    )

    // Generate fixed random petals
    val petals = remember {
        List(35) {
            SakuraPetal(
                initialX = Random.nextFloat(),
                speed = 0.15f + Random.nextFloat() * 0.25f,
                size = 14f + Random.nextFloat() * 16f,
                rotationSpeed = 30f + Random.nextFloat() * 80f,
                swayAmplitude = 20f + Random.nextFloat() * 35f,
                swayPhase = Random.nextFloat() * 6.28f
            )
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .graphicsLayer {
                translationY = swooshOffset
                alpha = entryAlpha * swooshAlpha
                scaleX = entryScale * swooshScale
                scaleY = entryScale * swooshScale
            },
        contentAlignment = Alignment.Center
    ) {
        // Falling Sakura Blossom Petals Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val canvasW = size.width
            val canvasH = size.height

            petals.forEach { petal ->
                val y = ((time * petal.speed * 80f) % (canvasH + 100f)) - 50f
                val sway = kotlin.math.sin((time * 0.05f) + petal.swayPhase) * petal.swayAmplitude
                val x = (petal.initialX * canvasW + sway) % canvasW
                val rotation = (time * petal.rotationSpeed * 0.1f) % 360f

                rotate(degrees = rotation, pivot = Offset(x, y)) {
                    val path = Path().apply {
                        moveTo(x, y - petal.size)
                        cubicTo(
                            x + petal.size * 0.8f, y - petal.size * 0.5f,
                            x + petal.size * 0.8f, y + petal.size * 0.5f,
                            x, y + petal.size
                        )
                        cubicTo(
                            x - petal.size * 0.8f, y + petal.size * 0.5f,
                            x - petal.size * 0.8f, y - petal.size * 0.5f,
                            x, y - petal.size
                        )
                        close()
                    }
                    drawPath(
                        path = path,
                        color = Color(0xFFFFB7C5).copy(alpha = 0.75f)
                    )
                }
            }
        }

        // Center Greeting Card - Popup animation with "HIII KRISHH!!"
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(28.dp)
        ) {
            Text(
                text = "🌸",
                fontSize = 64.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Text(
                text = "HIII KRISHH!!",
                fontFamily = BirchTypography,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.5.sp,
                color = BlushPinkPrimary,
                textAlign = TextAlign.Center
            )
        }
    }
}
