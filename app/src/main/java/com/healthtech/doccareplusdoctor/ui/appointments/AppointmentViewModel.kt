package com.healthtech.doccareplusdoctor.ui.appointments

import android.view.View
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.healthtech.doccareplusdoctor.common.state.UiState
import com.healthtech.doccareplusdoctor.data.remote.api.FirebaseApi
import com.healthtech.doccareplusdoctor.domain.model.Appointment
import com.healthtech.doccareplusdoctor.utils.SnackbarUtils
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
    private val firebaseApi: FirebaseApi, private val auth: FirebaseAuth
) : ViewModel() {

    private val _appointmentsState = MutableStateFlow<UiState<List<Appointment>>>(UiState.Idle)
    val appointmentsState: StateFlow<UiState<List<Appointment>>> = _appointmentsState

    private val currentDoctorId: String
        get() = auth.currentUser?.uid ?: ""

    init {
        Timber.d("Current doctor ID: %s", currentDoctorId)
        loadAppointments()
    }

    private fun loadAppointments() {
//        _appointmentsState.value = UiState.Loading
//        Timber.d("Loading appointments for doctor: $currentDoctorId")

        if (_appointmentsState.value is UiState.Loading) {
            Timber.d("Already loading appointments, skipping...")
            return
        }

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
//    private fun updateAppointmentStatusByDate(appointments: List<Appointment>): List<Appointment> {
//        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//        val today = Calendar.getInstance().apply {
//            set(Calendar.HOUR_OF_DAY, 0)
//            set(Calendar.MINUTE, 0)
//            set(Calendar.SECOND, 0)
//            set(Calendar.MILLISECOND, 0)
//        }.time
//
//        return appointments.map { appointment ->
//            try {
//                val appointmentDate = dateFormat.parse(appointment.date)
//
//                if (appointmentDate != null && appointmentDate.before(today) && appointment.status == "upcoming") {
//                    appointment.copy(status = "completed")
//                } else {
//                    appointment
//                }
//            } catch (e: Exception) {
//                appointment
//            }
//        }
//    }

    private suspend fun updateAppointmentStatusByDate(appointments: List<Appointment>): List<Appointment> {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.time

        var hasUpdates = false

        try {
            appointments.forEach { appointment ->
                try {
                    val appointmentDate = dateFormat.parse(appointment.date)
                    if (appointmentDate != null &&
                        appointmentDate.before(today) &&
                        appointment.status == "upcoming"
                    ) {
                        Timber.d("Updating status for appointment ${appointment.id} to completed")
                        val result =
                            firebaseApi.updateAppointmentStatus(appointment.id, "completed")
                        result.fold(
                            onSuccess = { hasUpdates = true },
                            onFailure = { error ->
                                Timber.e("Failed to update appointment ${appointment.id}: ${error.message}")
                            }
                        )
                    }
                } catch (e: Exception) {
                    Timber.e("Error processing appointment ${appointment.id}: ${e.message}")
                }
            }

            if (hasUpdates) {
                Timber.d("Some appointments were updated, reloading data...")
                loadAppointments()
            }
        } catch (e: Exception) {
            Timber.e("Error in updateAppointmentStatusByDate: ${e.message}")
        }

        return appointments
    }

    fun rescheduleAppointment(appointmentId: String) {
        // Xử lý logic đổi lịch
    }

//    fun cancelAppointment(appointmentId: String) {
//        _appointmentsState.value = UiState.Loading
//
//        viewModelScope.launch {
//            val result = firebaseApi.updateAppointmentStatus(appointmentId, "cancelled")
//
//            result.fold(
//                onSuccess = {
//                    loadAppointments()
//                },
//                onFailure = { error ->
//                    _appointmentsState.value =
//                        UiState.Error("Không thể hủy lịch hẹn: ${error.message}")
//                }
//            )
//        }
//    }

    fun cancelAppointment(appointmentId: String, view: View) {
        viewModelScope.launch {
            try {
                val appointment =
                    (appointmentsState.value as? UiState.Success)?.data?.find { it.id == appointmentId }
                        ?: run {
                            SnackbarUtils.showErrorSnackbar(view, "Không tìm thấy lịch hẹn")
                            return@launch
                        }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val appointmentDate = dateFormat.parse(appointment.date)
                val today = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }.time

                // Kiểm tra ngày đã qua
                if (appointmentDate != null && appointmentDate.before(today)) {
                    SnackbarUtils.showErrorSnackbar(view, "Không thể hủy lịch hẹn đã qua")
                    return@launch
                }

                // Kiểm tra trạng thái
                when (appointment.status.lowercase()) {
                    "cancelled" -> {
                        SnackbarUtils.showWarningSnackbar(view, "Lịch hẹn đã bị hủy trước đó")
                        return@launch
                    }

                    "completed" -> {
                        SnackbarUtils.showErrorSnackbar(
                            view, "Không thể hủy lịch hẹn đã hoàn thành"
                        )
                        return@launch
                    }
                }

                _appointmentsState.value = UiState.Loading

                val result = firebaseApi.updateAppointmentStatus(appointmentId, "cancelled")

                result.fold(onSuccess = {
                    SnackbarUtils.showSuccessSnackbar(view, "Đã hủy lịch hẹn thành công")
                    loadAppointments()
                }, onFailure = { error ->
                    SnackbarUtils.showErrorSnackbar(
                        view, "Không thể hủy lịch hẹn: ${error.message}"
                    )
                })
            } catch (e: Exception) {
                Timber.e(e, "Error cancelling appointment")
                SnackbarUtils.showErrorSnackbar(view, "Lỗi khi hủy lịch hẹn: ${e.message}")
            }
        }
    }
}