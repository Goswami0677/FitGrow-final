package com.example.data.local

import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CloudSyncData(
    val profile: ProfileInfo?,
    val attendance: List<AttendanceLog>,
    val workouts: List<WorkoutLog>,
    val nutrition: List<NutritionLog>,
    val water: List<DailyWater>,
    val events: List<DailyEvent>,
    val chat: List<ChatMessage>,
    val photos: List<ProgressPhoto>
)
