package com.example.data.repository

import com.example.data.local.*
import kotlinx.coroutines.flow.Flow

class AppRepository(private val dao: AppDao) {
    val profile: Flow<ProfileInfo?> = dao.getProfile()
    val attendance: Flow<List<AttendanceLog>> = dao.getAllAttendance()
    val workouts: Flow<List<WorkoutLog>> = dao.getWorkouts()
    val chatMessages: Flow<List<ChatMessage>> = dao.getChatMessages()

    suspend fun saveProfile(profileInfo: ProfileInfo) {
        dao.saveProfile(profileInfo)
    }

    suspend fun getAttendanceListDirect(): List<AttendanceLog> {
        return dao.getAttendanceListDirect()
    }

    suspend fun markAttendance(dateStr: String, isDone: Boolean) {
        dao.saveAttendance(AttendanceLog(dateStr, isDone))
    }

    suspend fun deleteAttendance(dateStr: String) {
        dao.deleteAttendance(dateStr)
    }
    
    suspend fun calculateStreak() {
        val list = dao.getAttendanceListDirect()
        val computedStreak = calculateStreakFromLogs(list)
        val currentProfile = dao.getProfileDirectly()
        if (currentProfile != null) {
            dao.saveProfile(currentProfile.copy(streak = computedStreak))
        }
    }

    private fun calculateStreakFromLogs(attendance: List<AttendanceLog>): Int {
        val completedDates = attendance.filter { it.isDone }.map { it.dateStr }.toSet()
        if (completedDates.isEmpty()) return 0

        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val today = java.util.Calendar.getInstance()
        val todayStr = sdf.format(today.time)
        
        val yesterday = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DAY_OF_YEAR, -1) }
        val yesterdayStr = sdf.format(yesterday.time)
        
        val startCal: java.util.Calendar
        if (completedDates.contains(todayStr)) {
            startCal = today
        } else if (completedDates.contains(yesterdayStr)) {
            // Check if today was marked MISSED
            val todayLog = attendance.find { it.dateStr == todayStr }
            if (todayLog != null && !todayLog.isDone) {
                return 0 // marked missed today, so streak is 0
            }
            startCal = yesterday
        } else {
            return 0
        }
        
        var streak = 0
        val checkCal = startCal.clone() as java.util.Calendar
        while (true) {
            val checkStr = sdf.format(checkCal.time)
            if (completedDates.contains(checkStr)) {
                streak++
                checkCal.add(java.util.Calendar.DAY_OF_YEAR, -1)
            } else {
                break
            }
        }
        return streak
    }

    suspend fun insertWorkout(log: WorkoutLog) {
        dao.insertWorkout(log)
    }

    suspend fun deleteWorkoutByDate(dateStr: String) {
        dao.deleteWorkoutByDate(dateStr)
    }

    fun getNutritionForDate(dateStr: String): Flow<List<NutritionLog>> {
        return dao.getNutritionForDate(dateStr)
    }

    fun getAllNutritionLogs(): Flow<List<NutritionLog>> {
        return dao.getAllNutritionLogs()
    }

    suspend fun insertNutrition(log: NutritionLog) {
        dao.insertNutrition(log)
    }

    fun getLatestPhoto(type: String): Flow<ProgressPhoto?> {
        return dao.getLatestPhoto(type)
    }

    suspend fun insertPhoto(photo: ProgressPhoto) {
        dao.insertPhoto(photo)
    }
    
    suspend fun updateWeights(current: Float, target: Float) {
        dao.updateCurrentWeight(current)
        dao.updateTargetWeight(target)
    }

    suspend fun sendChatMessage(msg: ChatMessage) {
        dao.insertChatMessage(msg)
    }

    fun getWaterForDate(dateStr: String): Flow<DailyWater?> {
        return dao.getWaterForDate(dateStr)
    }

    suspend fun insertWater(log: DailyWater) {
        dao.insertWater(log)
    }

    fun getEventsForDate(dateStr: String): Flow<List<DailyEvent>> {
        return dao.getEventsForDate(dateStr)
    }

    fun getAllEvents(): Flow<List<DailyEvent>> {
        return dao.getAllEvents()
    }

    suspend fun insertEvent(event: DailyEvent) {
        dao.insertEvent(event)
    }

    suspend fun getWorkoutsDirectly(): List<WorkoutLog> = dao.getWorkoutsDirectly()
    suspend fun getNutritionLogsDirectly(): List<NutritionLog> = dao.getNutritionLogsDirectly()
    suspend fun getChatMessagesDirectly(): List<ChatMessage> = dao.getChatMessagesDirectly()
    suspend fun getAllWaterDirectly(): List<DailyWater> = dao.getAllWaterDirectly()
    suspend fun getAllEventsDirectly(): List<DailyEvent> = dao.getAllEventsDirectly()
    suspend fun getProfileDirectly(): ProfileInfo? = dao.getProfileDirectly()
    suspend fun getAllPhotosDirectly(): List<ProgressPhoto> = dao.getAllPhotos()

    suspend fun insertWorkouts(workouts: List<WorkoutLog>) = dao.insertWorkouts(workouts)
    suspend fun insertNutritionLogs(nutrition: List<NutritionLog>) = dao.insertNutritionLogs(nutrition)
    suspend fun insertChatMessages(chat: List<ChatMessage>) = dao.insertChatMessages(chat)
    suspend fun insertWaterLogs(water: List<DailyWater>) = dao.insertWaterLogs(water)
    suspend fun insertEvents(events: List<DailyEvent>) = dao.insertEvents(events)
    suspend fun insertAttendance(attendance: List<AttendanceLog>) = dao.insertAttendance(attendance)
    suspend fun insertPhotos(photos: List<ProgressPhoto>) = dao.insertPhotos(photos)

    suspend fun clearAllLocalData() {
        dao.clearProfile()
        dao.clearAttendance()
        dao.clearWorkouts()
        dao.clearNutrition()
        dao.clearPhotos()
        dao.clearChat()
        dao.clearWater()
        dao.clearEvents()
    }
}
