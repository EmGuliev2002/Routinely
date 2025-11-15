package ru.routinely.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.routinely.app.model.Habit
import ru.routinely.app.viewmodel.HabitViewModel
import android.graphics.Color as AndroidColor // Импорт для использования Color.parseColor

// Временный список привычек для отображения дизайна (ЗАГЛУШКА)
val mockHabitsForStats = listOf(
    // icon: "MenuBook", "SportsGymnastics", "LocalFireDepartment", "SelfImprovement"
    Habit(1, "Чтение", "MenuBook", "#B88EFA", type = "daily", targetValue = 15, currentValue = 15),
    Habit(2, "Уборка", "SportsGymnastics", "#FF69B4", type = "daily", targetValue = 1, currentValue = 1),
    Habit(3, "Бег", "LocalFireDepartment", "#FFA500", type = "daily", targetValue = 10, currentValue = 5),
    Habit(4, "Сон", "SelfImprovement", "#00BFFF", type = "daily", targetValue = 8, currentValue = 0),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    habitViewModel: HabitViewModel,
    onNavigateBack: () -> Unit
) {
    // TODO: Здесь будет подписка на данные статистики из ViewModel

    Scaffold(
        topBar = { StatsTopBar(onNavigateBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- 1. Секция Календаря и Недели ---
            StatsHeaderSection()

            // --- 2. Список выполненных/невыполненных привычек за выбранный день ---
            DailyHabitCompletionList(mockHabitsForStats)

            Spacer(Modifier.height(24.dp))

            // --- 3. График прогресса (Пока заглушка) ---
            StatProgressChart(mockHabitsForStats.first())

            Spacer(Modifier.height(16.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = "Статистика", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun StatsHeaderSection() {
    val days = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "13.01–19.01",
                fontWeight = FontWeight.SemiBold
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                days.forEachIndexed { index, day ->
                    DayBadge(
                        day = day,
                        isSelected = index == 4, // "Пт" - выбранный день на скриншоте
                        isCompleted = index <= 4 // Пн-Пт выполнены
                    )
                }
            }
        }
    }
}

@Composable
fun DayBadge(day: String, isSelected: Boolean, isCompleted: Boolean) {
    val completedColor = Color(0xFFC8A2C8) // Фиолетовый из дизайна

    val backgroundColor = when {
        isSelected -> MaterialTheme.colorScheme.primary
        isCompleted -> completedColor
        else -> MaterialTheme.colorScheme.surface
    }

    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .clickable { /* TODO: Смена дня */ },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = day,
            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.labelMedium
        )
    }
}

@Composable
fun DailyHabitCompletionList(habits: List<Habit>) {
    Text(
        text = "Пятница 17.01",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp)) {
            habits.forEachIndexed { index, habit ->
                StatsHabitListItem(habit)
                if (index < habits.lastIndex) {
                    Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            }
        }
    }
}


@Composable
fun StatsHabitListItem(habit: Habit) {
    // Временная логика для соответствия скриншоту
    val isCompleted = habit.id <= 2
    val streakValue = if (isCompleted) "3 дн." else "0 дн."
    val completionTime = if (isCompleted) "18:50" else "00:00"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Иконка (используем функцию getIconByName из HabitItem.kt)
        Icon(
            imageVector = getIconByName(habit.icon),
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(AndroidColor.parseColor(habit.color ?: "#B88EFA")).copy(alpha = 0.85f))
                .padding(8.dp)
        )
        Spacer(Modifier.width(12.dp))

        // 2. Название и Прогресс
        Column(modifier = Modifier.weight(1f)) {
            Text(text = habit.name, fontWeight = FontWeight.Medium)
            Text(
                text = if (habit.targetValue > 1) "${habit.currentValue}/${habit.targetValue} стр." else "1 раз",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 3. Статус выполнения
        Icon(
            imageVector = if (isCompleted) Icons.Default.CheckCircle else Icons.Default.Circle,
            contentDescription = if (isCompleted) "Выполнено" else "Не выполнено",
            tint = if (isCompleted) Color(0xFFC8A2C8) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
            modifier = Modifier.size(20.dp)
        )

        Spacer(Modifier.width(12.dp))

        // 4. Стрик
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(40.dp)) {
            Text(text = streakValue, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
            Text(text = "ст.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.width(12.dp))

        // 5. Время
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Schedule, contentDescription = "Время выполнения", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
            Text(text = completionTime, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/**
 * Заглушка для графика прогресса.
 */
@Composable
fun StatProgressChart(habit: Habit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "График прогресса по: ${habit.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(16.dp))

            // Место для графика (Placeholder)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Здесь будет график",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Для обеспечения работы, если getIconByName не видна из HabitItem.kt
// Если HabitItem.kt находится в том же пакете (ru.routinely.app.ui), ее можно удалить.
// Оставляем для надежности.
fun getIconByName(iconName: String?): ImageVector {
    return when (iconName) {
        "MenuBook" -> Icons.Default.MenuBook
        "SportsGymnastics" -> Icons.Default.SportsGymnastics
        "LocalFireDepartment" -> Icons.Default.LocalFireDepartment
        "SelfImprovement" -> Icons.Default.SelfImprovement
        else -> Icons.Default.Circle
    }
}