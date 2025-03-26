package com.healthtech.doccareplusdoctor.ui.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplusdoctor.DocCarePlusDoctorApplication
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import com.healthtech.doccareplusdoctor.domain.repository.AuthRepository
import com.zegocloud.zimkit.services.ZIMKit
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import im.zego.zim.enums.ZIMErrorCode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val doctorPreferences: DoctorPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val _loginState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val loginState: StateFlow<UiState<Unit>> = _loginState.asStateFlow()

    private val _rememberMeState = MutableStateFlow(false)
    val rememberMeState: StateFlow<Boolean> = _rememberMeState.asStateFlow()

    init {
        _rememberMeState.value = doctorPreferences.isRememberMeChecked()
    }

    fun login(email: String, password: String, rememberMe: Boolean) {
        if (_loginState.value is UiState.Loading) {
            return
        }

        viewModelScope.launch {
            _loginState.value = UiState.Loading
            try {
                doctorPreferences.saveRememberMe(rememberMe)

                val result = authRepository.login(email, password, rememberMe)
                if (result.isSuccess) {
                    val doctor = doctorPreferences.getDoctor()

                    if (doctor != null) {
                        handleLoginSuccess(doctor)
                        connectToZegoCloud(doctor.id, doctor.name, doctor.avatar)
                        _loginState.value = UiState.Success(Unit)
                    } else {
                        _loginState.value = UiState.Error("Không tìm thấy thông tin bác sĩ")
                    }
                } else {
                    _loginState.value = UiState.Error(
                        result.exceptionOrNull()?.message ?: "Đăng nhập thất bại"
                    )
                }
            } catch (e: Exception) {
                _loginState.value = UiState.Error(e.message ?: "Đã xảy ra lỗi")
            }
        }
    }

    fun updateRememberMe(isChecked: Boolean) {
        doctorPreferences.saveRememberMe(isChecked)
        _rememberMeState.value = isChecked
    }

    fun resetLoginState() {
        _loginState.value = UiState.Idle
    }

    private fun connectToZegoCloud(doctorId: String, doctorName: String, doctorAvatar: String) {
        ZIMKit.connectUser(doctorId, doctorName, doctorAvatar) { error ->
            if (error.code != ZIMErrorCode.SUCCESS) {
                Timber.tag("com.healthtech.doccareplusdoctor.ui.auth.LoginViewModel")
                    .e("ZIMKit connect failed: %s", error.message)
            } else {
                Timber.tag("com.healthtech.doccareplusdoctor.ui.auth.LoginViewModel")
                    .d("ZIMKit connect success with doctorId: %s", doctorId)
            }
        }

        val app = context.applicationContext as DocCarePlusDoctorApplication
        app.initZegoCallService(doctorId, doctorName)
    }

    private fun handleLoginSuccess(doctor: Doctor) {
        viewModelScope.launch {
            doctorPreferences.saveDoctor(doctor)
        }
    }
}