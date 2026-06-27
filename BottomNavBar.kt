package com.example.ui.components

import android.view.HapticFeedbackConstants
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class NavItem(val title: String, val route: String, val icon: ImageVector, val hasBadge: Boolean = false)

@Composable
fun FloatingBottomNav(
    items: List<NavItem>,
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val view = LocalView.current
    val selectedIndex = remember(currentRoute) {
        items.indexOfFirst { it.route == currentRoute }.takeIf { it >= 0 } ?: 0
    }

    // Smooth horizontal sliding indicator using simple offset animation
    val animatedIndex by animateFloatAsState(
        targetValue = selectedIndex.toFloat(),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "IndicatorOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(Color(0xFF0F1015)) // dark background track
            .padding(top = 10.dp, bottom = 14.dp, start = 12.dp, end = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val totalWidth = maxWidth
            val tabCount = items.size
            val tabWidth = totalWidth / tabCount

            // 1. Sleek, solid, high-contrast violet pill indicator
            Box(
                modifier = Modifier
                    .offset(x = tabWidth * animatedIndex)
                    .width(tabWidth)
                    .fillMaxHeight()
                    .padding(horizontal = 4.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFF4C4DFF)) // Solid modern blue/violet highlight
            )

            // 2. Navigation labels and icons Row on top
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEachIndexed { index, item ->
                    val isSelected = index == selectedIndex
                    val tint by animateColorAsState(
                        targetValue = if (isSelected) Color.White else Color(0x99A0A0A5),
                        animationSpec = tween(200),
                        label = "ColorTint"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            ) {
                                if (!isSelected) {
                                    view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                                    onNavigate(item.route)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = item.title,
                                    tint = tint,
                                    modifier = Modifier.size(24.dp)
                                )
                                if (item.hasBadge) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(Color(0xFFEF4444), CircleShape)
                                            .align(Alignment.TopEnd)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                color = tint,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
