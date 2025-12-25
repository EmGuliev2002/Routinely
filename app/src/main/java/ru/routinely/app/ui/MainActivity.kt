package ru.routinely.app.ui

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import ru.routinely.app.data.AppDatabase
import ru.routinely.app.data.HabitRepository
import ru.routinely.app.data.UserPreferencesRepository
import ru.routinely.app.ui.theme.RoutinelyTheme
import ru.routinely.app.utils.HabitAlarmScheduler
import ru.routinely.app.viewmodel.HabitViewModel
import ru.routinely.app.viewmodel.HabitViewModelFactory
import ru.routinely.app.viewmodel.NotificationEvent

/**
 * Класс, представляющий все возможные разделы приложения.
 */
sealed class Screen(val route: String) {
    object Today : Screen("today")
    object Stats : Screen("stats")
    object Settings : Screen("settings")
    object About : Screen("about") // --- НОВОЕ: Добавлен экран "О программе"
}

/**
 * Главная Activity приложения.
 */
class MainActivity : ComponentActivity() {
    // 1. Инициализация базы данных
    private val database by lazy { AppDatabase.getDatabase(applicationContext) }

    // 2. Инициализация репозитория привычек
    private val repository by lazy { HabitRepository(database.habitDao()) }

    // 3. Инициализация репозитория настроек (DataStore)
    private val userPrefsRepository by lazy { UserPreferencesRepository(applicationContext) }

    // 4. Создание ViewModel с фабрикой
    private val habitViewModel: HabitViewModel by viewModels {
        HabitViewModelFactory(repository, userPrefsRepository)
    }

    // 5. Планировщик уведомлений
    private val alarmScheduler by lazy { HabitAlarmScheduler(applicationContext) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            // Подписываемся на настройки (тему)
            val userPrefs by habitViewModel.userPreferences.collectAsState()

            // Применяем тему
            RoutinelyTheme(darkTheme = userPrefs.isDarkTheme) {
                // Запрос разрешений на уведомления (Android 13+)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    val notificationPermissionLauncher = rememberLauncherForActivityResult(
                        contract = ActivityResultContracts.RequestPermission(),
                        onResult = { /* Обработка результата, если нужна */ }
                    )
                    LaunchedEffect(Unit) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation(habitViewModel = habitViewModel)
                }
            }
        }

        // Наблюдение за событиями уведомлений из ViewModel
        habitViewModel.notificationEvent.observe(this) { event ->
            when (event) {
                is NotificationEvent.Schedule -> alarmScheduler.schedule(event.habit)
                is NotificationEvent.Cancel -> alarmScheduler.cancel(event.habitId)
            }
        }
    }
}

/**
 * Определяет структуру навигации приложения с Bottom Bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation(habitViewModel: HabitViewModel) {
    val navController = rememberNavController()

    Scaffold(
        // Нижняя навигационная панель
        bottomBar = {
            RoutinelyBottomBar(navController = navController)
        }
    ) { paddingValues ->
        // Контейнер навигации
        NavHost(
            navController = navController,
            startDestination = Screen.Today.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Раздел "Сегодня"
            composable(Screen.Today.route) {
                TodayScreen(habitViewModel = habitViewModel)
            }

            // Раздел "Статистика"
            composable(Screen.Stats.route) {
                StatsScreen(
                    habitViewModel = habitViewModel,
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            // Раздел "Настройки"
            composable(Screen.Settings.route) {
                SettingsScreen(
                    habitViewModel = habitViewModel,
                    onNavigateBack = { navController.popBackStack() },
                    // --- НОВОЕ: Передаем навигацию на экран "О программе"
                    onNavigateToAbout = { navController.navigate(Screen.About.route) }
                )
            }

            // Экран "О программе" ---
            composable(Screen.About.route) {
                AboutScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
        }
    }
}