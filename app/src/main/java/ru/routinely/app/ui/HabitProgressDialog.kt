package ru.routinely.app.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.routinely.app.model.Habit
import kotlin.math.roundToInt

@Composable
fun HabitProgressDialog(
    habit: Habit,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    // Начальное значение слайдера = текущему значению привычки
    var sliderValue by remember { mutableFloatStateOf(habit.currentValue.toFloat()) }
    val maxVal = habit.targetValue.toFloat()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = habit.name, style = MaterialTheme.typography.titleLarge)
        },
        text = {
            Column {
                Text(
                    text = "Прогресс: ${sliderValue.toInt()} / ${habit.targetValue}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Слайдер
                Slider(
                    value = sliderValue,
                    onValueChange = { sliderValue = it },
                    valueRange = 0f..maxVal,
                    // Количество шагов = (max - min) - 1. Например, если max=10, шагов 9 (1..9)
                    steps = if (habit.targetValue > 1) habit.targetValue - 1 else 0
                )

                // Быстрые кнопки -1 и +1 для удобства
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = {
                        sliderValue = (sliderValue - 1f).coerceAtLeast(0f)
                    }) { Text("-1") }

                    TextButton(onClick = {
                        sliderValue = (sliderValue + 1f).coerceAtMost(maxVal)
                    }) { Text("+1") }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(sliderValue.roundToInt()) }) {
                Text("Сохранить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}