package com.healthtech.doccareplusdoctor.ui.telehealth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.data.remote.api.FirebaseApi
import com.healthtech.doccareplusdoctor.domain.model.Appointment
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

@HiltViewModel
class TelehealthViewModel @Inject constructor(
    private val firebaseApi: FirebaseApi,
    private val auth: FirebaseAuth
) : ViewModel() {

    // Không cần loadUpcomingAppointments vì chúng ta chỉ hiển thị ZIMKit UI

    private val currentDoctorId: String
        get() = auth.currentUser?.uid ?: ""

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    init {
        Timber.tag("TelehealthViewModel").d("Current doctor ID: %s", currentDoctorId)
    }

    fun startVideoCall(appointmentId: String) {
        Timber.d("Starting video call for appointment: $appointmentId")
    }

    fun startNewChat(context: Context, userId: String) {
        try {
            // Sử dụng ZIMKit API để kiểm tra user
            com.zegocloud.zimkit.services.ZIMKit.queryUserInfo(
                userId
            ) { user, error ->
                if (user != null) {
                    ZIMKitRouter.toMessageActivity(
                        context,
                        userId,
                        ZIMKitConversationType.ZIMKitConversationTypePeer
                    )
                } else {
                    viewModelScope.launch {
                        _errorEvent.emit("Không tìm thấy người dùng với ID: $userId trên hệ thống")
                    }
                }
            }
        } catch (e: Exception) {
            Timber.e("Error starting chat: ${e.message}")
            viewModelScope.launch {
                _errorEvent.emit("Không thể bắt đầu cuộc trò chuyện: ${e.message}")
            }
        }
    }
}