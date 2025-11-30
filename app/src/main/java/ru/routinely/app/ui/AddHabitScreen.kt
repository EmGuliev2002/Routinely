package ru.routinely.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import ru.routinely.app.model.Habit
import ru.routinely.app.viewmodel.HabitViewModel
import java.util.Calendar

val DEFAULT_COLOR_HEX = "#B88EFA"

// ИСПРАВЛЕНО: Используем Map для явной связки "Имя в БД" -> "Иконка UI"
val ICON_MAP = mapOf(
    "MenuBook" to Icons.Default.MenuBook,
    "SportsGymnastics" to Icons.Default.SportsGymnastics,
    "LocalFireDepartment" to Icons.Default.LocalFireDepartment,
    "SelfImprovement" to Icons.Default.SelfImprovement
)

val WEEK_DAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    viewModel: HabitViewModel,
    habitToEdit: Habit? = null,
    onNavigateBack: () -> Unit
) {
    // --- Состояние экрана ---
    var name by rememberSaveable { mutableStateOf(habitToEdit?.name ?: "") }
    var targetValue by rememberSaveable {
        mutableStateOf(habitToEdit?.targetValue?.toString() ?: "1")
    }
    var selectedColor by rememberSaveable {
        mutableStateOf(habitToEdit?.color ?: DEFAULT_COLOR_HEX)
    }

    // ИСПРАВЛЕНО: Инициализация иконки берется из ключей карты
    var selectedIconName by rememberSaveable {
        mutableStateOf(habitToEdit?.icon ?: ICON_MAP.keys.first())
    }

    // ИСПРАВЛЕНО: Более безопасный парсинг дней недели
    var selectedDays by rememberSaveable {
        mutableStateOf(
            if (habitToEdit != null && habitToEdit.type != "daily") {
                try {
                    habitToEdit.type.split(",")
                        .mapNotNull { it.trim().toIntOrNull() } // mapNotNull + toIntOrNull безопаснее
                        .toSet()
                } catch (e: Exception) {
                    setOf(1, 2, 3, 4, 5, 6, 7)
                }
            } else {
                setOf(1, 2, 3, 4, 5, 6, 7)
            }
        )
    }

    var notificationTime by rememberSaveable {
        mutableStateOf<String?>(habitToEdit?.notificationTime)
    }

    var showDeleteDialog by remember { mutableStateOf(false) }

    // --- Функция сохранения ---
    val onSaveHabit = {
        if (name.isNotBlank()) {
            val typeValue = if (selectedDays.size == 7) "daily" else
                selectedDays.sorted().joinToString(",")

            // Если редактируем - берем ID старой привычки, иначе 0
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
                icon = selectedIconName, // Сохраняем строковый ключ (напр. "MenuBook")
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
                title = {
                    Text(
                        if (habitToEdit == null) "Новая привычка" else "Редактирование",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Назад"
                        )
                    }
                },
                actions = {
                    if (habitToEdit != null) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(
                                Icons.Default.Delete,
                                contentDescription = "Удалить",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            BottomSaveButton(
                onSave = onSaveHabit,
                text = if (habitToEdit == null) "Создать" else "Сохранить"
            )
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
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // 2. Цель
            OutlinedTextField(
                value = targetValue,
                onValueChange = { if (it.all { char -> char.isDigit() }) targetValue = it },
                label = { Text("Цель на день (раз, км, стр)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), // Добавлена цифровая клавиатура
                supportingText = {
                    if ((targetValue.toIntOrNull() ?: 0) >= 5) {
                        Text("Будет использоваться слайдер для ввода")
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // 3. Иконка
            IconSelectorRow(
                selectedIconName = selectedIconName,
                onIconSelected = { selectedIconName = it }
            )

            Spacer(Modifier.height(16.dp))

            // 4. Цвет
            ColorSelectorRow(
                selectedColor = selectedColor,
                onColorSelected = { selectedColor = it }
            )

            Spacer(Modifier.height(16.dp))

            // 5. Расписание
            ScheduleSelector(
                selectedDays = selectedDays,
                onDayToggle = { day ->
                    selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                }
            )

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

// --- Вспомогательные компоненты ---

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
            Icon(
                Icons.Default.Notifications,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
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
            .imePadding()
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
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
            // ИСПРАВЛЕНО: Перебираем карту, а не список
            ICON_MAP.forEach { (name, iconVector) ->
                val isSelected = name == selectedIconName

                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onIconSelected(name) } // Передаем строковый ключ
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
                val dayIndex = index + 1
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