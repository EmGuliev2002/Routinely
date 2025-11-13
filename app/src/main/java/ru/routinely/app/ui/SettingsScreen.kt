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
import kotlinx.coroutines.launch

/**
 * Основной компонент для экрана настроек.
 * @param habitViewModel нужен для сброса данных.
 * @param onNavigateBack функция, вызываемая при нажатии на стрелку "Назад" (для TopBar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    habitViewModel: HabitViewModel,
    onNavigateBack: () -> Unit
) {
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
                // 1.1. Звук (Переключатель)
                SwitchSettingItem(
                    title = "Звук",
                    icon = Icons.Default.VolumeUp,
                    initialValue = true,
                    onCheckedChange = { /* TODO: Здесь будет логика сохранения настройки */ }
                )

                Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // 1.2. Рингтон (Кликабельный элемент)
                ClickableSettingItem(
                    title = "Рингтон",
                    icon = Icons.Default.MusicNote,
                    onClick = { /* TODO: Открыть выбор рингтона */ }
                )
            }

            Spacer(Modifier.height(24.dp))

            // --- 2. Секция Системных настроек ---
            SettingsSection(title = "Системные настройки") {
                // 2.1. Тема
                ClickableSettingItem(
                    title = "Тема",
                    icon = Icons.Default.BrightnessMedium,
                    onClick = { /* TODO: Открыть выбор темы */ }
                )

                Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // 2.2. Сброс данных (С диалогом подтверждения)
                ResetDataSettingItem(habitViewModel = habitViewModel)

                Divider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))

                // 2.3. О приложении
                ClickableSettingItem(
                    title = "О приложении",
                    icon = Icons.Default.Info,
                    onClick = { /* TODO: Открыть экран с информацией о версии */ }
                )
            }
        }
    }
}

// --- Вспомогательные компоненты для SettingsScreen ---

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

/**
 * Элемент для сброса данных, который показывает AlertDialog для подтверждения.
 */
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
                        habitViewModel.clearAllData() // Вызов ViewModel
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

@Composable
fun SwitchSettingItem(title: String, icon: ImageVector, initialValue: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var isChecked by remember { mutableStateOf(initialValue) }
    ListItem(
        modifier = Modifier.clickable { isChecked = !isChecked; onCheckedChange(isChecked) },
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            Switch(checked = isChecked, onCheckedChange = { isChecked = it; onCheckedChange(it) })
        }
    )
}

@Composable
fun ClickableSettingItem(title: String, icon: ImageVector, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(title) },
        leadingContent = { Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
        trailingContent = {
            Icon(Icons.Default.KeyboardArrowRight, contentDescription = "Перейти", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    )
}