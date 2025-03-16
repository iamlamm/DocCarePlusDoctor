package com.healthtech.doccareplusdoctor.ui.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplusdoctor.R
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import com.zegocloud.zimkit.services.ZIMKit
import dagger.hilt.android.lifecycle.HiltViewModel
import im.zego.zim.enums.ZIMErrorCode
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val doctorPreferences: DoctorPreferences
) : ViewModel() {
    private val _navigationState = MutableStateFlow<UiState<Int>>(UiState.Idle)
    val navigationState: StateFlow<UiState<Int>> = _navigationState.asStateFlow()

    fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                _navigationState.value = UiState.Loading
                
                delay(500)
                
                val isLoggedIn = doctorPreferences.isDoctorLoggedIn()
                val doctor = doctorPreferences.getDoctor()
                
                Timber.tag("SplashViewModel").d("isLoggedIn: $isLoggedIn, doctor: $doctor")

                val destination = if (isLoggedIn && doctor != null) {
                    try {
                        connectToZegoCloud(doctor.id, doctor.name, doctor.avatar)
                    } catch (e: Exception) {
                        Timber.tag("SplashViewModel")
                            .e("Error connecting to ZegoCloud: %s", e.message)
                    }
                    R.id.appointmentFragment
                } else {
                    R.id.loginFragment
                }

                _navigationState.value = UiState.Success(destination)
            } catch (e: Exception) {
                Timber.tag("SplashViewModel").e("Error checking login status: %s", e.message)
                _navigationState.value =
                    UiState.Error("Không thể kiểm tra trạng thái đăng nhập: ${e.message}")
            }
        }
    }

    private fun connectToZegoCloud(userId: String, userName: String, userAvatar: String) {
        try {
            ZIMKit.connectUser(userId, userName, userAvatar) { error ->
                if (error.code != ZIMErrorCode.SUCCESS) {
                    Timber.tag("SplashViewModel").e("ZIMKit reconnect failed: %s", error.message)
                } else {
                    Timber.tag("SplashViewModel")
                        .d("ZIMKit reconnect success with userId: %s", userId)
                }
            }
        } catch (e: Exception) {
            Timber.tag("SplashViewModel").e("Error in connectToZegoCloud: %s", e.message)
        }
    }
}