package com.example.runcoach.presentation.screens

import android.app.DatePickerDialog
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.window.DialogProperties
import com.example.runcoach.data.local.db.WorkoutEntity
import com.example.runcoach.domain.plan.VdotCalculator
import com.example.runcoach.presentation.MainViewModel
import com.example.runcoach.ui.theme.*
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlanScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val workoutsList by viewModel.workouts.collectAsState()
    
    // Dialog states
    var editingWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val sortedWorkouts = remember(workoutsList) {
        workoutsList.sortedBy { it.date }
    }

    val isDark = isSystemInDarkTheme()
    val bgBrush = Brush.verticalGradient(
        colors = if (isDark) listOf(Color(0xFF0F0F10), Color(0xFF1E1B4B))
        else listOf(Color(0xFFFAFAFA), Color(0xFFEEF2FF))
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Thiết Kế Giáo Án", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Quay lại")
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Default.Share, contentDescription = "Chia sẻ")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Xuất file CSV") },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportPlanToCsv(context) { uri ->
                                        if (uri != null) {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "text/csv"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Chia sẻ CSV"))
                                        } else {
                                            Toast.makeText(context, "Lỗi khi xuất file CSV!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Xuất file PDF") },
                                onClick = {
                                    showExportMenu = false
                                    viewModel.exportPlanToPdf(context) { uri ->
                                        if (uri != null) {
                                            val intent = Intent(Intent.ACTION_SEND).apply {
                                                type = "application/pdf"
                                                putExtra(Intent.EXTRA_STREAM, uri)
                                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                            }
                                            context.startActivity(Intent.createChooser(intent, "Chia sẻ PDF"))
                                        } else {
                                            Toast.makeText(context, "Lỗi khi xuất file PDF!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Thêm bài tập")
            }
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(bgBrush)
                .padding(innerPadding)
        ) {
            if (sortedWorkouts.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Không có bài tập nào. Hãy tạo mới hoặc hoàn thành onboarding để tự sinh giáo án.",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(32.dp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(sortedWorkouts, key = { it.date }) { workout ->
                        CustomWorkoutCard(
                            workout = workout,
                            onEdit = { editingWorkout = workout },
                            onDelete = {
                                viewModel.deleteWorkout(workout)
                                Toast.makeText(context, "Đã xóa bài tập ngày ${workout.date}", Toast.LENGTH_SHORT).show()
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog Add/Edit
    if (showAddDialog) {
        AddEditWorkoutDialog(
            workout = null,
            onDismiss = { showAddDialog = false },
            onConfirm = { newWorkout ->
                viewModel.upsertWorkout(newWorkout)
                showAddDialog = false
                Toast.makeText(context, "Đã thêm bài tập mới!", Toast.LENGTH_SHORT).show()
            },
            workoutsList = workoutsList
        )
    }

    if (editingWorkout != null) {
        AddEditWorkoutDialog(
            workout = editingWorkout,
            onDismiss = { editingWorkout = null },
            onConfirm = { updatedWorkout ->
                viewModel.upsertWorkout(updatedWorkout)
                editingWorkout = null
                Toast.makeText(context, "Đã cập nhật bài tập!", Toast.LENGTH_SHORT).show()
            },
            workoutsList = workoutsList
        )
    }
}

@Composable
fun CustomWorkoutCard(
    workout: WorkoutEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val wColor = workoutTypeColor(workout)
    val icon = workoutTypeIcon(workout)
    val formattedDate = remember(workout.date) {
        try {
            val date = LocalDate.parse(workout.date)
            val dayNames = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
            "${dayNames[date.dayOfWeek.value - 1]}, ${date.dayOfMonth}/${date.monthValue}"
        } catch (e: Exception) { workout.date }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, wColor.copy(alpha = 0.2f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(wColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = wColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "$formattedDate · Tuần ${workout.weekNumber}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Text(
                        text = workout.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (workout.type != "REST") {
                        val paceStr = if (workout.targetPaceSec > 0) {
                            " @ " + VdotCalculator.formatPace(workout.targetPaceSec)
                        } else ""
                        Text(
                            text = "${workout.targetDistanceKm} km$paceStr",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, contentDescription = "Sửa", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Xóa", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditWorkoutDialog(
    workout: WorkoutEntity?,
    onDismiss: () -> Unit,
    onConfirm: (WorkoutEntity) -> Unit,
    workoutsList: List<WorkoutEntity>
) {
    val isEdit = workout != null
    var selectedDate by remember { mutableStateOf(workout?.date ?: LocalDate.now().toString()) }
    var selectedType by remember { mutableStateOf(workout?.type ?: "EASY") }
    var description by remember { mutableStateOf(workout?.description ?: "") }
    var distanceText by remember { mutableStateOf(workout?.targetDistanceKm?.toString() ?: "5.0") }
    var paceMinText by remember { mutableStateOf(if (workout != null && workout.targetPaceSec > 0) (workout.targetPaceSec / 60).toString() else "6") }
    var paceSecText by remember { mutableStateOf(if (workout != null && workout.targetPaceSec > 0) (workout.targetPaceSec % 60).toString() else "30") }
    var instructions by remember { mutableStateOf(workout?.instructions ?: "") }
    var notes by remember { mutableStateOf(workout?.notes ?: "") }

    val context = LocalContext.current
    val workoutTypes = listOf("EASY", "LONG", "TEMPO", "INTERVAL", "REPETITION", "RECOVERY", "REST")
    var expandedDropdown by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = if (isEdit) "Chỉnh Sửa Bài Tập" else "Thêm Bài Tập Mới",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )

                // Date Picker field
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val parsed = try { LocalDate.parse(selectedDate) } catch(e: Exception) { LocalDate.now() }
                            DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    selectedDate = LocalDate.of(y, m + 1, d).toString()
                                },
                                parsed.year,
                                parsed.monthValue - 1,
                                parsed.dayOfMonth
                            ).show()
                        }
                ) {
                    OutlinedTextField(
                        value = selectedDate,
                        onValueChange = {},
                        label = { Text("Ngày") },
                        leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                        readOnly = true,
                        enabled = false,
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Type selector dropdown
                Box(modifier = Modifier.fillMaxWidth()) {
                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Loại bài tập") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            workoutTypes.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type) },
                                    onClick = {
                                        selectedType = type
                                        expandedDropdown = false
                                        // Autofill standard labels
                                        if (description.isBlank() || description.startsWith("Chạy") || description.startsWith("Nghỉ")) {
                                            description = when (type) {
                                                "REST" -> "Nghỉ ngơi hồi phục"
                                                "LONG" -> "Chạy Long Run"
                                                "EASY" -> "Chạy Easy"
                                                "TEMPO" -> "Chạy Tempo"
                                                "INTERVAL" -> "Chạy Interval"
                                                "RECOVERY" -> "Chạy Phục Hồi"
                                                "REPETITION" -> "Chạy Repetition"
                                                else -> "Bài tập chạy"
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }

                // Description field
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Mô tả bài chạy") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (selectedType != "REST") {
                    // Distance field
                    OutlinedTextField(
                        value = distanceText,
                        onValueChange = { distanceText = it },
                        label = { Text("Cự ly mục tiêu (km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Target pace field
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = paceMinText,
                            onValueChange = { paceMinText = it },
                            label = { Text("Pace (Phút)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paceSecText,
                            onValueChange = { paceSecText = it },
                            label = { Text("Pace (Giây)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Instructions field
                OutlinedTextField(
                    value = instructions,
                    onValueChange = { instructions = it },
                    label = { Text("Hướng dẫn kỹ thuật") },
                    maxLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú cá nhân (nếu có)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
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
                            val dist = distanceText.toDoubleOrNull() ?: 0.0
                            val pm = paceMinText.toIntOrNull() ?: 0
                            val ps = paceSecText.toIntOrNull() ?: 0
                            val totalPaceSec = pm * 60 + ps

                            if (selectedType != "REST" && dist <= 0.0) {
                                Toast.makeText(context, "Cự ly phải lớn hơn 0!", Toast.LENGTH_SHORT).show()
                                return@Button
                            }

                            // Calculate week number relative to earliest workout in plan
                            val earliestDate = workoutsList.minOfOrNull { LocalDate.parse(it.date) }
                            val targetLocalDate = LocalDate.parse(selectedDate)
                            val computedWeek = if (earliestDate != null) {
                                val diffDays = ChronoUnit.DAYS.between(earliestDate, targetLocalDate)
                                val wk = (diffDays / 7).toInt() + 1
                                wk.coerceAtLeast(1)
                            } else {
                                1
                            }

                            val newWorkout = WorkoutEntity(
                                date = selectedDate,
                                weekNumber = computedWeek,
                                type = selectedType,
                                targetDistanceKm = if (selectedType == "REST") 0.0 else dist,
                                targetPaceSec = if (selectedType == "REST") 0 else totalPaceSec,
                                description = description.ifBlank { selectedType },
                                instructions = instructions,
                                notes = notes,
                                isCompleted = workout?.isCompleted ?: false,
                                actualDistanceKm = workout?.actualDistanceKm ?: 0.0,
                                actualDurationMin = workout?.actualDurationMin ?: 0.0,
                                completedDate = workout?.completedDate,
                                syncSource = workout?.syncSource,
                                isSkipped = workout?.isSkipped ?: false,
                                isCustom = true
                            )
                            onConfirm(newWorkout)
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Lưu")
                    }
                }
            }
        }
    }
}
