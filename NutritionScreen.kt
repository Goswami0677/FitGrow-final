package com.example.ui.screens.nutrition

import kotlinx.coroutines.launch
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import com.example.MainViewModel
import com.example.ui.screens.home.CircularProgressIndicatorCustom
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun NutritionScreen(viewModel: MainViewModel, onBack: () -> Unit = {}) {
    var mealName by remember { mutableStateOf("") }
    var calories by remember { mutableStateOf("") }
    var protein by remember { mutableStateOf("") }
    
    var isScanningDiet by remember { mutableStateOf(false) }
    var selectedPlateIndex by remember { mutableStateOf(0) }
    var isAiAnalyzingScan by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    
    val profile = viewModel.profile.collectAsState().value
    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
    val logs = viewModel.getNutritionForDate(today).collectAsState(initial = emptyList()).value
    val waterLog = viewModel.getWaterForDate(today).collectAsState(initial = null).value
    
    val totalCals = logs.sumOf { it.calories }
    val totalProtein = logs.sumOf { it.protein }
    val totalWater = waterLog?.totalWaterMl ?: 0

    val targetCals = profile?.targetCalories ?: 2000
    val targetProtein = profile?.targetProtein ?: 150
    val targetWater = profile?.targetWater ?: 3000

    val calProgress = if (targetCals > 0) (totalCals / targetCals.toFloat()) else 0f
    val protProgress = if (targetProtein > 0) (totalProtein / targetProtein.toFloat()) else 0f
    val waterProgress = if (targetWater > 0) (totalWater / targetWater.toFloat()) else 0f
    
    val isGoalReached = calProgress >= 0.8f && protProgress >= 0.8f && waterProgress >= 0.8f

    val scrollState = rememberScrollState()
    val shouldScrollToTrends by viewModel.shouldScrollToNutritionTrends.collectAsState()
    var trendsYOffset by remember { mutableStateOf(0f) }

    LaunchedEffect(shouldScrollToTrends, trendsYOffset) {
        if (shouldScrollToTrends && trendsYOffset > 0f) {
            scrollState.animateScrollTo(trendsYOffset.toInt())
            viewModel.setScrollToNutritionTrends(false)
        }
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10)).verticalScroll(scrollState).padding(horizontal = 24.dp)) {
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 8.dp)) {
            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text("TRACKING", color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Daily Dashboard", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            }
            if (isGoalReached) {
                Box(modifier = Modifier.background(Color(0xFF00C853).copy(alpha=0.15f), CircleShape).border(1.dp, Color(0xFF00C853), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00C853), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ON TRACK", color = Color(0xFF00C853), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Box(modifier = Modifier.background(Color(0xFFFFB74D).copy(alpha=0.15f), CircleShape).border(1.dp, Color(0xFFFFB74D), CircleShape).padding(horizontal = 12.dp, vertical = 6.dp)) {
                    Text("IN PROGRESS", color = Color(0xFFFFB74D), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))

        // Progress Rings
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
                .padding(vertical = 24.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val waterStr = String.format("%.1f", totalWater / 1000f) + "L\n/${targetWater / 1000}L"
            CircularProgressIndicatorCustom("Cals", calProgress, Color(0xFFFFB74D), "$totalCals\n/$targetCals", "${(calProgress * 100).toInt()}%")
            CircularProgressIndicatorCustom("Protein", protProgress, Color(0xFFE91E63), "${totalProtein}g\n/$targetProtein", "${(protProgress * 100).toInt()}%")
            CircularProgressIndicatorCustom("Water", waterProgress, Color(0xFF03A9F4), waterStr, "${(waterProgress * 100).toInt()}%")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Quick Entry Form
        Text("LOG MEAL", color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        Spacer(modifier = Modifier.height(16.dp))
        
        Column(
            modifier = Modifier.fillMaxWidth().background(Color(0xFF15161A), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp)).padding(16.dp)
        ) {
            OutlinedTextField(
                value = mealName, onValueChange = { mealName = it }, 
                label = { Text("Meal Name (e.g. Chicken breast)", color = Color.Gray) },
                colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6B4EE6), unfocusedBorderColor = Color(0xFF222328), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = calories, onValueChange = { calories = it }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    label = { Text("Kcal", color = Color.Gray) }, 
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6B4EE6), unfocusedBorderColor = Color(0xFF222328), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = protein, onValueChange = { protein = it }, 
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), 
                    label = { Text("Protein (g)", color = Color.Gray) }, 
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF6B4EE6), unfocusedBorderColor = Color(0xFF222328), focusedTextColor = Color.White, unfocusedTextColor = Color.White),
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    val c = calories.toIntOrNull() ?: 0
                    val p = protein.toIntOrNull() ?: 0
                    if (mealName.isNotBlank() && (c > 0 || p > 0)) {
                        viewModel.addNutrition(today, mealName, c, p)
                        mealName = ""
                        calories = ""
                        protein = ""
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C4DFF)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("ADD DIET ENTRY", fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        if (isScanningDiet) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { isScanningDiet = false },
                properties = androidx.compose.ui.window.DialogProperties(
                    usePlatformDefaultWidth = false
                )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.75f))
                        .clickable { isScanningDiet = false },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxHeight(0.85f)
                            .aspectRatio(9f / 16f)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFF15161A))
                            .border(2.dp, Color(0xFF222328), RoundedCornerShape(24.dp))
                            .clickable(enabled = true, onClick = {}),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            IconButton(
                                onClick = { isScanningDiet = false },
                                modifier = Modifier.align(Alignment.CenterStart)
                            ) {
                                Icon(Icons.Default.Close, "Close Camera", tint = Color.White)
                            }
                            
                            Text(
                                "AI MEAL CAMERA",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.align(Alignment.Center)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.Black)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val cameraPlateImg = when (selectedPlateIndex) {
                                0 -> com.example.R.drawable.bg_breakfast
                                1 -> com.example.R.drawable.bg_lunch
                                2 -> com.example.R.drawable.bg_dinner
                                else -> com.example.R.drawable.bg_focus
                            }
                            
                            Image(
                                painter = androidx.compose.ui.res.painterResource(id = cameraPlateImg),
                                contentDescription = "Simulated plate of food",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().alpha(0.6f)
                            )

                            val infiniteTransition = rememberInfiniteTransition(label = "scanner")
                            val scanLineY by infiniteTransition.animateFloat(
                                initialValue = 0.1f,
                                targetValue = 0.9f,
                                animationSpec = infiniteRepeatable(
                                    animation = tween(2000, easing = LinearEasing),
                                    repeatMode = RepeatMode.Reverse
                                ),
                                label = "scan_line"
                            )
                            
                            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                                val y = size.height * scanLineY
                                drawLine(
                                    color = Color(0xFF00C853),
                                    start = androidx.compose.ui.geometry.Offset(0f, y),
                                    end = androidx.compose.ui.geometry.Offset(size.width, y),
                                    strokeWidth = 6f
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(180.dp)
                                    .border(2.dp, Color(0xFF00C853).copy(alpha = 0.7f), RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    "PLACE PLATE HERE",
                                    color = Color(0xFF00C853),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center
                                )
                            }

                            if (isAiAnalyzingScan) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.75f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        CircularProgressIndicator(color = Color(0xFF00C853))
                                        Spacer(modifier = Modifier.height(16.dp))
                                        Text("AI is scanning plate...", color = Color.White, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF15161A))
                                .padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Tap to simulate different plates of food:",
                                color = Color(0xFFA0A0A5),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            
                            val plates = listOf(
                                Triple("Paneer Tikka Plate", 320, 18),
                                Triple("Chicken Biryani Plate", 550, 32),
                                Triple("Oatmeal Fruits Bowl", 280, 8),
                                Triple("Protein Shake & Nuts", 350, 30)
                            )
                            
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                plates.forEachIndexed { idx, plate ->
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .background(
                                                if (selectedPlateIndex == idx) Color(0xFF4C4DFF) else Color(0xFF222328),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .border(
                                                1.dp,
                                                if (selectedPlateIndex == idx) Color.White else Color.Transparent,
                                                RoundedCornerShape(8.dp)
                                            )
                                            .clickable { selectedPlateIndex = idx }
                                            .padding(vertical = 6.dp, horizontal = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            plate.first.split(" ").first(),
                                            color = Color.White,
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1
                                        )
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    isAiAnalyzingScan = true
                                    scope.launch {
                                        kotlinx.coroutines.delay(1500)
                                        val selectedPlate = plates[selectedPlateIndex]
                                        mealName = selectedPlate.first
                                        calories = selectedPlate.second.toString()
                                        protein = selectedPlate.third.toString()
                                        isAiAnalyzingScan = false
                                        isScanningDiet = false
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(48.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00C853)),
                                shape = RoundedCornerShape(24.dp)
                            ) {
                                Text("CHOOSE MEAL", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Water Quick Actions
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("QUICK HYDRATION", color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Icon(Icons.Default.WaterDrop, null, tint = Color(0xFF03A9F4), modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            WaterButton("+250ml", Modifier.weight(1f)) { viewModel.addWater(today, 250) }
            WaterButton("+500ml", Modifier.weight(1f)) { viewModel.addWater(today, 500) }
            WaterButton("+1L", Modifier.weight(1f)) { viewModel.addWater(today, 1000) }
        }
        
        Spacer(modifier = Modifier.height(40.dp))
        // Analytics Trending
        Text(
            "TRENDS",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.onGloballyPositioned { coordinates ->
                trendsYOffset = coordinates.positionInParent().y
            }
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        val allLogs = viewModel.getAllNutritionLogs().collectAsState(initial = emptyList()).value
        // Month data
        val currentMonthPrefix = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val monthLogs = allLogs.filter { it.dateStr.startsWith(currentMonthPrefix) }
        val monthLogsByDate = monthLogs.groupBy { it.dateStr }
        val daysInMonth = Calendar.getInstance().getActualMaximum(Calendar.DAY_OF_MONTH)
        
        val calsList = FloatArray(daysInMonth)
        val protList = FloatArray(daysInMonth)
        for (day in 1..daysInMonth) {
            val dateStr = String.format("%s-%02d", currentMonthPrefix, day)
            val dLogs = monthLogsByDate[dateStr] ?: emptyList()
            calsList[day-1] = dLogs.sumOf { it.calories }.toFloat()
            protList[day-1] = dLogs.sumOf { it.protein }.toFloat()
        }

        TrendGraphMonthly("Monthly Calorie Graph", "cals", calsList, targetCals.toFloat())
        Spacer(modifier = Modifier.height(16.dp))
        TrendGraphMonthly("Monthly Protein Graph", "protein", protList, targetProtein.toFloat())
        
        Spacer(modifier = Modifier.height(120.dp))
    }
}

@Composable
fun WaterButton(label: String, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(48.dp)
            .background(Color(0xFF03A9F4).copy(alpha=0.15f), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF03A9F4).copy(alpha=0.3f), RoundedCornerShape(12.dp))
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = Color(0xFF03A9F4), fontWeight = FontWeight.Bold, fontSize = 14.sp)
    }
}

@Composable
fun TrendGraphMonthly(title: String, type: String, values: FloatArray, maxTarget: Float) {
    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF15161A), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp)).padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text(title, color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            Icon(androidx.compose.material.icons.Icons.Default.ShowChart, null, tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
        }
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
            val stepX = size.width / Math.max(1, values.size - 1)
            
            // Expected Target Label (drawn visually)
            // Daily actual progress path
            val realPath = Path()
            var started = false
            
            val todayDay = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)
            for (i in 0 until todayDay) {
                val dailyValue = values[i]
                val currentX = i * stepX
                
                // If daily value is 0 (day skipped), height is 100% (the bottom of the canvas)
                val finalH = if (dailyValue == 0f) {
                    size.height
                } else {
                    // Ratio of daily achievement (capped at 1.2x to fit neatly)
                    val ratio = (dailyValue / maxTarget).coerceIn(0f, 1.2f)
                    // Map 0f..1.2f to Y coordinate (bottom size.height to top size.height * 0.1f)
                    size.height - (size.height * 0.8f * (ratio / 1.2f))
                }
                
                if (!started) {
                    realPath.moveTo(currentX, finalH)
                    started = true
                } else {
                    realPath.lineTo(currentX, finalH)
                }
            }
            
            if (started) {
                drawPath(
                    path = realPath,
                    color = Color(0xFF00C853),
                    style = Stroke(width = 4f, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
                )
            }
        }
    }
}
