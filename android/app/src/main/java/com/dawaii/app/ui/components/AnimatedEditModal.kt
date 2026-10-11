package com.dawaii.app.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.dawaii.app.ui.theme.*

/**
 * Reusable animated edit modal bound to ViewModel boolean visibility state.
 * Uses Compose AnimatedVisibility for smooth spring scale-up and fade-in/out.
 * Touch Interception: Only active while isVisible is true; dismisses cleanly without leaving
 * any transparent Box or view hierarchy in the foreground.
 */
@Composable
fun AnimatedEditModal(
    isVisible: Boolean,
    onDismissRequest: () -> Unit,
    title: String,
    subtitle: String? = null,
    saveButtonText: String = "Done",
    isSaveEnabled: Boolean = true,
    onSave: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    if (!isVisible) return

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.70f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismissRequest
                ),
            contentAlignment = Alignment.Center
        ) {
            AnimatedVisibility(
                visible = true,
                enter = scaleIn(
                    initialScale = 0.88f,
                    animationSpec = spring(
                        dampingRatio = 0.75f,
                        stiffness = Spring.StiffnessMediumLow
                    )
                ) + fadeIn(animationSpec = tween(180)),
                exit = scaleOut(targetScale = 0.88f, animationSpec = tween(150)) + fadeOut(animationSpec = tween(150))
            ) {
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = CardDefaults.outlinedCardBorder().copy(
                        brush = SolidColor(DarkBorder)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.92f)
                        .wrapContentHeight()
                        .clip(RoundedCornerShape(24.dp))
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = { /* Intercept clicks on card so backdrop doesn't dismiss */ }
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp)
                    ) {
                        // Header with Title and Close Icon
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = title,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = BlushPinkPrimary,
                                    letterSpacing = 1.sp
                                )
                                if (subtitle != null) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = subtitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedGreyPink
                                    )
                                }
                            }

                            IconButton(
                                onClick = onDismissRequest,
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Close",
                                    tint = MutedGreyPink,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        HorizontalDivider(color = DarkBorder)
                        Spacer(modifier = Modifier.height(18.dp))

                        // Form Content with vertical scroll
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(weight = 1f, fill = false)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            content()
                        }

                        Spacer(modifier = Modifier.height(22.dp))
                        HorizontalDivider(color = DarkBorder)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Bottom Actions: Cancel and Save
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = onDismissRequest,
                                colors = ButtonDefaults.textButtonColors(contentColor = MutedGreyPink)
                            ) {
                                Text(
                                    text = "Cancel",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MutedGreyPink
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Button(
                                onClick = onSave,
                                enabled = isSaveEnabled,
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = BlushPinkPrimary,
                                    contentColor = DarkCharcoalBg,
                                    disabledContainerColor = BlushPinkContainer,
                                    disabledContentColor = MutedGreyPink
                                ),
                                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    text = saveButtonText,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = if (isSaveEnabled) DarkCharcoalBg else MutedGreyPink
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
