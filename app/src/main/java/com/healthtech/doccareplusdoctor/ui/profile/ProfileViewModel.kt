package com.healthtech.doccareplusdoctor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import com.healthtech.doccareplusdoctor.data.remote.api.FirebaseApi
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import com.healthtech.doccareplusdoctor.domain.repository.AuthRepository
import com.zegocloud.zimkit.services.ZIMKit
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val doctorPreferences: DoctorPreferences,
    private val firebaseApi: FirebaseApi,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Doctor>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    private val _logoutState = MutableStateFlow<UiState<Unit>>(UiState.Idle)
    val logoutState = _logoutState.asStateFlow()

    init {
        loadDoctorData()
    }

    private fun loadDoctorData() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading

                val localDoctor = doctorPreferences.getDoctor()
                if (localDoctor != null) {
                    _uiState.value = UiState.Success(localDoctor)
                }

                localDoctor?.id?.let { doctorId ->
                    firebaseApi.getDoctorById(doctorId).collect { result ->
                        result.onSuccess { remoteDoctor ->
                            if (remoteDoctor != localDoctor) {
                                _uiState.value = UiState.Success(remoteDoctor)
                                doctorPreferences.saveDoctor(remoteDoctor)
                            }
                        }.onFailure { e ->
                            Timber.e("Error loading remote data: ${e.message}")
                            if (_uiState.value !is UiState.Success) {
                                _uiState.value = UiState.Error(e.message ?: "Đã có lỗi xảy ra")
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                if (_uiState.value !is UiState.Success) {
                    _uiState.value = UiState.Error(e.message ?: "Đã có lỗi xảy ra")
                }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                _logoutState.value = UiState.Loading
                authRepository.logout()
                doctorPreferences.clearDoctor()
                ZIMKit.disconnectUser()
                _logoutState.value = UiState.Success(Unit)
            } catch (e: Exception) {
                _logoutState.value = UiState.Error(e.message ?: "Đã có lỗi xảy ra")
            }
        }
    }
}
