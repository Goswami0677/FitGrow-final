package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProfileInfo
import com.example.data.local.ProgressPhoto
import com.example.data.local.WorkoutLog
import com.example.data.local.ChatMessage
import com.example.data.local.NutritionLog
import com.example.data.local.DailyWater
import com.example.data.repository.AppRepository
import com.example.api.RetrofitClient
import com.example.api.GenerateContentRequest
import com.example.api.Content
import com.example.api.Part
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody

import android.net.Uri
import android.provider.MediaStore
import android.util.Base64
import java.io.ByteArrayOutputStream

class MainViewModel(private val application: android.app.Application, private val repository: AppRepository) : ViewModel() {
    val profile = repository.profile.stateIn(viewModelScope, SharingStarted.Lazily, null)
    val workouts = repository.workouts.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val chatMessages = repository.chatMessages.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val attendance = repository.attendance.stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
    val nutritionLogs = repository.getAllNutritionLogs().stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    private val _journeyStartTime = MutableStateFlow(-1L)
    val journeyStartTime = _journeyStartTime.asStateFlow()

    private val _weightHistory = MutableStateFlow<List<Pair<String, Float>>>(emptyList())
    val weightHistory = _weightHistory.asStateFlow()

    private val weightHistoryPrefs = application.getSharedPreferences("fitgrow_weight_history", android.content.Context.MODE_PRIVATE)

    fun loadWeightHistory() {
        val rawStr = weightHistoryPrefs.getString("weight_logs", "") ?: ""
        val list = if (rawStr.isNotEmpty()) {
            rawStr.split(",").mapNotNull {
                val parts = it.split(":")
                if (parts.size == 2) {
                    val date = parts[0]
                    val wt = parts[1].toFloatOrNull()
                    if (wt != null) Pair(date, wt) else null
                } else null
            }
        } else emptyList()
        _weightHistory.value = list
    }

    fun recordWeightHistoryLog(dateStr: String, weight: Float) {
        val rawStr = weightHistoryPrefs.getString("weight_logs", "") ?: ""
        val logsList = if (rawStr.isNotEmpty()) rawStr.split(",").toMutableList() else mutableListOf()
        
        // Remove existing entry for the same date
        logsList.removeAll { it.startsWith("$dateStr:") }
        logsList.add("$dateStr:$weight")
        
        // Sort by date Str
        logsList.sortBy { it.substringBefore(":") }
        
        val trimmed = if (logsList.size > 14) logsList.takeLast(14) else logsList
        weightHistoryPrefs.edit().putString("weight_logs", trimmed.joinToString(",")).apply()
        loadWeightHistory()
    }

    private val workoutPrefs = application.getSharedPreferences("fitgrow_ai_workout", android.content.Context.MODE_PRIVATE)

    private val _aiWorkoutSuggestions = MutableStateFlow<String?>(null)
    val aiWorkoutSuggestions = _aiWorkoutSuggestions.asStateFlow()

    private val _isAiWorkoutGenerating = MutableStateFlow(false)
    val isAiWorkoutGenerating = _isAiWorkoutGenerating.asStateFlow()

    private val _hasUnreadCoachMessage = MutableStateFlow(false)
    val hasUnreadCoachMessage = _hasUnreadCoachMessage.asStateFlow()

    private val _daysLeft = MutableStateFlow(30)
    val daysLeft = _daysLeft.asStateFlow()

    private val _successProbability = MutableStateFlow(85)
    val successProbability = _successProbability.asStateFlow()

    private val _successTrend = MutableStateFlow("Improving") // "Improving" or "Downfall"
    val successTrend = _successTrend.asStateFlow()

    init {
        loadWeightHistory()
        _aiWorkoutSuggestions.value = workoutPrefs.getString("suggestions", null)
        
        // 15-Day Dynamic Check-in Logic
        val installTime = workoutPrefs.getLong("install_time", -1L)
        val todayMs = System.currentTimeMillis()
        if (installTime == -1L) {
            workoutPrefs.edit().putLong("install_time", todayMs).apply()
            _hasUnreadCoachMessage.value = true
        } else {
            val daysSince = (todayMs - installTime) / (1000 * 60 * 60 * 24)
            if (daysSince <= 15) {
                _hasUnreadCoachMessage.value = true
            }
        }

        // Trigger dynamic accountability & macro check-ins whenever profile, workouts, or nutrition changes
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(profile, workouts, nutritionLogs) { p, _, _ -> p }
                .collect { p ->
                    if (p != null) {
                        performDailyAccountabilityCheck(p)
                    }
                }
        }

        // Automatic Cloud Sync Reactive Collector (auto backup when logged in and active)
        viewModelScope.launch {
            kotlinx.coroutines.flow.combine(profile, workouts, nutritionLogs, attendance, chatMessages) { _, _, _, _, _ ->
                System.currentTimeMillis()
            }.debounce(4000)
            .collect {
                syncToCloud()
            }
        }
    }

    fun clearAiWorkoutSuggestions() {
        _aiWorkoutSuggestions.value = null
        workoutPrefs.edit().remove("suggestions").apply()
    }

    fun markCoachMessagesRead() {
        _hasUnreadCoachMessage.value = false
    }

    fun generateAiWorkoutSuggestions(onComplete: (Boolean) -> Unit = {}) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _isAiWorkoutGenerating.value = true
            try {
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY
                if (apiKey.isNullOrBlank()) {
                    _aiWorkoutSuggestions.value = "AI API Key is missing. Please set it in the AI Studio Secrets panel!"
                    _isAiWorkoutGenerating.value = false
                    onComplete(false)
                    return@launch
                }

                val userProfile = profile.value
                val recentWorkouts = workouts.value.take(15)
                val workoutHistoryText = if (recentWorkouts.isEmpty()) {
                    "No workouts logged yet. Starting fresh!"
                } else {
                    recentWorkouts.joinToString("\n") { "- ${it.workoutName} on ${it.dateStr}" }
                }

                val profileDetails = """
                    - Age: ${userProfile?.age ?: "N/A"}
                    - Height: ${userProfile?.height ?: "N/A"} cm
                    - Current Weight: ${userProfile?.currentWeight ?: "N/A"} kg
                    - Target Weight: ${userProfile?.targetWeight ?: "N/A"} kg
                    - Body Type/Goal: ${userProfile?.bodyType ?: "General Fitness"}
                    - Target Calories: ${userProfile?.targetCalories ?: 2000} kcal
                    - Target Protein: ${userProfile?.targetProtein ?: 120} g
                """.trimIndent()

                val sysPrompt = """
                    You are FitGrow's friendly, supportive, humanoid AI Coach and Elite Personal Trainer.
                    Analyze the following user profile and recent workout logs to give them their next immediate training instructions.
                    
                    Keep your response extremely concise, supportive, and direct (kaam ki baat zyada, lamba lecture bilkul nahi taaki user bore na ho).
                    
                    === USER PROFILE ===
                    $profileDetails
                    
                    === RECENT WORKOUT HISTORY ===
                    $workoutHistoryText
                    
                    Format your response in simple, beautiful, highly compact Markdown exactly like this:
                    1. **Summary & Appreciations (1-2 lines)**: Warm humanoid Hinglish summary of what they logged/did, and direct encouragement.
                    2. **Next Steps (3-4 bullet points)**: Exactly what they need to do next (such as 3 key exercises, reps/sets, and 1 quick safety form cue). Keep it simple, clear, and direct!
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = sysPrompt))))
                )
                
                val response = RetrofitClient.service.generateContentFlash(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                
                if (!text.isNullOrBlank()) {
                    _aiWorkoutSuggestions.value = text
                    workoutPrefs.edit().putString("suggestions", text).apply()
                    _isAiWorkoutGenerating.value = false
                    onComplete(true)
                } else {
                    _aiWorkoutSuggestions.value = "Unable to get guidance from AI. Please try again."
                    _isAiWorkoutGenerating.value = false
                    onComplete(false)
                }
            } catch (e: Exception) {
                _aiWorkoutSuggestions.value = "Error connecting to AI Coach. ${e.localizedMessage ?: "Please try again."}"
                _isAiWorkoutGenerating.value = false
                onComplete(false)
            }
        }
    }

    fun markAttendance(dateStr: String, isDone: Boolean) {
        viewModelScope.launch {
            repository.markAttendance(dateStr, isDone)
        }
    }

    private val _isUserAuthenticated = MutableStateFlow(true)
    val isUserAuthenticated = _isUserAuthenticated.asStateFlow()

    private val _isDarkMode = MutableStateFlow(true)
    val isDarkMode = _isDarkMode.asStateFlow()

    private val _themeColorIndex = MutableStateFlow(0)
    val themeColorIndex = _themeColorIndex.asStateFlow()

    private val _shouldScrollToNutritionTrends = MutableStateFlow(false)
    val shouldScrollToNutritionTrends = _shouldScrollToNutritionTrends.asStateFlow()

    fun setScrollToNutritionTrends(scroll: Boolean) {
        _shouldScrollToNutritionTrends.value = scroll
    }

    fun setDarkMode(isDark: Boolean) {
        _isDarkMode.value = isDark
    }

    fun setThemeColorIndex(index: Int) {
        _themeColorIndex.value = index
    }

    fun updateProfileName(name: String) {
        viewModelScope.launch {
            val current = profile.value
            if (current != null) {
                repository.saveProfile(current.copy(name = name))
            }
        }
    }

    fun updateAuthState() {
        _isUserAuthenticated.value = true
    }

    private val _syncingState = MutableStateFlow<String?>(null) // null = idle, "syncing", "success", "error"
    val syncingState = _syncingState.asStateFlow()

    fun syncToCloud() {
        val email = profile.value?.email
        if (email.isNullOrBlank()) return
        
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            _syncingState.value = "syncing"
            try {
                val sanitizedEmail = email.lowercase().filter { it.isLetterOrDigit() }
                
                // Fetch all data
                val currentProfile = repository.getProfileDirectly() ?: ProfileInfo(email = email)
                val workoutsList = repository.getWorkoutsDirectly()
                val nutritionList = repository.getNutritionLogsDirectly()
                val chatList = repository.getChatMessagesDirectly()
                val waterList = repository.getAllWaterDirectly()
                val eventsList = repository.getAllEventsDirectly()
                val attendanceList = repository.getAttendanceListDirect()
                val photosList = repository.getAllPhotosDirectly()
                
                val cloudSyncData = com.example.data.local.CloudSyncData(
                    profile = currentProfile,
                    attendance = attendanceList,
                    workouts = workoutsList,
                    nutrition = nutritionList,
                    water = waterList,
                    events = eventsList,
                    chat = chatList,
                    photos = photosList
                )
                
                val moshi = com.squareup.moshi.Moshi.Builder().build()
                val adapter = moshi.adapter(com.example.data.local.CloudSyncData::class.java)
                val json = adapter.toJson(cloudSyncData)
                
                val client = okhttp3.OkHttpClient()
                val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
                val body = json.toRequestBody(mediaType)
                
                val request = okhttp3.Request.Builder()
                    .url("https://kvdb.io/fitgrow_sync_v1_df9b/$sanitizedEmail")
                    .put(body)
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        android.util.Log.d("CloudSync", "Data successfully synced to cloud for: $email")
                        _syncingState.value = "success"
                    } else {
                        android.util.Log.e("CloudSync", "Cloud sync failed with code: ${response.code}")
                        _syncingState.value = "error"
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _syncingState.value = "error"
            }
        }
    }

    fun loginAndSync(email: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val sanitizedEmail = email.lowercase().filter { it.isLetterOrDigit() }
                
                val client = okhttp3.OkHttpClient()
                val request = okhttp3.Request.Builder()
                    .url("https://kvdb.io/fitgrow_sync_v1_df9b/$sanitizedEmail")
                    .get()
                    .build()
                
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val json = response.body?.string()
                        if (!json.isNullOrBlank()) {
                            val moshi = com.squareup.moshi.Moshi.Builder().build()
                            val adapter = moshi.adapter(com.example.data.local.CloudSyncData::class.java)
                            val cloudSyncData = adapter.fromJson(json)
                            
                            if (cloudSyncData != null) {
                                // Clear existing local data
                                repository.clearAllLocalData()
                                
                                // Restore elements
                                cloudSyncData.profile?.let { repository.saveProfile(it.copy(email = email)) } ?: repository.saveProfile(ProfileInfo(email = email))
                                repository.insertWorkouts(cloudSyncData.workouts)
                                repository.insertNutritionLogs(cloudSyncData.nutrition)
                                repository.insertChatMessages(cloudSyncData.chat)
                                repository.insertWaterLogs(cloudSyncData.water)
                                repository.insertEvents(cloudSyncData.events)
                                repository.insertAttendance(cloudSyncData.attendance)
                                repository.insertPhotos(cloudSyncData.photos)
                                
                                onResult(true, "FOUND")
                                return@launch
                            }
                        }
                    }
                    
                    // No existing profile found (HTTP 404 or empty json), initialize fresh
                    repository.clearAllLocalData()
                    repository.saveProfile(ProfileInfo(email = email, isSetupComplete = false))
                    onResult(true, "NEW")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                onResult(false, e.localizedMessage ?: "Sync error")
            }
        }
    }
    
    // Auth & setup
    fun completeOnboarding(
        name: String, age: Int, height: Float, weight: Float, targetWeight: Float, targetDays: Int, 
        gymTime: String, breakfastTime: String, lunchTime: String, dinnerTime: String,
        targetCalories: Int, targetProtein: Int, targetWater: Int
    ) {
        viewModelScope.launch {
            val prefs = application.getSharedPreferences("fitgrow_accountability", android.content.Context.MODE_PRIVATE)
            prefs.edit().putLong("journey_start_time", System.currentTimeMillis()).apply()
            
            repository.saveProfile(ProfileInfo(
                name = name,
                age = age,
                height = height,
                startWeight = weight,
                currentWeight = weight,
                targetWeight = targetWeight,
                targetDays = targetDays,
                gymTime = gymTime,
                breakfastTime = breakfastTime,
                lunchTime = lunchTime,
                dinnerTime = dinnerTime,
                targetCalories = targetCalories,
                targetProtein = targetProtein,
                targetWater = targetWater,
                isSetupComplete = true
            ))

            val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val dateStr = sdf.format(java.util.Date())
            recordWeightHistoryLog(dateStr, weight)
        }
    }

    fun saveEmail(email: String) {
        viewModelScope.launch {
            val curProfile = profile.value ?: com.example.data.local.ProfileInfo()
            // Automagic API injection for developer email
            val injectedApiKey = if (email.trim() == "shubham08shat@gmail.com") {
                com.example.BuildConfig.GEMINI_API_KEY
            } else {
                curProfile.geminiApiKey
            }
            repository.saveProfile(curProfile.copy(email = email, geminiApiKey = injectedApiKey))
        }
    }
    
    fun updateTiming(type: String, time: String) {
        viewModelScope.launch {
            val curProfile = profile.value ?: return@launch
            val updated = when (type) {
                "gym" -> curProfile.copy(gymTime = time)
                "breakfast" -> curProfile.copy(breakfastTime = time)
                "lunch" -> curProfile.copy(lunchTime = time)
                "dinner" -> curProfile.copy(dinnerTime = time)
                else -> curProfile
            }
            repository.saveProfile(updated)
        }
    }

    fun persistImage(uriStr: String): String {
        try {
            if (uriStr.startsWith("file://")) return uriStr // already persisted
            val uri = if (uriStr.startsWith("/")) android.net.Uri.fromFile(java.io.File(uriStr)) else android.net.Uri.parse(uriStr)
            
            if (uriStr.startsWith("/")) return uri.toString() // already a local file but needed schema
            
            val resolver = application.contentResolver
            val inputStream = resolver.openInputStream(uri)
            val file = java.io.File(application.filesDir, "image_${System.currentTimeMillis()}.jpg")
            val outputStream = java.io.FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            return android.net.Uri.fromFile(file).toString()
        } catch(e: Exception) {
            return uriStr
        }
    }

    fun updateProfile(newCurrentWeight: Float? = null, newOriginalPic: String? = null, newCurrentPic: String? = null) {
        viewModelScope.launch {
            val curProfile = profile.value ?: return@launch
            
            val originalPicDest = if (newOriginalPic != null) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { persistImage(newOriginalPic) } else curProfile.originalPictureUri
            val currentPicDest = if (newCurrentPic != null) kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) { persistImage(newCurrentPic) } else curProfile.currentPictureUri

            val updated = curProfile.copy(
                currentWeight = newCurrentWeight ?: curProfile.currentWeight,
                originalPictureUri = originalPicDest,
                currentPictureUri = currentPicDest
            )
            repository.saveProfile(updated)
            
            if (newCurrentWeight != null) {
                val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
                val dateStr = sdf.format(java.util.Date())
                recordWeightHistoryLog(dateStr, newCurrentWeight)
            }
            
            if (newCurrentWeight != null && newCurrentWeight != curProfile.currentWeight) {
                val weightDiff = newCurrentWeight - curProfile.currentWeight
                val prompt = "I just updated my weight. Before it was ${curProfile.currentWeight}kg, now it is ${newCurrentWeight}kg. Difference: $weightDiff kg. My target is ${curProfile.targetWeight}kg. Tell me how I am doing as an Indian fitness coach. Be concise to exactly 1-2 lines in Hinglish."
                sendAiMessage(prompt)
            }
            
            // if both pics exist and currentPic was just updated, let's trigger comparison
            if (newCurrentPic != null && updated.originalPictureUri != null) {
                generatePicComparison(updated)
            } else if (newOriginalPic != null) {
                // If only BEFORE is uploaded, analyze it
                generatePicComparison(updated)
            }
        }
    }
    
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing = _isAnalyzing.asStateFlow()
    
    private fun generatePicComparison(curProfile: ProfileInfo) {
        viewModelScope.launch {
            try {
                _isAnalyzing.value = true
                repository.saveProfile(curProfile.copy(aiComparisonText = "Analyzing your physique... Please wait."))
                
                val apiKey = com.example.BuildConfig.GEMINI_API_KEY ?: return@launch
                
                val originalUri = curProfile.originalPictureUri
                val currentUri = curProfile.currentPictureUri
                val prompt = if (currentUri != null && originalUri != null) {
                    """
                    I have uploaded my Day 1 (BEFORE) photo and my CURRENT photo.
                    Please act as an elite fitness coach and deeply analyze the transformation.
                    Detail what has changed, what improved, what degraded if any, and what needs work.
                    Be genuinely honest. Do not just say good things, if progress is weak, say it.
                    Tone: Raw, honest, modern fitness coach. Give a brief summary in natural Hinglish.
                    Keep every value strictly under 2 or 3 lines. Do not generate long text.
                    Return a JSON with the following keys strictly:
                    - shoulders: string (status of shoulders, max 2 lines)
                    - chest: string (status of chest, max 2 lines)
                    - posture: string (status of posture, max 2 lines)
                    - overall: string (overall progress out of 100)
                    - comparison: string (your detailed honest advice paragraph, max 3 lines)
                    """.trimIndent()
                } else {
                    """
                    I have uploaded my BEFORE photo. 
                    Act as an elite fitness coach and deeply analyze this baseline physique.
                    Be genuinely honest. Tell me exactly what needs the most work.
                    Tone: Raw, honest, natural Hinglish.
                    Keep every value strictly under 2 lines. Do not generate long text.
                    Return a JSON with the following keys strictly:
                    - shoulders: string (status of shoulders, max 2 lines)
                    - chest: string (status of chest, max 2 lines)
                    - posture: string (status of posture, max 2 lines)
                    - overall: string (estimated starting potential out of 100)
                    - comparison: string (your detailed honest advice paragraph, max 2 lines)
                    """.trimIndent()
                }

                val parts = mutableListOf(Part(text = prompt))
                
                if (originalUri != null) {
                    val b64 = getBase64FromUri(originalUri)
                    if (b64 != null) parts.add(Part(inlineData = com.example.api.InlineData("image/jpeg", b64)))
                }
                
                if (currentUri != null) {
                    val b64 = getBase64FromUri(currentUri)
                    if (b64 != null) parts.add(Part(inlineData = com.example.api.InlineData("image/jpeg", b64)))
                }

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = parts)),
                    generationConfig = com.example.api.GenerationConfig(responseMimeType = "application/json")
                )

                val response = RetrofitClient.service.generateContentFlash(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: ""
                
                val finalProfile = profile.value ?: return@launch
                repository.saveProfile(finalProfile.copy(aiComparisonText = text))
                repository.sendChatMessage(ChatMessage(sender = "COACH", text = "Bhai teri progress report update kardi hai maine progress screen pe. Check kar le. Mehnat chalu rakh!"))
                
            } catch(e: Exception) {
                val p = profile.value ?: return@launch
                repository.saveProfile(p.copy(aiComparisonText = "Failed to analyze. ${e.message}"))
            } finally {
                _isAnalyzing.value = false
            }
        }
    }

    // Workout

    fun addWorkout(name: String, date: String) {
        viewModelScope.launch {
            repository.insertWorkout(WorkoutLog(dateStr = date, workoutName = name))
            repository.markAttendance(date, true)
            repository.calculateStreak()
            
            val updatedProfile = profile.value ?: return@launch
            val newStreak = updatedProfile.streak
            
            // Check for weekly completion
            if (newStreak > 0 && newStreak % 7 == 0) {
                val prompt = "I just hit a $newStreak day workout streak! Appreciate my consistency, tell me how much better I am doing, and hype me up for the next week in natural Hinglish."
                sendAiMessage(prompt)
            } else if (newStreak == 1) {
                // Just started
                val prompt = "I just did my first workout! Hype me up in natural Hinglish and tell me consistency is key."
                sendAiMessage(prompt)
            }
        }
    }

    fun resetWorkout(dateStr: String) {
        viewModelScope.launch {
            repository.deleteWorkoutByDate(dateStr)
            repository.deleteAttendance(dateStr)
            repository.calculateStreak()
        }
    }

    fun missWorkout(date: String) {
        viewModelScope.launch {
            repository.markAttendance(date, false)
            repository.calculateStreak()
            val curProfile = profile.value ?: return@launch
            if (curProfile.streak > 0) {
                // Streak broken warning AI msg
                sendAiMessage("I just missed my workout. Berate me lightly in casual Hinglish for breaking my ${curProfile.streak}-day streak, tell me 'aise physique nahi banega bhai' and to get back on track tomorrow.")
            }
        }
    }

    // Nutrition
    fun addNutrition(date: String, meal: String, cals: Int, protein: Int) {
        viewModelScope.launch {
            repository.insertNutrition(NutritionLog(dateStr = date, mealName = meal, calories = cals, protein = protein))
        }
    }
    
    fun getNutritionForDate(date: String) = repository.getNutritionForDate(date)

    fun getAllNutritionLogs() = repository.getAllNutritionLogs()

    fun getWaterForDate(date: String) = repository.getWaterForDate(date)

    fun getEventsForDate(date: String) = repository.getEventsForDate(date)

    fun markEvent(date: String, type: String, status: String) {
        viewModelScope.launch {
            repository.insertEvent(com.example.data.local.DailyEvent(dateStr = date, eventType = type, status = status))
        }
    }

    fun addWater(date: String, mlToAdd: Int) {
        viewModelScope.launch {
            val currentWaterLog = repository.getWaterForDate(date).firstOrNull()
            val newTotal = (currentWaterLog?.totalWaterMl ?: 0) + mlToAdd
            repository.insertWater(DailyWater(dateStr = date, totalWaterMl = newTotal))
        }
    }

    // Progress
    fun savePhoto(uri: String, type: String, date: String) {
        viewModelScope.launch {
            val curProfile = profile.value ?: return@launch
            repository.insertPhoto(ProgressPhoto(uri = uri, type = type, dateStr = date, streakSnapshot = curProfile.streak))
            
            // Trigger AI analysis based on photo type
            if (type == "BEFORE_WEEKLY" || type == "BEFORE_MONTHLY" || type == "BEFORE") {
                val prompt = """
                    I just uploaded my BEFORE photo (Day 1). 
                    Perform a detailed baseline AI Photo Analysis of my physique.
                    Focus only on visible changes (e.g., muscle definition, fat loss). Do not hallucinate.
                    You MUST output exactly 5 to 7 short lines.
                    Use natural Hinglish. Tell me you have saved this Day 1 photo.
                """.trimIndent()
                sendAiMessage(prompt, uri)
            } else if (type == "CURRENT_WEEKLY" || type == "CURRENT") {
                val prompt = """
                    I just uploaded my CURRENT photo (Weekly).
                    Compare it with my Day 1 (BEFORE) photo that you remember.
                    Focus only on visible changes (e.g., muscle definition, fat loss). Do not hallucinate.
                    You MUST output exactly 5 to 7 short lines.
                    Acknowledge even small progress honestly. Keep tone raw, respectful, and natural Hinglish.
                """.trimIndent()
                sendAiMessage(prompt, uri)
            } else if (type == "CURRENT_MONTHLY") {
                val prompt = """
                    I just uploaded my CURRENT photo (Monthly).
                    Compare this current picture with my Day 1 (BEFORE) photo.
                    Focus only on visible changes (e.g., muscle definition, fat loss). Do not hallucinate.
                    You MUST output exactly 5 to 7 short lines.
                    Keep tone raw, respectful, and natural Hinglish.
                """.trimIndent()
                sendAiMessage(prompt, uri)
            }
        }
    }
    
    fun getLatestPhoto(type: String) = repository.getLatestPhoto(type)

    fun triggerCheckInPromptIfNeeded() {
        val installTime = workoutPrefs.getLong("install_time", -1L)
        if (installTime != -1L) {
            val daysSince = (System.currentTimeMillis() - installTime) / (1000 * 60 * 60 * 24)
            val alreadyPrompted = workoutPrefs.getBoolean("has_triggered_intro_prompt_today", false)
            if (daysSince <= 15 && !alreadyPrompted) {
                workoutPrefs.edit().putBoolean("has_triggered_intro_prompt_today", true).apply()
                sendAiMessage("Ask the user how their body is feeling today.")
            }
        }
    }

    // AI Coach Chat
    fun sendUserMessage(text: String, imageUri: String? = null) {
        viewModelScope.launch {
            repository.sendChatMessage(ChatMessage(sender = "USER", text = text, imageUri = imageUri))
            sendAiMessage(text, imageUri)
        }
    }

    fun activateCoach(apiKey: String, onResult: (Boolean) -> Unit) {
        onResult(true)
    }

    private fun getBase64FromUri(uriStr: String): String? {
        try {
            val uri = if (uriStr.startsWith("/")) android.net.Uri.fromFile(java.io.File(uriStr)) else Uri.parse(uriStr)
            val resolver = application.contentResolver
            val bitmap = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                val source = android.graphics.ImageDecoder.createSource(resolver, uri)
                android.graphics.ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(resolver, uri)
            }
            // scale down to avoid large payloads
            val maxDim = 800
            val scale = Math.min(maxDim.toFloat() / bitmap.width, maxDim.toFloat() / bitmap.height)
            val scaledBitmap = android.graphics.Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
            val outputStream = ByteArrayOutputStream()
            scaledBitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 70, outputStream)
            return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
        } catch(e: Exception) {
            return null
        }
    }

    private fun sendAiMessage(prompt: String, imageUri: String? = null, providedApiKey: String? = null) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val apiKey = providedApiKey ?: com.example.BuildConfig.GEMINI_API_KEY ?: return@launch
                val sysPrompt = """
                    You are '@growswami', an elite, smart, and professional AI fitness coach integrated into the FitGrow app.
                    Tone rules: 
                    - Concise & Humanoid: Speak very little, like a busy, real-world coach. NO long paragraphs. Match the length of the user's input.
                    - Praise & Influence: Validate and praise the user when they do well or stay consistent.
                    - Tough Love: Provide strong, sharp motivation that hits hard but doesn't cross into insulting.
                    Core Rules:
                    - Smart Meal Vision: When analyzing a food photo, calculate the macros accurately. ALWAYS add: "Tip: Next time, keep a coin or note near the plate for perfect 100% accurate quantity measurement." Calculate macros even without reference.
                    - Progressive Overload: Proactively ask "Weight badhayein is week?" Do not update weights unless confirmed. ONLY ease workouts if injury is stated.
                    - Weight Updates: If user mentions new weight, acknowledge it, and briefly suggest 1-2 100% natural supplements (Whey/Creatine) suited for them.
                    Listen in natural Hinglish. User says: $prompt
                """.trimIndent()

                val parts = mutableListOf(Part(text = sysPrompt))
                if (imageUri != null) {
                    val b64 = getBase64FromUri(imageUri)
                    if (b64 != null) parts.add(Part(inlineData = com.example.api.InlineData("image/jpeg", b64)))
                }
                val request = GenerateContentRequest(contents = listOf(Content(parts = parts)))
                val response = RetrofitClient.service.generateContentFlash(apiKey, request)
                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text ?: "Sahi lag raha hai bhai, lage raho!"
                repository.sendChatMessage(ChatMessage(sender = "COACH", text = text))
            } catch(e: Exception) {
                repository.sendChatMessage(ChatMessage(sender = "COACH", text = "Bhai internet check kar, connection fail ho gaya."))
            }
        }
    }

    private var hasTriggeredRecalibrationThisSession = false

    suspend fun performDailyAccountabilityCheck(profileInfo: ProfileInfo) {
        val prefs = application.getSharedPreferences("fitgrow_accountability", android.content.Context.MODE_PRIVATE)
        val todayCal = java.util.Calendar.getInstance()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val todayStr = sdf.format(todayCal.time)
        
        var startTime = prefs.getLong("journey_start_time", -1L)
        if (startTime == -1L) {
            // Default to 5 days ago to demonstrate dynamic success calculation immediately
            val fiveDaysAgo = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -5) }
            startTime = fiveDaysAgo.timeInMillis
            prefs.edit().putLong("journey_start_time", startTime).apply()
        }
        _journeyStartTime.value = startTime
        
        val daysPassed = ((System.currentTimeMillis() - startTime) / (1000 * 60 * 60 * 24)).toInt()
        val targetDays = profileInfo.targetDays
        val daysLeft = maxOf(1, targetDays - daysPassed)
        
        // Find skipped days
        val allWorkouts = workouts.value
        val allNutrition = nutritionLogs.value
        
        val workoutDates = allWorkouts.map { it.dateStr }.toSet()
        val nutritionDates = allNutrition.map { it.dateStr }.toSet()
        
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = startTime
        
        var totalDaysCount = 0
        var totalScoreSum = 0f
        var skippedCount = 0
        
        while (!calendar.after(todayCal)) {
            val dStr = sdf.format(calendar.time)
            val hasWorkout = workoutDates.contains(dStr)
            val hasNutrition = nutritionDates.contains(dStr)
            
            val isToday = dStr == todayStr
            
            // Calculate daily score
            val dailyScore = (if (hasWorkout) 50f else 0f) + (if (hasNutrition) 50f else 0f)
            
            if (!isToday) {
                // For past days, if nothing is logged, it's a skipped day
                if (!hasWorkout && !hasNutrition) {
                    repository.markAttendance(dStr, false)
                    skippedCount++
                } else {
                    repository.markAttendance(dStr, true)
                }
            }
            
            totalDaysCount++
            totalScoreSum += dailyScore
            
            calendar.add(java.util.Calendar.DAY_OF_YEAR, 1)
        }
        
        // Calculate dynamic real-time success probability
        val rawProb = if (totalDaysCount > 0) (totalScoreSum / totalDaysCount) else 85f
        // Clamp between 10% and 98% for realism, but ensure it goes up immediately when they update!
        val prob = maxOf(10, minOf(98, rawProb.toInt()))
        
        val prevProb = prefs.getInt("previous_success_probability", 85)
        prefs.edit().putInt("previous_success_probability", prob).apply()
        
        _daysLeft.value = daysLeft
        _successProbability.value = prob
        _successTrend.value = if (prob >= prevProb) "Improving" else "Downfall"
        
        // Only trigger warning / recalibration once per session to prevent spamming
        if (skippedCount > 0 && !hasTriggeredRecalibrationThisSession) {
            val lastWarnedSkipCount = prefs.getInt("last_warned_skip_count", 0)
            if (skippedCount > lastWarnedSkipCount) {
                hasTriggeredRecalibrationThisSession = true
                prefs.edit().putInt("last_warned_skip_count", skippedCount).apply()
                
                val isWeightLoss = profileInfo.targetWeight < profileInfo.startWeight
                val adjustedCals: Int
                val adjustedProt: Int
                if (isWeightLoss) {
                    adjustedCals = maxOf(1400, profileInfo.targetCalories - (skippedCount * 30))
                    adjustedProt = minOf(200, profileInfo.targetProtein + (skippedCount * 2))
                } else {
                    adjustedCals = minOf(3500, profileInfo.targetCalories + (skippedCount * 40))
                    adjustedProt = minOf(200, profileInfo.targetProtein + (skippedCount * 2))
                }
                
                repository.saveProfile(profileInfo.copy(
                    targetCalories = adjustedCals,
                    targetProtein = adjustedProt
                ))
                
                val deviationPercent = skippedCount * 5
                val msgText = "Tumne pichle $skippedCount din update nahi kiya, target $deviationPercent% se peeche ho gaya hai. Kya hua?\n\nOriginal target ko hit karne ke liye maine tumhare daily goals ko adjust kiya hai: Ab se target hai $adjustedCals kcal aur ${adjustedProt}g protein."
                repository.sendChatMessage(ChatMessage(sender = "COACH", text = msgText))
                _hasUnreadCoachMessage.value = true
            }
        }
    }

    fun eraseAllData(onComplete: () -> Unit) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            // 1. Clear local database tables
            try {
                com.example.data.local.AppDatabase.getDatabase(application).clearAllTables()
            } catch (e: Exception) {
                e.printStackTrace()
            }

            // 2. Clear all Shared Preferences
            val sharedPrefsToClear = listOf(
                "fitgrow_routines",
                "fitgrow_measurements",
                "fitgrow_prefs",
                "fitgrow_accountability",
                "fitgrow_weight_history",
                "fitgrow_ai_workout"
            )
            sharedPrefsToClear.forEach { prefName ->
                try {
                    application.getSharedPreferences(prefName, android.content.Context.MODE_PRIVATE).edit().clear().apply()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // 3. Reset in-memory values to initial
            _weightHistory.value = emptyList()
            _aiWorkoutSuggestions.value = null
            _isAiWorkoutGenerating.value = false
            _hasUnreadCoachMessage.value = false
            _daysLeft.value = 30
            _successProbability.value = 85
            _successTrend.value = "Improving"
            _journeyStartTime.value = -1L
            _isUserAuthenticated.value = false

            // 4. Run completion callback on Main thread
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                onComplete()
            }
        }
    }
}
