package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "profile_info")
@JsonClass(generateAdapter = true)
data class ProfileInfo(
    @PrimaryKey val id: Int = 1,
    val name: String = "",
    val age: Int = 0,
    val height: Float = 0f,
    val startWeight: Float = 0f,
    val targetWeight: Float = 0f,
    val currentWeight: Float = 0f,
    val streak: Int = 0,
    val isSetupComplete: Boolean = false,
    val profileImageUri: String? = null,
    val originalPictureUri: String? = null,
    val currentPictureUri: String? = null,
    val bodyType: String? = null,
    val aiComparisonText: String? = null,
    val geminiApiKey: String? = null,
    val email: String? = null,
    val targetDays: Int = 30,
    val gymTime: String = "18:00",
    val breakfastTime: String = "08:00",
    val lunchTime: String = "13:00",
    val dinnerTime: String = "20:00",
    val targetCalories: Int = 2000,
    val targetProtein: Int = 120,
    val targetWater: Int = 3000
)

@Entity(tableName = "daily_water")
@JsonClass(generateAdapter = true)
data class DailyWater(
    @PrimaryKey val dateStr: String, // YYYY-MM-DD
    val totalWaterMl: Int = 0
)

@Entity(tableName = "attendance_logs")
@JsonClass(generateAdapter = true)
data class AttendanceLog(
    @PrimaryKey val dateStr: String, // YYYY-MM-DD
    val isDone: Boolean
)

@Entity(tableName = "workout_logs")
@JsonClass(generateAdapter = true)
data class WorkoutLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateStr: String,
    val workoutName: String,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "nutrition_logs")
@JsonClass(generateAdapter = true)
data class NutritionLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateStr: String,
    val mealName: String,
    val calories: Int,
    val protein: Int,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "progress_photos")
@JsonClass(generateAdapter = true)
data class ProgressPhoto(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val uri: String,
    val type: String, // "BEFORE_WEEKLY", "CURRENT_WEEKLY", "BEFORE_MONTHLY", "CURRENT_MONTHLY", "CHAT"
    val dateStr: String,
    val streakSnapshot: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "daily_events")
@JsonClass(generateAdapter = true)
data class DailyEvent(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dateStr: String, // YYYY-MM-DD
    val eventType: String, // Gym, Breakfast, Lunch, Dinner
    val status: String, // DONE, MISSED
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "chat_messages")
@JsonClass(generateAdapter = true)
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val sender: String, // "USER", "AI"
    val text: String,
    val imageUri: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
