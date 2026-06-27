package com.example.ui.screens.progress

import android.net.Uri
import android.content.Intent
import android.content.Context
import android.widget.Toast
import java.io.File
import androidx.core.content.FileProvider
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.MainViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProgressScreen(viewModel: MainViewModel, onBack: () -> Unit = {}) {
    val isAnalyzing = viewModel.isAnalyzing.collectAsState().value
    val profile = viewModel.profile.collectAsState().value
    val workouts = viewModel.workouts.collectAsState().value
    val attendance = viewModel.attendance.collectAsState().value
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val appContext = androidx.compose.ui.platform.LocalContext.current

    var selectedMilestoneForSharing by remember { mutableStateOf<ProgressMilestone?>(null) }
    var generatedMilestoneFile by remember { mutableStateOf<File?>(null) }
    var isGeneratingMilestoneImage by remember { mutableStateOf(false) }
    var showMilestoneShareSheet by remember { mutableStateOf(false) }
    var showAllMilestonesSheet by remember { mutableStateOf(false) }

    // Weight achievement logic
    val startingW = profile?.startWeight ?: 0f
    val currentW = profile?.currentWeight ?: 0f
    val targetW = profile?.targetWeight ?: 0f
    val streakVal = profile?.streak ?: 0
    val hasPhoto = !profile?.originalPictureUri.isNullOrBlank() || !profile?.currentPictureUri.isNullOrBlank()

    val reachedGoalWeight = currentW > 0f && targetW > 0f && (
        (startingW >= targetW && currentW <= targetW) || 
        (startingW <= targetW && currentW >= targetW)
    )
    val weightDiff = if (startingW > 0f && currentW > 0f) Math.abs(startingW - currentW) else 0f
    val weightDiffStr = String.format(Locale.US, "%.1f", weightDiff)

    val milestonesList = remember(profile, workouts, attendance) {
        listOf(
            ProgressMilestone(
                id = "day_1_active",
                title = "Day 1 Active",
                description = "Started your journey and logged your first check-in!",
                iconTxt = "🏁",
                badgeColor = Color(0xFF9E82FF),
                isUnlocked = attendance.isNotEmpty(),
                statLabel1 = "Logged Days",
                statValue1 = "${attendance.size} Days",
                statLabel2 = "Current Streak",
                statValue2 = "$streakVal Days",
                coachMessage = "Every epic journey starts with a single step. You are officially in the game! Let's build consistency."
            ),
            ProgressMilestone(
                id = "streak_3",
                title = "3-Day Streak",
                description = "Consistent for 3 days! Building the foundation.",
                iconTxt = "🔥",
                badgeColor = Color(0xFF4C4DFF),
                isUnlocked = streakVal >= 3,
                statLabel1 = "Active Streak",
                statValue1 = "$streakVal Days",
                statLabel2 = "Status",
                statValue2 = "Consistent",
                coachMessage = "Teen din lagataar! That is how healthy habits are built. Keep this momentum firing!"
            ),
            ProgressMilestone(
                id = "streak_7",
                title = "7-Day Streak",
                description = "One full week of pure discipline. Incredible job!",
                iconTxt = "👑",
                badgeColor = Color(0xFF03A9F4),
                isUnlocked = streakVal >= 7,
                statLabel1 = "Streak",
                statValue1 = "$streakVal Days",
                statLabel2 = "Level",
                statValue2 = "Rising Star",
                coachMessage = "Pure focus for 7 days straight. You are proving to yourself that you can do this!"
            ),
            ProgressMilestone(
                id = "streak_30",
                title = "30-Day Streak",
                description = "An entire month of relentless execution. Legendary!",
                iconTxt = "🏆",
                badgeColor = Color(0xFFFFB74D),
                isUnlocked = streakVal >= 30,
                statLabel1 = "Elite Streak",
                statValue1 = "$streakVal Days",
                statLabel2 = "Discipline",
                statValue2 = "Relentless",
                coachMessage = "A month of absolute dedication! This is no longer a test—it is your permanent lifestyle."
            ),
            ProgressMilestone(
                id = "goal_weight",
                title = "Goal Weight Achieved",
                description = "Successfully reached your target metabolic weight!",
                iconTxt = "🎯",
                badgeColor = Color(0xFFFF2E93),
                isUnlocked = reachedGoalWeight,
                statLabel1 = "Current Weight",
                statValue1 = "${currentW}kg",
                statLabel2 = "Target Weight",
                statValue2 = "${targetW}kg",
                coachMessage = "Unbelievable! You set a target, put in the work, and absolutely smashed it."
            ),
            ProgressMilestone(
                id = "weight_progress",
                title = "Weight Progress",
                description = "Making measurable shifts in your physical progress.",
                iconTxt = "⚖️",
                badgeColor = Color(0xFF00E676),
                isUnlocked = weightDiff >= 1.0f,
                statLabel1 = "Progress Shift",
                statValue1 = "${weightDiffStr}kg",
                statLabel2 = "Current Weight",
                statValue2 = "${currentW}kg",
                coachMessage = "Your body is physically responding to the discipline. Real progress, day by day!"
            ),
            ProgressMilestone(
                id = "first_workout",
                title = "First Workout Logged",
                description = "Inaugurated your dynamic AI training plan!",
                iconTxt = "🏋️",
                badgeColor = Color(0xFF9C27B0),
                isUnlocked = workouts.isNotEmpty(),
                statLabel1 = "Total Workouts",
                statValue1 = "${workouts.size}",
                statLabel2 = "Status",
                statValue2 = "Inbound",
                coachMessage = "First session crushed! The sweat of today is the strength of tomorrow."
            ),
            ProgressMilestone(
                id = "workout_10",
                title = "Workout Warrior",
                description = "Completed 10 comprehensive workout sessions!",
                iconTxt = "💪",
                badgeColor = Color(0xFFE040FB),
                isUnlocked = workouts.size >= 10,
                statLabel1 = "Crushed Workouts",
                statValue1 = "${workouts.size}",
                statLabel2 = "Training Tier",
                statValue2 = "Warrior",
                coachMessage = "10 sessions in! Your body is adapting, getting stronger, and feeling powerful."
            ),
            ProgressMilestone(
                id = "workout_30",
                title = "Elite Athlete",
                description = "Successfully crushed 30 complete training workouts!",
                iconTxt = "🛡️",
                badgeColor = Color(0xFFFF5252),
                isUnlocked = workouts.size >= 30,
                statLabel1 = "Elite Sessions",
                statValue1 = "${workouts.size}",
                statLabel2 = "Athlete Tier",
                statValue2 = "Elite",
                coachMessage = "30 workouts completed! You have built a serious, high-performing physical engine."
            )
        )
    }

    // Background rendering launcher when a milestone is selected
    LaunchedEffect(selectedMilestoneForSharing) {
        val ms = selectedMilestoneForSharing
        if (ms != null) {
            isGeneratingMilestoneImage = true
            generatedMilestoneFile = null
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val file = MilestoneImageGenerator.generateMilestoneCard(
                        appContext,
                        ms.title,
                        ms.description,
                        ms.iconTxt,
                        ms.badgeColor.value.toInt(),
                        ms.statLabel1,
                        ms.statValue1,
                        ms.statLabel2,
                        ms.statValue2,
                        ms.coachMessage
                    )
                    generatedMilestoneFile = file
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    isGeneratingMilestoneImage = false
                }
            }
        }
    }

    var aiShoulders by remember { mutableStateOf("Pending") }
    var aiChest by remember { mutableStateOf("Pending") }
    var aiPosture by remember { mutableStateOf("Pending") }
    var aiOverallStatus by remember { mutableStateOf("Pending") }
    var aiScore by remember { mutableStateOf("0") }
    var aiLongText by remember { mutableStateOf("") }
    
    val measurementsPrefs = appContext.getSharedPreferences("fitgrow_measurements", android.content.Context.MODE_PRIVATE)
    
    var isEditingMeasurements by remember { mutableStateOf(false) }
    
    val bodyParts = listOf("Chest", "Waist", "Arms", "Thighs")
    
    var measurements by remember {
        mutableStateOf(
            bodyParts.map { part ->
                val b = measurementsPrefs.getFloat("before_$part", 0f)
                val c = measurementsPrefs.getFloat("current_$part", 0f)
                Triple(part, b, c)
            }
        )
    }

    LaunchedEffect(profile?.aiComparisonText) {
        val txt = profile?.aiComparisonText
        if (!txt.isNullOrEmpty() && txt.startsWith("{")) {
            try {
                val json = org.json.JSONObject(txt)
                aiShoulders = json.optString("shoulders", "Pending")
                aiChest = json.optString("chest", "Pending")
                aiPosture = json.optString("posture", "Pending")
                aiOverallStatus = json.optString("comparison", "")
                val overallVal = json.optString("overall", "0")
                aiScore = overallVal.filter { it.isDigit() }
                if (aiScore.isEmpty()) aiScore = "67"
            } catch (e: Exception) {
                aiLongText = txt
            }
        } else if (!txt.isNullOrEmpty()) {
            aiLongText = txt
        }
    }

    var currentPhotoType by remember { mutableStateOf("") }
    
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            if (currentPhotoType == "ORIGINAL") {
                viewModel.updateProfile(newOriginalPic = uri.toString())
            } else if (currentPhotoType == "CURRENT") {
                viewModel.updateProfile(newCurrentPic = uri.toString())
            }
        }
    }
    
    fun launchCamera(type: String) {
        currentPhotoType = type
        imagePickerLauncher.launch("image/*")
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0B0C10))) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.IconButton(onClick = onBack, modifier = Modifier.padding(bottom = 8.dp)) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
            }
            
            // Header
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("PROGRESS", color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                    Space(4)
                    Text("Your Transformation", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Space(4)
                    val daysDone = profile?.streak ?: 0
                    val subtitle = when {
                        daysDone < 7 -> "Every transformation starts with one photo."
                        daysDone < 30 -> "Small changes become visible through consistency."
                        daysDone < 90 -> "Your body is adapting. Keep going."
                        else -> "This is what discipline looks like."
                    }
                    Text(subtitle, color = Color(0xFFA0A0A5), fontSize = 14.sp)
                }
            }

            Space(32)

            // Section 1: BEFORE VS CURRENT Photos
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("TRANSFORMATION PHOTOS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Icon(Icons.Default.Info, contentDescription = "Info", tint = Color(0xFFA0A0A5), modifier = Modifier.size(16.dp))
            }
            Space(12)
            
            // "Tough Love" quote generated by AI (Harsh Motivation on Transformation Screen)
            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
                Text(
                    text = "\"Pain is temporary. Quitting lasts forever. What's it gonna be?\"", 
                    color = Color(0xFFA0A0A5), 
                    fontSize = 11.sp, 
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    modifier = Modifier.align(Alignment.CenterStart)
                )
            }
            
            val prefs = androidx.compose.ui.platform.LocalContext.current.getSharedPreferences("fitgrow_prefs", android.content.Context.MODE_PRIVATE)
            var weekText by remember { mutableStateOf(prefs.getString("week_count", "4") ?: "4") }

            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // BEFORE Card
                    BeforeAfterCard(
                        modifier = Modifier.weight(1f),
                        title = "BEFORE",
                        uri = profile?.originalPictureUri,
                        onClick = { launchCamera("ORIGINAL") }
                    )
                    
                    // CURRENT Card
                    BeforeAfterCard(
                        modifier = Modifier.weight(1f),
                        title = "CURRENT",
                        uri = profile?.currentPictureUri,
                        onClick = { launchCamera("CURRENT") }
                    )
                }
                
                // Glowing Arrow Overlay
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically, 
                        modifier = Modifier
                            .background(Color(0xE615161A), RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFF222328), RoundedCornerShape(8.dp))
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        androidx.compose.foundation.text.BasicTextField(
                            value = weekText,
                            onValueChange = { 
                                if (it.length <= 3 && it.all { char -> char.isDigit() }) {
                                    weekText = it 
                                    prefs.edit().putString("week_count", it).apply()
                                }
                            },
                            textStyle = androidx.compose.ui.text.TextStyle(
                                color = Color(0xFF9E82FF), 
                                fontSize = 18.sp, 
                                fontWeight = FontWeight.ExtraBold, 
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            ),
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                            cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFF9E82FF)),
                            modifier = Modifier.width(40.dp)
                        )
                        Space(4)
                        Text("WEEKS", color = Color(0xFF9E82FF), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Space(4)
                    Box(modifier = Modifier.size(48.dp), contentAlignment = Alignment.Center) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(Brush.radialGradient(listOf(Color(0xFF4C4DFF).copy(alpha=0.6f), Color.Transparent)), radius = size.width / 1.5f)
                        }
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF15161A), CircleShape)
                                .border(1.dp, Color(0xFF4C4DFF).copy(alpha=0.3f), CircleShape), 
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "To", tint = Color.White, modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
            
            if (aiLongText.isNotBlank()) {
                Space(16)
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                    Text(
                        text = aiLongText,
                        color = Color(0xFFA0A0A5),
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.fillMaxWidth(0.6f) // 60% width forces text to wrap like a Notes app
                    )
                }
            }
            
            Space(24)

            // 1. BODY MEASUREMENTS (Moved right below photos)
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("BODY MEASUREMENTS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Icon(androidx.compose.material.icons.Icons.Default.AddCircleOutline, contentDescription = "Add", tint = Color(0xFF9E82FF), modifier = Modifier.size(20.dp).clickable { isEditingMeasurements = true })
            }
            Space(16)
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                measurements.forEachIndexed { index, triple ->
                    val diff = triple.third - triple.second
                    val diffStr = if (diff > 0) "+$diff" else if (diff < 0) "$diff" else "0"
                    MeasurementRow(
                        title = triple.first,
                        before = "${triple.second}",
                        current = "${triple.third}",
                        diffStr = diffStr,
                        isDown = diff < 0,
                        isNeutral = diff == 0f
                    )
                    if (index < measurements.size - 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            if (isEditingMeasurements) {
                androidx.compose.ui.window.Dialog(onDismissRequest = { isEditingMeasurements = false }) {
                    var tempBeforeInputs by remember {
                        mutableStateOf(measurements.map { if (it.second == 0f) "" else it.second.toString() })
                    }
                    var tempCurrentInputs by remember {
                        mutableStateOf(measurements.map { if (it.third == 0f) "" else it.third.toString() })
                    }
                    
                    Box(modifier = Modifier.fillMaxWidth().height(500.dp).background(Color(0xFF15161A), RoundedCornerShape(16.dp)).border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp)).padding(16.dp)) {
                        Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                            Text("EDIT MEASUREMENTS", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(16.dp))
                            
                            measurements.forEachIndexed { i, triple ->
                                Text(triple.first, color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    androidx.compose.material3.OutlinedTextField(
                                        value = tempBeforeInputs[i],
                                        onValueChange = { newVal ->
                                            tempBeforeInputs = tempBeforeInputs.toMutableList().apply { set(i, newVal) }
                                        },
                                        label = { Text("Before") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedLabelColor = Color(0xFFA0A0A5)),
                                        modifier = Modifier.weight(1f).height(60.dp)
                                    )
                                    androidx.compose.material3.OutlinedTextField(
                                        value = tempCurrentInputs[i],
                                        onValueChange = { newVal ->
                                            tempCurrentInputs = tempCurrentInputs.toMutableList().apply { set(i, newVal) }
                                        },
                                        label = { Text("Current") },
                                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number),
                                        colors = androidx.compose.material3.OutlinedTextFieldDefaults.colors(unfocusedTextColor = Color.White, focusedTextColor = Color.White, unfocusedLabelColor = Color(0xFFA0A0A5)),
                                        modifier = Modifier.weight(1f).height(60.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            
                            androidx.compose.material3.Button(
                                onClick = {
                                    val editor = measurementsPrefs.edit()
                                    val updatedMap = measurements.mapIndexed { i, triple ->
                                        val bVal = tempBeforeInputs[i].toFloatOrNull() ?: 0f
                                        val cVal = tempCurrentInputs[i].toFloatOrNull() ?: 0f
                                        editor.putFloat("before_${triple.first}", bVal)
                                        editor.putFloat("current_${triple.first}", cVal)
                                        Triple(triple.first, bVal, cVal)
                                    }
                                    editor.apply()
                                    measurements = updatedMap
                                    isEditingMeasurements = false
                                },
                                modifier = Modifier.fillMaxWidth().height(50.dp),
                                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = Color(0xFF6B4EE6))
                            ) {
                                Text("SAVE", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Space(32)

            // 2. AI PREDICTION
            Text("AI PREDICTION", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Space(16)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Column {
                    val curW = profile?.currentWeight ?: 0f
                    Text("If you continue this pace (Target: ${profile?.targetDays ?: 30} Days)...", color = Color(0xFFA0A0A5), fontSize = 14.sp)
                    Space(16)
                    
                    val targetW = profile?.targetWeight ?: 0f
                    val totalDiff = targetW - curW
                    val days = profile?.targetDays ?: 30
                    val pDays = listOf(days / 4, days / 2, days * 3 / 4, days)
                    
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        for (i in 0 until Math.min(3, pDays.size)) {
                            val pDiff = totalDiff * (pDays[i].toFloat() / days.toFloat())
                            val pWeight = String.format("%.1f", curW + pDiff) + "kg"
                            PredictionItem("${pDays[i]} Days", pWeight)
                        }
                    }
                    Space(16)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(androidx.compose.material.icons.Icons.Default.AutoAwesome, null, tint = Color(0xFF00C853), modifier = Modifier.size(16.dp))
                        Space(8)
                        val streakVal = profile?.streak ?: 0
                        Text("Probability: ${if (streakVal > 7) "85%" else "60%"}", color = Color(0xFF00C853), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Space(32)

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("ACHIEVEMENTS", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                Text(
                    text = "View All & Share 🌍",
                    color = Color(0xFF9E82FF),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { showAllMilestonesSheet = true }
                )
            }
            Space(16)
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Link achievements to trigger milestones
                BadgeItem("1", "Day 1\nActive", Color(0xFF9E82FF), isLocked = attendance.isEmpty(), modifier = Modifier.weight(1f), onClick = {
                    val ms = milestonesList.firstOrNull { it.id == "day_1_active" }
                    if (ms != null && ms.isUnlocked) {
                        selectedMilestoneForSharing = ms
                        showMilestoneShareSheet = true
                    } else {
                        Toast.makeText(appContext, "Log at least 1 check-in to unlock! 🏁", Toast.LENGTH_SHORT).show()
                    }
                })
                BadgeItem("3", "3-Day\nStreak", Color(0xFF4C4DFF), isLocked = streakVal < 3, modifier = Modifier.weight(1f), onClick = {
                    val ms = milestonesList.firstOrNull { it.id == "streak_3" }
                    if (ms != null && ms.isUnlocked) {
                        selectedMilestoneForSharing = ms
                        showMilestoneShareSheet = true
                    } else {
                        Toast.makeText(appContext, "Reach a 3-day streak to unlock! 🔥", Toast.LENGTH_SHORT).show()
                    }
                })
                BadgeItem("7", "7-Day\nStreak", Color(0xFF03A9F4), isLocked = streakVal < 7, modifier = Modifier.weight(1f), onClick = {
                    val ms = milestonesList.firstOrNull { it.id == "streak_7" }
                    if (ms != null && ms.isUnlocked) {
                        selectedMilestoneForSharing = ms
                        showMilestoneShareSheet = true
                    } else {
                        Toast.makeText(appContext, "Reach a 7-day streak to unlock! 👑", Toast.LENGTH_SHORT).show()
                    }
                })
                BadgeItem("30", "30-Day\nStreak", Color(0xFFFFB74D), isLocked = streakVal < 30, modifier = Modifier.weight(1f), onClick = {
                    val ms = milestonesList.firstOrNull { it.id == "streak_30" }
                    if (ms != null && ms.isUnlocked) {
                        selectedMilestoneForSharing = ms
                        showMilestoneShareSheet = true
                    } else {
                        Toast.makeText(appContext, "Reach a 30-day streak to unlock! 🏆", Toast.LENGTH_SHORT).show()
                    }
                })
                BadgeItem("📸", "First\nPhoto", Color(0xFF00C853), isLocked = !hasPhoto, modifier = Modifier.weight(1f), onClick = {
                    if (hasPhoto) {
                        // Quick customized photo milestone trigger
                        val ms = milestonesList.firstOrNull { it.id == "weight_progress" } ?: milestonesList.first()
                        selectedMilestoneForSharing = ms
                        showMilestoneShareSheet = true
                    } else {
                        Toast.makeText(appContext, "Upload your first transformation photo to unlock! 📸", Toast.LENGTH_SHORT).show()
                    }
                })
            }

            Space(32)

            // 4. AI SLOGAN / MOTIVATION (At the overall bottom)
            Text("AI MOTIVATION", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Space(16)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF1E1E40), Color(0xFF15161A))), RoundedCornerShape(16.dp))
                    .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFF6B4EE6).copy(alpha = 0.2f), CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF9E82FF))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        val sloganTitle = if (hasPhoto) "Visual changes are loading..." else "Consistency today, transformation tomorrow."
                        val sloganDesc = if (hasPhoto) "You've successfully uploaded progress photos! Let's hit the next milestone." else "You've got this! Keep pushing forward."
                        Text(sloganTitle, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        Space(4)
                        Text(sloganDesc, color = Color(0xFFA0A0A5), fontSize = 12.sp)
                    }
                }
            }

            Space(120) // Bottom nav padding
        }
    }

    // --- ALL MILESTONES SHEET ---
    if (showAllMilestonesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAllMilestonesSheet = false },
            containerColor = Color(0xFF0F1015),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF222328)) }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 48.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Milestones & Achievements", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showAllMilestonesSheet = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                    }
                }
                Text("Your fitness accomplishments ready to be shared with friends & social platforms.", color = Color(0xFFA0A0A5), fontSize = 13.sp)
                Space(16)

                androidx.compose.foundation.lazy.LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    items(count = milestonesList.size) { index ->
                        val milestone = milestonesList[index]
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF15161A)),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (milestone.isUnlocked) Color(0xFF222328) else Color(0xFF1B1B22)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .background(if (milestone.isUnlocked) milestone.badgeColor.copy(alpha = 0.15f) else Color(0xFF1C1D24), CircleShape)
                                        .border(1.5.dp, if (milestone.isUnlocked) milestone.badgeColor else Color(0xFF2B2D38), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (milestone.isUnlocked) {
                                        Text(milestone.iconTxt, fontSize = 18.sp)
                                    } else {
                                        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = Color(0xFFA0A0A5), modifier = Modifier.size(14.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = milestone.title,
                                        color = if (milestone.isUnlocked) Color.White else Color(0xFFA0A0A5),
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(1.dp))
                                    Text(
                                        text = milestone.description,
                                        color = Color(0xFFA0A0A5),
                                        fontSize = 11.sp,
                                        lineHeight = 14.sp
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                if (milestone.isUnlocked) {
                                    Button(
                                        onClick = {
                                            selectedMilestoneForSharing = milestone
                                            showMilestoneShareSheet = true
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = milestone.badgeColor),
                                        shape = RoundedCornerShape(8.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        modifier = Modifier.height(26.dp)
                                    ) {
                                        Text("Share", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                } else {
                                    Text("Locked", color = Color(0xFF55565A), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- SHARE MILESTONE PREVIEW SHEET ---
    if (showMilestoneShareSheet) {
        ModalBottomSheet(
            onDismissRequest = { showMilestoneShareSheet = false },
            containerColor = Color(0xFF0F1015),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Color(0xFF222328)) }
        ) {
            val ms = selectedMilestoneForSharing
            if (ms != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Share Your Milestone 🌟", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                        IconButton(onClick = { showMilestoneShareSheet = false }) {
                            Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                        }
                    }
                    Space(20)

                    // Card Preview
                    Box(
                        modifier = Modifier
                            .size(280.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(1.dp, Color(0xFF222328), RoundedCornerShape(24.dp))
                            .background(Color(0xFF0B0C10)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isGeneratingMilestoneImage || generatedMilestoneFile == null) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = ms.badgeColor)
                                Space(12)
                                Text("Crafting beautiful card...", color = Color(0xFFA0A0A5), fontSize = 12.sp)
                            }
                        } else {
                            AsyncImage(
                                model = generatedMilestoneFile,
                                contentDescription = "Milestone card preview",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Space(24)

                    // Caption copying section
                    val captionText = "Just achieved a major milestone on FitGrow! ⚡\n" +
                        "🏆 ${ms.title}: ${ms.description}\n" +
                        "📊 ${ms.statLabel1}: ${ms.statValue1} | ${ms.statLabel2}: ${ms.statValue2}\n" +
                        "🔥 Join me on the grind! #FitGrow #Consistency"

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15161A)),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222328)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Suggested Caption", color = Color(0xFFA0A0A5), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    text = "COPY CAPTION 📋",
                                    color = Color(0xFF9E82FF),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    modifier = Modifier.clickable {
                                        val clipboard = appContext.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clip = android.content.ClipData.newPlainText("FitGrow Caption", captionText)
                                        clipboard.setPrimaryClip(clip)
                                        Toast.makeText(appContext, "Caption copied to clipboard! 📋", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                            Space(6)
                            Text(
                                text = captionText,
                                color = Color.White,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }

                    Space(24)

                    // Share actions
                    Button(
                        onClick = {
                            val file = generatedMilestoneFile
                            if (file != null && file.exists()) {
                                try {
                                    val uri = FileProvider.getUriForFile(appContext, "com.example.fileprovider", file)
                                    val intent = Intent(Intent.ACTION_SEND).apply {
                                        type = "image/png"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_TEXT, captionText)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    appContext.startActivity(Intent.createChooser(intent, "Share Milestone to..."))
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                    Toast.makeText(appContext, "Error launching sharing: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                Toast.makeText(appContext, "Card is rendering. Please wait!", Toast.LENGTH_SHORT).show()
                            }
                        },
                        enabled = !isGeneratingMilestoneImage && generatedMilestoneFile != null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ms.badgeColor)
                    ) {
                        Text("Share to Social Media 📱", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }

                    Space(12)

                    OutlinedButton(
                        onClick = {
                            val file = generatedMilestoneFile
                            if (file != null && file.exists()) {
                                // Inform user they can also save or copy
                                Toast.makeText(appContext, "Milestone image generated and ready to share! 💾", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222328))
                    ) {
                        Text("Download Image 💾", fontSize = 14.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun Space(size: Int) {
    Spacer(modifier = Modifier.height(size.dp))
}

@Composable
fun SpaceW(size: Int) {
    Spacer(modifier = Modifier.width(size.dp))
}

@Composable
fun BeforeAfterCard(modifier: Modifier = Modifier, title: String, uri: String?, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .height(280.dp)
            .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        if (uri != null) {
            val mappedUri = if (uri.startsWith("/")) "file://$uri" else uri
            AsyncImage(model = mappedUri, contentDescription = title, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            // Placeholder Human Outline effect using Canvas & DrawBehind
            Box(
                modifier = Modifier.fillMaxSize().drawBehind {
                    // Simple simulated background glow
                    drawRect(Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF0D0D14))))
                },
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.AddAPhoto, contentDescription = "Add", tint = Color(0xFF4C4DFF), modifier = Modifier.size(32.dp))
            }
        }
        
        // Label
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 16.dp)
                .background(Color(0x80000000), RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Text(title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
        }
        
        // Update Icon always visible at top right
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp)
                .background(Color(0x80000000), CircleShape)
                .padding(8.dp)
        ) {
            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.White, modifier = Modifier.size(14.dp))
        }
        
        if (uri == null) {
            Text("Upload Photo", color = Color(0xFFA0A0A5), fontSize = 12.sp, modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 32.dp))
        }
    }
}

@Composable
fun AnalysisItem(title: String, status: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF15161A), RoundedCornerShape(12.dp))
            .border(1.dp, Color(0xFF222328), RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Medium)
        Text(status, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun OverviewMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, desc: String, color: Color) {
    Column(
        modifier = Modifier
            .width(100.dp)
            .background(Color(0xFF15161A), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF222328), RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Box(modifier = Modifier.size(24.dp).background(color.copy(alpha=0.15f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
        }
        Space(12)
        Text(value, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Space(4)
        Text(desc, color = Color(0xFFA0A0A5), fontSize = 10.sp)
        Space(12)
        // Mini graph
        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxWidth().height(16.dp)) {
            val path = Path()
            val stepX = size.width / 4
            var curX = 0f
            path.moveTo(curX, size.height * Random.nextFloat())
            for (i in 1..4) {
                curX += stepX
                path.lineTo(curX, size.height * Random.nextFloat())
            }
            drawPath(path, color, style = Stroke(width = 3f, cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
        }
    }
}

@Composable
fun BadgeItem(iconTxt: String, title: String, color: Color, isLocked: Boolean = false, modifier: Modifier = Modifier, onClick: () -> Unit = {}) {
    Column(modifier = modifier.clickable { onClick() }, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .background(if (isLocked) Color(0xFF15161A) else color.copy(alpha = 0.15f), CircleShape)
                .border(1.5.dp, if (isLocked) Color(0xFF222328) else color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isLocked) {
                Icon(Icons.Default.Lock, null, tint = Color(0xFFA0A0A5), modifier = Modifier.size(16.dp))
            } else {
                Text(iconTxt, color = color, fontSize = 18.sp, fontWeight = FontWeight.ExtraBold)
            }
        }
        Space(6)
        Text(title, color = if (isLocked) Color(0xFFA0A0A5) else Color.White, fontSize = 10.sp, textAlign = TextAlign.Center, lineHeight = 13.sp)
    }
}

@Composable
fun PredictionItem(time: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(time, color = Color(0xFFA0A0A5), fontSize = 12.sp)
        Space(4)
        Text(value, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun MeasurementRow(title: String, before: String, current: String, diffStr: String, isDown: Boolean = false, isNeutral: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = Color(0xFFA0A0A5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        
        Row(modifier = Modifier.weight(2f), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
            Text(before, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            
            SpaceW(8)
            val diffColor = if (isNeutral) Color(0xFFA0A0A5) else if (isDown) Color(0xFF00C853) else Color(0xFF6B4EE6)
            Text(diffStr, color = diffColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            SpaceW(8)
            
            Text(current, color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

data class ProgressMilestone(
    val id: String,
    val title: String,
    val description: String,
    val iconTxt: String,
    val badgeColor: Color,
    val isUnlocked: Boolean,
    val statLabel1: String,
    val statValue1: String,
    val statLabel2: String,
    val statValue2: String,
    val coachMessage: String
)

