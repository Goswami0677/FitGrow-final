package com.example.ui.screens.progress

import android.content.Context
import android.graphics.*
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*

object MilestoneImageGenerator {

    /**
     * Generates a beautiful 1080x1080 share card for a milestone.
     * Returns the File pointing to the generated PNG image in the cache directory.
     */
    fun generateMilestoneCard(
        context: Context,
        title: String,
        subtitle: String,
        iconTxt: String,
        badgeColorHex: Int,
        statLabel1: String,
        statValue1: String,
        statLabel2: String,
        statValue2: String,
        coachMessage: String
    ): File {
        val size = 1080
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Colors
        val bgDark = 0xFF0D0E12.toInt()
        val bgBottom = 0xFF14151B.toInt()
        val borderPurple = 0xFF2D2254.toInt()
        val accentColor = badgeColorHex
        val textWhite = 0xFFFFFFFF.toInt()
        val textGray = 0xFFA0A0A5.toInt()
        val glassBg = 0x1F222328 // semi-transparent

        // 1. Draw modern dark vertical gradient background
        val bgPaint = Paint().apply {
            isAntiAlias = true
            shader = LinearGradient(
                0f, 0f, 0f, size.toFloat(),
                bgDark, bgBottom, Shader.TileMode.CLAMP
            )
        }
        canvas.drawRect(0f, 0f, size.toFloat(), size.toFloat(), bgPaint)

        // 2. Draw subtle glowing neon radial backgrounds to create visual richness
        val glowPaint = Paint().apply {
            isAntiAlias = true
            shader = RadialGradient(
                size.toFloat() / 2f, size.toFloat() / 2f, 500f,
                accentColor and 0x33FFFFFF.toInt(), // 20% opacity
                Color.TRANSPARENT,
                Shader.TileMode.CLAMP
            )
        }
        canvas.drawCircle(size / 2f, size / 2f, 500f, glowPaint)

        // 3. Draw dual accent lines / outer border with a sleek futuristic framing
        val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 12f
            color = accentColor
        }
        val innerMargin = 40f
        canvas.drawRoundRect(
            innerMargin, innerMargin,
            size - innerMargin, size - innerMargin,
            48f, 48f, borderPaint
        )

        // Subtle secondary thin inner border
        borderPaint.apply {
            strokeWidth = 2f
            color = borderPurple
        }
        canvas.drawRoundRect(
            innerMargin + 20f, innerMargin + 20f,
            size - innerMargin - 20f, size - innerMargin - 20f,
            36f, 36f, borderPaint
        )

        // 4. Branding at Top
        val brandPaint = Paint().apply {
            isAntiAlias = true
            color = textWhite
            textSize = 28f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            letterSpacing = 0.25f
        }
        canvas.drawText("⚡ FITGROW AI COACH", 100f, 110f, brandPaint)

        val dateStr = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date())
        val datePaint = Paint().apply {
            isAntiAlias = true
            color = textGray
            textSize = 24f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val dateWidth = datePaint.measureText(dateStr)
        canvas.drawText(dateStr, size - 100f - dateWidth, 110f, datePaint)

        // 5. Centered Milestone Circle Badge
        val badgeCenterX = size / 2f
        val badgeCenterY = 360f
        val badgeRadius = 130f

        // Badge Glow shadow ring
        val ringGlowPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 20f
            color = accentColor and 0x22FFFFFF.toInt() // glow background
        }
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius + 15f, ringGlowPaint)

        // Inner glowing solid outline
        val badgeOutlinePaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 6f
            color = accentColor
        }
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius, badgeOutlinePaint)

        // Badge solid background
        val badgeBgPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = 0xFF1B1B22.toInt()
        }
        canvas.drawCircle(badgeCenterX, badgeCenterY, badgeRadius - 3f, badgeBgPaint)

        // Badge Emoji / Symbol in center
        val emojiPaint = Paint().apply {
            isAntiAlias = true
            textSize = 100f
            textAlign = Paint.Align.CENTER
        }
        // Handle Emoji vertically centered
        val fontMetrics = emojiPaint.fontMetrics
        val emojiY = badgeCenterY - (fontMetrics.ascent + fontMetrics.descent) / 2f
        canvas.drawText(iconTxt, badgeCenterX, emojiY, emojiPaint)

        // 6. Huge Title
        val titlePaint = Paint().apply {
            isAntiAlias = true
            color = textWhite
            textSize = 58f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.05f
        }
        canvas.drawText(title.uppercase(), size / 2f, 580f, titlePaint)

        // Subtitle/Detail
        val subPaint = Paint().apply {
            isAntiAlias = true
            color = textGray
            textSize = 30f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(subtitle, size / 2f, 630f, subPaint)

        // 7. Stats Panel Boxes (Grid at bottom)
        val statsY = 690f
        val boxWidth = 400f
        val boxHeight = 150f
        val boxCorner = 24f

        val boxPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = glassBg
        }
        val boxBorderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = 2f
            color = 0xFF222328.toInt()
        }

        // Left Stat Box
        val leftX = size / 2f - boxWidth - 20f
        canvas.drawRoundRect(leftX, statsY, leftX + boxWidth, statsY + boxHeight, boxCorner, boxCorner, boxPaint)
        canvas.drawRoundRect(leftX, statsY, leftX + boxWidth, statsY + boxHeight, boxCorner, boxCorner, boxBorderPaint)

        // Right Stat Box
        val rightX = size / 2f + 20f
        canvas.drawRoundRect(rightX, statsY, rightX + boxWidth, statsY + boxHeight, boxCorner, boxCorner, boxPaint)
        canvas.drawRoundRect(rightX, statsY, rightX + boxWidth, statsY + boxHeight, boxCorner, boxCorner, boxBorderPaint)

        // Left Stat Values
        val statTitlePaint = Paint().apply {
            isAntiAlias = true
            color = textGray
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            textAlign = Paint.Align.CENTER
        }
        val statValuePaint = Paint().apply {
            isAntiAlias = true
            color = accentColor
            textSize = 38f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
        }

        val boxCenterX1 = leftX + boxWidth / 2f
        canvas.drawText(statLabel1.uppercase(), boxCenterX1, statsY + 55f, statTitlePaint)
        canvas.drawText(statValue1, boxCenterX1, statsY + 110f, statValuePaint)

        val boxCenterX2 = rightX + boxWidth / 2f
        canvas.drawText(statLabel2.uppercase(), boxCenterX2, statsY + 55f, statTitlePaint)
        canvas.drawText(statValue2, boxCenterX2, statsY + 110f, statValuePaint)

        // 8. Coach Motivational Msg Bubble
        val msgY = 880f
        val msgPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFFD1D1D6.toInt()
            textSize = 26f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
            textAlign = Paint.Align.CENTER
        }
        
        // Wrap Coach message to fit nicely if long
        val wrappedLines = wrapText(coachMessage, msgPaint, size - 240)
        var currentY = msgY
        for (line in wrappedLines) {
            canvas.drawText(line, size / 2f, currentY, msgPaint)
            currentY += 36f
        }

        // 9. Footnote Branding
        val footerPaint = Paint().apply {
            isAntiAlias = true
            color = 0xFF4C4DFF.toInt() // primary brand color
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.1f
        }
        canvas.drawText("PROUD MEMBER OF THE #FITGROW CLUB 🏆", size / 2f, size - 80f, footerPaint)

        // Save Bitmap to a local file in Cache directory to share
        val milestonesDir = File(context.cacheDir, "milestones")
        if (!milestonesDir.exists()) {
            milestonesDir.mkdirs()
        }
        
        val safeTitleName = title.replace("\\s+".toRegex(), "_").lowercase()
        val imageFile = File(milestonesDir, "fitgrow_${safeTitleName}_${System.currentTimeMillis()}.png")
        
        FileOutputStream(imageFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        
        return imageFile
    }

    private fun wrapText(text: String, paint: Paint, maxWidth: Int): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = StringBuilder()

        for (word in words) {
            val testLine = if (currentLine.isEmpty()) word else "${currentLine} $word"
            val width = paint.measureText(testLine)
            if (width > maxWidth) {
                if (currentLine.isNotEmpty()) {
                    lines.add(currentLine.toString())
                }
                currentLine = StringBuilder(word)
            } else {
                currentLine.append(if (currentLine.isEmpty()) word else " $word")
            }
        }
        if (currentLine.isNotEmpty()) {
            lines.add(currentLine.toString())
        }
        return lines
    }
}
