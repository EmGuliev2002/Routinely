package ru.routinely.app.ui.effects

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

// Цвета для конфетти
private val ParticleColors = listOf(
    Color(0xFF8A65D6), // Purple
    Color(0xFFFF70A6), // Pink
    Color(0xFF70A6FF), // Blue
    Color(0xFF5CC8A5), // Teal
    Color(0xFFFFBF69), // Orange
    Color(0xFFE9C46A)  // Gold
)

@Composable
fun ConfettiExplosion(
    visible: Boolean,
    modifier: Modifier = Modifier,
    onAnimationEnd: () -> Unit = {}
) {
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(visible) {
        if (visible) {
            animationProgress.snapTo(0f)
            animationProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 800, easing = LinearOutSlowInEasing)
            )
            onAnimationEnd()
        } else {
            animationProgress.snapTo(0f)
        }
    }

    val progress = animationProgress.value

    if (progress > 0f && progress < 1f) {
        val alpha = if (progress > 0.6f) {
            ((1f - progress) / 0.4f).coerceIn(0f, 1f)
        } else {
            1f
        }

        Canvas(modifier = modifier.fillMaxSize()) {
            val center = this.center
            // Увеличиваем радиус разлета: используем почти всю ширину контейнера
            val maxRadius = size.minDimension / 2

            // Увеличиваем количество частиц до 20
            val particleCount = 20

            // Используем фиксированный сид для Random, чтобы анимация была одинаковой, но "случайной" на вид
            // Или можно просто использовать i для псевдо-рандома

            for (i in 0 until particleCount) {
                // Псевдо-рандом на основе индекса
                val randomFactor = (i * 1337 % 100) / 100f

                // Угол с небольшим смещением, чтобы не было идеального круга
                val angle = (2 * PI * i / particleCount).toFloat() + (randomFactor * 0.5f)

                // Разная скорость полета для частиц (от 70% до 100% радиуса)
                val distanceFactor = 0.7f + (randomFactor * 0.3f)
                val currentRadius = maxRadius * progress * distanceFactor

                val x = center.x + cos(angle) * currentRadius
                val y = center.y + sin(angle) * currentRadius

                // Разный размер частиц
                val baseSize = 6.dp.toPx() // Увеличили базовый размер
                val sizeVariation = baseSize * (0.5f + randomFactor * 0.5f)

                withTransform({
                    rotate(degrees = progress * 360f * (if (i % 2 == 0) 1 else -1), pivot = Offset(x, y))
                }) {
                    drawCircle(
                        color = ParticleColors[i % ParticleColors.size].copy(alpha = alpha),
                        radius = sizeVariation * (1f - progress * 0.3f), // Медленнее уменьшаются
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}