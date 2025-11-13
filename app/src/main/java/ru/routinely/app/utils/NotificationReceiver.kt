package ru.routinely.app.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import ru.routinely.app.R

class NotificationReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitName = intent.getStringExtra(EXTRA_HABIT_NAME) ?: "Время выполнить привычку!"
        val habitId = intent.getIntExtra(EXTRA_HABIT_ID, 0)

        // Создаем канал уведомлений (требуется для Android 8.0+)
        createNotificationChannel(context)

        // Создаем само уведомление
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // ВАЖНО: Замените на свою иконку!
            .setContentTitle("Не забудьте о своей цели!")
            .setContentText(habitName)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true) // Уведомление исчезнет по клику
            .build()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Показываем уведомление. Используем habitId как уникальный ID для уведомления.
        notificationManager.notify(habitId, notification)
    }

    private fun createNotificationChannel(context: Context) {
        val name = "Уведомления о привычках"
        val descriptionText = "Канал для ежедневных напоминаний о привычках"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
            description = descriptionText
        }
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_ID = "habit_channel"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
        const val EXTRA_HABIT_ID = "extra_habit_id"
    }
}