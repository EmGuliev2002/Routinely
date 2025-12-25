package ru.routinely.app.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import ru.routinely.app.viewmodel.HabitViewModel

/**
 * Основной экран настроек.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    habitViewModel: HabitViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToAbout: () -> Unit
) {
    // Подписываемся на поток настроек из ViewModel
    val prefs by habitViewModel.userPreferences.collectAsState()

    Scaffold(
        topBar = { SettingsTopBar(onNavigateBack) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(8.dp))

            // --- 1. Секция Уведомлений ---
            SettingsSection(title = "Уведомления") {
                // Переключатель уведомлений
                SwitchSettingItem(
                    title = "Уведомления",
                    icon = Icons.Default.Notifications,
                    isChecked = prefs.notificationsEnabled,
                    onCheckedChange = { isEnabled ->
                        habitViewModel.toggleNotifications(isEnabled)
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- 2. Секция Внешний вид ---
            SettingsSection(title = "Внешний вид") {
                // Переключатель темной темы
                SwitchSettingItem(
                    title = "Темная тема",
                    icon = Icons.Default.DarkMode,
                    isChecked = prefs.isDarkTheme,
                    onCheckedChange = { isDark ->
                        habitViewModel.toggleTheme(isDark)
                    }
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- 3. Секция Данные ---
            SettingsSection(title = "Данные") {
                // Пункт сброса данных (с диалогом)
                ResetDataSettingItem(habitViewModel = habitViewModel)
            }

            Spacer(Modifier.height(24.dp))

            // --- 4. О приложении ---
            SettingsSection(title = "О приложении") {
                ClickableSettingItem(
                    title = "О программе",
                    icon = Icons.Default.Info,
                    onClick = onNavigateToAbout
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

// --- Вспомогательные компоненты ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsTopBar(onNavigateBack: () -> Unit) {
    TopAppBar(
        title = { Text(text = "Настройки", fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
    )
}

@Composable
fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SwitchSettingItem(
    title: String,
    icon: ImageVector,
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    ListItem(
        modifier = Modifier.clickable { onCheckedChange(!isChecked) },
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Switch(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )
        }
    )
}

@Composable
fun ClickableSettingItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        leadingContent = {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        },
        trailingContent = {
            Icon(
                Icons.Default.KeyboardArrowRight,
                contentDescription = "Перейти",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    )
}

@Composable
fun ResetDataSettingItem(habitViewModel: HabitViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Сброс данных") },
            text = { Text("Вы уверены, что хотите удалить ВСЕ данные о привычках? Это действие необратимо.") },
            confirmButton = {
                Button(
                    onClick = {
                        habitViewModel.clearAllData() // Вызов метода очистки
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Удалить все")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) { Text("Отмена") }
            }
        )
    }

    ClickableSettingItem(
        title = "Сброс данных",
        icon = Icons.Default.Delete,
        onClick = { showDialog = true }
    )
}