package ru.routinely.app.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt
import ru.routinely.app.model.Habit

// Хелпер для получения иконки по имени
fun getIconByName(iconName: String?): ImageVector {
    return when (iconName) {
        "MenuBook" -> Icons.Default.MenuBook
        "SportsGymnastics" -> Icons.Default.SportsGymnastics
        "LocalFireDepartment" -> Icons.Default.LocalFireDepartment
        "SelfImprovement" -> Icons.Default.SelfImprovement
        // Добавьте другие иконки по мере необходимости
        else -> Icons.Default.Menu // Иконка по умолчанию
    }
}

// Состояния для свайпа
private enum class SwipeState {
    IDLE, SWIPED
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HabitItem(
    habit: Habit,
    isCompletedToday: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onItemClick: () -> Unit // Оставляем параметр для совместимости, но не используем его здесь
) {
    // 1. Настройка Swipeable (свайп вправо)
    val swipeableState = rememberSwipeableState(initialValue = SwipeState.IDLE)
    val density = LocalDensity.current
    val swipeThreshold = 100.dp // Порог свайпа

    // Якоря: 0 - на месте, swipeThreshold - состояние SWIPED
    val anchors = with(density) {
        mapOf(
            0f to SwipeState.IDLE,
            swipeThreshold.toPx() to SwipeState.SWIPED
        )
    }

    // Обработка срабатывания свайпа
    LaunchedEffect(swipeableState.currentValue) {
        if (swipeableState.currentValue == SwipeState.SWIPED) {
            // Меняем статус на противоположный
            onCheckedChange(!isCompletedToday)
            // Возвращаем карточку на место с анимацией
            swipeableState.animateTo(SwipeState.IDLE, spring(Spring.DampingRatioMediumBouncy))
        }
    }

    // Определяем цвета и иконки
    val cardColor = habit.color?.let {
        try {
            Color(android.graphics.Color.parseColor(it))
        } catch (e: Exception) {
            MaterialTheme.colorScheme.primary
        }
    } ?: MaterialTheme.colorScheme.primary

    val actionIcon = if (isCompletedToday) Icons.Default.Refresh else Icons.Default.Check

    // Смещение для анимации свайпа
    val offsetX = swipeableState.offset.value.coerceAtLeast(0f)

    // Прогресс свайпа для анимации заднего фона
    val swipeThresholdPx = with(density) { swipeThreshold.toPx() }
    val swipeProgress = (offsetX / swipeThresholdPx).coerceIn(0f, 1f)

    val iconSize = 40.dp * swipeProgress.coerceAtMost(1f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min) // Высота по контенту карточки
    ) {
        // --- Задний фон (иконка действия при свайпе) ---
        Row(
            modifier = Modifier
                .matchParentSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = actionIcon,
                contentDescription = null,
                tint = cardColor,
                modifier = Modifier.size(iconSize)
            )
        }

        // --- Передний план (Карточка привычки) ---
        Card(
            modifier = Modifier
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    orientation = Orientation.Horizontal,
                    thresholds = { _, _ -> FractionalThreshold(0.5f) }
                )
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth()
                .alpha(if (isCompletedToday) 0.6f else 1f), // Полупрозрачность, если выполнено
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            // Здесь НЕТ .clickable, чтобы работал combinedClickable в TodayScreen
        ) {
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

                // Текстовая информация
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White, // На цветном фоне текст белый
                        textDecoration = if (isCompletedToday) TextDecoration.LineThrough else null
                    )

                    Spacer(Modifier.height(4.dp))

                    // Прогресс-бар
                    HabitProgressBar(habit = habit)

                    // Информация о стрике
                    if (habit.currentStreak > 0) {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "${habit.currentStreak} ${if (habit.currentStreak % 10 == 1 && habit.currentStreak % 100 != 11) "день" else "дней"} подряд",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }
    }
}

// Компонент прогресс-бара
@Composable
fun HabitProgressBar(habit: Habit) {
    val progress = if (habit.targetValue > 0) {
        habit.currentValue.toFloat() / habit.targetValue.toFloat()
    } else 0f

    Row(verticalAlignment = Alignment.CenterVertically) {
        LinearProgressIndicator(
            progress = { progress.coerceIn(0f, 1f) },
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color.White,
            trackColor = Color.White.copy(alpha = 0.4f),
        )

        Spacer(Modifier.width(8.dp))

        Text(
            text = "${habit.currentValue}/${habit.targetValue}",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}