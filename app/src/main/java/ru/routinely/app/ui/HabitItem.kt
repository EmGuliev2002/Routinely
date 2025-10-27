import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.routinely.app.model.Habit
import kotlin.math.roundToInt

@Composable
fun HabitItem(
    habit: Habit,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    // На самом деле, цвет лучше конвертировать из строки в Color. Я использую заглушку.
    cardColor: Color = Color(android.graphics.Color.parseColor(habit.color ?: "#B88EFA"))
) {
    val isChecked = remember(habit.lastCompletedDate) {
        // Логика: если дата последнего выполнения - сегодня
        habit.lastCompletedDate == System.currentTimeMillis()
    }

    // Вся карточка
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(90.dp) // Фиксированная или адаптивная высота
            .clip(RoundedCornerShape(20.dp)),
        colors = CardDefaults.cardColors(containerColor = cardColor.copy(alpha = 0.85f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {

            // 1. Иконка
            Icon(
                // Используем заглушку, так как нет информации о хранении иконок.
                // В реальном проекте здесь будет логика загрузки иконки по habit.icon
                imageVector = Icons.Default.Menu,
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
                // Название
                Text(
                    text = habit.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )

                // Прогресс/Описание (15 стр. / 10 км.)
                Text(
                    text = "${habit.currentStreak} дн. (ст.).",
                    color = Color.White.copy(alpha = 0.8f),
                    fontSize = 12.sp
                )

                Spacer(Modifier.height(4.dp))

                // 3. Прогресс-бар (только для количественных привычек)
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
                        // Вызов колбэка для обновления состояния привычки в ViewModel
                        onCheckedChange(!isChecked)
                    },
                contentAlignment = Alignment.Center
            ) {
                Checkbox(
                    checked = isChecked,
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