package com.example.ui.screens.splash

import android.media.RingtoneManager
import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onAnimationEnd: () -> Unit) {
    val context = LocalContext.current
    val view = LocalView.current
    val progress = remember { Animatable(0f) }
    
    // Green background color based on the logo reference: #8CC63F roughly
    val bgGreen = Color(0xFF8CC63F)
    
    LaunchedEffect(Unit) {
        // Vibrate initially
        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
        
        // RingtoneManager sound playback removed to resolve audio AppOps system log warnings
        
        // Let it form
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1200, easing = FastOutSlowInEasing)
        )
        // Vibrate when done forming
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
        
        delay(500) // Brief pause
        
        onAnimationEnd()
    }
    
    val textMeasurer = rememberTextMeasurer()
    val textStyle = TextStyle(
        fontSize = 80.sp,
        fontWeight = FontWeight.Black,
        color = Color.Black,
        letterSpacing = (-4).sp
    )
    val subStyle = TextStyle(
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = Color.Black,
        letterSpacing = 4.sp
    )

    Box(modifier = Modifier.fillMaxSize().background(bgGreen)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            
            // "FG" Text
            val textLayoutResult = textMeasurer.measure("FG", textStyle)
            val subLayoutResult = textMeasurer.measure("FIT GROW", subStyle)
            
            val textOffset = Offset(
                center.x - textLayoutResult.size.width / 2,
                center.y - textLayoutResult.size.height / 2 - 40f
            )
            val subOffset = Offset(
                center.x - subLayoutResult.size.width / 2,
                center.y + textLayoutResult.size.height / 2 + 10f
            )

            // Simple ping-pong / spring effect on the letters
            // Scale goes 0 -> 1.2 -> 1.0 based on progress
            val scale = if (progress.value < 0.6f) {
                progress.value / 0.6f * 1.2f
            } else {
                1.2f - ((progress.value - 0.6f) / 0.4f * 0.2f)
            }
            
            val alpha = (progress.value * 1.5f).coerceIn(0f, 1f)
            
            withTransform({
                translate(left = textOffset.x, top = textOffset.y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset(textLayoutResult.size.width/2f, textLayoutResult.size.height/2f))
            }) {
                drawText(textLayoutResult, color = Color.Black.copy(alpha = alpha))
            }
            
            withTransform({
                translate(left = subOffset.x, top = subOffset.y)
                scale(scaleX = scale, scaleY = scale, pivot = Offset(subLayoutResult.size.width/2f, subLayoutResult.size.height/2f))
            }) {
                drawText(subLayoutResult, color = Color.Black.copy(alpha = alpha))
            }
            
            // Draw random "particles" flying into the center
            if (progress.value < 0.8f) {
                val particleCount = 20
                for (i in 0 until particleCount) {
                    val angle = (i * Math.PI * 2) / particleCount
                    val startDist = size.width
                    val currentDist = startDist * (1f - (progress.value / 0.8f))
                    val px = center.x + Math.cos(angle).toFloat() * currentDist
                    val py = center.y + Math.sin(angle).toFloat() * currentDist
                    
                    drawCircle(
                        color = Color.Black,
                        radius = 8f,
                        center = Offset(px, py)
                    )
                }
            }
        }
    }
}
