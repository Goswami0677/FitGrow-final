package com.example.ui.screens.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.MainViewModel
import com.example.ui.theme.Zinc100
import com.example.ui.theme.Zinc400
import com.example.ui.theme.Zinc800
import com.example.ui.theme.Zinc900
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.Image
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import android.content.Intent
import androidx.core.content.FileProvider
import android.os.Environment


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onEraseAllData: () -> Unit
) {
    val profile = viewModel.profile.collectAsState().value
    var name by remember(profile) { mutableStateOf(profile?.name ?: "") }
    var weightString by remember(profile) { mutableStateOf(profile?.currentWeight?.toString() ?: "") }
    var showEraseConfirmDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val attendance = viewModel.attendance.collectAsState().value
    val workouts = viewModel.workouts.collectAsState().value
    val nutritionLogs = viewModel.nutritionLogs.collectAsState().value

    var isGeneratingReport by remember { mutableStateOf(false) }
    var currentReportFile by remember { mutableStateOf<File?>(null) }
    var reportPreviewBitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    var selectedPeriodDays by remember { mutableStateOf<Int?>(null) }
    var showReportBottomSheet by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("Analyzing workouts...") }

    LaunchedEffect(isGeneratingReport) {
        if (isGeneratingReport) {
            val sequences = listOf(
                "Analyzing workouts...",
                "Calculating nutrition...",
                "Building charts...",
                "Finalizing your report..."
            )
            var i = 0
            while (isGeneratingReport) {
                loadingText = sequences[i % sequences.size]
                kotlinx.coroutines.delay(800)
                i++
            }
        }
    }
    
    if (showEraseConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showEraseConfirmDialog = false },
            title = {
                Text(
                    text = "⚠️ Erase All Data?",
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete all your data? This will permanently erase your profile, daily logs, workout history, and custom goals. This action cannot be undone.",
                    color = Zinc400,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showEraseConfirmDialog = false
                        viewModel.eraseAllData {
                            onEraseAllData()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                ) {
                    Text("ERASE EVERYTHING", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEraseConfirmDialog = false }
                ) {
                    Text("CANCEL", color = Zinc100)
                }
            },
            containerColor = Zinc900,
            textContentColor = Zinc100
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile & Settings", color = Zinc100) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Zinc100)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val imagePickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
                androidx.activity.result.contract.ActivityResultContracts.GetContent()
            ) { uri ->
                if (uri != null) {
                    viewModel.updateProfile(newCurrentPic = uri.toString())
                }
            }

            // Profile Edit
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .background(Zinc800, CircleShape)
                    .align(Alignment.CenterHorizontally)
                    .clickable { imagePickerLauncher.launch("image/*") }
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                if (profile?.currentPictureUri != null) {
                    val mappedUri = if (profile.currentPictureUri.startsWith("/")) "file://${profile.currentPictureUri}" else profile.currentPictureUri
                    coil.compose.AsyncImage(model = mappedUri, contentDescription = "Profile", contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                } else {
                    val initial = name.firstOrNull()?.uppercase() ?: "U"
                    Text(initial, color = Zinc100, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text("Tap to change picture", color = Zinc400, fontSize = 12.sp, modifier = Modifier.align(Alignment.CenterHorizontally))
            
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name", color = Zinc400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4C4DFF),
                    unfocusedBorderColor = Zinc800,
                    focusedTextColor = Zinc100,
                    unfocusedTextColor = Zinc100
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = weightString,
                onValueChange = { weightString = it },
                label = { Text("Current Weight (kg)", color = Zinc400) },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF4C4DFF),
                    unfocusedBorderColor = Zinc800,
                    focusedTextColor = Zinc100,
                    unfocusedTextColor = Zinc100
                ),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = {
                    profile?.let {
                        viewModel.updateProfileName(name)
                        try {
                            val w = weightString.toFloat()
                            viewModel.updateProfile(newCurrentWeight = w)
                        } catch (e: Exception) {}
                    }
                },
                modifier = Modifier.fillMaxWidth().height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4C4DFF))
            ) {
                Text("SAVE PROFILE", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Google Sync Account Card
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Zinc900.copy(alpha = 0.5f)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Zinc800),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFF4285F4), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("G", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Connected Gmail", color = Zinc400, fontSize = 11.sp)
                        Text(profile?.email ?: "Not connected", color = Zinc100, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        
                        val syncState = viewModel.syncingState.collectAsState().value
                        val syncStatusText = when (syncState) {
                            "syncing" -> "Syncing training logs..."
                            "success" -> "All workouts synced to Google Cloud"
                            "error" -> "Cloud sync offline"
                            else -> "Cloud backup is active"
                        }
                        val syncStatusColor = when (syncState) {
                            "syncing" -> Color(0xFFFFA000)
                            "success" -> Color(0xFF00C853)
                            "error" -> Color(0xFFD32F2F)
                            else -> Color(0xFF00C853)
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .background(syncStatusColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(syncStatusText, color = syncStatusColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    IconButton(
                        onClick = {
                            viewModel.syncToCloud()
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudSync,
                            contentDescription = "Sync Now",
                            tint = Color(0xFF4C4DFF)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(48.dp))
            Text("SETTINGS & TIMINGS", color = Zinc400, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))
            
            var showTimePickerFor by remember { mutableStateOf<String?>(null) }
            
            if (showTimePickerFor != null) {
                val currentStr = when (showTimePickerFor) {
                    "Gym" -> profile?.gymTime ?: "18:00"
                    "Breakfast" -> profile?.breakfastTime ?: "08:00"
                    "Lunch" -> profile?.lunchTime ?: "13:00"
                    "Dinner" -> profile?.dinnerTime ?: "20:00"
                    else -> "12:00"
                }
                val parts = currentStr.split(":")
                val curHour = parts.getOrNull(0)?.toIntOrNull() ?: 12
                val curMin = parts.getOrNull(1)?.toIntOrNull() ?: 0

                com.example.ui.screens.onboarding.IosTimePickerDialog(
                    title = "Select $showTimePickerFor Time",
                    initialHour = curHour,
                    initialMinute = curMin,
                    onDismiss = { showTimePickerFor = null },
                    onConfirm = { h, m ->
                        val newVal = "${h.toString().padStart(2, '0')}:${m.toString().padStart(2, '0')}"
                        when (showTimePickerFor) {
                            "Gym" -> viewModel.updateTiming("gym", newVal)
                            "Breakfast" -> viewModel.updateTiming("breakfast", newVal)
                            "Lunch" -> viewModel.updateTiming("lunch", newVal)
                            "Dinner" -> viewModel.updateTiming("dinner", newVal)
                        }
                        showTimePickerFor = null
                    }
                )
            }
            
            com.example.ui.screens.onboarding.TimeRow(label = "🥗 Breakfast", time24 = profile?.breakfastTime ?: "08:00") { showTimePickerFor = "Breakfast" }
            com.example.ui.screens.onboarding.TimeRow(label = "🍛 Lunch", time24 = profile?.lunchTime ?: "13:00") { showTimePickerFor = "Lunch" }
            com.example.ui.screens.onboarding.TimeRow(label = "🍲 Dinner", time24 = profile?.dinnerTime ?: "20:00") { showTimePickerFor = "Dinner" }
            com.example.ui.screens.onboarding.TimeRow(label = "🏋️ Gym Time", time24 = profile?.gymTime ?: "18:00") { showTimePickerFor = "Gym" }

            Spacer(modifier = Modifier.height(40.dp))
            Text("PROGRESS REPORT", color = Zinc400, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(16.dp))

            val loggedDays = attendance.size
            val todayVal = Calendar.getInstance()
            val displayFormat = SimpleDateFormat("dd MMM", Locale.getDefault())
            val keyFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

            val cal7 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -6) }
            val range7Str = "${displayFormat.format(cal7.time)} – ${displayFormat.format(todayVal.time)}"
            val done7 = attendance.filter { it.dateStr in keyFormat.format(cal7.time)..keyFormat.format(todayVal.time) && it.isDone }.size
            val rate7 = if (done7 > 0) ((done7.toFloat() / 7f) * 100).toInt() else 0

            val cal14 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -13) }
            val range14Str = "${displayFormat.format(cal14.time)} – ${displayFormat.format(todayVal.time)}"
            val done14 = attendance.filter { it.dateStr in keyFormat.format(cal14.time)..keyFormat.format(todayVal.time) && it.isDone }.size
            val rate14 = if (done14 > 0) ((done14.toFloat() / 14f) * 100).toInt() else 0

            val cal30 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -29) }
            val range30Str = "${displayFormat.format(cal30.time)} – ${displayFormat.format(todayVal.time)}"
            val done30 = attendance.filter { it.dateStr in keyFormat.format(cal30.time)..keyFormat.format(todayVal.time) && it.isDone }.size
            val rate30 = if (done30 > 0) ((done30.toFloat() / 30f) * 100).toInt() else 0

            val cal90 = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -89) }
            val range90Str = "${displayFormat.format(cal90.time)} – ${displayFormat.format(todayVal.time)}"
            val done90 = attendance.filter { it.dateStr in keyFormat.format(cal90.time)..keyFormat.format(todayVal.time) && it.isDone }.size
            val rate90 = if (done90 > 0) ((done90.toFloat() / 90f) * 100).toInt() else 0

            val doneAll = attendance.filter { it.isDone }.size

            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ReportOptionRow(
                    title = "📊  Last 7 Days",
                    range = range7Str,
                    detail = "$done7/7 workouts · $rate7% complete",
                    isSelected = selectedPeriodDays == 7,
                    onSelect = { selectedPeriodDays = 7 }
                )

                ReportOptionRow(
                    title = "📈  Last 14 Days",
                    range = range14Str,
                    detail = "$done14/14 workouts · $rate14% complete",
                    isSelected = selectedPeriodDays == 14,
                    onSelect = { selectedPeriodDays = 14 }
                )

                ReportOptionRow(
                    title = "🗓️  Last 30 Days",
                    range = range30Str,
                    detail = "$done30/30 workouts · $rate30% complete",
                    isSelected = selectedPeriodDays == 30,
                    onSelect = { selectedPeriodDays = 30 }
                )

                ReportOptionRow(
                    title = "🏆  Last 3 Months",
                    range = range90Str,
                    detail = "$done90/90 workouts · $rate90% complete",
                    isSelected = selectedPeriodDays == 90,
                    onSelect = { selectedPeriodDays = 90 }
                )

                ReportOptionRow(
                    title = "📁  Complete History",
                    range = "Day 1 – Today",
                    detail = "$doneAll total workouts logged",
                    isSelected = selectedPeriodDays == -1,
                    onSelect = { selectedPeriodDays = -1 }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Button(
                onClick = {
                    selectedPeriodDays?.let { days ->
                        isGeneratingReport = true
                        coroutineScope.launch {
                            try {
                                val file = ReportGenerator.generatePdfReport(
                                    context = context,
                                    profile = profile,
                                    attendance = attendance,
                                    workouts = workouts,
                                    nutritionLogs = nutritionLogs,
                                    days = days
                                )
                                currentReportFile = file

                                try {
                                    val fileDescriptor = android.os.ParcelFileDescriptor.open(file, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                                    val renderer = android.graphics.pdf.PdfRenderer(fileDescriptor)
                                    if (renderer.pageCount > 0) {
                                        val page = renderer.openPage(0)
                                        val bmp = android.graphics.Bitmap.createBitmap(page.width / 4, page.height / 4, android.graphics.Bitmap.Config.ARGB_8888)
                                        page.render(bmp, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                        reportPreviewBitmap = bmp
                                        page.close()
                                    }
                                    renderer.close()
                                    fileDescriptor.close()
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }

                                showReportBottomSheet = true
                            } catch (e: Exception) {
                                e.printStackTrace()
                            } finally {
                                isGeneratingReport = false
                            }
                        }
                    }
                },
                enabled = selectedPeriodDays != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Unspecified,
                    disabledContainerColor = Zinc800.copy(alpha = 0.5f)
                ),
                contentPadding = PaddingValues()
            ) {
                val isEnabled = selectedPeriodDays != null
                val brush = if (isEnabled) {
                    Brush.horizontalGradient(colors = listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))
                } else {
                    Brush.horizontalGradient(colors = listOf(Zinc800, Zinc800))
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(brush)
                        .padding(horizontal = 16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Generate Report 📄",
                        color = if (isEnabled) Color.White else Zinc400,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
            HorizontalDivider(color = Zinc800, thickness = 1.dp)
            Spacer(modifier = Modifier.height(24.dp))
            
            Text("DANGER ZONE", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(12.dp))
            
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0x1AEF4444)),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Delete All App Data",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "This will permanently delete all your personal information, progress photos, weight charts, streaks, and chat history.",
                        color = Zinc400,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { showEraseConfirmDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth().height(44.dp)
                    ) {
                        Text("ERASE ALL DATA", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (isGeneratingReport) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = {}
        ) {
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .background(Zinc900.copy(alpha = 0.95f), RoundedCornerShape(24.dp))
                    .border(2.dp, Color(0xFF4C4DFF), RoundedCornerShape(24.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFF4C4DFF),
                        strokeWidth = 4.dp,
                        modifier = Modifier.size(56.dp)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = loadingText,
                        color = Zinc100,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            }
        }
    }

    if (showReportBottomSheet && currentReportFile != null) {
        val file = currentReportFile!!
        ModalBottomSheet(
            onDismissRequest = { showReportBottomSheet = false },
            containerColor = Color(0xFF0F1015),
            dragHandle = { BottomSheetDefaults.DragHandle(color = Zinc800) }
        ) {
            val durationLabel = when (selectedPeriodDays) {
                7 -> "Last 7 Days"
                14 -> "Last 14 Days"
                30 -> "Last 30 Days"
                90 -> "Last 3 Months"
                else -> "Complete History"
            }
            
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 36.dp)
            ) {
                Text(
                    text = "Your Report is Ready 📄",
                    color = Zinc100,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$durationLabel • 5 pages",
                    color = Zinc400,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Zinc900)
                        .border(1.dp, Zinc800, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (reportPreviewBitmap != null) {
                        Image(
                            bitmap = reportPreviewBitmap!!.asImageBitmap(),
                            contentDescription = "Report Cover Page Preview",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = androidx.compose.material.icons.Icons.Default.Info,
                                contentDescription = null,
                                tint = Zinc400,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("PDF Report", color = Zinc400, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
                
                Button(
                    onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
                            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                                setDataAndType(uri, "application/pdf")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(viewIntent, "View Report"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(context, "Error opening PDF: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Unspecified),
                    contentPadding = PaddingValues()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Brush.horizontalGradient(colors = listOf(Color(0xFF7C3AED), Color(0xFFEC4899)))),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("📄 View Report", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedButton(
                    onClick = {
                        try {
                            val downloadDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "FitGrow")
                            if (!downloadDir.exists()) {
                                downloadDir.mkdirs()
                            }
                            val cleanName = (profile?.name ?: "User").replace("\\s+".toRegex(), "_")
                            val durationLabelClean = when (selectedPeriodDays) {
                                7 -> "7_Days"
                                14 -> "14_Days"
                                30 -> "30_Days"
                                90 -> "3_Months"
                                else -> "Complete_History"
                            }
                            val dateStamp = SimpleDateFormat("ddMMMyyyy", Locale.getDefault()).format(Date())
                            val targetFile = File(downloadDir, "FitGrow_${cleanName}_${durationLabelClean}_${dateStamp}.pdf")
                            
                            file.inputStream().use { input ->
                                targetFile.outputStream().use { output ->
                                    input.copyTo(output)
                                }
                            }
                            android.widget.Toast.makeText(context, "Saved to Downloads/FitGrow/ ✓", android.widget.Toast.LENGTH_LONG).show()
                        } catch (e: Exception) {
                            e.printStackTrace()
                            android.widget.Toast.makeText(context, "Failed to save file: ${e.localizedMessage}", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF4C4DFF)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF4C4DFF))
                ) {
                    Text("⬇️ Download to Device", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                OutlinedButton(
                    onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
                            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_SUBJECT, "My FitGrow Progress Report")
                                putExtra(Intent.EXTRA_TEXT, "Here is my FitGrow fitness progress report...")
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(emailIntent, "Email Report"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Zinc800),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Zinc100)
                ) {
                    Text("📧 Email Report", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(10.dp))
                
                TextButton(
                    onClick = {
                        try {
                            val uri = FileProvider.getUriForFile(context, "com.example.fileprovider", file)
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Report"))
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                ) {
                    Text("🔗 Share", color = Zinc400, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
                Spacer(modifier = Modifier.height(8.dp))
                
                TextButton(
                    onClick = { showReportBottomSheet = false },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cancel", color = Color.Red, fontWeight = FontWeight.Medium, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
fun ReportOptionRow(
    title: String,
    range: String,
    detail: String,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0x1A4C4DFF) else Zinc900
        ),
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            2.dp,
            if (isSelected) Color(0xFF4C4DFF) else Zinc800
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
    ) {
        Row(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(4.dp, 60.dp)
                    .background(Color(0xFF4C4DFF), RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, color = Zinc100, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(range, color = Zinc400, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(2.dp))
                Text(detail, color = Color(0xFF4C4DFF), fontSize = 13.sp, fontWeight = FontWeight.Medium)
            }
        }
    }
}

