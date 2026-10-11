package com.dawaii.app.ui.customize

import android.content.Context
import android.media.RingtoneManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.dawaii.app.data.preferences.AppPreferences
import com.dawaii.app.data.preferences.AppTheme
import com.dawaii.app.data.preferences.GestureAction
import com.dawaii.app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomizeScreen() {
    val context = LocalContext.current
    val prefs = remember { AppPreferences.getInstance(context) }
    val focusManager = LocalFocusManager.current

    val currentTheme by prefs.themeFlow.collectAsState()
    val isChimeEnabled by prefs.chimeFlow.collectAsState()
    val chimeOnOverlay by prefs.chimeOnOverlayFlow.collectAsState()
    val chimeOnNotification by prefs.chimeOnNotificationFlow.collectAsState()
    val chimeDelay by prefs.chimeDelayFlow.collectAsState()
    val dimnessPercent by prefs.dimnessFlow.collectAsState()
    val isOverlayEnabled by prefs.overlayEnabledFlow.collectAsState()
    val brightenOnScroll by prefs.brightenOnScrollFlow.collectAsState()
    val is24Hour by prefs.is24HourFlow.collectAsState()
    val overlayTextPref by prefs.overlayTextFlow.collectAsState()
    val caregiverPhonePref by prefs.caregiverPhoneFlow.collectAsState()
    val caregiverEnabledPref by prefs.caregiverEnabledFlow.collectAsState()

    var overlayTextInput by remember(overlayTextPref) { mutableStateOf(overlayTextPref) }
    var caregiverPhoneInput by remember(caregiverPhonePref) { mutableStateOf(caregiverPhonePref) }
    var doubleTapSelected by remember { mutableStateOf(prefs.doubleTapAction) }
    var tripleTapSelected by remember { mutableStateOf(prefs.tripleTapAction) }

    fun playChimeSample() {
        try {
            val notificationUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val ringtone = RingtoneManager.getRingtone(context, notificationUri)
            ringtone?.play()
        } catch (_: Exception) {}
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkCharcoalBg)
            .padding(horizontal = 20.dp),
        contentPadding = PaddingValues(top = 24.dp, bottom = 80.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        // Screen Header: Tall Birch Serif + Bodoni Poster Description
        item {
            Column(modifier = Modifier.padding(bottom = 24.dp)) {
                Text(
                    text = "Customize",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = BirchTypography,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = 1.5.sp
                    ),
                    color = BlushPinkPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Tailor display aesthetics, acoustic cues, and tactile gestures",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = BodoniPosterFamily,
                        fontSize = 13.sp,
                        color = MutedGreyPink
                    )
                )
            }
        }

        // ==========================================
        // CATEGORY 1: THEME (Matching Wireframe Layout)
        // ==========================================
        item {
            CategoryCard(
                title = "Theme",
                subtitle = "Visual environment and aesthetic palette"
            ) {
                // Top Row: Two square clickable boxes side-by-side as shown in image
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ThemeSelectionBox(
                        title = "Floral",
                        subtitle = "Floral Elegance",
                        icon = "🌸",
                        isSelected = (currentTheme == AppTheme.SAKURA),
                        accentColor = BlushPinkPrimary,
                        modifier = Modifier.weight(1f),
                        onClick = { prefs.setThemePreference(AppTheme.SAKURA) }
                    )

                    ThemeSelectionBox(
                        title = "Minimal",
                        subtitle = "Pure Minimalist",
                        icon = "🌑",
                        isSelected = (currentTheme == AppTheme.NORMAL),
                        accentColor = SoftWhite,
                        modifier = Modifier.weight(1f),
                        onClick = { prefs.setThemePreference(AppTheme.NORMAL) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Wide Box below matching wireframe layout
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        // Time Format
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Time Format",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = SoftWhite
                                )
                                Text(
                                    text = "Clock representation on overlay reminder.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = BodoniPosterFamily,
                                        fontSize = 12.sp,
                                        color = MutedGreyPink
                                    )
                                )
                            }

                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF141217))
                                    .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                                    .padding(3.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                SegmentedButton(
                                    text = "12hr",
                                    isSelected = !is24Hour,
                                    onClick = { prefs.setIs24HourPreference(false) }
                                )
                                SegmentedButton(
                                    text = "24hr",
                                    isSelected = is24Hour,
                                    onClick = { prefs.setIs24HourPreference(true) }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(DarkBorder)
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        // Overlay Prompt Text
                        Text(
                            text = "Overlay Prompt Text",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = SoftWhite
                        )
                        Text(
                            text = "Central prompt displayed on lock-screen reminder.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = BodoniPosterFamily,
                                fontSize = 12.sp,
                                color = MutedGreyPink
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        MinimalInputField(
                            value = overlayTextInput,
                            onValueChange = {
                                overlayTextInput = it
                                prefs.setOverlayTextPreference(it)
                            },
                            placeholder = "DAWA!",
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ==========================================
        // CATEGORY 2: LOCK-SCREEN OVERLAY
        // ==========================================
        item {
            CategoryCard(
                title = "Lock-Screen Overlay",
                subtitle = "Manage full-screen reminder behavior, illumination, and opacity"
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurfaceVariant)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    Column {
                        // 1. Enable / Disable Lock-Screen Overlay Switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Lock-Screen Overlay",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = SoftWhite
                                )
                                Text(
                                    text = "Display full-screen reminder window when dose is due.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = BodoniPosterFamily,
                                        fontSize = 12.sp,
                                        color = MutedGreyPink
                                    )
                                )
                            }
                            Switch(
                                checked = isOverlayEnabled,
                                onCheckedChange = { prefs.setOverlayEnabledPreference(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkCharcoalBg,
                                    checkedTrackColor = BlushPinkPrimary,
                                    uncheckedThumbColor = MutedGreyPink,
                                    uncheckedTrackColor = DarkSurfaceVariant,
                                    uncheckedBorderColor = DarkBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder.copy(alpha = 0.5f)))
                        Spacer(modifier = Modifier.height(14.dp))

                        // 2. Brighten on Scrollwheel Adjustment (On by default)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Brighten on scrolling wheel",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = SoftWhite
                                )
                                Text(
                                    text = "Turn up brightness to 100% when adjusting scrollwheel.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = BodoniPosterFamily,
                                        fontSize = 12.sp,
                                        color = MutedGreyPink
                                    )
                                )
                            }
                            Switch(
                                checked = brightenOnScroll,
                                onCheckedChange = { prefs.setBrightenOnScrollPreference(it) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = DarkCharcoalBg,
                                    checkedTrackColor = BlushPinkPrimary,
                                    uncheckedThumbColor = MutedGreyPink,
                                    uncheckedTrackColor = DarkSurfaceVariant,
                                    uncheckedBorderColor = DarkBorder
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(DarkBorder.copy(alpha = 0.5f)))
                        Spacer(modifier = Modifier.height(14.dp))

                        // 3. Initial Ambient Dimness Slider
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Initial Dimness Level",
                                    style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                                    color = SoftWhite
                                )
                                Text(
                                    text = "Screen transparency when reminder awakens.",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = BodoniPosterFamily,
                                        fontSize = 12.sp,
                                        color = MutedGreyPink
                                    )
                                )
                            }
                            Text(
                                text = "${dimnessPercent.toInt()}%",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = BirchTypography,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp
                                ),
                                color = BlushPinkPrimary
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        MinimalSlimSlider(
                            value = dimnessPercent,
                            onValueChange = { prefs.setDimnessPreference(it) },
                            valueRange = 10f..95f
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ==========================================
        // CATEGORY 2: AUDIO & OVERLAY
        // ==========================================
        item {
            CategoryCard(
                title = "Audio & Overlay",
                subtitle = "Gentle acoustic alerts and delay intervals"
            ) {
                // Master Chime Toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Audio Chimes",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = SoftWhite
                        )
                        Text(
                            text = "Enable subtle audio cues for reminders.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = BodoniPosterFamily,
                                fontSize = 12.sp,
                                color = MutedGreyPink
                            )
                        )
                    }

                    Switch(
                        checked = isChimeEnabled,
                        onCheckedChange = {
                            prefs.setChimePreference(it)
                            if (it) playChimeSample()
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkCharcoalBg,
                            checkedTrackColor = BlushPinkPrimary,
                            uncheckedThumbColor = MutedGreyPink,
                            uncheckedTrackColor = DarkSurfaceVariant,
                            uncheckedBorderColor = DarkBorder
                        )
                    )
                }

                // Chime Delay Setting (0s to 15s)
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Chime Delay",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = SoftWhite
                        )
                        Text(
                            text = "Set seconds to wait before playing audio.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = BodoniPosterFamily,
                                fontSize = 12.sp,
                                color = MutedGreyPink
                            )
                        )
                    }
                    Text(
                        text = "${chimeDelay}s",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = BirchTypography,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = BlushPinkPrimary
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                MinimalSlimSlider(
                    value = chimeDelay.toFloat(),
                    onValueChange = { prefs.setChimeDelayPreference(it.toInt()) },
                    valueRange = 0f..15f,
                    steps = 14
                )

                // Sub-toggles
                Spacer(modifier = Modifier.height(18.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Lock-Screen Overlay Chime",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = SoftWhite
                        )
                        Text(
                            text = "Play gentle chime when overlay awakens",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = BodoniPosterFamily,
                                fontSize = 11.sp,
                                color = MutedGreyPink
                            )
                        )
                    }
                    Switch(
                        checked = chimeOnOverlay,
                        onCheckedChange = { prefs.setChimeOnOverlayPreference(it) },
                        enabled = isChimeEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkCharcoalBg,
                            checkedTrackColor = BlushPinkPrimary,
                            uncheckedThumbColor = MutedGreyPink,
                            uncheckedTrackColor = DarkSurfaceVariant,
                            uncheckedBorderColor = DarkBorder
                        )
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "System Notification Sound",
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            color = SoftWhite
                        )
                        Text(
                            text = "Play chime during regular system alerts",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = BodoniPosterFamily,
                                fontSize = 11.sp,
                                color = MutedGreyPink
                            )
                        )
                    }
                    Switch(
                        checked = chimeOnNotification,
                        onCheckedChange = { prefs.setChimeOnNotificationPreference(it) },
                        enabled = isChimeEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkCharcoalBg,
                            checkedTrackColor = BlushPinkPrimary,
                            uncheckedThumbColor = MutedGreyPink,
                            uncheckedTrackColor = DarkSurfaceVariant,
                            uncheckedBorderColor = DarkBorder
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // ==========================================
        // CATEGORY 3: CONTROLS & CAREGIVER
        // ==========================================
        item {
            CategoryCard(
                title = "Controls & Caregiver",
                subtitle = "Tactile muscle-memory gestures and emergency safety net"
            ) {
                // Double Tap Gesture
                // Double Tap Gesture
                GestureDropdownItem(
                    label = "Double Tap Gesture",
                    description = "Action when double-tapping overlay screen",
                    selectedAction = doubleTapSelected,
                    options = listOf(GestureAction.UNLOCK_PHONE, GestureAction.MARK_TAKEN, GestureAction.NONE),
                    onSelect = { action ->
                        doubleTapSelected = action
                        prefs.doubleTapAction = action
                        if (action != GestureAction.NONE && action == tripleTapSelected) {
                            val alternate = if (action == GestureAction.UNLOCK_PHONE) GestureAction.MARK_TAKEN else GestureAction.UNLOCK_PHONE
                            tripleTapSelected = alternate
                            prefs.tripleTapAction = alternate
                        }
                    }
                )

                Spacer(modifier = Modifier.height(18.dp))

                // Triple Tap Gesture
                GestureDropdownItem(
                    label = "Triple Tap Gesture",
                    description = "Action when triple-tapping overlay screen",
                    selectedAction = tripleTapSelected,
                    options = listOf(GestureAction.MARK_TAKEN, GestureAction.UNLOCK_PHONE, GestureAction.NONE),
                    onSelect = { action ->
                        tripleTapSelected = action
                        prefs.tripleTapAction = action
                        if (action != GestureAction.NONE && action == doubleTapSelected) {
                            val alternate = if (action == GestureAction.UNLOCK_PHONE) GestureAction.MARK_TAKEN else GestureAction.UNLOCK_PHONE
                            doubleTapSelected = alternate
                            prefs.doubleTapAction = alternate
                        }
                    }
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Caregiver SMS Sync
                var isCaregiverPhoneError by remember { mutableStateOf(false) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Caregiver SMS Sync",
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
                            color = SoftWhite
                        )
                        Text(
                            text = "Notify a partner or caregiver if a medication is missed.",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = BodoniPosterFamily,
                                fontSize = 12.sp,
                                color = MutedGreyPink
                            )
                        )
                    }

                    Switch(
                        checked = caregiverEnabledPref,
                        onCheckedChange = { prefs.setCaregiverEnabledPreference(it) },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkCharcoalBg,
                            checkedTrackColor = BlushPinkPrimary,
                            uncheckedThumbColor = MutedGreyPink,
                            uncheckedTrackColor = DarkSurfaceVariant,
                            uncheckedBorderColor = DarkBorder
                        )
                    )
                }

                AnimatedVisibility(
                    visible = caregiverEnabledPref,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Spacer(modifier = Modifier.height(12.dp))

                        MinimalInputField(
                            value = caregiverPhoneInput,
                            onValueChange = {
                                caregiverPhoneInput = it
                                val clean = it.trim().replace(Regex("[\\s\\-\\(\\)]"), "")
                                if (Regex("^(\\+91|91|0)?[6-9]\\d{9}$").matches(clean)) {
                                    isCaregiverPhoneError = false
                                    prefs.setCaregiverPhonePreference(it.trim())
                                }
                            },
                            placeholder = "9692298485",
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Phone,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(onDone = {
                                val clean = caregiverPhoneInput.trim().replace(Regex("[\\s\\-\\(\\)]"), "")
                                if (caregiverPhoneInput.isNotBlank() && !Regex("^(\\+91|91|0)?[6-9]\\d{9}$").matches(clean)) {
                                    isCaregiverPhoneError = true
                                } else {
                                    isCaregiverPhoneError = false
                                    prefs.setCaregiverPhonePreference(caregiverPhoneInput.trim())
                                }
                                focusManager.clearFocus()
                            })
                        )

                        if (isCaregiverPhoneError) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Incorrect number",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF6B6B)
                                )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Automated SMS: \"(time of the day. eg. noon, morning) (medication name) missed at (time)\"",
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = BodoniPosterFamily,
                                fontSize = 11.sp,
                                color = MutedGreyPink.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
            }
        }
    }
}

/**
 * Large spacious Category Card with dark subtle background.
 */
@Composable
fun CategoryCard(
    title: String,
    subtitle: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141217)),
        border = BorderStroke(1.dp, Color(0xFF2E2633))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = BirchTypography,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    letterSpacing = 1.sp
                ),
                color = BlushPinkPrimary
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = BodoniPosterFamily,
                        fontSize = 12.sp,
                        color = MutedGreyPink
                    )
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
            content()
        }
    }
}

/**
 * Elegantly outlined box for theme selection with icon badge.
 */
@Composable
fun ThemeSelectionBox(
    title: String,
    subtitle: String,
    icon: String,
    isSelected: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (isSelected) BlushPinkContainer else DarkSurfaceVariant)
            .border(
                width = if (isSelected) 1.5.dp else 1.dp,
                color = if (isSelected) accentColor else DarkBorder,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable { onClick() }
            .padding(16.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = BirchTypography,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp
                    ),
                    color = if (isSelected) accentColor else SoftWhite
                )
                Text(
                    text = icon,
                    fontSize = 16.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = BodoniPosterFamily,
                    fontSize = 12.sp,
                    color = MutedGreyPink
                )
            )
        }
    }
}

/**
 * Sleek segmented button for 12hr / 24hr switching.
 */
@Composable
fun SegmentedButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(if (isSelected) BlushPinkPrimary else Color.Transparent)
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = BirchTypography,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (isSelected) DarkCharcoalBg else SoftWhite
        )
    }
}

/**
 * Ultra-thin slider for opacity/dimness and chime delay adjustment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MinimalSlimSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    modifier: Modifier = Modifier
) {
    Slider(
        value = value,
        onValueChange = onValueChange,
        valueRange = valueRange,
        steps = steps,
        track = { sliderState ->
            SliderDefaults.Track(
                sliderState = sliderState,
                modifier = Modifier.height(2.5.dp),
                colors = SliderDefaults.colors(
                    activeTrackColor = BlushPinkPrimary,
                    inactiveTrackColor = DarkBorder.copy(alpha = 0.6f)
                )
            )
        },
        thumb = {
            Box(
                modifier = Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(BlushPinkPrimary)
                    .border(2.dp, DarkCharcoalBg, CircleShape)
            )
        },
        modifier = modifier.fillMaxWidth()
    )
}

/**
 * Dropdown setting item for Gestures with guaranteed DARK theme background & blush pink text.
 */
@Composable
fun GestureDropdownItem(
    label: String,
    description: String,
    selectedAction: GestureAction,
    options: List<GestureAction>,
    onSelect: (GestureAction) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 15.sp),
            color = SoftWhite
        )
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = BodoniPosterFamily,
                fontSize = 12.sp,
                color = MutedGreyPink
            )
        )

        Spacer(modifier = Modifier.height(8.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(DarkSurfaceVariant)
                .border(1.dp, DarkBorder, RoundedCornerShape(12.dp))
                .clickable { expanded = true }
                .padding(horizontal = 14.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = selectedAction.label,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = BirchTypography,
                        letterSpacing = 0.5.sp
                    ),
                    color = SoftWhite
                )

                Text(
                    text = "▾",
                    color = BlushPinkPrimary,
                    fontSize = 16.sp
                )
            }

            // Material3 DropdownMenu with dark theme container and styled items
            MaterialTheme(
                colorScheme = MaterialTheme.colorScheme.copy(
                    surface = Color(0xFF141217),
                    onSurface = SoftWhite
                )
            ) {
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .background(Color(0xFF141217))
                        .border(1.dp, Color(0xFF332D37), RoundedCornerShape(12.dp))
                ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option.label,
                                color = if (option == selectedAction) BlushPinkPrimary else SoftWhite,
                                fontFamily = BirchTypography,
                                fontSize = 14.sp
                            )
                        },
                        colors = MenuDefaults.itemColors(
                            textColor = SoftWhite,
                            leadingIconColor = BlushPinkPrimary,
                            trailingIconColor = BlushPinkPrimary
                        ),
                        modifier = Modifier.background(
                            if (option == selectedAction) Color(0xFF231D27) else Color.Transparent
                        ),
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}
}

@Composable
fun MinimalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        placeholder = {
            Text(placeholder, color = MutedGreyPink.copy(alpha = 0.5f), fontFamily = BodoniPosterFamily)
        },
        textStyle = MaterialTheme.typography.bodyMedium.copy(
            letterSpacing = 0.8.sp,
            color = SoftWhite
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BlushPinkPrimary,
            unfocusedBorderColor = DarkBorder,
            focusedContainerColor = DarkSurfaceVariant,
            unfocusedContainerColor = DarkSurfaceVariant,
            cursorColor = BlushPinkPrimary
        ),
        shape = RoundedCornerShape(12.dp),
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        modifier = Modifier.fillMaxWidth()
    )
}
