import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import java.util.concurrent.TimeUnit
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.routinely.app.model.Habit

import java.util.Calendar
import kotlin.math.roundToInt

fun getIconByName(iconName: String?): ImageVector {
    return when (iconName) {
        "MenuBook" -> Icons.Default.MenuBook
        "SportsGymnastics" -> Icons.Default.SportsGymnastics
        "LocalFireDepartment" -> Icons.Default.LocalFireDepartment
        "SelfImprovement" -> Icons.Default.SelfImprovement
        // Убедитесь, что все имена здесь соответствуют тем, что сохраняются в БД
        else -> Icons.Default.Menu // Иконка по умолчанию
    }
}


@Composable
fun HabitItem(
    habit: Habit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    cardColor: Color = Color(android.graphics.Color.parseColor(habit.color ?: "#B88EFA"))
) {
    // Временно проверяем, если выполнение было сегодня, сравнивая с текущей датой.
    val isCompletedToday = remember(habit.lastCompletedDate) {
        if (habit.lastCompletedDate == null) return@remember false

        // Получаем метку времени начала сегодняшнего дня (00:00:00)
        val todayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

        // Привычка выполнена сегодня, если lastCompletedDate >= начало сегодняшнего дня
        habit.lastCompletedDate >= todayStart
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp)
            .clip(RoundedCornerShape(20.dp))
            .then(if (isCompletedToday) Modifier.alpha(0.6f) else Modifier),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 1. Иконка (ИСПРАВЛЕНО)
            Icon(
                imageVector = getIconByName(habit.icon), // <-- ИСПОЛЬЗУЕМ ФУНКЦИЮ
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.1f))
                    .padding(8.dp)
            )

            Spacer(Modifier.width(12.dp))

            // 2. Название, стрик и прогресс
            Column(
                modifier = Modifier.weight(1f)
            ) {
                // ... (остальной код)
                Text(
                    text = habit.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textDecoration = if (isCompletedToday) TextDecoration.LineThrough else null
                )

                Text(
                    text = "${habit.currentStreak} дн. (ст.).",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(4.dp))

                // 3. Прогресс-бар
                if (habit.targetValue > 1) {
                    HabitProgressBar(habit, cardColor)
                }
            }

            Spacer(Modifier.width(16.dp))

            // 4. Чекбокс
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable {
                        onCheckedChange(!isCompletedToday)
                    },
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isCompletedToday,
                    onCheckedChange = onCheckedChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color.White,
                        uncheckedColor = Color.White.copy(alpha = 0.5f),
                        checkmarkColor = cardColor
                    )
                )
            }
        }
    }
}

// Отдельный компонент для прогресс-бара
@Composable
fun HabitProgressBar(habit: Habit, color: Color) {
    val progress = habit.currentValue.toFloat() / habit.targetValue.toFloat()

    Row(verticalAlignment = Alignment.CenterVertically) {
        // Прогресс-бар
        LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
        color = Color.White,
        trackColor = Color.White.copy(alpha = 0.4f),
        strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
        )
        // Текстовое значение прогресса
        Spacer(Modifier.width(8.dp))
        Text(
            text = "${habit.currentValue}/${habit.targetValue}",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}