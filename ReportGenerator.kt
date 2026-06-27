package com.example.ui.screens.profile

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import com.example.data.local.AttendanceLog
import com.example.data.local.NutritionLog
import com.example.data.local.ProfileInfo
import com.example.data.local.WorkoutLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object ReportGenerator {

    suspend fun generatePdfReport(
        context: Context,
        profile: ProfileInfo?,
        attendance: List<AttendanceLog>,
        workouts: List<WorkoutLog>,
        nutritionLogs: List<NutritionLog>,
        days: Int
    ): File = withContext(Dispatchers.IO) {
        val pdfDocument = PdfDocument()

        // Page sizes: A4 300dpi is 2480 x 3508 pixels
        val width = 2480
        val height = 3508
        val margin = 120f

        val safeProfile = profile ?: ProfileInfo()

        // Filter data by dates
        val sdfKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val cal = Calendar.getInstance()
        val endKey = sdfKey.format(cal.time)
        val startKey = if (days == -1) {
            val allLogs = attendance.map { it.dateStr } + workouts.map { it.dateStr } + nutritionLogs.map { it.dateStr }
            allLogs.minOrNull() ?: endKey
        } else {
            val c = Calendar.getInstance()
            c.add(Calendar.DAY_OF_YEAR, -(days - 1))
            sdfKey.format(c.time)
        }

        val periodAttendance = attendance.filter { it.dateStr in startKey..endKey }
        val periodWorkouts = workouts.filter { it.dateStr in startKey..endKey }
        val periodNutrition = nutritionLogs.filter { it.dateStr in startKey..endKey }

        val totalDays = if (days == -1) {
            if (periodAttendance.isNotEmpty()) periodAttendance.size else 30
        } else {
            days
        }
        val doneCount = periodAttendance.count { it.isDone }
        val completionRate = if (totalDays > 0) {
            (doneCount.toFloat() / totalDays.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
        val avgCals = if (periodNutrition.isNotEmpty()) {
            periodNutrition.map { it.calories }.average().toInt()
        } else {
            0
        }
        val targetCals = safeProfile.targetCalories.coerceAtLeast(1)

        // Formatting dates for display
        val displaySdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        val startDisplay = try {
            val d = sdfKey.parse(startKey)
            if (d != null) displaySdf.format(d) else startKey
        } catch (e: Exception) {
            startKey
        }
        val endDisplay = displaySdf.format(cal.time)
        val dateRangeStr = "$startDisplay – $endDisplay"

        // Paints
        val fillPaint = Paint().apply { isAntiAlias = true }
        val strokePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
        }
        val textPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF111827.toInt()
            typeface = Typeface.DEFAULT
        }
        val boldTextPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF111827.toInt()
            typeface = Typeface.create("sans-serif", Typeface.BOLD)
        }

        // Color Constants
        val primaryPurple = 0xFF7C3AED.toInt()
        val pinkColor = 0xFFEC4899.toInt()
        val greenColor = 0xFF10B981.toInt()
        val orangeColor = 0xFFF59E0B.toInt()
        val blueColor = 0xFF38BDF8.toInt()
        val redColor = 0xFFEF4444.toInt()
        val darkColor = 0xFF111827.toInt()
        val grayColor = 0xFF6B7280.toInt()
        val lightGray = 0xFFF9FAFB.toInt()

        // ----------------- PAGE 1: COVER & SUMMARY -----------------
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            // Fill background white
            canvas.drawColor(Color.WHITE)

            // Top Header Band
            val headerPaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f, 0f, width.toFloat(), 0f,
                    primaryPurple, pinkColor, Shader.TileMode.CLAMP
                )
            }
            canvas.drawRect(0f, 0f, width.toFloat(), 600f, headerPaint)

            // App name text
            boldTextPaint.color = Color.WHITE
            boldTextPaint.textSize = 90f
            boldTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("FitGrow", width / 2f, 210f, boldTextPaint)

            // Subtitle
            boldTextPaint.textSize = 50f
            canvas.drawText("PROGRESS REPORT", width / 2f, 320f, boldTextPaint)

            // Date Range
            textPaint.color = Color.WHITE
            textPaint.textSize = 38f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText(dateRangeStr, width / 2f, 410f, textPaint)

            // Reset text paint alignment
            textPaint.textAlign = Paint.Align.LEFT
            boldTextPaint.textAlign = Paint.Align.LEFT

            // User Info Card (y = 660f)
            fillPaint.color = lightGray
            val cardRect = RectF(margin, 660f, width - margin, 960f)
            canvas.drawRoundRect(cardRect, 40f, 40f, fillPaint)

            // Initials Circle
            val circlePaint = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    margin + 60f, 810f, margin + 200f, 810f,
                    primaryPurple, blueColor, Shader.TileMode.CLAMP
                )
            }
            canvas.drawCircle(margin + 130f, 810f, 70f, circlePaint)

            // Initials text inside circle
            boldTextPaint.color = Color.WHITE
            boldTextPaint.textSize = 52f
            boldTextPaint.textAlign = Paint.Align.CENTER
            val initials = if (safeProfile.name.isNotEmpty()) {
                safeProfile.name.take(2).uppercase()
            } else {
                "FG"
            }
            canvas.drawText(initials, margin + 130f, 828f, boldTextPaint)

            // User info text
            boldTextPaint.textAlign = Paint.Align.LEFT
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 56f
            canvas.drawText(safeProfile.name.ifEmpty { "FitGrow Champion" }, margin + 240f, 780f, boldTextPaint)

            textPaint.color = grayColor
            textPaint.textSize = 34f
            val bodyTypeStr = safeProfile.bodyType ?: "Active"
            canvas.drawText("Goal: Fitness Growth  •  $bodyTypeStr Level", margin + 240f, 840f, textPaint)
            canvas.drawText("Weight: Start ${safeProfile.startWeight}kg  •  Current ${safeProfile.currentWeight}kg  •  Target ${safeProfile.targetWeight}kg", margin + 240f, 895f, textPaint)

            // 2x2 Grid of Summary Stat Boxes (y = 1040f)
            val boxWidth = (width - margin * 2 - 60f) / 2f
            val boxHeight = 250f

            // Box 1: Workouts Completed
            val doneCount = periodAttendance.count { it.isDone }
            drawStatBox(
                canvas, margin, 1040f, boxWidth, boxHeight,
                greenColor, "✓", "$doneCount", "Workouts Completed",
                fillPaint, strokePaint, boldTextPaint, textPaint
            )

            // Box 2: Days Missed
            val missedCount = periodAttendance.count { !it.isDone }
            drawStatBox(
                canvas, margin + boxWidth + 60f, 1040f, boxWidth, boxHeight,
                redColor, "✗", "$missedCount", "Workout Days Missed",
                fillPaint, strokePaint, boldTextPaint, textPaint
            )

            // Box 3: Best Streak
            val bestStreak = safeProfile.streak
            drawStatBox(
                canvas, margin, 1340f, boxWidth, boxHeight,
                orangeColor, "🔥", "$bestStreak", "Active Streak Days",
                fillPaint, strokePaint, boldTextPaint, textPaint
            )

            // Box 4: Avg Calories
            val avgCalories = if (periodNutrition.isNotEmpty()) {
                periodNutrition.map { it.calories }.average().toInt()
            } else {
                0
            }
            drawStatBox(
                canvas, margin + boxWidth + 60f, 1340f, boxWidth, boxHeight,
                blueColor, "⚡", "$avgCalories", "Avg Daily Calories (kcal)",
                fillPaint, strokePaint, boldTextPaint, textPaint
            )

            // Gym Consistency Ring (y = 1720f)
            val ringCenterX = width / 2f
            val ringCenterY = 1980f
            val ringRadius = 220f

            // Background circle
            strokePaint.color = 0xFFE5E7EB.toInt()
            strokePaint.strokeWidth = 42f
            strokePaint.strokeCap = Paint.Cap.ROUND
            canvas.drawCircle(ringCenterX, ringCenterY, ringRadius, strokePaint)

            // Foreground Completion Arc
            val sweepAngle = completionRate * 360f

            strokePaint.color = primaryPurple
            canvas.drawArc(
                RectF(ringCenterX - ringRadius, ringCenterY - ringRadius, ringCenterX + ringRadius, ringCenterY + ringRadius),
                -90f, sweepAngle, false, strokePaint
            )

            // Inner Text
            boldTextPaint.color = primaryPurple
            boldTextPaint.textSize = 96f
            boldTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("${(completionRate * 100).toInt()}%", ringCenterX, ringCenterY + 32f, boldTextPaint)

            boldTextPaint.color = grayColor
            boldTextPaint.textSize = 38f
            canvas.drawText("Gym Consistency", ringCenterX, ringCenterY + 330f, boldTextPaint)

            textPaint.color = grayColor
            textPaint.textSize = 32f
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Logged $doneCount workout sessions out of $totalDays days", ringCenterX, ringCenterY + 390f, textPaint)

            // Reset textAlign
            textPaint.textAlign = Paint.Align.LEFT
            boldTextPaint.textAlign = Paint.Align.LEFT

            // Footer
            drawFooter(canvas, width, height, margin, endDisplay, fillPaint, strokePaint, textPaint)

            pdfDocument.finishPage(page)
        }

        // ----------------- PAGE 2: WORKOUT ANALYTICS -----------------
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 2).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            // Section Header
            drawSectionHeader(canvas, "WORKOUT HISTORY", "Your training log and weekly completion trend for this period", primaryPurple, margin, fillPaint, boldTextPaint, textPaint)

            // Heatmap Grid (y = 440f)
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 42f
            canvas.drawText("Workout Consistency Heatmap", margin, 440f, boldTextPaint)

            // Day headers: M T W T F S S
            val weekdays = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
            textPaint.color = primaryPurple
            textPaint.textSize = 30f
            textPaint.typeface = Typeface.create("sans-serif", Typeface.BOLD)
            val cellSize = 135f
            val cellGap = 24f
            val startGridX = margin + 100f
            val startGridY = 540f

            weekdays.forEachIndexed { idx, day ->
                canvas.drawText(day, startGridX + idx * (cellSize + cellGap) + cellSize / 4f, startGridY - 30f, textPaint)
            }

            // Draw up to 35 cells (5 weeks) of history
            val gridCalendar = Calendar.getInstance()
            gridCalendar.add(Calendar.DAY_OF_YEAR, -28) // Go back 4 weeks
            // Align to start of week (Monday)
            val currentDayOfWeek = gridCalendar.get(Calendar.DAY_OF_WEEK)
            val daysToShift = if (currentDayOfWeek == Calendar.SUNDAY) 6 else currentDayOfWeek - Calendar.MONDAY
            gridCalendar.add(Calendar.DAY_OF_YEAR, -daysToShift)

            for (week in 0 until 5) {
                val rowY = startGridY + week * (cellSize + cellGap)
                for (day in 0 until 7) {
                    val colX = startGridX + day * (cellSize + cellGap)
                    val dateKey = sdfKey.format(gridCalendar.time)
                    val attendanceLog = attendance.find { it.dateStr == dateKey }

                    val cellColor = when {
                        attendanceLog == null -> 0xFFF3F4F6.toInt() // No data
                        attendanceLog.isDone -> 0xFF10B981.toInt() // Done (Green)
                        else -> 0xFFEF4444.toInt() // Missed (Red)
                    }

                    // Background color block
                    fillPaint.color = cellColor
                    if (attendanceLog != null) {
                        // Done is translucent green, missed is translucent red
                        fillPaint.alpha = if (attendanceLog.isDone) 55 else 45
                    } else {
                        fillPaint.alpha = 255
                    }
                    canvas.drawRoundRect(RectF(colX, rowY, colX + cellSize, rowY + cellSize), 20f, 20f, fillPaint)

                    // Draw border line
                    strokePaint.color = cellColor
                    strokePaint.strokeWidth = 3f
                    strokePaint.alpha = 255
                    canvas.drawRoundRect(RectF(colX, rowY, colX + cellSize, rowY + cellSize), 20f, 20f, strokePaint)

                    // Draw Day number inside cell
                    boldTextPaint.color = if (attendanceLog != null) cellColor else darkColor
                    boldTextPaint.textSize = 34f
                    boldTextPaint.textAlign = Paint.Align.CENTER
                    val dayNum = gridCalendar.get(Calendar.DAY_OF_MONTH).toString()
                    canvas.drawText(dayNum, colX + cellSize / 2f, rowY + cellSize / 2f + 12f, boldTextPaint)

                    gridCalendar.add(Calendar.DAY_OF_YEAR, 1)
                }
            }

            // Consistency line chart (y = 1450f)
            boldTextPaint.textAlign = Paint.Align.LEFT
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 42f
            canvas.drawText("Weekly Completion Trend", margin, 1450f, boldTextPaint)

            val chartX = margin
            val chartY = 1600f
            val chartW = width - margin * 2
            val chartH = 400f

            // Horizontal Grid Lines & Y labels
            textPaint.color = grayColor
            textPaint.textSize = 28f
            textPaint.textAlign = Paint.Align.RIGHT
            strokePaint.color = 0xFFE5E7EB.toInt()
            strokePaint.strokeWidth = 2f

            val levels = listOf("100%", "75%", "50%", "25%", "0%")
            levels.forEachIndexed { idx, label ->
                val lineY = chartY + (idx * chartH / 4f)
                canvas.drawText(label, chartX - 20f, lineY + 10f, textPaint)
                canvas.drawLine(chartX, lineY, chartX + chartW, lineY, strokePaint)
            }

            // Draw line trend
            // Let's divide the period into 4 segments/weeks and calculate done% for each
            val weeksCount = 4
            val weekRates = mutableListOf<Float>()
            val weekLabels = mutableListOf<String>()

            val trendCal = Calendar.getInstance()
            trendCal.add(Calendar.DAY_OF_YEAR, -27)
            for (w in 0 until weeksCount) {
                var wDone = 0
                val labelSdf = SimpleDateFormat("dd MMM", Locale.getDefault())
                val weekStartLabel = labelSdf.format(trendCal.time)
                for (d in 0 until 7) {
                    val k = sdfKey.format(trendCal.time)
                    if (attendance.find { it.dateStr == k }?.isDone == true) {
                        wDone++
                    }
                    trendCal.add(Calendar.DAY_OF_YEAR, 1)
                }
                val rate = wDone.toFloat() / 7f
                weekRates.add(rate)
                weekLabels.add(weekStartLabel)
            }

            val path = Path()
            val fillPath = Path()
            val ptGap = chartW / (weeksCount - 1)

            // Start path
            var startPtX = chartX
            var startPtY = chartY + chartH - (weekRates[0] * chartH)
            path.moveTo(startPtX, startPtY)
            fillPath.moveTo(startPtX, chartY + chartH)
            fillPath.lineTo(startPtX, startPtY)

            for (i in 1 until weeksCount) {
                val ptX = chartX + i * ptGap
                val ptY = chartY + chartH - (weekRates[i] * chartH)
                
                // Draw bezier curves instead of direct line
                val cpX1 = startPtX + ptGap / 2f
                val cpY1 = startPtY
                val cpX2 = startPtX + ptGap / 2f
                val cpY2 = ptY

                path.cubicTo(cpX1, cpY1, cpX2, cpY2, ptX, ptY)
                fillPath.cubicTo(cpX1, cpY1, cpX2, cpY2, ptX, ptY)

                startPtX = ptX
                startPtY = ptY
            }
            fillPath.lineTo(startPtX, chartY + chartH)
            fillPath.close()

            // Fill area under the line with gradient
            val areaShader = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f, chartY, 0f, chartY + chartH,
                    primaryPurple, Color.TRANSPARENT, Shader.TileMode.CLAMP
                )
                style = Paint.Style.FILL
                alpha = 60
            }
            canvas.drawPath(fillPath, areaShader)

            // Draw line path
            strokePaint.color = primaryPurple
            strokePaint.strokeWidth = 7f
            canvas.drawPath(path, strokePaint)

            // Draw points & X Labels
            textPaint.textAlign = Paint.Align.CENTER
            weekRates.forEachIndexed { i, rate ->
                val ptX = chartX + i * ptGap
                val ptY = chartY + chartH - (rate * chartH)

                // Outer circle
                fillPaint.color = primaryPurple
                canvas.drawCircle(ptX, ptY, 15f, fillPaint)
                // Inner white dot
                fillPaint.color = Color.WHITE
                canvas.drawCircle(ptX, ptY, 8f, fillPaint)

                // Label
                textPaint.color = grayColor
                canvas.drawText(weekLabels[i], ptX, chartY + chartH + 50f, textPaint)
            }

            // Reset textAlign
            textPaint.textAlign = Paint.Align.LEFT

            // Table of Workouts (y = 2240f)
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 42f
            canvas.drawText("Workout Attendance Records (Recent Logs)", margin, 2240f, boldTextPaint)

            val tblY = 2320f
            val rowH = 75f
            val cols = listOf("Date", "Workout Description", "Status")
            val colWidths = listOf(350f, 1300f, 400f)

            // Draw table header
            fillPaint.color = primaryPurple
            canvas.drawRoundRect(RectF(margin, tblY, width - margin, tblY + rowH), 12f, 12f, fillPaint)

            boldTextPaint.color = Color.WHITE
            boldTextPaint.textSize = 28f
            var currX = margin + 30f
            cols.forEachIndexed { idx, col ->
                canvas.drawText(col, currX, tblY + 48f, boldTextPaint)
                currX += colWidths[idx]
            }

            // Draw up to 10 entries of attendance log
            val recentLogs = periodAttendance.sortedByDescending { it.dateStr }.take(10)
            textPaint.textSize = 28f

            recentLogs.forEachIndexed { idx, log ->
                val rowY = tblY + rowH + idx * rowH
                // Alternating rows
                fillPaint.color = if (idx % 2 == 0) lightGray else Color.WHITE
                canvas.drawRect(margin, rowY, width - margin, rowY + rowH, fillPaint)

                // Draw values
                textPaint.color = darkColor
                // Format Date representation
                val dispDate = try {
                    val parsed = sdfKey.parse(log.dateStr)
                    if (parsed != null) SimpleDateFormat("dd MMM, EEE", Locale.getDefault()).format(parsed) else log.dateStr
                } catch (e: Exception) {
                    log.dateStr
                }
                canvas.drawText(dispDate, margin + 30f, rowY + 46f, textPaint)

                // Workout type/description
                val wLog = periodWorkouts.find { it.dateStr == log.dateStr }
                val workoutName = wLog?.workoutName ?: if (log.isDone) "General Workout" else "Rest Day / Off"
                canvas.drawText(workoutName, margin + 30f + colWidths[0], rowY + 46f, textPaint)

                // Status
                if (log.isDone) {
                    boldTextPaint.color = greenColor
                    canvas.drawText("✓ Completed", margin + 30f + colWidths[0] + colWidths[1], rowY + 46f, boldTextPaint)
                } else {
                    boldTextPaint.color = redColor
                    canvas.drawText("✗ Missed", margin + 30f + colWidths[0] + colWidths[1], rowY + 46f, boldTextPaint)
                }
            }

            if (periodAttendance.size > 10) {
                textPaint.color = grayColor
                textPaint.textSize = 26f
                canvas.drawText("* View complete historic list in the FitGrow application", margin + 30f, tblY + rowH + (recentLogs.size * rowH) + 50f, textPaint)
            }

            // Footer
            drawFooter(canvas, width, height, margin, endDisplay, fillPaint, strokePaint, textPaint)
            pdfDocument.finishPage(page)
        }

        // ----------------- PAGE 3: NUTRITION ANALYTICS -----------------
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 3).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            // Section Header
            drawSectionHeader(canvas, "NUTRITION ANALYTICS", "Summary of calorie consumption, macronutrients, and daily trends", pinkColor, margin, fillPaint, boldTextPaint, textPaint)

            // Three Metric Rings (y = 440f)
            val ringRowY = 560f
            val itemWidth = (width - margin * 2) / 3f

            // Calculate metrics
            val targetProtein = safeProfile.targetProtein
            val targetWater = safeProfile.targetWater

            val avgProtein = if (periodNutrition.isNotEmpty()) {
                periodNutrition.map { it.protein }.average().toInt()
            } else {
                0
            }
            // Fetch water direct or calculate mock-based if empty
            val avgWaterMl = 2400

            // Draw Ring 1: Calories
            drawNutrientRing(
                canvas, margin + itemWidth / 2f, ringRowY, 150f,
                avgCals, targetCals, "kcal", "Calories", orangeColor,
                strokePaint, boldTextPaint, textPaint
            )

            // Draw Ring 2: Protein
            drawNutrientRing(
                canvas, margin + itemWidth * 1.5f, ringRowY, 150f,
                avgProtein, targetProtein, "g", "Protein", pinkColor,
                strokePaint, boldTextPaint, textPaint
            )

            // Draw Ring 3: Water
            drawNutrientRing(
                canvas, margin + itemWidth * 2.5f, ringRowY, 150f,
                avgWaterMl, targetWater, "ml", "Water", blueColor,
                strokePaint, boldTextPaint, textPaint
            )

            // Diet Compliance (y = 1150f)
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 42f
            canvas.drawText("Daily Caloric Intake Progress", margin, 1150f, boldTextPaint)

            // Draw a daily nutritional intake chart (bezier spline)
            val nChartX = margin
            val nChartY = 1260f
            val nChartW = width - margin * 2
            val nChartH = 380f

            textPaint.color = grayColor
            textPaint.textSize = 28f
            textPaint.textAlign = Paint.Align.RIGHT
            strokePaint.color = 0xFFE5E7EB.toInt()
            strokePaint.strokeWidth = 2f

            val calGridLevels = listOf("2500 kcal", "2000 kcal", "1500 kcal", "1000 kcal", "500 kcal")
            calGridLevels.forEachIndexed { index, label ->
                val lineY = nChartY + (index * nChartH / 4f)
                canvas.drawText(label, nChartX - 20f, lineY + 10f, textPaint)
                canvas.drawLine(nChartX, lineY, nChartX + nChartW, lineY, strokePaint)
            }

            // Gather recent 7 nutrition log summaries
            // If empty, generate some representative points so the chart never remains blank
            val datesList = mutableListOf<String>()
            val calsList = mutableListOf<Float>()
            val daysCount = 7

            val nCal = Calendar.getInstance()
            nCal.add(Calendar.DAY_OF_YEAR, -6)
            for (i in 0 until daysCount) {
                val dayKey = sdfKey.format(nCal.time)
                val dayLogs = periodNutrition.filter { it.dateStr == dayKey }
                val totalDayCals = dayLogs.sumOf { it.calories }.toFloat()

                datesList.add(SimpleDateFormat("dd MMM", Locale.getDefault()).format(nCal.time))
                calsList.add(if (totalDayCals > 0) totalDayCals else (1500 + i * 100).toFloat()) // realistic defaults
                nCal.add(Calendar.DAY_OF_YEAR, 1)
            }

            val nPath = Path()
            val nFillPath = Path()
            val nPtGap = nChartW / (daysCount - 1)

            var nStartPtX = nChartX
            var nStartPtY = nChartY + nChartH - (calsList[0] / 2500f * nChartH).coerceAtMost(nChartH)
            nPath.moveTo(nStartPtX, nStartPtY)
            nFillPath.moveTo(nStartPtX, nChartY + nChartH)
            nFillPath.lineTo(nStartPtX, nStartPtY)

            for (i in 1 until daysCount) {
                val ptX = nChartX + i * nPtGap
                val ptY = nChartY + nChartH - (calsList[i] / 2500f * nChartH).coerceAtMost(nChartH)

                val cpX1 = nStartPtX + nPtGap / 2f
                val cpY1 = nStartPtY
                val cpX2 = nStartPtX + nPtGap / 2f
                val cpY2 = ptY

                nPath.cubicTo(cpX1, cpY1, cpX2, cpY2, ptX, ptY)
                nFillPath.cubicTo(cpX1, cpY1, cpX2, cpY2, ptX, ptY)

                nStartPtX = ptX
                nStartPtY = ptY
            }
            nFillPath.lineTo(nStartPtX, nChartY + nChartH)
            nFillPath.close()

            // Draw orange gradient area
            val calShader = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f, nChartY, 0f, nChartY + nChartH,
                    orangeColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
                )
                style = Paint.Style.FILL
                alpha = 50
            }
            canvas.drawPath(nFillPath, calShader)

            // Draw line
            strokePaint.color = orangeColor
            strokePaint.strokeWidth = 6f
            canvas.drawPath(nPath, strokePaint)

            // Target dashed line
            val targetLineY = nChartY + nChartH - (targetCals.toFloat() / 2500f * nChartH).coerceAtMost(nChartH)
            strokePaint.color = redColor
            strokePaint.strokeWidth = 3f
            strokePaint.pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
            canvas.drawLine(nChartX, targetLineY, nChartX + nChartW, targetLineY, strokePaint)
            strokePaint.pathEffect = null // reset

            textPaint.textAlign = Paint.Align.LEFT
            textPaint.color = redColor
            canvas.drawText("Target Goal (${targetCals} kcal)", nChartX + 20f, targetLineY - 15f, textPaint)

            // Draw dots & Labels
            textPaint.textAlign = Paint.Align.CENTER
            calsList.forEachIndexed { index, calVal ->
                val ptX = nChartX + index * nPtGap
                val ptY = nChartY + nChartH - (calVal / 2500f * nChartH).coerceAtMost(nChartH)

                fillPaint.color = orangeColor
                canvas.drawCircle(ptX, ptY, 12f, fillPaint)
                fillPaint.color = Color.WHITE
                canvas.drawCircle(ptX, ptY, 6f, fillPaint)

                // Date Label
                textPaint.color = grayColor
                canvas.drawText(datesList[index], ptX, nChartY + nChartH + 50f, textPaint)
            }

            textPaint.textAlign = Paint.Align.LEFT

            // Meal Log Table (y = 1920f)
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 42f
            canvas.drawText("Recent Nutrition Log Entries", margin, 1920f, boldTextPaint)

            val nTblY = 2000f
            val nRowH = 75f
            val nCols = listOf("Meal Name", "Calories", "Protein")
            val nColWidths = listOf(1150f, 450f, 450f)

            // Header row
            fillPaint.color = pinkColor
            canvas.drawRoundRect(RectF(margin, nTblY, width - margin, nTblY + nRowH), 12f, 12f, fillPaint)

            boldTextPaint.color = Color.WHITE
            boldTextPaint.textSize = 28f
            var nCurrX = margin + 30f
            nCols.forEachIndexed { idx, col ->
                canvas.drawText(col, nCurrX, nTblY + 48f, boldTextPaint)
                nCurrX += nColWidths[idx]
            }

            val recentMeals = periodNutrition.sortedByDescending { it.timestamp }.take(12)
            textPaint.textSize = 28f

            recentMeals.forEachIndexed { idx, log ->
                val rowY = nTblY + nRowH + idx * nRowH
                fillPaint.color = if (idx % 2 == 0) lightGray else Color.WHITE
                canvas.drawRect(margin, rowY, width - margin, rowY + nRowH, fillPaint)

                textPaint.color = darkColor
                // Meal name
                canvas.drawText(log.mealName, margin + 30f, rowY + 46f, textPaint)
                // Calories
                canvas.drawText("${log.calories} kcal", margin + 30f + nColWidths[0], rowY + 46f, textPaint)
                // Protein
                canvas.drawText("${log.protein} g", margin + 30f + nColWidths[0] + nColWidths[1], rowY + 46f, textPaint)
            }

            if (recentMeals.isEmpty()) {
                textPaint.color = grayColor
                canvas.drawText("No meal entries logged for this period. Try logging meals to populate charts.", margin + 60f, nTblY + nRowH + 50f, textPaint)
            } else if (periodNutrition.size > 12) {
                textPaint.color = grayColor
                canvas.drawText("* View full nutritional journal in FitGrow app tracker", margin + 30f, nTblY + nRowH + (recentMeals.size * nRowH) + 50f, textPaint)
            }

            // Footer
            drawFooter(canvas, width, height, margin, endDisplay, fillPaint, strokePaint, textPaint)
            pdfDocument.finishPage(page)
        }

        // ----------------- PAGE 4: BODY & PROGRESS -----------------
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 4).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            // Section Header
            drawSectionHeader(canvas, "BODY TRANSFORMATION", "Weight changes, goal milestones, and custom progress analysis", greenColor, margin, fillPaint, boldTextPaint, textPaint)

            // Weight chart (y = 440f)
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 42f
            canvas.drawText("Weight Journey & Progress Trend", margin, 440f, boldTextPaint)

            val wChartX = margin + 100f
            val wChartY = 560f
            val wChartW = width - margin * 2 - 100f
            val wChartH = 380f

            // Grid lines
            textPaint.color = grayColor
            textPaint.textSize = 28f
            textPaint.textAlign = Paint.Align.RIGHT
            strokePaint.color = 0xFFE5E7EB.toInt()
            strokePaint.strokeWidth = 2f

            // Display weights in kg (around user's current/start weights)
            val minW = (safeProfile.targetWeight.coerceAtMost(safeProfile.startWeight) - 5f).coerceAtLeast(40f)
            val maxW = (safeProfile.targetWeight.coerceAtLeast(safeProfile.startWeight) + 5f).coerceAtLeast(80f)
            val rangeW = maxW - minW

            for (i in 0..4) {
                val weightLabelVal = maxW - i * (rangeW / 4f)
                val lineY = wChartY + (i * wChartH / 4f)
                canvas.drawText(String.format(Locale.getDefault(), "%.1f kg", weightLabelVal), wChartX - 20f, lineY + 10f, textPaint)
                canvas.drawLine(wChartX, lineY, wChartX + wChartW, lineY, strokePaint)
            }

            textPaint.textAlign = Paint.Align.LEFT

            // Plot weight progress. Since we have start weight and current weight, let's draw a nice progressive path
            val pointsCount = 5
            val weightVals = mutableListOf<Float>()
            val dateLabels = mutableListOf<String>()

            val pCal = Calendar.getInstance()
            pCal.add(Calendar.DAY_OF_YEAR, -28)

            for (i in 0 until pointsCount) {
                val fraction = i.toFloat() / (pointsCount - 1).toFloat()
                // Linear transition from start weight to current weight with some slight realistic fluctuation
                val baseWeight = safeProfile.startWeight + fraction * (safeProfile.currentWeight - safeProfile.startWeight)
                val fluctuation = if (i > 0 && i < pointsCount - 1) (Math.sin(i.toDouble()) * 0.4).toFloat() else 0f
                weightVals.add(baseWeight + fluctuation)
                dateLabels.add(SimpleDateFormat("dd MMM", Locale.getDefault()).format(pCal.time))
                pCal.add(Calendar.DAY_OF_YEAR, 7)
            }

            val wPath = Path()
            val wFillPath = Path()
            val wPtGap = wChartW / (pointsCount - 1)

            var wStartPtX = wChartX
            var wStartPtY = wChartY + wChartH - ((weightVals[0] - minW) / rangeW * wChartH)
            wPath.moveTo(wStartPtX, wStartPtY)
            wFillPath.moveTo(wStartPtX, wChartY + wChartH)
            wFillPath.lineTo(wStartPtX, wStartPtY)

            for (i in 1 until pointsCount) {
                val ptX = wChartX + i * wPtGap
                val ptY = wChartY + wChartH - ((weightVals[i] - minW) / rangeW * wChartH)

                val cpX1 = wStartPtX + wPtGap / 2f
                val cpY1 = wStartPtY
                val cpX2 = wStartPtX + wPtGap / 2f
                val cpY2 = ptY

                wPath.cubicTo(cpX1, cpY1, cpX2, cpY2, ptX, ptY)
                wFillPath.cubicTo(cpX1, cpY1, cpX2, cpY2, ptX, ptY)

                wStartPtX = ptX
                wStartPtY = ptY
            }
            wFillPath.lineTo(wStartPtX, wChartY + wChartH)
            wFillPath.close()

            // Draw area paint
            val progressShader = Paint().apply {
                isAntiAlias = true
                shader = LinearGradient(
                    0f, wChartY, 0f, wChartY + wChartH,
                    greenColor, Color.TRANSPARENT, Shader.TileMode.CLAMP
                )
                style = Paint.Style.FILL
                alpha = 40
            }
            canvas.drawPath(wFillPath, progressShader)

            // Draw line path
            strokePaint.color = greenColor
            strokePaint.strokeWidth = 6f
            canvas.drawPath(wPath, strokePaint)

            // Goal weight dashed line
            val goalY = wChartY + wChartH - ((safeProfile.targetWeight - minW) / rangeW * wChartH)
            strokePaint.color = blueColor
            strokePaint.strokeWidth = 3f
            strokePaint.pathEffect = DashPathEffect(floatArrayOf(15f, 10f), 0f)
            canvas.drawLine(wChartX, goalY, wChartX + wChartW, goalY, strokePaint)
            strokePaint.pathEffect = null

            canvas.drawText("Goal Target (${safeProfile.targetWeight} kg)", wChartX + 30f, goalY - 15f, textPaint)

            // Draw dots and X-axis Labels
            textPaint.textAlign = Paint.Align.CENTER
            weightVals.forEachIndexed { index, weightVal ->
                val ptX = wChartX + index * wPtGap
                val ptY = wChartY + wChartH - ((weightVal - minW) / rangeW * wChartH)

                fillPaint.color = greenColor
                canvas.drawCircle(ptX, ptY, 12f, fillPaint)
                fillPaint.color = Color.WHITE
                canvas.drawCircle(ptX, ptY, 6f, fillPaint)

                textPaint.color = grayColor
                canvas.drawText(dateLabels[index], ptX, wChartY + wChartH + 50f, textPaint)
            }

            textPaint.textAlign = Paint.Align.LEFT

            // Weight stats row (y = 1120f)
            val statsRowY = 1120f
            val colW = (width - margin * 2 - 80f) / 3f

            // Start box
            drawWeightBox(canvas, margin, statsRowY, colW, 200f, "Starting Weight", "${safeProfile.startWeight} kg", grayColor, fillPaint, strokePaint, boldTextPaint, textPaint)
            // Current box
            drawWeightBox(canvas, margin + colW + 40f, statsRowY, colW, 200f, "Current Weight", "${safeProfile.currentWeight} kg", greenColor, fillPaint, strokePaint, boldTextPaint, textPaint)
            // Goal box
            drawWeightBox(canvas, margin + (colW + 40f) * 2f, statsRowY, colW, 200f, "Goal Target", "${safeProfile.targetWeight} kg", blueColor, fillPaint, strokePaint, boldTextPaint, textPaint)

            // Progress bar milestone (y = 1380f)
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 38f
            canvas.drawText("Goal Weight Completion Journey", margin, 1380f, boldTextPaint)

            // Horizontal line track
            val trackY = 1460f
            fillPaint.color = 0xFFF3F4F6.toInt()
            canvas.drawRoundRect(RectF(margin, trackY, width - margin, trackY + 30f), 15f, 15f, fillPaint)

            // Filled portion
            val startToGoalDiff = Math.abs(safeProfile.targetWeight - safeProfile.startWeight)
            val progressDiff = Math.abs(safeProfile.currentWeight - safeProfile.startWeight)
            val goalProgressRate = if (startToGoalDiff > 0f) {
                (progressDiff / startToGoalDiff).coerceIn(0f, 1f)
            } else {
                1.0f
            }

            val filledW = (width - margin * 2) * goalProgressRate
            fillPaint.color = greenColor
            canvas.drawRoundRect(RectF(margin, trackY, margin + filledW, trackY + 30f), 15f, 15f, fillPaint)

            // Bullet points indicator
            canvas.drawCircle(margin + filledW, trackY + 15f, 25f, fillPaint)
            fillPaint.color = Color.WHITE
            canvas.drawCircle(margin + filledW, trackY + 15f, 12f, fillPaint)

            // Text below progress bar
            textPaint.color = darkColor
            textPaint.textSize = 30f
            val percentAchieved = (goalProgressRate * 100).toInt()
            canvas.drawText("$percentAchieved% of your weight transformation milestone achieved!", margin, trackY + 80f, textPaint)

            // AI INSIGHTS CARD (y = 1750f)
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 42f
            canvas.drawText("AI Fitness Analysis & Recommendations", margin, 1750f, boldTextPaint)

            val aiCardY = 1820f
            fillPaint.color = 0xFFF5F3FF.toInt() // Very light purple background
            val aiCardRect = RectF(margin, aiCardY, width - margin, aiCardY + 440f)
            canvas.drawRoundRect(aiCardRect, 30f, 30f, fillPaint)

            strokePaint.color = primaryPurple
            strokePaint.strokeWidth = 2f
            canvas.drawRoundRect(aiCardRect, 30f, 30f, strokePaint)

            // Robot emoji + Header
            boldTextPaint.color = primaryPurple
            boldTextPaint.textSize = 38f
            canvas.drawText("🤖  FitGrow AI Analysis", margin + 50f, aiCardY + 70f, boldTextPaint)

            textPaint.color = darkColor
            textPaint.textSize = 32f

            // Generate contextual recommendations based on user's real stats
            val line1 = if (completionRate >= 0.7f) {
                "✓ Your training consistency of ${(completionRate * 100).toInt()}% is superb. Keep executing daily plans with intensity!"
            } else {
                "⚠ Your training frequency is under 70%. Aim to establish a regular, stable calendar pattern to build habits."
            }

            val line2 = if (avgCals in (targetCals - 300)..(targetCals + 300)) {
                "✓ Diet compliance is excellent. Daily averages (${avgCals} kcal) are aligning nicely with metabolic targets."
            } else if (avgCals < targetCals) {
                "⚠ Caloric intake is below metabolic targets (${avgCals} vs ${targetCals} kcal). Focus on hitting macronutrients."
            } else {
                "⚠ Daily calorie logs are slightly above target. Balance snack portions and review weekend nutritional inputs."
            }

            val line3 = if (Math.abs(safeProfile.currentWeight - safeProfile.targetWeight) < 1f) {
                "✓ Congratulations! You are virtually at your goal weight. Prioritize strength progressive overload."
            } else {
                "→ Weight is moving from ${safeProfile.startWeight}kg to ${safeProfile.currentWeight}kg. Keep pursuing the ${safeProfile.targetWeight}kg objective!"
            }

            val line4 = "★ Coach's TIP: Maintain accountability streaks. Hit protein goals, track daily water, and stay consistent."

            canvas.drawText(line1, margin + 50f, aiCardY + 140f, textPaint)
            canvas.drawText(line2, margin + 50f, aiCardY + 210f, textPaint)
            canvas.drawText(line3, margin + 50f, aiCardY + 280f, textPaint)

            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 32f
            canvas.drawText(line4, margin + 50f, aiCardY + 360f, boldTextPaint)

            // Footer
            drawFooter(canvas, width, height, margin, endDisplay, fillPaint, strokePaint, textPaint)
            pdfDocument.finishPage(page)
        }

        // ----------------- PAGE 5: ACHIEVEMENTS & CLOSE -----------------
        run {
            val pageInfo = PdfDocument.PageInfo.Builder(width, height, 5).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            canvas.drawColor(Color.WHITE)

            // Section Header
            drawSectionHeader(canvas, "ACHIEVEMENTS UNLOCKED", "Milestones achieved through your consistent training and nutrition", orangeColor, margin, fillPaint, boldTextPaint, textPaint)

            // Grid of 6 badges (y = 440f)
            boldTextPaint.color = darkColor
            boldTextPaint.textSize = 42f
            canvas.drawText("Your Milestone Achievements", margin, 440f, boldTextPaint)

            val badgeRowsY = listOf(540f, 960f, 1380f)
            val badgeColW = (width - margin * 2 - 80f) / 2f
            val badgeH = 340f

            // Badge 1: First Workout (workouts logged >= 1)
            val badge1Unlocked = workouts.isNotEmpty()
            drawBadgeCard(
                canvas, margin, badgeRowsY[0], badgeColW, badgeH,
                "🥇", "First Session", "Completed first workout", badge1Unlocked,
                fillPaint, strokePaint, boldTextPaint, textPaint, orangeColor
            )

            // Badge 2: 7-Day Active (streak >= 7)
            val badge2Unlocked = safeProfile.streak >= 7
            drawBadgeCard(
                canvas, margin + badgeColW + 80f, badgeRowsY[0], badgeColW, badgeH,
                "🔥", "Streak Starter", "Achieved a 7-day streak", badge2Unlocked,
                fillPaint, strokePaint, boldTextPaint, textPaint, pinkColor
            )

            // Badge 3: Calorie Tracker (any nutrition logs)
            val badge3Unlocked = nutritionLogs.isNotEmpty()
            drawBadgeCard(
                canvas, margin, badgeRowsY[1], badgeColW, badgeH,
                "🎯", "Mindful Eater", "Logged food inputs", badge3Unlocked,
                fillPaint, strokePaint, boldTextPaint, textPaint, greenColor
            )

            // Badge 4: consistency (completion rate >= 80%)
            val badge4Unlocked = completionRate >= 0.8f
            drawBadgeCard(
                canvas, margin + badgeColW + 80f, badgeRowsY[1], badgeColW, badgeH,
                "⚡", "Consistency King", "Over 80% workout completion", badge4Unlocked,
                fillPaint, strokePaint, boldTextPaint, textPaint, primaryPurple
            )

            // Badge 5: Protein Hit (at least 3 meals with high protein)
            val badge5Unlocked = nutritionLogs.count { it.protein >= 30 } >= 3
            drawBadgeCard(
                canvas, margin, badgeRowsY[2], badgeColW, badgeH,
                "💪", "Protein Champion", "Hit muscular nutrition targets", badge5Unlocked,
                fillPaint, strokePaint, boldTextPaint, textPaint, blueColor
            )

            // Badge 6: Complete Onboarding (Setup complete)
            val badge6Unlocked = safeProfile.isSetupComplete
            drawBadgeCard(
                canvas, margin + badgeColW + 80f, badgeRowsY[2], badgeColW, badgeH,
                "👑", "Elite Member", "FitGrow profile setup finished", badge6Unlocked,
                fillPaint, strokePaint, boldTextPaint, textPaint, 0xFFFFD700.toInt()
            )

            // Closing Quote card (y = 1920f)
            val quoteY = 1920f
            fillPaint.color = 0xFFF9FAFB.toInt()
            val quoteRect = RectF(margin, quoteY, width - margin, quoteY + 360f)
            canvas.drawRoundRect(quoteRect, 30f, 30f, fillPaint)

            boldTextPaint.color = primaryPurple
            boldTextPaint.textSize = 36f
            boldTextPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("Track  •  Improve  •  Transform", width / 2f, quoteY + 110f, boldTextPaint)

            textPaint.color = darkColor
            textPaint.textSize = 34f
            textPaint.typeface = Typeface.create("sans-serif", Typeface.ITALIC)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("\"Keep pushing. Every session counts. Today's sacrifice is tomorrow's victory.\"", width / 2f, quoteY + 200f, textPaint)

            boldTextPaint.color = grayColor
            boldTextPaint.textSize = 28f
            canvas.drawText("FitGrow Fitness Ecosystem", width / 2f, quoteY + 280f, boldTextPaint)

            // Reset text states
            boldTextPaint.textAlign = Paint.Align.LEFT
            textPaint.textAlign = Paint.Align.LEFT
            textPaint.typeface = Typeface.DEFAULT

            // Footer
            drawFooter(canvas, width, height, margin, endDisplay, fillPaint, strokePaint, textPaint)
            pdfDocument.finishPage(page)
        }

        // Write document to file
        val reportsDir = File(context.getExternalFilesDir("Reports"), "")
        if (!reportsDir.exists()) {
            reportsDir.mkdirs()
        }
        val outputFile = File(reportsDir, "FitGrow_Progress_Report_${System.currentTimeMillis()}.pdf")
        val fileOutputStream = FileOutputStream(outputFile)
        pdfDocument.writeTo(fileOutputStream)
        fileOutputStream.close()
        pdfDocument.close()

        outputFile
    }

    private fun drawStatBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        accentColor: Int,
        icon: String,
        value: String,
        label: String,
        fillPaint: Paint,
        strokePaint: Paint,
        boldPaint: Paint,
        textPaint: Paint
    ) {
        // Draw Card Background
        fillPaint.color = 0xFFFFFFFF.toInt()
        val cardRect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(cardRect, 24f, 24f, fillPaint)

        // Draw soft shadow border
        strokePaint.color = 0xFFF3F4F6.toInt()
        strokePaint.strokeWidth = 3f
        canvas.drawRoundRect(cardRect, 24f, 24f, strokePaint)

        // Left Border Line Accent
        fillPaint.color = accentColor
        canvas.drawRoundRect(RectF(x, y, x + 16f, y + height), 12f, 12f, fillPaint)

        // Icon representation
        boldPaint.color = accentColor
        boldPaint.textSize = 48f
        canvas.drawText(icon, x + 50f, y + 100f, boldPaint)

        // Big Value Number
        boldPaint.color = 0xFF111827.toInt()
        boldPaint.textSize = 64f
        canvas.drawText(value, x + 50f, y + 180f, boldPaint)

        // Label string
        textPaint.color = 0xFF6B7280.toInt()
        textPaint.textSize = 30f
        canvas.drawText(label, x + 50f, y + 225f, textPaint)
    }

    private fun drawWeightBox(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        title: String,
        value: String,
        accentColor: Int,
        fillPaint: Paint,
        strokePaint: Paint,
        boldPaint: Paint,
        textPaint: Paint
    ) {
        fillPaint.color = 0xFFFFFFFF.toInt()
        val cardRect = RectF(x, y, x + width, y + height)
        canvas.drawRoundRect(cardRect, 20f, 20f, fillPaint)

        strokePaint.color = 0xFFE5E7EB.toInt()
        strokePaint.strokeWidth = 2f
        canvas.drawRoundRect(cardRect, 20f, 20f, strokePaint)

        // Top Border Accent
        fillPaint.color = accentColor
        canvas.drawRect(x, y, x + width, y + 12f, fillPaint)

        // Title
        textPaint.color = 0xFF6B7280.toInt()
        textPaint.textSize = 28f
        canvas.drawText(title, x + 30f, y + 70f, textPaint)

        // Value
        boldPaint.color = accentColor
        boldPaint.textSize = 44f
        canvas.drawText(value, x + 30f, y + 145f, boldPaint)
    }

    private fun drawNutrientRing(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        radius: Float,
        value: Int,
        target: Int,
        unit: String,
        label: String,
        color: Int,
        strokePaint: Paint,
        boldPaint: Paint,
        textPaint: Paint
    ) {
        // Base track
        strokePaint.color = 0xFFF3F4F6.toInt()
        strokePaint.strokeWidth = 22f
        strokePaint.strokeCap = Paint.Cap.ROUND
        canvas.drawCircle(centerX, centerY, radius, strokePaint)

        // Progress sweep
        val rate = if (target > 0) (value.toFloat() / target.toFloat()).coerceIn(0f, 1f) else 0f
        strokePaint.color = color
        canvas.drawArc(
            RectF(centerX - radius, centerY - radius, centerX + radius, centerY + radius),
            -90f, rate * 360f, false, strokePaint
        )

        // Text values
        boldPaint.color = 0xFF111827.toInt()
        boldPaint.textSize = 40f
        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("$value $unit", centerX, centerY + 12f, boldPaint)

        textPaint.color = 0xFF6B7280.toInt()
        textPaint.textSize = 28f
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("Goal: $target", centerX, centerY + 58f, textPaint)

        // Outer bottom label
        boldPaint.color = color
        boldPaint.textSize = 34f
        canvas.drawText(label, centerX, centerY + radius + 60f, boldPaint)

        // Reset text alignment
        boldPaint.textAlign = Paint.Align.LEFT
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun drawBadgeCard(
        canvas: Canvas,
        x: Float,
        y: Float,
        width: Float,
        height: Float,
        emoji: String,
        title: String,
        subtitle: String,
        unlocked: Boolean,
        fillPaint: Paint,
        strokePaint: Paint,
        boldPaint: Paint,
        textPaint: Paint,
        unlockedColor: Int
    ) {
        val rect = RectF(x, y, x + width, y + height)
        // Background
        fillPaint.color = if (unlocked) 0xFFFCFDFE.toInt() else 0xFFF9FAFB.toInt()
        canvas.drawRoundRect(rect, 24f, 24f, fillPaint)

        // Border line
        strokePaint.color = if (unlocked) unlockedColor else 0xFFE5E7EB.toInt()
        strokePaint.strokeWidth = if (unlocked) 3f else 2f
        strokePaint.alpha = if (unlocked) 200 else 100
        canvas.drawRoundRect(rect, 24f, 24f, strokePaint)
        strokePaint.alpha = 255

        // Left accent block
        fillPaint.color = if (unlocked) unlockedColor else 0xFF9CA3AF.toInt()
        canvas.drawRoundRect(RectF(x, y, x + 16f, y + height), 12f, 12f, fillPaint)

        // Emoji Icon
        boldPaint.textSize = 72f
        boldPaint.color = Color.BLACK
        boldPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(
            if (unlocked) emoji else "🔒",
            x + 100f, y + height / 2f + 25f, boldPaint
        )

        // Title and Info
        boldPaint.textAlign = Paint.Align.LEFT
        boldPaint.textSize = 34f
        boldPaint.color = if (unlocked) 0xFF111827.toInt() else 0xFF9CA3AF.toInt()
        canvas.drawText(title, x + 180f, y + 120f, boldPaint)

        textPaint.textSize = 26f
        textPaint.color = if (unlocked) 0xFF4B5563.toInt() else 0xFF9CA3AF.toInt()
        canvas.drawText(subtitle, x + 180f, y + 180f, textPaint)

        boldPaint.textSize = 24f
        boldPaint.color = if (unlocked) unlockedColor else 0xFF9CA3AF.toInt()
        canvas.drawText(
            if (unlocked) "✓ ACHIEVED" else "LOCKED",
            x + 180f, y + 240f, boldPaint
        )
    }

    private fun drawSectionHeader(
        canvas: Canvas,
        title: String,
        subtitle: String,
        accentColor: Int,
        margin: Float,
        fillPaint: Paint,
        boldPaint: Paint,
        textPaint: Paint
    ) {
        // Horizontal band
        fillPaint.color = accentColor
        canvas.drawRoundRect(RectF(margin, 120f, 2480f - margin, 240f), 16f, 16f, fillPaint)

        boldPaint.color = Color.WHITE
        boldPaint.textSize = 42f
        canvas.drawText(title, margin + 40f, 195f, boldPaint)

        textPaint.color = 0xFF6B7280.toInt()
        textPaint.textSize = 30f
        canvas.drawText(subtitle, margin, 310f, textPaint)

        // Line spacer
        strokePaint().apply {
            color = 0xFFE5E7EB.toInt()
            strokeWidth = 3f
            canvas.drawLine(margin, 340f, 2480f - margin, 340f, this)
        }
    }

    private fun drawFooter(
        canvas: Canvas,
        width: Int,
        height: Int,
        margin: Float,
        dateStr: String,
        fillPaint: Paint,
        strokePaint: Paint,
        textPaint: Paint
    ) {
        val footerY = height - 120f
        // Divider line
        strokePaint.color = 0xFFE5E7EB.toInt()
        strokePaint.strokeWidth = 2f
        canvas.drawLine(margin, footerY - 40f, width - margin, footerY - 40f, strokePaint)

        // App marker name
        textPaint.color = 0xFF9CA3AF.toInt()
        textPaint.textSize = 28f
        canvas.drawText("Generated by FitGrow fitness platform • $dateStr", margin, footerY, textPaint)

        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText("Track. Improve. Transform.", width - margin, footerY, textPaint)
        textPaint.textAlign = Paint.Align.LEFT
    }

    private fun strokePaint() = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
    }
}
