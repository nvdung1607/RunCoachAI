package com.example.runcoach.presentation.screens

import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.runcoach.data.local.db.WorkoutEntity
import com.example.runcoach.presentation.MainViewModel
import com.example.runcoach.ui.theme.*
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val workoutsList by viewModel.workouts.collectAsState()
    val workoutsMap = remember(workoutsList) {
        workoutsList.associateBy { it.date }
    }

    var currentYearMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    val selectedWorkout = selectedDate?.let { workoutsMap[it.toString()] }
    var showWorkoutDetails by remember { mutableStateOf<WorkoutEntity?>(null) }
    var showLegendDialog by remember { mutableStateOf(false) }

    val isDark = isSystemInDarkTheme()
    val bgBrush = Brush.verticalGradient(
        colors = if (isDark)
            listOf(Color(0xFF0F0F10), Color(0xFF1E293B))
        else
            listOf(Color(0xFFF9FAFB), Color(0xFFEEF2FF))
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Text(
                text = "Lịch Tập Tháng",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Center
            )
            // Legend button
            IconButton(onClick = { showLegendDialog = true }) {
                Icon(Icons.Default.Info, contentDescription = "Legend", tint = MaterialTheme.colorScheme.primary)
            }
        }

        // Month navigation
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { currentYearMonth = currentYearMonth.minusMonths(1) }) {
                    Icon(Icons.Default.ChevronLeft, contentDescription = "Previous month", tint = MaterialTheme.colorScheme.primary)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = currentYearMonth.month.getDisplayName(TextStyle.FULL, Locale("vi")),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${currentYearMonth.year}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                IconButton(onClick = { currentYearMonth = currentYearMonth.plusMonths(1) }) {
                    Icon(Icons.Default.ChevronRight, contentDescription = "Next month", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Day-of-week headers (Mon-Sun)
        val dayHeaders = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            dayHeaders.forEach { day ->
                Text(
                    text = day,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (day == "CN") ColorWarning.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Calendar grid
        val daysInMonth = currentYearMonth.lengthOfMonth()
        val firstDayOfMonth = currentYearMonth.atDay(1)
        // Adjust: Monday = 0
        val firstDayOffset = (firstDayOfMonth.dayOfWeek.value - 1) % 7
        val totalCells = firstDayOffset + daysInMonth
        val rows = (totalCells + 6) / 7

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
        ) {
            for (row in 0 until rows) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (col in 0 until 7) {
                        val cellIndex = row * 7 + col
                        val dayNumber = cellIndex - firstDayOffset + 1
                        val isValidDay = dayNumber in 1..daysInMonth
                        val date = if (isValidDay) currentYearMonth.atDay(dayNumber) else null
                        val workout = date?.let { workoutsMap[it.toString()] }
                        val isToday = date == LocalDate.now()
                        val isSelected = date != null && date == selectedDate

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 72.dp)
                                .padding(2.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    when {
                                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                                        isToday -> Color(0xFFF59E0B).copy(alpha = 0.08f) // Orange background for today
                                        !isValidDay -> Color.Transparent
                                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.5f)
                                    }
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 0.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                                    else Color.Transparent,
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable(enabled = isValidDay) {
                                    selectedDate = if (selectedDate == date) null else date
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (isValidDay && date != null) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = "$dayNumber",
                                        fontSize = 12.sp,
                                        fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isToday) {
                                            if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)
                                        } else MaterialTheme.colorScheme.onSurface
                                    )
                                    if (workout != null) {
                                        Spacer(modifier = Modifier.height(2.dp))
                                        CalendarDayDot(workout = workout)
                                        Text(
                                            text = workoutTypeShort(workout),
                                            fontSize = 9.sp,
                                            color = workoutTypeColor(workout).copy(alpha = 0.9f),
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Legend
        Spacer(modifier = Modifier.height(8.dp))
        WorkoutLegend()

        // Selected day detail
        if (selectedWorkout != null && selectedDate != null) {
            Spacer(modifier = Modifier.height(8.dp))
            SelectedDayDetail(
                date = selectedDate!!,
                workout = selectedWorkout,
                onClick = { showWorkoutDetails = selectedWorkout }
            )
        }
    }

    if (showWorkoutDetails != null) {
        WorkoutDetailsDialog(
            workout = showWorkoutDetails!!,
            onDismiss = { showWorkoutDetails = null }
        )
    }

    if (showLegendDialog) {
        CalendarLegendDialog(
            onDismiss = { showLegendDialog = false }
        )
    }
}

@Composable
fun CalendarDayDot(workout: WorkoutEntity) {
    val color = workoutTypeColor(workout)
    val icon = workoutTypeIcon(workout)
    Box(
        modifier = Modifier
            .size(18.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = if (workout.isCompleted) 1f else 0.25f))
            .border(1.dp, color.copy(alpha = if (workout.isSkipped) 0.3f else 0.7f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        if (workout.isCompleted) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(10.dp)
            )
        } else if (workout.isSkipped) {
            Icon(
                imageVector = Icons.Default.Remove,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(10.dp)
            )
        }
    }
}

@Composable
fun WorkoutLegend() {
    val items = listOf(
        "Chạy dài" to ColorLong,
        "Easy" to ColorEasy,
        "Tempo" to ColorTempo,
        "Phục hồi" to ColorRecovery,
        "Race" to ColorRace,
        "Nghỉ" to ColorRest
    )
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { (label, color) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                    Text(text = label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun SelectedDayDetail(date: LocalDate, workout: WorkoutEntity, onClick: () -> Unit) {
    val color = workoutTypeColor(workout)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = workoutTypeIcon(workout),
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = date.toString(),
                    fontSize = 11.sp,
                    color = color
                )
                Text(
                    text = workout.description,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (workout.targetDistanceKm > 0) {
                    Text(
                        text = "${String.format(java.util.Locale.US, "%.1f", workout.targetDistanceKm)} km - Pace: ${com.example.runcoach.domain.plan.VdotCalculator.formatPace(workout.targetPaceSec)}",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            when {
                workout.isCompleted -> {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = ColorCompleted, modifier = Modifier.size(24.dp))
                }
                workout.isSkipped -> {
                    Icon(Icons.Default.RemoveCircle, contentDescription = null, tint = ColorSkipped, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// Utility functions for workout type display
fun workoutTypeColor(workout: WorkoutEntity): Color {
    return when {
        workout.isSkipped -> ColorSkipped
        workout.type == "LONG" -> ColorLong
        workout.type == "EASY" -> ColorEasy
        workout.type == "TEMPO" || workout.type == "INTERVAL" -> ColorTempo
        workout.type == "RECOVERY" || workout.type == "CT" -> ColorRecovery
        workout.type == "RACE" -> ColorRace
        else -> ColorRest
    }
}

fun workoutTypeIcon(workout: WorkoutEntity): ImageVector {
    return when (workout.type) {
        "LONG" -> Icons.Default.DirectionsRun
        "EASY" -> Icons.Default.DirectionsRun
        "TEMPO", "INTERVAL" -> Icons.Default.Speed
        "RECOVERY" -> Icons.Default.SelfImprovement
        "CT" -> Icons.Default.Pool
        "RACE" -> Icons.Default.EmojiEvents
        "REST" -> Icons.Default.Bedtime
        else -> Icons.Default.FitnessCenter
    }
}

fun workoutTypeShort(workout: WorkoutEntity): String {
    if (workout.isSkipped) return "SKIP"
    return when (workout.type) {
        "LONG" -> "LONG"
        "EASY" -> "EASY"
        "TEMPO" -> "TEMPO"
        "INTERVAL" -> "INTV"
        "RECOVERY" -> "RCV"
        "CT" -> "CT"
        "RACE" -> "RACE"
        "REST" -> "REST"
        else -> workout.type.take(4).uppercase()
    }
}

@Composable
fun CalendarLegendDialog(onDismiss: () -> Unit) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight()
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header Icon
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Thông Tin Lịch Tập",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "Giải thích các loại bài tập và màu sắc tương ứng trong lịch trình luyện tập của bạn.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

                Spacer(modifier = Modifier.height(16.dp))

                val legendItems = listOf(
                    LegendItemData(
                        title = "Chạy dài (Long Run)",
                        color = ColorLong,
                        icon = Icons.Default.DirectionsRun,
                        desc = "Phát triển sức chịu đựng bền bỉ và tim mạch. Rất quan trọng cho cự ly dài."
                    ),
                    LegendItemData(
                        title = "Chạy Easy (Easy Run)",
                        color = ColorEasy,
                        icon = Icons.Default.DirectionsRun,
                        desc = "Chạy nhẹ nhàng thư giãn tích lũy thể tích tuần, củng cố cơ khớp mà ít gây mỏi cơ."
                    ),
                    LegendItemData(
                        title = "Tốc độ (Tempo/Interval)",
                        color = ColorTempo,
                        icon = Icons.Default.Speed,
                        desc = "Chạy cường độ cao giúp nâng cao ngưỡng lactate và gia tăng tốc độ tối đa."
                    ),
                    LegendItemData(
                        title = "Phục hồi (Recovery/CT)",
                        color = ColorRecovery,
                        icon = Icons.Default.SelfImprovement,
                        desc = "Chạy siêu chậm giúp lưu thông máu phục hồi, hoặc các bài tập bổ trợ nhóm cơ chéo."
                    ),
                    LegendItemData(
                        title = "Ngày đua (Race Day)",
                        color = ColorRace,
                        icon = Icons.Default.EmojiEvents,
                        desc = "Ngày tham gia giải chạy mục tiêu hoặc chạy kiểm tra thể lực định kỳ."
                    ),
                    LegendItemData(
                        title = "Nghỉ ngơi (Rest Day)",
                        color = ColorRest,
                        icon = Icons.Default.Bedtime,
                        desc = "Nghỉ ngơi hoàn toàn để phục hồi cơ bắp, tránh chấn thương và quá tải."
                    )
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    legendItems.forEach { item ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(item.color.copy(alpha = 0.18f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = item.icon,
                                    contentDescription = null,
                                    tint = item.color,
                                    modifier = Modifier.size(20.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = item.desc,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    modifier = Modifier.fillMaxWidth().height(48.dp)
                ) {
                    Text("Đóng", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}

data class LegendItemData(
    val title: String,
    val color: Color,
    val icon: ImageVector,
    val desc: String
)
