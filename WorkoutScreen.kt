package com.example.ui.screens.workout

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import androidx.compose.animation.core.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.Canvas
import kotlin.math.cos
import kotlin.math.sin
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

data class WorkoutDay(
    val dayOfWeek: String,
    val dateStr: String,
    val isToday: Boolean,
    val routineName: String,
    val muscleGroup: String,
    val durationMin: Int,
    var status: String // "PENDING", "DONE", "MISSED", "LOCKED"
)

@Composable
fun SimpleMarkdownText(text: String, modifier: Modifier = Modifier) {
    val lines = text.split("\n")
    Column(modifier = modifier) {
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.startsWith("###")) {
                Text(
                    text = trimmed.removePrefix("###").trim().removePrefix("**").removeSuffix("**"),
                    color = Color(0xFF9E82FF),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            } else if (trimmed.startsWith("##")) {
                Text(
                    text = trimmed.removePrefix("##").trim().removePrefix("**").removeSuffix("**"),
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            } else if (trimmed.startsWith("#")) {
                Text(
                    text = trimmed.removePrefix("#").trim().removePrefix("**").removeSuffix("**"),
                    color = Color.White,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 10.dp)
                )
            } else if (trimmed.startsWith("-") || trimmed.startsWith("*")) {
                val cleanLine = trimmed.substring(1).trim().removePrefix("**").removeSuffix("**")
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Text("•", color = Color(0xFF6B4EE6), fontSize = 14.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(end = 8.dp))
                    Text(
                        text = cleanLine,
                        color = Color(0xFFA0A0A5),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            } else if (trimmed.isNotEmpty()) {
                Text(
                    text = trimmed.removePrefix("**").removeSuffix("**"),
                    color = Color(0xFFA0A0A5),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Normal,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun WorkoutScreen(viewModel: MainViewModel, onBack: () -> Unit = {}) {
    val profile = viewModel.profile.collectAsState().value
    val workouts = viewModel.workouts.collectAsState().value
    val events = viewModel.getEventsForDate("").collectAsState(initial = emptyList()).value // Need to load events for past days ideally, but we will mock it based on attendance logic
    
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("fitgrow_routines", android.content.Context.MODE_PRIVATE)

    var isEditingRoutine by remember { mutableStateOf(false) }

    val defaultPattern = listOf(
        Triple("Push Day", "Chest, Shoulders, Triceps", 60),
        Triple("Pull Day", "Back, Biceps", 55),
        Triple("Legs", "Quads, Hamstrings, Calves", 65),
        Triple("Active Recovery", "Yoga, Mobility", 30),
        Triple("Upper Body Power", "Chest, Back, Arms", 60),
        Triple("Lower Body Power", "Glutes, Quads", 65),
        Triple("Rest Day", "Recovery", 0)
    )

    var currentPattern by remember {
        mutableStateOf(
            (0..6).map { i ->
                val name = prefs.getString("routine_name_$i", defaultPattern[i].first) ?: defaultPattern[i].first
                val desc = prefs.getString("routine_desc_$i", defaultPattern[i].second) ?: defaultPattern[i].second
                val dur = prefs.getInt("routine_dur_$i", defaultPattern[i].third)
                Triple(name, desc, dur)
            }
        )
    }

    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val todayStr = sdf.format(Calendar.getInstance().time)
    
    val weekDays = remember(currentPattern) {
        val days = mutableListOf<WorkoutDay>()
        val calLoop = Calendar.getInstance()
        calLoop.firstDayOfWeek = Calendar.MONDAY
        calLoop.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        
        for (i in 0..6) {
            val dStr = sdf.format(calLoop.time)
            val isToday = dStr == todayStr
            val isFuture = calLoop.time.after(Calendar.getInstance().time) && !isToday
            val status = if (isFuture) "LOCKED" else "PENDING"
            val dayName = SimpleDateFormat("EEE", Locale.getDefault()).format(calLoop.time).uppercase()
            
            days.add(WorkoutDay(
                dayOfWeek = dayName,
                dateStr = dStr,
                isToday = isToday,
                routineName = currentPattern[i].first,
                muscleGroup = currentPattern[i].second,
                durationMin = currentPattern[i].third,
                status = status
            ))
            calLoop.add(Calendar.DAY_OF_MONTH, 1)
        }
        days
    }
    
    // Check actual status from db
    val attendanceLogs = viewModel.attendance.collectAsState().value
    val allWorkouts = viewModel.workouts.collectAsState().value
    val allNutrition = viewModel.nutritionLogs.collectAsState().value
    val journeyStartVal = viewModel.journeyStartTime.collectAsState().value
    val startDateStr = if (journeyStartVal > 0) sdf.format(java.util.Date(journeyStartVal)) else ""

    val displayDays = weekDays.map { wd ->
        val hasWorkout = allWorkouts.any { it.dateStr == wd.dateStr }
        val hasNutrition = allNutrition.any { it.dateStr == wd.dateStr }
        val isBeforeStart = startDateStr.isNotEmpty() && wd.dateStr < startDateStr
        
        val finalStatus = when {
            isBeforeStart -> "BEFORE_START"
            wd.isToday -> {
                if (hasWorkout || hasNutrition) "DONE" else "PENDING"
            }
            wd.dateStr > todayStr -> "LOCKED"
            else -> {
                if (hasWorkout || hasNutrition) "DONE" else "MISSED"
            }
        }
        wd.copy(status = finalStatus)
    }

    if (isEditingRoutine) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { isEditingRoutine = false }) {
            Box(modifier = Modifier.fillMaxWidth().height(500.dp).background(Color(0xFF15161A), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp)).padding(16.dp)) {
                Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                    Text("EDIT WEEKLY ROUTINE", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    var tempPattern by remember { mutableStateOf(currentPattern.toMutableList()) }
                    
                    val dayNames = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday")
                    
                    for (i in 0..6) {
                        Text(dayNames[i], color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = tempPattern[i].first,
                            onValueChange = { tempPattern = tempPattern.toMutableList().apply { set(i, Triple(it, tempPattern[i].second, tempPattern[i].third)) } },
                            label = { Text("Workout Level/Name") },
                            colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedLabelColor = Color(0xFFA0A0A5)),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        androidx.compose.material3.OutlinedTextField(
                            value = tempPattern[i].second,
                            onValueChange = { tempPattern = tempPattern.toMutableList().apply { set(i, Triple(tempPattern[i].first, it, tempPattern[i].third)) } },
                            label = { Text("Focus/Muscles") },
                            colors = OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedLabelColor = Color(0xFFA0A0A5)),
                            modifier = Modifier.fillMaxWidth().height(56.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    
                    Button(
                        onClick = {
                            val editor = prefs.edit()
                            for (i in 0..6) {
                                editor.putString("routine_name_$i", tempPattern[i].first)
                                editor.putString("routine_desc_$i", tempPattern[i].second)
                                editor.putInt("routine_dur_$i", tempPattern[i].third)
                            }
                            editor.apply()
                            currentPattern = tempPattern.toList()
                            isEditingRoutine = false
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EE6))
                    ) {
                        Text("SAVE ROUTINE", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    LazyColumn(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10)).padding(horizontal = 24.dp)) {
        item {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("WORKOUT PLAN", color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Intermediate Level", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Phase 2: Hypertrophy Focus", color = Color(0xFF6B4EE6), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                
                val daysLeftVal = viewModel.daysLeft.collectAsState().value
                
                Box(
                    modifier = Modifier.padding(start = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    // Purple Glow background
                    Box(
                        modifier = Modifier
                            .size(70.dp, 45.dp)
                            .background(Color(0xFF9E82FF).copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                            .blur(12.dp)
                    )
                    Box(
                        modifier = Modifier
                            .background(Color(0xFF15161A), RoundedCornerShape(12.dp))
                            .border(1.5.dp, Color(0xFF9E82FF), RoundedCornerShape(12.dp))
                            .clip(RoundedCornerShape(12.dp))
                            .size(72.dp, 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        ProfileParticleEffect()
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                            Text(
                                text = "DAYS LEFT",
                                color = Color(0xFFA0A0A5),
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$daysLeftVal",
                                color = Color(0xFF9E82FF),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Calendar Week
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                displayDays.forEach { day ->
                    val isToday = day.isToday
                    val isBeforeStart = day.status == "BEFORE_START"
                    
                    val bgColor = when {
                        isToday -> Color(0xFF6B4EE6)
                        else -> Color(0xFF15161A)
                    }
                    
                    val txtColor = when {
                        isToday -> Color.White
                        isBeforeStart -> Color(0x4DFFFFFF) // greyed out
                        else -> Color(0xFFFFFFFF) // normal white font
                    }
                    
                    val outBorder = when {
                        isToday -> Color(0xFF9E82FF)
                        isBeforeStart -> Color(0x1AFFFFFF) // faint border
                        else -> Color(0xFF222328)
                    }
                    
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .height(72.dp)
                            .background(bgColor, RoundedCornerShape(12.dp))
                            .border(1.dp, outBorder, RoundedCornerShape(12.dp))
                            .padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            day.dayOfWeek, 
                            color = txtColor.copy(alpha = if (isToday) 0.8f else 0.5f), 
                            fontSize = 9.sp, 
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            day.dateStr.substring(8), 
                            color = txtColor, 
                            fontSize = 16.sp, 
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        
                        // Status dot
                        val dotColor = when (day.status) {
                            "DONE" -> Color(0xFF00C853)
                            "MISSED" -> Color(0xFFFF1744)
                            "BEFORE_START" -> Color.Transparent
                            else -> Color.Transparent
                        }
                        if (dotColor != Color.Transparent) {
                            Box(modifier = Modifier.size(5.dp).background(dotColor, CircleShape))
                        } else {
                            Spacer(modifier = Modifier.height(5.dp))
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Action items for Today
        val todayData = displayDays.find { it.isToday }
        if (todayData != null) {
            item {
                Text("TODAY'S MISSION", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
                ) {
                    val imgRes = when {
                        todayData.routineName.contains("Push", ignoreCase = true) -> null // com.example.R.drawable.push_day
                        todayData.routineName.contains("Pull", ignoreCase = true) -> null // com.example.R.drawable.pull_day
                        todayData.routineName.contains("Legs", ignoreCase = true) -> null // com.example.R.drawable.legs_day
                        else -> null
                    }
                    if (imgRes != null) {
                        Box(modifier = Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(200.dp).clip(RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp))) {
                            androidx.compose.foundation.Image(
                                painter = androidx.compose.ui.res.painterResource(id = imgRes),
                                contentDescription = null,
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().blur(16.dp),
                                alpha = 0.1f
                            )
                            // Dark overlay gradient
                            Box(modifier = Modifier.fillMaxSize().background(
                                androidx.compose.ui.graphics.Brush.horizontalGradient(
                                    colors = listOf(Color(0xFF15161A), Color.Transparent),
                                    startX = 0f,
                                    endX = Float.POSITIVE_INFINITY
                                )
                            ))
                        }
                    }
                
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.background(Color(0xFF6B4EE6).copy(alpha=0.2f), RoundedCornerShape(8.dp)).padding(horizontal = 10.dp, vertical = 4.dp)) {
                                Text(todayData.muscleGroup.uppercase(), color = Color(0xFF9E82FF), fontSize = 10.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Schedule, null, tint = Color(0xFFA0A0A5), modifier = Modifier.size(14.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${todayData.durationMin} Min", color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(todayData.routineName, color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (todayData.status == "DONE") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFF00C853).copy(alpha=0.15f), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF00C853), RoundedCornerShape(12.dp)).pointerInput(Unit) { detectTapGestures(onDoubleTap = { viewModel.resetWorkout(todayStr) }) }.padding(16.dp),
                                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00C853))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("WORKOUT COMPLETED", color = Color(0xFF00C853), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Double tap to unmark", color = Color(0xFFA0A0A5), fontSize = 11.sp)
                            }
                        } else if (todayData.status == "MISSED") {
                            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().background(Color(0xFFFF1744).copy(alpha=0.15f), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFFFF1744), RoundedCornerShape(12.dp)).pointerInput(Unit) { detectTapGestures(onDoubleTap = { viewModel.resetWorkout(todayStr) }) }.padding(16.dp),
                                    horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Cancel, null, tint = Color(0xFFFF1744))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("WORKOUT MISSED", color = Color(0xFFFF1744), fontWeight = FontWeight.Bold)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Double tap to unmark", color = Color(0xFFA0A0A5), fontSize = 11.sp)
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                Box(
                                    modifier = Modifier.weight(1f).background(Color(0xFF6B4EE6), RoundedCornerShape(12.dp)).clickable { viewModel.addWorkout(todayData.routineName, todayStr) }.padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("MARK COMPLETED", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                }
                                Box(
                                    modifier = Modifier.size(52.dp).background(Color(0xFF15161A), RoundedCornerShape(12.dp)).border(1.dp, Color(0xFF222328), RoundedCornerShape(12.dp)).clickable { viewModel.missWorkout(todayStr) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Close, null, tint = Color(0xFFFF1744))
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // AI Coach Tailored Suggestions Guide
        item {
            val suggestions by viewModel.aiWorkoutSuggestions.collectAsState()
            val isGenerating by viewModel.isAiWorkoutGenerating.collectAsState()

            Text("AI COACH TRAINING GUIDE", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.AutoAwesome,
                                contentDescription = "AI Coach Guide",
                                tint = Color(0xFF9E82FF),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "TAILORED SUGGESTIONS",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        if (!isGenerating) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (suggestions != null) {
                                    Box(
                                        modifier = Modifier
                                            .background(Color(0xFFFF1744).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                            .border(1.dp, Color(0xFFFF1744), RoundedCornerShape(8.dp))
                                            .clickable { viewModel.clearAiWorkoutSuggestions() }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(
                                            "CLOSE",
                                            color = Color(0xFFFF1744),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFF6B4EE6).copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                                        .border(1.dp, Color(0xFF6B4EE6), RoundedCornerShape(8.dp))
                                        .clickable { viewModel.generateAiWorkoutSuggestions() }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        if (suggestions == null) "GENERATE" else "REFRESH",
                                        color = Color(0xFF9E82FF),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))

                    if (isGenerating) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            androidx.compose.material3.CircularProgressIndicator(
                                color = Color(0xFF6B4EE6),
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                "AI Coach is analyzing goals & history...",
                                color = Color(0xFFA0A0A5),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    } else if (suggestions != null) {
                        SimpleMarkdownText(text = suggestions!!)
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Get a customized guide with specific exercises, reps, sets, and expert Hinglish tips designed just for you based on target goals & logged history.",
                                color = Color(0xFFA0A0A5),
                                fontSize = 12.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.generateAiWorkoutSuggestions() },
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EE6)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("ANALYZE & GENERATE PLAN", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Full Schedule
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WEEKLY SCHEDULE",
                    color = Color.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color(0xFF15161A), CircleShape)
                        .border(1.dp, Color(0xFF222328), CircleShape)
                        .clickable { isEditingRoutine = true },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = "Settings",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        items(displayDays.reversed()) { day ->
            val alpha = if (day.status == "LOCKED" || day.status == "BEFORE_START") 0.5f else 1f
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp)
                    .background(Color(0xFF15161A).copy(alpha = alpha), RoundedCornerShape(10.dp))
                    .border(1.dp, Color(0xFF222328).copy(alpha = alpha), RoundedCornerShape(10.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Day Block
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(32.dp)) {
                    Text(day.dayOfWeek.take(3), color = Color(0xFFA0A0A5), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    Text(day.dateStr.substring(8), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.ExtraBold)
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                // Middle Info
                Column(modifier = Modifier.weight(1f)) {
                    Text(day.routineName, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(day.muscleGroup, color = Color(0xFF9E82FF), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(Icons.Default.Schedule, null, tint = Color(0xFFA0A0A5), modifier = Modifier.size(10.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text("${day.durationMin}m", color = Color(0xFFA0A0A5), fontSize = 10.sp)
                    }
                }
                
                Spacer(modifier = Modifier.width(8.dp))
                // Right Status
                if (day.status == "DONE") {
                    Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
                } else if (day.status == "MISSED") {
                    Icon(Icons.Default.Cancel, null, tint = Color(0xFFFF1744), modifier = Modifier.size(16.dp))
                } else if (day.status == "LOCKED" || day.status == "BEFORE_START") {
                    Icon(Icons.Default.Lock, null, tint = Color(0xFF222328), modifier = Modifier.size(14.dp))
                } else {
                    Box(modifier = Modifier.size(14.dp).border(2.dp, Color(0xFF222328), CircleShape))
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
            // Analytics section
            Text("ANALYTICS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                val consistency = viewModel.successProbability.collectAsState().value

                Column {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("Consistency", color = Color(0xFFA0A0A5), fontSize = 12.sp)
                            Text("$consistency%", color = Color(0xFF00C853), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text("Current Streak", color = Color(0xFFA0A0A5), fontSize = 12.sp)
                            Text("${profile?.streak ?: 0} Days", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    
                    // Simple Chart based on real day-by-day actions (Last 14 Days)
                    val sdfChart = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
                    val recentDays = remember {
                        (0 until 14).map { offset ->
                            val c = Calendar.getInstance()
                            c.add(Calendar.DAY_OF_YEAR, -13 + offset)
                            sdfChart.format(c.time)
                        }
                    }

                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                        val path = Path()
                        val stepX = size.width / 13f
                        
                        val points: List<androidx.compose.ui.geometry.Offset> = recentDays.mapIndexed { idx, dayStr ->
                            var completedScore = 0f
                            for (j in 0..idx) {
                                val d = recentDays[j]
                                val hasW = allWorkouts.any { it.dateStr == d }
                                val hasN = allNutrition.any { it.dateStr == d }
                                completedScore += (if (hasW) 0.5f else 0f) + (if (hasN) 0.5f else 0f)
                            }
                            
                            val maxPossible = idx + 1
                            val runningAverage = if (maxPossible > 0) completedScore / maxPossible else 0f
                            
                            // Map runningAverage 0f..1f to Y coordinate (0.15f to 0.85f of height to avoid cutting off at edges)
                            val y = size.height * (1.0f - (runningAverage * 0.7f + 0.15f))
                            val x = idx * stepX
                            androidx.compose.ui.geometry.Offset(x, y)
                        }
                        
                        // Draw connecting path
                        if (points.isNotEmpty()) {
                            path.moveTo(points.first().x, points.first().y)
                            for (i in 1 until points.size) {
                                path.lineTo(points[i].x, points[i].y)
                            }
                            drawPath(
                                path,
                                color = Color(0xFF6B4EE6),
                                style = Stroke(
                                    width = 5f,
                                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                                    join = androidx.compose.ui.graphics.StrokeJoin.Round
                                )
                            )
                            
                            // Draw dots on each point
                            points.forEach { pt ->
                                drawCircle(
                                    color = Color(0xFF9E82FF),
                                    radius = 5f,
                                    center = pt
                                )
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(120.dp))
        }
    }
}

@Composable
fun ProfileParticleEffect() {
    val infiniteTransition = rememberInfiniteTransition(label = "particles")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "particle_time"
    )

    val particles = remember {
        List(12) {
            ParticleData(
                startX = Random.nextFloat(),
                startY = Random.nextFloat(),
                speed = Random.nextFloat() * 0.5f + 0.5f,
                radius = Random.nextFloat() * 3f + 1f,
                angleOffset = Random.nextFloat() * (2 * Math.PI).toFloat(),
                directionX = if (Random.nextBoolean()) 1f else -1f,
                directionY = if (Random.nextBoolean()) 1f else -1f
            )
        }
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        for (p in particles) {
            // Movement logic: orbit around start pos, plus slight drift
            val x = w * p.startX + sin(time * Math.PI.toFloat() * 2f * p.speed + p.angleOffset) * 10f * p.directionX
            val y = h * p.startY + cos(time * Math.PI.toFloat() * 2f * p.speed + p.angleOffset) * 10f * p.directionY
            
            // Fade particles in and out based on time + offset
            val alpha = (sin(time * Math.PI.toFloat() * 2f + p.angleOffset) + 1f) / 2f

            drawCircle(
                color = Color(0xFF9E82FF).copy(alpha = alpha * 0.8f),
                radius = p.radius,
                center = Offset(x, y)
            )
        }
    }
}

data class ParticleData(
    val startX: Float,
    val startY: Float,
    val speed: Float,
    val radius: Float,
    val angleOffset: Float,
    val directionX: Float,
    val directionY: Float
)
