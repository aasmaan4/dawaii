package com.dawaii.app.ui.howitworks

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawaii.app.ui.theme.*

@Composable
fun HowItWorksScreen() {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 20.dp, bottom = 48.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item {
            Column {
                Text(
                    text = "HOW IT WORKS",
                    fontFamily = TallSerifFamily,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp,
                    color = BlushPinkPrimary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Gentle interactions, lock-screen rituals, and safety nets.",
                    fontFamily = BodoniPosterFamily,
                    fontSize = 13.sp,
                    color = MutedGreyPink
                )
            }
        }

        item {
            HowItWorksSection(
                icon = Icons.Outlined.Lock,
                title = "The Lock Screen Overlay",
                description = "DAWAII gently awakens over your lock screen 15 minutes after a scheduled dose if not yet recorded. It opens in a calm, dimmed state with an optional gentle chime to draw your attention without loud alarms."
            )
        }

        item {
            HowItWorksSection(
                icon = Icons.Outlined.TouchApp,
                title = "Single Tap · Wake & Focus",
                description = "A single gentle tap anywhere on the screen brings the ambient display to full clarity and activates the postponement scrollwheel."
            )
        }

        item {
            HowItWorksSection(
                icon = Icons.Outlined.Swipe,
                title = "Scroll & Confirm · Postpone",
                description = "Slide the horizontal wheel to select a delay (from 5 to 60 minutes). Tap the minimal Confirm button to reschedule your reminder."
            )
        }

        item {
            HowItWorksSection(
                icon = Icons.Outlined.Close,
                title = "Double Tap · Unlock / Dismiss",
                description = "Double-tapping instantly dismisses the ambient overlay, returning you straight to your phone's normal system lock screen."
            )
        }

        item {
            HowItWorksSection(
                icon = Icons.Outlined.CheckCircleOutline,
                title = "Triple Tap · Mark Taken",
                description = "Triple-tapping acknowledges your dose as taken immediately in your daily log, clearing all pending reminder sequences."
            )
        }

        item {
            HowItWorksSection(
                icon = Icons.Outlined.NightsStay,
                title = "Night Shift Smart Window",
                description = "Between 12:00 AM and 2:00 AM, if you turn your screen off to rest, DAWAII sounds two soft chimes. If the device remains asleep for 1 hour, it peacefully cancels the notification so your sleep is never disturbed."
            )
        }

        item {
            HowItWorksSection(
                icon = Icons.Outlined.Send,
                title = "Caregiver SMS Sync",
                description = "When enabled, if an evening window expires or a scheduled dose remains unacknowledged, DAWAII sends an automated SMS alert to your designated contact."
            )
        }
    }
}

@Composable
fun HowItWorksSection(
    icon: ImageVector,
    title: String,
    description: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(DarkSurface, RoundedCornerShape(16.dp))
            .border(1.dp, SolidColor(DarkBorder), RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(BlushPinkContainer, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = BlushPinkPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                fontFamily = TallSerifFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
                color = SoftWhite
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = description,
            fontFamily = BodoniPosterFamily,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            color = MutedGreyPink
        )
    }
}
