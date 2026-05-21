package com.example.runcoach.presentation.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.runcoach.data.local.db.WorkoutEntity
import com.example.runcoach.domain.plan.VdotCalculator
import com.example.runcoach.presentation.MainViewModel
import com.example.runcoach.ui.theme.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val workoutsList by viewModel.workouts.collectAsState()
    val context = LocalContext.current

    // Filter out REST (rest days) and CT (cross training / supplementary workouts)
    val runningWorkoutsOnly = remember(workoutsList) {
        workoutsList.filter { it.type != "REST" && it.type != "CT" }
    }

    // Sorting by date descending (most recent first)
    val sortedWorkouts = remember(runningWorkoutsOnly) {
        runningWorkoutsOnly.sortedByDescending { it.date }
    }

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Hoàn thành, 1: Chưa tập, 2: Bỏ lỡ
    val tabs = listOf("Hoàn thành", "Chưa tập", "Bỏ lỡ")

    val filteredWorkouts = remember(sortedWorkouts, runningWorkoutsOnly, selectedTab) {
        when (selectedTab) {
            0 -> sortedWorkouts.filter { it.isCompleted }
            1 -> runningWorkoutsOnly.filter { !it.isCompleted && !it.isSkipped }.sortedBy { it.date }
            2 -> sortedWorkouts.filter { it.isSkipped }
            else -> sortedWorkouts
        }
    }

    var editingWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }

    val isDark = isSystemInDarkTheme()
    val bgBrush = Brush.verticalGradient(
        colors = if (isDark)
            listOf(Color(0xFF0F0F10), Color(0xFF1E293B))
        else
            listOf(Color(0xFFF9FAFB), Color(0xFFEEF2FF))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Lịch Sử Tập Luyện",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onBackground
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Elegant Tab selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    tabs.forEachIndexed { index, label ->
                        val isSelected = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
            }

            // Stats Quick Summary Panel
            if (selectedTab == 0) {
                val completedRuns = runningWorkoutsOnly.filter { it.isCompleted }
                val totalDistance = completedRuns.sumOf { it.actualDistanceKm }
                val totalDuration = completedRuns.sumOf { it.actualDurationMin }
                
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f)
                    ),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceAround,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Tổng quãng đường", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                text = String.format("%.1f km", totalDistance),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Thời gian chạy", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                text = String.format("%d phút", totalDuration.toInt()),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .height(32.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f))
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Đã hoàn thành", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                            Text(
                                text = "${completedRuns.size} buổi",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorCompleted
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Workouts List
            if (filteredWorkouts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Không có lịch sử phù hợp.",
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                            fontSize = 14.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(filteredWorkouts, key = { it.date }) { workout ->
                        HistoryWorkoutCard(
                            workout = workout,
                            onEditClick = { editingWorkout = workout }
                        )
                    }
                }
            }
        }
    }

    // Edit Stats Dialog
    if (editingWorkout != null) {
        EditWorkoutStatsDialog(
            workout = editingWorkout!!,
            onDismiss = { editingWorkout = null },
            onConfirm = { distance, duration, isCompleted ->
                viewModel.updateWorkoutManualStats(
                    date = editingWorkout!!.date,
                    distanceKm = distance,
                    durationMin = duration,
                    isCompleted = isCompleted
                )
                editingWorkout = null
                Toast.makeText(context, "✅ Đã cập nhật kết quả tập luyện!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
fun HistoryWorkoutCard(
    workout: WorkoutEntity,
    onEditClick: () -> Unit
) {
    val wColor = workoutTypeColor(workout)
    val icon = workoutTypeIcon(workout)

    val formattedDate = remember(workout.date) {
        try {
            val date = LocalDate.parse(workout.date)
            val names = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
            "${names[date.dayOfWeek.value - 1]}, ${date.dayOfMonth}/${date.monthValue}/${date.year}"
        } catch (e: Exception) {
            workout.date
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                workout.isCompleted -> ColorCompleted.copy(alpha = 0.05f)
                workout.isSkipped -> ColorSkipped.copy(alpha = 0.04f)
                else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
            }
        ),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(
            width = 1.dp,
            color = if (workout.isCompleted) ColorCompleted.copy(alpha = 0.2f)
            else wColor.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(wColor.copy(alpha = 0.18f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = wColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = formattedDate,
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        Text(
                            text = workout.description,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Edit Button
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f),
                            CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit stats",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details / Targets
            if (workout.type != "REST") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("🎯 Mục tiêu giáo án", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(2.dp))
                        if (workout.targetDistanceKm > 0) {
                            Text(
                                text = "${workout.targetDistanceKm} km @ ${VdotCalculator.formatPace(workout.targetPaceSec)}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        } else {
                            Text(
                                text = workout.type,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = wColor
                            )
                        }
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text("🏁 Kết quả thực tế", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        Spacer(modifier = Modifier.height(2.dp))
                        if (workout.isCompleted) {
                            val sourceLabel = when (workout.syncSource) {
                                "HEALTH_CONNECT" -> "Health Connect"
                                "MANUAL" -> "Thủ công"
                                else -> "Đồng bộ"
                            }
                            Text(
                                text = "${workout.actualDistanceKm} km trong ${workout.actualDurationMin.toInt()} phút",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorCompleted
                            )
                            Text(
                                text = "Nguồn: $sourceLabel",
                                fontSize = 9.sp,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        } else if (workout.isSkipped) {
                            Text(
                                text = "Đã dời lịch tập",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ColorSkipped
                            )
                        } else {
                            Text(
                                text = "Chưa hoàn thành",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            } else {
                // Rest day custom text
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            ColorRest.copy(alpha = 0.05f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(12.dp)
                ) {
                    Text(
                        text = "😴 Hôm nay là ngày nghỉ ngơi phục hồi cơ thể.",
                        fontSize = 12.sp,
                        color = ColorRest,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditWorkoutStatsDialog(
    workout: WorkoutEntity,
    onDismiss: () -> Unit,
    onConfirm: (Double, Double, Boolean) -> Unit
) {
    var isCompleted by remember { mutableStateOf(workout.isCompleted) }
    var distanceText by remember { mutableStateOf(if (workout.isCompleted) workout.actualDistanceKm.toString() else workout.targetDistanceKm.toString()) }
    var durationText by remember { mutableStateOf(if (workout.isCompleted) workout.actualDurationMin.toInt().toString() else "") }
    
    val context = LocalContext.current

    if (durationText.isEmpty() && !isCompleted) {
        // Estimate duration based on target distance and target pace
        if (workout.targetDistanceKm > 0 && workout.targetPaceSec > 0) {
            val estimatedMinutes = ((workout.targetDistanceKm * workout.targetPaceSec) / 60).toInt()
            durationText = estimatedMinutes.toString()
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Icon(
                    imageVector = Icons.Default.EditCalendar,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp)
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Cập Nhật Kết Quả",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = workout.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Toggle Completed Status
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.02f))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Đã hoàn thành bài tập",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Switch(
                        checked = isCompleted,
                        onCheckedChange = { isCompleted = it }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Input fields (animated visibility when completed is checked)
                AnimatedVisibility(
                    visible = isCompleted,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column {
                        OutlinedTextField(
                            value = distanceText,
                            onValueChange = { distanceText = it },
                            label = { Text("Quãng đường chạy thực tế (km)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.DirectionsRun, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedTextField(
                            value = durationText,
                            onValueChange = { durationText = it },
                            label = { Text("Thời gian chạy thực tế (phút)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "💡 Nhập đúng quãng đường và thời gian hiển thị trên đồng hồ Garmin/Strava của bạn để tối ưu dự báo VDOT.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                            lineHeight = 15.sp
                        )
                    }
                }

                // If rest day or not completed
                if (!isCompleted) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.05f)),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.15f))
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Lưu ý: Đánh dấu chưa hoàn thành sẽ xóa mọi kết quả thực tế đã ghi nhận trước đó cho ngày này.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Hủy")
                    }

                    Button(
                        onClick = {
                            if (isCompleted) {
                                val distance = distanceText.toDoubleOrNull() ?: 0.0
                                val duration = durationText.toDoubleOrNull() ?: 0.0
                                if (distance <= 0.0 || duration <= 0.0) {
                                    Toast.makeText(context, "Vui lòng nhập quãng đường và thời gian hợp lệ lớn hơn 0!", Toast.LENGTH_LONG).show()
                                    return@Button
                                }
                                onConfirm(distance, duration, true)
                            } else {
                                onConfirm(0.0, 0.0, false)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lưu kết quả")
                    }
                }
            }
        }
    }
}
