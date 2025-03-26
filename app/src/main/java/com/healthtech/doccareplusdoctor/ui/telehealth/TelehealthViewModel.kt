package com.healthtech.doccareplusdoctor.ui.telehealth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import com.zegocloud.zimkit.common.ZIMKitRouter
import com.zegocloud.zimkit.common.enums.ZIMKitConversationType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class TelehealthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val doctorPreferences: DoctorPreferences
) : ViewModel() {

    private val currentDoctorId: String
        get() = auth.currentUser?.uid ?: ""

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent = _errorEvent.asSharedFlow()

    init {
        Timber.tag("TelehealthViewModel").d("Current doctor ID: %s", currentDoctorId)
    }

    fun getDoctorId(): String {
        return doctorPreferences.getDoctor()?.id ?: currentDoctorId
    }

    fun getDoctorName(): String {
        return doctorPreferences.getDoctor()?.name ?: "Bác sĩ"
    }

    fun startVideoCall(appointmentId: String) {
        Timber.d("Starting video call for appointment: $appointmentId")
    }

    fun startNewChat(context: Context, userId: String) {
        try {
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