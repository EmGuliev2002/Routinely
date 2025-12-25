package ru.routinely.app.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ru.routinely.app.model.Habit
import ru.routinely.app.ui.effects.ConfettiExplosion
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// --- Хелперы ---

fun getIconByName(iconName: String?): ImageVector {
    return ALL_ICONS[iconName] ?: Icons.Default.Warning
}

private enum class SwipeState { IDLE, TOGGLE_STATUS, DELETE }

// --- Основной Компонент ---

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HabitItem(
    habit: Habit,
    isCompletedToday: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onItemClick: () -> Unit
) {
    // --- 1. Настройка Swipeable (Свайп) ---
    val swipeableState = rememberSwipeableState(initialValue = SwipeState.IDLE)
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val swipeThreshold = 80.dp
    val swipeThresholdPx = with(density) { swipeThreshold.toPx() }
    val anchors = mapOf(
        0f to SwipeState.IDLE,
        swipeThresholdPx to SwipeState.TOGGLE_STATUS,
        -swipeThresholdPx to SwipeState.DELETE
    )

    // --- 2. Эффекты (Конфетти и Вибрация) ---
    // УБРАЛИ: toneGenerator
    var showConfetti by remember { mutableStateOf(false) }

    // Флаг для отслеживания первой отрисовки
    var isInitialComposition by remember { mutableStateOf(true) }

    // Логика переключения статуса и запуска эффектов
    LaunchedEffect(isCompletedToday) {
        if (isInitialComposition) {
            isInitialComposition = false
            return@LaunchedEffect
        }

        if (isCompletedToday) {
            showConfetti = true
            // Добавляем вибрацию при успешном выполнении (через чекбокс)
            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        } else {
            showConfetti = false
        }
    }

    // Логика действий при свайпе
    LaunchedEffect(swipeableState.currentValue) {
        when (swipeableState.currentValue) {
            SwipeState.TOGGLE_STATUS -> {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                onCheckedChange(!isCompletedToday)
                swipeableState.animateTo(SwipeState.IDLE)
            }
            SwipeState.DELETE -> {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onDelete()
                swipeableState.animateTo(SwipeState.IDLE)
            }
            else -> Unit
        }
    }

    // --- 3. Анимации UI ---

    val iconScale by animateFloatAsState(
        targetValue = if (isCompletedToday) 1.2f else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )

    val textAlpha by animateFloatAsState(
        targetValue = if (isCompletedToday) 0.5f else 1f,
        label = "textAlpha"
    )

    val baseColor = habit.color?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { MaterialTheme.colorScheme.primary }
    } ?: MaterialTheme.colorScheme.primary

    val animatedCardColor by animateColorAsState(
        targetValue = if (isCompletedToday) baseColor.copy(alpha = 0.8f) else baseColor,
        label = "cardColor"
    )

    val offsetX = swipeableState.offset.value
    val isSwipingRight = offsetX > 0
    val isSwipingLeft = offsetX < 0
    val actionIconRight = if (isCompletedToday) Icons.Default.Refresh else Icons.Default.Check
    val swipeProgress = (offsetX.absoluteValue / swipeThresholdPx).coerceIn(0f, 1f)
    val swipeIconSize = 32.dp * swipeProgress

    // --- UI Верстка ---

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        // Задний фон свайпа
        if (isSwipingRight) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = actionIconRight,
                    contentDescription = null,
                    tint = animatedCardColor,
                    modifier = Modifier.padding(start = 24.dp).size(swipeIconSize)
                )
            }
        }
        if (isSwipingLeft) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.errorContainer),
                contentAlignment = Alignment.CenterEnd
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(end = 24.dp).size(swipeIconSize)
                )
            }
        }

        // Карточка привычки
        Card(
            modifier = Modifier
                .swipeable(
                    state = swipeableState,
                    anchors = anchors,
                    orientation = Orientation.Horizontal,
                    thresholds = { _, _ -> FractionalThreshold(0.3f) }
                )
                .offset { IntOffset(offsetX.roundToInt(), 0) }
                .fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = animatedCardColor),
            elevation = CardDefaults.cardElevation(defaultElevation = if (isCompletedToday) 0.dp else 4.dp),
            onClick = onItemClick
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // 1. Иконка с эффектом
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .scale(iconScale)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    ConfettiExplosion(
                        visible = showConfetti,
                        modifier = Modifier.size(200.dp)
                    )

                    Icon(
                        imageVector = getIconByName(habit.icon),
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(Modifier.width(16.dp))

                // 2. Текстовый контент
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = habit.name,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = Color.White.copy(alpha = textAlpha),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textDecoration = if (isCompletedToday) TextDecoration.LineThrough else null
                    )

                    Spacer(Modifier.height(4.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.alpha(0.9f * textAlpha)
                    ) {
                        if (habit.notificationTime != null) {
                            Icon(
                                imageVector = Icons.Default.AccessTime,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = habit.notificationTime,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("•", style = MaterialTheme.typography.labelSmall, color = Color.White)
                            Spacer(Modifier.width(8.dp))
                        }
                        Text(
                            text = formatScheduleDescription(habit.type),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White
                        )
                    }

                    if (habit.targetValue > 1) {
                        Spacer(Modifier.height(8.dp))
                        AnimatedHabitProgressBar(habit, textAlpha)
                    }
                }

                // 3. Кнопка выполнения справа
                IconButton(
                    onClick = { onCheckedChange(!isCompletedToday) }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        ConfettiExplosion(
                            visible = showConfetti,
                            modifier = Modifier.size(120.dp)
                        )

                        Icon(
                            imageVector = if (isCompletedToday) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                            contentDescription = "Выполнить",
                            tint = if (isCompletedToday) Color.White else Color.White.copy(alpha = 0.5f),
                            modifier = Modifier.size(32.dp).scale(iconScale)
                        )
                    }
                }
            }
        }
    }
}

// --- Вспомогательные компоненты ---

@Composable
fun AnimatedHabitProgressBar(habit: Habit, parentAlpha: Float) {
    val targetProgress = if (habit.targetValue > 0) {
        habit.currentValue.toFloat() / habit.targetValue.toFloat()
    } else 0f

    val animatedProgress by animateFloatAsState(
        targetValue = targetProgress.coerceIn(0f, 1f),
        animationSpec = spring(stiffness = Spring.StiffnessLow),
        label = "progressAnimation"
    )

    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Text(
                text = "${habit.currentValue} / ${habit.targetValue}",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = parentAlpha)
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp)),
            color = Color.White.copy(alpha = parentAlpha),
            trackColor = Color.White.copy(alpha = 0.3f * parentAlpha),
        )
    }
}

fun formatScheduleDescription(type: String): String {
    return if (type == "daily") {
        "Ежедневно"
    } else {
        try {
            val daysMap = mapOf(1 to "Пн", 2 to "Вт", 3 to "Ср", 4 to "Чт", 5 to "Пт", 6 to "Сб", 7 to "Вс")
            val days = type.split(",").mapNotNull { it.trim().toIntOrNull() }.sorted()
            if (days.size == 7) "Ежедневно"
            else if (days.containsAll(listOf(1, 2, 3, 4, 5)) && days.size == 5) "Будни"
            else if (days.containsAll(listOf(6, 7)) && days.size == 2) "Выходные"
            else days.joinToString(", ") { daysMap[it] ?: "" }
        } catch (e: Exception) {
            "По расписанию"
        }
    }
}