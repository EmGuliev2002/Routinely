package ru.routinely.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.FractionalThreshold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

import kotlin.math.roundToInt
import ru.routinely.app.model.Habit



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

// Определения якорей для свайпа
private enum class SwipeState {
    IDLE, SWIPED
}
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HabitItem(
    habit: Habit,
    isCompletedToday: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit
) {
    // 1. Настройка Swipeable
    val swipeableState = rememberSwipeableState(initialValue = SwipeState.IDLE)
    val density = LocalDensity.current

    val swipeThreshold = 100.dp // Порог свайпа в dp

    val anchors = with(density) {
        mapOf(
            0f to SwipeState.IDLE,
            swipeThreshold.toPx() to SwipeState.SWIPED
        )
    }

    // Обработка перехода в состояние SWIPED
    LaunchedEffect(swipeableState.currentValue) {
        if (swipeableState.currentValue == SwipeState.SWIPED) {
            // Вызываем логику выполнения/отмены
            onCheckedChange(!isCompletedToday)

            // Возвращаем в IDLE с анимацией
            swipeableState.animateTo(SwipeState.IDLE, spring(Spring.DampingRatioMediumBouncy))
        }
    }

    // Определяем цвет иконки и ее тип
    val cardColor = habit.color?.let { Color(android.graphics.Color.parseColor(it)) } ?: MaterialTheme.colorScheme.primary
    val actionIcon = if (isCompletedToday) Icons.Default.Refresh else Icons.Default.Check

    // Текущее смещение свайпа
    val offsetX = swipeableState.offset.value.coerceAtLeast(0f)

    // Прогресс анимации иконки
    // Получаем порог свайпа в пикселях (Float)
    val swipeThresholdPx = with(density) { swipeThreshold.toPx() }

// Используем полученное значение для расчета прогресса
    val swipeProgress = (offsetX / swipeThresholdPx).coerceIn(0f, 1f)

    // Размер иконки (растет от 0 до 1)
    val iconSize = 40.dp * swipeProgress.coerceAtMost(1f)
    // Альфа иконки (появляется)
    val iconAlpha = swipeProgress.coerceAtMost(1f)


    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .height(IntrinsicSize.Min) // Для корректной работы fillMaxHeight() внутри
    ) {
        // --- Фоновый слой с иконкой действия ---
        Row(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start // Иконка слева
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = cardColor, // Анимированная прозрачность
                modifier = Modifier.size(iconSize) // Анимированный размер
            )
        }

        // --- Передний план (сама карточка привычки) ---
        Card(
            modifier = Modifier
                // 2. Применение модификатора Swipeable
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    orientation = Orientation.Horizontal,
                    thresholds = { _, _ -> FractionalThreshold(0.5f) } // Срабатывание при 50%
                )
                .offset { androidx.compose.ui.unit.IntOffset(offsetX.roundToInt(), 0) } // Смещение карточки
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color.White)
                .clickable(onClick = onItemClick)
                .alpha(if (isCompletedToday) 0.6f else 1f) // Немного приглушаем выполненные
            ,
            shape = RoundedCornerShape(12.dp),
            // ... (остальные параметры Card)
        ) {
            // ... (Вся внутренняя структура Card: Row, Column, Icon, Text, HabitProgressBar)
            // Внутреннее содержимое HabitItem остается без изменений, за исключением Checkbox.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(cardColor)
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Иконка привычки
                Icon(
                    imageVector = getIconByName(habit.icon),
                    contentDescription = habit.name,
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(Modifier.width(16.dp))

                // Информация о привычке
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textDecoration = if (isCompletedToday) TextDecoration.LineThrough else null
                    )
                    Spacer(Modifier.height(4.dp))

                    // Прогресс-бар
                    HabitProgressBar(habit = habit, color = cardColor)

                    // Стрики
                    if (habit.currentStreak > 0) {
                        Text(
                            text = "🔥 ${habit.currentStreak} ${if (habit.currentStreak > 1) "дней подряд" else "день"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
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