package com.example.ui.screens.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.MainViewModel
import kotlin.math.abs

val BrandPurple = Color(0xFF7B61FF)
val MatteBlack = Color(0xFF09090A)
val GlassWhite = Color(0xFF141519).copy(alpha = 0.97f)
val GlassBorder = Color.White.copy(alpha = 0.12f)

fun formatTime12(hour: Int, min: Int): String {
    val ampm = if (hour >= 12) "PM" else "AM"
    val h12 = if (hour % 12 == 0) 12 else hour % 12
    return "${h12.toString().padStart(2, '0')}:${min.toString().padStart(2, '0')} $ampm"
}

@Composable
fun WheelPickerView(
    items: List<String>,
    selectedIndex: Int,
    onSelectionChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = selectedIndex)
    val haptic = LocalHapticFeedback.current
    var lastHapticIndex by remember { mutableStateOf(selectedIndex) }

    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }.collect { offset ->
            val index = listState.firstVisibleItemIndex + if (offset > 20) 1 else 0
            if (index != lastHapticIndex) {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                lastHapticIndex = index
            }
        }
    }
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            val offset = listState.firstVisibleItemScrollOffset
            val index = listState.firstVisibleItemIndex + if (offset > 20) 1 else 0
            if (index != selectedIndex) {
                onSelectionChanged(index)
            }
        }
    }

    LazyColumn(
        state = listState,
        flingBehavior = rememberSnapFlingBehavior(lazyListState = listState),
        modifier = modifier.height(120.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item { Spacer(modifier = Modifier.height(40.dp)) }
        items(items.size) { index ->
            val isSelected = index == lastHapticIndex
            Box(modifier = Modifier.height(40.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = items[index],
                    fontSize = if (isSelected) 24.sp else 16.sp,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onBackground else Color.Gray,
                )
            }
        }
        item { Spacer(modifier = Modifier.height(40.dp)) }
    }
}

@Composable
fun IosTimePickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, min: Int) -> Unit
) {
    var selectedHour12 by remember { mutableStateOf((if (initialHour % 12 == 0) 12 else initialHour % 12) - 1) }
    var selectedMin by remember { mutableStateOf(initialMinute) }
    var selectedAmPm by remember { mutableStateOf(if (initialHour >= 12) 1 else 0) }

    val hours = (1..12).map { it.toString().padStart(2, '0') }
    val mins = (0..59).map { it.toString().padStart(2, '0') }
    val amPms = listOf("AM", "PM")

    Dialog(onDismissRequest = onDismiss) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.surface) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    WheelPickerView(items = hours, selectedIndex = selectedHour12, onSelectionChanged = { selectedHour12 = it }, modifier = Modifier.weight(1f))
                    Text(":", fontSize = 24.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                    WheelPickerView(items = mins, selectedIndex = selectedMin, onSelectionChanged = { selectedMin = it }, modifier = Modifier.weight(1f))
                    Spacer(modifier = Modifier.width(16.dp))
                    WheelPickerView(items = amPms, selectedIndex = selectedAmPm, onSelectionChanged = { selectedAmPm = it }, modifier = Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(onClick = {
                        val h12 = selectedHour12 + 1
                        val hour24 = if (selectedAmPm == 1 && h12 < 12) h12 + 12
                                     else if (selectedAmPm == 0 && h12 == 12) 0
                                     else h12
                        onConfirm(hour24, selectedMin)
                    }, colors = ButtonDefaults.buttonColors(containerColor = BrandPurple)) { Text("Confirm") }
                }
            }
        }
    }
}

@Composable
fun GlassTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String,
    icon: ImageVector,
    suffix: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier,
    isError: Boolean = false
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isError) Color.Red else (if (isFocused) BrandPurple else GlassBorder)
    val bgColor = if (isFocused) Color(0xFF1C1D22).copy(alpha = 0.98f) else GlassWhite
    val labelColor = if (isError) Color.Red else Color.White
    val iconColor = if (isError) Color.Red else BrandPurple

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Medium),
        modifier = modifier
            .onFocusChanged { isFocused = it.isFocused }
            .background(bgColor, RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .padding(16.dp),
        decorationBox = { innerTextField ->
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(label, color = labelColor, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(2.dp))
                    Box {
                        if (value.isEmpty()) {
                            Text(placeholder, color = Color.Gray, fontSize = 14.sp)
                        }
                        innerTextField()
                    }
                }
                if (suffix != null) {
                    Text(suffix, color = if (isError) Color.Red else Color.Gray, fontSize = 14.sp)
                }
            }
        }
    )
}

@Composable
fun TimeRow(label: String, time24: String, onClick: () -> Unit) {
    val h = time24.split(":").getOrNull(0)?.toIntOrNull() ?: 12
    val m = time24.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    val display = formatTime12(h, m)

    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        Text(display, fontSize = 16.sp, color = BrandPurple, fontWeight = FontWeight.Bold)
    }
    HorizontalDivider(color = MatteBlack)
}

@Composable
fun PremiumTimeRow(icon: ImageVector, label: String, time24: String, onClick: () -> Unit) {
    val h = time24.split(":").getOrNull(0)?.toIntOrNull() ?: 12
    val m = time24.split(":").getOrNull(1)?.toIntOrNull() ?: 0
    val display = formatTime12(h, m)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(label, color = Color.White, fontSize = 16.sp)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(display, color = BrandPurple, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
        }
    }
    HorizontalDivider(color = GlassBorder, thickness = 1.dp)
}

@Composable
fun OnboardingScreen(viewModel: MainViewModel, onFinish: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var targetWeight by remember { mutableStateOf("") }
    var targetDays by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    
    var hasAttemptedSubmit by remember { mutableStateOf(false) }
    var validationErrorMessage by remember { mutableStateOf<String?>(null) }
    
    var gymTime by remember { mutableStateOf("18:00") }
    var breakfastTime by remember { mutableStateOf("08:00") }
    var lunchTime by remember { mutableStateOf("13:00") }
    var dinnerTime by remember { mutableStateOf("20:00") }
    var showTimePickerFor by remember { mutableStateOf<String?>(null) } 

    val currentW = weight.toFloatOrNull() ?: 0f
    val targetW = targetWeight.toFloatOrNull() ?: 0f
    val days = targetDays.toIntOrNull() ?: 0

    var difficultyText by remember { mutableStateOf("") }
    var difficultyColor by remember { mutableStateOf(Color.Gray) }
    var suggestedCalories by remember { mutableStateOf(2000) }
    var suggestedProtein by remember { mutableStateOf(120) }
    var suggestedWater by remember { mutableStateOf(3000) }

    LaunchedEffect(currentW, targetW, days) {
        if (currentW > 0 && targetW > 0 && days > 0) {
            val diff = targetW - currentW
            val absDiff = abs(diff)
            val kgPerDay = absDiff / days
            
            val dailyGoalCals = (kgPerDay * 7700).toInt()
            suggestedCalories = if (diff > 0) 2000 + dailyGoalCals else 2000 - dailyGoalCals
            if (suggestedCalories < 1200) suggestedCalories = 1200 
            
            suggestedProtein = (targetW * 2f).toInt() 
            suggestedWater = 3000 + (absDiff * 50).toInt()

            val statsStr = "(${suggestedCalories} kcal, ${suggestedProtein}g prot, ${suggestedWater/1000f}L water)"

            when {
                kgPerDay > 0.2f -> {
                    difficultyText = "INSANE: Maut ka khel hai meri jaan! 💀 $statsStr"
                    difficultyColor = Color(0xFFAA0000)
                }
                kgPerDay > 0.15f -> {
                    difficultyText = "HARD: Kyu lanka lagwa rha hai bhai? Aag me kudna hai tko? 🥵 $statsStr"
                    difficultyColor = Color.Red
                }
                kgPerDay > 0.07f -> {
                    difficultyText = "MODERATE: Thoda push karna hoga, maintain consistency! 🔥 $statsStr"
                    difficultyColor = Color(0xFFD4AF37)
                }
                kgPerDay > 0.03f -> {
                    difficultyText = "EASY: Sahi pace hai, mast consistency chahiye bas. 👍 $statsStr"
                    difficultyColor = Color(0xFF4CAF50)
                }
                else -> {
                    difficultyText = "VERY EASY: Aaram se, bilkul makkhan. 😄 $statsStr"
                    difficultyColor = Color(0xFF81C784)
                }
            }
        } else {
            difficultyText = ""
        }
    }
    
    if (showTimePickerFor != null) {
        val currentStr = when (showTimePickerFor) {
            "Gym Time" -> gymTime
            "Breakfast" -> breakfastTime
            "Lunch" -> lunchTime
            "Dinner" -> dinnerTime
            else -> "12:00"
        }
        val parts = currentStr.split(":")
        val curHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
        val curMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

        IosTimePickerDialog(
            title = "Select $showTimePickerFor",
            initialHour = curHour,
            initialMinute = curMin,
            onDismiss = { showTimePickerFor = null },
            onConfirm = { h, m ->
                val newVal = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                when (showTimePickerFor) {
                    "Gym Time" -> gymTime = newVal
                    "Breakfast" -> breakfastTime = newVal
                    "Lunch" -> lunchTime = newVal
                    "Dinner" -> dinnerTime = newVal
                }
                showTimePickerFor = null
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize().background(MatteBlack)) {
        // Fullscreen opening background drawable
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.example.R.drawable.opening_background),
            contentDescription = "Opening Background",
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(48.dp))
            
            // Header & Profile Setup
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = androidx.compose.ui.text.buildAnnotatedString {
                        append("Welcome to ")
                        pushStyle(androidx.compose.ui.text.SpanStyle(color = BrandPurple))
                        append("FitGrow")
                        pop()
                    },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Let's set up your profile to begin your transformation.",
                    fontSize = 15.sp,
                    color = Color.LightGray,
                    lineHeight = 22.sp
                )
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            
            // Input Fields
            GlassTextField(
                value = name,
                onValueChange = { name = it },
                label = "Your Name",
                placeholder = "Enter your name",
                icon = Icons.Default.Person,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                isError = hasAttemptedSubmit && name.isBlank()
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
                GlassTextField(
                    value = weight,
                    onValueChange = { weight = it },
                    label = "Current Weight",
                    placeholder = "Enter in kg",
                    icon = Icons.Default.MonitorWeight,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                    isError = hasAttemptedSubmit && (weight.isBlank() || (weight.toFloatOrNull() ?: 0f) <= 0f)
                )
                GlassTextField(
                    value = targetWeight,
                    onValueChange = { targetWeight = it },
                    label = "Target Weight",
                    placeholder = "Enter in kg",
                    icon = Icons.Default.AdsClick,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    modifier = Modifier.weight(1f),
                    isError = hasAttemptedSubmit && (targetWeight.isBlank() || (targetWeight.toFloatOrNull() ?: 0f) <= 0f)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassTextField(
                value = targetDays,
                onValueChange = { targetDays = it },
                label = "In how many days?",
                placeholder = "e.g. 90",
                icon = Icons.Default.CalendarToday,
                suffix = "days",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                isError = hasAttemptedSubmit && (targetDays.isBlank() || (targetDays.toIntOrNull() ?: 0) <= 0)
            )
            
            if (difficultyText.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(color = difficultyColor.copy(alpha=0.1f), shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    Text(difficultyText, color = difficultyColor, fontWeight = FontWeight.Bold, fontSize = 14.sp, modifier = Modifier.padding(16.dp), lineHeight = 20.sp)
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            GlassTextField(
                value = height,
                onValueChange = { height = it },
                label = "Height (cm)",
                placeholder = "Enter your height",
                icon = Icons.Default.Height,
                suffix = "cm",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                isError = hasAttemptedSubmit && (height.isBlank() || (height.toFloatOrNull() ?: 0f) <= 0f)
            )

            Spacer(modifier = Modifier.height(16.dp))
            
            // Info Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandPurple.copy(alpha = 0.05f), RoundedCornerShape(16.dp))
                    .border(
                        1.dp,
                        Brush.linearGradient(listOf(BrandPurple.copy(alpha = 0.5f), Color.Transparent)),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = BrandPurple, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Why we need this?", color = BrandPurple, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "This helps our AI coach analyze your stats and create a personalized plan just for you.",
                            color = Color.LightGray,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("YOUR TIMINGS", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = BrandPurple, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.width(8.dp))
                Text("(You can change later)", fontSize = 12.sp, color = Color.Gray)
            }
            Spacer(modifier = Modifier.height(16.dp))
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(GlassWhite, RoundedCornerShape(16.dp))
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp)
            ) {
                PremiumTimeRow(icon = Icons.Default.Restaurant, label = "Breakfast", time24 = breakfastTime) { showTimePickerFor = "Breakfast" }
                PremiumTimeRow(icon = Icons.Default.LunchDining, label = "Lunch", time24 = lunchTime) { showTimePickerFor = "Lunch" }
                PremiumTimeRow(icon = Icons.Default.DinnerDining, label = "Dinner", time24 = dinnerTime) { showTimePickerFor = "Dinner" }
                PremiumTimeRow(icon = Icons.Default.FitnessCenter, label = "Gym Time", time24 = gymTime) { showTimePickerFor = "Gym Time" }
            }
            
            Spacer(modifier = Modifier.height(24.dp))

            if (validationErrorMessage != null) {
                Surface(
                    color = Color.Red.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color.Red.copy(alpha = 0.8f)),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Validation Error",
                            tint = Color.Red,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = validationErrorMessage ?: "",
                            color = Color.Red,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Start Journey Button
            Button(
                onClick = {
                    hasAttemptedSubmit = true
                    val hVal = height.toFloatOrNull() ?: 0f
                    if (name.isBlank() || currentW <= 0f || targetW <= 0f || days <= 0 || hVal <= 0f) {
                        val missing = mutableListOf<String>()
                        if (name.isBlank()) missing.add("Your Name")
                        if (currentW <= 0f) missing.add("Current Weight")
                        if (targetW <= 0f) missing.add("Target Weight")
                        if (days <= 0) missing.add("Target Days")
                        if (hVal <= 0f) missing.add("Height")
                        validationErrorMessage = "Please fill all required fields: ${missing.joinToString(", ")}"
                    } else {
                        validationErrorMessage = null
                        viewModel.completeOnboarding(
                            name = name,
                            age = 25,
                            height = hVal,
                            weight = currentW,
                            targetWeight = targetW,
                            targetDays = days,
                            gymTime = gymTime,
                            breakfastTime = breakfastTime,
                            lunchTime = lunchTime,
                            dinnerTime = dinnerTime,
                            targetCalories = suggestedCalories,
                            targetProtein = suggestedProtein,
                            targetWater = suggestedWater
                        )
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .shadow(16.dp, spotColor = BrandPurple, ambientColor = BrandPurple, shape = RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                contentPadding = PaddingValues(0.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.horizontalGradient(listOf(Color(0xFF6C63FF), Color(0xFF8A5CFF))),
                            RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("START JOURNEY", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp, letterSpacing = 1.sp)
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(Icons.Default.ArrowForward, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray.copy(alpha = 0.6f), modifier = Modifier.size(12.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Your data is 100% private and secure", color = Color.Gray.copy(alpha = 0.6f), fontSize = 12.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

