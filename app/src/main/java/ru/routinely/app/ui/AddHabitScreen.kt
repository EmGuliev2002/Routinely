package ru.routinely.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.routinely.app.model.Habit
import ru.routinely.app.viewmodel.HabitViewModel
import java.util.Calendar

val DEFAULT_COLOR_HEX = "#B88EFA"
val ICON_OPTIONS = listOf(
    Icons.Default.MenuBook, Icons.Default.SportsGymnastics,
    Icons.Default.LocalFireDepartment, Icons.Default.SelfImprovement
)
val WEEK_DAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    viewModel: HabitViewModel,
    habitToEdit: Habit? = null, // Если не null, значит редактируем
    onNavigateBack: () -> Unit
) {
    // --- Состояние экрана ---
    // Если habitToEdit есть, берем данные из него, иначе дефолтные
    var name by rememberSaveable { mutableStateOf(habitToEdit?.name ?: "") }
    var targetValue by rememberSaveable { mutableStateOf(habitToEdit?.targetValue?.toString() ?: "1") }
    var selectedColor by rememberSaveable { mutableStateOf(habitToEdit?.color ?: DEFAULT_COLOR_HEX) }
    var selectedIconName by rememberSaveable { mutableStateOf(habitToEdit?.icon ?: ICON_OPTIONS.first().name) }

    // Парсинг дней недели из типа привычки (для упрощения MVP)
    var selectedDays by rememberSaveable {
        mutableStateOf(
            if (habitToEdit != null && habitToEdit.type != "daily") {
                try {
                    habitToEdit.type.split(",").map { it.toInt() }.toSet()
                } catch (e: Exception) { setOf(1, 2, 3, 4, 5, 6, 7) }
            } else {
                setOf(1, 2, 3, 4, 5, 6, 7)
            }
        )
    }

    var notificationTime by rememberSaveable { mutableStateOf<String?>(habitToEdit?.notificationTime) }

    // Диалог подтверждения удаления
    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- Функция сохранения ---
    val onSaveHabit = {
        if (name.isNotBlank()) {
            val typeValue = if (selectedDays.size == 7) "daily" else selectedDays.sorted().joinToString(",")

            // Если редактируем - берем ID старой привычки, иначе 0 (новый)
            val habitId = habitToEdit?.id ?: 0
            // Сохраняем текущий прогресс при редактировании
            val currentVal = habitToEdit?.currentValue ?: 0
            val streak = habitToEdit?.currentStreak ?: 0
            val bestStreak = habitToEdit?.bestStreak ?: 0
            val lastDate = habitToEdit?.lastCompletedDate
            val creation = habitToEdit?.creationDate ?: System.currentTimeMillis()

            val newHabit = Habit(
                id = habitId,
                name = name,
                icon = selectedIconName,
                color = selectedColor,
                type = typeValue,
                targetValue = targetValue.toIntOrNull() ?: 1,
                currentValue = currentVal,
                creationDate = creation,
                notificationTime = notificationTime,
                lastCompletedDate = lastDate,
                currentStreak = streak,
                bestStreak = bestStreak
            )
            viewModel.saveHabit(newHabit)
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (habitToEdit == null) "Новая привычка" else "Редактирование", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    // Кнопка удаления (только при редактировании)
                    if (habitToEdit != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Удалить", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomSaveButton(onSave = onSaveHabit, text = if (habitToEdit == null) "Создать" else "Сохранить")
        }
    ) { paddingValues ->

        if (showDeleteDialog && habitToEdit != null) {
            AlertDialog(
                onDismissRequest = { showDeleteDialog = false },
                title = { Text("Удалить привычку?") },
                text = { Text("Привычка \"${habitToEdit.name}\" и вся её история будут удалены.") },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteHabit(habitToEdit)
                            showDeleteDialog = false
                            onNavigateBack()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) { Text("Удалить") }
                },
                dismissButton = {
                    TextButton(onClick = { showDeleteDialog = false }) { Text("Отмена") }
                }
            )
        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Название
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp)
            )

            // 2. Цель (с пояснением)
            OutlinedTextField(
                value = targetValue,
                onValueChange = { if (it.all { char -> char.isDigit() }) targetValue = it },
                label = { Text("Цель на день (раз, км, стр)") },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                supportingText = {
                    if ((targetValue.toIntOrNull() ?: 0) >= 5) {
                        Text("Будет использоваться слайдер для ввода")
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // 3. Иконка
            IconSelectorRow(selectedIconName = selectedIconName, onIconSelected = { selectedIconName = it })
            Spacer(Modifier.height(16.dp))

            // 4. Цвет
            ColorSelectorRow(selectedColor = selectedColor, onColorSelected = { selectedColor = it })
            Spacer(Modifier.height(16.dp))

            // 5. Расписание
            ScheduleSelector(selectedDays = selectedDays, onDayToggle = { day ->
                selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
            })
            Spacer(Modifier.height(16.dp))

            // 6. Уведомление
            NotificationTimePicker(
                time = notificationTime,
                onTimeSelected = { notificationTime = it }
            )

            Spacer(Modifier.height(80.dp)) // Отступ для кнопки
        }
    }
}

// Вспомогательный компонент для времени (вынесен для чистоты)
@Composable
fun NotificationTimePicker(time: String?, onTimeSelected: (String?) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val dialog = TimePickerDialog(
        context,
        { _, hour, minute -> onTimeSelected(String.format("%02d:%02d", hour, minute)) },
        calendar.get(Calendar.HOUR_OF_DAY),
        calendar.get(Calendar.MINUTE),
        true
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .clickable { dialog.show() }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Notifications, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Text(text = time ?: "Добавить напоминание")
        }
        if (time != null) {
            IconButton(onClick = { onTimeSelected(null) }) {
                Icon(Icons.Default.Clear, contentDescription = "Удалить")
            }
        }
    }
}

@Composable
fun BottomSaveButton(onSave: () -> Unit, text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .imePadding() // Чтобы клавиатура не перекрывала кнопку
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Text(text, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
fun IconSelectorRow(selectedIconName: String, onIconSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Выберите иконку",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ICON_OPTIONS.forEach { icon ->
                val iconName = icon.name
                val isSelected = iconName == selectedIconName

                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onIconSelected(iconName) }
                        .padding(4.dp)
                )
            }
        }
    }
}

@Composable
fun ColorSelectorRow(selectedColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf("#B88EFA", "#FF70A6", "#70A6FF", "#5CC8A5", "#FFBF69", "#A8DADC")

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Выберите цвет",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            colors.forEach { colorHex ->
                val isSelected = colorHex == selectedColor
                val size = if (isSelected) 40.dp else 32.dp
                val borderWidth = if (isSelected) 4.dp else 0.dp

                Box(
                    modifier = Modifier
                        .size(size)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(colorHex)))
                        .clickable { onColorSelected(colorHex) }
                        .border(borderWidth, MaterialTheme.colorScheme.onSurface, CircleShape)
                )
            }
        }
    }
}

@Composable
fun ScheduleSelector(selectedDays: Set<Int>, onDayToggle: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Расписание",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            WEEK_DAYS.forEachIndexed { index, dayName ->
                val dayIndex = index + 1 // 1 = Пн, 7 = Вс
                val isSelected = dayIndex in selectedDays

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                        .clickable { onDayToggle(dayIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}