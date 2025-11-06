package ru.routinely.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border // Нужен для ColorSelectorRow
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape // Нужен для ColorSelectorRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector // Нужен для IconSelectorRow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.routinely.app.model.Habit // Обязательный импорт для модели данных
import ru.routinely.app.viewmodel.HabitViewModel // Обязательный импорт для ViewModel
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.SportsGymnastics
import androidx.compose.ui.draw.clip

// Временные заглушки для упрощения.
val DEFAULT_COLOR_HEX = "#B88EFA"
val ICON_OPTIONS = listOf(
    Icons.Default.MenuBook, Icons.Default.SportsGymnastics,
    Icons.Default.LocalFireDepartment, Icons.Default.SelfImprovement
)

val WEEK_DAYS = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
val WEEK_DAY_INDICES = 1..7 // Используем индексы для хранения в БД

/**
 * Экран для создания и редактирования привычки (Stateful).
 * @param viewModel ViewModel для сохранения данных.
 * @param onNavigateBack Колбэк для закрытия экрана (Bottom Sheet).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    viewModel: HabitViewModel,
    onNavigateBack: () -> Unit
) {
    // --- Состояние экрана (временные переменные для новой привычки) ---
    var name by rememberSaveable { mutableStateOf("") }
    var targetValue by rememberSaveable { mutableStateOf("1") }
    var selectedColor by rememberSaveable { mutableStateOf(DEFAULT_COLOR_HEX) }
    // Сохраняем имя иконки, чтобы потом ее получить
    var selectedIconName by rememberSaveable { mutableStateOf(ICON_OPTIONS.first().name) }
    // Временно для типа привычки
    var selectedDays by rememberSaveable { mutableStateOf(setOf(1, 2, 3, 4, 5, 6, 7)) } // По умолчанию - ежедневно (все дни)

    // --- Функция для сохранения ---
    val onSaveHabit = {
        if (name.isNotBlank()) {
            val typeValue = if (selectedDays.size == 7) {
                "daily"
            } else {
                // Иначе сохраняем список выбранных дней в виде строки, например: "1,3,5"
                selectedDays.sorted().joinToString(",")
            }

            val newHabit = Habit(
                name = name,
                icon = selectedIconName, // Теперь сохраняем строковое имя иконки
                color = selectedColor,
                type = typeValue,
                targetValue = targetValue.toIntOrNull() ?: 1,
                creationDate = System.currentTimeMillis()
                // Остальные поля инициализируются по умолчанию в модели
            )
            viewModel.saveHabit(newHabit)
            onNavigateBack() // Закрываем экран после сохранения
        }
    }

    // В Bottom Sheet не нужен полноценный Scaffold с TopAppBar,
    // но мы можем использовать его для структуры.
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Новая привычка", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        },
        bottomBar = {
            BottomSaveButton(onSaveHabit)
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Поле ввода Названия
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Название привычки") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.height(16.dp))

            // 2. Поле для Целевого значения
            OutlinedTextField(
                value = targetValue,
                onValueChange = { targetValue = it.filter { char -> char.isDigit() } },
                label = { Text("Целевое значение (1 - для бинарных)") },
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
            )
            Spacer(Modifier.height(32.dp))

            // 3. Выбор иконки
            IconSelectorRow(
                selectedIconName = selectedIconName,
                onIconSelected = { selectedIconName = it }
            )
            Spacer(Modifier.height(32.dp))

            // 4. Выбор цвета
            Text("Выберите цвет", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Spacer(Modifier.height(8.dp))
            ColorSelectorRow(selectedColor = selectedColor) { selectedColor = it }
            Spacer(Modifier.height(64.dp))

            ScheduleSelector(
                selectedDays = selectedDays,
                onDayToggle = { dayIndex ->
                    selectedDays = if (dayIndex in selectedDays) {
                        selectedDays - dayIndex
                    } else {
                        selectedDays + dayIndex
                    }
                }
            )



            Spacer(Modifier.height(64.dp))
        }
    }
}

/**
 * Компонент для выбора цвета.
 */
@Composable
fun ColorSelectorRow(selectedColor: String, onColorSelected: (String) -> Unit) {
    val colors = listOf("#B88EFA", "#FF70A6", "#70A6FF", "#5CC8A5", "#FFBF69", "#A8DADC")

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
                    .then(
                        if (isSelected) Modifier.border(borderWidth, Color.White, CircleShape)
                        else Modifier
                    )
            )
        }
    }
}

/**
 * Компонент для выбора иконки.
 */
@Composable
fun IconSelectorRow(selectedIconName: String, onIconSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Выберите иконку", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
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
                val fullName = icon.name
                val simpleName = fullName.substringAfterLast('.')

                Icon(
                    imageVector = icon,
                    contentDescription = simpleName,
                    tint = if (simpleName == selectedIconName)
                        MaterialTheme.colorScheme.primary
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clickable { onIconSelected(simpleName) }
                )
            }
        }
    }
}

/**
 * Нижняя кнопка для сохранения.
 */
@Composable
fun BottomSaveButton(onSave: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .height(72.dp) // Добавим немного высоты для отступа
    ) {
        Button(
            onClick = onSave,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
        ) {
            Text("Сохранить привычку", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
fun ScheduleSelector(selectedDays: Set<Int>, onDayToggle: (Int) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Расписание", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
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
                val dayIndex = index + 1 // Индекс дня (1-7)
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