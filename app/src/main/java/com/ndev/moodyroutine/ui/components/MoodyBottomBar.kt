package com.ndev.moodyroutine.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

enum class MoodyTab(val title: String, val icon: ImageVector) {
    MODES("Modes", Icons.Rounded.Dashboard),
    ROUTINES("Routines", Icons.Rounded.Repeat),
    SETTINGS("Settings", Icons.Rounded.Settings)
}

@Composable
fun MoodyBottomBar(
    currentTab: MoodyTab,
    onTabSelected: (MoodyTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by rememberSaveable { mutableStateOf(true) }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        val totalWidth = maxWidth
        val toggleButtonSize = 56.dp

        val pillWidth by animateDpAsState(
            targetValue = if (isExpanded) totalWidth else toggleButtonSize,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "pill_width"
        )

        val tabsWidth by animateDpAsState(
            targetValue = if (isExpanded) (totalWidth - toggleButtonSize).coerceAtLeast(0.dp) else 0.dp,
            animationSpec = spring(
                dampingRatio = 0.82f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "tabs_width"
        )

        val chevronRotation by animateFloatAsState(
            targetValue = if (isExpanded) 0f else 180f,
            animationSpec = spring(
                dampingRatio = 0.78f,
                stiffness = Spring.StiffnessMediumLow
            ),
            label = "chevron_rotation"
        )

        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
            shadowElevation = 12.dp,
            tonalElevation = 0.dp,
            modifier = Modifier
                .width(pillWidth)
                .height(toggleButtonSize)
                .clip(CircleShape)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End
            ) {
                // Collapsible Tabs Container
                if (tabsWidth > 20.dp) {
                    val progress = (tabsWidth / (totalWidth - toggleButtonSize)).coerceIn(0f, 1f)

                    BoxWithConstraints(
                        modifier = Modifier
                            .width(tabsWidth)
                            .fillMaxHeight()
                            .clipToBounds()
                            .graphicsLayer { alpha = progress }
                            .padding(horizontal = 4.dp, vertical = 4.dp)
                    ) {
                        val tabCount = MoodyTab.entries.size
                        val tabWidth = maxWidth / tabCount
                        val selectedIndex = currentTab.ordinal

                        // Animated sliding / flowing highlight indicator
                        val indicatorOffset by animateDpAsState(
                            targetValue = tabWidth * selectedIndex,
                            animationSpec = spring(
                                dampingRatio = 0.78f,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            label = "tab_indicator_offset"
                        )

                        // Flowing highlight capsule
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .width(tabWidth)
                                .fillMaxHeight()
                                .padding(horizontal = 3.dp, vertical = 1.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f))
                                .border(
                                    width = 1.dp,
                                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                                    shape = CircleShape
                                )
                        )

                        // Evenly distributed tab icons without labels
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            MoodyTab.entries.forEach { tab ->
                                val isSelected = currentTab == tab
                                val tabTint by animateColorAsState(
                                    targetValue = if (isSelected) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f)
                                    },
                                    animationSpec = tween(220),
                                    label = "tab_color_${tab.name}"
                                )

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(CircleShape)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { onTabSelected(tab) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = tab.icon,
                                        contentDescription = tab.title,
                                        tint = tabTint,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Collapse (>) / Expand (<) Toggle Button
                Box(
                    modifier = Modifier
                        .size(toggleButtonSize)
                        .clip(CircleShape)
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) { isExpanded = !isExpanded },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = if (isExpanded) "Collapse navigation bar" else "Expand navigation bar",
                        tint = if (isExpanded) {
                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer(rotationZ = chevronRotation)
                    )
                }
            }
        }
    }
}
