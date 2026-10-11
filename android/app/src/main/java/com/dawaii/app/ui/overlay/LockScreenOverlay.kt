package com.dawaii.app.ui.overlay

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawaii.app.R
import com.dawaii.app.ui.theme.BirchTypography
import com.dawaii.app.ui.theme.BlushPinkPrimary
import com.dawaii.app.ui.theme.DarkCharcoalBg
import com.dawaii.app.ui.theme.MutedGreyPink
import com.dawaii.app.ui.theme.SoftWhite

/**
 * SYSTEM_ALERT_WINDOW Lock-Screen Overlay in Jetpack Compose
 * Rebuilt to perfectly match image_a52895.png and user layout specification:
 * - Full-screen Box container.
 * - Top-left neutralized floral image: Alignment.TopStart, ContentScale.Crop, fillMaxHeight(0.3f), stops well above clock.
 * - Bottom-right neutralized floral image: Alignment.BottomEnd, ContentScale.Crop, hugs corner below scrollwheel.
 * - Central Column: Alignment.Center with generous vertical padding (zero overlap).
 * - Selection boxes matching media_1790865267605.png: Two square boxes side-by-side on top, wide box below.
 * - Confirm button appears only after tapping once or adjusting delay.
 */
@Composable
fun LockScreenOverlayContent(
    modifier: Modifier = Modifier,
    isSakuraTheme: Boolean = true,
    initialDimness: Float = 0.40f,
    overlayPromptText: String = "DAWA!",
    timeText: String = "11:30",
    amPmText: String = "PM",
    onConfirmPostpone: (Int) -> Unit = {},
    onDismiss: () -> Unit = {},
    onMarkTaken: () -> Unit = {}
) {
    var isActivated by remember { mutableStateOf(false) }
    var selectedMinutes by remember { mutableIntStateOf(15) }

    val currentAlpha = if (isActivated) 1.0f else initialDimness.coerceIn(0.1f, 0.95f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .alpha(currentAlpha)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (!isActivated) {
                            isActivated = true
                        }
                    },
                    onDoubleTap = {
                        onDismiss()
                    }
                )
            }
    ) {
        // 1. Top Flora: Stretched to cover the left and right edge of the screen at the top
        if (isSakuraTheme) {
            Image(
                painter = painterResource(id = R.drawable.bg_floral_overlay),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth(1.25f)
                    .height(350.dp)
                    .offset(y = (-65).dp)
                    .rotate(90f)
            )
        }

        // 2. Bottom Flora: Stretched to cover the left and right edge of the screen at the bottom
        if (isSakuraTheme) {
            Image(
                painter = painterResource(id = R.drawable.bg_floral_overlay),
                contentDescription = null,
                contentScale = ContentScale.FillBounds,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(1.25f)
                    .height(350.dp)
                    .offset(y = 65.dp)
                    .rotate(270f)
            )
        }

        // 3. Central Elements: Wrapped in Column aligned to Alignment.Center with generous vertical padding (ZERO OVERLAP)
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .padding(vertical = 120.dp, horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Digital Clock: 11:30 PM (No date header above to mirror image_a52895.png)
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeText,
                    fontSize = 62.sp,
                    fontFamily = BirchTypography,
                    fontWeight = FontWeight.Light,
                    letterSpacing = 2.sp,
                    color = BlushPinkPrimary
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = amPmText,
                    fontSize = 20.sp,
                    fontFamily = BirchTypography,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 1.sp,
                    color = BlushPinkPrimary,
                    modifier = Modifier.padding(bottom = 10.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Reminder Title / Prompt
            Text(
                text = overlayPromptText.ifBlank { "DAWA!" },
                fontSize = 17.sp,
                fontFamily = BirchTypography,
                fontWeight = FontWeight.Bold,
                letterSpacing = 4.sp,
                color = BlushPinkPrimary
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Snapping Ruler Scrollwheel
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(96.dp)
                    .clickable {
                        isActivated = true
                        onConfirmPostpone(selectedMinutes)
                    },
                contentAlignment = Alignment.Center
            ) {
                // Top and bottom ruler tick lines
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BlushPinkPrimary.copy(alpha = 0.35f))
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(BlushPinkPrimary.copy(alpha = 0.35f))
                    )
                }

                val delayOptions = listOf(5, 10, 15, 20, 25, 30, 45, 60)
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    items(delayOptions) { min ->
                        val isSelected = (min == selectedMinutes)
                        Text(
                            text = "$min",
                            fontFamily = BirchTypography,
                            fontSize = if (isSelected) 36.sp else 24.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) BlushPinkPrimary else BlushPinkPrimary.copy(alpha = 0.35f),
                            modifier = Modifier
                                .clickable {
                                    selectedMinutes = min
                                    onConfirmPostpone(min)
                                }
                                .padding(horizontal = 8.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }
    }
}
