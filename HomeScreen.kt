package com.example.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Adjust
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.ArrowOutward
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Face
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.LocalMall
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.sin
import kotlin.math.cos
import kotlin.random.Random
import com.example.MainViewModel
import com.example.ui.components.MetricCard
import com.example.ui.theme.*
import java.util.Calendar
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.abs

import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle

@Composable
fun HomeScreen(
    viewModel: MainViewModel,
    onProfileClick: () -> Unit = {},
    onNutritionClick: () -> Unit = {},
    onWorkoutClick: () -> Unit = {},
    onProgressClick: () -> Unit = {}
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val profile = viewModel.profile.collectAsState().value

    val cal = Calendar.getInstance()
    var currentHour by remember { mutableStateOf(cal.get(Calendar.HOUR_OF_DAY)) }
    var currentMinute by remember { mutableStateOf(cal.get(Calendar.MINUTE)) }

    val today = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().time)
    val dailyNutrition = viewModel.getNutritionForDate(today).collectAsState(initial = emptyList()).value
    val dailyWater = viewModel.getWaterForDate(today).collectAsState(initial = null).value
    val dailyEvents = viewModel.getEventsForDate(today).collectAsState(initial = emptyList()).value

    val totalCals = dailyNutrition.sumOf { it.calories }
    val totalProtein = dailyNutrition.sumOf { it.protein }
    val totalWater = dailyWater?.totalWaterMl ?: 0

    val targetCals = profile?.targetCalories ?: 2000
    val targetProtein = profile?.targetProtein ?: 150
    val targetWater = profile?.targetWater ?: 3000

    val calProgress = if (targetCals > 0) (totalCals / targetCals.toFloat()) else 0f
    val protProgress = if (targetProtein > 0) (totalProtein / targetProtein.toFloat()) else 0f
    val waterProgress = if (targetWater > 0) (totalWater / targetWater.toFloat()) else 0f

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    data class EventDef(val name: String, val timeStr: String, val hour: Int, val min: Int)

    val events = listOf(
        EventDef("BREAKFAST", profile?.breakfastTime ?: "08:00", profile?.breakfastTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 8, profile?.breakfastTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0),
        EventDef("LUNCH TIME", profile?.lunchTime ?: "13:00", profile?.lunchTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 13, profile?.lunchTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0),
        EventDef("GYM TIME", profile?.gymTime ?: "18:00", profile?.gymTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 18, profile?.gymTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0),
        EventDef("DINNER TIME", profile?.dinnerTime ?: "20:00", profile?.dinnerTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 20, profile?.dinnerTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0)
    ).sortedBy { it.hour * 60 + it.min }

    var currentEventTitle by remember { mutableStateOf("FOCUS & HYDRATE") }
    var currentEventSub by remember { mutableStateOf("") }
    var isCurrentEventActive by remember { mutableStateOf(false) }
    var currentEventNameSaved by remember { mutableStateOf("") }
    var currentEventTimeActive by remember { mutableStateOf("") }
    var gradientColors by remember { mutableStateOf(listOf(Color(0xFF232526), Color(0xFF414345))) }

    LaunchedEffect(currentHour, currentMinute, profile, dailyEvents) {
        val currTimeMins = currentHour * 60 + currentMinute
        
        var foundActive = false
        for (ev in events) {
            val evTimeMins = ev.hour * 60 + ev.min
            // Active if within -1 hr to +1 hr of the event time
            if (currTimeMins in (evTimeMins - 60)..(evTimeMins + 60)) {
                foundActive = true
                currentEventTitle = ev.name
                currentEventNameSaved = ev.name
                val ampm = if (ev.hour >= 12) "PM" else "AM"
                val h12 = if (ev.hour == 0) 12 else if (ev.hour > 12) ev.hour - 12 else ev.hour
                currentEventTimeActive = String.format(java.util.Locale.US, "%02d:%02d %s", h12, ev.min, ampm)
                currentEventSub = "CURRENTLY"
                isCurrentEventActive = true
                gradientColors = when (ev.name) {
                    "GYM TIME" -> listOf(Color(0xFFFF416C), Color(0xFFFF4B2B))
                    "BREAKFAST" -> listOf(Color(0xFFF2994A), Color(0xFFF2C94C))
                    "LUNCH TIME" -> listOf(Color(0xFF56CCF2), Color(0xFF2F80ED))
                    "DINNER TIME" -> listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0))
                    else -> listOf(Color(0xFF232526), Color(0xFF414345))
                }
                break
            }
        }
        
        if (!foundActive) {
            currentEventTitle = "STAY HYDRATED"
            currentEventNameSaved = "HYDRATION"
            currentEventSub = "REST & RECOVER"
            currentEventTimeActive = "Always"
            isCurrentEventActive = false
            gradientColors = listOf(Color(0xFF2C3E50), Color(0xFF000000))
        }
    }

    val currentW = profile?.currentWeight ?: 0f
    val targetW = profile?.targetWeight ?: 0f
    val days = profile?.targetDays ?: 30
    var goalSignal = ""
    var goalColor = Color.Gray

    if (days > 0) {
        val diff = abs(currentW - targetW)
        val kgPerDay = diff / days
        when {
            kgPerDay > 0.15f -> {
                goalSignal = "HARD: Eat STRICT. ~${profile?.targetCalories?.minus(500)} kcal/day. Very difficult."
                goalColor = Color.Red
            }
            kgPerDay > 0.05f -> {
                goalSignal = "MODERATE: Consistency is key. ~${profile?.targetCalories} kcal/day."
                goalColor = Color(0xFFD4AF37)
            }
            else -> {
                goalSignal = "EASY: Relaxed pace. ~${profile?.targetCalories?.plus(200)} kcal/day."
                goalColor = Color(0xFF4CAF50)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF09090A))) {
        // Dark overlay
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    androidx.compose.ui.graphics.Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color(0xFF09090A)),
                        startY = 0f,
                        endY = Float.POSITIVE_INFINITY
                    )
                )
        )

        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp)) {
            val greeting = when (currentHour) {
                in 5..11 -> "Good Morning,"
                in 12..16 -> "Good Afternoon,"
                in 17..21 -> "Good Evening,"
                else -> "Good Night,"
            }

            val prefs = context.getSharedPreferences("fitgrow_accountability", android.content.Context.MODE_PRIVATE)
            val startTime = prefs.getLong("journey_start_time", 0L)
            val daysPassed = if (startTime == 0L) 14 else {
                val diffMs = System.currentTimeMillis() - startTime
                val diffDays = (diffMs / (24 * 60 * 60 * 1000L)).toInt()
                diffDays + 1
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .background(Color(0xFF15161A), CircleShape)
                            .clip(CircleShape)
                            .clickable { onProfileClick() }
                            .border(1.dp, Color(0xFF7B61FF).copy(alpha = 0.5f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        if (profile?.currentPictureUri != null) {
                            val mappedUri = if (profile.currentPictureUri.startsWith("/")) "file://${profile.currentPictureUri}" else profile.currentPictureUri
                            coil.compose.AsyncImage(model = mappedUri, contentDescription = "Profile", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                        } else {
                            Icon(Icons.Default.Person, contentDescription = "Profile", tint = Color.White)
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(greeting, color = Color(0xFFA0A0A5), fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text((profile?.name ?: "User") + " 👋", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text("Day $daysPassed of your transformation", color = Color(0xFFA0A0A5), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    }
                }
                
                // Beautiful top-right Streak card
                Box(
                    modifier = Modifier
                        .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
                        .border(1.dp, Color(0xFF7B61FF).copy(alpha = 0.3f), RoundedCornerShape(16.dp))
                        .clickable { onProgressClick() }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = "Streak",
                                tint = Color(0xFFFFB74D),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${profile?.streak ?: 0} Days",
                                color = Color(0xFF9E82FF),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Text(
                            text = "Current Streak",
                            color = Color(0xFFA0A0A5),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Normal,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Moderate Insight Card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(Brush.radialGradient(listOf(Color(0xFF4C4DFF), Color(0xFF1E1E40))), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Star, "Insight", tint = Color.White, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        withStyle(androidx.compose.ui.text.SpanStyle(color = Color.White, fontWeight = FontWeight.Bold)) {
                            append(goalSignal.substringBefore(":"))
                        }
                        withStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFFA0A0A5))) {
                            append(":")
                            append(goalSignal.substringAfter(":").substringBefore("~").trim())
                        }
                    },
                    fontSize = 12.sp
                )
                Text(
                    text = "~${profile?.targetCalories ?: 2000} kcal/day",
                    color = Color(0xFFFFB74D),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Row(
                modifier = Modifier
                    .border(1.dp, Color(0xFF222328), RoundedCornerShape(8.dp))
                    .clickable { onNutritionClick() }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.ShowChart, "Insights", tint = Color(0xFFA0A0A5), modifier = Modifier.size(14.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("View Insights", color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        val eventLog = dailyEvents.find { it.eventType == currentEventNameSaved }
        val isEventDone = eventLog?.status == "DONE"
        val isEventMissed = eventLog?.status == "MISSED"

        val foodEvents = listOf(
            EventDef("BREAKFAST", profile?.breakfastTime ?: "08:00", profile?.breakfastTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 8, profile?.breakfastTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0),
            EventDef("LUNCH TIME", profile?.lunchTime ?: "13:00", profile?.lunchTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 13, profile?.lunchTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0),
            EventDef("DINNER TIME", profile?.dinnerTime ?: "20:00", profile?.dinnerTime?.split(":")?.getOrNull(0)?.toIntOrNull() ?: 20, profile?.dinnerTime?.split(":")?.getOrNull(1)?.toIntOrNull() ?: 0)
        ).sortedBy { it.hour * 60 + it.min }

        val currTimeMins = currentHour * 60 + currentMinute
        val nextFoodEvent = foodEvents.firstOrNull { (it.hour * 60 + it.min) > currTimeMins } ?: foodEvents.firstOrNull()
        val nextFoodName = nextFoodEvent?.name ?: "BREAKFAST"
        val nextFoodHour = nextFoodEvent?.hour ?: 8
        val nextFoodMin = nextFoodEvent?.min ?: 0
        val nextFoodAmPm = if (nextFoodHour >= 12) "PM" else "AM"
        val nextFoodH12 = if (nextFoodHour == 0) 12 else if (nextFoodHour > 12) nextFoodHour - 12 else nextFoodHour
        val nextFoodTimeStr = String.format(java.util.Locale.US, "%02d:%02d %s", nextFoodH12, nextFoodMin, nextFoodAmPm)

        // Massive Dynamic Timer Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(18f / 9f)
                .background(Color(0xFF15161A), RoundedCornerShape(24.dp))
                .border(1.dp, Color(0xFF222328), RoundedCornerShape(24.dp))
                .clickable { 
                    if (isEventDone || isEventMissed) {
                        viewModel.markEvent(today, currentEventNameSaved, "PENDING")
                    } else if (currentEventNameSaved.contains("GYM", ignoreCase = true)) {
                        onWorkoutClick() 
                    }
                }
        ) {
            val eventBannerImgRes = when (currentEventNameSaved) {
                "BREAKFAST" -> com.example.R.drawable.bg_breakfast
                "LUNCH TIME" -> com.example.R.drawable.bg_lunch
                "GYM TIME" -> com.example.R.drawable.bg_gym
                "DINNER TIME" -> com.example.R.drawable.bg_dinner
                else -> com.example.R.drawable.bg_focus
            }
            // Event Image Background
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
            ) {
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(id = eventBannerImgRes),
                    contentDescription = currentEventNameSaved,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                    alpha = 0.8f // High visibility but slight dimming
                )
                // Gradient overlay from left (black) to right (transparent) for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(Color(0xFF15161A).copy(alpha = 0.9f), Color.Transparent, Color.Transparent),
                                startX = 0f,
                                endX = 800f
                            )
                        )
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 20.dp, horizontal = 24.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.Start
            ) {
                Column {
                    Text(currentEventSub.uppercase(), color = Color(0xFFA0A0A5), fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    
                    val parts = currentEventTitle.split(" ")
                    val lastPart = if (parts.size > 1) parts.last() else currentEventTitle.takeLast(3)
                    val firstPart = currentEventTitle.removeSuffix(lastPart)
                    
                    Row {
                        Text(firstPart, color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                        Text(lastPart, style = TextStyle(brush = Brush.horizontalGradient(listOf(Color(0xFF6B4EE6), Color(0xFF4C4DFF)))), fontSize = 26.sp, fontWeight = FontWeight.ExtraBold)
                    }
                    
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    // The time block (Icon + Time: + actual time)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Schedule, "Time", tint = Color(0xFFA0A0A5), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Time: ", style = TextStyle(brush = Brush.horizontalGradient(listOf(Color(0xFF6B4EE6), Color(0xFF4C4DFF)))), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(currentEventTimeActive, color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
                
                Text(
                    text = "NEXT MEAL: $nextFoodName AT $nextFoodTimeStr",
                    color = Color(0xFFFFB74D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("TODAY'S TRACKING", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onNutritionClick() }) {
                Text("View Details", color = Color(0xFFA0A0A5), fontSize = 12.sp)
                Spacer(modifier = Modifier.width(4.dp))
                Icon(Icons.Default.ArrowOutward, null, tint = Color(0xFFA0A0A5), modifier = Modifier.size(12.dp))
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Nutrition Circular Tracking
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val waterStr = String.format("%.1f", totalWater / 1000f) + "L\n/${targetWater / 1000}L"
            CircularProgressIndicatorCustom("Cals", calProgress, Color(0xFFFFB74D), "$totalCals\n/$targetCals", "${(calProgress * 100).toInt()}%")
            CircularProgressIndicatorCustom("Protein", protProgress, Color(0xFFE91E63), "${totalProtein}g\n/$targetProtein", "${(protProgress * 100).toInt()}%")
            CircularProgressIndicatorCustom("Water", waterProgress, Color(0xFF03A9F4), waterStr, "${(waterProgress * 100).toInt()}%")
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        val weightHistory = viewModel.weightHistory.collectAsState().value
        val weightPoints = remember(weightHistory) { weightHistory.map { it.second } }

        val startW = profile?.startWeight ?: 0f
        val currW = profile?.currentWeight ?: 0f
        val targW = profile?.targetWeight ?: 0f
        val progressPercent = if (targW != startW) {
            val p = (currW - startW) / (targW - startW)
            (p.coerceIn(0f, 1f) * 100).toInt()
        } else {
            0
        }

        val pointsToDraw = remember(weightPoints, startW, currW) {
            if (weightPoints.size >= 2) {
                weightPoints
            } else {
                val pts = mutableListOf<Float>()
                val steps = 5
                for (i in 0 until steps) {
                    val t = i.toFloat() / (steps - 1)
                    val jitter = if (i > 0 && i < steps - 1) (kotlin.random.Random(1337 + i).nextFloat() - 0.5f) * 0.15f else 0f
                    pts.add(startW + t * (currW - startW) + jitter)
                }
                pts
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            WeightGraphCard(title = "STARTING", value = "${startW} kg", color = Color(0xFF9E82FF), lineMode = 0, startW = startW, targW = targW, pointsToDraw = pointsToDraw, modifier = Modifier.weight(1f).fillMaxHeight())
            WeightGraphCard(title = "CURRENT", value = "${currW} kg", color = Color(0xFF03A9F4), lineMode = 1, startW = startW, targW = targW, pointsToDraw = pointsToDraw, modifier = Modifier.weight(1f).fillMaxHeight(), actionLabel = "UPDATE", onAction = { onProfileClick() })
            WeightGraphCard(title = "TARGET", value = "${targW} kg", color = Color(0xFF4CAF50), lineMode = 2, startW = startW, targW = targW, pointsToDraw = pointsToDraw, modifier = Modifier.weight(1f).fillMaxHeight())
        }

        Spacer(modifier = Modifier.height(24.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ShowChart, "Progress", tint = Color(0xFFA0A0A5), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("OVERALL PROGRESS", color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
            Text("$progressPercent%", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .background(Color(0xFF1E1E24), CircleShape)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progressPercent / 100f)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF4C4DFF), Color(0xFF9E82FF))), CircleShape)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Beautiful Instagram Promo Card instead of Coach Insights
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    try {
                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse("https://www.instagram.com/growswami_?igsh=ZHZjN3o4aXFmeG9i&utm_source=qr"))
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                },
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFF7B61FF).copy(alpha = 0.2f)),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF15161A))
        ) {
            Row(
                modifier = Modifier.padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        append("Follow developer ")
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = Color(0xFF9E82FF), fontWeight = FontWeight.Bold))
                        append("@growswami_")
                        pop()
                        append(" on Instagram")
                    },
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    imageVector = Icons.Default.ArrowOutward,
                    contentDescription = "Open Link",
                    tint = Color(0xFF9E82FF),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(140.dp))
        }
    }
}

@Composable
fun WeightGraphCard(
    title: String,
    value: String,
    color: Color,
    lineMode: Int = 0,
    startW: Float = 0f,
    targW: Float = 0f,
    pointsToDraw: List<Float> = emptyList(),
    modifier: Modifier = Modifier,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    Box(
        modifier = modifier
            .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
            .padding(14.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    color = Color(0xFFA0A0A5),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                
                val icon = when (title.uppercase()) {
                    "STARTING" -> Icons.Default.LocalMall
                    "CURRENT" -> Icons.Default.ArrowOutward
                    else -> Icons.Default.Adjust
                }
                
                val iconColor = when (title.uppercase()) {
                    "STARTING" -> Color(0xFF7B61FF)
                    "CURRENT" -> Color(0xFF00E5FF)
                    else -> Color(0xFF7B61FF)
                }

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                // Thin neon divider
                Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF7B61FF).copy(alpha = 0.2f)))
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = actionLabel,
                    color = Color(0xFF00E5FF),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { onAction() }
                )
            }
        }
    }
}

@Composable
fun CircularProgressIndicatorCustom(label: String, progress: Float, color: Color, innerText: String, percentageText: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(86.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 20f
                // If progress is 0, use a tiny sweep angle so a small circular dot appears at the start (thanks to round caps)
                val effectiveProgress = if (progress <= 0f) 0.015f else progress
                val sweepAngle = effectiveProgress.coerceIn(0f, 1f) * 300f
                val startAngle = 120f
                
                drawArc(
                    color = Color(0xFF1E1E24),
                    startAngle = startAngle,
                    sweepAngle = 300f,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
                
                drawArc(
                    color = color,
                    startAngle = startAngle,
                    sweepAngle = sweepAngle,
                    useCenter = false,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                )
            }
            
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                val lines = innerText.split("\n")
                if(lines.size == 2) {
                    Text(lines[0].replace("g","").replace("L",""), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text(lines[1], color = Color(0xFFA0A0A5), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                } else {
                    Text(innerText, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                when (label.uppercase()) {
                    "CALS" -> Icons.Default.LocalFireDepartment
                    "PROTEIN" -> Icons.Default.WaterDrop
                    else -> Icons.Default.WaterDrop
                },
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(label.uppercase(), color = Color.Gray, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.sp)
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
