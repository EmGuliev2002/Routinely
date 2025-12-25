package ru.routinely.app.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.* // Используем Rounded для красоты
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

// --- ГЛОБАЛЬНАЯ КАРТА ИКОНОК ---
// Доступна из любой части UI-пакета
val ALL_ICONS: Map<String, ImageVector> = mapOf(
    // Спорт и Активность
    "Бег" to Icons.Default.DirectionsRun,
    "Фитнес" to Icons.Default.FitnessCenter,
    "Велосипед" to Icons.Default.PedalBike,
    "Ходьба" to Icons.Default.DirectionsWalk,
    "Йога" to Icons.Default.SelfImprovement,
    "Плавание" to Icons.Default.Pool,
    "Спорт" to Icons.Default.SportsGymnastics,

    // Здоровье и Еда
    "Вода" to Icons.Default.WaterDrop,
    "Еда" to Icons.Default.Restaurant,
    "Яблоко" to Icons.Default.LocalDining,
    "Сон" to Icons.Default.Bed,
    "Медицина" to Icons.Default.MedicalServices,
    "Не курить" to Icons.Default.SmokeFree,

    // Работа и Учеба
    "Работа" to Icons.Default.Work,
    "Учеба" to Icons.Default.School,
    "Книги" to Icons.Default.MenuBook,
    "Код" to Icons.Default.Code,
    "Компьютер" to Icons.Default.Computer,
    "Деньги" to Icons.Default.Savings,
    "Идеи" to Icons.Default.Lightbulb,

    // Дом и Быт
    "Дом" to Icons.Default.Home,
    "Уборка" to Icons.Default.CleaningServices,
    "Покупки" to Icons.Default.ShoppingCart,
    "Готовка" to Icons.Default.Kitchen,
    "Семья" to Icons.Default.FamilyRestroom,
    "Питомцы" to Icons.Default.Pets,

    // Хобби и Отдых
    "Игры" to Icons.Default.SportsEsports,
    "Музыка" to Icons.Default.MusicNote,
    "Рисование" to Icons.Default.Brush,
    "Фото" to Icons.Default.CameraAlt,
    "Путешествия" to Icons.Default.Flight,
    "Природа" to Icons.Default.Forest,
    "Огонь" to Icons.Default.LocalFireDepartment
)

val DEFAULT_COLOR_HEX = "#B88EFA"
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

    // Инициализация иконки: берем из привычки или первую из карты
    var selectedIconName by rememberSaveable {
        mutableStateOf(habitToEdit?.icon ?: ALL_ICONS.keys.first())
    }

    // Парсинг дней недели
    var selectedDays by rememberSaveable {
        mutableStateOf(
            if (habitToEdit != null && habitToEdit.type != "daily") {
                try {
                    habitToEdit.type.split(",")
                        .mapNotNull { it.trim().toIntOrNull() }
                        .toSet()
                } catch (e: Exception) {
                    setOf(1, 2, 3, 4, 5, 6, 7)
                }
            } else {
                setOf(1, 2, 3, 4, 5, 6, 7) // По умолчанию все дни
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
            // Если выбраны все 7 дней, считаем это "daily"
            val typeValue = if (selectedDays.size == 7) "daily" else
                selectedDays.sorted().joinToString(",")

            val habitId = habitToEdit?.id ?: 0

            // Сохраняем прогресс, если редактируем
            val currentVal = habitToEdit?.currentValue ?: 0
            val streak = habitToEdit?.currentStreak ?: 0
            val bestStreak = habitToEdit?.bestStreak ?: 0
            val lastDate = habitToEdit?.lastCompletedDate
            val creation = habitToEdit?.creationDate ?: System.currentTimeMillis()

            val newHabit = Habit(
                id = habitId,
                name = name,
                icon = selectedIconName, // Сохраняем ключ (строку)
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

        // Диалог удаления
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
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                supportingText = {
                    if ((targetValue.toIntOrNull() ?: 0) >= 5) {
                        Text("Будет использоваться слайдер для ввода")
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // 3. Выбор Иконки (Обновленный)
            IconSelectorRow(
                selectedIconName = selectedIconName,
                onIconSelected = { selectedIconName = it }
            )

            Spacer(Modifier.height(16.dp))

            // 4. Выбор Цвета
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

// --- Компоненты UI ---

@Composable
fun IconSelectorRow(selectedIconName: String, onIconSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Выберите иконку",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))

        // Используем LazyRow для горизонтальной прокрутки большого количества иконок
        LazyRow(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(ALL_ICONS.toList()) { (name, iconVector) ->
                val isSelected = name == selectedIconName

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.clickable { onIconSelected(name) }
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(CircleShape)
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                    ) {
                        Icon(
                            imageVector = iconVector,
                            contentDescription = name,
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                    // Опционально: подпись иконки
                    // Text(name, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

@Composable
fun ColorSelectorRow(selectedColor: String, onColorSelected: (String) -> Unit) {
    // Расширенная палитра цветов
    val colors = listOf(
        "#B88EFA", "#FF70A6", "#70A6FF", "#5CC8A5", "#FFBF69", "#A8DADC",
        "#E63946", "#F1FAEE", "#457B9D", "#1D3557", "#2A9D8F", "#E9C46A"
    )

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            "Выберите цвет",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )
        Spacer(Modifier.height(8.dp))

        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(colors) { colorHex ->
                val isSelected = colorHex == selectedColor
                val size = if (isSelected) 40.dp else 32.dp
                val borderWidth = if (isSelected) 3.dp else 0.dp

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
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceContainerHigh
                        )
                        .clickable { onDayToggle(dayIndex) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = dayName,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationTimePicker(time: String?, onTimeSelected: (String?) -> Unit) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    val dialog = TimePickerDialog(
        context,
        { _, hour, minute ->
            onTimeSelected(String.format("%02d:%02d", hour, minute))
        },
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