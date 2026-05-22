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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
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
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomPlanScreen(
    viewModel: MainViewModel,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val workoutsList by viewModel.workouts.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState()
    
    // Dialog states
    var editingWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }
    var showRegenerateConfirm by remember { mutableStateOf(false) }

    // Plan parameter local states
    var isExpanded by remember { mutableStateOf(false) }
    var raceDate by remember { mutableStateOf("") }
    var targetDistance by remember { mutableStateOf(21) }
    var gender by remember { mutableStateOf("MALE") }
    var age by remember { mutableStateOf(25) }
    var timeMinText by remember { mutableStateOf("") }
    var timeSecText by remember { mutableStateOf("") }

    LaunchedEffect(userPrefs) {
        if (raceDate.isEmpty()) {
            raceDate = userPrefs.raceDate
            targetDistance = userPrefs.targetDistance
            gender = userPrefs.gender
            age = userPrefs.age
            val totalSeconds = VdotCalculator.get3kTimeFromVdot(userPrefs.vdotScore.toDouble())
            val min = (totalSeconds / 60).toInt()
            val sec = (totalSeconds % 60).roundToInt()
            timeMinText = min.toString()
            timeSecText = sec.toString()
        }
    }

    val sortedWorkouts = remember(workoutsList) {
        workoutsList.sortedBy { it.date }
    }

    // Group workouts chronologically by Month and Week
    val groupedWorkouts = remember(sortedWorkouts) {
        val list = mutableListOf<Pair<String, List<Pair<Int, List<WorkoutEntity>>>>>()
        val byMonth = sortedWorkouts.groupBy {
            try {
                val d = LocalDate.parse(it.date)
                "Tháng ${d.monthValue}, ${d.year}"
            } catch (e: Exception) {
                "Không xác định"
            }
        }
        byMonth.forEach { (monthStr, workoutsInMonth) ->
            val byWeek = workoutsInMonth.groupBy { it.weekNumber }
            val sortedWeeks = byWeek.toList().sortedBy { it.first }
            list.add(Pair(monthStr, sortedWeeks))
        }
        list
    }

    // Drag and Drop Swap states
    var showSwapConfirmation by remember { mutableStateOf(false) }
    var swapSourceWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }
    var swapTargetWorkout by remember { mutableStateOf<WorkoutEntity?>(null) }

    val dragAndDropState = remember(workoutsList) {
        DragAndDropState(
            onDrop = { source, target ->
                swapSourceWorkout = source
                swapTargetWorkout = target
                showSwapConfirmation = true
            }
        )
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
            Column(modifier = Modifier.fillMaxSize()) {
                // Collapsible Original Plan parameters card at the top
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { isExpanded = !isExpanded },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Thông số giáo án gốc",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (!isExpanded) {
                                    val distanceStr = when (targetDistance) {
                                        5 -> "5K"
                                        10 -> "10K"
                                        21 -> "21K"
                                        42 -> "42K"
                                        else -> "${targetDistance}K"
                                    }
                                    val genderStr = when (gender) {
                                        "MALE" -> "Nam"
                                        "FEMALE" -> "Nữ"
                                        else -> "Khác"
                                    }
                                    Text(
                                        text = "$distanceStr · $genderStr · $age tuổi",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                        modifier = Modifier.padding(end = 4.dp)
                                    )
                                }
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Thu gọn" else "Mở rộng",
                                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }

                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Race Date
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val parsed = try { LocalDate.parse(raceDate) } catch(e: Exception) { LocalDate.now() }
                                        DatePickerDialog(
                                            context,
                                            { _, y, m, d ->
                                                raceDate = LocalDate.of(y, m + 1, d).toString()
                                            },
                                            parsed.year,
                                            parsed.monthValue - 1,
                                            parsed.dayOfMonth
                                        ).show()
                                    }
                            ) {
                                OutlinedTextField(
                                    value = raceDate,
                                    onValueChange = {},
                                    label = { Text("Ngày chạy giải (Race Date)") },
                                    leadingIcon = { Icon(Icons.Default.CalendarToday, contentDescription = null) },
                                    readOnly = true,
                                    enabled = false,
                                    shape = RoundedCornerShape(20.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        disabledTextColor = MaterialTheme.colorScheme.onSurface,
                                        disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Target distance
                            Text(
                                text = "Cự ly mục tiêu",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val distances = listOf(5, 10, 21, 42)
                                distances.forEach { dist ->
                                    val selected = targetDistance == dist
                                    val emoji = when (dist) {
                                        5 -> "🏃"
                                        10 -> "⚡"
                                        21 -> "🏅"
                                        42 -> "🏆"
                                        else -> "👟"
                                    }
                                    val label = when (dist) {
                                        5 -> "5K"
                                        10 -> "10K"
                                        21 -> "21K"
                                        42 -> "42K"
                                        else -> "${dist}K"
                                    }
                                    val subtext = when (dist) {
                                        5 -> "Fun"
                                        10 -> "Speed"
                                        21 -> "Half"
                                        42 -> "Full"
                                        else -> "Run"
                                    }
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(
                                                if (selected) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                            )
                                            .border(
                                                1.dp,
                                                if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                                RoundedCornerShape(16.dp)
                                            )
                                            .clickable { targetDistance = dist }
                                            .padding(vertical = 10.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(text = emoji, fontSize = 20.sp)
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = label,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = subtext,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Gender & Age
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Column(modifier = Modifier.weight(1.8f)) {
                                    Text(
                                        text = "Giới tính",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                                    )
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        val genders = listOf("MALE", "FEMALE", "OTHER")
                                        genders.forEach { g ->
                                            val selected = gender == g
                                            val icon = when (g) {
                                                "MALE" -> Icons.Default.Male
                                                "FEMALE" -> Icons.Default.Female
                                                else -> Icons.Default.Transgender
                                            }
                                            val label = when (g) {
                                                "MALE" -> "Nam"
                                                "FEMALE" -> "Nữ"
                                                else -> "Khác"
                                            }
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .fillMaxHeight()
                                                    .clip(RoundedCornerShape(20.dp))
                                                    .background(
                                                        if (selected) {
                                                            when (g) {
                                                                "MALE" -> Color(0xFF3B82F6)
                                                                "FEMALE" -> Color(0xFFEC4899)
                                                                else -> Color(0xFF8B5CF6)
                                                            }
                                                        } else {
                                                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                                                        }
                                                    )
                                                    .border(
                                                        1.dp,
                                                        if (selected) {
                                                            when (g) {
                                                                "MALE" -> Color(0xFF3B82F6)
                                                                "FEMALE" -> Color(0xFFEC4899)
                                                                else -> Color(0xFF8B5CF6)
                                                            }
                                                        } else {
                                                            MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                                        },
                                                        RoundedCornerShape(20.dp)
                                                    )
                                                    .clickable { gender = g },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = icon,
                                                        contentDescription = label,
                                                        tint = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                    Text(
                                                        text = label,
                                                        fontSize = 10.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Spacer(modifier = Modifier.height(20.dp))
                                    OutlinedTextField(
                                        value = if (age > 0) age.toString() else "",
                                        onValueChange = {
                                            val clean = it.filter { c -> c.isDigit() }
                                            age = clean.toIntOrNull() ?: 0
                                        },
                                        label = { Text("Tuổi") },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        singleLine = true,
                                        shape = RoundedCornerShape(20.dp),
                                        modifier = Modifier.fillMaxWidth().height(56.dp),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                                            unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                                        )
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))

                            // Best 3km running time
                            Text(
                                text = "Thời gian chạy 3km tốt nhất",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.padding(start = 4.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                OutlinedTextField(
                                    value = timeMinText,
                                    onValueChange = { timeMinText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Phút") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedTextField(
                                    value = timeSecText,
                                    onValueChange = { timeSecText = it.filter { c -> c.isDigit() } },
                                    label = { Text("Giây") },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    singleLine = true,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            Spacer(modifier = Modifier.height(14.dp))

                            // Regenerate button
                            Button(
                                onClick = {
                                    val min = timeMinText.toIntOrNull() ?: 0
                                    val sec = timeSecText.toIntOrNull() ?: 0
                                    val totalSec = min * 60.0 + sec
                                    if (totalSec <= 0) {
                                        Toast.makeText(context, "Vui lòng nhập thời gian chạy 3km hợp lệ!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (raceDate.isBlank()) {
                                        Toast.makeText(context, "Vui lòng chọn ngày chạy giải!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (age <= 0) {
                                        Toast.makeText(context, "Vui lòng nhập tuổi hợp lệ!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    showRegenerateConfirm = true
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Tạo Lại Giáo Án",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

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
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        groupedWorkouts.forEach { (monthStr, weeksList) ->
                            item(key = "month_header_$monthStr") {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 16.dp, bottom = 8.dp)
                                ) {
                                    Text(
                                        text = monthStr.uppercase(),
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier
                                            .background(
                                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                                shape = RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    )
                                }
                            }

                            weeksList.forEach { (weekNum, workoutsInWeek) ->
                                item(key = "week_header_${monthStr}_$weekNum") {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(start = 4.dp, top = 8.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Tuần $weekNum",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }

                                items(workoutsInWeek, key = { workoutItem -> workoutItem.date }) { workout ->
                                    CustomWorkoutCard(
                                        workout = workout,
                                        onEdit = { editingWorkout = workout },
                                        onDelete = {
                                            viewModel.deleteWorkout(workout)
                                            Toast.makeText(context, "Đã xóa bài tập ngày ${workout.date}", Toast.LENGTH_SHORT).show()
                                        },
                                        dragAndDropState = dragAndDropState,
                                        workoutsList = workoutsList
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Drag preview overlay
        if (dragAndDropState.isDragging && dragAndDropState.dragItem != null) {
            val dragItem = dragAndDropState.dragItem!!
            val offset = dragAndDropState.currentDragPosition - dragAndDropState.localTouchOffset
            val density = LocalDensity.current
            val itemRect = dragAndDropState.itemBounds[dragItem.date]
            val itemWidth = if (itemRect != null) {
                with(density) { itemRect.width.toDp() }
            } else {
                320.dp
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .offset { IntOffset(offset.x.toInt(), offset.y.toInt()) }
                        .width(itemWidth)
                        .shadow(16.dp, RoundedCornerShape(16.dp))
                        .alpha(1.0f)
                ) {
                    CustomWorkoutCard(
                        workout = dragItem,
                        onEdit = {},
                        onDelete = {},
                        isDragging = false
                    )
                }
            }
        }
    }

    // Swap confirmation dialog
    if (showSwapConfirmation && swapSourceWorkout != null && swapTargetWorkout != null) {
        Dialog(
            onDismissRequest = { showSwapConfirmation = false },
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
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Đổi lịch tập luyện",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.fillMaxWidth()
                    )

                    val dateAStr = try {
                        val d = LocalDate.parse(swapSourceWorkout!!.date)
                        val names = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
                        "${names[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
                    } catch (e: Exception) { swapSourceWorkout!!.date }

                    val dateBStr = try {
                        val d = LocalDate.parse(swapTargetWorkout!!.date)
                        val names = listOf("Thứ 2", "Thứ 3", "Thứ 4", "Thứ 5", "Thứ 6", "Thứ 7", "CN")
                        "${names[d.dayOfWeek.value - 1]} ${d.dayOfMonth}/${d.monthValue}"
                    } catch (e: Exception) { swapTargetWorkout!!.date }

                    Text(
                        text = "Bạn muốn tráo đổi bài tập giữa ngày $dateAStr và $dateBStr cho chỉ tuần này hay áp dụng cho toàn bộ các tuần tiếp theo?",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showSwapConfirmation = false
                                viewModel.swapWorkouts(swapSourceWorkout!!, swapTargetWorkout!!, applyToSubsequentWeeks = false)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        ) {
                            Text("Chỉ tuần này", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = {
                                showSwapConfirmation = false
                                viewModel.swapWorkouts(swapSourceWorkout!!, swapTargetWorkout!!, applyToSubsequentWeeks = true)
                            },
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Tất cả tuần sau", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    OutlinedButton(
                        onClick = { showSwapConfirmation = false },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Hủy", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Regenerate confirmation dialog
    if (showRegenerateConfirm) {
        AlertDialog(
            onDismissRequest = { showRegenerateConfirm = false },
            title = { Text("Tạo lại giáo án mới?") },
            text = {
                Text("Hành động này sẽ XÓA TOÀN BỘ bài tập hiện tại và sinh lại giáo án mới dựa trên thông số bạn vừa nhập. Bạn có chắc chắn muốn tiếp tục không?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        showRegenerateConfirm = false
                        val min = timeMinText.toIntOrNull() ?: 0
                        val sec = timeSecText.toIntOrNull() ?: 0
                        val totalSec = min * 60.0 + sec
                        viewModel.regeneratePlan(
                            raceDate = raceDate,
                            targetDistance = targetDistance,
                            gender = gender,
                            age = age,
                            timeSeconds = totalSec
                        ) {
                            Toast.makeText(context, "Đã tạo lại giáo án mới thành công!", Toast.LENGTH_SHORT).show()
                            isExpanded = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Đồng ý", color = MaterialTheme.colorScheme.onError)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showRegenerateConfirm = false }
                ) {
                    Text("Hủy")
                }
            }
        )
    }
}

@Composable
fun CustomWorkoutCard(
    workout: WorkoutEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dragAndDropState: DragAndDropState? = null,
    workoutsList: List<WorkoutEntity> = emptyList(),
    isDragging: Boolean = false
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

    val isHovered = dragAndDropState?.hoverItem?.date == workout.date

    val dragModifier = if (dragAndDropState != null) {
        Modifier.workoutDragAndDropTarget(workout, dragAndDropState, workoutsList)
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(dragModifier),
        colors = CardDefaults.cardColors(
            containerColor = if (isDragging) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
        ),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(
            if (isHovered) 2.dp else 1.dp,
            if (isHovered) MaterialTheme.colorScheme.primary else wColor.copy(alpha = if (isDragging) 0.6f else 0.2f)
        )
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
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDragging) 0.8f else 0.5f)
                    )
                    Text(
                        text = workout.description,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (workout.type != "REST") {
                        val paceStr = if (workout.targetPaceSec > 0) {
                            " - Pace: " + VdotCalculator.formatPace(workout.targetPaceSec)
                        } else ""
                        Text(
                            text = "${String.format(java.util.Locale.US, "%.1f", workout.targetDistanceKm)} km$paceStr",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isDragging) 0.9f else 0.7f),
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
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
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
                            shape = RoundedCornerShape(20.dp),
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
                    shape = RoundedCornerShape(20.dp),
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
                        shape = RoundedCornerShape(20.dp),
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
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = paceSecText,
                            onValueChange = { paceSecText = it },
                            label = { Text("Pace (Giây)") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            shape = RoundedCornerShape(20.dp),
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
                    shape = RoundedCornerShape(20.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Notes field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Ghi chú cá nhân (nếu có)") },
                    maxLines = 2,
                    shape = RoundedCornerShape(20.dp),
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
