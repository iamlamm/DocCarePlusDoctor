package com.healthtech.doccareplusdoctor.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtech.doccareplusdoctor.data.local.preferences.DoctorPreferences
import com.healthtech.doccareplusdoctor.domain.model.Doctor
import com.healthtech.doccareplusdoctor.domain.service.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val doctorPreferences: DoctorPreferences,
    private val notificationService: NotificationService
) : ViewModel() {

    private val _doctorData = MutableStateFlow<Doctor?>(null)
    val doctorData: StateFlow<Doctor?> = _doctorData.asStateFlow()

    private val _isDoctorLoggedIn = MutableStateFlow(true)
    val isDoctorLoggedIn: StateFlow<Boolean> = _isDoctorLoggedIn.asStateFlow()

    private val _unreadNotificationCount = MutableStateFlow(0)
    val unreadNotificationCount: StateFlow<Int> = _unreadNotificationCount.asStateFlow()

    init {
        loadDoctorData()
        checkLoginStatus()
        observeUnreadNotifications()
    }

    private fun loadDoctorData() {
        viewModelScope.launch {
            try {
                _doctorData.value = doctorPreferences.getDoctor()
                Timber.d("Thông tin bác sĩ: ${_doctorData.value}")
            } catch (e: Exception) {
                Timber.e("Lỗi khi tải thông tin bác sĩ: ${e.message}")
            }
        }
    }

    private fun checkLoginStatus() {
        viewModelScope.launch {
            try {
                _isDoctorLoggedIn.value = doctorPreferences.isDoctorLoggedIn() 
                Timber.d("Trạng thái đăng nhập: ${_isDoctorLoggedIn.value}")
            } catch (e: Exception) {
                Timber.e("Lỗi khi kiểm tra trạng thái đăng nhập: ${e.message}")
                _isDoctorLoggedIn.value = false
            }
        }
    }

    private fun observeUnreadNotifications() {
        viewModelScope.launch {
            try {
                doctorPreferences.getDoctor()?.id?.let { doctorId ->
                    notificationService.getUnreadNotificationCount(doctorId)
                        .collect { count ->
                            _unreadNotificationCount.value = count
                        }
                }
            } catch (e: Exception) {
                Timber.e("Error observing notifications: ${e.message}")
            }
        }
    }

    fun refreshDoctorData() {
        loadDoctorData()
        checkLoginStatus()
    }
}