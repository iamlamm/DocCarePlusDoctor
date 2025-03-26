package com.healthtech.doccareplusdoctor.ui.appointments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.data.remote.api.FirebaseApi
import com.healthtech.doccareplusdoctor.domain.model.Appointment
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class AppointmentViewModel @Inject constructor(
    private val firebaseApi: FirebaseApi,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _appointmentsState = MutableStateFlow<UiState<List<Appointment>>>(UiState.Idle)
    val appointmentsState: StateFlow<UiState<List<Appointment>>> = _appointmentsState

    private val currentDoctorId: String
        get() = auth.currentUser?.uid ?: ""

    init {
        Timber.d("Current doctor ID: %s", currentDoctorId)
        loadAppointments()
    }

    fun loadAppointments() {
        _appointmentsState.value = UiState.Loading
        Timber.d("Loading appointments for doctor: $currentDoctorId")

        viewModelScope.launch {
            try {
                firebaseApi.getDoctorAppointments(currentDoctorId).collect { appointments ->
                    Timber.d("Received ${appointments.size} appointments from Firebase")

                    if (appointments.isEmpty()) {
                        Timber.d("No appointments found")
                        _appointmentsState.value = UiState.Success(emptyList())
                    } else {
                        val updatedAppointments = updateAppointmentStatusByDate(appointments)
                        Timber.d("Updated ${updatedAppointments.size} appointments")
                        _appointmentsState.value = UiState.Success(updatedAppointments)
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Error loading appointments")
                _appointmentsState.value = UiState.Error("Lỗi khi tải lịch hẹn: ${e.message}")
            }
        }
    }

    /**
     * Cập nhật trạng thái cuộc hẹn dựa vào ngày hiện tại
     * - Nếu ngày hẹn < ngày hiện tại: trạng thái sẽ là "completed"
     * - Nếu ngày hẹn >= ngày hiện tại: giữ nguyên trạng thái
     */
    private fun updateAppointmentStatusByDate(appointments: List<Appointment>): List<Appointment> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        return appointments.map { appointment ->
            try {
                val appointmentDate = dateFormat.parse(appointment.date)

                // Nếu ngày hẹn đã qua và trạng thái vẫn là "upcoming", đổi thành "completed"
                if (appointmentDate != null && appointmentDate.before(today) && appointment.status == "upcoming") {
                    // Tạo bản sao mới với status đã cập nhật
                    appointment.copy(status = "completed")
                } else {
                    // Giữ nguyên appointment nếu không cần thay đổi
                    appointment
                }
            } catch (e: Exception) {
                appointment
            }
        }
    }

    fun rescheduleAppointment(appointmentId: String) {
        // Xử lý logic đổi lịch (sẽ triển khai sau)
    }

    fun cancelAppointment(appointmentId: String) {
        _appointmentsState.value = UiState.Loading

        viewModelScope.launch {
            val result = firebaseApi.updateAppointmentStatus(appointmentId, "cancelled")

            result.fold(
                onSuccess = {
                    loadAppointments() // Tải lại danh sách sau khi hủy
                },
                onFailure = { error ->
                    _appointmentsState.value =
                        UiState.Error("Không thể hủy lịch hẹn: ${error.message}")
                }
            )
        }
    }
}