package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.Zinc800
import com.example.ui.theme.Zinc900
import com.example.ui.theme.Indigo500
import com.example.ui.theme.Zinc100
import com.example.ui.theme.Zinc400
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AttendanceCalendar(
    attendanceLogs: Map<String, Boolean>,
    onDayClick: (String, Boolean) -> Unit
) {
    val calendar = Calendar.getInstance()
    val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentMonthYear = SimpleDateFormat("MMMM yyyy", Locale.getDefault()).format(calendar.time).uppercase()
    
    val currentYearFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(calendar.time)

    var showDialog by remember { mutableStateOf<String?>(null) } // holds selected dateStr

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Zinc900, RoundedCornerShape(16.dp))
            .border(1.dp, Zinc800, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("ATTENDANCE TRACKER", color = Zinc400, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            Text(currentMonthYear, color = Indigo500, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.height(16.dp))
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            listOf("S", "M", "T", "W", "T", "F", "S").forEach {
                Text(it, color = Zinc400, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        LazyVerticalGrid(
            columns = GridCells.Fixed(7),
            modifier = Modifier.heightIn(max = 250.dp),
            contentPadding = PaddingValues(bottom = 8.dp)
        ) {
            items(daysInMonth) { dayIndex ->
                val day = dayIndex + 1
                val dayStr = String.format("%02d", day)
                val dateStr = "$currentYearFormat-$dayStr"

                val isDone = attendanceLogs[dateStr]
                
                val bgColor = when(isDone) {
                    true -> Color(0xFF1B5E20).copy(alpha = 0.5f)
                    false -> Color(0xFFB71C1C).copy(alpha = 0.5f)
                    else -> Zinc800.copy(alpha = 0.5f)
                }
                
                val borderColor = when(isDone) {
                    true -> Color.Green.copy(alpha = 0.5f)
                    false -> Color.Red.copy(alpha = 0.5f)
                    else -> Color.Transparent
                }

                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .aspectRatio(1f)
                        .background(bgColor, RoundedCornerShape(8.dp))
                        .border(1.dp, borderColor, RoundedCornerShape(8.dp))
                        .clickable {
                            showDialog = dateStr
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(day.toString(), color = Zinc100, fontSize = 14.sp)
                    if (isDone == false) {
                        Text("X", color = Color.Red.copy(alpha = 0.7f), fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomCenter))
                    } else if (isDone == true) {
                        Box(modifier = Modifier.size(4.dp).background(Color.Green, androidx.compose.foundation.shape.CircleShape).align(Alignment.BottomCenter).padding(bottom=2.dp))
                    }
                }
            }
        }
    }

    if (showDialog != null) {
        val dateSelected = showDialog!!
        AlertDialog(
            onDismissRequest = { showDialog = null },
            title = { Text("Log Workout for $dateSelected") },
            text = { Text("Did you hit the gym or miss it?") },
            confirmButton = {
                TextButton(onClick = {
                    onDayClick(dateSelected, true)
                    showDialog = null
                }) {
                    Text("DONE", color = Color.Green)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    onDayClick(dateSelected, false)
                    showDialog = null
                }) {
                    Text("MISSED", color = Color.Red)
                }
            },
            containerColor = Zinc900,
            titleContentColor = Zinc100,
            textContentColor = Zinc400
        )
    }
}
