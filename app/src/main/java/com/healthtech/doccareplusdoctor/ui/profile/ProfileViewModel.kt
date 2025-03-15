package com.healthtech.doccareplusdoctor.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import com.healthtech.doccareplusdoctor.data.remote.api.FirebaseApi
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import timber.log.Timber

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val doctorPreferences: DoctorPreferences,
    private val firebaseApi: FirebaseApi
) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState<Doctor>>(UiState.Idle)
    val uiState = _uiState.asStateFlow()

    init {
        loadDoctorData()
    }

    private fun loadDoctorData() {
        viewModelScope.launch {
            try {
                _uiState.value = UiState.Loading
                
                // Load local data first
                val localDoctor = doctorPreferences.getDoctor()
                if (localDoctor != null) {
                    _uiState.value = UiState.Success(localDoctor)
                }

                // Subscribe to remote changes
                localDoctor?.id?.let { doctorId ->
                    firebaseApi.getDoctorById(doctorId).collect { result ->
                        result.onSuccess { remoteDoctor ->
                            // Compare and update if different
                            if (remoteDoctor != localDoctor) {
                                _uiState.value = UiState.Success(remoteDoctor)
                                doctorPreferences.saveDoctor(remoteDoctor)
                            }
                        }.onFailure { e ->
                            Timber.e("Error loading remote data: ${e.message}")
                            // Only show error if we don't have local data
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
}
