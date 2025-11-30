package ru.routinely.app.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.routinely.app.data.HabitRepository
import ru.routinely.app.data.UserPreferencesRepository

class HabitViewModelFactory(
    private val repository: HabitRepository,
    private val userPrefsRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(HabitViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return HabitViewModel(repository, userPrefsRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}