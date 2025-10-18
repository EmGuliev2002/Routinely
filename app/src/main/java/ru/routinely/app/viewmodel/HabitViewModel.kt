package ru.routinely.app.viewmodel

import androidx.lifecycle.*
import kotlinx.coroutines.launch
import ru.routinely.app.data.HabitRepository
import ru.routinely.app.model.Habit

class HabitViewModel(private val repository: HabitRepository) : ViewModel() {
    val allHabits: LiveData<List<Habit>> = repository.allHabits.asLiveData()

    fun insert(habit: Habit) = viewModelScope.launch {
        repository.insert(habit)
    }
}