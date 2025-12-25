package ru.routinely.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
// import androidx.compose.material3.dynamicDarkColorScheme // Убираем или не используем
// import androidx.compose.material3.dynamicLightColorScheme // Убираем или не используем
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.compose.ui.graphics.Color

// Настраиваем темную тему (на случай, если пользователь включит её)
private val DarkColorScheme = darkColorScheme(
    primary = PurplePrimary, // Используем тот же фиолетовый, чтобы бренд сохранялся
    secondary = PurpleContainer,
    tertiary = PinkAccent,
    background = Color(0xFF1C1B1F), // Темный фон
    surface = Color(0xFF1C1B1F),
    surfaceVariant = Color(0xFF49454F)
)

// Настраиваем светлую тему (основная, как на скриншотах)
private val LightColorScheme = lightColorScheme(
    primary = PurplePrimary,             // Кнопки, чекбоксы, активные слайдеры
    onPrimary = PurpleOnPrimary,
    primaryContainer = PurpleContainer,  // Фон активных привычек
    onPrimaryContainer = OnPurpleContainer,
    secondary = PurplePrimary,           // Вторичные элементы тоже фиолетовые
    background = AppBackground,          // Общий фон приложения
    surface = Color.White,               // Фон карточек (белый)
    surfaceVariant = SurfaceVariant,     // Фон "серых" зон и рамок
    onSurface = Color(0xFF1C1B1F),       // Основной текст
    outline = PurplePrimary              // Цвет границ (border)
)

@Composable
fun RoutinelyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic Color отключаем принудительно, поставив false по умолчанию
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        // Если вы хотите оставить динамические цвета только для Android 12+, раскомментируйте,
        // но для единого дизайна лучше оставить ветку else.
        /*
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        */
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Красим статус бар (где часы и батарейка) в цвет фона или primary
            window.statusBarColor = colorScheme.background.toArgb()
            // Делаем иконки статус бара темными, если тема светлая
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography, // Убедитесь, что у вас есть этот файл, или удалите строку
        content = content
    )
}