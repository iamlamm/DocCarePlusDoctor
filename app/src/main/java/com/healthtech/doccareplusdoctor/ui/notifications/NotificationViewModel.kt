package com.healthtech.doccareplusdoctor.ui.notifications

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import com.healthtech.doccareplusdoctor.domain.service.NotificationService
import com.healthtech.doccareplusdoctor.domain.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import dagger.hilt.android.lifecycle.HiltViewModel
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class NotificationViewModel @Inject constructor(
    private val notificationService: NotificationService,
    private val doctorPreferences: DoctorPreferences
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<List<Notification>>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    init {
        loadNotifications()
    }

    private fun loadNotifications() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                
                val doctorId = doctorPreferences.getDoctor()?.id
                Timber.d("Loading notifications for doctor: $doctorId")
                
                if (doctorId == null) {
                    _uiState.value = UiState.Error("Không tìm thấy thông tin bác sĩ")
                    return@launch
                }

                notificationService.observeNotifications(doctorId).collect { result ->
                    result.onSuccess { notifications ->
                        Timber.d("Loaded ${notifications.size} notifications")
                        _uiState.value = UiState.Success(notifications)
                    }.onFailure { e ->
                        Timber.e(e, "Error loading notifications")
                        _uiState.value = UiState.Error(e.message ?: "Đã có lỗi xảy ra")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error in loadNotifications")
                _uiState.value = UiState.Error(e.message ?: "Đã có lỗi xảy ra")
            }
        }
    }

    fun markAsRead(notificationId: String) {
        viewModelScope.launch {
            try {
                val doctorId = doctorPreferences.getDoctor()?.id ?: return@launch
                notificationService.markAsRead(notificationId, doctorId)
            } catch (e: Exception) {
                Timber.e(e)
            }
        }
    }
}