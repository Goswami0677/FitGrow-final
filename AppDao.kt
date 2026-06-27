package com.example.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    // Profile
    @Query("SELECT * FROM profile_info WHERE id = 1")
    fun getProfile(): Flow<ProfileInfo?>

    @Query("SELECT * FROM profile_info WHERE id = 1")
    suspend fun getProfileDirectly(): ProfileInfo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveProfile(profile: ProfileInfo)

    @Query("UPDATE profile_info SET currentWeight = :weight WHERE id = 1")
    suspend fun updateCurrentWeight(weight: Float)

    @Query("UPDATE profile_info SET targetWeight = :weight WHERE id = 1")
    suspend fun updateTargetWeight(weight: Float)
    
    @Query("UPDATE profile_info SET streak = :streak WHERE id = 1")
    suspend fun updateStreak(streak: Int)

    // Attendance
    @Query("SELECT * FROM attendance_logs")
    fun getAllAttendance(): Flow<List<AttendanceLog>>

    @Query("SELECT * FROM attendance_logs")
    suspend fun getAttendanceListDirect(): List<AttendanceLog>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveAttendance(log: AttendanceLog)

    @Query("DELETE FROM attendance_logs WHERE dateStr = :dateStr")
    suspend fun deleteAttendance(dateStr: String)

    // Workouts
    @Query("SELECT * FROM workout_logs ORDER BY timestamp DESC")
    fun getWorkouts(): Flow<List<WorkoutLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkout(log: WorkoutLog)

    @Query("DELETE FROM workout_logs WHERE dateStr = :dateStr")
    suspend fun deleteWorkoutByDate(dateStr: String)

    // Nutrition
    @Query("SELECT * FROM nutrition_logs ORDER BY timestamp ASC")
    fun getAllNutritionLogs(): Flow<List<NutritionLog>>

    @Query("SELECT * FROM nutrition_logs WHERE dateStr = :dateStr ORDER BY timestamp DESC")
    fun getNutritionForDate(dateStr: String): Flow<List<NutritionLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutrition(log: NutritionLog)

    // Photos
    @Query("SELECT * FROM progress_photos WHERE type = :type ORDER BY timestamp DESC LIMIT 1")
    fun getLatestPhoto(type: String): Flow<ProgressPhoto?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhoto(photo: ProgressPhoto)
    
    @Query("SELECT * FROM progress_photos ORDER BY timestamp DESC")
    suspend fun getAllPhotos(): List<ProgressPhoto>

    // Chat
    @Query("SELECT * FROM chat_messages ORDER BY timestamp ASC")
    fun getChatMessages(): Flow<List<ChatMessage>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessage(msg: ChatMessage)

    // Water
    @Query("SELECT * FROM daily_water WHERE dateStr = :dateStr")
    fun getWaterForDate(dateStr: String): Flow<DailyWater?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWater(log: DailyWater)

    // Daily Events
    @Query("SELECT * FROM daily_events WHERE dateStr = :dateStr")
    fun getEventsForDate(dateStr: String): Flow<List<DailyEvent>>
    
    @Query("SELECT * FROM daily_events")
    fun getAllEvents(): Flow<List<DailyEvent>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvent(event: DailyEvent)

    // Direct fetch queries for cloud backup and sync
    @Query("SELECT * FROM workout_logs")
    suspend fun getWorkoutsDirectly(): List<WorkoutLog>

    @Query("SELECT * FROM nutrition_logs")
    suspend fun getNutritionLogsDirectly(): List<NutritionLog>

    @Query("SELECT * FROM chat_messages")
    suspend fun getChatMessagesDirectly(): List<ChatMessage>

    @Query("SELECT * FROM daily_water")
    suspend fun getAllWaterDirectly(): List<DailyWater>

    @Query("SELECT * FROM daily_events")
    suspend fun getAllEventsDirectly(): List<DailyEvent>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWorkouts(workouts: List<WorkoutLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNutritionLogs(nutrition: List<NutritionLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChatMessages(chat: List<ChatMessage>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWaterLogs(water: List<DailyWater>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEvents(events: List<DailyEvent>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttendance(attendance: List<AttendanceLog>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhotos(photos: List<ProgressPhoto>)

    @Query("DELETE FROM profile_info")
    suspend fun clearProfile()

    @Query("DELETE FROM attendance_logs")
    suspend fun clearAttendance()

    @Query("DELETE FROM workout_logs")
    suspend fun clearWorkouts()

    @Query("DELETE FROM nutrition_logs")
    suspend fun clearNutrition()

    @Query("DELETE FROM progress_photos")
    suspend fun clearPhotos()

    @Query("DELETE FROM chat_messages")
    suspend fun clearChat()

    @Query("DELETE FROM daily_water")
    suspend fun clearWater()

    @Query("DELETE FROM daily_events")
    suspend fun clearEvents()
}
